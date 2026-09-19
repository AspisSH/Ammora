package com.ammora.mod.test;

import com.ammora.mod.core.Candle;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSPosition;
import com.ammora.mod.db.AuctionRecord;
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

    @Test
    @DisplayName("Should retrieve all accounts ordered by balance for admin panel")
    public void testGetAllAccountsForAdmin() throws SQLException {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        PlayerAccount a1 = new PlayerAccount(u1, "Alice", 500.0, 10, 2, System.currentTimeMillis());
        PlayerAccount a2 = new PlayerAccount(u2, "Bob", 1500.0, 50, 4, System.currentTimeMillis());
        PlayerAccount a3 = new PlayerAccount(u3, "Charlie", 200.0, 0, 1, System.currentTimeMillis());

        dao.saveAccount(a1);
        dao.saveAccount(a2);
        dao.saveAccount(a3);

        List<PlayerAccount> all = dao.getAllAccounts();
        assertEquals(3, all.size());
        assertEquals("Bob", all.get(0).getPlayerName());
        assertEquals(1500.0, all.get(0).getBalanceCbx());
        assertEquals("Alice", all.get(1).getPlayerName());
        assertEquals("Charlie", all.get(2).getPlayerName());

        // Admin modifies balance
        a1.setBalanceCbx(9999.0);
        dao.saveAccount(a1);

        List<PlayerAccount> updated = dao.getAllAccounts();
        assertEquals("Alice", updated.get(0).getPlayerName());
        assertEquals(9999.0, updated.get(0).getBalanceCbx());
    }

    @Test
    @DisplayName("Should create auction, place bids with anti-sniping, and retrieve expired auctions")
    public void testAuctionPersistenceAndBidding() throws SQLException {
        UUID seller = UUID.randomUUID();
        UUID bidder1 = UUID.randomUUID();
        UUID bidder2 = UUID.randomUUID();

        long now = System.currentTimeMillis();
        long expires = now + 50_000L; // 50 seconds remaining (within anti-sniping window)

        AuctionRecord auction = new AuctionRecord(
                "auc-1",
                seller,
                "SellerPlayer",
                "minecraft:diamond_sword",
                "{Damage:0}",
                "Diamond Sword",
                1,
                100.0,
                0.0,
                10.0,
                500.0,
                null,
                "",
                now,
                expires,
                "ACTIVE"
        );

        dao.saveOrUpdateAuction(auction);

        AuctionRecord loaded = dao.getAuction("auc-1");
        assertNotNull(loaded);
        assertEquals("Diamond Sword", loaded.getDisplayName());
        assertEquals(100.0, loaded.getNextMinBid());
        assertTrue(loaded.hasBuyout());

        // Bidder 1 places first bid at start price
        boolean bid1 = loaded.placeBid(bidder1, "BidderOne", 100.0);
        assertTrue(bid1);
        assertEquals(100.0, loaded.getCurrentBid());
        assertEquals(bidder1, loaded.getHighestBidderUuid());
        // Anti-sniping should have extended expiresAt because remaining was < 60s
        assertTrue(loaded.getExpiresAt() > expires);

        dao.saveOrUpdateAuction(loaded);

        // Bidder 2 attempts bid below minimum step (100 + 10 = 110 required)
        AuctionRecord loaded2 = dao.getAuction("auc-1");
        assertEquals(110.0, loaded2.getNextMinBid());
        boolean invalidBid = loaded2.placeBid(bidder2, "BidderTwo", 105.0);
        assertFalse(invalidBid);

        // Bidder 2 places valid bid
        boolean validBid = loaded2.placeBid(bidder2, "BidderTwo", 120.0);
        assertTrue(validBid);
        assertEquals(120.0, loaded2.getCurrentBid());
        assertEquals(bidder2, loaded2.getHighestBidderUuid());
        dao.saveOrUpdateAuction(loaded2);

        // Check active auctions list
        List<AuctionRecord> active = dao.getActiveAuctions();
        assertEquals(1, active.size());

        // Check expired filter
        List<AuctionRecord> expiredNone = dao.getExpiredActiveAuctions(now);
        assertTrue(expiredNone.isEmpty());

        List<AuctionRecord> expiredLater = dao.getExpiredActiveAuctions(loaded2.getExpiresAt() + 1000L);
        assertEquals(1, expiredLater.size());
    }

    @Test
    @DisplayName("Should successfully link and unlink player shop to corporate account")
    public void testPlayerShopCompanyLinkAndUnlink() throws SQLException {
        UUID owner = UUID.randomUUID();
        String shopId = "shop-" + UUID.randomUUID();
        String companyId = UUID.randomUUID().toString();

        com.ammora.mod.db.PlayerShopRecord shop = new com.ammora.mod.db.PlayerShopRecord(
                shopId, owner, "ShopOwner", "Test Shop",
                "minecraft:overworld", 100, 64, 200,
                true, 0, 0.0, System.currentTimeMillis(),
                5, 64, false, null
        );
        dao.saveOrUpdatePlayerShop(shop);

        com.ammora.mod.db.PlayerShopRecord loaded = dao.getPlayerShop(shopId);
        assertNotNull(loaded);
        assertNull(loaded.getCompanyId());

        // Link to company
        shop.setCompanyId(companyId);
        dao.saveOrUpdatePlayerShop(shop);

        loaded = dao.getPlayerShop(shopId);
        assertNotNull(loaded);
        assertEquals(companyId, loaded.getCompanyId());

        // Unlink from company
        shop.setCompanyId(null);
        dao.saveOrUpdatePlayerShop(shop);

        loaded = dao.getPlayerShop(shopId);
        assertNotNull(loaded);
        assertNull(loaded.getCompanyId());
    }
}
