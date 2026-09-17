package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to request market data for a specific resource,
 * optionally pinning it as the redstone monitored resource for the terminal block.
 */
public record ServerboundSelectResourcePayload(
        String resourceId,
        boolean pinForRedstone
) implements CustomPacketPayload {

    public static final Type<ServerboundSelectResourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "select_resource"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundSelectResourcePayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundSelectResourcePayload::write,
            ServerboundSelectResourcePayload::new
    );

    public ServerboundSelectResourcePayload(String resourceId) {
        this(resourceId, false);
    }

    public ServerboundSelectResourcePayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(resourceId);
        buf.writeBoolean(pinForRedstone);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
