package com.exchange.mod.core.events;

import net.minecraft.network.chat.Component;

/**
 * Model representing an active or template market event.
 * Supports both datapack-defined translation keys and literal titles/descriptions.
 */
public class MarketEvent {
    private final String id;
    private final String title;
    private final String titleKey;
    private final String description;
    private final String descriptionKey;
    private final String affectedResourceId; // e.g. "minecraft:gold_ingot" or "*"
    private final double priceMultiplier;    // e.g. -0.25 (-25%) or +0.35 (+35%)
    private final int weight;
    private int remainingDays;

    public MarketEvent(String id, String title, String description, String affectedResourceId, double priceMultiplier, int remainingDays) {
        this(id, title, "", description, "", affectedResourceId, priceMultiplier, 10, remainingDays);
    }

    public MarketEvent(String id, String title, String titleKey, String description, String descriptionKey,
                       String affectedResourceId, double priceMultiplier, int weight, int remainingDays) {
        this.id = id;
        this.title = title != null ? title : "";
        this.titleKey = titleKey != null ? titleKey : "";
        this.description = description != null ? description : "";
        this.descriptionKey = descriptionKey != null ? descriptionKey : "";
        this.affectedResourceId = affectedResourceId;
        this.priceMultiplier = priceMultiplier;
        this.weight = weight > 0 ? weight : 10;
        this.remainingDays = remainingDays;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        if (titleKey != null && !titleKey.isEmpty()) {
            try {
                String localized = Component.translatable(titleKey).getString();
                if (!localized.equals(titleKey)) return localized;
            } catch (Throwable ignored) {}
        }
        return (title != null && !title.isEmpty()) ? title : id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getDescription() {
        if (descriptionKey != null && !descriptionKey.isEmpty()) {
            try {
                String localized = Component.translatable(descriptionKey).getString();
                if (!localized.equals(descriptionKey)) return localized;
            } catch (Throwable ignored) {}
        }
        return description != null ? description : "";
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getAffectedResourceId() {
        return affectedResourceId;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public int getWeight() {
        return weight;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }

    public boolean isExpired() {
        return remainingDays <= 0;
    }
}
