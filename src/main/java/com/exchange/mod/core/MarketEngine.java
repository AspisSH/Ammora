package com.exchange.mod.core;

/**
 * Core mathematical engine for the exchange.
 * Implements Automated Market Maker (AMM) pricing, negative disposal fees,
 * and mean-reversion sinking.
 */
public class MarketEngine {

    /**
     * Calculates the raw spot price P(S) = P_0 * (S_target / S)^k.
     */
    public static double calculateSpotPrice(double stock, MarketResource res) {
        double safeStock = Math.max(1.0, stock);
        return res.getEffectiveBasePrice() * Math.pow(res.getTargetReserve() / safeStock, res.getElasticity());
    }

    /**
     * Calculates the ecological waste disposal fee C_disposal(S).
     * If S > S_max, returns alpha * ((S - S_max) / S_target)^2, otherwise 0.
     */
    public static double calculateDisposalFee(double stock, MarketResource res) {
        if (stock <= res.getMaxReserve()) {
            return 0.0;
        }
        double excess = stock - res.getMaxReserve();
        double ratio = excess / res.getTargetReserve();
        return res.getDisposalAlpha() * (ratio * ratio);
    }

    /**
     * Calculates the unit price for a player buying from the exchange.
     * P_buy(S) = max(P(S) * (1 + fee), floor)
     */
    public static double calculateBuyPrice(double stock, MarketResource res) {
        double spot = calculateSpotPrice(stock, res);
        double price = spot * (1.0 + res.getFeeRate());
        return Math.max(price, res.getMinPriceFloor());
    }

    /**
     * Calculates the unit price for a player selling to the exchange.
     * P_sell(S) = P(S) * (1 - fee) - C_disposal(S)
     * NOTE: Can be negative when stock exceeds S_max!
     */
    public static double calculateSellPrice(double stock, MarketResource res) {
        double spot = calculateSpotPrice(stock, res);
        double feeDeduction = spot * (1.0 - res.getFeeRate());
        double disposalFee = calculateDisposalFee(stock, res);
        return feeDeduction - disposalFee;
    }

    /**
     * Calculates the total cost for buying N items.
     * Uses midpoint stock level to account for AMM curve shift during the batch.
     */
    public static double calculateTotalBuyCost(int amount, MarketResource res) {
        if (amount <= 0) return 0.0;
        double currentStock = res.getCurrentStock();
        // As items are bought, remaining stock decreases, driving price up
        double avgStock = Math.max(1.0, currentStock - (amount / 2.0));
        double avgUnitPrice = calculateBuyPrice(avgStock, res);
        return round2(amount * avgUnitPrice);
    }

    /**
     * Calculates the total payout (or disposal bill if negative) for selling N items.
     * Uses midpoint stock level as inventory fills up.
     */
    public static double calculateTotalSellPayout(int amount, MarketResource res) {
        if (amount <= 0) return 0.0;
        double currentStock = res.getCurrentStock();
        // As items are sold, stock increases, lowering price or triggering disposal fee
        double avgStock = currentStock + (amount / 2.0);
        double avgUnitPrice = calculateSellPrice(avgStock, res);
        return round2(amount * avgUnitPrice);
    }

    /**
     * Applies periodic government order / export consumption (Mean Reversion).
     * S_new = S - (S - S_target) * beta
     *
     * @param beta rate of surplus drain, e.g. 0.05 to 0.10
     * @return number of units burned/consumed
     */
    public static double applyMeanReversion(MarketResource res, double beta) {
        double stock = res.getCurrentStock();
        double target = res.getTargetReserve();
        if (stock <= target) {
            return 0.0;
        }
        double surplus = stock - target;
        double consumed = surplus * beta;
        res.setCurrentStock(stock - consumed);
        return consumed;
    }

    /**
     * Helper to round double values to 2 decimal places.
     */
    public static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    /**
     * Calculates Redstone signal strength (0-15) based on mode:
     * - MODE_WAREHOUSE_FILL (0): Stock / MaxReserve * 15
     * - MODE_PRICE_RATIO (1): SpotPrice / (BasePrice * 2) * 15
     * - MODE_PRICE_THRESHOLD (2): 15 if (isLessThan ? sellPrice < threshold : sellPrice > threshold), else 0
     */
    public static int calculateRedstoneSignal(int mode, MarketResource res, double thresholdPrice, boolean isLessThan) {
        if (res == null) return 0;
        switch (mode) {
            case 1 -> { // MODE_PRICE_RATIO
                double spot = calculateSpotPrice(res.getCurrentStock(), res);
                double ratio = spot / Math.max(0.1, res.getBasePrice() * 2.0);
                return (int) Math.min(15, Math.max(0, Math.round(ratio * 15)));
            }
            case 2 -> { // MODE_PRICE_THRESHOLD
                double sellPrice = calculateSellPrice(res.getCurrentStock(), res);
                boolean triggered = isLessThan ? (sellPrice < thresholdPrice) : (sellPrice > thresholdPrice);
                return triggered ? 15 : 0;
            }
            case 0 -> { // MODE_WAREHOUSE_FILL
                double ratio = res.getCurrentStock() / res.getMaxReserve();
                return (int) Math.min(15, Math.max(0, Math.round(ratio * 15)));
            }
            default -> {
                return 0;
            }
        }
    }
}
