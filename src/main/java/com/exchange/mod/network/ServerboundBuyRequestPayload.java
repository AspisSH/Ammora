package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload to create, fulfill, or cancel an escrow Buy Request (RFQ / Bounty).
 */
public record ServerboundBuyRequestPayload(
        String action, // "CREATE", "FULFILL", "CANCEL"
        String requestId,
        String itemId,
        String displayName,
        double unitPrice,
        int amount
) implements CustomPacketPayload {

    public static final Type<ServerboundBuyRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "buy_request_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundBuyRequestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundBuyRequestPayload::write,
            ServerboundBuyRequestPayload::new
    );

    public ServerboundBuyRequestPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readVarInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeUtf(requestId != null ? requestId : "");
        buf.writeUtf(itemId != null ? itemId : "");
        buf.writeUtf(displayName != null ? displayName : "");
        buf.writeDouble(unitPrice);
        buf.writeVarInt(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
