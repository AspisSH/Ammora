package com.ammora.mod.client;

import com.ammora.mod.client.gui.ColdWalletScreen;
import com.ammora.mod.client.gui.MarketplaceScreen;
import com.ammora.mod.client.gui.PlayerShopScreen;
import com.ammora.mod.client.gui.PurchaseDockScreen;
import com.ammora.mod.client.gui.TerminalScreen;
import com.ammora.mod.network.ColdWalletDataPayload;
import com.ammora.mod.network.MarketDataPayload;
import com.ammora.mod.network.MarketplaceDataPayload;
import com.ammora.mod.network.PlayerShopDataPayload;
import com.ammora.mod.network.PurchaseDockDataPayload;
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

    public static void handleAdminData(com.ammora.mod.network.ClientboundAdminDataPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.ammora.mod.client.gui.AdminScreen screen) {
            screen.updateData(payload);
        } else {
            mc.setScreen(new com.ammora.mod.client.gui.AdminScreen(payload));
        }
    }

    public static void handleTradeSync(com.ammora.mod.network.ClientboundTradeSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.activeSession()) {
            if (mc.screen instanceof com.ammora.mod.client.gui.SecureTradeScreen screen) {
                screen.updateData(payload);
            } else {
                mc.setScreen(new com.ammora.mod.client.gui.SecureTradeScreen(payload));
            }
        } else {
            if (mc.screen instanceof com.ammora.mod.client.gui.SecureTradeScreen) {
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

    public static void handleTradeInvite(com.ammora.mod.network.ClientboundTradeInvitePayload payload) {
        pendingTradeInviteUuid = payload.senderUuid();
        pendingTradeInviteName = payload.senderName();
        pendingTradeInviteExpiry = System.currentTimeMillis() + 60000L;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ColdWalletScreen screen) {
            screen.setIncomingTradeInvite(payload.senderUuid(), payload.senderName());
        }
    }

    public static void handleCompanyInvite(com.ammora.mod.network.ClientboundCompanyInvitePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new com.ammora.mod.client.gui.CompanyInviteScreen(payload.companyId(), payload.companyName(), payload.inviterName()));
    }
}
