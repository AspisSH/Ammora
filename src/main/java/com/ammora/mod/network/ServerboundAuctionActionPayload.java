package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload for Live Auction operations:
 * CREATE (list lot from inventory), BID (place higher bid), BUYOUT (instant purchase), CANCEL (retract lot).
 */
public record ServerboundAuctionActionPayload(
        String action, // "CREATE", "BID", "BUYOUT", "CANCEL"
        String auctionId,
        int slotIndex,
        double startPrice,
        double minBidStep,
        double buyoutPrice,
        int durationMinutes,
        double bidAmount
) implements CustomPacketPayload {

    public static final Type<ServerboundAuctionActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "auction_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundAuctionActionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundAuctionActionPayload::write,
            ServerboundAuctionActionPayload::new
    );

    public ServerboundAuctionActionPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(auctionId != null ? auctionId : "");
        buf.writeVarInt(slotIndex);
        buf.writeDouble(startPrice);
        buf.writeDouble(minBidStep);
        buf.writeDouble(buyoutPrice);
        buf.writeVarInt(durationMinutes);
        buf.writeDouble(bidAmount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
