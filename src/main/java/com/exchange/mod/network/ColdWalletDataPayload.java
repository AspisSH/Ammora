package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload sent from server to client to synchronize Cold Wallet state:
 * player CBX balance, rank, nearby online players for P2P transfer, and transaction ledger.
 */
public record ColdWalletDataPayload(
        double balanceCbx,
        int repLevel,
        int repPoints,
        List<NearbyPlayerItem> nearbyPlayers,
        List<LedgerItem> ledgerEntries,
        String statusMessage,
        boolean isError
) implements CustomPacketPayload {

    public static final Type<ColdWalletDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "cold_wallet_data"));

    public static final StreamCodec<FriendlyByteBuf, ColdWalletDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ColdWalletDataPayload::write,
            ColdWalletDataPayload::new
    );

    public record NearbyPlayerItem(UUID uuid, String name, double distance) {}
    public record LedgerItem(String type, String title, double amountCbx, long timestamp) {
        @Deprecated
        public double amountUsdt() {
            return amountCbx();
        }
    }

    @Deprecated
    public double balanceUsdt() {
        return balanceCbx();
    }

    public ColdWalletDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                readPlayers(buf),
                readLedger(buf),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    private static List<NearbyPlayerItem> readPlayers(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<NearbyPlayerItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new NearbyPlayerItem(buf.readUUID(), buf.readUtf(), buf.readDouble()));
        }
        return list;
    }

    private static List<LedgerItem> readLedger(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<LedgerItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new LedgerItem(buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readLong()));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(balanceCbx);
        buf.writeVarInt(repLevel);
        buf.writeVarInt(repPoints);

        buf.writeVarInt(nearbyPlayers.size());
        for (NearbyPlayerItem p : nearbyPlayers) {
            buf.writeUUID(p.uuid());
            buf.writeUtf(p.name());
            buf.writeDouble(p.distance());
        }

        buf.writeVarInt(ledgerEntries.size());
        for (LedgerItem item : ledgerEntries) {
            buf.writeUtf(item.type());
            buf.writeUtf(item.title());
            buf.writeDouble(item.amountCbx());
            buf.writeLong(item.timestamp());
        }

        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isError);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
