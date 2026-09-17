package com.exchange.mod.db;

import java.util.UUID;

/**
 * Model representing an escrow-backed buy request (bounty / RFQ).
 */
public class BuyRequestRecord {
    private final String requestId;
    private final UUID buyerUuid;
    private String buyerName;
    private final String itemId;
    private final String itemNbt;
    private final String displayName;
    private final double unitPrice;
    private final int amountRequested;
    private int amountFulfilled;
    private double escrowCbx;
    private String status; // "ACTIVE", "COMPLETED", "CANCELLED"
    private final long createdAt;

    public BuyRequestRecord(String requestId, UUID buyerUuid, String buyerName,
                            String itemId, String itemNbt, String displayName,
                            double unitPrice, int amountRequested, int amountFulfilled,
                            double escrowCbx, String status, long createdAt) {
        this.requestId = requestId;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.itemId = itemId;
        this.itemNbt = itemNbt;
        this.displayName = displayName;
        this.unitPrice = unitPrice;
        this.amountRequested = amountRequested;
        this.amountFulfilled = amountFulfilled;
        this.escrowCbx = escrowCbx;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getRequestId() { return requestId; }
    public UUID getBuyerUuid() { return buyerUuid; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getItemId() { return itemId; }
    public String getItemNbt() { return itemNbt; }
    public String getDisplayName() { return displayName; }
    public double getUnitPrice() { return unitPrice; }
    public int getAmountRequested() { return amountRequested; }
    public int getAmountFulfilled() { return amountFulfilled; }
    public int getRemainingAmount() { return Math.max(0, amountRequested - amountFulfilled); }
    public double getEscrowCbx() { return escrowCbx; }
    @Deprecated public double getEscrowUsdt() { return getEscrowCbx(); }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }

    public void fulfill(int count) {
        this.amountFulfilled += count;
        this.escrowCbx -= (count * unitPrice);
        if (this.amountFulfilled >= this.amountRequested || this.escrowCbx <= 0.001) {
            this.status = "COMPLETED";
        }
    }

    public void cancel() {
        this.status = "CANCELLED";
    }
}
