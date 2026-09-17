package com.exchange.mod.test;

import com.exchange.mod.core.LimitOrder;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.core.OMSManager;
import com.exchange.mod.db.DatabaseManager;
import com.exchange.mod.db.MarketDAO;
import com.exchange.mod.db.PlayerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndConcurrencyTest {

    private MarketManager marketManager;
    private MarketDAO dao;
    private UUID playerUuid;

    @BeforeEach
    public void setup() throws SQLException {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();

        playerUuid = UUID.randomUUID();
        PlayerAccount acc = new PlayerAccount(playerUuid, "CyberTrader", 50000.0, 0, 1, System.currentTimeMillis());
        dao.saveAccount(acc);
    }

    @Test
    @DisplayName("Should reject invalid or malicious input parameters")
    public void testMaliciousInputsRejected() throws SQLException {
        // Zero or negative amounts
        var res1 = marketManager.executeBuy(playerUuid, "CyberTrader", "minecraft:iron_ingot", 0);
        assertFalse(res1.success());

        var res2 = marketManager.executeBuy(playerUuid, "CyberTrader", "minecraft:iron_ingot", -10);
        assertFalse(res2.success());

        var res3 = marketManager.executeSell(playerUuid, "CyberTrader", "minecraft:iron_ingot", 0);
        assertFalse(res3.success());

        var res4 = marketManager.executeSell(playerUuid, "CyberTrader", "minecraft:iron_ingot", -5);
        assertFalse(res4.success());

        // Invalid OMS amount
        var res5 = marketManager.executeOpenOMSPosition(playerUuid, "CyberTrader", "minecraft:iron_ingot", 0.0);
        assertFalse(res5.success());

        var res6 = marketManager.executeOpenOMSPosition(playerUuid, "CyberTrader", "minecraft:iron_ingot", -100.0);
        assertFalse(res6.success());

        // NaN and Infinite in limit orders
        var res7 = marketManager.placeLimitOrder(playerUuid, "CyberTrader", "minecraft:iron_ingot", LimitOrder.TYPE_BUY, 10, Double.NaN);
        assertFalse(res7.success());

        var res8 = marketManager.placeLimitOrder(playerUuid, "CyberTrader", "minecraft:iron_ingot", LimitOrder.TYPE_BUY, 10, Double.POSITIVE_INFINITY);
        assertFalse(res8.success());

        var res9 = marketManager.placeLimitOrder(playerUuid, "CyberTrader", "minecraft:iron_ingot", LimitOrder.TYPE_BUY, -5, 10.0);
        assertFalse(res9.success());

        // Non-existent resource
        var res10 = marketManager.executeBuy(playerUuid, "CyberTrader", "minecraft:bedrock", 10);
        assertFalse(res10.success());
    }

    @Test
    @DisplayName("Should maintain atomic consistency under concurrent multi-threaded trading")
    public void testConcurrentTradingConsistency() throws Exception {
        int threadCount = 8;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successBuys = new AtomicInteger(0);
        AtomicInteger successSells = new AtomicInteger(0);

        MarketResource copper = marketManager.getResource("minecraft:copper_ingot");
        double initialStock = copper.getCurrentStock();

        // Create 8 accounts with funds
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UUID id = UUID.randomUUID();
            players.add(id);
            dao.saveAccount(new PlayerAccount(id, "Trader" + i, 10000.0, 0, 1, System.currentTimeMillis()));
        }

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    UUID uid = players.get(threadIdx);
                    String name = "Trader" + threadIdx;

                    for (int i = 0; i < operationsPerThread; i++) {
                        if (i % 2 == 0) {
                            var bRes = marketManager.executeBuy(uid, name, "minecraft:copper_ingot", 2);
                            if (bRes.success()) successBuys.incrementAndGet();
                        } else {
                            var sRes = marketManager.executeSell(uid, name, "minecraft:copper_ingot", 2);
                            if (sRes.success()) successSells.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Check conservation of stock: delta = (sells - buys) * 2
        double expectedStock = initialStock + (successSells.get() - successBuys.get()) * 2;
        assertEquals(expectedStock, copper.getCurrentStock(), 0.001);
        assertTrue(copper.getCurrentStock() > 0);
    }
}
