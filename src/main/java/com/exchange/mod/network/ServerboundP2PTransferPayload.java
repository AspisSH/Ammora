package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Sent from client to server to perform a peer-to-peer (P2P) CBX transfer
 * via the Cold Wallet item.
 */
public record ServerboundP2PTransferPayload(
        UUID targetPlayerUuid,
        String targetPlayerName,
        double amount
) implements CustomPacketPayload {

    public static final Type<ServerboundP2PTransferPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "p2p_transfer"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundP2PTransferPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundP2PTransferPayload::write,
            ServerboundP2PTransferPayload::new
    );

    public ServerboundP2PTransferPayload(FriendlyByteBuf buf) {
        this(
                buf.readUUID(),
                buf.readUtf(),
                buf.readDouble()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerUuid != null ? targetPlayerUuid : new UUID(0L, 0L));
        buf.writeUtf(targetPlayerName != null ? targetPlayerName : "");
        buf.writeDouble(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
