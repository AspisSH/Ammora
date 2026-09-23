package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to buy or select a courier skin.
 */
public record ServerboundCourierSkinPayload(
        String action, // "SELECT" or "BUY"
        String courierId // "BEE", "ALLAY", "HEAVY_BEE", "PHANTOM"
) implements CustomPacketPayload {

    public static final Type<ServerboundCourierSkinPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_skin"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundCourierSkinPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ServerboundCourierSkinPayload::write, ServerboundCourierSkinPayload::new);

    public ServerboundCourierSkinPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.action != null ? this.action : "");
        buf.writeUtf(this.courierId != null ? this.courierId : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ServerboundCourierSkinPayload select(String courierId) {
        return new ServerboundCourierSkinPayload("SELECT", courierId != null ? courierId : "BEE");
    }

    public static ServerboundCourierSkinPayload buy(String courierId) {
        return new ServerboundCourierSkinPayload("BUY", courierId != null ? courierId : "BEE");
    }
}
