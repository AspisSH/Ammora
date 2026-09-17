package com.ammora.mod.db;

/**
 * Model representing a single product slot in a player's vending machine.
 */
public class ShopSlotRecord {
    private final String shopId;
    private final int slotIndex;
    private String itemId;
    private String itemNbt;
    private String displayName;
    private double priceCbx;
    private int stockCount;
    private String shopName;
    private String ownerName;

    public ShopSlotRecord(String shopId, int slotIndex, String itemId, String itemNbt,
                          String displayName, double priceCbx, int stockCount) {
        this.shopId = shopId;
        this.slotIndex = slotIndex;
        this.itemId = itemId;
        this.itemNbt = itemNbt;
        this.displayName = displayName;
        this.priceCbx = priceCbx;
        this.stockCount = stockCount;
    }

    public String getShopId() { return shopId; }
    public int getSlotIndex() { return slotIndex; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemNbt() { return itemNbt; }
    public void setItemNbt(String itemNbt) { this.itemNbt = itemNbt; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public double getPriceCbx() { return priceCbx; }
    public void setPriceCbx(double priceCbx) { this.priceCbx = priceCbx; }
    @Deprecated public double getPriceUsdt() { return getPriceCbx(); }
    @Deprecated public void setPriceUsdt(double p) { setPriceCbx(p); }
    public int getStockCount() { return stockCount; }
    public void setStockCount(int stockCount) { this.stockCount = stockCount; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || stockCount <= 0;
    }
}
