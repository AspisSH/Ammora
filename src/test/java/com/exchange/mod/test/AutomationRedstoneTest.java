package com.exchange.mod.test;

import com.exchange.mod.core.MarketEngine;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AutomationRedstoneTest {

    private MarketDAO dao;
    private MarketManager marketManager;
    private UUID playerUuid;

    @BeforeEach
    public void setup() throws Exception {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();

        playerUuid = UUID.randomUUID();
        PlayerAccount account = dao.getAccount(playerUuid, "TestEngineer");
        account.deposit(1000.0);
        dao.saveAccount(account);
    }

    @Test
    @DisplayName("Redstone Mode 0 (Warehouse Fill) should emit proportional 0-15 signal")
    public void testRedstoneWarehouseMode() {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertNotNull(iron);
        iron.setMaxReserve(15000.0);

        // Empty warehouse: 0
        iron.setCurrentStock(0.0);
        assertEquals(0, MarketEngine.calculateRedstoneSignal(0, iron, 0.0, false));

        // Half filled (7500 / 15000): 8 / 15
        iron.setCurrentStock(7500.0);
        assertEquals(8, MarketEngine.calculateRedstoneSignal(0, iron, 0.0, false));

        // Full warehouse (15000 / 15000): 15 / 15
        iron.setCurrentStock(15000.0);
        assertEquals(15, MarketEngine.calculateRedstoneSignal(0, iron, 0.0, false));
    }

    @Test
    @DisplayName("Redstone Mode 1 (Price Ratio) should reflect spot price vs base price")
    public void testRedstonePriceRatioMode() {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertNotNull(iron);

        // Spot = Base (12.0), ratio = 12.0 / 24.0 = 0.5 -> round(0.5 * 15) = 8
        iron.setCurrentStock(iron.getTargetReserve());
        assertEquals(8, MarketEngine.calculateRedstoneSignal(1, iron, 0.0, false));

        // Low stock causes spot price to double to 24.0 -> ratio = 1.0 -> 15
        iron.setCurrentStock(iron.getTargetReserve() * 0.42);
        assertTrue(MarketEngine.calculateRedstoneSignal(1, iron, 0.0, false) >= 14);
    }

    @Test
    @DisplayName("Redstone Mode 2 (Threshold Trigger) should emit 15 (ON) or 0 (OFF)")
    public void testRedstoneThresholdTrigger() {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertNotNull(iron);
        iron.setCurrentStock(iron.getTargetReserve()); // Spot = 12.0, sell price ~ 11.76

        // Condition: Sell Price < $7.0 (Farm Stop-Loss trigger)
        // Current sell price is ~ 11.76 -> Condition 11.76 < 7.0 is FALSE -> Signal 0
        assertEquals(0, MarketEngine.calculateRedstoneSignal(2, iron, 7.0, true));

        // Oversupply flood: price crashes down to $4.50 -> Condition 4.50 < 7.0 is TRUE -> Signal 15!
        iron.setCurrentStock(25000.0);
        double crashedSellPrice = MarketEngine.calculateSellPrice(iron.getCurrentStock(), iron);
        assertTrue(crashedSellPrice < 7.0);
        assertEquals(15, MarketEngine.calculateRedstoneSignal(2, iron, 7.0, true),
                "Threshold condition met: should output full redstone power (15)");

        // Condition: Price > $10.0
        // Crashed price is 4.50 -> Condition 4.50 > 10.0 is FALSE -> 0
        assertEquals(0, MarketEngine.calculateRedstoneSignal(2, iron, 10.0, false));

        // Reset stock to normal (price ~ 11.76) -> Condition 11.76 > 10.0 is TRUE -> 15
        iron.setCurrentStock(iron.getTargetReserve());
        assertEquals(15, MarketEngine.calculateRedstoneSignal(2, iron, 10.0, false));
    }

    @Test
    @DisplayName("Automated Purchase should execute, debiting CBX and reducing exchange stock")
    public void testAutomatedPurchaseExecution() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();
        double initialBalance = dao.getAccount(playerUuid, "TestEngineer").getBalanceCbx();

        MarketManager.MarketTransactionResult result = marketManager.executeAutomatedPurchase(
                playerUuid, "TestEngineer", "minecraft:iron_ingot", 4, 25.0
        );

        assertTrue(result.success(), "Automated purchase should succeed with sufficient funds and stock");
        assertEquals(initialStock - 4, iron.getCurrentStock(), "Exchange stock must be reduced by 4");

        PlayerAccount accountAfter = dao.getAccount(playerUuid, "TestEngineer");
        assertTrue(accountAfter.getBalanceCbx() < initialBalance, "Balance must be debited");
        assertTrue(result.cbxAmount() > 0, "Transaction cost must be recorded");
    }

    @Test
    @DisplayName("Automated Purchase Stop-High guard should block purchase if spot price exceeds maximum limit")
    public void testAutomatedPurchaseStopHighGuard() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();
        double initialBalance = dao.getAccount(playerUuid, "TestEngineer").getBalanceCbx();

        // Unit buy price is ~12.24 CBX. Setting limit to 10.0 CBX must trigger Stop-High guard.
        MarketManager.MarketTransactionResult result = marketManager.executeAutomatedPurchase(
                playerUuid, "TestEngineer", "minecraft:iron_ingot", 1, 10.0
        );

        assertFalse(result.success(), "Purchase must be blocked when unit buy price exceeds maxBuyPrice");
        assertTrue(result.message().contains("exceeds max buy limit"), "Failure message should mention max buy limit");

        // Verify no funds withdrawn and no stock changed
        assertEquals(initialStock, iron.getCurrentStock());
        assertEquals(initialBalance, dao.getAccount(playerUuid, "TestEngineer").getBalanceCbx());
    }

    @Test
    @DisplayName("Automated Purchase should fail gracefully if player has insufficient CBX")
    public void testAutomatedPurchaseInsufficientFunds() throws SQLException {
        UUID poorPlayer = UUID.randomUUID();
        PlayerAccount poorAcc = dao.getAccount(poorPlayer, "PoorPlayer");
        dao.saveAccount(poorAcc); // 0 CBX

        MarketManager.MarketTransactionResult result = marketManager.executeAutomatedPurchase(
                poorPlayer, "PoorPlayer", "minecraft:diamond", 1, 500.0
        );

        assertFalse(result.success(), "Purchase must fail when balance is insufficient");
        assertTrue(result.message().contains("Insufficient CBX"), "Failure message should mention insufficient balance");
    }
}

