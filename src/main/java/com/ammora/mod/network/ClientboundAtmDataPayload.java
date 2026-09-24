package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from server to client to synchronize account balances and notifications for the ATM GUI.
 */
public record ClientboundAtmDataPayload(
        double personalBalance,
        boolean hasCompany,
        String companyId,
        String companyName,
        double companyBalance,
        boolean isCompanyOwner,
        double memberDailyLimit,
        double memberSpentToday,
        String statusMessage,
        boolean isError
) implements CustomPacketPayload {

    public static final Type<ClientboundAtmDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "atm_data"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundAtmDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ClientboundAtmDataPayload::write,
            ClientboundAtmDataPayload::new
    );

    public ClientboundAtmDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readDouble(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(personalBalance);
        buf.writeBoolean(hasCompany);
        buf.writeUtf(companyId != null ? companyId : "");
        buf.writeUtf(companyName != null ? companyName : "");
        buf.writeDouble(companyBalance);
        buf.writeBoolean(isCompanyOwner);
        buf.writeDouble(memberDailyLimit);
        buf.writeDouble(memberSpentToday);
        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
