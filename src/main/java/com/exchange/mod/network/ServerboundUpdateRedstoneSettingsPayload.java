package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to configure advanced redstone modes
 * (Warehouse fill, Price ratio, Threshold trigger) on an Exchange Terminal.
 */
public record ServerboundUpdateRedstoneSettingsPayload(
        BlockPos pos,
        int redstoneMode,
        double thresholdPrice,
        boolean thresholdIsLessThan,
        String resourceId
) implements CustomPacketPayload {

    public static final Type<ServerboundUpdateRedstoneSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "update_redstone_settings"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundUpdateRedstoneSettingsPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundUpdateRedstoneSettingsPayload::write,
            ServerboundUpdateRedstoneSettingsPayload::new
    );

    public ServerboundUpdateRedstoneSettingsPayload(FriendlyByteBuf buf) {
        this(
                buf.readBoolean() ? buf.readBlockPos() : null,
                buf.readVarInt(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readUtf()
        );
    }

    public void write(FriendlyByteBuf buf) {
        if (pos != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(pos);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeVarInt(redstoneMode);
        buf.writeDouble(thresholdPrice);
        buf.writeBoolean(thresholdIsLessThan);
        buf.writeUtf(resourceId != null ? resourceId : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
