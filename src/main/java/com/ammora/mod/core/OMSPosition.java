package com.ammora.mod.core;

import java.util.UUID;

/**
 * Model representing an Unallocated Metal Account position (OMS / Synthetic Commodity Account).
 */
public class OMSPosition {
private final String positionId;
private final UUID playerUuid;
private final String resourceId;
    private double amountUnits;
    private double investedCbx;
    private double avgBuyPrice;
    private final long openedTimestamp;

    public OMSPosition(String positionId, UUID playerUuid, String resourceId,
                       double amountUnits, double investedCbx, double avgBuyPrice,
                       long openedTimestamp) {
        this.positionId = positionId;
        this.playerUuid = playerUuid;
        this.resourceId = resourceId;
        this.amountUnits = amountUnits;
        this.investedCbx = investedCbx;
        this.avgBuyPrice = avgBuyPrice;
        this.openedTimestamp = openedTimestamp;
    }

    public String getPositionId() {
        return positionId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public double getAmountUnits() {
        return amountUnits;
    }

    public void setAmountUnits(double amountUnits) {
        this.amountUnits = amountUnits;
    }

    public double getInvestedCbx() {
        return investedCbx;
    }

    public void setInvestedCbx(double investedCbx) {
        this.investedCbx = investedCbx;
    }

    @Deprecated
    public double getInvestedUSDT() {
        return getInvestedCbx();
    }

    @Deprecated
    public void setInvestedUSDT(double invested) {
        setInvestedCbx(invested);
    }

    public double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public void setAvgBuyPrice(double avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    public long getOpenedTimestamp() {
        return openedTimestamp;
    }

    /**
     * Calculates unrealized profit/loss in CBX based on current sell price.
     */
    public double calculatePnL(MarketResource res) {
        double currentSellPrice = MarketEngine.calculateSellPrice(res.getCurrentStock(), res);
        double currentValue = amountUnits * currentSellPrice;
        double costBasis = amountUnits * avgBuyPrice;
        return MarketEngine.round2(currentValue - costBasis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OMSPosition that = (OMSPosition) o;
        return java.util.Objects.equals(positionId, that.positionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(positionId);
    }
}

