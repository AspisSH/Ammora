package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload sent from server to client to synchronize the Purchase Dock GUI state.
 */
public record PurchaseDockDataPayload(
        BlockPos pos,
        String targetResourceId,
        String targetResourceDisplayName,
        double spotPrice,
        int batchSize,
        double maxBuyPrice,
        int repLevel,
        int repPoints,
        double userBalanceCbx,
        String ownerName,
        List<DockResourceItem> availableResources,
        String statusMessage,
        boolean isError,
        String companyName,
        boolean isCompanyLinked
) implements CustomPacketPayload {

    public PurchaseDockDataPayload(
            BlockPos pos, String targetResourceId, String targetResourceDisplayName,
            double spotPrice, int batchSize, double maxBuyPrice, int repLevel, int repPoints,
            double userBalanceCbx, String ownerName, List<DockResourceItem> availableResources,
            String statusMessage, boolean isError
    ) {
        this(pos, targetResourceId, targetResourceDisplayName, spotPrice, batchSize, maxBuyPrice,
                repLevel, repPoints, userBalanceCbx, ownerName, availableResources, statusMessage, isError, "", false);
    }

    public static final Type<PurchaseDockDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "purchase_dock_data"));

    public static final StreamCodec<FriendlyByteBuf, PurchaseDockDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            PurchaseDockDataPayload::write,
            PurchaseDockDataPayload::new
    );

    public record DockResourceItem(String resourceId, String displayName, double spotPrice) {}

    @Deprecated
    public double userBalanceUsdt() {
        return userBalanceCbx();
    }

    public PurchaseDockDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readUtf(),
                readResources(buf),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    private static List<DockResourceItem> readResources(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DockResourceItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new DockResourceItem(buf.readUtf(), buf.readUtf(), buf.readDouble()));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(targetResourceId != null ? targetResourceId : "");
        buf.writeUtf(targetResourceDisplayName != null ? targetResourceDisplayName : "");
        buf.writeDouble(spotPrice);
        buf.writeVarInt(batchSize);
        buf.writeDouble(maxBuyPrice);
        buf.writeVarInt(repLevel);
        buf.writeVarInt(repPoints);
        buf.writeDouble(userBalanceCbx);
        buf.writeUtf(ownerName != null ? ownerName : "");

        buf.writeVarInt(availableResources.size());
        for (DockResourceItem item : availableResources) {
            buf.writeUtf(item.resourceId());
            buf.writeUtf(item.displayName());
            buf.writeDouble(item.spotPrice());
        }

        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
        buf.writeUtf(companyName != null ? companyName : "");
        buf.writeBoolean(isCompanyLinked);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
