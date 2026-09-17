package com.exchange.mod.datapack;

import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads commodity definitions from datapacks located at data/<namespace>/commodities/*.json
 */
public class CommodityReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<String, MarketResource> loadedCommodities = new ConcurrentHashMap<>();
    private MarketManager marketManager;

    public CommodityReloadListener() {
        super(GSON, "commodities");
    }

    public void setMarketManager(MarketManager marketManager) {
        this.marketManager = marketManager;
    }

    public Map<String, MarketResource> getLoadedCommodities() {
        return Collections.unmodifiableMap(loadedCommodities);
    }

    public void applyTo(MarketManager mm) {
        this.marketManager = mm;
        for (MarketResource res : loadedCommodities.values()) {
            try {
                mm.registerCustomResource(res);
                LOGGER.info("[Exchange] Applied cached commodity: {} ({})", res.getResourceId(), res.getDisplayName());
            } catch (Exception e) {
                LOGGER.error("[Exchange] Error registering cached commodity {}: {}", res.getResourceId(), e.getMessage());
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("[Exchange] Loading commodities from datapacks... (Found: {} definitions)", resources.size());
        loadedCommodities.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject obj = entry.getValue().getAsJsonObject();

                if (obj.has("enabled") && !obj.get("enabled").getAsBoolean()) {
                    continue;
                }

                String resourceId = obj.has("resourceId") ? obj.get("resourceId").getAsString().trim() : "";
                if (resourceId.isEmpty()) {
                    resourceId = fileId.toString();
                }

                String displayName = obj.has("displayName") ? obj.get("displayName").getAsString().trim() : "";
                if (displayName.isEmpty()) {
                    displayName = formatDisplayName(resourceId);
                }

                double basePrice = obj.has("basePrice") ? obj.get("basePrice").getAsDouble() : 10.0;
                double targetReserve = obj.has("targetReserve") ? obj.get("targetReserve").getAsDouble() : 5000.0;
                double currentStock = obj.has("currentStock") ? obj.get("currentStock").getAsDouble() : targetReserve;
                double elasticity = obj.has("elasticity") ? obj.get("elasticity").getAsDouble() : 0.85;
                double maxReserve = obj.has("maxReserve") ? obj.get("maxReserve").getAsDouble() : (targetReserve * 1.5);
                double disposalAlpha = obj.has("disposalAlpha") ? obj.get("disposalAlpha").getAsDouble() : 15.0;
                double feeRate = obj.has("feeRate") ? obj.get("feeRate").getAsDouble() : 0.02;
                double minPriceFloor = obj.has("minPriceFloor") ? obj.get("minPriceFloor").getAsDouble() : 0.05;

                MarketResource res = new MarketResource(
                        resourceId, displayName, basePrice, currentStock, targetReserve,
                        elasticity, maxReserve, disposalAlpha, feeRate, minPriceFloor
                );

                loadedCommodities.put(resourceId, res);

                if (marketManager != null) {
                    marketManager.registerCustomResource(res);
                    LOGGER.info("[Exchange] Registered/updated commodity from datapack: {} ({})", resourceId, displayName);
                }
            } catch (Exception e) {
                LOGGER.error("[Exchange] Error parsing commodity JSON from {}: {}", fileId, e.getMessage());
            }
        }
    }

    public static String formatDisplayName(String resourceId) {
        String path = resourceId.contains(":") ? resourceId.split(":", 2)[1] : resourceId;
        String[] parts = path.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
