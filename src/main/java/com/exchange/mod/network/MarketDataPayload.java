package com.exchange.mod.network;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.core.MarketResource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server to client containing market statistics, depth, candlestick history,
 * AMM parameters, status notifications, pinned resource, daily fluctuations, active events,
 * and terminal redstone settings.
 */
public record MarketDataPayload(
        String resourceId,
        String displayName,
        double spotPrice,
        double buyPrice,
        double sellPrice,
        double disposalFee,
        double currentStock,
        double targetReserve,
        double maxReserve,
        double userBalanceCbx,
        int userRepLevel,
        int userRepPoints,
        List<CandleItem> candles,
        String statusMessage,
        boolean isStatusError,
        double basePrice,
        double elasticity,
        double feeRate,
        double disposalAlpha,
        double minPriceFloor,
        List<MarketSummaryItem> availableMarkets,
        String pinnedResourceId,
        double dailyModifier,
        String activeEventTitle,
        String activeEventDescription,
        int redstoneMode,
        double thresholdPrice,
        boolean thresholdIsLessThan,
        BlockPos terminalPos,
        List<OMSItem> omsPositions,
        List<LimitOrderItem> limitOrders,
        List<ContractItem> contracts,
        List<String> unlockedResources
) implements CustomPacketPayload {

    public static final Type<MarketDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "market_data"));

    public static final StreamCodec<FriendlyByteBuf, MarketDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
            MarketDataPayload::write,
            MarketDataPayload::new
    );

    @Deprecated
    public double userBalanceUsdt() {
        return userBalanceCbx();
    }

    public record CandleItem(long timestamp, double open, double high, double low, double close, double volume) {}
    public record MarketSummaryItem(String resourceId, String displayName, double spotPrice, double fillPercent, double dailyModifier, boolean isUnlocked) {}
    public record OMSItem(String positionId, String resourceId, double units, double investedCbx, double avgBuyPrice, double currentPnL) {
        @Deprecated public double investedUsdt() { return investedCbx(); }
    }
    public record LimitOrderItem(String orderId, String resourceId, String type, int amount, double limitPrice, double reservedCbx, String status) {
        @Deprecated public double reservedUsdt() { return reservedCbx(); }
    }
    public record ContractItem(String contractId, String title, String resourceId, int targetAmount, int deliveredAmount, double guaranteedPrice, double collateral, boolean isAcceptedByMe, long remainingTicks, int rewardRep, String status) {}

    public MarketDataPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                readCandleList(buf),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                readMarketSummaryList(buf),
                buf.readUtf(),
                buf.readDouble(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readBoolean() ? buf.readBlockPos() : null,
                readOMSList(buf),
                readLimitOrderList(buf),
                readContractList(buf),
                readUnlockedList(buf)
        );
    }

    private static List<CandleItem> readCandleList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<CandleItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new CandleItem(
                    buf.readLong(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
        return list;
    }

    private static List<MarketSummaryItem> readMarketSummaryList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MarketSummaryItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new MarketSummaryItem(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readBoolean()
            ));
        }
        return list;
    }

    private static List<String> readUnlockedList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }

    private static List<OMSItem> readOMSList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<OMSItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new OMSItem(buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return list;
    }

    private static List<LimitOrderItem> readLimitOrderList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<LimitOrderItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new LimitOrderItem(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readUtf()));
        }
        return list;
    }

    private static List<ContractItem> readContractList(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ContractItem> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new ContractItem(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readLong(), buf.readInt(), buf.readUtf()));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(resourceId);
        buf.writeUtf(displayName);
        buf.writeDouble(spotPrice);
        buf.writeDouble(buyPrice);
        buf.writeDouble(sellPrice);
        buf.writeDouble(disposalFee);
        buf.writeDouble(currentStock);
        buf.writeDouble(targetReserve);
        buf.writeDouble(maxReserve);
        buf.writeDouble(userBalanceCbx);
        buf.writeInt(userRepLevel);
        buf.writeInt(userRepPoints);
        buf.writeInt(candles.size());
        for (CandleItem c : candles) {
            buf.writeLong(c.timestamp());
            buf.writeDouble(c.open());
            buf.writeDouble(c.high());
            buf.writeDouble(c.low());
            buf.writeDouble(c.close());
            buf.writeDouble(c.volume());
        }
        buf.writeUtf(statusMessage != null ? statusMessage : "");
        buf.writeBoolean(isStatusError);
        buf.writeDouble(basePrice);
        buf.writeDouble(elasticity);
        buf.writeDouble(feeRate);
        buf.writeDouble(disposalAlpha);
        buf.writeDouble(minPriceFloor);
        List<MarketSummaryItem> summaries = availableMarkets != null ? availableMarkets : List.of();
        buf.writeInt(summaries.size());
        for (MarketSummaryItem m : summaries) {
            buf.writeUtf(m.resourceId());
            buf.writeUtf(m.displayName());
            buf.writeDouble(m.spotPrice());
            buf.writeDouble(m.fillPercent());
            buf.writeDouble(m.dailyModifier());
            buf.writeBoolean(m.isUnlocked());
        }
        buf.writeUtf(pinnedResourceId != null ? pinnedResourceId : "");
        buf.writeDouble(dailyModifier);
        buf.writeUtf(activeEventTitle != null ? activeEventTitle : "");
        buf.writeUtf(activeEventDescription != null ? activeEventDescription : "");
        buf.writeVarInt(redstoneMode);
        buf.writeDouble(thresholdPrice);
        buf.writeBoolean(thresholdIsLessThan);
        if (terminalPos != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(terminalPos);
        } else {
            buf.writeBoolean(false);
        }

        List<OMSItem> oms = omsPositions != null ? omsPositions : List.of();
        buf.writeInt(oms.size());
        for (OMSItem o : oms) {
            buf.writeUtf(o.positionId());
            buf.writeUtf(o.resourceId());
            buf.writeDouble(o.units());
            buf.writeDouble(o.investedCbx());
            buf.writeDouble(o.avgBuyPrice());
            buf.writeDouble(o.currentPnL());
        }

        List<LimitOrderItem> limits = limitOrders != null ? limitOrders : List.of();
        buf.writeInt(limits.size());
        for (LimitOrderItem l : limits) {
            buf.writeUtf(l.orderId());
            buf.writeUtf(l.resourceId());
            buf.writeUtf(l.type());
            buf.writeInt(l.amount());
            buf.writeDouble(l.limitPrice());
            buf.writeDouble(l.reservedCbx());
            buf.writeUtf(l.status());
        }

        List<ContractItem> cnts = contracts != null ? contracts : List.of();
        buf.writeInt(cnts.size());
        for (ContractItem c : cnts) {
            buf.writeUtf(c.contractId());
            buf.writeUtf(c.title());
            buf.writeUtf(c.resourceId());
            buf.writeInt(c.targetAmount());
            buf.writeInt(c.deliveredAmount());
            buf.writeDouble(c.guaranteedPrice());
            buf.writeDouble(c.collateral());
            buf.writeBoolean(c.isAcceptedByMe());
            buf.writeLong(c.remainingTicks());
            buf.writeInt(c.rewardRep());
            buf.writeUtf(c.status());
        }

        List<String> unl = unlockedResources != null ? unlockedResources : List.of();
        buf.writeVarInt(unl.size());
        for (String s : unl) {
            buf.writeUtf(s);
        }
    }

    public boolean isCurrentResourceUnlocked() {
        return unlockedResources != null && unlockedResources.contains(resourceId);
    }

    /**
     * Reconstructs MarketResource representation for client-side AMM curve calculations.
     */
    public MarketResource toMarketResource() {
        return new MarketResource(
                resourceId,
                displayName,
                basePrice,
                targetReserve,
                currentStock,
                elasticity,
                maxReserve,
                disposalAlpha,
                feeRate,
                minPriceFloor,
                dailyModifier,
                0.0
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
