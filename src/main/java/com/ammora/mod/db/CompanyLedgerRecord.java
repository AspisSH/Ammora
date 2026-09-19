package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing an immutable audit ledger entry for a company account.
 */
public class CompanyLedgerRecord {
    private final String entryId;
    private final String companyId;
    private final UUID playerUuid;
    private final String playerName;
    private final String actionType;
    private final double amountCbx;
    private final String description;
    private final long timestamp;

    public CompanyLedgerRecord(String entryId, String companyId, UUID playerUuid, String playerName,
                               String actionType, double amountCbx, String description, long timestamp) {
        this.entryId = entryId;
        this.companyId = companyId;
        this.playerUuid = playerUuid;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.actionType = actionType != null ? actionType : "TRANSACTION";
        this.amountCbx = amountCbx;
        this.description = description != null ? description : "";
        this.timestamp = timestamp;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getActionType() {
        return actionType;
    }

    public double getAmountCbx() {
        return amountCbx;
    }

    public String getDescription() {
        return description;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
