package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a player's exchange balance and broker rank.
 */
public class PlayerAccount {
    private final UUID playerUuid;
    private String playerName;
    private double balanceCbx;
    private int repPoints;
    private int repLevel;
    private long updatedAt;

    public PlayerAccount(UUID playerUuid, String playerName, double balanceCbx, int repPoints, int repLevel, long updatedAt) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.balanceCbx = balanceCbx;
        this.repPoints = repPoints;
        this.repLevel = repLevel;
        this.updatedAt = updatedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public double getBalanceCbx() {
        return balanceCbx;
    }

    public void setBalanceCbx(double balanceCbx) {
        this.balanceCbx = balanceCbx;
    }

    @Deprecated
    public double getBalanceUsdt() {
        return getBalanceCbx();
    }

    @Deprecated
    public void setBalanceUsdt(double balance) {
        setBalanceCbx(balance);
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balanceCbx += amount;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && this.balanceCbx >= amount) {
            this.balanceCbx -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getRepPoints() {
        return repPoints;
    }

    public void addRepPoints(int points) {
        this.repPoints += points;
        updateRepLevel();
    }

    public int getRepLevel() {
        return repLevel;
    }

    private static int rankTrader = 500;
    private static int rankBroker = 2500;
    private static int rankInvestor = 10000;
    private static int rankOligarch = 50000;

    public static void setRankThresholds(int trader, int broker, int investor, int oligarch) {
        rankTrader = trader;
        rankBroker = broker;
        rankInvestor = investor;
        rankOligarch = oligarch;
    }

    private void updateRepLevel() {
        // Levels based on concept.md:
        // I. Novice: 0 REP
        // II. Trader: 500 REP
        // III. Broker: 2500 REP
        // IV. Investor: 10000 REP
        // V. Whale: 50000 REP
        if (repPoints >= rankOligarch) {
            this.repLevel = 5;
        } else if (repPoints >= rankInvestor) {
            this.repLevel = 4;
        } else if (repPoints >= rankBroker) {
            this.repLevel = 3;
        } else if (repPoints >= rankTrader) {
            this.repLevel = 2;
        } else {
            this.repLevel = 1;
        }
    }

    public double getBrokerFeeRate() {
        return switch (repLevel) {
            case 5 -> 0.005; // 0.5%
            case 4 -> 0.010; // 1.0%
            case 3 -> 0.015; // 1.5%
            case 2 -> 0.020; // 2.0%
            default -> 0.025; // 2.5%
        };
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
