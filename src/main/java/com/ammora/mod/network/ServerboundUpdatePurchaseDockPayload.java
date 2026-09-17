package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from client to server to update settings of a Purchase Dock:
 * selected resource, batch size, and stop-high max buy price guard.
 */
public record ServerboundUpdatePurchaseDockPayload(
        BlockPos pos,
        String targetResourceId,
        int batchSize,
        double maxBuyPrice
) implements CustomPacketPayload {

    public static final Type<ServerboundUpdatePurchaseDockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "update_purchase_dock"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundUpdatePurchaseDockPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundUpdatePurchaseDockPayload::write,
            ServerboundUpdatePurchaseDockPayload::new
    );

    public ServerboundUpdatePurchaseDockPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readDouble()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(targetResourceId != null ? targetResourceId : "");
        buf.writeVarInt(batchSize);
        buf.writeDouble(maxBuyPrice);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
