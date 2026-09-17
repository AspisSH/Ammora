package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Sent from server to client when another player invites them to trade.
 */
public record ClientboundTradeInvitePayload(UUID senderUuid, String senderName) implements CustomPacketPayload {

    public static final Type<ClientboundTradeInvitePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "trade_invite_notify"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundTradeInvitePayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ClientboundTradeInvitePayload::write,
                    ClientboundTradeInvitePayload::new
            );

    public ClientboundTradeInvitePayload(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(senderUuid != null ? senderUuid : new UUID(0L, 0L));
        buf.writeUtf(senderName != null ? senderName : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
