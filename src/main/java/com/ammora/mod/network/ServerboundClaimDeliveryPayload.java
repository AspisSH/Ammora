package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload to claim items from the tablet delivery buffer into inventory.
 */
public record ServerboundClaimDeliveryPayload(
        String deliveryId,
        boolean claimAll
) implements CustomPacketPayload {

    public static final Type<ServerboundClaimDeliveryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "claim_delivery_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundClaimDeliveryPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundClaimDeliveryPayload::write,
            ServerboundClaimDeliveryPayload::new
    );

    public ServerboundClaimDeliveryPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(deliveryId != null ? deliveryId : "");
        buf.writeBoolean(claimAll);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
