package com.ammora.mod.test;

import com.ammora.mod.db.BuyRequestRecord;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.MarketTxRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.db.PlayerShopRecord;
import com.ammora.mod.db.ShopSlotRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Rust-style Player Vending Machines, Marketplace, Escrow RFQ, and Ledger.
 */
public class PlayerShopTest {

    private DatabaseManager dbManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
    }

    @Test
    @DisplayName("Should persist, query, and update player vending machine in SQLite")
    public void testPlayerShopPersistence() throws SQLException {
        UUID ownerUuid = UUID.randomUUID();
        String shopId = UUID.randomUUID().toString();

        PlayerShopRecord shop = new PlayerShopRecord(
                shopId, ownerUuid, "TraderBob", "Броня и Оружие",
                "minecraft:overworld", 100, 64, -250,
                true, 0, 0.0, System.currentTimeMillis()
        );

        dao.saveOrUpdatePlayerShop(shop);

        PlayerShopRecord loaded = dao.getPlayerShop(shopId);
        assertNotNull(loaded);
        assertEquals("TraderBob", loaded.getOwnerName());
        assertEquals("Броня и Оружие", loaded.getShopName());
        assertTrue(loaded.isBroadcast());
        assertEquals(100, loaded.getPosX());

        // Update sales and revenue
        loaded.incrementSales(5);
        loaded.addRevenue(1250.50);
        dao.saveOrUpdatePlayerShop(loaded);

        PlayerShopRecord updated = dao.getPlayerShop(shopId);
        assertEquals(5, updated.getTotalSales());
        assertEquals(1250.50, updated.getAccumulatedRevenue(), 0.001);

        // Check broadcast list
        List<PlayerShopRecord> broadcastShops = dao.getAllBroadcastShops();
        assertEquals(1, broadcastShops.size());
        assertEquals("TraderBob", broadcastShops.get(0).getOwnerName());
    }

    @Test
    @DisplayName("Should manage shop slots and index active catalog for remote shopping")
    public void testShopSlotsAndCatalog() throws SQLException {
        String shopId = UUID.randomUUID().toString();
        PlayerShopRecord shop = new PlayerShopRecord(
                shopId, UUID.randomUUID(), "MerchantAlice", "Аптека",
                "minecraft:overworld", 0, 70, 0,
                true, 0, 0.0, System.currentTimeMillis()
        );
        dao.saveOrUpdatePlayerShop(shop);

        // Save 3 slots: 2 in stock, 1 out of stock
        dao.saveOrUpdateShopSlot(new ShopSlotRecord(shopId, 0, "minecraft:golden_apple", "", "Золотое яблоко", 15.0, 16));
        dao.saveOrUpdateShopSlot(new ShopSlotRecord(shopId, 1, "minecraft:potion", "", "Зелье лечения II", 35.0, 4));
        dao.saveOrUpdateShopSlot(new ShopSlotRecord(shopId, 2, "minecraft:totem_of_undying", "", "Тотем бессмертия", 500.0, 0)); // Sold out

        List<ShopSlotRecord> slots = dao.getShopSlots(shopId);
        assertEquals(3, slots.size());

        // Catalog query should only return slots with stock > 0
        List<ShopSlotRecord> catalog = dao.getAllActiveCatalogSlots();
        assertEquals(2, catalog.size());
        assertEquals("Золотое яблоко", catalog.get(0).getDisplayName());
        assertEquals("Зелье лечения II", catalog.get(1).getDisplayName());
    }

    @Test
    @DisplayName("Should verify full Escrow cycle for Buy Requests (RFQ): Create, partial fulfill, and refund cancel")
    public void testBuyRequestEscrowCycle() throws SQLException {
        UUID buyerUuid = UUID.randomUUID();
        PlayerAccount buyer = dao.getAccount(buyerUuid, "RichBuyer");
        buyer.setBalanceCbx(5000.0);
        dao.saveAccount(buyer);

        // Buyer places request for 2 Diamond Pickaxes at 1200 CBX each = 2400 CBX escrow
        double unitPrice = 1200.0;
        int requestedAmount = 2;
        double totalEscrow = unitPrice * requestedAmount;

        assertTrue(buyer.withdraw(totalEscrow));
        dao.saveAccount(buyer);
        assertEquals(2600.0, dao.getAccount(buyerUuid, "RichBuyer").getBalanceCbx(), 0.001);

        String reqId = UUID.randomUUID().toString();
        BuyRequestRecord req = new BuyRequestRecord(
                reqId, buyerUuid, "RichBuyer",
                "minecraft:diamond_pickaxe", "", "Алмазная кирка",
                unitPrice, requestedAmount, 0, totalEscrow, "ACTIVE", System.currentTimeMillis()
        );
        dao.saveBuyRequest(req);

        // Another player fulfills 1 pickaxe
        BuyRequestRecord activeReq = dao.getBuyRequest(reqId);
        assertEquals("ACTIVE", activeReq.getStatus());
        assertEquals(2, activeReq.getRemainingAmount());

        activeReq.fulfill(1); // Fulfill 1
        dao.updateBuyRequest(activeReq);

        BuyRequestRecord partiallyFilled = dao.getBuyRequest(reqId);
        assertEquals(1, partiallyFilled.getAmountFulfilled());
        assertEquals(1, partiallyFilled.getRemainingAmount());
        assertEquals(1200.0, partiallyFilled.getEscrowCbx(), 0.001);
        assertEquals("ACTIVE", partiallyFilled.getStatus());

        // Buyer decides to cancel remaining 1 pickaxe -> gets 1200 CBX escrow refund
        double refund = partiallyFilled.getEscrowCbx();
        partiallyFilled.cancel();
        dao.updateBuyRequest(partiallyFilled);

        buyer = dao.getAccount(buyerUuid, "RichBuyer");
        buyer.deposit(refund);
        dao.saveAccount(buyer);

        assertEquals(3800.0, dao.getAccount(buyerUuid, "RichBuyer").getBalanceCbx(), 0.001);
        assertEquals("CANCELLED", dao.getBuyRequest(reqId).getStatus());
    }

    @Test
    @DisplayName("Should record and audit marketplace transaction ledger")
    public void testMarketTransactionLedger() throws SQLException {
        UUID buyer = UUID.randomUUID();
        UUID seller = UUID.randomUUID();

        dao.recordMarketTransaction(new MarketTxRecord(
                UUID.randomUUID().toString(),
                "LOCAL_BUY",
                "shop-123",
                buyer, "Steve",
                seller, "Alex",
                "minecraft:iron_ingot", "Железный слиток",
                32, 160.0, 0.0, System.currentTimeMillis()
        ));

        dao.recordMarketTransaction(new MarketTxRecord(
                UUID.randomUUID().toString(),
                "REMOTE_BUY",
                "shop-123",
                buyer, "Steve",
                seller, "Alex",
                "minecraft:diamond", "Алмаз",
                3, 750.0, 15.0, System.currentTimeMillis() + 1000
        ));

        List<MarketTxRecord> txs = dao.getRecentMarketTransactions(10);
        assertEquals(2, txs.size());
        assertEquals("REMOTE_BUY", txs.get(0).getTxType()); // Ordered by timestamp DESC
        assertEquals("Алмаз", txs.get(0).getItemName());
        assertEquals(15.0, txs.get(0).getFeeCbx(), 0.001);
    }

    @Test
    @DisplayName("Should delete shop and cascade delete its slot indexes")
    public void testShopDeletionCascade() throws SQLException {
        String shopId = UUID.randomUUID().toString();
        PlayerShopRecord shop = new PlayerShopRecord(
                shopId, UUID.randomUUID(), "Vendor", "Лавка",
                "minecraft:overworld", 50, 60, 70,
                true, 0, 0.0, System.currentTimeMillis()
        );
        dao.saveOrUpdatePlayerShop(shop);
        dao.saveOrUpdateShopSlot(new ShopSlotRecord(shopId, 0, "minecraft:bread", "", "Хлеб", 1.0, 64));

        assertNotNull(dao.getPlayerShop(shopId));
        assertEquals(1, dao.getShopSlots(shopId).size());

        dao.deletePlayerShop(shopId);

        assertNull(dao.getPlayerShop(shopId));
        assertTrue(dao.getShopSlots(shopId).isEmpty());
    }
}
