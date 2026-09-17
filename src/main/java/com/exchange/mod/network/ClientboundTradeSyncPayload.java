package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent from server to client to synchronize the 2-player P2P secure trade window.
 * Synchronizes 18 items on each side, money offers, ready/confirmed states, and status banners.
 */
public record ClientboundTradeSyncPayload(
        boolean activeSession,
        boolean isPlayerA,
        String myName,
        String partnerName,
        double myMoney,
        double partnerMoney,
        List<ItemStack> myItems,
        List<ItemStack> partnerItems,
        boolean myLocked,
        boolean partnerLocked,
        boolean myConfirmed,
        boolean partnerConfirmed,
        String statusMessage,
        boolean isErrorMessage
) implements CustomPacketPayload {

    public static final Type<ClientboundTradeSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "trade_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTradeSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(
                    ClientboundTradeSyncPayload::write,
                    ClientboundTradeSyncPayload::new
            );

    public ClientboundTradeSyncPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                readItemStacks(buf),
                readItemStacks(buf),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(activeSession);
        buf.writeBoolean(isPlayerA);
        buf.writeUtf(myName != null ? myName : "");
        buf.writeUtf(partnerName != null ? partnerName : "");
        buf.writeDouble(myMoney);
        buf.writeDouble(partnerMoney);
        writeItemStacks(buf, myItems);
        writeItemStacks(buf, partnerItems);
        buf.writeBoolean(myLocked);
        buf.writeBoolean(partnerLocked);
        buf.writeBoolean(myConfirmed);
        buf.writeBoolean(partnerConfirmed);
        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isErrorMessage);
    }

    private static List<ItemStack> readItemStacks(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return list;
    }

    private static void writeItemStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        if (stacks == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack != null ? stack : ItemStack.EMPTY);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
