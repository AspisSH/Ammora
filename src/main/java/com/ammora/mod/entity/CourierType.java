package com.ammora.mod.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Enumeration of available courier skins and delivery mob variants.
 */
public enum CourierType {
    BEE("BEE", 0.0, "courier.type.bee", "courier.type.bee.desc", () -> Items.HONEYCOMB),
    ALLAY("ALLAY", 500.0, "courier.type.allay", "courier.type.allay.desc", () -> Items.AMETHYST_SHARD),
    HEAVY_BEE("HEAVY_BEE", 1000.0, "courier.type.heavy_bee", "courier.type.heavy_bee.desc", () -> Items.HONEY_BLOCK),
    PHANTOM("PHANTOM", 2500.0, "courier.type.phantom", "courier.type.phantom.desc", () -> Items.PHANTOM_MEMBRANE);

    private final String id;
    private final double priceCbx;
    private final String nameKey;
    private final String descKey;
    private final Supplier<Item> iconSupplier;

    CourierType(String id, double priceCbx, String nameKey, String descKey, Supplier<Item> iconSupplier) {
        this.id = id;
        this.priceCbx = priceCbx;
        this.nameKey = nameKey;
        this.descKey = descKey;
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

    public Item getIconItem() {
        return iconSupplier.get();
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
