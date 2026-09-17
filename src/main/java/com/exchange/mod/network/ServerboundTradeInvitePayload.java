package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Sent from client to server to invite a nearby player to a secure P2P trade session.
 */
public record ServerboundTradeInvitePayload(UUID targetPlayerUuid) implements CustomPacketPayload {

    public static final Type<ServerboundTradeInvitePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "trade_invite_req"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundTradeInvitePayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ServerboundTradeInvitePayload::write,
                    ServerboundTradeInvitePayload::new
            );

    public ServerboundTradeInvitePayload(FriendlyByteBuf buf) {
        this(buf.readUUID());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerUuid != null ? targetPlayerUuid : new UUID(0L, 0L));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
