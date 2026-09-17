package com.ammora.mod.core;

import java.util.*;

/**
 * Manager handling Unallocated Metal Accounts (OMS / Synthetic Commodity Accounts).
 */
public class OMSManager {

private final Map<UUID, List<OMSPosition>> playerPositions = new HashMap<>();

    /**
     * Opens an OMS position by converting CBX into virtual commodity units.
     */
    public OMSPosition openPosition(UUID playerUuid, MarketResource resource, double cbxAmount) {
        if (cbxAmount <= 0) {
            throw new IllegalArgumentException("Investment amount must be positive");
        }
        double unitBuyPrice = MarketEngine.calculateBuyPrice(resource.getCurrentStock(), resource);
        double unitsPurchased = cbxAmount / unitBuyPrice;

        String id = UUID.randomUUID().toString();
        OMSPosition position = new OMSPosition(
                id,
                playerUuid,
                resource.getResourceId(),
                unitsPurchased,
                cbxAmount,
                unitBuyPrice,
                System.currentTimeMillis()
        );

        playerPositions.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(position);
        return position;
    }

    /**
     * Closes an OMS position (or partial amount) into CBX based on current market sell price.
     *
     * @return Payout in CBX
     */
    public double closePosition(OMSPosition position, double unitsToClose, MarketResource resource) {
        if (unitsToClose <= 0.0) {
            unitsToClose = position.getAmountUnits();
        }
        if (unitsToClose > position.getAmountUnits() + 0.005) {
            throw new IllegalArgumentException("Invalid units amount to close");
        }
        unitsToClose = Math.min(unitsToClose, position.getAmountUnits());

        double unitSellPrice = MarketEngine.calculateSellPrice(resource.getCurrentStock(), resource);
        double payout = unitsToClose * unitSellPrice;

        double remaining = position.getAmountUnits() - unitsToClose;
        position.setAmountUnits(remaining);

        if (remaining <= 0.001) {
            List<OMSPosition> list = playerPositions.get(position.getPlayerUuid());
            if (list != null) {
                list.removeIf(p -> p.getPositionId().equals(position.getPositionId()));
            }
        }

        return MarketEngine.round2(payout);
    }

    /**
     * Applies a periodic carry fee (storage cost) across all open positions.
     */
    public void applyCarryFee(double rate) {
        for (List<OMSPosition> list : playerPositions.values()) {
            for (OMSPosition pos : list) {
                double deductedUnits = pos.getAmountUnits() * rate;
                pos.setAmountUnits(Math.max(0.0, pos.getAmountUnits() - deductedUnits));
            }
            list.removeIf(p -> p.getAmountUnits() <= 0.001);
        }
    }

    public List<OMSPosition> getPositions(UUID playerUuid) {
        List<OMSPosition> list = playerPositions.getOrDefault(playerUuid, Collections.emptyList());
        return list.stream().filter(p -> p.getAmountUnits() > 0.001).toList();
    }

    public void loadPositions(List<OMSPosition> positions) {
        playerPositions.clear();
        for (OMSPosition pos : positions) {
            if (pos.getAmountUnits() > 0.001) {
                playerPositions.computeIfAbsent(pos.getPlayerUuid(), k -> new ArrayList<>()).add(pos);
            }
        }
    }
}

