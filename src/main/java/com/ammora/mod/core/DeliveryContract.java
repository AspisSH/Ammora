package com.ammora.mod.core;

import java.util.UUID;

/**
 * Model representing a state delivery contract (Futures / Bounties).
 * Guaranteed above-market payout upon full delivery before deadline.
 */
public class DeliveryContract {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private final String contractId;
    private final String title;
    private final String resourceId;
    private final int targetAmount;
    private int deliveredAmount;
    private final double guaranteedUnitPrice;
    private final double collateralCbx;
    private UUID acceptedPlayerUuid;
    private String acceptedPlayerName;
    private long deadlineTick;
    private final int rewardRep;
    private String status;

    public DeliveryContract(String contractId, String title, String resourceId, int targetAmount,
                            int deliveredAmount, double guaranteedUnitPrice, double collateralCbx,
                            UUID acceptedPlayerUuid, String acceptedPlayerName, long deadlineTick,
                            int rewardRep, String status) {
        this.contractId = contractId;
        this.title = title;
        this.resourceId = resourceId;
        this.targetAmount = targetAmount;
        this.deliveredAmount = deliveredAmount;
        this.guaranteedUnitPrice = guaranteedUnitPrice;
        this.collateralCbx = collateralCbx;
        this.acceptedPlayerUuid = acceptedPlayerUuid;
        this.acceptedPlayerName = acceptedPlayerName;
        this.deadlineTick = deadlineTick;
        this.rewardRep = rewardRep;
        this.status = status;
    }

    public String getContractId() {
        return contractId;
    }

    public String getTitle() {
        return title;
    }

    public String getResourceId() {
        return resourceId;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getDeliveredAmount() {
        return deliveredAmount;
    }

    public void setDeliveredAmount(int deliveredAmount) {
        this.deliveredAmount = deliveredAmount;
    }

    public double getGuaranteedUnitPrice() {
        return guaranteedUnitPrice;
    }

    public double getCollateralCbx() {
        return collateralCbx;
    }

    @Deprecated
    public double getCollateralUsdt() {
        return getCollateralCbx();
    }

    public UUID getAcceptedPlayerUuid() {
        return acceptedPlayerUuid;
    }

    public void setAcceptedPlayerUuid(UUID acceptedPlayerUuid) {
        this.acceptedPlayerUuid = acceptedPlayerUuid;
    }

    public String getAcceptedPlayerName() {
        return acceptedPlayerName;
    }

    public void setAcceptedPlayerName(String acceptedPlayerName) {
        this.acceptedPlayerName = acceptedPlayerName;
    }

    public long getDeadlineTick() {
        return deadlineTick;
    }

    public void setDeadlineTick(long deadlineTick) {
        this.deadlineTick = deadlineTick;
    }

    public int getRewardRep() {
        return rewardRep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public double getTotalPayout() {
        return targetAmount * guaranteedUnitPrice;
    }
}
