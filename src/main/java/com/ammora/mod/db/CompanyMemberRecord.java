package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a member of a company, their role, and their daily spending limit.
 */
public class CompanyMemberRecord {
    private final String companyId;
    private final UUID playerUuid;
    private String playerName;
    private String role; // "OWNER", "MANAGER", "MEMBER"
    private double dailyLimitCbx;
    private double spentTodayCbx;
    private long lastSpentDay;
    private final long joinedAt;

    public CompanyMemberRecord(String companyId, UUID playerUuid, String playerName, String role,
                               double dailyLimitCbx, double spentTodayCbx, long lastSpentDay, long joinedAt) {
        this.companyId = companyId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role != null ? role.toUpperCase() : "MEMBER";
        this.dailyLimitCbx = Math.max(0.0, dailyLimitCbx);
        this.spentTodayCbx = Math.max(0.0, spentTodayCbx);
        this.lastSpentDay = lastSpentDay;
        this.joinedAt = joinedAt;
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

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : "MEMBER";
    }

    public double getDailyLimitCbx() {
        return dailyLimitCbx;
    }

    public void setDailyLimitCbx(double dailyLimitCbx) {
        this.dailyLimitCbx = Math.max(0.0, dailyLimitCbx);
    }

    public double getSpentTodayCbx() {
        return spentTodayCbx;
    }

    public void setSpentTodayCbx(double spentTodayCbx) {
        this.spentTodayCbx = Math.max(0.0, spentTodayCbx);
    }

    public long getLastSpentDay() {
        return lastSpentDay;
    }

    public void setLastSpentDay(long lastSpentDay) {
        this.lastSpentDay = lastSpentDay;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public boolean isOwner() {
        return "OWNER".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }

    public boolean isMember() {
        return "MEMBER".equalsIgnoreCase(role);
    }

    public void checkAndResetDailyLimit() {
        long currentDay = System.currentTimeMillis() / 86400000L;
        if (currentDay > this.lastSpentDay) {
            this.spentTodayCbx = 0.0;
            this.lastSpentDay = currentDay;
        }
    }

    public double getRemainingDailyLimit() {
        if (isOwner()) {
            return Double.MAX_VALUE;
        }
        checkAndResetDailyLimit();
        return Math.max(0.0, dailyLimitCbx - spentTodayCbx);
    }

    public boolean canSpend(double amount) {
        if (amount <= 0.0) return false;
        if (isOwner()) return true;
        if (dailyLimitCbx <= 0.0) return false;
        checkAndResetDailyLimit();
        return (spentTodayCbx + amount) <= (dailyLimitCbx + 0.0001);
    }

    public void recordSpend(double amount) {
        if (amount > 0.0 && !isOwner()) {
            checkAndResetDailyLimit();
            this.spentTodayCbx += amount;
        }
    }
}
