package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from client to server for any trade window interaction.
 * Actions:
 * - "SET_MONEY": amount
 * - "OFFER_ITEM": invSlot
 * - "REMOVE_ITEM": tradeSlot
 * - "TOGGLE_LOCK": none
 * - "CONFIRM": none
 * - "CANCEL": none
 */
public record ServerboundTradeActionPayload(String action, int slotIndex, double amount) implements CustomPacketPayload {

    public static final Type<ServerboundTradeActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "trade_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundTradeActionPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ServerboundTradeActionPayload::write,
                    ServerboundTradeActionPayload::new
            );

    public ServerboundTradeActionPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readVarInt(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeVarInt(slotIndex);
        buf.writeDouble(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
