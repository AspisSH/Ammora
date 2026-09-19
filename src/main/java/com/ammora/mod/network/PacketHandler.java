package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.client.ClientPacketHandler;
import com.ammora.mod.core.TradeSessionManager;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Central registrar and payload router for NeoForge 1.21.1 network communications.
 * Dispatches domain payloads to specialized handlers:
 * <ul>
 *     <li>{@link OrderPacketHandler} - Trading orders, OMS derivatives, limit orders, contracts, redstone</li>
 *     <li>{@link ShopPacketHandler} - Player shops, vending machine configurations, remote/local purchases</li>
 *     <li>{@link EscrowPacketHandler} - Marketplace catalog, RFQ buy requests, quests, delivery buffer</li>
 *     <li>{@link WalletPacketHandler} - Cold wallet balances, ledger queries, P2P transfers, purchase docks</li>
 *     <li>{@link TradeSessionManager} - Direct player-to-player trade GUI state sync</li>
 * </ul>
 */
public class PacketHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        // ----------------------------------------------------
        // Server to Client Payloads
        // ----------------------------------------------------
        registrar.playToClient(
                MarketDataPayload.TYPE,
                MarketDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleMarketData(payload))
        );

        registrar.playToClient(
                PurchaseDockDataPayload.TYPE,
                PurchaseDockDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handlePurchaseDockData(payload))
        );

        registrar.playToClient(
                ColdWalletDataPayload.TYPE,
                ColdWalletDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleColdWalletData(payload))
        );

        registrar.playToClient(
                PlayerShopDataPayload.TYPE,
                PlayerShopDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handlePlayerShopData(payload))
        );

        registrar.playToClient(
                MarketplaceDataPayload.TYPE,
                MarketplaceDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleMarketplaceData(payload))
        );

        registrar.playToClient(
                ClientboundTradeInvitePayload.TYPE,
                ClientboundTradeInvitePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleTradeInvite(payload))
        );

        registrar.playToClient(
                ClientboundTradeSyncPayload.TYPE,
                ClientboundTradeSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleTradeSync(payload))
        );

        registrar.playToClient(
                ClientboundAdminDataPayload.TYPE,
                ClientboundAdminDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleAdminData(payload))
        );

        // ----------------------------------------------------
        // Client to Server: Order & Market Terminal Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundExecuteOrderPayload.TYPE,
                ServerboundExecuteOrderPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleOrderExecution(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundSelectResourcePayload.TYPE,
                ServerboundSelectResourcePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        if (payload.pinForRedstone()) {
                            OrderPacketHandler.pinTerminalResource(serverPlayer, payload.resourceId());
                            var mr = AmmoraMod.getMarketManager() != null ? AmmoraMod.getMarketManager().getResource(payload.resourceId()) : null;
                            String name = mr != null ? mr.getDisplayName() : payload.resourceId();
                            OrderPacketHandler.sendMarketDataToClient(serverPlayer, payload.resourceId(), AmmoraLang.notify("terminal.redstone_pinned", name, 15), false);
                        } else {
                            OrderPacketHandler.sendMarketDataToClient(serverPlayer, payload.resourceId(), "", false);
                        }
                    }
                })
        );

        registrar.playToServer(
                ServerboundUpdateRedstoneSettingsPayload.TYPE,
                ServerboundUpdateRedstoneSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleUpdateRedstoneSettings(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundOMSPayload.TYPE,
                ServerboundOMSPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleOMSOperation(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundLimitOrderPayload.TYPE,
                ServerboundLimitOrderPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleLimitOrderOperation(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundContractPayload.TYPE,
                ServerboundContractPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleContractOperation(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundUnlockResourcePayload.TYPE,
                ServerboundUnlockResourcePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        OrderPacketHandler.handleUnlockResource(serverPlayer, payload);
                    }
                })
        );

        // ----------------------------------------------------
        // Client to Server: Wallet & Purchase Dock Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundUpdatePurchaseDockPayload.TYPE,
                ServerboundUpdatePurchaseDockPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        WalletPacketHandler.handleUpdatePurchaseDock(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundP2PTransferPayload.TYPE,
                ServerboundP2PTransferPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        WalletPacketHandler.handleP2PTransfer(serverPlayer, payload);
                    }
                })
        );

        // ----------------------------------------------------
        // Client to Server: Player Shop Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundConfigureShopPayload.TYPE,
                ServerboundConfigureShopPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        ShopPacketHandler.handleConfigureShop(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundShopPurchasePayload.TYPE,
                ServerboundShopPurchasePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        ShopPacketHandler.handleShopPurchase(serverPlayer, payload);
                    }
                })
        );

        // ----------------------------------------------------
        // Client to Server: Escrow & Marketplace Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundBuyRequestPayload.TYPE,
                ServerboundBuyRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        EscrowPacketHandler.handleBuyRequestAction(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundCommunityQuestPayload.TYPE,
                ServerboundCommunityQuestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        EscrowPacketHandler.handleCommunityQuestAction(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundClaimDeliveryPayload.TYPE,
                ServerboundClaimDeliveryPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        EscrowPacketHandler.handleClaimDelivery(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                ServerboundAuctionActionPayload.TYPE,
                ServerboundAuctionActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        AuctionPacketHandler.handleAuctionAction(serverPlayer, payload);
                    }
                })
        );

        // ----------------------------------------------------
        // Client to Server: P2P Direct Trade Session Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundTradeInvitePayload.TYPE,
                ServerboundTradeInvitePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        TradeSessionManager.getInstance().sendInvite(serverPlayer, payload.targetPlayerUuid());
                    }
                })
        );

        registrar.playToServer(
                ServerboundTradeInviteResponsePayload.TYPE,
                ServerboundTradeInviteResponsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        TradeSessionManager.getInstance().respondInvite(serverPlayer, payload.senderUuid(), payload.accept());
                    }
                })
        );

        registrar.playToServer(
                ServerboundTradeActionPayload.TYPE,
                ServerboundTradeActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        TradeSessionManager.getInstance().handleAction(serverPlayer, payload);
                    }
                })
        );

        // ----------------------------------------------------
        // Client to Server: Operator Admin Domain
        // ----------------------------------------------------
        registrar.playToServer(
                ServerboundAdminActionPayload.TYPE,
                ServerboundAdminActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        AdminPacketHandler.handleAdminAction(serverPlayer, payload);
                    }
                })
        );
    }

    // ----------------------------------------------------
    // Public Delegation API (Preserving full compatibility)
    // ----------------------------------------------------

    public static void sendMarketDataToClient(ServerPlayer player) {
        OrderPacketHandler.sendMarketDataToClient(player);
    }

    public static void sendMarketDataToClient(ServerPlayer player, String statusMsg, boolean isError) {
        OrderPacketHandler.sendMarketDataToClient(player, statusMsg, isError);
    }

    public static void sendMarketDataToClient(ServerPlayer player, String targetResourceId, String statusMsg, boolean isError) {
        OrderPacketHandler.sendMarketDataToClient(player, targetResourceId, statusMsg, isError);
    }

    public static void sendPurchaseDockData(ServerPlayer player, BlockPos pos, String statusMsg, boolean isError) {
        WalletPacketHandler.sendPurchaseDockData(player, pos, statusMsg, isError);
    }

    public static void sendColdWalletData(ServerPlayer player, String statusMsg, boolean isError) {
        WalletPacketHandler.sendColdWalletData(player, statusMsg, isError);
    }

    public static void sendMarketplaceData(ServerPlayer player, String statusMsg, boolean isError) {
        EscrowPacketHandler.sendMarketplaceData(player, statusMsg, isError);
    }

    public static boolean canPlayerHoldItem(Inventory inv, ItemStack target, int count) {
        return InventoryHelper.canPlayerHoldItem(inv, target, count);
    }
}
