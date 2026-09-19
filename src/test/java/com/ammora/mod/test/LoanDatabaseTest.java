package com.ammora.mod.test;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.LoanRecord;
import com.ammora.mod.db.MarketDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LoanDatabaseTest {

    private DatabaseManager dbManager;
    private MarketDAO dao;

    @BeforeEach
    public void setup() throws SQLException {
        // In-memory SQLite for testing
        dbManager = new DatabaseManager(null);
        dbManager.initializeTables();
        dao = new MarketDAO(dbManager);
    }

    @Test
    @DisplayName("Should successfully persist and retrieve P2P loan record")
    public void testLoanPersistence() throws SQLException {
        UUID lenderUuid = UUID.randomUUID();
        UUID borrowerUuid = UUID.randomUUID();
        String loanId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expiresAt = now + (24 * 3600000L);

        double principal = 100.0;
        double rate = 15.0;
        double totalRepay = MarketEngine.round2(principal * (1.0 + (rate / 100.0)));

        LoanRecord loan = new LoanRecord(
                loanId, lenderUuid, "LenderSteve", borrowerUuid, "BorrowerAlex",
                principal, rate, totalRepay, "minecraft:diamond_sword", "{Damage:0}",
                "Diamond Sword", 1, now, expiresAt, "ACTIVE"
        );

        dao.saveLoan(loan);

        LoanRecord fetched = dao.getLoan(loanId);
        assertNotNull(fetched);
        assertEquals(loanId, fetched.getLoanId());
        assertEquals("LenderSteve", fetched.getLenderName());
        assertEquals("BorrowerAlex", fetched.getBorrowerName());
        assertEquals(100.0, fetched.getPrincipalCbx());
        assertEquals(15.0, fetched.getInterestRate());
        assertEquals(115.0, fetched.getTotalRepayCbx());
        assertEquals("minecraft:diamond_sword", fetched.getItemId());
        assertEquals("Diamond Sword", fetched.getDisplayName());
        assertEquals("ACTIVE", fetched.getStatus());
    }

    @Test
    @DisplayName("Should query loans for lender and borrower correctly")
    public void testPlayerLoansQuery() throws SQLException {
        UUID lender = UUID.randomUUID();
        UUID borrower = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        long now = System.currentTimeMillis();
        dao.saveLoan(new LoanRecord(
                "loan-1", lender, "Lender", borrower, "Borrower",
                50.0, 10.0, 55.0, "minecraft:iron_sword", "", "Iron Sword", 1, now, now + 10000, "ACTIVE"
        ));
        dao.saveLoan(new LoanRecord(
                "loan-2", lender, "Lender", other, "Other",
                200.0, 20.0, 240.0, "minecraft:netherite_ingot", "", "Netherite", 2, now, now + 20000, "ACTIVE"
        ));

        List<LoanRecord> lenderLoans = dao.getPlayerLoans(lender);
        assertEquals(2, lenderLoans.size());

        List<LoanRecord> borrowerLoans = dao.getPlayerLoans(borrower);
        assertEquals(1, borrowerLoans.size());
        assertEquals("loan-1", borrowerLoans.get(0).getLoanId());
    }

    @Test
    @DisplayName("Should update loan status to REPAID and DEFAULTED")
    public void testLoanStatusTransitions() throws SQLException {
        String loanId = "loan-trans";
        long now = System.currentTimeMillis();
        dao.saveLoan(new LoanRecord(
                loanId, UUID.randomUUID(), "L", UUID.randomUUID(), "B",
                100.0, 15.0, 115.0, "minecraft:emerald", "", "Emerald", 10, now, now + 10000, "ACTIVE"
        ));

        assertEquals("ACTIVE", dao.getLoan(loanId).getStatus());

        dao.updateLoanStatus(loanId, "REPAID");
        assertEquals("REPAID", dao.getLoan(loanId).getStatus());

        dao.updateLoanStatus(loanId, "DEFAULTED");
        assertEquals("DEFAULTED", dao.getLoan(loanId).getStatus());
    }

    @Test
    @DisplayName("Should query expired active loans correctly")
    public void testExpiredActiveLoans() throws SQLException {
        long now = System.currentTimeMillis();

        // Expired loan (1 hour ago)
        dao.saveLoan(new LoanRecord(
                "expired-1", UUID.randomUUID(), "L1", UUID.randomUUID(), "B1",
                50.0, 15.0, 57.5, "minecraft:gold_block", "", "Gold Block", 1, now - 7200000L, now - 3600000L, "ACTIVE"
        ));

        // Active future loan
        dao.saveLoan(new LoanRecord(
                "future-1", UUID.randomUUID(), "L2", UUID.randomUUID(), "B2",
                100.0, 10.0, 110.0, "minecraft:iron_block", "", "Iron Block", 1, now, now + 3600000L, "ACTIVE"
        ));

        // Already repaid expired loan (should NOT be returned)
        dao.saveLoan(new LoanRecord(
                "repaid-1", UUID.randomUUID(), "L3", UUID.randomUUID(), "B3",
                70.0, 10.0, 77.0, "minecraft:copper_block", "", "Copper Block", 1, now - 7200000L, now - 3600000L, "REPAID"
        ));

        List<LoanRecord> expired = dao.getExpiredActiveLoans();
        assertEquals(1, expired.size());
        assertEquals("expired-1", expired.get(0).getLoanId());
    }
}
