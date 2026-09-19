package com.ammora.mod.test;

import com.ammora.mod.db.CompanyMemberRecord;
import com.ammora.mod.db.CompanyRecord;
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

public class CompanyTest {

    private DatabaseManager dbManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        // Use in-memory SQLite database for test isolation
        dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
    }

    @Test
    @DisplayName("Should enforce dynamic admin registration fee and single membership constraint")
    public void testCompanyRegistrationWithDynamicFee() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        PlayerAccount ownerAcc = dao.getAccount(ownerUuid, "SteveOwner");
        ownerAcc.setBalanceCbx(1000.0);
        dao.saveAccount(ownerAcc);

        // Verify default registration fee
        assertEquals(500.0, dao.getCompanyRegistrationFee(), 0.001);

        // Admin updates fee to 600.0
        dao.setCompanyRegistrationFee(600.0);
        assertEquals(600.0, dao.getCompanyRegistrationFee(), 0.001);

        // Create company with dynamic fee
        CompanyRecord comp = dao.createCompany("AmmoraCorp", ownerUuid, "SteveOwner", 600.0);
        assertNotNull(comp);
        assertEquals("AmmoraCorp", comp.getCompanyName());
        assertEquals(ownerUuid, comp.getOwnerUuid());
        assertEquals(0.0, comp.getBalanceCbx(), 0.001);

        // Check owner personal account was debited (1000 - 600 = 400)
        PlayerAccount updatedOwner = dao.getAccount(ownerUuid, "SteveOwner");
        assertEquals(400.0, updatedOwner.getBalanceCbx(), 0.001);

        // Check owner member record was created
        CompanyMemberRecord ownerMem = dao.getCompanyMember(comp.getCompanyId(), ownerUuid);
        assertNotNull(ownerMem);
        assertEquals("OWNER", ownerMem.getRole());
        assertTrue(ownerMem.isOwner());

        // Check registration ledger was recorded
        var ledger = dao.getCompanyLedger(comp.getCompanyId(), 10);
        assertFalse(ledger.isEmpty());
        assertEquals("REGISTRATION", ledger.get(0).getActionType());

        // Duplicate name should fail
        UUID otherUuid = UUID.randomUUID();
        PlayerAccount otherAcc = dao.getAccount(otherUuid, "AlexOther");
        otherAcc.setBalanceCbx(1000.0);
        dao.saveAccount(otherAcc);

        assertThrows(SQLException.class, () -> {
            dao.createCompany("AmmoraCorp", otherUuid, "AlexOther", 600.0);
        });

        // Already in company should fail
        SQLException ex = assertThrows(SQLException.class, () -> {
            dao.createCompany("SecondCorp", ownerUuid, "SteveOwner", 100.0);
        });
        assertTrue(ex.getMessage().contains("ALREADY_IN_COMPANY"));
    }

    @Test
    @DisplayName("Should manage team roster, role promotion/demotion, and kick")
    public void testMemberManagementAndRoles() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        PlayerAccount ownerAcc = dao.getAccount(ownerUuid, "SteveOwner");
        ownerAcc.setBalanceCbx(1000.0);
        dao.saveAccount(ownerAcc);

        CompanyRecord comp = dao.createCompany("TechLogistics", ownerUuid, "SteveOwner", 500.0);
        UUID memberUuid = UUID.randomUUID();

        // Add member
        dao.addCompanyMember(comp.getCompanyId(), memberUuid, "WorkerBob", "MEMBER", 100.0);
        CompanyMemberRecord bob = dao.getCompanyMember(comp.getCompanyId(), memberUuid);
        assertNotNull(bob);
        assertTrue(bob.isMember());
        assertFalse(bob.isManager());
        assertFalse(bob.isOwner());
        assertEquals(100.0, bob.getDailyLimitCbx(), 0.001);

        // Promote to MANAGER
        dao.updateMemberRole(comp.getCompanyId(), memberUuid, "MANAGER");
        bob = dao.getCompanyMember(comp.getCompanyId(), memberUuid);
        assertTrue(bob.isManager());
        assertFalse(bob.isMember());

        // Update daily allowance
        dao.updateMemberDailyLimit(comp.getCompanyId(), memberUuid, 300.0);
        bob = dao.getCompanyMember(comp.getCompanyId(), memberUuid);
        assertEquals(300.0, bob.getDailyLimitCbx(), 0.001);

        // Roster size
        List<CompanyMemberRecord> roster = dao.getCompanyMembers(comp.getCompanyId());
        assertEquals(2, roster.size());

        // Kick member
        dao.removeCompanyMember(comp.getCompanyId(), memberUuid);
        assertNull(dao.getCompanyMember(comp.getCompanyId(), memberUuid));
        assertNull(dao.getPlayerCompany(memberUuid));
        assertEquals(1, dao.getCompanyMembers(comp.getCompanyId()).size());
    }

    @Test
    @DisplayName("Should enforce daily allowance limits and rollover correctly")
    public void testDailyAllowanceAndSpending() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        PlayerAccount ownerAcc = dao.getAccount(ownerUuid, "SteveOwner");
        ownerAcc.setBalanceCbx(1000.0);
        dao.saveAccount(ownerAcc);

        CompanyRecord comp = dao.createCompany("MiningAlliance", ownerUuid, "SteveOwner", 500.0);
        dao.depositToCompany(comp.getCompanyId(), ownerUuid, "SteveOwner", 500.0);

        UUID managerUuid = UUID.randomUUID();
        dao.addCompanyMember(comp.getCompanyId(), managerUuid, "ManagerAlice", "MANAGER", 200.0);
        CompanyMemberRecord alice = dao.getCompanyMember(comp.getCompanyId(), managerUuid);

        // Can spend within limit
        assertTrue(alice.canSpend(150.0));
        alice.recordSpend(150.0);
        dao.saveCompanyMember(alice);

        // Next spend within same day: remaining is 50.0
        alice = dao.getCompanyMember(comp.getCompanyId(), managerUuid);
        assertEquals(150.0, alice.getSpentTodayCbx(), 0.001);
        assertTrue(alice.canSpend(50.0));
        assertFalse(alice.canSpend(50.01));

        // Attempting to withdraw as MANAGER
        boolean ok = dao.withdrawFromCompany(comp.getCompanyId(), managerUuid, "ManagerAlice", 50.0);
        assertTrue(ok);

        // Treasury updated
        CompanyRecord updatedComp = dao.getCompany(comp.getCompanyId());
        assertEquals(450.0, updatedComp.getBalanceCbx(), 0.001);

        // Test day rollover: simulate member spending on an earlier day
        alice = dao.getCompanyMember(comp.getCompanyId(), managerUuid);
        alice.setLastSpentDay(alice.getLastSpentDay() - 2); // 2 days ago
        assertTrue(alice.canSpend(200.0)); // Day rolled over, limit restored
        alice.recordSpend(50.0);
        assertEquals(50.0, alice.getSpentTodayCbx(), 0.001);
    }

    @Test
    @DisplayName("Should execute atomic corporate transfers and update company audit ledger")
    public void testCorporateTransferAndLedger() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        PlayerAccount ownerAcc = dao.getAccount(ownerUuid, "SteveOwner");
        ownerAcc.setBalanceCbx(1000.0);
        dao.saveAccount(ownerAcc);

        CompanyRecord comp = dao.createCompany("EnergyCorp", ownerUuid, "SteveOwner", 500.0);
        dao.depositToCompany(comp.getCompanyId(), ownerUuid, "SteveOwner", 400.0);

        UUID recipientUuid = UUID.randomUUID();
        PlayerAccount dan = dao.getAccount(recipientUuid, "SupplierDan");
        dan.setBalanceCbx(0.0);
        dao.saveAccount(dan);

        boolean transferred = dao.transferFromCompanyToPlayer(
                comp.getCompanyId(), ownerUuid, "SteveOwner", recipientUuid, "SupplierDan", 150.0
        );
        assertTrue(transferred);

        // Company balance reduced (400 - 150 = 250)
        CompanyRecord freshComp = dao.getCompany(comp.getCompanyId());
        assertEquals(250.0, freshComp.getBalanceCbx(), 0.001);

        // Recipient received funds (0 + 150 = 150)
        PlayerAccount freshDan = dao.getAccount(recipientUuid, "SupplierDan");
        assertEquals(150.0, freshDan.getBalanceCbx(), 0.001);

        // Audit ledger logged TRANSFER_OUT
        var ledger = dao.getCompanyLedger(comp.getCompanyId(), 10);
        assertTrue(ledger.stream().anyMatch(l -> "TRANSFER_OUT".equals(l.getActionType()) && l.getAmountCbx() == 150.0));
    }

    @Test
    @DisplayName("Should route shop revenue and handle complete company dissolution")
    public void testShopRevenueAndDissolution() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        PlayerAccount ownerAcc = dao.getAccount(ownerUuid, "SteveOwner");
        ownerAcc.setBalanceCbx(1000.0);
        dao.saveAccount(ownerAcc);

        CompanyRecord comp = dao.createCompany("RetailGiants", ownerUuid, "SteveOwner", 500.0);
        dao.depositToCompany(comp.getCompanyId(), ownerUuid, "SteveOwner", 300.0);

        // Link player shop to company
        PlayerShopRecord shop = new PlayerShopRecord(
                "shop-101", ownerUuid, "SteveOwner", "Steve's Mall",
                "minecraft:overworld", 100, 64, 100,
                false, 0, 0.0, System.currentTimeMillis(),
                5, 64, false, comp.getCompanyId()
        );
        dao.saveOrUpdatePlayerShop(shop);

        // Verify shop linked
        PlayerShopRecord loadedShop = dao.getPlayerShop("shop-101");
        assertEquals(comp.getCompanyId(), loadedShop.getCompanyId());

        // Dissolve company: all remaining 300.0 treasury funds must be refunded to owner
        boolean dissolved = dao.dissolveCompany(comp.getCompanyId(), ownerUuid, "SteveOwner");
        assertTrue(dissolved);

        // Company should no longer exist
        assertNull(dao.getCompany(comp.getCompanyId()));
        assertNull(dao.getPlayerCompany(ownerUuid));

        // Owner balance: initial 1000 - 500 fee - 300 deposit + 300 refund = 500
        PlayerAccount refreshedOwner = dao.getAccount(ownerUuid, "SteveOwner");
        assertEquals(500.0, refreshedOwner.getBalanceCbx(), 0.001);

        // Shop company_id should now be null
        PlayerShopRecord unlinkedShop = dao.getPlayerShop("shop-101");
        assertNull(unlinkedShop.getCompanyId());
    }
}
