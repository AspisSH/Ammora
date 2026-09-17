package com.exchange.mod.test;

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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceUnlockTest {

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
        PlayerAccount acc = new PlayerAccount(playerUuid, "ResearchSteve", 50000.0, 0, 1, System.currentTimeMillis());
        dao.saveAccount(acc);
    }

    @Test
    @DisplayName("Starter resources (Iron, Copper) should be unlocked by default")
    public void testStarterResourcesUnlockedByDefault() {
        assertTrue(marketManager.isResourceUnlockedForPlayer(playerUuid, "minecraft:iron_ingot"),
                "Iron ingot must be unlocked by default");
        assertTrue(marketManager.isResourceUnlockedForPlayer(playerUuid, "minecraft:copper_ingot"),
                "Copper ingot must be unlocked by default");

        Set<String> unlocked = marketManager.getUnlockedResourcesForPlayer(playerUuid);
        assertTrue(unlocked.contains("minecraft:iron_ingot"));
        assertTrue(unlocked.contains("minecraft:copper_ingot"));
    }

    @Test
    @DisplayName("Rare resources (Diamond, Netherite) should be locked until researched")
    public void testRareResourcesLockedInitially() throws SQLException {
        assertFalse(marketManager.isResourceUnlockedForPlayer(playerUuid, "minecraft:diamond"),
                "Diamond must be locked before player conducts research");
        assertFalse(marketManager.isResourceUnlockedForPlayer(playerUuid, "minecraft:netherite_ingot"),
                "Netherite must be locked before player conducts research");

        // Buying locked resource must fail with unlock reminder
        MarketManager.MarketTransactionResult buyResult = marketManager.executeBuy(
                playerUuid, "ResearchSteve", "minecraft:diamond", 1
        );
        assertFalse(buyResult.success(), "Buying locked resource must fail");
        assertTrue(buyResult.message().toLowerCase().contains("locked") || buyResult.message().contains("заблокирован"), "Failure message must indicate locked resource");
    }

    @Test
    @DisplayName("Unlocking resource allows player to buy it successfully")
    public void testUnlockResourceAllowsPurchase() throws SQLException {
        // Unlock diamond for player
        marketManager.unlockResourceForPlayer(playerUuid, "minecraft:diamond");

        assertTrue(marketManager.isResourceUnlockedForPlayer(playerUuid, "minecraft:diamond"),
                "Diamond should be marked as unlocked in DB");

        Set<String> unlocked = marketManager.getUnlockedResourcesForPlayer(playerUuid);
        assertTrue(unlocked.contains("minecraft:diamond"));

        // Now purchase should succeed
        MarketManager.MarketTransactionResult buyResult = marketManager.executeBuy(
                playerUuid, "ResearchSteve", "minecraft:diamond", 1
        );
        assertTrue(buyResult.success(), "Purchase should succeed once resource is unlocked");
    }

    @Test
    @DisplayName("Selling locked resource should always be permitted (liquidation / disposal)")
    public void testSellingLockedResourceAllowed() throws SQLException {
        // Selling diamonds even if not researched should be allowed so players can earn money
        MarketManager.MarketTransactionResult sellResult = marketManager.executeSell(
                playerUuid, "ResearchSteve", "minecraft:diamond", 1
        );
        assertTrue(sellResult.success(), "Selling should be allowed regardless of research unlock state");
    }
}
