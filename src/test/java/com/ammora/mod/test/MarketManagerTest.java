package com.ammora.mod.test;

import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSManager;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MarketManagerTest {

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
        PlayerAccount acc = new PlayerAccount(playerUuid, "TraderSteve", 1000.0, 0, 1, System.currentTimeMillis());
        dao.saveAccount(acc);
    }

    @Test
    @DisplayName("Should execute valid buy order and update stock and balance")
    public void testExecuteBuy() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();

        MarketManager.MarketTransactionResult res = marketManager.executeBuy(playerUuid, "TraderSteve", "minecraft:iron_ingot", 64);
        assertTrue(res.success());
        assertTrue(res.cbxAmount() > 0);

        assertEquals(initialStock - 64, iron.getCurrentStock());

        PlayerAccount acc = dao.getAccount(playerUuid, "TraderSteve");
        assertEquals(1000.0 - res.cbxAmount(), acc.getBalanceCbx(), 0.01);
    }

    @Test
    @DisplayName("Should execute valid sell order and deposit payout")
    public void testExecuteSell() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();

        MarketManager.MarketTransactionResult res = marketManager.executeSell(playerUuid, "TraderSteve", "minecraft:iron_ingot", 64);
        assertTrue(res.success());
        assertTrue(res.cbxAmount() > 0);

        assertEquals(initialStock + 64, iron.getCurrentStock());

        PlayerAccount acc = dao.getAccount(playerUuid, "TraderSteve");
        assertEquals(1000.0 + res.cbxAmount(), acc.getBalanceCbx(), 0.01);
    }

    @Test
    @DisplayName("Negative pricing should debit player balance or reject if balance is too low")
    public void testNegativePricingExecution() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        // Artificially flood the warehouse far past S_max (15000)
        iron.setCurrentStock(25000.0);
        dao.upsertMarket(iron);

        // Player with 5000 CBX sells 100 items into negative market
        PlayerAccount richAcc = dao.getAccount(playerUuid, "TraderSteve");
        richAcc.setBalanceCbx(5000.0);
        dao.saveAccount(richAcc);

        MarketManager.MarketTransactionResult res = marketManager.executeSell(playerUuid, "TraderSteve", "minecraft:iron_ingot", 100);
        assertTrue(res.success(), "Transaction should succeed with message: " + res.message());
        assertTrue(res.cbxAmount() < 0, "Payout must be negative");

        PlayerAccount acc = dao.getAccount(playerUuid, "TraderSteve");
        // Balance decreased by disposal fee
        assertEquals(5000.0 - Math.abs(res.cbxAmount()), acc.getBalanceCbx(), 0.01);


        // Now test poor player with only 5 CBX
        UUID poorPlayer = UUID.randomUUID();
        PlayerAccount poorAcc = new PlayerAccount(poorPlayer, "BrokeSteve", 5.0, 0, 1, System.currentTimeMillis());
        dao.saveAccount(poorAcc);

        MarketManager.MarketTransactionResult failRes = marketManager.executeSell(poorPlayer, "BrokeSteve", "minecraft:iron_ingot", 100);
        assertFalse(failRes.success(), "Must reject dump if player cannot afford disposal bill");
        assertTrue(failRes.message().contains("Negative price"));
    }

    @Test
    @DisplayName("Should have all 8 default commodities registered and tradable")
    public void testAllCommoditiesInitializedAndTradable() {
        String[] expectedIds = {
                "minecraft:iron_ingot",
                "minecraft:gold_ingot",
                "minecraft:diamond",
                "minecraft:netherite_ingot",
                "minecraft:copper_ingot",
                "minecraft:redstone",
                "minecraft:emerald",
                "minecraft:lapis_lazuli"
        };

        assertEquals(8, marketManager.getAllResources().size());

        for (String id : expectedIds) {
            MarketResource res = marketManager.getResource(id);
            assertNotNull(res, "Resource should exist: " + id);
            assertTrue(res.getBasePrice() > 0, "Base price must be positive for " + id);
            assertTrue(res.getCurrentStock() > 0, "Current stock must be positive for " + id);
            assertTrue(res.getMaxReserve() > res.getTargetReserve(), "Max reserve must be greater than target for " + id);
        }
    }
}
