package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Sent from client to server accepting or declining a trade invitation.
 */
public record ServerboundTradeInviteResponsePayload(UUID senderUuid, boolean accept) implements CustomPacketPayload {

    public static final Type<ServerboundTradeInviteResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "trade_invite_response"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundTradeInviteResponsePayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ServerboundTradeInviteResponsePayload::write,
                    ServerboundTradeInviteResponsePayload::new
            );

    public ServerboundTradeInviteResponsePayload(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(senderUuid != null ? senderUuid : new UUID(0L, 0L));
        buf.writeBoolean(accept);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
