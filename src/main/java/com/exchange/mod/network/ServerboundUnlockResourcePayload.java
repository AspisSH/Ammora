package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to unlock/research a market resource
 * by consuming 1 unit of that item from the player's inventory.
 */
public record ServerboundUnlockResourcePayload(String resourceId) implements CustomPacketPayload {

    public static final Type<ServerboundUnlockResourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "unlock_resource"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundUnlockResourcePayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ServerboundUnlockResourcePayload::write,
                    ServerboundUnlockResourcePayload::new
            );

    public ServerboundUnlockResourcePayload(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(resourceId != null ? resourceId : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
