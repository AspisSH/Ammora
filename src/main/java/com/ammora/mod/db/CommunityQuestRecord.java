package com.ammora.mod.db;

import java.util.UUID;

/**
 * Model representing a player-created task/quest/bounty on the community board.
 */
public class CommunityQuestRecord {
    private final String questId;
    private final UUID creatorUuid;
    private final String creatorName;
    private String title;
    private String description;
    private double rewardCbx;
    private String status; // "OPEN", "IN_PROGRESS", "COMPLETED"
    private UUID workerUuid;
    private String workerName;
    private final long createdAt;

    public CommunityQuestRecord(String questId, UUID creatorUuid, String creatorName,
                                String title, String description, double rewardCbx,
                                String status, UUID workerUuid, String workerName, long createdAt) {
        this.questId = questId;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.title = title;
        this.description = description;
        this.rewardCbx = rewardCbx;
        this.status = status;
        this.workerUuid = workerUuid;
        this.workerName = workerName == null ? "" : workerName;
        this.createdAt = createdAt;
    }

    public String getQuestId() { return questId; }
    public UUID getCreatorUuid() { return creatorUuid; }
    public String getCreatorName() { return creatorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getRewardCbx() { return rewardCbx; }
    public void setRewardCbx(double rewardCbx) { this.rewardCbx = rewardCbx; }
    @Deprecated public double getRewardUsdt() { return getRewardCbx(); }
    @Deprecated public void setRewardUsdt(double r) { setRewardCbx(r); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getWorkerUuid() { return workerUuid; }
    public void setWorkerUuid(UUID workerUuid) { this.workerUuid = workerUuid; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName == null ? "" : workerName; }
    public long getCreatedAt() { return createdAt; }

    public boolean isOpen() { return "OPEN".equalsIgnoreCase(status); }
    public boolean isInProgress() { return "IN_PROGRESS".equalsIgnoreCase(status); }
    public boolean isCompleted() { return "COMPLETED".equalsIgnoreCase(status); }
}
