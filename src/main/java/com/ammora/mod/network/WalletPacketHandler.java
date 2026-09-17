package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.LedgerEntry;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.TradeSessionManager;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles cold wallet balance queries, peer-to-peer (P2P) transfers,
 * transaction ledger history, and purchase dock automation configuration.
 */
public final class WalletPacketHandler {

    private WalletPacketHandler() {}

    public static void sendColdWalletData(ServerPlayer player, String statusMsg, boolean isError) {
        if (AmmoraMod.getMarketManager() == null || AmmoraMod.getMarketDAO() == null) {
            return;
        }

        try {
            PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());

            // Scan nearby players within 30 blocks radius
            AABB scanArea = player.getBoundingBox().inflate(30.0);
            List<ServerPlayer> nearbyEntities = player.serverLevel().getEntitiesOfClass(ServerPlayer.class, scanArea);
            List<ColdWalletDataPayload.NearbyPlayerItem> nearbyPlayers = new ArrayList<>();
            for (ServerPlayer other : nearbyEntities) {
                if (!other.getUUID().equals(player.getUUID())) {
                    double dist = Math.round(player.distanceTo(other) * 10.0) / 10.0;
                    nearbyPlayers.add(new ColdWalletDataPayload.NearbyPlayerItem(other.getUUID(), other.getName().getString(), dist));
                }
            }
            nearbyPlayers.sort((a, b) -> Double.compare(a.distance(), b.distance()));

            // Fetch persistent ledger
            List<LedgerEntry> rawLedger = AmmoraMod.getMarketDAO().getPlayerLedger(player.getUUID(), 30);
            List<ColdWalletDataPayload.LedgerItem> ledgerItems = new ArrayList<>();
            for (LedgerEntry entry : rawLedger) {
                ledgerItems.add(new ColdWalletDataPayload.LedgerItem(
                        entry.getType(),
                        entry.getTitle(),
                        MarketEngine.round2(entry.getAmountCbx()),
                        entry.getTimestamp()
                ));
            }

            ColdWalletDataPayload payload = new ColdWalletDataPayload(
                    MarketEngine.round2(acc.getBalanceCbx()),
                    acc.getRepLevel(),
                    acc.getRepPoints(),
                    nearbyPlayers,
                    ledgerItems,
                    statusMsg,
                    isError
            );
            PacketDistributor.sendToPlayer(player, payload);

            var pending = TradeSessionManager.getInstance().getPendingInvite(player.getUUID());
            if (pending != null) {
                PacketDistributor.sendToPlayer(player, new ClientboundTradeInvitePayload(pending.senderUuid(), pending.senderName()));
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to send cold wallet data", e);
        }
    }

    public static void handleP2PTransfer(ServerPlayer player, ServerboundP2PTransferPayload payload) {
        if (AmmoraMod.getMarketManager() == null || AmmoraMod.getMarketDAO() == null) return;

        double amount = payload.amount();
        if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            sendColdWalletData(player, "key:wallet.notif_err_amount", true);
            return;
        }

        ServerPlayer targetPlayer = null;
        UUID targetUuid = payload.targetPlayerUuid();
        if (targetUuid != null && !targetUuid.equals(new UUID(0L, 0L))) {
            targetPlayer = player.server.getPlayerList().getPlayer(targetUuid);
        }
        if (targetPlayer == null && payload.targetPlayerName() != null && !payload.targetPlayerName().trim().isEmpty()) {
            targetPlayer = player.server.getPlayerList().getPlayerByName(payload.targetPlayerName().trim());
        }

        if (targetPlayer == null) {
            String targetName = (payload.targetPlayerName() != null && !payload.targetPlayerName().trim().isEmpty())
                    ? payload.targetPlayerName().trim()
                    : "Recipient";
            sendColdWalletData(player, "key:wallet.notif_err_player_not_found;" + targetName, true);
            return;
        }

        if (targetPlayer.getUUID().equals(player.getUUID())) {
            sendColdWalletData(player, "key:wallet.notif_err_self", true);
            return;
        }

        if (player.distanceTo(targetPlayer) > 30.0) {
            sendColdWalletData(player, "key:wallet.notif_err_too_far;" + targetPlayer.getName().getString(), true);
            return;
        }

        try {
            var result = AmmoraMod.getMarketManager().executeP2PTransfer(
                    player.getUUID(), player.getName().getString(),
                    targetPlayer.getUUID(), targetPlayer.getName().getString(),
                    amount
            );

            if (result.success()) {
                String amtStr = MarketEngine.round2(amount) + " CBX";
                player.sendSystemMessage(Component.translatable("message.ammora.wallet.transfer_sent", amtStr, targetPlayer.getName().getString()));
                targetPlayer.sendSystemMessage(Component.translatable("message.ammora.wallet.transfer_received", amtStr, player.getName().getString()));

                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                targetPlayer.level().playSound(null, targetPlayer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);

                sendColdWalletData(player, "key:wallet.notif_transfer_sent;" + amtStr, false);
            } else {
                sendColdWalletData(player, "§c" + result.message(), true);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to execute P2P transfer", e);
            sendColdWalletData(player, "§cError: " + e.getMessage(), true);
        }
    }

    public static void sendPurchaseDockData(ServerPlayer player, BlockPos pos, String statusMsg, boolean isError) {
        if (AmmoraMod.getMarketManager() == null || AmmoraMod.getMarketDAO() == null) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof com.ammora.mod.blocks.PurchaseDockEntity dock)) {
            return;
        }

        try {
            PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            String targetId = dock.getTargetResourceId();
            MarketResource res = AmmoraMod.getMarketManager().getResource(targetId);
            if (res == null) {
                res = AmmoraMod.getMarketManager().getResource("minecraft:iron_ingot");
                if (res != null) {
                    targetId = res.getResourceId();
                }
            }
            double spot = res != null ? MarketEngine.round2(MarketEngine.calculateSpotPrice(res.getCurrentStock(), res)) : 0.0;
            String displayName = res != null ? res.getDisplayName() : targetId;

            List<PurchaseDockDataPayload.DockResourceItem> resources = new ArrayList<>();
            for (MarketResource mr : AmmoraMod.getMarketManager().getAllResources()) {
                double s = MarketEngine.round2(MarketEngine.calculateSpotPrice(mr.getCurrentStock(), mr));
                resources.add(new PurchaseDockDataPayload.DockResourceItem(mr.getResourceId(), mr.getDisplayName(), s));
            }

            PurchaseDockDataPayload payload = new PurchaseDockDataPayload(
                    pos,
                    targetId,
                    displayName,
                    spot,
                    dock.getBatchSize(),
                    dock.getMaxBuyPrice(),
                    acc.getRepLevel(),
                    acc.getRepPoints(),
                    MarketEngine.round2(acc.getBalanceCbx()),
                    dock.getOwnerName(),
                    resources,
                    statusMsg,
                    isError
            );
            PacketDistributor.sendToPlayer(player, payload);
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to send purchase dock data", e);
        }
    }

    public static void handleUpdatePurchaseDock(ServerPlayer player, ServerboundUpdatePurchaseDockPayload payload) {
        if (AmmoraMod.getMarketManager() == null || AmmoraMod.getMarketDAO() == null) return;
        BlockPos pos = payload.pos();
        if (pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof com.ammora.mod.blocks.PurchaseDockEntity dock)) {
            return;
        }

        try {
            PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());

            // Check owner permission: if dock has an owner and it's not the current player (and player is not op)
            if (dock.getOwnerUuid() != null && !player.getUUID().equals(dock.getOwnerUuid()) && !player.hasPermissions(2)) {
                sendPurchaseDockData(player, pos, AmmoraLang.notify("dock_owned_by", dock.getOwnerName()), true);
                return;
            }

            // Verify resource exists
            String resId = payload.targetResourceId();
            if (resId != null && !resId.isEmpty() && AmmoraMod.getMarketManager().getResource(resId) != null) {
                dock.setTargetResourceId(resId);
            }

            // Check rank for batch size:
            // Rank I: 1
            // Rank II (Trader): 4, 8, 16
            // Rank IV (Investor): 32, 64
            int requestedBatch = payload.batchSize();
            if (requestedBatch > 16 && acc.getRepLevel() < 4) {
                sendPurchaseDockData(player, pos, AmmoraLang.notify("dock.rank_investor_req"), true);
                return;
            } else if (requestedBatch > 1 && acc.getRepLevel() < 2) {
                sendPurchaseDockData(player, pos, AmmoraLang.notify("dock.rank_trader_req"), true);
                return;
            } else if (requestedBatch >= 1 && requestedBatch <= 64) {
                dock.setBatchSize(requestedBatch);
            }

            // Check rank for Stop-High max price limit:
            // Rank III (Broker): Configurable Stop-High price guard
            if (payload.maxBuyPrice() > 0 && !Double.isNaN(payload.maxBuyPrice()) && !Double.isInfinite(payload.maxBuyPrice())) {
                if (acc.getRepLevel() < 3) {
                    // Lower ranks cannot adjust stop-high guard
                    if (Math.abs(payload.maxBuyPrice() - dock.getMaxBuyPrice()) > 0.01) {
                        sendPurchaseDockData(player, pos, AmmoraLang.notify("dock.rank_broker_req"), true);
                        return;
                    }
                } else {
                    dock.setMaxBuyPrice(MarketEngine.round2(payload.maxBuyPrice()));
                }
            }

            dock.setChanged();
            sendPurchaseDockData(player, pos, AmmoraLang.notify("dock_settings_saved"), false);
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to update purchase dock", e);
            sendPurchaseDockData(player, pos, AmmoraLang.notify("dock_error", e.getMessage()), true);
        }
    }
}
