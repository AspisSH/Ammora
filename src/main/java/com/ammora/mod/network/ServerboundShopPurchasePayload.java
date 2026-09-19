package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload to purchase goods from a player shop (local or remote).
 */
public record ServerboundShopPurchasePayload(
        String shopId,
        int slotIndex,
        int amount,
        boolean isRemote,
        boolean fromCompanyAccount
) implements CustomPacketPayload {

    public ServerboundShopPurchasePayload(String shopId, int slotIndex, int amount, boolean isRemote) {
        this(shopId, slotIndex, amount, isRemote, false);
    }

    public static final Type<ServerboundShopPurchasePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "shop_purchase"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundShopPurchasePayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundShopPurchasePayload::write,
            ServerboundShopPurchasePayload::new
    );

    public ServerboundShopPurchasePayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(shopId);
        buf.writeVarInt(slotIndex);
        buf.writeVarInt(amount);
        buf.writeBoolean(isRemote);
        buf.writeBoolean(fromCompanyAccount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
