package com.ammora.mod.test;

import com.ammora.mod.core.DeliveryContract;
import com.ammora.mod.core.LimitOrder;
import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSManager;
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

public class DerivativesTest {

    private MarketManager marketManager;
    private MarketDAO dao;
    private OMSManager omsManager;
    private UUID playerUuid;
    private final String playerName = "TraderPro";

    @BeforeEach
    public void setup() throws SQLException {
        DatabaseManager dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
        omsManager = new OMSManager();
        marketManager = new MarketManager(dao, omsManager);
        marketManager.initialize();

        playerUuid = UUID.randomUUID();
        PlayerAccount acc = new PlayerAccount(playerUuid, playerName, 2000.0, 0, 1, System.currentTimeMillis());
        dao.saveAccount(acc);
    }

    // ==========================================
    // 1. Unallocated Metal Accounts (ОМС) Tests
    // ==========================================

    @Test
    @DisplayName("Should successfully open OMS position and deduct CBX")
    public void testOpenOMSPosition() throws SQLException {
        double investCbx = 300.0;
        MarketManager.MarketTransactionResult res = marketManager.executeOpenOMSPosition(
                playerUuid, playerName, "minecraft:iron_ingot", investCbx
        );

        assertTrue(res.success(), "OMS open should succeed");
        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertEquals(2000.0 - investCbx, acc.getBalanceCbx(), 0.01);

        List<OMSPosition> positions = omsManager.getPositions(playerUuid);
        assertEquals(1, positions.size());
        OMSPosition pos = positions.get(0);
        assertEquals("minecraft:iron_ingot", pos.getResourceId());
        assertTrue(pos.getAmountUnits() > 0);
        assertEquals(investCbx, pos.getInvestedCbx(), 0.01);
    }

    @Test
    @DisplayName("Should close OMS position with dynamic profit upon market price rise")
    public void testCloseOMSPositionWithProfit() throws SQLException {
        double investCbx = 200.0;
        marketManager.executeOpenOMSPosition(playerUuid, playerName, "minecraft:iron_ingot", investCbx);

        OMSPosition pos = omsManager.getPositions(playerUuid).get(0);
        double initialUnits = pos.getAmountUnits();

        // Simulate market price surge by reducing warehouse stock
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        iron.setCurrentStock(iron.getCurrentStock() * 0.3); // Severe scarcity -> price jumps
        dao.upsertMarket(iron);

        double pnl = pos.calculatePnL(iron);
        assertTrue(pnl > 0, "PnL should be positive after price surge");

        // Close entire position
        MarketManager.MarketTransactionResult closeRes = marketManager.executeCloseOMSPosition(
                playerUuid, playerName, pos.getPositionId(), initialUnits
        );

        assertTrue(closeRes.success(), "OMS close should succeed");
        assertTrue(closeRes.cbxAmount() > investCbx, "Payout should include profit");

        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertTrue(acc.getBalanceCbx() > 2000.0, "Balance should reflect earned profit");
        assertEquals(0, omsManager.getPositions(playerUuid).size(), "Position should be completely closed");
    }

    @Test
    @DisplayName("Should apply daily carry fee to open OMS positions")
    public void testDailyCarryFee() throws SQLException {
        marketManager.executeOpenOMSPosition(playerUuid, playerName, "minecraft:iron_ingot", 500.0);
        OMSPosition pos = omsManager.getPositions(playerUuid).get(0);
        double initialUnits = pos.getAmountUnits();

        // Apply carry fee of 1% (0.01)
        marketManager.applyDailyCarryFee(0.01);

        List<OMSPosition> updated = dao.loadAllOMSPositions();
        assertEquals(1, updated.size());
        assertEquals(initialUnits * 0.99, updated.get(0).getAmountUnits(), 0.001);
    }

    // ==========================================
    // 2. Limit Orders Engine Tests
    // ==========================================

    @Test
    @DisplayName("Should place BUY limit order and reserve CBX")
    public void testPlaceLimitOrderAndReservation() throws SQLException {
        int amount = 32;
        double targetPrice = 9.0;
        double expectedReserve = amount * targetPrice; // 288.0 CBX

        MarketManager.MarketTransactionResult res = marketManager.placeLimitOrder(
                playerUuid, playerName, "minecraft:iron_ingot", LimitOrder.TYPE_BUY, amount, targetPrice
        );

        assertTrue(res.success());
        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertEquals(2000.0 - expectedReserve, acc.getBalanceCbx(), 0.01);

        List<LimitOrder> orders = dao.getPlayerLimitOrders(playerUuid);
        assertEquals(1, orders.size());
        assertEquals(LimitOrder.STATUS_PENDING, orders.get(0).getStatus());
        assertEquals(expectedReserve, orders.get(0).getReservedCbx(), 0.01);
    }

    @Test
    @DisplayName("Should cancel pending limit order and refund reserved CBX")
    public void testCancelLimitOrderRefund() throws SQLException {
        marketManager.placeLimitOrder(
                playerUuid, playerName, "minecraft:iron_ingot", LimitOrder.TYPE_BUY, 20, 10.0
        );
        LimitOrder order = dao.getPlayerLimitOrders(playerUuid).get(0);

        MarketManager.MarketTransactionResult cancelRes = marketManager.cancelLimitOrder(playerUuid, order.getOrderId());
        assertTrue(cancelRes.success());

        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertEquals(2000.0, acc.getBalanceCbx(), 0.01, "Reserved CBX must be fully refunded");

        LimitOrder updated = dao.getLimitOrder(order.getOrderId());
        assertNotNull(updated);
        assertEquals(LimitOrder.STATUS_CANCELLED, updated.getStatus());
    }

    @Test
    @DisplayName("Should automatically execute BUY limit order when market price drops to limit")
    public void testAutomaticBuyLimitExecution() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();

        // Place BUY limit order at $11.0 (currently iron base is $12.0)
        marketManager.placeLimitOrder(
                playerUuid, playerName, "minecraft:iron_ingot", LimitOrder.TYPE_BUY, 16, 11.0
        );
        LimitOrder order = dao.getPlayerLimitOrders(playerUuid).get(0);
        assertTrue(order.isPending());

        // Simulate price drop by adding surplus stock to warehouse
        iron.setCurrentStock(initialStock + 4000.0);
        dao.upsertMarket(iron);

        // Process pending orders
        marketManager.processPendingLimitOrders();

        LimitOrder after = dao.getLimitOrder(order.getOrderId());
        assertNotNull(after);
        assertEquals(LimitOrder.STATUS_FILLED, after.getStatus(), "Order should be filled after price drop");
        assertEquals(initialStock + 4000.0 - 16, iron.getCurrentStock(), "Purchased goods should be deducted from stock");

        // Player should receive course change refund if actual purchase cost was lower than limit reservation
        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertTrue(acc.getBalanceCbx() > (2000.0 - (16 * 11.0)), "Remaining difference between limit price and actual buy price refunded");

        // Verify items were placed into unclaimed deliveries for player (since no online player handler was active)
        List<MarketDAO.UnclaimedDelivery> unclaimed = dao.getUnclaimedDeliveries(playerUuid);
        assertEquals(1, unclaimed.size(), "Unclaimed delivery record must be created for offline/unhandled player");
        assertEquals(16, unclaimed.get(0).amount(), "Delivery amount must match order amount");
        assertEquals("minecraft:iron_ingot", unclaimed.get(0).resourceId());

        // Verify active limit orders filtering
        assertEquals(0, dao.getPlayerActiveLimitOrders(playerUuid).size(), "Filled order must not appear in active orders");
        assertEquals(1, dao.getPlayerLimitOrders(playerUuid).size(), "Filled order remains in full history");
    }

    @Test
    @DisplayName("Should automatically execute SELL limit order when market price rises to limit")
    public void testAutomaticSellLimitExecution() throws SQLException {
        MarketResource iron = marketManager.getResource("minecraft:iron_ingot");
        double initialStock = iron.getCurrentStock();

        // Place SELL limit order at $18.0
        marketManager.placeLimitOrder(
                playerUuid, playerName, "minecraft:iron_ingot", LimitOrder.TYPE_SELL, 20, 18.0
        );
        LimitOrder order = dao.getPlayerLimitOrders(playerUuid).get(0);
        assertTrue(order.isPending());

        // Simulate price rise by reducing stock
        iron.setCurrentStock(initialStock * 0.4);
        dao.upsertMarket(iron);

        marketManager.processPendingLimitOrders();

        LimitOrder after = dao.getLimitOrder(order.getOrderId());
        assertNotNull(after);
        assertEquals(LimitOrder.STATUS_FILLED, after.getStatus(), "SELL limit order should be filled");

        PlayerAccount acc = dao.getAccount(playerUuid, playerName);
        assertTrue(acc.getBalanceCbx() > 2000.0, "Player balance should receive sale payout");
    }

    @Test
    @DisplayName("Should deliver items directly via ItemDeliveryHandler when player is online")
    public void testLimitBuyDirectItemDeliveryHandler() throws SQLException {
        boolean[] delivered = new boolean[]{false};
        int[] deliveredAmount = new int[]{0};

        marketManager.setItemDeliveryHandler((uuid, resourceId, amount, displayName) -> {
            if (uuid.equals(playerUuid) && "minecraft:diamond".equals(resourceId)) {
                delivered[0] = true;
                deliveredAmount[0] = amount;
                return true; // Successfully delivered directly
            }
            return false;
        });

        // Place BUY limit for diamond
        marketManager.unlockResourceForPlayer(playerUuid, "minecraft:diamond");
        marketManager.placeLimitOrder(playerUuid, playerName, "minecraft:diamond", LimitOrder.TYPE_BUY, 4, 300.0);
        MarketResource dia = marketManager.getResource("minecraft:diamond");
        dia.setCurrentStock(dia.getCurrentStock() + 2000.0);
        dao.upsertMarket(dia);

        marketManager.processPendingLimitOrders();

        assertTrue(delivered[0], "Online delivery handler should be triggered");
        assertEquals(4, deliveredAmount[0], "Correct item quantity delivered");
        assertEquals(0, dao.getUnclaimedDeliveries(playerUuid).size(), "No unclaimed deliveries needed when directly delivered");
    }

    @Test
    @DisplayName("Should properly delete unclaimed delivery once fulfilled")
    public void testUnclaimedDeliveryDeletion() throws SQLException {
        String deliveryId = UUID.randomUUID().toString();
        dao.saveUnclaimedDelivery(deliveryId, playerUuid, "minecraft:gold_ingot", 32, System.currentTimeMillis());

        List<MarketDAO.UnclaimedDelivery> list = dao.getUnclaimedDeliveries(playerUuid);
        assertEquals(1, list.size());
        assertEquals(32, list.get(0).amount());

        dao.deleteUnclaimedDelivery(deliveryId);
        assertEquals(0, dao.getUnclaimedDeliveries(playerUuid).size());
    }

    // ==========================================
    // 3. Delivery Contracts (Futures / Госзаказ)
    // ==========================================

    @Test
    @DisplayName("Should accept delivery contract, hold collateral, and pay premium reward on fulfillment")
    public void testContractAcceptAndFulfillment() throws SQLException {
        List<DeliveryContract> available = dao.getAvailableAndPlayerContracts(playerUuid);
        assertFalse(available.isEmpty(), "Daily contracts must be generated on initialization");

        DeliveryContract target = available.get(0);
        String contractId = target.getContractId();
        double collateral = target.getCollateralCbx();
        int targetAmount = target.getTargetAmount();

        // 1. Accept contract
        MarketManager.MarketTransactionResult acceptRes = marketManager.acceptContract(
                playerUuid, playerName, contractId, 1000L
        );
        assertTrue(acceptRes.success());

        PlayerAccount accAfterAccept = dao.getAccount(playerUuid, playerName);
        assertEquals(2000.0 - collateral, accAfterAccept.getBalanceCbx(), 0.01, "Collateral held");

        // 2. Partial delivery
        int firstBatch = targetAmount / 2;
        MarketManager.MarketTransactionResult partRes = marketManager.deliverContractItems(
                playerUuid, playerName, contractId, firstBatch
        );
        assertTrue(partRes.success());

        // 3. Final delivery fulfilling target
        int secondBatch = targetAmount - firstBatch;
        MarketManager.MarketTransactionResult finalRes = marketManager.deliverContractItems(
                playerUuid, playerName, contractId, secondBatch
        );
        assertTrue(finalRes.success(), "Final delivery should complete contract");

        // 4. Verify completed status and payout
        DeliveryContract completedContract = dao.getContract(contractId);
        assertNotNull(completedContract);
        assertEquals(DeliveryContract.STATUS_COMPLETED, completedContract.getStatus());

        PlayerAccount accFinal = dao.getAccount(playerUuid, playerName);
        double expectedMinBalance = 2000.0 + completedContract.getTotalPayout();
        assertEquals(expectedMinBalance, accFinal.getBalanceCbx(), 0.01, "Player should receive contract payout + collateral refund");
        assertTrue(accFinal.getRepPoints() >= completedContract.getRewardRep(), "REP points rewarded");
    }

    @Test
    @DisplayName("Should expire overdue contracts when deadline passes")
    public void testContractExpiration() throws SQLException {
        List<DeliveryContract> available = dao.getAvailableAndPlayerContracts(playerUuid);
        DeliveryContract target = available.get(0);

        marketManager.acceptContract(playerUuid, playerName, target.getContractId(), 1000L);

        // Advance ticks beyond deadline (currentTick + 72000)
        long expiredTick = 1000L + 72001L;
        marketManager.processContractTicks(expiredTick);

        List<DeliveryContract> active = dao.getAllActiveContracts();
        boolean stillActive = active.stream().anyMatch(c -> c.getContractId().equals(target.getContractId()));
        assertFalse(stillActive, "Contract must no longer be active after deadline expiration");
    }
}
