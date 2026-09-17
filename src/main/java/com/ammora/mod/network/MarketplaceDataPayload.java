package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload sent from server to client to synchronize Global Marketplace data:
 * Active catalog slots, registered shops, buy requests (RFQ), community quests, and transaction history.
 */
public record MarketplaceDataPayload(
        double balanceCbx,
        int repLevel,
        List<MarketplaceSlotItem> catalogSlots,
        List<MarketplaceShopItem> shops,
        List<BuyRequestItem> buyRequests,
        List<CommunityQuestItem> quests,
        List<MarketTxItem> transactions,
        List<DeliveryBufferItem> deliveries,
        String statusMessage,
        boolean isError
) implements CustomPacketPayload {

    public MarketplaceDataPayload(
            double balanceCbx,
            int repLevel,
            List<MarketplaceSlotItem> catalogSlots,
            List<MarketplaceShopItem> shops,
            List<BuyRequestItem> buyRequests,
            List<CommunityQuestItem> quests,
            List<MarketTxItem> transactions,
            String statusMessage,
            boolean isError
    ) {
        this(balanceCbx, repLevel, catalogSlots, shops, buyRequests, quests, transactions, List.of(), statusMessage, isError);
    }

    @Deprecated
    public double balanceUsdt() {
        return balanceCbx();
    }

    public static final Type<MarketplaceDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "marketplace_data"));

    public static final StreamCodec<FriendlyByteBuf, MarketplaceDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            MarketplaceDataPayload::write,
            MarketplaceDataPayload::new
    );

    public record MarketplaceSlotItem(
            String shopId,
            String shopName,
            String ownerName,
            int slotIndex,
            String itemId,
            String displayName,
            double priceCbx,
            int stockCount,
            double deliveryFeeCbx,
            List<String> lore
    ) {
        @Deprecated public double priceUsdt() { return priceCbx(); }
        @Deprecated public double deliveryFeeUsdt() { return deliveryFeeCbx(); }
    }

    public record MarketplaceShopItem(
            String shopId,
            String shopName,
            UUID ownerUuid,
            String ownerName,
            String dimension,
            int posX,
            int posY,
            int posZ,
            int activeSlotsCount,
            int totalSales
    ) {}

    public record BuyRequestItem(
            String requestId,
            UUID buyerUuid,
            String buyerName,
            String itemId,
            String displayName,
            double unitPrice,
            int remainingAmount,
            double escrowCbx,
            boolean isOwn
    ) {
        @Deprecated public double escrowUsdt() { return escrowCbx(); }
    }

    public record CommunityQuestItem(
            String questId,
            UUID creatorUuid,
            String creatorName,
            String title,
            String description,
            double rewardCbx,
            String status,
            UUID workerUuid,
            String workerName,
            long createdAt,
            boolean isOwn,
            boolean isAssignedToMe
    ) {
        @Deprecated public double rewardUsdt() { return rewardCbx(); }
    }

    public record MarketTxItem(
            String txId,
            String txType,
            String buyerName,
            String sellerName,
            String itemName,
            int amount,
            double totalCbx,
            double feeCbx,
            long timestamp
    ) {
        @Deprecated public double totalUsdt() { return totalCbx(); }
        @Deprecated public double feeUsdt() { return feeCbx(); }
    }

    public record DeliveryBufferItem(
            String deliveryId,
            String itemId,
            String displayName,
            int amount,
            long timestamp,
            String itemNbt
    ) {}

    public MarketplaceDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readDouble(),
                buf.readVarInt(),
                readCatalog(buf),
                readShops(buf),
                readBuyRequests(buf),
                readQuests(buf),
                readTx(buf),
                readDeliveries(buf),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    private static List<MarketplaceSlotItem> readCatalog(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MarketplaceSlotItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String shopId = buf.readUtf();
            String shopName = buf.readUtf();
            String ownerName = buf.readUtf();
            int slotIndex = buf.readVarInt();
            String itemId = buf.readUtf();
            String displayName = buf.readUtf();
            double priceCbx = buf.readDouble();
            int stockCount = buf.readVarInt();
            double deliveryFeeCbx = buf.readDouble();
            int loreSize = buf.readVarInt();
            List<String> lore = new ArrayList<>(loreSize);
            for (int j = 0; j < loreSize; j++) {
                lore.add(buf.readUtf());
            }
            list.add(new MarketplaceSlotItem(shopId, shopName, ownerName, slotIndex, itemId, displayName, priceCbx, stockCount, deliveryFeeCbx, lore));
        }
        return list;
    }

    private static List<MarketplaceShopItem> readShops(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MarketplaceShopItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new MarketplaceShopItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }
        return list;
    }

    private static List<BuyRequestItem> readBuyRequests(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BuyRequestItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new BuyRequestItem(
                    buf.readUtf(),
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readBoolean()
            ));
        }
        return list;
    }

    private static List<CommunityQuestItem> readQuests(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<CommunityQuestItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String qId = buf.readUtf();
            UUID creatorUuid = buf.readUUID();
            String creatorName = buf.readUtf();
            String title = buf.readUtf();
            String description = buf.readUtf();
            double rewardCbx = buf.readDouble();
            String status = buf.readUtf();
            boolean hasWorker = buf.readBoolean();
            UUID workerUuid = hasWorker ? buf.readUUID() : null;
            String workerName = buf.readUtf();
            long createdAt = buf.readLong();
            boolean isOwn = buf.readBoolean();
            boolean isAssignedToMe = buf.readBoolean();
            list.add(new CommunityQuestItem(qId, creatorUuid, creatorName, title, description, rewardCbx, status, workerUuid, workerName, createdAt, isOwn, isAssignedToMe));
        }
        return list;
    }

    private static List<MarketTxItem> readTx(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MarketTxItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new MarketTxItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readLong()
            ));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(balanceCbx);
        buf.writeVarInt(repLevel);

        buf.writeVarInt(catalogSlots.size());
        for (MarketplaceSlotItem item : catalogSlots) {
            buf.writeUtf(item.shopId());
            buf.writeUtf(item.shopName());
            buf.writeUtf(item.ownerName());
            buf.writeVarInt(item.slotIndex());
            buf.writeUtf(item.itemId());
            buf.writeUtf(item.displayName());
            buf.writeDouble(item.priceCbx());
            buf.writeVarInt(item.stockCount());
            buf.writeDouble(item.deliveryFeeCbx());
            buf.writeVarInt(item.lore().size());
            for (String line : item.lore()) {
                buf.writeUtf(line);
            }
        }

        buf.writeVarInt(shops.size());
        for (MarketplaceShopItem shop : shops) {
            buf.writeUtf(shop.shopId());
            buf.writeUtf(shop.shopName());
            buf.writeUUID(shop.ownerUuid());
            buf.writeUtf(shop.ownerName());
            buf.writeUtf(shop.dimension());
            buf.writeVarInt(shop.posX());
            buf.writeVarInt(shop.posY());
            buf.writeVarInt(shop.posZ());
            buf.writeVarInt(shop.activeSlotsCount());
            buf.writeVarInt(shop.totalSales());
        }

        buf.writeVarInt(buyRequests.size());
        for (BuyRequestItem req : buyRequests) {
            buf.writeUtf(req.requestId());
            buf.writeUUID(req.buyerUuid());
            buf.writeUtf(req.buyerName());
            buf.writeUtf(req.itemId());
            buf.writeUtf(req.displayName());
            buf.writeDouble(req.unitPrice());
            buf.writeVarInt(req.remainingAmount());
            buf.writeDouble(req.escrowCbx());
            buf.writeBoolean(req.isOwn());
        }

        buf.writeVarInt(quests.size());
        for (CommunityQuestItem q : quests) {
            buf.writeUtf(q.questId());
            buf.writeUUID(q.creatorUuid());
            buf.writeUtf(q.creatorName());
            buf.writeUtf(q.title());
            buf.writeUtf(q.description());
            buf.writeDouble(q.rewardCbx());
            buf.writeUtf(q.status());
            buf.writeBoolean(q.workerUuid() != null);
            if (q.workerUuid() != null) {
                buf.writeUUID(q.workerUuid());
            }
            buf.writeUtf(q.workerName() != null ? q.workerName() : "");
            buf.writeLong(q.createdAt());
            buf.writeBoolean(q.isOwn());
            buf.writeBoolean(q.isAssignedToMe());
        }

        buf.writeVarInt(transactions.size());
        for (MarketTxItem tx : transactions) {
            buf.writeUtf(tx.txId());
            buf.writeUtf(tx.txType());
            buf.writeUtf(tx.buyerName());
            buf.writeUtf(tx.sellerName());
            buf.writeUtf(tx.itemName());
            buf.writeVarInt(tx.amount());
            buf.writeDouble(tx.totalCbx());
            buf.writeDouble(tx.feeCbx());
            buf.writeLong(tx.timestamp());
        }

        buf.writeVarInt(deliveries != null ? deliveries.size() : 0);
        if (deliveries != null) {
            for (DeliveryBufferItem d : deliveries) {
                buf.writeUtf(d.deliveryId());
                buf.writeUtf(d.itemId());
                buf.writeUtf(d.displayName());
                buf.writeVarInt(d.amount());
                buf.writeLong(d.timestamp());
                buf.writeUtf(d.itemNbt() != null ? d.itemNbt() : "");
            }
        }

        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
    }

    private static List<DeliveryBufferItem> readDeliveries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DeliveryBufferItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new DeliveryBufferItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readLong(),
                    buf.readUtf()
            ));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
