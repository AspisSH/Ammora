package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to place or cancel a limit order.
 */
public record ServerboundLimitOrderPayload(
        String action, // "PLACE" or "CANCEL"
        String orderId,
        String resourceId,
        String orderType, // "BUY" or "SELL"
        int amount,
        double limitPrice
) implements CustomPacketPayload {

    public static final Type<ServerboundLimitOrderPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "limit_order_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundLimitOrderPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundLimitOrderPayload::write,
            ServerboundLimitOrderPayload::new
    );

    public ServerboundLimitOrderPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(orderId != null ? orderId : "");
        buf.writeUtf(resourceId != null ? resourceId : "");
        buf.writeUtf(orderType != null ? orderType : "");
        buf.writeInt(amount);
        buf.writeDouble(limitPrice);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
