package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-to-Client payload containing synchronization data for the Operator Admin Panel:
 * Player accounts, trade ledger logs, available event templates, active market event status,
 * and current AMM resource pricing and reserve statistics.
 */
public record ClientboundAdminDataPayload(
        List<AdminAccountItem> accounts,
        List<AdminTxItem> transactions,
        List<AdminEventTemplateItem> eventTemplates,
        String activeEventId,
        String activeEventTitle,
        String activeEventDesc,
        int activeEventRemainingDays,
        double activeEventMultiplier,
        List<AdminResourceItem> resources,
        String statusMessage,
        boolean isError
) implements CustomPacketPayload {

    public static final Type<ClientboundAdminDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "admin_data"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundAdminDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ClientboundAdminDataPayload::write,
            ClientboundAdminDataPayload::new
    );

    public record AdminAccountItem(
            UUID playerUuid,
            String playerName,
            double balanceCbx,
            int repPoints,
            int repLevel,
            long updatedAt
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(playerUuid);
            buf.writeUtf(playerName);
            buf.writeDouble(balanceCbx);
            buf.writeInt(repPoints);
            buf.writeInt(repLevel);
            buf.writeLong(updatedAt);
        }

        public static AdminAccountItem read(FriendlyByteBuf buf) {
            return new AdminAccountItem(
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readLong()
            );
        }
    }

    public record AdminTxItem(
            String txId,
            String txType,
            String buyerName,
            String sellerName,
            String itemId,
            String itemName,
            int amount,
            double totalCbx,
            double feeCbx,
            long timestamp
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(txId);
            buf.writeUtf(txType);
            buf.writeUtf(buyerName);
            buf.writeUtf(sellerName);
            buf.writeUtf(itemId);
            buf.writeUtf(itemName);
            buf.writeInt(amount);
            buf.writeDouble(totalCbx);
            buf.writeDouble(feeCbx);
            buf.writeLong(timestamp);
        }

        public static AdminTxItem read(FriendlyByteBuf buf) {
            return new AdminTxItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readLong()
            );
        }
    }

    public record AdminEventTemplateItem(
            String id,
            String title,
            String description,
            String resourceId,
            double multiplier,
            int defaultDuration
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(title);
            buf.writeUtf(description);
            buf.writeUtf(resourceId);
            buf.writeDouble(multiplier);
            buf.writeInt(defaultDuration);
        }

        public static AdminEventTemplateItem read(FriendlyByteBuf buf) {
            return new AdminEventTemplateItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readInt()
            );
        }
    }

    public record AdminResourceItem(
            String resourceId,
            String displayName,
            double spotPrice,
            double basePrice,
            double currentStock,
            double targetReserve,
            double elasticity,
            double dailyModifier,
            double eventModifier
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(resourceId);
            buf.writeUtf(displayName);
            buf.writeDouble(spotPrice);
            buf.writeDouble(basePrice);
            buf.writeDouble(currentStock);
            buf.writeDouble(targetReserve);
            buf.writeDouble(elasticity);
            buf.writeDouble(dailyModifier);
            buf.writeDouble(eventModifier);
        }

        public static AdminResourceItem read(FriendlyByteBuf buf) {
            return new AdminResourceItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            );
        }
    }

    public ClientboundAdminDataPayload(FriendlyByteBuf buf) {
        this(
                readAccounts(buf),
                readTransactions(buf),
                readEventTemplates(buf),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readDouble(),
                readResources(buf),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(accounts.size());
        for (AdminAccountItem a : accounts) a.write(buf);

        buf.writeInt(transactions.size());
        for (AdminTxItem t : transactions) t.write(buf);

        buf.writeInt(eventTemplates.size());
        for (AdminEventTemplateItem e : eventTemplates) e.write(buf);

        buf.writeUtf(activeEventId != null ? activeEventId : "");
        buf.writeUtf(activeEventTitle != null ? activeEventTitle : "");
        buf.writeUtf(activeEventDesc != null ? activeEventDesc : "");
        buf.writeInt(activeEventRemainingDays);
        buf.writeDouble(activeEventMultiplier);

        buf.writeInt(resources.size());
        for (AdminResourceItem r : resources) r.write(buf);

        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
    }

    private static List<AdminAccountItem> readAccounts(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<AdminAccountItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(AdminAccountItem.read(buf));
        return list;
    }

    private static List<AdminTxItem> readTransactions(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<AdminTxItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(AdminTxItem.read(buf));
        return list;
    }

    private static List<AdminEventTemplateItem> readEventTemplates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<AdminEventTemplateItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(AdminEventTemplateItem.read(buf));
        return list;
    }

    private static List<AdminResourceItem> readResources(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<AdminResourceItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(AdminResourceItem.read(buf));
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
