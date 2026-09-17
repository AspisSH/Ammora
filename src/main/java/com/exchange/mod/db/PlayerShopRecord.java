package com.exchange.mod.db;

import java.util.UUID;

/**
 * Model representing an in-world player vending machine (shop).
 * Stores owner info, sales, revenue, and upgrade tiers (maxSlots, slotCapacity, networkUnlocked).
 */
public class PlayerShopRecord {
    private final String shopId;
    private final UUID ownerUuid;
    private String ownerName;
    private String shopName;
    private final String dimension;
    private final int posX;
    private final int posY;
    private final int posZ;
    private boolean broadcast;
    private int totalSales;
    private double accumulatedRevenue;
    private final long createdAt;

    // Upgrades
    private int maxSlots;
    private int slotCapacity;
    private boolean networkUnlocked;

    public PlayerShopRecord(String shopId, UUID ownerUuid, String ownerName, String shopName,
                            String dimension, int posX, int posY, int posZ,
                            boolean broadcast, int totalSales, double accumulatedRevenue, long createdAt,
                            int maxSlots, int slotCapacity, boolean networkUnlocked) {
        this.shopId = shopId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.shopName = shopName;
        this.dimension = dimension;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.broadcast = broadcast;
        this.totalSales = totalSales;
        this.accumulatedRevenue = accumulatedRevenue;
        this.createdAt = createdAt;
        this.maxSlots = Math.max(5, Math.min(10, maxSlots));
        this.slotCapacity = Math.max(64, slotCapacity);
        this.networkUnlocked = networkUnlocked;
    }

    public PlayerShopRecord(String shopId, UUID ownerUuid, String ownerName, String shopName,
                            String dimension, int posX, int posY, int posZ,
                            boolean broadcast, int totalSales, double accumulatedRevenue, long createdAt) {
        this(shopId, ownerUuid, ownerName, shopName, dimension, posX, posY, posZ,
                broadcast, totalSales, accumulatedRevenue, createdAt, 5, 64, false);
    }

    public String getShopId() { return shopId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getDimension() { return dimension; }
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public int getPosZ() { return posZ; }
    public boolean isBroadcast() { return broadcast; }
    public void setBroadcast(boolean broadcast) { this.broadcast = broadcast; }
    public int getTotalSales() { return totalSales; }
    public void incrementSales(int count) { this.totalSales += count; }
    public double getAccumulatedRevenue() { return accumulatedRevenue; }
    public void addRevenue(double revenue) { this.accumulatedRevenue += revenue; }
    public void clearRevenue() { this.accumulatedRevenue = 0.0; }
    public long getCreatedAt() { return createdAt; }

    public int getMaxSlots() { return maxSlots; }
    public void setMaxSlots(int maxSlots) { this.maxSlots = Math.max(5, Math.min(10, maxSlots)); }

    public int getSlotCapacity() { return slotCapacity; }
    public void setSlotCapacity(int slotCapacity) { this.slotCapacity = Math.max(64, slotCapacity); }

    public boolean isNetworkUnlocked() { return networkUnlocked; }
    public void setNetworkUnlocked(boolean networkUnlocked) { this.networkUnlocked = networkUnlocked; }

    private int activeSlotCount;
    public int getActiveSlotCount() { return activeSlotCount; }
    public void setActiveSlotCount(int activeSlotCount) { this.activeSlotCount = activeSlotCount; }
}
