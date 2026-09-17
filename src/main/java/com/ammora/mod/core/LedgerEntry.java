package com.ammora.mod.core;

/**
 * Represents a single entry in the Cold Wallet ledger history.
 */
public class LedgerEntry {

    private final String type; // P2P_IN, P2P_OUT, BUY, SELL, OMS, CONTRACT
    private final String title;
    private final double amountCbx;
    private final long timestamp;

    public LedgerEntry(String type, String title, double amountCbx, long timestamp) {
        this.type = type;
        this.title = title;
        this.amountCbx = amountCbx;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public double getAmountCbx() {
        return amountCbx;
    }

    @Deprecated
    public double getAmountUsdt() {
        return getAmountCbx();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
