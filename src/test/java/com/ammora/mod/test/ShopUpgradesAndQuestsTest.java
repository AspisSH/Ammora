package com.ammora.mod.test;

import com.ammora.mod.db.CommunityQuestRecord;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.db.PlayerShopRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShopUpgradesAndQuestsTest {

    private DatabaseManager dbManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        // Use in-memory SQLite database
        dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
    }

    @Test
    @DisplayName("Should persist and load player shop upgrades (custom name, capacity up to 1024, slots up to 10, network module)")
    public void testShopUpgradesPersistence() throws SQLException {
        String shopId = UUID.randomUUID().toString();
        UUID ownerUuid = UUID.randomUUID();

        // 1. Initial shop with default values
        PlayerShopRecord shop = new PlayerShopRecord(
                shopId, ownerUuid, "Steve", "Торговый автомат",
                "minecraft:overworld", 100, 64, -200,
                false, 0, 0.0, System.currentTimeMillis()
        );

        assertEquals(5, shop.getMaxSlots());
        assertEquals(64, shop.getSlotCapacity());
        assertFalse(shop.isNetworkUnlocked());

        dao.saveOrUpdatePlayerShop(shop);

        PlayerShopRecord loaded = dao.getPlayerShop(shopId);
        assertNotNull(loaded);
        assertEquals("Торговый автомат", loaded.getShopName());
        assertEquals(5, loaded.getMaxSlots());
        assertEquals(64, loaded.getSlotCapacity());
        assertFalse(loaded.isNetworkUnlocked());

        // 2. Upgrade capacity from 64 to 1024
        loaded.setSlotCapacity(1024);
        // 3. Unlock slots 6 through 10
        loaded.setMaxSlots(10);
        // 4. Install satellite network module
        loaded.setNetworkUnlocked(true);
        loaded.setBroadcast(true);
        // 5. Rename shop
        loaded.setShopName("Кибер-Маркет Редстоуна");

        dao.saveOrUpdatePlayerShop(loaded);

        PlayerShopRecord upgraded = dao.getPlayerShop(shopId);
        assertNotNull(upgraded);
        assertEquals("Кибер-Маркет Редстоуна", upgraded.getShopName());
        assertEquals(10, upgraded.getMaxSlots());
        assertEquals(1024, upgraded.getSlotCapacity());
        assertTrue(upgraded.isNetworkUnlocked());
        assertTrue(upgraded.isBroadcast());
    }

    @Test
    @DisplayName("Should handle complete lifecycle of community quest (create, accept, cancel, complete, delete)")
    public void testCommunityQuestLifecycle() throws SQLException {
        String questId = UUID.randomUUID().toString();
        UUID creatorUuid = UUID.randomUUID();
        UUID workerUuid = UUID.randomUUID();

        // 1. Create quest
        CommunityQuestRecord quest = new CommunityQuestRecord(
                questId, creatorUuid, "Alice",
                "Снос горы под застройку",
                "Координаты базы X: 250, Z: -400. Убрать верхушку горы до уровня Y: 70.",
                500.0, "OPEN", null, "", System.currentTimeMillis()
        );

        dao.saveOrUpdateQuest(quest);

        CommunityQuestRecord loaded = dao.getQuest(questId);
        assertNotNull(loaded);
        assertEquals("Снос горы под застройку", loaded.getTitle());
        assertEquals(500.0, loaded.getRewardCbx());
        assertEquals("OPEN", loaded.getStatus());
        assertTrue(loaded.isOpen());
        assertNull(loaded.getWorkerUuid());

        // 2. Accept quest by worker
        loaded.setStatus("IN_PROGRESS");
        loaded.setWorkerUuid(workerUuid);
        loaded.setWorkerName("BuilderBob");
        dao.saveOrUpdateQuest(loaded);

        CommunityQuestRecord inProgress = dao.getQuest(questId);
        assertNotNull(inProgress);
        assertEquals("IN_PROGRESS", inProgress.getStatus());
        assertTrue(inProgress.isInProgress());
        assertEquals(workerUuid, inProgress.getWorkerUuid());
        assertEquals("BuilderBob", inProgress.getWorkerName());

        // 3. Worker cancels work -> returns to OPEN
        inProgress.setStatus("OPEN");
        inProgress.setWorkerUuid(null);
        inProgress.setWorkerName("");
        dao.saveOrUpdateQuest(inProgress);

        CommunityQuestRecord reopened = dao.getQuest(questId);
        assertNotNull(reopened);
        assertTrue(reopened.isOpen());
        assertNull(reopened.getWorkerUuid());

        // 4. Accept again and mark completed
        reopened.setStatus("IN_PROGRESS");
        reopened.setWorkerUuid(workerUuid);
        reopened.setWorkerName("BuilderBob");
        reopened.setStatus("COMPLETED");
        dao.saveOrUpdateQuest(reopened);

        CommunityQuestRecord completed = dao.getQuest(questId);
        assertNotNull(completed);
        assertTrue(completed.isCompleted());

        // 5. Delete quest
        dao.deleteQuest(questId);
        assertNull(dao.getQuest(questId));
    }

    @Test
    @DisplayName("Should execute P2P reward transfer for completed quest between creator and worker")
    public void testQuestP2PRewardTransfer() throws SQLException {
        UUID creatorUuid = UUID.randomUUID();
        UUID workerUuid = UUID.randomUUID();

        PlayerAccount creator = new PlayerAccount(creatorUuid, "QuestCreator", 1000.0, 0, 1, System.currentTimeMillis());
        PlayerAccount worker = new PlayerAccount(workerUuid, "HardWorker", 50.0, 0, 1, System.currentTimeMillis());

        dao.saveAccount(creator);
        dao.saveAccount(worker);

        double reward = 350.0;

        // Verify balances before transfer
        PlayerAccount cBefore = dao.getAccount(creatorUuid, "QuestCreator");
        PlayerAccount wBefore = dao.getAccount(workerUuid, "HardWorker");
        assertEquals(1000.0, cBefore.getBalanceCbx());
        assertEquals(50.0, wBefore.getBalanceCbx());

        // Execute P2P transfer
        cBefore.withdraw(reward);
        wBefore.deposit(reward);

        dao.saveAccount(cBefore);
        dao.saveAccount(wBefore);

        // Verify balances after transfer
        PlayerAccount cAfter = dao.getAccount(creatorUuid, "QuestCreator");
        PlayerAccount wAfter = dao.getAccount(workerUuid, "HardWorker");
        assertEquals(650.0, cAfter.getBalanceCbx(), 0.001);
        assertEquals(400.0, wAfter.getBalanceCbx(), 0.001);
    }

    @Test
    @DisplayName("Should list all active community quests ordered by creation time")
    public void testListAllQuests() throws SQLException {
        UUID creator = UUID.randomUUID();

        dao.saveOrUpdateQuest(new CommunityQuestRecord(
                "q1", creator, "Steve", "Квест 1", "Описание 1", 100.0, "OPEN", null, "", 1000L
        ));
        dao.saveOrUpdateQuest(new CommunityQuestRecord(
                "q2", creator, "Steve", "Квест 2", "Описание 2", 200.0, "OPEN", null, "", 2000L
        ));
        dao.saveOrUpdateQuest(new CommunityQuestRecord(
                "q3", creator, "Steve", "Квест 3", "Описание 3", 300.0, "COMPLETED", null, "", 3000L
        ));

        List<CommunityQuestRecord> all = dao.getAllQuests();
        assertEquals(3, all.size());
        // ORDER BY created_at DESC -> q3 first, then q2, then q1
        assertEquals("q3", all.get(0).getQuestId());
        assertEquals("q2", all.get(1).getQuestId());
        assertEquals("q1", all.get(2).getQuestId());
    }
}
