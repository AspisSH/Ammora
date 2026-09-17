package com.ammora.mod.core;

/**
 * Model representing a tradable resource on the exchange.
 */
public class MarketResource {
    private final String resourceId;
    private String displayName;
    private double basePrice;       // P_0
    private double targetReserve;   // S_target
    private double currentStock;    // S
    private double elasticity;      // k (e.g. 0.8)
    private double maxReserve;      // S_max
    private double disposalAlpha;   // alpha (e.g. 15.0)
    private double feeRate;         // fee (e.g. 0.02 = 2%)
    private double minPriceFloor;   // floor price (e.g. 0.10 CBX)

    // Dynamic Economy modifiers
    private double dailyModifier = 0.0;  // e.g. +0.07 (+7%) or -0.05 (-5%)
    private double eventModifier = 0.0;  // e.g. +0.35 or -0.25 from market events

    public MarketResource(String resourceId, String displayName, double basePrice,
                          double targetReserve, double currentStock, double elasticity,
                          double maxReserve, double disposalAlpha, double feeRate,
                          double minPriceFloor) {
        this(resourceId, displayName, basePrice, targetReserve, currentStock, elasticity,
                maxReserve, disposalAlpha, feeRate, minPriceFloor, 0.0, 0.0);
    }

    public MarketResource(String resourceId, String displayName, double basePrice,
                          double targetReserve, double currentStock, double elasticity,
                          double maxReserve, double disposalAlpha, double feeRate,
                          double minPriceFloor, double dailyModifier, double eventModifier) {
        this.resourceId = resourceId;
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.targetReserve = targetReserve;
        this.currentStock = currentStock;
        this.elasticity = elasticity;
        this.maxReserve = maxReserve;
        this.disposalAlpha = disposalAlpha;
        this.feeRate = feeRate;
        this.minPriceFloor = minPriceFloor;
        this.dailyModifier = dailyModifier;
        this.eventModifier = eventModifier;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getTargetReserve() {
        return targetReserve;
    }

    public void setTargetReserve(double targetReserve) {
        this.targetReserve = targetReserve;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public double getElasticity() {
        return elasticity;
    }

    public void setElasticity(double elasticity) {
        this.elasticity = elasticity;
    }

    public double getMaxReserve() {
        return maxReserve;
    }

    public void setMaxReserve(double maxReserve) {
        this.maxReserve = maxReserve;
    }

    public double getDisposalAlpha() {
        return disposalAlpha;
    }

    public void setDisposalAlpha(double disposalAlpha) {
        this.disposalAlpha = disposalAlpha;
    }

    public double getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(double feeRate) {
        this.feeRate = feeRate;
    }

    public double getMinPriceFloor() {
        return minPriceFloor;
    }

    public void setMinPriceFloor(double minPriceFloor) {
        this.minPriceFloor = minPriceFloor;
    }

    public double getDailyModifier() {
        return dailyModifier;
    }

    public void setDailyModifier(double dailyModifier) {
        this.dailyModifier = dailyModifier;
    }

    public double getEventModifier() {
        return eventModifier;
    }

    public void setEventModifier(double eventModifier) {
        this.eventModifier = eventModifier;
    }

    /**
     * Effective base price accounting for daily fluctuations and active market events:
     * P_0_effective = max(floor, P_0 * (1.0 + dailyModifier + eventModifier))
     */
    public double getEffectiveBasePrice() {
        double multiplier = Math.max(0.10, 1.0 + dailyModifier + eventModifier);
        return Math.max(minPriceFloor, basePrice * multiplier);
    }
}
