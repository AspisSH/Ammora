package com.exchange.mod.client;

import com.exchange.mod.client.gui.ColdWalletScreen;
import com.exchange.mod.client.gui.MarketplaceScreen;
import com.exchange.mod.client.gui.PlayerShopScreen;
import com.exchange.mod.client.gui.PurchaseDockScreen;
import com.exchange.mod.client.gui.TerminalScreen;
import com.exchange.mod.network.ColdWalletDataPayload;
import com.exchange.mod.network.MarketDataPayload;
import com.exchange.mod.network.MarketplaceDataPayload;
import com.exchange.mod.network.PlayerShopDataPayload;
import com.exchange.mod.network.PurchaseDockDataPayload;
import net.minecraft.client.Minecraft;

/**
 * Handles incoming client-side packets and GUI dispatch.
 */
public class ClientPacketHandler {

    public static void handleMarketData(MarketDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerminalScreen screen) {
            screen.updateMarketData(payload);
        } else {
            mc.setScreen(new TerminalScreen(payload));
        }
    }

    public static void handlePurchaseDockData(PurchaseDockDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PurchaseDockScreen screen) {
            screen.updateData(payload);
        } else {
            mc.setScreen(new PurchaseDockScreen(payload));
        }
    }

    public static void handleColdWalletData(ColdWalletDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ColdWalletScreen screen) {
            screen.updateData(payload);
        } else {
            mc.setScreen(new ColdWalletScreen(payload));
        }
    }

    public static void handlePlayerShopData(PlayerShopDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PlayerShopScreen screen) {
            screen.updateData(payload);
        } else {
            mc.setScreen(new PlayerShopScreen(payload));
        }
    }

    public static void handleMarketplaceData(MarketplaceDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MarketplaceScreen screen) {
            screen.updateData(payload);
        } else {
            mc.setScreen(new MarketplaceScreen(payload));
        }
    }

    public static void handleTradeSync(com.exchange.mod.network.ClientboundTradeSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.activeSession()) {
            if (mc.screen instanceof com.exchange.mod.client.gui.SecureTradeScreen screen) {
                screen.updateData(payload);
            } else {
                mc.setScreen(new com.exchange.mod.client.gui.SecureTradeScreen(payload));
            }
        } else {
            if (mc.screen instanceof com.exchange.mod.client.gui.SecureTradeScreen) {
                mc.setScreen(null);
            }
        }
    }

    private static java.util.UUID pendingTradeInviteUuid = null;
    private static String pendingTradeInviteName = null;
    private static long pendingTradeInviteExpiry = 0L;

    public static boolean hasActiveTradeInvite() {
        return pendingTradeInviteUuid != null && System.currentTimeMillis() < pendingTradeInviteExpiry;
    }

    public static java.util.UUID getPendingTradeInviteUuid() {
        return hasActiveTradeInvite() ? pendingTradeInviteUuid : null;
    }

    public static String getPendingTradeInviteName() {
        return hasActiveTradeInvite() ? pendingTradeInviteName : null;
    }

    public static void clearPendingTradeInvite() {
        pendingTradeInviteUuid = null;
        pendingTradeInviteName = null;
        pendingTradeInviteExpiry = 0L;
    }

    public static void handleTradeInvite(com.exchange.mod.network.ClientboundTradeInvitePayload payload) {
        pendingTradeInviteUuid = payload.senderUuid();
        pendingTradeInviteName = payload.senderName();
        pendingTradeInviteExpiry = System.currentTimeMillis() + 60000L;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "gui.exchange.wallet.p2p_invite_chat", payload.senderName()
            ));
        }
        if (mc.screen instanceof ColdWalletScreen screen) {
            screen.setIncomingTradeInvite(payload.senderUuid(), payload.senderName());
        }
    }
}
