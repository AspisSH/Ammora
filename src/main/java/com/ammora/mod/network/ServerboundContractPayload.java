package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to accept a contract or deliver items to it.
 */
public record ServerboundContractPayload(
        String action, // "ACCEPT" or "DELIVER"
        String contractId,
        int amount
) implements CustomPacketPayload {

    public static final Type<ServerboundContractPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "contract_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundContractPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundContractPayload::write,
            ServerboundContractPayload::new
    );

    public ServerboundContractPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(contractId != null ? contractId : "");
        buf.writeInt(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
