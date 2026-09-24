package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from client to server when performing an ATM operation (withdraw, deposit all, deposit amount).
 */
public record ServerboundAtmActionPayload(
        int action, // 0 = WITHDRAW, 1 = DEPOSIT_ALL, 2 = DEPOSIT_AMOUNT
        double amount,
        boolean useCompanyAccount
) implements CustomPacketPayload {

    public static final int ACTION_WITHDRAW = 0;
    public static final int ACTION_DEPOSIT_ALL = 1;
    public static final int ACTION_DEPOSIT_AMOUNT = 2;

    public static final Type<ServerboundAtmActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "atm_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundAtmActionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundAtmActionPayload::write,
            ServerboundAtmActionPayload::new
    );

    public ServerboundAtmActionPayload(FriendlyByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readDouble(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeDouble(amount);
        buf.writeBoolean(useCompanyAccount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
