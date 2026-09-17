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
 * Payload sent from server to client to synchronize a specific Player Shop (Vending Machine).
 * Includes slot contents, revenues, and upgrade tiers (maxSlots, slotCapacity, networkUnlocked).
 */
public record PlayerShopDataPayload(
        String shopId,
        String shopName,
        UUID ownerUuid,
        String ownerName,
        boolean isOwner,
        boolean isBroadcast,
        double accumulatedRevenue,
        int totalSales,
        double buyerBalanceCbx,
        List<ShopSlotItem> slots,
        int maxSlots,
        int slotCapacity,
        boolean networkUnlocked,
        String statusMessage,
        boolean isError
) implements CustomPacketPayload {

    public static final Type<PlayerShopDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "player_shop_data"));

    public static final StreamCodec<FriendlyByteBuf, PlayerShopDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            PlayerShopDataPayload::write,
            PlayerShopDataPayload::new
    );

    public record ShopSlotItem(
            int slotIndex,
            String itemId,
            String displayName,
            double priceCbx,
            int stockCount,
            List<String> lore
    ) {
        @Deprecated
        public double priceUsdt() {
            return priceCbx();
        }
    }

    @Deprecated
    public double buyerBalanceUsdt() {
        return buyerBalanceCbx();
    }

    public PlayerShopDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUUID(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(),
                readSlots(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    private static List<ShopSlotItem> readSlots(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ShopSlotItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int slotIndex = buf.readVarInt();
            String itemId = buf.readUtf();
            String displayName = buf.readUtf();
            double priceCbx = buf.readDouble();
            int stockCount = buf.readVarInt();
            int loreSize = buf.readVarInt();
            List<String> lore = new ArrayList<>(loreSize);
            for (int j = 0; j < loreSize; j++) {
                lore.add(buf.readUtf());
            }
            list.add(new ShopSlotItem(slotIndex, itemId, displayName, priceCbx, stockCount, lore));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(shopId);
        buf.writeUtf(shopName);
        buf.writeUUID(ownerUuid);
        buf.writeUtf(ownerName);
        buf.writeBoolean(isOwner);
        buf.writeBoolean(isBroadcast);
        buf.writeDouble(accumulatedRevenue);
        buf.writeVarInt(totalSales);
        buf.writeDouble(buyerBalanceCbx);

        buf.writeVarInt(slots.size());
        for (ShopSlotItem slot : slots) {
            buf.writeVarInt(slot.slotIndex());
            buf.writeUtf(slot.itemId());
            buf.writeUtf(slot.displayName());
            buf.writeDouble(slot.priceCbx());
            buf.writeVarInt(slot.stockCount());
            buf.writeVarInt(slot.lore().size());
            for (String line : slot.lore()) {
                buf.writeUtf(line);
            }
        }

        buf.writeVarInt(maxSlots);
        buf.writeVarInt(slotCapacity);
        buf.writeBoolean(networkUnlocked);

        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
