package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload dispatched from the Operator Admin Panel to execute
 * administrative mutations (adjusting player balances, triggering/stopping economic events,
 * changing AMM base prices/reserves/modifiers, or requesting a fresh snapshot).
 */
public record ServerboundAdminActionPayload(
        String action,
        String targetId,
        double numericValue,
        String extraData
) implements CustomPacketPayload {

    public static final Type<ServerboundAdminActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "admin_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundAdminActionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundAdminActionPayload::write,
            ServerboundAdminActionPayload::new
    );

    public ServerboundAdminActionPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readUtf()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(targetId != null ? targetId : "");
        buf.writeDouble(numericValue);
        buf.writeUtf(extraData != null ? extraData : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
