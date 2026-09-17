package com.exchange.mod.test;

import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MarketEngineTest {

    private MarketResource ironResource;

    @BeforeEach
    public void setup() {
        // P0=10.0, Starget=10000, S=10000, k=0.8, Smax=15000, alpha=15.0, fee=0.02, floor=0.10
        ironResource = new MarketResource(
                "minecraft:iron_ingot",
                "Iron Ingot",
                10.0,
                10000.0,
                10000.0,
                0.8,
                15000.0,
                15.0,
                0.02,
                0.10
        );
    }

    @Test
    @DisplayName("Normal balanced market price should match base price with fees")
    public void testBalancedMarket() {
        double spot = MarketEngine.calculateSpotPrice(10000.0, ironResource);
        assertEquals(10.0, spot, 0.01);

        double buy = MarketEngine.calculateBuyPrice(10000.0, ironResource);
        assertEquals(10.2, buy, 0.01);

        double sell = MarketEngine.calculateSellPrice(10000.0, ironResource);
        assertEquals(9.8, sell, 0.01);

        double disposal = MarketEngine.calculateDisposalFee(10000.0, ironResource);
        assertEquals(0.0, disposal, 0.001);
    }

    @Test
    @DisplayName("Deficit raises prices above base")
    public void testDeficitMarket() {
        double spot = MarketEngine.calculateSpotPrice(5000.0, ironResource);
        assertTrue(spot > 17.0 && spot < 18.0, "Expected spot around 17.4, got: " + spot);

        double buy = MarketEngine.calculateBuyPrice(5000.0, ironResource);
        double sell = MarketEngine.calculateSellPrice(5000.0, ironResource);

        assertTrue(buy > spot);
        assertTrue(sell > 16.0);
    }

    @Test
    @DisplayName("Surplus above S_max triggers ecological disposal fee")
    public void testDisposalFeeTrigger() {
        // At S = 18000, S_max = 15000: excess = 3000, ratio = 0.3, ratio^2 = 0.09, alpha*0.09 = 1.35
        double disposal = MarketEngine.calculateDisposalFee(18000.0, ironResource);
        assertEquals(1.35, disposal, 0.01);

        double sell = MarketEngine.calculateSellPrice(18000.0, ironResource);
        // Spot is ~6.24. Fee 2% -> ~6.11. 6.11 - 1.35 = ~4.76
        assertTrue(sell > 4.5 && sell < 5.0, "Expected sell around 4.76, got: " + sell);
    }

    @Test
    @DisplayName("Extreme surplus results in negative sell price (seller pays disposal fee)")
    public void testNegativePricing() {
        // At S = 23000, excess = 8000, ratio = 0.8, ratio^2 = 0.64, alpha*0.64 = 9.60
        double disposal = MarketEngine.calculateDisposalFee(23000.0, ironResource);
        assertEquals(9.60, disposal, 0.01);

        double sell = MarketEngine.calculateSellPrice(23000.0, ironResource);
        assertTrue(sell < 0.0, "Sell price must be negative when disposal fee exceeds revenue, got: " + sell);
        assertTrue(sell < -4.0 && sell > -5.0, "Expected sell around -4.56, got: " + sell);
    }

    @Test
    @DisplayName("Mean reversion reduces excess stock towards target")
    public void testMeanReversion() {
        ironResource.setCurrentStock(20000.0);
        double burned = MarketEngine.applyMeanReversion(ironResource, 0.10);
        assertEquals(1000.0, burned, 0.01);
        assertEquals(19000.0, ironResource.getCurrentStock(), 0.01);
    }
}
