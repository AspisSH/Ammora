package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.events.MarketEvent;
import com.ammora.mod.db.MarketTxRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Server-side network handler for the Operator Admin Panel.
 * Validates operator permissions (OP level 2+) and processes administrative operations.
 */
public class AdminPacketHandler {

    /**
     * Gathers all system data and opens/updates the Admin Panel GUI for the operator.
     */
    public static void openAdminScreen(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("command.ammora.admin.no_permission"));
            return;
        }
        sendAdminData(player, AmmoraLang.notify("admin.ready"), false);
    }

    /**
     * Handles administrative actions sent from the client.
     */
    public static void handleAdminAction(ServerPlayer player, ServerboundAdminActionPayload payload) {
        if (!player.hasPermissions(2)) {
            AmmoraMod.LOGGER.warn("Unauthorized admin action attempt by player: {}", player.getName().getString());
            return;
        }

        String action = payload.action();
        String targetId = payload.targetId();
        double numVal = payload.numericValue();
        String extra = payload.extraData();

        try {
            switch (action) {
                case "SET_BALANCE" -> {
                    UUID targetUuid = UUID.fromString(targetId);
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(targetUuid, extra.isEmpty() ? "Player" : extra);
                    double newBal = Math.max(0.0, numVal);
                    acc.setBalanceCbx(newBal);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    notifyTargetPlayerIfOnline(player.getServer(), targetUuid,
                            Component.translatable("message.ammora.admin.balance_set_notify", String.format(Locale.US, "%.2f", newBal)));
                    AmmoraMod.LOGGER.info("Admin {} set balance of {} to {} CBX", player.getName().getString(), acc.getPlayerName(), newBal);
                    sendAdminData(player, AmmoraLang.notify("admin.balance_set", acc.getPlayerName(), String.format(Locale.US, "%.2f", newBal)), false);
                }
                case "ADD_BALANCE" -> {
                    UUID targetUuid = UUID.fromString(targetId);
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(targetUuid, extra.isEmpty() ? "Player" : extra);
                    double addAmt = Math.max(0.0, numVal);
                    acc.deposit(addAmt);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    notifyTargetPlayerIfOnline(player.getServer(), targetUuid,
                            Component.translatable("message.ammora.admin.balance_add_notify", String.format(Locale.US, "%.2f", addAmt)));
                    AmmoraMod.LOGGER.info("Admin {} added {} CBX to {}", player.getName().getString(), addAmt, acc.getPlayerName());
                    sendAdminData(player, AmmoraLang.notify("admin.balance_added", String.format(Locale.US, "%.2f", addAmt), acc.getPlayerName()), false);
                }
                case "SUB_BALANCE" -> {
                    UUID targetUuid = UUID.fromString(targetId);
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(targetUuid, extra.isEmpty() ? "Player" : extra);
                    double subAmt = Math.max(0.0, numVal);
                    acc.withdraw(subAmt);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    notifyTargetPlayerIfOnline(player.getServer(), targetUuid,
                            Component.translatable("message.ammora.admin.balance_sub_notify", String.format(Locale.US, "%.2f", subAmt)));
                    AmmoraMod.LOGGER.info("Admin {} deducted {} CBX from {}", player.getName().getString(), subAmt, acc.getPlayerName());
                    sendAdminData(player, AmmoraLang.notify("admin.balance_subbed", String.format(Locale.US, "%.2f", subAmt), acc.getPlayerName()), false);
                }
                case "TRIGGER_EVENT" -> {
                    var eventMgr = AmmoraMod.getMarketEventManager();
                    if (eventMgr != null) {
                        MarketEvent template = eventMgr.getTemplatePool().stream()
                                .filter(t -> t.getId().equalsIgnoreCase(targetId))
                                .findFirst().orElse(null);
                        if (template != null) {
                            int durationDays = Math.max(1, (int) numVal);
                            MarketEvent newEvent = eventMgr.createEventFromTemplate(template, durationDays);
                            eventMgr.setActiveEvent(newEvent, AmmoraMod.getMarketManager());
                            if (player.getServer() != null) {
                                player.getServer().getPlayerList().broadcastSystemMessage(
                                        Component.translatable("message.ammora.admin.event_broadcast", newEvent.getTitle(), newEvent.getDescription()),
                                        false
                                );
                            }
                            AmmoraMod.LOGGER.info("Admin {} triggered market event: {} for {} days", player.getName().getString(), newEvent.getTitle(), durationDays);
                            sendAdminData(player, AmmoraLang.notify("admin.event_triggered", newEvent.getTitle(), String.valueOf(durationDays)), false);
                        } else {
                            sendAdminData(player, AmmoraLang.notify("admin.event_not_found"), true);
                        }
                    }
                }
                case "STOP_EVENT" -> {
                    var eventMgr = AmmoraMod.getMarketEventManager();
                    if (eventMgr != null && eventMgr.getActiveEvent() != null) {
                        String oldTitle = eventMgr.getActiveEvent().getTitle();
                        eventMgr.clearEventModifiers(AmmoraMod.getMarketManager());
                        eventMgr.setActiveEvent(null, AmmoraMod.getMarketManager());
                        if (player.getServer() != null) {
                            player.getServer().getPlayerList().broadcastSystemMessage(
                                    Component.translatable("message.ammora.admin.event_stopped_broadcast", oldTitle),
                                    false
                            );
                        }
                        AmmoraMod.LOGGER.info("Admin {} stopped active market event {}", player.getName().getString(), oldTitle);
                        sendAdminData(player, AmmoraLang.notify("admin.event_stopped", oldTitle), false);
                    } else {
                        sendAdminData(player, AmmoraLang.notify("admin.no_active_event"), false);
                    }
                }
                case "SET_BASE_PRICE" -> {
                    var marketMgr = AmmoraMod.getMarketManager();
                    if (marketMgr != null) {
                        MarketResource res = marketMgr.getResource(targetId);
                        if (res != null) {
                            double oldSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            res.setBasePrice(Math.max(0.01, numVal));
                            AmmoraMod.getMarketDAO().upsertMarket(res);
                            double newSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            try {
                                marketMgr.recordMarketEventCandle(res, oldSpot, newSpot);
                            } catch (Exception ignored) {}
                            AmmoraMod.LOGGER.info("Admin {} changed base price of {} to {}", player.getName().getString(), res.getDisplayName(), res.getBasePrice());
                            sendAdminData(player, AmmoraLang.notify("admin.base_price_set", res.getDisplayName(), String.format(Locale.US, "%.2f", res.getBasePrice())), false);
                        } else {
                            sendAdminData(player, AmmoraLang.notify("admin.resource_not_found", targetId), true);
                        }
                    }
                }
                case "SET_MODIFIER" -> {
                    var marketMgr = AmmoraMod.getMarketManager();
                    if (marketMgr != null) {
                        MarketResource res = marketMgr.getResource(targetId);
                        if (res != null) {
                            double oldSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            res.setDailyModifier(numVal);
                            AmmoraMod.getMarketDAO().upsertMarket(res);
                            double newSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            try {
                                marketMgr.recordMarketEventCandle(res, oldSpot, newSpot);
                            } catch (Exception ignored) {}
                            AmmoraMod.LOGGER.info("Admin {} set price modifier of {} to {}%", player.getName().getString(), res.getDisplayName(), numVal * 100.0);
                            sendAdminData(player, AmmoraLang.notify("admin.modifier_set", res.getDisplayName(), String.format(Locale.US, "%+.1f%%", numVal * 100.0)), false);
                        } else {
                            sendAdminData(player, AmmoraLang.notify("admin.resource_not_found", targetId), true);
                        }
                    }
                }
                case "SET_STOCK" -> {
                    var marketMgr = AmmoraMod.getMarketManager();
                    if (marketMgr != null) {
                        MarketResource res = marketMgr.getResource(targetId);
                        if (res != null) {
                            double oldSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            res.setCurrentStock(Math.max(1.0, numVal));
                            AmmoraMod.getMarketDAO().upsertMarket(res);
                            double newSpot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                            try {
                                marketMgr.recordMarketEventCandle(res, oldSpot, newSpot);
                            } catch (Exception ignored) {}
                            AmmoraMod.LOGGER.info("Admin {} set stock of {} to {}", player.getName().getString(), res.getDisplayName(), res.getCurrentStock());
                            sendAdminData(player, AmmoraLang.notify("admin.stock_set", res.getDisplayName(), String.valueOf((long) res.getCurrentStock())), false);
                        } else {
                            sendAdminData(player, AmmoraLang.notify("admin.resource_not_found", targetId), true);
                        }
                    }
                }
                case "SET_COMPANY_FEE" -> {
                    double fee = Math.max(0.0, MarketEngine.round2(numVal));
                    AmmoraMod.getMarketDAO().setCompanyRegistrationFee(fee);
                    AmmoraMod.LOGGER.info("Admin {} set company registration fee to {} CBX", player.getName().getString(), fee);
                    sendAdminData(player, AmmoraLang.notify("admin.company_fee_set", String.format(Locale.US, "%.2f", fee)), false);
                }
                case "REFRESH" -> {
                    sendAdminData(player, AmmoraLang.notify("admin.refreshed"), false);
                }
                default -> sendAdminData(player, AmmoraLang.notify("admin.unknown_action", action), true);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Error executing admin action " + action, e);
            sendAdminData(player, AmmoraLang.notify("admin.error", e.getMessage()), true);
        }
    }

    private static void sendAdminData(ServerPlayer player, String statusMsg, boolean isError) {
        try {
            // 1. Player Accounts
            List<ClientboundAdminDataPayload.AdminAccountItem> accounts = new ArrayList<>();
            for (PlayerAccount acc : AmmoraMod.getMarketDAO().getAllAccounts()) {
                accounts.add(new ClientboundAdminDataPayload.AdminAccountItem(
                        acc.getPlayerUuid(),
                        acc.getPlayerName(),
                        acc.getBalanceCbx(),
                        acc.getRepPoints(),
                        acc.getRepLevel(),
                        acc.getUpdatedAt()
                ));
            }

            // 2. Transactions Log
            List<ClientboundAdminDataPayload.AdminTxItem> transactions = new ArrayList<>();
            for (MarketTxRecord tx : AmmoraMod.getMarketDAO().getRecentMarketTransactions(150)) {
                transactions.add(new ClientboundAdminDataPayload.AdminTxItem(
                        tx.getTxId(),
                        tx.getTxType(),
                        tx.getBuyerName(),
                        tx.getSellerName(),
                        tx.getItemId(),
                        tx.getItemName(),
                        tx.getAmount(),
                        tx.getTotalCbx(),
                        tx.getFeeCbx(),
                        tx.getTimestamp()
                ));
            }

            // 3. Event Templates & Active Event
            List<ClientboundAdminDataPayload.AdminEventTemplateItem> eventTemplates = new ArrayList<>();
            String activeEventId = "";
            String activeEventTitle = "";
            String activeEventDesc = "";
            int activeEventRemainingDays = 0;
            double activeEventMultiplier = 0.0;

            var eventMgr = AmmoraMod.getMarketEventManager();
            if (eventMgr != null) {
                for (MarketEvent t : eventMgr.getTemplatePool()) {
                    eventTemplates.add(new ClientboundAdminDataPayload.AdminEventTemplateItem(
                            t.getId(),
                            t.getTitle(),
                            t.getDescription(),
                            t.getAffectedResourceId(),
                            t.getPriceMultiplier(),
                            t.getRemainingDays()
                    ));
                }
                MarketEvent active = eventMgr.getActiveEvent();
                if (active != null) {
                    activeEventId = active.getId();
                    activeEventTitle = active.getTitle();
                    activeEventDesc = active.getDescription();
                    activeEventRemainingDays = active.getRemainingDays();
                    activeEventMultiplier = active.getPriceMultiplier();
                }
            }

            // 4. Resources
            List<ClientboundAdminDataPayload.AdminResourceItem> resources = new ArrayList<>();
            var marketMgr = AmmoraMod.getMarketManager();
            if (marketMgr != null) {
                for (MarketResource mr : marketMgr.getAllResources()) {
                    double spot = MarketEngine.calculateSpotPrice(mr.getCurrentStock(), mr);
                    resources.add(new ClientboundAdminDataPayload.AdminResourceItem(
                            mr.getResourceId(),
                            mr.getDisplayName(),
                            spot,
                            mr.getBasePrice(),
                            mr.getCurrentStock(),
                            mr.getTargetReserve(),
                            mr.getElasticity(),
                            mr.getDailyModifier(),
                            mr.getEventModifier()
                    ));
                }
            }

            double companyRegistrationFee = AmmoraMod.getMarketDAO().getCompanyRegistrationFee();

            ClientboundAdminDataPayload payload = new ClientboundAdminDataPayload(
                    accounts,
                    transactions,
                    eventTemplates,
                    activeEventId,
                    activeEventTitle,
                    activeEventDesc,
                    activeEventRemainingDays,
                    activeEventMultiplier,
                    resources,
                    companyRegistrationFee,
                    statusMsg,
                    isError
            );

            PacketDistributor.sendToPlayer(player, payload);
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to gather admin data for player: " + player.getName().getString(), e);
            player.sendSystemMessage(Component.translatable("message.ammora.admin.err_fetch", e.getMessage()));
        }
    }

    private static void notifyTargetPlayerIfOnline(net.minecraft.server.MinecraftServer server, UUID targetUuid, Component msg) {
        if (server == null) return;
        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target != null) {
            target.sendSystemMessage(msg);
        }
    }
}
