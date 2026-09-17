package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.core.Candle;
import com.exchange.mod.core.LimitOrder;
import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.db.PlayerAccount;
import com.exchange.mod.util.ExchangeLang;
import com.exchange.mod.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles order placement, market buy/sell, limit orders, derivatives (OMS),
 * delivery contracts, resource licensing/research, and terminal redstone settings.
 */
public final class OrderPacketHandler {

    private OrderPacketHandler() {}

    public static void pinTerminalResource(ServerPlayer player, String resourceId) {
        BlockPos p = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(p.offset(-6, -4, -6), p.offset(6, 4, 6))) {
            if (player.level().getBlockEntity(pos) instanceof com.exchange.mod.blocks.ExchangeTerminalEntity terminal) {
                if (player.getUUID().equals(terminal.getOwnerUuid())) {
                    terminal.setMonitoredResource(resourceId);
                    int sig = terminal.getRedstoneSignal();
                    var res = ExchangeMod.getMarketManager() != null ? ExchangeMod.getMarketManager().getResource(resourceId) : null;
                    player.sendSystemMessage(Component.translatable(
                            "message.exchange.terminal.redstone_pinned",
                            (res != null ? res.getDisplayName() : resourceId),
                            sig
                    ));
                    break;
                }
            }
        }
    }

    public static void handleOrderExecution(ServerPlayer player, ServerboundExecuteOrderPayload payload) {
        if (ExchangeMod.getMarketManager() == null || ExchangeMod.getMarketDAO() == null) {
            return;
        }

        String resId = payload.resourceId();
        int amount = payload.amount();
        if (resId == null || resId.trim().isEmpty() || amount <= 0 || amount > 2304) {
            sendMarketDataToClient(player, "minecraft:iron_ingot", ExchangeLang.notify("invalid_order_params"), true);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resId));
        if (item == null || item == Items.AIR) {
            sendMarketDataToClient(player, "minecraft:iron_ingot", ExchangeLang.notify("unknown_resource"), true);
            return;
        }

        if ("BUY".equalsIgnoreCase(payload.orderType())) {
            try {
                var result = ExchangeMod.getMarketManager().executeBuy(
                        player.getUUID(),
                        player.getName().getString(),
                        resId,
                        amount
                );

                if (result.success()) {
                    // Deliver physical items to player inventory
                    ItemStack stack = new ItemStack(item, amount);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    player.sendSystemMessage(Component.translatable("message.exchange.terminal.buy_success", amount, MarketEngine.round2(result.cbxAmount())));
                    sendMarketDataToClient(player, resId, "key:message.exchange.buy_success;" + amount + ";" + MarketEngine.round2(result.cbxAmount()), false);
                } else {
                    player.sendSystemMessage(Component.translatable("message.exchange.terminal.buy_error", result.message()));
                    sendMarketDataToClient(player, resId, "key:message.exchange.terminal.buy_error;" + result.message(), true);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("message.exchange.terminal.internal_error", String.valueOf(e.getMessage())));
                sendMarketDataToClient(player, resId, "key:message.exchange.terminal.internal_error;" + e.getMessage(), true);
            }
        } else if ("SELL".equalsIgnoreCase(payload.orderType())) {
            // Verify player has the items in inventory
            int available = InventoryHelper.countPlayerItems(player, item);

            if (available < amount) {
                player.sendSystemMessage(Component.translatable("message.exchange.terminal.insufficient_items", available, amount));
                sendMarketDataToClient(player, resId, "key:message.exchange.insufficient_items;" + available + ";" + amount, true);
                return;
            }

            try {
                var result = ExchangeMod.getMarketManager().executeSell(
                        player.getUUID(),
                        player.getName().getString(),
                        resId,
                        amount
                );

                if (result.success()) {
                    // Remove items from player inventory
                    InventoryHelper.removePlayerItems(player, item, amount);

                    if (result.cbxAmount() >= 0) {
                        player.sendSystemMessage(Component.translatable("message.exchange.terminal.sell_success", amount, MarketEngine.round2(result.cbxAmount())));
                        sendMarketDataToClient(player, resId, "key:message.exchange.sell_success;" + amount + ";" + MarketEngine.round2(result.cbxAmount()), false);
                    } else {
                        double fee = Math.abs(result.cbxAmount());
                        player.sendSystemMessage(Component.translatable("message.exchange.terminal.recycle_fee_paid", amount, MarketEngine.round2(fee)));
                        sendMarketDataToClient(player, resId, "key:message.exchange.recycle_fee_paid;" + amount + ";" + MarketEngine.round2(fee), false);
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("message.exchange.terminal.sell_error", result.message()));
                    sendMarketDataToClient(player, resId, "key:message.exchange.terminal.sell_error;" + result.message(), true);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("message.exchange.terminal.internal_error", String.valueOf(e.getMessage())));
                sendMarketDataToClient(player, resId, "key:message.exchange.terminal.internal_error;" + e.getMessage(), true);
            }
        }
    }

    public static void sendMarketDataToClient(ServerPlayer player) {
        sendMarketDataToClient(player, "minecraft:iron_ingot", "", false);
    }

    public static void sendMarketDataToClient(ServerPlayer player, String statusMsg, boolean isError) {
        sendMarketDataToClient(player, "minecraft:iron_ingot", statusMsg, isError);
    }

    public static void sendMarketDataToClient(ServerPlayer player, String targetResourceId, String statusMsg, boolean isError) {
        if (ExchangeMod.getMarketManager() == null || ExchangeMod.getMarketDAO() == null) {
            ExchangeMod.LOGGER.warn("MarketManager or MarketDAO is null when requested by {}", player.getName().getString());
            player.sendSystemMessage(Component.translatable("message.exchange.terminal.db_not_ready"));
            return;
        }

        String resId = (targetResourceId != null && !targetResourceId.isEmpty()) ? targetResourceId : "minecraft:iron_ingot";
        MarketResource res = ExchangeMod.getMarketManager().getResource(resId);
        if (res == null) {
            res = ExchangeMod.getMarketManager().getResource("minecraft:iron_ingot");
            if (res == null) {
                ExchangeMod.LOGGER.warn("Neither {} nor default resource minecraft:iron_ingot found in market", targetResourceId);
                return;
            }
        }

        try {
            PlayerAccount acc = ExchangeMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            List<Candle> rawCandles = ExchangeMod.getMarketDAO().getRecentCandles(res.getResourceId(), "1d", 100);
            if (rawCandles.isEmpty()) {
                ExchangeMod.getMarketManager().generateInitialCandles(res);
                rawCandles = ExchangeMod.getMarketDAO().getRecentCandles(res.getResourceId(), "1d", 100);
            }
            List<MarketDataPayload.CandleItem> candleItems = new ArrayList<>();
            for (Candle c : rawCandles) {
                candleItems.add(new MarketDataPayload.CandleItem(c.getTimestamp(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()));
            }

            Set<String> unlockedSet = ExchangeMod.getMarketManager().getUnlockedResourcesForPlayer(player.getUUID());

            // Collect summaries of all available commodities for quick switching in the GUI
            List<MarketDataPayload.MarketSummaryItem> summaryItems = new ArrayList<>();
            for (MarketResource mr : ExchangeMod.getMarketManager().getAllResources()) {
                double s = mr.getCurrentStock();
                double spot = MarketEngine.round2(MarketEngine.calculateSpotPrice(s, mr));
                double fill = MarketEngine.round2((s / mr.getMaxReserve()) * 100.0);
                boolean isUnlocked = unlockedSet.contains(mr.getResourceId());
                summaryItems.add(new MarketDataPayload.MarketSummaryItem(
                        mr.getResourceId(),
                        mr.getDisplayName(),
                        spot,
                        fill,
                        mr.getDailyModifier(),
                        isUnlocked
                ));
            }

            double stock = res.getCurrentStock();
            String pinnedId = "minecraft:iron_ingot";
            int redstoneMode = 0;
            double thresholdPrice = 7.0;
            boolean thresholdIsLessThan = true;
            BlockPos terminalPos = null;

            BlockPos p = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(p.offset(-6, -4, -6), p.offset(6, 4, 6))) {
                if (player.level().getBlockEntity(pos) instanceof com.exchange.mod.blocks.ExchangeTerminalEntity terminal) {
                    if (player.getUUID().equals(terminal.getOwnerUuid())) {
                        pinnedId = terminal.getMonitoredResource();
                        redstoneMode = terminal.getRedstoneMode();
                        thresholdPrice = terminal.getThresholdPrice();
                        thresholdIsLessThan = terminal.isThresholdIsLessThan();
                        terminalPos = pos.immutable();
                        break;
                    }
                }
            }

            var activeEvent = ExchangeMod.getMarketEventManager() != null ? ExchangeMod.getMarketEventManager().getActiveEvent() : null;
            String eventTitle = activeEvent != null ? activeEvent.getTitle() : "";
            String eventDesc = activeEvent != null ? activeEvent.getDescription() : "";

            // Derivatives data
            List<MarketDataPayload.OMSItem> omsItems = new ArrayList<>();
            if (ExchangeMod.getMarketManager() != null && ExchangeMod.getMarketManager().getOmsManager() != null) {
                var positions = ExchangeMod.getMarketManager().getOmsManager().getPositions(player.getUUID());
                for (var pos : positions) {
                    var mRes = ExchangeMod.getMarketManager().getResource(pos.getResourceId());
                    double pnl = mRes != null ? pos.calculatePnL(mRes) : 0.0;
                    omsItems.add(new MarketDataPayload.OMSItem(
                            pos.getPositionId(),
                            pos.getResourceId(),
                            MarketEngine.round2(pos.getAmountUnits()),
                            MarketEngine.round2(pos.getInvestedCbx()),
                            MarketEngine.round2(pos.getAvgBuyPrice()),
                            MarketEngine.round2(pnl)
                    ));
                }
            }

            List<MarketDataPayload.LimitOrderItem> limitOrderItems = new ArrayList<>();
            if (ExchangeMod.getMarketDAO() != null) {
                var orders = ExchangeMod.getMarketDAO().getPlayerActiveLimitOrders(player.getUUID());
                for (var ord : orders) {
                    limitOrderItems.add(new MarketDataPayload.LimitOrderItem(
                            ord.getOrderId(),
                            ord.getResourceId(),
                            ord.getOrderType(),
                            ord.getAmount(),
                            ord.getLimitPrice(),
                            ord.getReservedCbx(),
                            ord.getStatus()
                    ));
                }
            }

            List<MarketDataPayload.ContractItem> contractItems = new ArrayList<>();
            if (ExchangeMod.getMarketDAO() != null) {
                var contracts = ExchangeMod.getMarketDAO().getAvailableAndPlayerContracts(player.getUUID());
                long gameTime = player.level().getGameTime();
                for (var c : contracts) {
                    boolean isMine = player.getUUID().equals(c.getAcceptedPlayerUuid());
                    long rem = c.getDeadlineTick() > 0 ? Math.max(0, c.getDeadlineTick() - gameTime) : 0;
                    contractItems.add(new MarketDataPayload.ContractItem(
                            c.getContractId(),
                            c.getTitle(),
                            c.getResourceId(),
                            c.getTargetAmount(),
                            c.getDeliveredAmount(),
                            c.getGuaranteedUnitPrice(),
                            c.getCollateralCbx(),
                            isMine,
                            rem,
                            c.getRewardRep(),
                            c.getStatus()
                    ));
                }
            }

            MarketDataPayload payload = new MarketDataPayload(
                    res.getResourceId(),
                    res.getDisplayName(),
                    MarketEngine.round2(MarketEngine.calculateSpotPrice(stock, res)),
                    MarketEngine.round2(MarketEngine.calculateBuyPrice(stock, res)),
                    MarketEngine.round2(MarketEngine.calculateSellPrice(stock, res)),
                    MarketEngine.round2(MarketEngine.calculateDisposalFee(stock, res)),
                    stock,
                    res.getTargetReserve(),
                    res.getMaxReserve(),
                    acc.getBalanceCbx(),
                    acc.getRepLevel(),
                    acc.getRepPoints(),
                    candleItems,
                    statusMsg,
                    isError,
                    res.getBasePrice(),
                    res.getElasticity(),
                    res.getFeeRate(),
                    res.getDisposalAlpha(),
                    res.getMinPriceFloor(),
                    summaryItems,
                    pinnedId,
                    res.getDailyModifier(),
                    eventTitle,
                    eventDesc,
                    redstoneMode,
                    thresholdPrice,
                    thresholdIsLessThan,
                    terminalPos,
                    omsItems,
                    limitOrderItems,
                    contractItems,
                    new ArrayList<>(unlockedSet)
            );

            PacketDistributor.sendToPlayer(player, payload);
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to send market data to client", e);
        }
    }

    public static void handleUnlockResource(ServerPlayer player, ServerboundUnlockResourcePayload payload) {
        if (ExchangeMod.getMarketManager() == null || ExchangeMod.getMarketDAO() == null) return;
        String resId = payload.resourceId();
        if (resId == null || resId.trim().isEmpty()) return;

        if (ExchangeMod.getMarketManager().isResourceUnlockedForPlayer(player.getUUID(), resId)) {
            sendMarketDataToClient(player, resId, ExchangeLang.notify("already_researched"), false);
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resId));
        if (item == null || item == Items.AIR) {
            sendMarketDataToClient(player, resId, ExchangeLang.notify("unknown_resource"), true);
            return;
        }

        int count = InventoryHelper.countPlayerItems(player, item);
        if (count < 1) {
            sendMarketDataToClient(player, resId, ExchangeLang.notify("missing_sample"), true);
            return;
        }

        try {
            InventoryHelper.removePlayerItems(player, item, 1);
            ExchangeMod.getMarketManager().unlockResourceForPlayer(player.getUUID(), resId);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            String name = ExchangeMod.getMarketManager().getResource(resId) != null ? ExchangeMod.getMarketManager().getResource(resId).getDisplayName() : resId;
            player.sendSystemMessage(Component.translatable("message.exchange.research.success", name));
            sendMarketDataToClient(player, resId, ExchangeLang.notify("research_success"), false);
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to unlock resource", e);
            sendMarketDataToClient(player, resId, ExchangeLang.notify("research_error", e.getMessage()), true);
        }
    }

    public static void handleOMSOperation(ServerPlayer player, ServerboundOMSPayload payload) {
        if (ExchangeMod.getMarketManager() == null) return;
        try {
            if ("OPEN".equalsIgnoreCase(payload.action())) {
                if (payload.amount() <= 0 || Double.isNaN(payload.amount()) || Double.isInfinite(payload.amount())) {
                    sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("oms_invalid_amount"), true);
                    return;
                }
                var res = ExchangeMod.getMarketManager().executeOpenOMSPosition(
                        player.getUUID(), player.getName().getString(), payload.resourceId(), payload.amount()
                );
                sendMarketDataToClient(player, payload.resourceId(), (res.success() ? "§a" : "§c") + res.message(), !res.success());
            } else if ("CLOSE".equalsIgnoreCase(payload.action())) {
                if (Double.isNaN(payload.amount()) || Double.isInfinite(payload.amount())) {
                    sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("oms_invalid_volume"), true);
                    return;
                }
                var res = ExchangeMod.getMarketManager().executeCloseOMSPosition(
                        player.getUUID(), player.getName().getString(), payload.positionId(), payload.amount()
                );
                sendMarketDataToClient(player, payload.resourceId(), (res.success() ? "§a" : "§c") + res.message(), !res.success());
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to execute OMS operation", e);
            sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("oms_error", e.getMessage()), true);
        }
    }

    public static void handleLimitOrderOperation(ServerPlayer player, ServerboundLimitOrderPayload payload) {
        if (ExchangeMod.getMarketManager() == null || ExchangeMod.getMarketDAO() == null) return;
        try {
            if ("PLACE".equalsIgnoreCase(payload.action())) {
                if (payload.amount() <= 0 || payload.amount() > 2304 || payload.limitPrice() <= 0
                        || Double.isNaN(payload.limitPrice()) || Double.isInfinite(payload.limitPrice())) {
                    sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("limit_invalid_params"), true);
                    return;
                }

                boolean isSell = LimitOrder.TYPE_SELL.equalsIgnoreCase(payload.orderType());
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(payload.resourceId()));
                if (isSell) {
                    if (item == null) {
                        sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("limit_unknown_item"), true);
                        return;
                    }
                    int inInv = InventoryHelper.countPlayerItems(player, item);
                    if (inInv < payload.amount()) {
                        sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("limit_insufficient_items", inInv, payload.amount()), true);
                        return;
                    }
                    InventoryHelper.removePlayerItems(player, item, payload.amount());
                }

                var res = ExchangeMod.getMarketManager().placeLimitOrder(
                        player.getUUID(), player.getName().getString(), payload.resourceId(),
                        payload.orderType(), payload.amount(), payload.limitPrice()
                );
                if (res.success()) {
                    sendMarketDataToClient(player, payload.resourceId(), "§a" + res.message(), false);
                } else {
                    if (isSell && item != null) {
                        InventoryHelper.giveOrDropItems(player, item, payload.amount());
                    }
                    sendMarketDataToClient(player, payload.resourceId(), "§c" + res.message(), true);
                }
            } else if ("CANCEL".equalsIgnoreCase(payload.action())) {
                LimitOrder target = ExchangeMod.getMarketDAO().getLimitOrder(payload.orderId());
                var res = ExchangeMod.getMarketManager().cancelLimitOrder(player.getUUID(), payload.orderId());
                if (res.success()) {
                    if (target != null && LimitOrder.TYPE_SELL.equalsIgnoreCase(target.getOrderType())) {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(target.getResourceId()));
                        if (item != null) {
                            InventoryHelper.giveOrDropItems(player, item, target.getAmount());
                        }
                        sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("limit_sell_canceled", target.getAmount()), false);
                    } else {
                        sendMarketDataToClient(player, payload.resourceId(), "§a" + res.message(), false);
                    }
                } else {
                    sendMarketDataToClient(player, payload.resourceId(), "§c" + res.message(), true);
                }
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to execute Limit Order operation", e);
            sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("limit_error", e.getMessage()), true);
        }
    }

    public static void handleContractOperation(ServerPlayer player, ServerboundContractPayload payload) {
        if (ExchangeMod.getMarketManager() == null || ExchangeMod.getMarketDAO() == null) return;
        try {
            if ("ACCEPT".equalsIgnoreCase(payload.action())) {
                var res = ExchangeMod.getMarketManager().acceptContract(
                        player.getUUID(), player.getName().getString(), payload.contractId(), player.level().getGameTime()
                );
                var c = ExchangeMod.getMarketDAO().getContract(payload.contractId());
                String resId = c != null ? c.getResourceId() : "minecraft:iron_ingot";
                sendMarketDataToClient(player, resId, (res.success() ? "§a" : "§c") + res.message(), !res.success());
            } else if ("DELIVER".equalsIgnoreCase(payload.action())) {
                var contracts = ExchangeMod.getMarketDAO().getAvailableAndPlayerContracts(player.getUUID());
                com.exchange.mod.core.DeliveryContract target = null;
                for (var c : contracts) {
                    if (c.getContractId().equals(payload.contractId())) {
                        target = c;
                        break;
                    }
                }
                if (target == null) {
                    sendMarketDataToClient(player, "minecraft:iron_ingot", ExchangeLang.notify("contract_not_found"), true);
                    return;
                }

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(target.getResourceId()));
                if (item == null) {
                    sendMarketDataToClient(player, target.getResourceId(), ExchangeLang.notify("contract_unknown_item"), true);
                    return;
                }

                int inInv = InventoryHelper.countPlayerItems(player, item);
                int needed = target.getTargetAmount() - target.getDeliveredAmount();
                int toDeliver = Math.min(inInv, Math.min(payload.amount() > 0 ? payload.amount() : needed, needed));

                if (toDeliver <= 0) {
                    sendMarketDataToClient(player, target.getResourceId(), ExchangeLang.notify("contract_no_items"), true);
                    return;
                }

                var res = ExchangeMod.getMarketManager().deliverContractItems(
                        player.getUUID(), player.getName().getString(), payload.contractId(), toDeliver
                );
                if (res.success()) {
                    InventoryHelper.removePlayerItems(player, item, toDeliver);
                    sendMarketDataToClient(player, target.getResourceId(), "§a" + res.message(), false);
                } else {
                    sendMarketDataToClient(player, target.getResourceId(), "§c" + res.message(), true);
                }
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to execute Contract operation", e);
            sendMarketDataToClient(player, "minecraft:iron_ingot", ExchangeLang.notify("contract_error", e.getMessage()), true);
        }
    }

    public static void handleUpdateRedstoneSettings(ServerPlayer player, ServerboundUpdateRedstoneSettingsPayload payload) {
        try {
            PlayerAccount acc = ExchangeMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            if (payload.redstoneMode() > 0 && acc.getRepLevel() < 3) {
                sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("terminal.redstone_rank_req"), true);
                return;
            }

            BlockPos pos = payload.pos();
            if (pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                return;
            }

            if (player.level().getBlockEntity(pos) instanceof com.exchange.mod.blocks.ExchangeTerminalEntity terminal) {
                if (!player.getUUID().equals(terminal.getOwnerUuid()) && !player.hasPermissions(2)) {
                    sendMarketDataToClient(player, payload.resourceId(), ExchangeLang.notify("terminal.redstone_not_owner"), true);
                    return;
                }

                terminal.setRedstoneMode(payload.redstoneMode());
                terminal.setThresholdPrice(payload.thresholdPrice());
                terminal.setThresholdIsLessThan(payload.thresholdIsLessThan());
                terminal.setChanged();

                String modeName = switch (payload.redstoneMode()) {
                    case 1 -> ExchangeLang.guiStr("terminal.redstone_mode_price");
                    case 2 -> ExchangeLang.guiStr("terminal.redstone_mode_threshold", (payload.thresholdIsLessThan() ? "<" : ">"), String.format(java.util.Locale.US, "%.1f", payload.thresholdPrice()));
                    default -> ExchangeLang.guiStr("terminal.redstone_mode_stock");
                };
                sendMarketDataToClient(player, terminal.getMonitoredResource(), ExchangeLang.notify("terminal.redstone_saved", modeName), false);
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to update redstone settings", e);
        }
    }
}
