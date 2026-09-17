package com.ammora.mod.test;

import com.ammora.mod.core.Candle;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSPosition;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    private DatabaseManager dbManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        // Use in-memory SQLite database for fast unit testing
        dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
    }

    @Test
    @DisplayName("Should successfully insert and load market resources")
    public void testMarketPersistence() throws SQLException {
        MarketResource gold = new MarketResource(
                "minecraft:gold_ingot",
                "Gold Ingot",
                25.0,
                5000.0,
                4800.0,
                0.85,
                8000.0,
                20.0,
                0.02,
                0.20
        );

        dao.upsertMarket(gold);

        List<MarketResource> markets = dao.loadAllMarkets();
        assertEquals(1, markets.size());
        MarketResource loaded = markets.get(0);
        assertEquals("minecraft:gold_ingot", loaded.getResourceId());
        assertEquals(25.0, loaded.getBasePrice());
        assertEquals(4800.0, loaded.getCurrentStock());
    }

    @Test
    @DisplayName("Should handle player accounts and broker progression")
    public void testAccountManagement() throws SQLException {
        UUID playerUuid = UUID.randomUUID();
        PlayerAccount account = dao.getAccount(playerUuid, "Steve");
        assertNotNull(account);
        assertEquals(100.0, account.getBalanceCbx());
        assertEquals(1, account.getRepLevel());

        account.deposit(500.0);
        account.addRepPoints(3000); // Should promote to Broker (Level 3)
        dao.saveAccount(account);

        PlayerAccount reloaded = dao.getAccount(playerUuid, "Steve");
        assertEquals(600.0, reloaded.getBalanceCbx());
        assertEquals(3, reloaded.getRepLevel());
        assertEquals(0.015, reloaded.getBrokerFeeRate());
    }

    @Test
    @DisplayName("Should store and retrieve candlesticks in chronological order")
    public void testCandlestickStorage() throws SQLException {
        String resId = "minecraft:diamond";
        long now = System.currentTimeMillis();

        Candle c1 = new Candle(now - 2000, 100.0, 105.0, 98.0, 102.0, 50);
        Candle c2 = new Candle(now - 1000, 102.0, 110.0, 101.0, 108.0, 80);
        Candle c3 = new Candle(now, 108.0, 109.0, 104.0, 105.0, 30);

        dao.recordCandle(resId, "1d", c1);
        dao.recordCandle(resId, "1d", c2);
        dao.recordCandle(resId, "1d", c3);

        List<Candle> recent = dao.getRecentCandles(resId, "1d", 10);
        assertEquals(3, recent.size());
        assertEquals(102.0, recent.get(0).getClose());
        assertEquals(105.0, recent.get(2).getClose());
    }

    @Test
    @DisplayName("Should aggregate live trade prices into candle buckets")
    public void testRecordTradePrice() throws SQLException {
        String resId = "minecraft:iron_ingot";
        // Record first trade
        dao.recordTradePrice(resId, "1d", 10.0, 10.0, 30000L);
        // Record second trade in same bucket (higher price, more volume)
        dao.recordTradePrice(resId, "1d", 12.0, 15.0, 30000L);
        // Record third trade in same bucket (lower price)
        dao.recordTradePrice(resId, "1d", 9.0, 5.0, 30000L);

        List<Candle> candles = dao.getRecentCandles(resId, "1d", 10);
        assertEquals(1, candles.size());
        Candle current = candles.get(0);
        assertEquals(10.0, current.getOpen());
        assertEquals(12.0, current.getHigh());
        assertEquals(9.0, current.getLow());
        assertEquals(9.0, current.getClose());
        assertEquals(30.0, current.getVolume());
    }

    @Test
    @DisplayName("Should persist and manage OMS positions")
    public void testOMSPositions() throws SQLException {
        UUID playerUuid = UUID.randomUUID();
        String posId = UUID.randomUUID().toString();

        OMSPosition pos = new OMSPosition(
                posId,
                playerUuid,
                "minecraft:iron_ingot",
                100.0,
                1000.0,
                10.0,
                System.currentTimeMillis()
        );

        dao.saveOMSPosition(pos);

        List<OMSPosition> positions = dao.getOMSPositions(playerUuid);
        assertEquals(1, positions.size());
        assertEquals(100.0, positions.get(0).getAmountUnits());

        dao.deleteOMSPosition(posId);
        assertTrue(dao.getOMSPositions(playerUuid).isEmpty());
    }

    @Test
    @DisplayName("Should persist, update amount, and retrieve unclaimed deliveries with NBT")
    public void testUnclaimedDeliveries() throws SQLException {
        UUID playerUuid = UUID.randomUUID();
        String delId = UUID.randomUUID().toString();

        dao.saveUnclaimedDelivery(delId, playerUuid, "minecraft:diamond_sword", 5, System.currentTimeMillis(), "{Damage:10}");

        List<MarketDAO.UnclaimedDelivery> list = dao.getUnclaimedDeliveries(playerUuid);
        assertEquals(1, list.size());
        assertEquals("minecraft:diamond_sword", list.get(0).resourceId());
        assertEquals(5, list.get(0).amount());
        assertEquals("{Damage:10}", list.get(0).itemNbt());

        dao.updateUnclaimedDeliveryAmount(delId, 2);
        List<MarketDAO.UnclaimedDelivery> updated = dao.getUnclaimedDeliveries(playerUuid);
        assertEquals(2, updated.get(0).amount());

        dao.deleteUnclaimedDelivery(delId);
        assertTrue(dao.getUnclaimedDeliveries(playerUuid).isEmpty());
    }
}
