package com.exchange.mod.datapack;

import com.exchange.mod.core.MarketManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loads delivery contract templates from datapacks located at data/<namespace>/delivery_contracts/*.json
 */
public class ContractReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<ContractTemplate> loadedTemplates = new CopyOnWriteArrayList<>();
    private MarketManager marketManager;

    public ContractReloadListener() {
        super(GSON, "delivery_contracts");
    }

    public void setMarketManager(MarketManager marketManager) {
        this.marketManager = marketManager;
    }

    public List<ContractTemplate> getLoadedTemplates() {
        return Collections.unmodifiableList(loadedTemplates);
    }

    public void applyTo(MarketManager mm) {
        this.marketManager = mm;
        if (!loadedTemplates.isEmpty()) {
            mm.setContractTemplates(loadedTemplates);
            LOGGER.info("[Exchange] Applied {} cached contract templates to MarketManager.", loadedTemplates.size());
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("[Exchange] Loading delivery contracts from datapacks... (Found: {} definitions)", resources.size());
        List<ContractTemplate> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject obj = entry.getValue().getAsJsonObject();

                if (obj.has("enabled") && !obj.get("enabled").getAsBoolean()) {
                    continue;
                }

                String id = obj.has("id") ? obj.get("id").getAsString().trim() : fileId.getPath();
                String title = obj.has("title") ? obj.get("title").getAsString().trim() : "";
                String titleKey = obj.has("titleKey") ? obj.get("titleKey").getAsString().trim() : "contract.exchange." + id + ".title";
                String resourceId = obj.has("resourceId") ? obj.get("resourceId").getAsString().trim() : "minecraft:iron_ingot";

                int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 32;
                int minAmount = obj.has("minAmount") ? obj.get("minAmount").getAsInt() : amount;
                int maxAmount = obj.has("maxAmount") ? obj.get("maxAmount").getAsInt() : amount;
                if (maxAmount < minAmount) maxAmount = minAmount;

                double pricePerUnit = obj.has("pricePerUnit") ? obj.get("pricePerUnit").getAsDouble() : 15.0;
                double collateralCbx = obj.has("collateralCbx") ? obj.get("collateralCbx").getAsDouble() : 100.0;
                int requiredReputation = obj.has("requiredReputation") ? obj.get("requiredReputation").getAsInt() : 100;

                ContractTemplate template = new ContractTemplate(
                        id, title, titleKey, resourceId, minAmount, maxAmount, pricePerUnit, collateralCbx, requiredReputation
                );

                parsed.add(template);
                LOGGER.info("[Exchange] Loaded contract template: {} ({} of {}, payout: {} CBX/u)", id, minAmount, resourceId, pricePerUnit);
            } catch (Exception e) {
                LOGGER.error("[Exchange] Error parsing contract JSON from {}: {}", fileId, e.getMessage());
            }
        }

        if (!parsed.isEmpty()) {
            loadedTemplates.clear();
            loadedTemplates.addAll(parsed);
            if (marketManager != null) {
                marketManager.setContractTemplates(parsed);
                LOGGER.info("[Exchange] MarketManager contract templates updated with {} contracts.", parsed.size());
            }
        }
    }
}
