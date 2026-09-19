package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a registered Company / Corporate Account in the Ammora financial system.
 */
public class CompanyRecord {
    private final String companyId;
    private String companyName;
    private final UUID ownerUuid;
    private double balanceCbx;
    private final long createdAt;

    public CompanyRecord(String companyId, String companyName, UUID ownerUuid, double balanceCbx, long createdAt) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.ownerUuid = ownerUuid;
        this.balanceCbx = balanceCbx;
        this.createdAt = createdAt;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public double getBalanceCbx() {
        return balanceCbx;
    }

    public void setBalanceCbx(double balanceCbx) {
        this.balanceCbx = balanceCbx;
    }

    public void deposit(double amount) {
        if (amount > 0.0) {
            this.balanceCbx += amount;
        }
    }

    public boolean withdraw(double amount) {
        if (amount > 0.0 && this.balanceCbx >= amount) {
            this.balanceCbx -= amount;
            return true;
        }
        return false;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
