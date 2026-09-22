package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from server to client when a player receives an invitation to join a corporation.
 */
public record ClientboundCompanyInvitePayload(String companyId, String companyName, String inviterName) implements CustomPacketPayload {

    public static final Type<ClientboundCompanyInvitePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "company_invite_notify"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundCompanyInvitePayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ClientboundCompanyInvitePayload::write,
                    ClientboundCompanyInvitePayload::new
            );

    public ClientboundCompanyInvitePayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(companyId != null ? companyId : "");
        buf.writeUtf(companyName != null ? companyName : "");
        buf.writeUtf(inviterName != null ? inviterName : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
