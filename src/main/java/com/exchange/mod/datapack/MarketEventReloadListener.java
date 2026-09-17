package com.exchange.mod.datapack;

import com.exchange.mod.core.events.MarketEvent;
import com.exchange.mod.core.events.MarketEventManager;
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
 * Loads economic event templates from datapacks located at data/<namespace>/market_events/*.json
 */
public class MarketEventReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<MarketEvent> loadedTemplates = new CopyOnWriteArrayList<>();
    private MarketEventManager marketEventManager;

    public MarketEventReloadListener() {
        super(GSON, "market_events");
    }

    public void setMarketEventManager(MarketEventManager marketEventManager) {
        this.marketEventManager = marketEventManager;
    }

    public List<MarketEvent> getLoadedTemplates() {
        return Collections.unmodifiableList(loadedTemplates);
    }

    public void applyTo(MarketEventManager mem) {
        this.marketEventManager = mem;
        if (!loadedTemplates.isEmpty()) {
            mem.setTemplatePool(loadedTemplates);
            LOGGER.info("[Exchange] Applied {} cached market event templates.", loadedTemplates.size());
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("[Exchange] Loading market events from datapacks... (Found: {} definitions)", resources.size());
        List<MarketEvent> parsed = new ArrayList<>();
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
                String titleKey = obj.has("titleKey") ? obj.get("titleKey").getAsString().trim() : "event.exchange." + id + ".title";
                String description = obj.has("description") ? obj.get("description").getAsString().trim() : "";
                String descriptionKey = obj.has("descriptionKey") ? obj.get("descriptionKey").getAsString().trim() : "event.exchange." + id + ".desc";
                String resourceId = obj.has("resourceId") ? obj.get("resourceId").getAsString().trim() : "*";
                double priceMultiplier = obj.has("priceMultiplier") ? obj.get("priceMultiplier").getAsDouble() : 0.0;
                int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 10;
                int defaultDuration = obj.has("durationDays") ? obj.get("durationDays").getAsInt() : 2;

                MarketEvent event = new MarketEvent(
                        id, title, titleKey, description, descriptionKey, resourceId, priceMultiplier, weight, defaultDuration
                );

                parsed.add(event);
                LOGGER.info("[Exchange] Loaded market event template: {} (Impact: {}% on {})", id, (int)(priceMultiplier * 100), resourceId);
            } catch (Exception e) {
                LOGGER.error("[Exchange] Error parsing market event JSON from {}: {}", fileId, e.getMessage());
            }
        }

        if (!parsed.isEmpty()) {
            loadedTemplates.clear();
            loadedTemplates.addAll(parsed);
            if (marketEventManager != null) {
                marketEventManager.setTemplatePool(parsed);
                LOGGER.info("[Exchange] Active template pool updated with {} events.", parsed.size());
            }
        }
    }
}
