package com.exchange.mod.db;

import java.util.UUID;

/**
 * Model representing a completed marketplace transaction for the public ledger.
 */
public class MarketTxRecord {
    private final String txId;
    private final String txType; // "LOCAL_BUY", "REMOTE_BUY", "BUY_REQUEST"
    private final String shopId;
    private final UUID buyerUuid;
    private final String buyerName;
    private final UUID sellerUuid;
    private final String sellerName;
    private final String itemId;
    private final String itemName;
    private final int amount;
    private final double totalCbx;
    private final double feeCbx;
    private final long timestamp;

    public MarketTxRecord(String txId, String txType, String shopId,
                          UUID buyerUuid, String buyerName,
                          UUID sellerUuid, String sellerName,
                          String itemId, String itemName,
                          int amount, double totalCbx, double feeCbx, long timestamp) {
        this.txId = txId;
        this.txType = txType;
        this.shopId = shopId;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.amount = amount;
        this.totalCbx = totalCbx;
        this.feeCbx = feeCbx;
        this.timestamp = timestamp;
    }

    public String getTxId() { return txId; }
    public String getTxType() { return txType; }
    public String getShopId() { return shopId; }
    public UUID getBuyerUuid() { return buyerUuid; }
    public String getBuyerName() { return buyerName; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getAmount() { return amount; }
    public double getTotalCbx() { return totalCbx; }
    public double getFeeCbx() { return feeCbx; }
    @Deprecated public double getTotalUsdt() { return getTotalCbx(); }
    @Deprecated public double getFeeUsdt() { return getFeeCbx(); }
    public long getTimestamp() { return timestamp; }
}
