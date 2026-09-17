package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to request execution of a market order (BUY / SELL).
 */
public record ServerboundExecuteOrderPayload(
        String resourceId,
        String orderType, // "BUY" or "SELL"
        int amount
) implements CustomPacketPayload {

    public static final Type<ServerboundExecuteOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "execute_order"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundExecuteOrderPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundExecuteOrderPayload::write,
            ServerboundExecuteOrderPayload::new
    );

    public ServerboundExecuteOrderPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(resourceId);
        buf.writeUtf(orderType);
        buf.writeInt(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
