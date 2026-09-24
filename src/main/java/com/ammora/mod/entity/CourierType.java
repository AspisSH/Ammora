package com.ammora.mod.entity;

import com.ammora.mod.db.MarketDAO;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Enumeration of available courier skins with achievement unlock conditions and buyout prices.
 */
public enum CourierType {
    BEE("BEE", 0.0, "courier.type.bee", "courier.type.bee.desc", "courier.type.bee.achieve", 0.0, () -> Items.HONEYCOMB),
    ALLAY("ALLAY", 2500.0, "courier.type.allay", "courier.type.allay.desc", "courier.type.allay.achieve", 1.0, () -> Items.AMETHYST_SHARD),
    PARROT("PARROT", 5000.0, "courier.type.parrot", "courier.type.parrot.desc", "courier.type.parrot.achieve", 1.0, () -> Items.FEATHER),
    DRONE("DRONE", 15000.0, "courier.type.drone", "courier.type.drone.desc", "courier.type.drone.achieve", 10.0, () -> Items.COMPASS),
    PHANTOM("PHANTOM", 25000.0, "courier.type.phantom", "courier.type.phantom.desc", "courier.type.phantom.achieve", 100.0, () -> Items.PHANTOM_MEMBRANE),
    HEAVY_BEE("HEAVY_BEE", 50000.0, "courier.type.heavy_bee", "courier.type.heavy_bee.desc", "courier.type.heavy_bee.achieve", 200000.0, () -> Items.HONEY_BLOCK);

    private final String id;
    private final double priceCbx;
    private final String nameKey;
    private final String descKey;
    private final String achievementDescKey;
    private final double targetGoal;
    private final Supplier<Item> iconSupplier;

    CourierType(String id, double priceCbx, String nameKey, String descKey, String achievementDescKey, double targetGoal, Supplier<Item> iconSupplier) {
        this.id = id;
        this.priceCbx = priceCbx;
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.achievementDescKey = achievementDescKey;
        this.targetGoal = targetGoal;
        this.iconSupplier = iconSupplier;
    }

    public String getId() {
        return id;
    }

    public double getPriceCbx() {
        return priceCbx;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getDescKey() {
        return descKey;
    }

    public String getAchievementDescKey() {
        return achievementDescKey;
    }

    public double getTargetGoal() {
        return targetGoal;
    }

    public Item getIconItem() {
        return iconSupplier.get();
    }

    public boolean isClaimable(MarketDAO.CourierProgressStats stats) {
        if (this == BEE) return true;
        if (stats == null) return false;
        return switch (this) {
            case BEE -> true;
            case ALLAY -> stats.exchangeTrades() >= 1;
            case PARROT -> stats.companyOrQuests() >= 1;
            case DRONE -> stats.remoteOrders() >= 10;
            case PHANTOM -> stats.shopSales() >= 100;
            case HEAVY_BEE -> stats.currentBalance() >= 200000.0;
        };
    }

    public double getProgress(MarketDAO.CourierProgressStats stats) {
        if (stats == null) return 0.0;
        return switch (this) {
            case BEE -> 1.0;
            case ALLAY -> stats.exchangeTrades();
            case PARROT -> stats.companyOrQuests();
            case DRONE -> stats.remoteOrders();
            case PHANTOM -> stats.shopSales();
            case HEAVY_BEE -> stats.currentBalance();
        };
    }

    public static CourierType fromId(String id) {
        if (id == null) return BEE;
        for (CourierType type : values()) {
            if (type.id.equalsIgnoreCase(id.trim())) {
                return type;
            }
        }
        return BEE;
    }
}
