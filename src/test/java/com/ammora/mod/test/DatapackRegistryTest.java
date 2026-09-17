package com.ammora.mod.test;

import com.ammora.mod.config.AmmoraConfig;
import com.ammora.mod.core.DeliveryContract;
import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.OMSManager;
import com.ammora.mod.core.events.MarketEvent;
import com.ammora.mod.core.events.MarketEventManager;
import com.ammora.mod.datapack.ContractTemplate;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatapackRegistryTest {

    private MarketManager marketManager;
    private MarketDAO dao;
    private MarketEventManager marketEventManager;

    @BeforeEach
    public void setup() throws SQLException {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();
        marketEventManager = new MarketEventManager();
    }

    @Test
    @DisplayName("Verify built-in commodity datapack JSON files parse with valid parameters")
    public void testBuiltinCommodityFiles() {
        String[] commodities = {
                "iron_ingot", "gold_ingot", "diamond", "netherite_ingot",
                "copper_ingot", "redstone", "emerald", "lapis_lazuli"
        };

        for (String c : commodities) {
            String path = "/data/ammora/commodities/" + c + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            assertNotNull(is, "Built-in commodity file must exist in jar resources: " + path);

            JsonObject obj = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(obj.has("resourceId"), "Must have resourceId: " + c);
            assertTrue(obj.has("basePrice"), "Must have basePrice: " + c);
            assertTrue(obj.get("basePrice").getAsDouble() > 0, "basePrice must be positive: " + c);
            assertTrue(obj.has("targetReserve"), "Must have targetReserve: " + c);
            assertTrue(obj.get("targetReserve").getAsDouble() > 0, "targetReserve must be positive: " + c);
            assertTrue(obj.has("elasticity"), "Must have elasticity: " + c);
            double elasticity = obj.get("elasticity").getAsDouble();
            assertTrue(elasticity > 0 && elasticity < 1.0, "elasticity must be between 0 and 1: " + c);
        }
    }

    @Test
    @DisplayName("Verify built-in market event datapack JSON files parse with valid parameters")
    public void testBuiltinMarketEventFiles() {
        String[] events = {
                "gold_rush", "diamond_collapse", "construction_boom", "netherite_discovery",
                "redstone_automation", "emerald_embargo", "lapis_enchantment", "copper_demand"
        };

        for (String ev : events) {
            String path = "/data/ammora/market_events/" + ev + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            assertNotNull(is, "Built-in event file must exist in jar resources: " + path);

            JsonObject obj = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(obj.has("id"), "Must have id: " + ev);
            assertTrue(obj.has("priceMultiplier"), "Must have priceMultiplier: " + ev);
            assertNotEquals(0.0, obj.get("priceMultiplier").getAsDouble(), "priceMultiplier must not be 0: " + ev);
            assertTrue(obj.has("durationDays"), "Must have durationDays: " + ev);
            assertTrue(obj.get("durationDays").getAsInt() >= 1, "durationDays must be at least 1: " + ev);
            assertTrue(obj.has("titleKey"), "Must have titleKey: " + ev);
            assertTrue(obj.has("descriptionKey"), "Must have descriptionKey: " + ev);
        }
    }

    @Test
    @DisplayName("Verify built-in delivery contract datapack JSON files parse with valid parameters")
    public void testBuiltinDeliveryContractFiles() {
        String[] contracts = {
                "iron_supply", "gold_reserve", "diamond_strategic", "copper_industrial"
        };

        for (String ctr : contracts) {
            String path = "/data/ammora/delivery_contracts/" + ctr + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            assertNotNull(is, "Built-in contract file must exist in jar resources: " + path);

            JsonObject obj = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(obj.has("id"), "Must have id: " + ctr);
            assertTrue(obj.has("resourceId"), "Must have resourceId: " + ctr);
            assertTrue(obj.has("minAmount"), "Must have minAmount: " + ctr);
            assertTrue(obj.get("minAmount").getAsInt() > 0, "minAmount must be > 0: " + ctr);
            assertTrue(obj.has("pricePerUnit"), "Must have pricePerUnit: " + ctr);
            assertTrue(obj.get("pricePerUnit").getAsDouble() > 0, "pricePerUnit must be > 0: " + ctr);
            assertTrue(obj.has("collateralCbx"), "Must have collateralCbx: " + ctr);
            assertTrue(obj.has("requiredReputation"), "Must have requiredReputation: " + ctr);
        }
    }

    @Test
    @DisplayName("Dynamic contract templates in MarketManager should generate configured contracts")
    public void testDynamicContractGeneration() throws SQLException {
        ContractTemplate copperTemplate = new ContractTemplate(
                "copper_mega", "Mega Copper Delivery", "contract.ammora.copper_industrial.title",
                "minecraft:copper_ingot", 200, 200, 12.5, 300.0, 150
        );

        marketManager.setContractTemplates(List.of(copperTemplate));
        marketManager.generateDailyContracts();

        List<DeliveryContract> active = dao.getAvailableAndPlayerContracts(null);
        assertNotNull(active);
        boolean foundCopper = false;
        for (DeliveryContract c : active) {
            if ("minecraft:copper_ingot".equals(c.getResourceId())) {
                foundCopper = true;
                assertEquals(200, c.getTargetAmount());
                assertEquals(12.5, c.getGuaranteedUnitPrice(), 0.001);
                assertEquals(300.0, c.getCollateralCbx(), 0.001);
                assertEquals(150, c.getRewardRep());
            }
        }
        assertTrue(foundCopper, "Generated daily contracts must include template for copper_ingot");
    }

    @Test
    @DisplayName("Format display name logic works for various IDs")
    public void testCommodityNameFormatting() {
        assertEquals("Iron Ingot", formatName("minecraft:iron_ingot"));
        assertEquals("Pure Netherite Ingot", formatName("create:pure_netherite_ingot"));
        assertEquals("Redstone Dust", formatName("redstone_dust"));
    }

    private String formatName(String resourceId) {
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

    @Test
    @DisplayName("AmmoraConfig de-hardcoded helper methods provide safe fallbacks")
    public void testConfigFallbacks() {
        assertEquals(0.08, AmmoraConfig.getSurplusBurnRate(), 0.001);
        assertEquals(0.0005, AmmoraConfig.getDailyCarryFeeRate(), 0.00001);
        assertEquals(500, AmmoraConfig.getRankThreshold(2));
        assertEquals(2500, AmmoraConfig.getRankThreshold(3));
        assertEquals(10000, AmmoraConfig.getRankThreshold(4));
        assertEquals(50000, AmmoraConfig.getRankThreshold(5));

        // Vending capacity upgrades
        assertEquals(250.0, AmmoraConfig.getCapacityUpgradeCost(64), 0.01);
        assertEquals(500.0, AmmoraConfig.getCapacityUpgradeCost(128), 0.01);
        assertEquals(1000.0, AmmoraConfig.getCapacityUpgradeCost(256), 0.01);
        assertEquals(2500.0, AmmoraConfig.getCapacityUpgradeCost(512), 0.01);

        // Vending slot unlocks
        assertEquals(100.0, AmmoraConfig.getSlotUnlockCost(6), 0.01);
        assertEquals(200.0, AmmoraConfig.getSlotUnlockCost(7), 0.01);
        assertEquals(300.0, AmmoraConfig.getSlotUnlockCost(8), 0.01);
        assertEquals(400.0, AmmoraConfig.getSlotUnlockCost(9), 0.01);
        assertEquals(500.0, AmmoraConfig.getSlotUnlockCost(10), 0.01);

        // Satellite module
        assertEquals(500.0, AmmoraConfig.getSatelliteModuleCost(), 0.01);
    }
}
