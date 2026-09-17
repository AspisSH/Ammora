package com.exchange.mod.core;

import java.util.UUID;

/**
 * Model representing a P2P money transfer between two players.
 */
public class P2PTransfer {

    private final String transferId;
    private final UUID fromUuid;
    private final String fromName;
    private final UUID toUuid;
    private final String toName;
    private final double amount;
    private final long timestamp;

    public P2PTransfer(String transferId, UUID fromUuid, String fromName, UUID toUuid, String toName, double amount, long timestamp) {
        this.transferId = transferId;
        this.fromUuid = fromUuid;
        this.fromName = fromName;
        this.toUuid = toUuid;
        this.toName = toName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getTransferId() {
        return transferId;
    }

    public UUID getFromUuid() {
        return fromUuid;
    }

    public String getFromName() {
        return fromName;
    }

    public UUID getToUuid() {
        return toUuid;
    }

    public String getToName() {
        return toName;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
