package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload to configure player vending machine:
 * Set slot prices, rename shop, toggle network broadcast, or claim accumulated revenue.
 */
public record ServerboundConfigureShopPayload(
        String shopId,
        String action,
        int slotIndex,
        double price,
        String textParam
) implements CustomPacketPayload {

    public static final Type<ServerboundConfigureShopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "configure_shop"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundConfigureShopPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundConfigureShopPayload::write,
            ServerboundConfigureShopPayload::new
    );

    public ServerboundConfigureShopPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readUtf()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(shopId);
        buf.writeUtf(action);
        buf.writeVarInt(slotIndex);
        buf.writeDouble(price);
        buf.writeUtf(textParam != null ? textParam : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
