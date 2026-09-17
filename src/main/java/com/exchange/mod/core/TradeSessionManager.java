package com.exchange.mod.core;

import com.exchange.mod.network.ClientboundTradeInvitePayload;
import com.exchange.mod.network.ServerboundTradeActionPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for active P2P trade sessions and pending invites.
 */
public class TradeSessionManager {

    private static final TradeSessionManager INSTANCE = new TradeSessionManager();
    public static TradeSessionManager getInstance() { return INSTANCE; }

    public record TradeInvite(UUID senderUuid, String senderName, long timestamp) {}

    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToSession = new ConcurrentHashMap<>();
    private final Map<UUID, TradeInvite> pendingInvites = new ConcurrentHashMap<>();

    private TradeSessionManager() {}

    public TradeInvite getPendingInvite(UUID targetUuid) {
        TradeInvite invite = pendingInvites.get(targetUuid);
        if (invite != null && (System.currentTimeMillis() - invite.timestamp() <= 60000L)) {
            return invite;
        }
        if (invite != null) {
            pendingInvites.remove(targetUuid);
        }
        return null;
    }

    public void sendInvite(ServerPlayer sender, UUID targetUuid) {
        MinecraftServer server = sender.server;
        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);

        if (target == null) {
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.player_not_found"));
            return;
        }

        if (sender.getUUID().equals(targetUuid)) {
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.cannot_trade_self"));
            return;
        }

        if (sender.distanceToSqr(target) > 900.0) { // 30 blocks radius
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.too_far"));
            return;
        }

        if (playerToSession.containsKey(sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.already_in_session"));
            return;
        }

        if (playerToSession.containsKey(targetUuid)) {
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.target_busy", target.getName().getString()));
            return;
        }

        // Store invite valid for 60 seconds
        pendingInvites.put(targetUuid, new TradeInvite(sender.getUUID(), sender.getName().getString(), System.currentTimeMillis()));

        sender.sendSystemMessage(Component.translatable("message.exchange.trade.invite_sent", target.getName().getString()));
        target.sendSystemMessage(Component.translatable("message.exchange.trade.invite_received", sender.getName().getString()));
        target.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);

        PacketDistributor.sendToPlayer(target, new ClientboundTradeInvitePayload(sender.getUUID(), sender.getName().getString()));
    }

    public void respondInvite(ServerPlayer responder, UUID senderUuid, boolean accept) {
        TradeInvite invite = pendingInvites.get(responder.getUUID());
        if (invite == null || !invite.senderUuid().equals(senderUuid) || (System.currentTimeMillis() - invite.timestamp() > 60000L)) {
            responder.sendSystemMessage(Component.translatable("message.exchange.trade.invite_expired"));
            pendingInvites.remove(responder.getUUID());
            return;
        }

        pendingInvites.remove(responder.getUUID());
        ServerPlayer sender = responder.server.getPlayerList().getPlayer(senderUuid);

        if (sender == null) {
            responder.sendSystemMessage(Component.translatable("message.exchange.trade.sender_left"));
            return;
        }

        if (!accept) {
            sender.sendSystemMessage(Component.translatable("message.exchange.trade.target_declined", responder.getName().getString()));
            responder.sendSystemMessage(Component.translatable("message.exchange.trade.you_declined"));
            return;
        }

        // Start session
        UUID sessionId = UUID.randomUUID();
        TradeSession session = new TradeSession(sessionId, sender, responder);
        activeSessions.put(sessionId, session);
        playerToSession.put(sender.getUUID(), sessionId);
        playerToSession.put(responder.getUUID(), sessionId);

        sender.sendSystemMessage(Component.translatable("message.exchange.trade.session_opened"));
        responder.sendSystemMessage(Component.translatable("message.exchange.trade.session_opened"));

        session.syncBoth("key:trade.status_session_opened", false);
    }

    public void handleAction(ServerPlayer player, ServerboundTradeActionPayload payload) {
        UUID sessionId = playerToSession.get(player.getUUID());
        if (sessionId == null) return;
        TradeSession session = activeSessions.get(sessionId);
        if (session == null || session.isFinished()) {
            playerToSession.remove(player.getUUID());
            return;
        }

        String action = payload.action();
        if ("SET_MONEY".equalsIgnoreCase(action)) {
            session.setMoney(player, payload.amount());
        } else if ("OFFER_ITEM".equalsIgnoreCase(action)) {
            session.offerItem(player, payload.slotIndex(), (int) payload.amount());
        } else if ("REMOVE_ITEM".equalsIgnoreCase(action)) {
            session.removeItem(player, payload.slotIndex(), (int) payload.amount());
        } else if ("TOGGLE_LOCK".equalsIgnoreCase(action)) {
            session.toggleLock(player);
        } else if ("CONFIRM".equalsIgnoreCase(action)) {
            session.confirmTrade(player);
        } else if ("CANCEL".equalsIgnoreCase(action)) {
            session.cancel(player.server, "key:trade.cancelled_by;" + player.getName().getString());
            cleanupSession(sessionId, session);
        }

        if (session.isFinished()) {
            cleanupSession(sessionId, session);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        UUID sessionId = playerToSession.get(player.getUUID());
        if (sessionId != null) {
            TradeSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.cancel(player.server, "key:trade.cancel_player_disconnected");
                cleanupSession(sessionId, session);
            }
        }
    }

    private void cleanupSession(UUID sessionId, TradeSession session) {
        activeSessions.remove(sessionId);
        playerToSession.remove(session.getPlayerAUuid());
        playerToSession.remove(session.getPlayerBUuid());
    }
}
