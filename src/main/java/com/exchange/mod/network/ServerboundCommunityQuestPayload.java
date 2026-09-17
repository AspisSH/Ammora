package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server payload for interacting with community quests:
 * CREATE, ACCEPT, CANCEL_WORK, COMPLETE, PAY_REWARD, DELETE.
 */
public record ServerboundCommunityQuestPayload(
        String action,
        String questId,
        String title,
        String description,
        double rewardCbx
) implements CustomPacketPayload {

    @Deprecated
    public double rewardUsdt() {
        return rewardCbx();
    }

    public static final Type<ServerboundCommunityQuestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "community_quest"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundCommunityQuestPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundCommunityQuestPayload::write,
            ServerboundCommunityQuestPayload::new
    );

    public ServerboundCommunityQuestPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(questId != null ? questId : "");
        buf.writeUtf(title != null ? title : "");
        buf.writeUtf(description != null ? description : "");
        buf.writeDouble(rewardCbx);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
