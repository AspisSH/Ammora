package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a peer-to-peer secured collateral loan between players.
 */
public class LoanRecord {
    private final String loanId;
    private final UUID lenderUuid;
    private String lenderName;
    private final UUID borrowerUuid;
    private String borrowerName;
    private final double principalCbx;
    private final double interestRate;
    private final double totalRepayCbx;
    private final String itemId;
    private final String itemNbt;
    private final String displayName;
    private final int itemCount;
    private final long createdAt;
    private long expiresAt;
    private String status; // "ACTIVE", "REPAID", "DEFAULTED"

    public LoanRecord(String loanId, UUID lenderUuid, String lenderName,
                      UUID borrowerUuid, String borrowerName,
                      double principalCbx, double interestRate, double totalRepayCbx,
                      String itemId, String itemNbt, String displayName, int itemCount,
                      long createdAt, long expiresAt, String status) {
        this.loanId = loanId;
        this.lenderUuid = lenderUuid;
        this.lenderName = lenderName;
        this.borrowerUuid = borrowerUuid;
        this.borrowerName = borrowerName;
        this.principalCbx = principalCbx;
        this.interestRate = interestRate;
        this.totalRepayCbx = totalRepayCbx;
        this.itemId = itemId;
        this.itemNbt = itemNbt != null ? itemNbt : "";
        this.displayName = displayName;
        this.itemCount = Math.max(1, itemCount);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getLoanId() { return loanId; }
    public UUID getLenderUuid() { return lenderUuid; }
    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }
    public UUID getBorrowerUuid() { return borrowerUuid; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public double getPrincipalCbx() { return principalCbx; }
    public double getInterestRate() { return interestRate; }
    public double getTotalRepayCbx() { return totalRepayCbx; }
    public String getItemId() { return itemId; }
    public String getItemNbt() { return itemNbt; }
    public String getDisplayName() { return displayName; }
    public int getItemCount() { return itemCount; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "ACTIVE".equalsIgnoreCase(status); }
    public boolean isRepaid() { return "REPAID".equalsIgnoreCase(status); }
    public boolean isDefaulted() { return "DEFAULTED".equalsIgnoreCase(status); }
}
