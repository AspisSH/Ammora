package com.ammora.mod.test;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSManager;
import com.ammora.mod.core.events.MarketEvent;
import com.ammora.mod.core.events.MarketEventManager;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicEconomyTest {

    private MarketResource ironResource;
    private MarketManager marketManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        ironResource = new MarketResource(
                "minecraft:iron_ingot",
                "Iron Ingot",
                12.0,
                10000.0,
                10000.0,
                0.8,
                15000.0,
                15.0,
                0.02,
                0.10
        );

        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();
    }

    @Test
    @DisplayName("Daily price fluctuation should shift effective base price and spot price")
    public void testDailyFluctuations() {
        assertEquals(12.0, ironResource.getEffectiveBasePrice(), 0.001);
        double initialSpot = MarketEngine.calculateSpotPrice(10000.0, ironResource);
        assertEquals(12.0, initialSpot, 0.01);

        // Bullish day (+8.5%)
        ironResource.setDailyModifier(0.085);
        assertEquals(12.0 * 1.085, ironResource.getEffectiveBasePrice(), 0.01);

        double bullishSpot = MarketEngine.calculateSpotPrice(10000.0, ironResource);
        assertEquals(initialSpot * 1.085, bullishSpot, 0.01);

        // Bearish day (-7.0%)
        ironResource.setDailyModifier(-0.070);
        assertEquals(12.0 * 0.930, ironResource.getEffectiveBasePrice(), 0.01);

        double bearishSpot = MarketEngine.calculateSpotPrice(10000.0, ironResource);
        assertEquals(initialSpot * 0.930, bearishSpot, 0.01);
    }

    @Test
    @DisplayName("Market event should apply correct price modifier to affected asset")
    public void testMarketEventApplication() {
        MarketResource gold = marketManager.getResource("minecraft:gold_ingot");
        assertNotNull(gold);
        double baseGoldPrice = gold.getBasePrice();

        MarketEventManager eventManager = new MarketEventManager();
        MarketEvent goldRush = new MarketEvent(
                "GOLD_RUSH",
                "Золотая лихорадка",
                "Цена падает на 25%",
                "minecraft:gold_ingot",
                -0.25,
                2
        );

        eventManager.setActiveEvent(goldRush, marketManager);
        assertEquals(-0.25, gold.getEventModifier(), 0.001);
        assertEquals(baseGoldPrice * 0.75, gold.getEffectiveBasePrice(), 0.01);

        // Other assets should remain unaffected by this event
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertEquals(0.0, iron.getEventModifier(), 0.001);
    }

    @Test
    @DisplayName("Daily modifier and event modifier should stack multiplicatively/additively")
    public void testCombinedModifiers() {
        ironResource.setDailyModifier(0.10);  // +10% daily trend
        ironResource.setEventModifier(-0.20); // -20% event

        // Net multiplier is 1.0 + 0.10 - 0.20 = 0.90
        assertEquals(12.0 * 0.90, ironResource.getEffectiveBasePrice(), 0.01);
    }

    @Test
    @DisplayName("Market event lifecycle: ticks, decrements and cleans up on expiration")
    public void testEventLifecycle() {
        MarketEventManager eventManager = new MarketEventManager();
        MarketEvent event = new MarketEvent(
                "TEST_EVENT",
                "Тест",
                "Тестовое описание",
                "minecraft:iron_ingot",
                0.30,
                2
        );

        eventManager.setActiveEvent(event, marketManager);
        assertEquals(2, eventManager.getActiveEvent().getRemainingDays());

        // Day 1 passes: event remaining days decremented to 1
        String msg1 = eventManager.onDayChanged(marketManager, true, 0.0, 2);
        assertNull(msg1);
        assertEquals(1, eventManager.getActiveEvent().getRemainingDays());
        assertEquals(0.30, marketManager.getResource("minecraft:iron_ingot").getEventModifier(), 0.001);

        // Day 2 passes: event expires and clean up message returned
        String msg2 = eventManager.onDayChanged(marketManager, true, 0.0, 2);
        assertNotNull(msg2);
        assertTrue(msg2.contains("ended") || msg2.contains("завершилось") || msg2.contains("event.expired"));
        assertNull(eventManager.getActiveEvent());
        assertEquals(0.0, marketManager.getResource("minecraft:iron_ingot").getEventModifier(), 0.001);
    }

    @Test
    @DisplayName("SQLite should persist and restore active market events")
    public void testActiveEventPersistence() throws SQLException {
        MarketEvent event = new MarketEvent(
                "DIAMOND_COLLAPSE",
                "Обвал в шахтах",
                "Дефицит алмазов",
                "minecraft:diamond",
                0.35,
                3
        );

        dao.saveActiveEvent(event);

        MarketEvent loaded = dao.loadActiveEvent();
        assertNotNull(loaded);
        assertEquals("DIAMOND_COLLAPSE", loaded.getId());
        assertEquals("Обвал в шахтах", loaded.getTitle());
        assertEquals("minecraft:diamond", loaded.getAffectedResourceId());
        assertEquals(0.35, loaded.getPriceMultiplier(), 0.001);
        assertEquals(3, loaded.getRemainingDays());

        // Clear active event
        dao.saveActiveEvent(null);
        assertNull(dao.loadActiveEvent());
    }

    @Test
    @DisplayName("Daily fluctuations should generate 4 progressive candles smoothly transitioning to new price")
    public void testDailyTransitionCandlesProgression() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertNotNull(iron);

        int initialCandleCount = dao.getRecentCandles("minecraft:iron_ingot", "1d", 100).size();
        double oldPrice = MarketEngine.calculateSpotPrice(iron.getCurrentStock(), iron);
        assertEquals(12.0, oldPrice, 0.01);

        // Daily fluctuation +10%
        iron.setDailyModifier(0.10);
        double targetNewPrice = MarketEngine.calculateSpotPrice(iron.getCurrentStock(), iron);
        assertEquals(13.20, targetNewPrice, 0.01);

        marketManager.recordDailyTransitionCandles(iron, oldPrice, targetNewPrice);

        var recentCandles = dao.getRecentCandles("minecraft:iron_ingot", "1d", 100);
        assertEquals(initialCandleCount + 4, recentCandles.size());

        // Extract the 4 newly generated transition candles
        var step1 = recentCandles.get(recentCandles.size() - 4);
        var step2 = recentCandles.get(recentCandles.size() - 3);
        var step3 = recentCandles.get(recentCandles.size() - 2);
        var step4 = recentCandles.get(recentCandles.size() - 1);

        // Timestamps must be strictly increasing
        assertTrue(step1.getTimestamp() < step2.getTimestamp());
        assertTrue(step2.getTimestamp() < step3.getTimestamp());
        assertTrue(step3.getTimestamp() < step4.getTimestamp());

        // Progression must move upwards towards targetNewPrice
        assertEquals(oldPrice, step1.getOpen(), 0.01);
        assertTrue(step1.getClose() > oldPrice);
        assertTrue(step2.getClose() > step1.getOpen());
        assertTrue(step3.getClose() > step2.getOpen());

        // Final candle must close exactly at targetNewPrice
        assertEquals(targetNewPrice, step4.getClose(), 0.001);

        // Volume must be in realistic daily range (200 - 500)
        for (var c : java.util.List.of(step1, step2, step3, step4)) {
            assertTrue(c.getVolume() >= 200.0 && c.getVolume() <= 500.0, "Volume should be in 200-500 range, was: " + c.getVolume());
        }
    }

    @Test
    @DisplayName("Market event should generate 1 sharp impulse candle with high volume surge")
    public void testMarketEventSharpCandle() throws SQLException {
        MarketResource gold = marketManager.getResource("minecraft:gold_ingot");
        assertNotNull(gold);
        int initialCandleCount = dao.getRecentCandles("minecraft:gold_ingot", "1d", 100).size();

        MarketEventManager eventManager = new MarketEventManager();
        MarketEvent goldRush = new MarketEvent(
                "GOLD_RUSH",
                "Золотая лихорадка",
                "Цена падает на 25%",
                "minecraft:gold_ingot",
                -0.25,
                2
        );

        eventManager.setActiveEvent(goldRush, marketManager);

        var recentCandles = dao.getRecentCandles("minecraft:gold_ingot", "1d", 100);
        // Exactly 1 new candle added
        assertEquals(initialCandleCount + 1, recentCandles.size());

        var eventCandle = recentCandles.get(recentCandles.size() - 1);
        assertEquals(40.0, eventCandle.getOpen(), 0.01);
        assertEquals(30.0, eventCandle.getClose(), 0.01);
        assertTrue(eventCandle.getHigh() >= 40.0, "High should include wick above open");
        assertTrue(eventCandle.getLow() <= 30.0, "Low should include panic dip below close");

        // High volume surge (1500 - 2500)
        assertTrue(eventCandle.getVolume() >= 1500.0, "Event volume should reflect surge >= 1500, was: " + eventCandle.getVolume());
    }

    @Test
    @DisplayName("Event expiration should record 1 stabilization candle restoring price")
    public void testEventExpirationCandle() throws SQLException {
        MarketEventManager eventManager = new MarketEventManager();
        MarketEvent diamondCollapse = new MarketEvent(
                "DIAMOND_COLLAPSE",
                "Обвал шахт",
                "Алмазы взлетели на +35%",
                "minecraft:diamond",
                0.35,
                1
        );

        eventManager.setActiveEvent(diamondCollapse, marketManager);
        var candlesAfterEvent = dao.getRecentCandles("minecraft:diamond", "1d", 100);
        int countWithEvent = candlesAfterEvent.size();

        // Expire event
        String expireMsg = eventManager.onDayChanged(marketManager, true, 0.0, 1);
        assertNotNull(expireMsg);
        assertTrue(expireMsg.contains("ended") || expireMsg.contains("завершилось") || expireMsg.contains("event.expired"));

        var candlesAfterExpire = dao.getRecentCandles("minecraft:diamond", "1d", 100);
        assertEquals(countWithEvent + 1, candlesAfterExpire.size());

        var expireCandle = candlesAfterExpire.get(candlesAfterExpire.size() - 1);
        // Pre-expiration price was ~ 350 * 1.35 = 472.5, post-expiration is 350.0
        assertEquals(350.0 * 1.35, expireCandle.getOpen(), 0.01);
        assertEquals(350.0, expireCandle.getClose(), 0.01);
        assertTrue(expireCandle.getVolume() >= 1500.0);
    }
}
