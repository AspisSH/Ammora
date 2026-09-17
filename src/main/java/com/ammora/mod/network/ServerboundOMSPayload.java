package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to open or close an OMS position.
 */
public record ServerboundOMSPayload(
        String action, // "OPEN" or "CLOSE"
        String positionId,
        String resourceId,
        double amount // CBX for open, units for close
) implements CustomPacketPayload {

    public static final Type<ServerboundOMSPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "oms_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundOMSPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundOMSPayload::write,
            ServerboundOMSPayload::new
    );

    public ServerboundOMSPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(positionId != null ? positionId : "");
        buf.writeUtf(resourceId != null ? resourceId : "");
        buf.writeDouble(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
