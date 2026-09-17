package com.exchange.mod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Loads custom commodities (e.g. from Create, Mekanism, Thermal, Botania, etc.)
 * from config/exchange_custom_items.json into the SQLite database and AMM market engine.
 */
public class CustomMarketLoader {

    public static final String FILE_NAME = "exchange_custom_items.json";
    private static final Logger LOGGER = LoggerFactory.getLogger("Ammora");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getConfigFilePath() {
        try {
            Class<?> pathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            Object configDirObj = pathsClass.getField("CONFIGDIR").get(null);
            @SuppressWarnings("unchecked")
            Supplier<Path> supplier = (Supplier<Path>) configDirObj;
            return supplier.get().resolve(FILE_NAME);
        } catch (Throwable t) {
            return Paths.get("config", FILE_NAME);
        }
    }

    /**
     * Initializes and loads custom market items into MarketManager.
     */
    public static void loadCustomItems(MarketManager marketManager) {
        Path path = getConfigFilePath();
        try {
            if (!Files.exists(path)) {
                createDefaultTemplate(path);
                return;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonArray()) {
                    LOGGER.warn("[Exchange] {} must contain a JSON array of item objects!", FILE_NAME);
                    return;
                }

                JsonArray array = rootElement.getAsJsonArray();
                int loadedCount = 0;

                for (JsonElement elem : array) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject obj = elem.getAsJsonObject();

                    // Check enabled flag (default: true)
                    if (obj.has("enabled") && !obj.get("enabled").getAsBoolean()) {
                        continue;
                    }

                    if (!obj.has("resourceId")) {
                        LOGGER.warn("[Exchange] Custom market item skipped: missing 'resourceId'");
                        continue;
                    }

                    String resourceId = obj.get("resourceId").getAsString().trim();
                    if (resourceId.isEmpty()) continue;

                    String displayName = obj.has("displayName") ? obj.get("displayName").getAsString().trim() : "";
                    if (displayName.isEmpty()) {
                        displayName = generateDisplayName(resourceId);
                    }

                    double basePrice = obj.has("basePrice") ? obj.get("basePrice").getAsDouble() : 10.0;
                    if (basePrice <= 0.0) basePrice = 1.0;

                    double targetReserve = obj.has("targetReserve") ? obj.get("targetReserve").getAsDouble() : 5000.0;
                    if (targetReserve <= 0.0) targetReserve = 1000.0;

                    double currentStock = obj.has("currentStock") ? obj.get("currentStock").getAsDouble() : targetReserve;
                    if (currentStock <= 0.0) currentStock = targetReserve;

                    double elasticity = obj.has("elasticity") ? obj.get("elasticity").getAsDouble() : 0.85;
                    if (elasticity <= 0.0 || elasticity >= 1.0) elasticity = 0.85;

                    double maxReserve = obj.has("maxReserve") ? obj.get("maxReserve").getAsDouble() : (targetReserve * 1.6);
                    if (maxReserve <= targetReserve) maxReserve = targetReserve * 1.5;

                    double disposalAlpha = obj.has("disposalAlpha") ? obj.get("disposalAlpha").getAsDouble() : 15.0;
                    if (disposalAlpha <= 0.0) disposalAlpha = 15.0;

                    double feeRate = obj.has("feeRate") ? obj.get("feeRate").getAsDouble() : 0.02;
                    if (feeRate < 0.0 || feeRate > 0.5) feeRate = 0.02;

                    double minPriceFloor = obj.has("minPriceFloor") ? obj.get("minPriceFloor").getAsDouble() : Math.max(0.01, basePrice * 0.01);
                    if (minPriceFloor <= 0.0) minPriceFloor = 0.01;

                    MarketResource customRes = new MarketResource(
                            resourceId,
                            displayName,
                            basePrice,
                            targetReserve,
                            currentStock,
                            elasticity,
                            maxReserve,
                            disposalAlpha,
                            feeRate,
                            minPriceFloor
                    );

                    marketManager.registerCustomResource(customRes);
                    loadedCount++;
                    LOGGER.info("[Exchange] Loaded custom commodity: {} ({}) @ {} CBX",
                            displayName, resourceId, basePrice);
                }

                LOGGER.info("[Exchange] Successfully loaded {} custom items from {}", loadedCount, FILE_NAME);
            }
        } catch (Exception e) {
            LOGGER.error("[Exchange] Failed to load custom market items from {}: {}", FILE_NAME, e.getMessage(), e);
        }
    }

    private static String generateDisplayName(String resourceId) {
        String path = resourceId.contains(":") ? resourceId.substring(resourceId.indexOf(':') + 1) : resourceId;
        String[] parts = path.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static void createDefaultTemplate(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            JsonArray array = new JsonArray();

            // Create sample template items (disabled by default so server owner can inspect and activate)
            JsonObject createBrass = new JsonObject();
            createBrass.addProperty("resourceId", "create:brass_ingot");
            createBrass.addProperty("displayName", "Brass Ingot");
            createBrass.addProperty("basePrice", 28.0);
            createBrass.addProperty("targetReserve", 6000.0);
            createBrass.addProperty("currentStock", 6000.0);
            createBrass.addProperty("elasticity", 0.85);
            createBrass.addProperty("maxReserve", 10000.0);
            createBrass.addProperty("disposalAlpha", 20.0);
            createBrass.addProperty("feeRate", 0.02);
            createBrass.addProperty("minPriceFloor", 0.20);
            createBrass.addProperty("enabled", false);
            array.add(createBrass);

            JsonObject createZinc = new JsonObject();
            createZinc.addProperty("resourceId", "create:zinc_ingot");
            createZinc.addProperty("displayName", "Zinc Ingot");
            createZinc.addProperty("basePrice", 14.0);
            createZinc.addProperty("targetReserve", 12000.0);
            createZinc.addProperty("currentStock", 12000.0);
            createZinc.addProperty("elasticity", 0.80);
            createZinc.addProperty("maxReserve", 18000.0);
            createZinc.addProperty("disposalAlpha", 15.0);
            createZinc.addProperty("feeRate", 0.02);
            createZinc.addProperty("minPriceFloor", 0.10);
            createZinc.addProperty("enabled", false);
            array.add(createZinc);

            JsonObject mekanismSteel = new JsonObject();
            mekanismSteel.addProperty("resourceId", "mekanism:ingot_steel");
            mekanismSteel.addProperty("displayName", "Steel Ingot");
            mekanismSteel.addProperty("basePrice", 22.0);
            mekanismSteel.addProperty("targetReserve", 8000.0);
            mekanismSteel.addProperty("currentStock", 8000.0);
            mekanismSteel.addProperty("elasticity", 0.85);
            mekanismSteel.addProperty("maxReserve", 12000.0);
            mekanismSteel.addProperty("disposalAlpha", 18.0);
            mekanismSteel.addProperty("feeRate", 0.02);
            mekanismSteel.addProperty("minPriceFloor", 0.15);
            mekanismSteel.addProperty("enabled", false);
            array.add(mekanismSteel);

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }

            LOGGER.info("[Exchange] Generated default custom items template at {}", path.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("[Exchange] Failed to create default template {}: {}", FILE_NAME, e.getMessage());
        }
    }
}
