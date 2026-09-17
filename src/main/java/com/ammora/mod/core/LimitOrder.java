package com.ammora.mod.core;

import java.util.UUID;

/**
 * Model representing a pending limit order.
 * Automatically filled when spot price reaches or improves upon limitPrice.
 */
public class LimitOrder {

    public static final String TYPE_BUY = "BUY";
    public static final String TYPE_SELL = "SELL";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final String orderId;
    private final UUID playerUuid;
    private final String playerName;
    private final String resourceId;
    private final String orderType; // BUY or SELL
    private final int amount;
    private final double limitPrice;
    private final double reservedCbx;
    private final long createdTimestamp;
    private String status;

    public LimitOrder(String orderId, UUID playerUuid, String playerName, String resourceId,
                      String orderType, int amount, double limitPrice, double reservedCbx,
                      long createdTimestamp, String status) {
        this.orderId = orderId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.resourceId = resourceId;
        this.orderType = orderType;
        this.amount = amount;
        this.limitPrice = limitPrice;
        this.reservedCbx = reservedCbx;
        this.createdTimestamp = createdTimestamp;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOrderType() {
        return orderType;
    }

    public int getAmount() {
        return amount;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public double getReservedCbx() {
        return reservedCbx;
    }

    @Deprecated
    public double getReservedUsdt() {
        return getReservedCbx();
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
}
