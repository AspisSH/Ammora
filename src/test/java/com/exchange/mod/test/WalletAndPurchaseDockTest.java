package com.exchange.mod.test;

import com.exchange.mod.core.LedgerEntry;
import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.core.OMSManager;
import com.exchange.mod.core.P2PTransfer;
import com.exchange.mod.db.DatabaseManager;
import com.exchange.mod.db.MarketDAO;
import com.exchange.mod.db.PlayerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WalletAndPurchaseDockTest {

    private MarketDAO dao;
    private MarketManager marketManager;
    private UUID aliceUuid;
    private UUID bobUuid;

    @BeforeEach
    public void setup() throws Exception {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        OMSManager omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();

        aliceUuid = UUID.randomUUID();
        PlayerAccount alice = dao.getAccount(aliceUuid, "Alice");
        alice.setBalanceCbx(500.0);
        dao.saveAccount(alice);

        bobUuid = UUID.randomUUID();
        PlayerAccount bob = dao.getAccount(bobUuid, "Bob");
        bob.setBalanceCbx(100.0);
        dao.saveAccount(bob);

    }

    @Test
    @DisplayName("P2P transfer should transfer CBX and record ledger entries for both players")
    public void testP2PTransferSuccess() throws SQLException {
        var result = marketManager.executeP2PTransfer(aliceUuid, "Alice", bobUuid, "Bob", 200.0);
        assertTrue(result.success());
        assertEquals(200.0, result.cbxAmount());

        PlayerAccount alice = dao.getAccount(aliceUuid, "Alice");
        PlayerAccount bob = dao.getAccount(bobUuid, "Bob");

        assertEquals(300.0, alice.getBalanceCbx(), 0.001);
        assertEquals(300.0, bob.getBalanceCbx(), 0.001);

        // Verify transfer history
        List<P2PTransfer> transfers = dao.getPlayerTransfers(aliceUuid, 10);
        assertEquals(1, transfers.size());
        assertEquals(200.0, transfers.get(0).getAmount());
        assertEquals("Alice", transfers.get(0).getFromName());
        assertEquals("Bob", transfers.get(0).getToName());

        // Verify unified ledger
        List<LedgerEntry> aliceLedger = dao.getPlayerLedger(aliceUuid, 10);
        assertFalse(aliceLedger.isEmpty());
        assertEquals("P2P_OUT", aliceLedger.get(0).getType());
        assertEquals(-200.0, aliceLedger.get(0).getAmountCbx(), 0.001);

        List<LedgerEntry> bobLedger = dao.getPlayerLedger(bobUuid, 10);
        assertFalse(bobLedger.isEmpty());
        assertEquals("P2P_IN", bobLedger.get(0).getType());
        assertEquals(200.0, bobLedger.get(0).getAmountCbx(), 0.001);
    }

    @Test
    @DisplayName("P2P transfer should reject insufficient balance")
    public void testP2PTransferInsufficientFunds() throws SQLException {
        var result = marketManager.executeP2PTransfer(aliceUuid, "Alice", bobUuid, "Bob", 9999.0);
        assertFalse(result.success());
        assertTrue(result.message().contains("Insufficient") || result.message().contains("Недостаточно"));

        PlayerAccount alice = dao.getAccount(aliceUuid, "Alice");
        PlayerAccount bob = dao.getAccount(bobUuid, "Bob");
        assertEquals(500.0, alice.getBalanceCbx(), 0.001);
        assertEquals(100.0, bob.getBalanceCbx(), 0.001);
    }

    @Test
    @DisplayName("P2P transfer should reject self-transfers and invalid amounts")
    public void testP2PTransferInvalidOperations() throws SQLException {
        var selfResult = marketManager.executeP2PTransfer(aliceUuid, "Alice", aliceUuid, "Alice", 50.0);
        assertFalse(selfResult.success());
        assertTrue(selfResult.message().contains("yourself") || selfResult.message().contains("самому себе"));

        var zeroResult = marketManager.executeP2PTransfer(aliceUuid, "Alice", bobUuid, "Bob", 0.0);
        assertFalse(zeroResult.success());

        var negResult = marketManager.executeP2PTransfer(aliceUuid, "Alice", bobUuid, "Bob", -25.0);
        assertFalse(negResult.success());

        var nanResult = marketManager.executeP2PTransfer(aliceUuid, "Alice", bobUuid, "Bob", Double.NaN);
        assertFalse(nanResult.success());
    }

    @Test
    @DisplayName("Purchase Dock: Stop-High guard blocks purchase if price exceeds ceiling")
    public void testPurchaseDockStopHighGuard() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        assertNotNull(iron);
        iron.setCurrentStock(5000.0);
        dao.upsertMarket(iron);

        double currentBuyPrice = MarketEngine.calculateBuyPrice(iron.getCurrentStock(), iron);

        // Ceiling is set lower than current market buy price -> FAIL
        double lowCeiling = currentBuyPrice - 2.0;
        var blockedResult = marketManager.executeAutomatedPurchase(aliceUuid, "Alice", "minecraft:iron_ingot", 4, lowCeiling);
        assertFalse(blockedResult.success());
        assertTrue(blockedResult.message().contains("exceeds max buy limit"));

        // Ceiling is set higher than current market buy price -> SUCCESS
        double safeCeiling = currentBuyPrice + 10.0;
        var successResult = marketManager.executeAutomatedPurchase(aliceUuid, "Alice", "minecraft:iron_ingot", 4, safeCeiling);
        assertTrue(successResult.success());
        assertTrue(successResult.cbxAmount() > 0);

        PlayerAccount alice = dao.getAccount(aliceUuid, "Alice");
        assertTrue(alice.getBalanceCbx() < 500.0);
    }

    @Test
    @DisplayName("Purchase Dock: Investor Rank Level progression validates protection permissions")
    public void testPurchaseDockRankProgression() throws SQLException {
        PlayerAccount novice = dao.getAccount(UUID.randomUUID(), "NovicePlayer");
        assertEquals(1, novice.getRepLevel());

        // Level 2 Trader requires 500 REP
        novice.addRepPoints(500);
        assertEquals(2, novice.getRepLevel());

        // Level 3 Broker requires 2500 REP (Unlocks Stop-High price ceiling protection)
        novice.addRepPoints(2000);
        assertEquals(3, novice.getRepLevel());

        // Level 4 Investor requires 10000 REP (Unlocks bulk wholesale batch 32-64)
        novice.addRepPoints(7500);
        assertEquals(4, novice.getRepLevel());

        // Level 5 Whale requires 50000 REP
        novice.addRepPoints(40000);
        assertEquals(5, novice.getRepLevel());
    }
}
