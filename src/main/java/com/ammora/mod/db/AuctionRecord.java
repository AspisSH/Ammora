package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a live auction listing in the marketplace.
 */
public class AuctionRecord {
    private final String auctionId;
    private final UUID sellerUuid;
    private String sellerName;
    private final String itemId;
    private final String itemNbt;
    private final String displayName;
    private final int itemCount;
    private final double startPrice;
    private double currentBid;
    private final double minBidStep;
    private final double buyoutPrice;
    private UUID highestBidderUuid;
    private String highestBidderName;
    private final long createdAt;
    private long expiresAt;
    private String status; // "ACTIVE", "COMPLETED", "EXPIRED", "CANCELLED"

    public AuctionRecord(String auctionId, UUID sellerUuid, String sellerName,
                         String itemId, String itemNbt, String displayName, int itemCount,
                         double startPrice, double currentBid, double minBidStep, double buyoutPrice,
                         UUID highestBidderUuid, String highestBidderName,
                         long createdAt, long expiresAt, String status) {
        this.auctionId = auctionId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemId = itemId;
        this.itemNbt = itemNbt != null ? itemNbt : "";
        this.displayName = displayName;
        this.itemCount = Math.max(1, itemCount);
        this.startPrice = startPrice;
        this.currentBid = currentBid;
        this.minBidStep = Math.max(0.5, minBidStep);
        this.buyoutPrice = buyoutPrice;
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName != null ? highestBidderName : "";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getAuctionId() { return auctionId; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getItemId() { return itemId; }
    public String getItemNbt() { return itemNbt; }
    public String getDisplayName() { return displayName; }
    public int getItemCount() { return itemCount; }
    public double getStartPrice() { return startPrice; }
    public double getCurrentBid() { return currentBid; }
    public double getMinBidStep() { return minBidStep; }
    public double getBuyoutPrice() { return buyoutPrice; }
    public boolean hasBuyout() { return buyoutPrice > 0.0; }
    public UUID getHighestBidderUuid() { return highestBidderUuid; }
    public String getHighestBidderName() { return highestBidderName; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }

    public double getNextMinBid() {
        if (currentBid <= 0.0 || highestBidderUuid == null) {
            return startPrice;
        }
        return currentBid + minBidStep;
    }

    public boolean placeBid(UUID bidderUuid, String bidderName, double amount) {
        if (!"ACTIVE".equals(status)) return false;
        if (amount < getNextMinBid()) return false;

        this.highestBidderUuid = bidderUuid;
        this.highestBidderName = bidderName;
        this.currentBid = amount;

        // Anti-sniping: If less than 60 seconds remain, extend by 60 seconds
        long now = System.currentTimeMillis();
        if (this.expiresAt - now < 60_000L) {
            this.expiresAt = now + 60_000L;
        }
        return true;
    }

    public boolean isExpired(long now) {
        return "ACTIVE".equals(status) && now >= expiresAt;
    }

    public void complete() {
        this.status = "COMPLETED";
    }

    public void expire() {
        this.status = "EXPIRED";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }
}
