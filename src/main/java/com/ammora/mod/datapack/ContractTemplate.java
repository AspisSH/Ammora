package com.ammora.mod.datapack;

import net.minecraft.network.chat.Component;

/**
 * Template definition for daily state delivery contracts loaded from datapacks.
 */
public record ContractTemplate(
        String id,
        String title,
        String titleKey,
        String resourceId,
        int minAmount,
        int maxAmount,
        double pricePerUnit,
        double collateralCbx,
        int requiredReputation
) {
    public String getDisplayTitle() {
        if (titleKey != null && !titleKey.isEmpty()) {
            try {
                String localized = Component.translatable(titleKey).getString();
                if (!localized.equals(titleKey)) return localized;
            } catch (Throwable ignored) {}
        }
        return (title != null && !title.isEmpty()) ? title : id;
    }
}
