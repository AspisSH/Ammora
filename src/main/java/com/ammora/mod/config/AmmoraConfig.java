package com.ammora.mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Central configuration specification for the Ammora mod.
 * Manages currency settings, macroeconomic daily cycles, reputation milestones,
 * market events, and player shop upgrade costs.
 */
public class AmmoraConfig {
    public static ModConfigSpec SPEC;

    // Currency & Research
    public static ModConfigSpec.ConfigValue<String> CURRENCY_NAME;
    public static ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL;
    public static ModConfigSpec.BooleanValue REQUIRE_RESOURCE_RESEARCH;

    // Daily Macroeconomics
    public static ModConfigSpec.BooleanValue ENABLE_DAILY_FLUCTUATIONS;
    public static ModConfigSpec.DoubleValue MAX_DAILY_FLUCTUATION;
    public static ModConfigSpec.DoubleValue SURPLUS_BURN_RATE;
    public static ModConfigSpec.DoubleValue DAILY_CARRY_FEE_RATE;

    // Market Events
    public static ModConfigSpec.BooleanValue ENABLE_MARKET_EVENTS;
    public static ModConfigSpec.IntValue EVENT_INTERVAL_DAYS;
    public static ModConfigSpec.DoubleValue EVENT_CHANCE;
    public static ModConfigSpec.IntValue EVENT_DURATION_DAYS;

    // Reputation Ranks
    public static ModConfigSpec.IntValue RANK_TRADER_SCORE;
    public static ModConfigSpec.IntValue RANK_BROKER_SCORE;
    public static ModConfigSpec.IntValue RANK_INVESTOR_SCORE;
    public static ModConfigSpec.IntValue RANK_OLIGARCH_SCORE;

    // Vending Machine Upgrades
    public static ModConfigSpec.DoubleValue SHOP_CAPACITY_128_COST;
    public static ModConfigSpec.DoubleValue SHOP_CAPACITY_256_COST;
    public static ModConfigSpec.DoubleValue SHOP_CAPACITY_512_COST;
    public static ModConfigSpec.DoubleValue SHOP_CAPACITY_1024_COST;
    public static ModConfigSpec.DoubleValue SHOP_SLOT_6_COST;
    public static ModConfigSpec.DoubleValue SHOP_SLOT_7_COST;
    public static ModConfigSpec.DoubleValue SHOP_SLOT_8_COST;
    public static ModConfigSpec.DoubleValue SHOP_SLOT_9_COST;
    public static ModConfigSpec.DoubleValue SHOP_SLOT_10_COST;
    public static ModConfigSpec.DoubleValue SHOP_SATELLITE_MODULE_COST;

    static {
        ModConfigSpec built = null;
        try {
            built = buildConfigSpec();
        } catch (Throwable ignored) {
            // Running in standalone test environment without NeoForge classpath
        }
        SPEC = built;
    }

    private static ModConfigSpec buildConfigSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Ammora Economic Simulation Settings").push("economy");

        CURRENCY_NAME = builder
                .comment("Display name of the currency")
                .define("currencyName", "ChainBX");

        CURRENCY_SYMBOL = builder
                .comment("Ticker symbol of the currency")
                .define("currencySymbol", "CBX");

        REQUIRE_RESOURCE_RESEARCH = builder
                .comment("Require players to unlock/research a resource by consuming 1 unit before trading it")
                .define("requireResourceResearch", true);

        ENABLE_DAILY_FLUCTUATIONS = builder
                .comment("Enable random daily price fluctuations in the range of +-10% at the start of each in-game day.")
                .define("enableDailyFluctuations", true);

        MAX_DAILY_FLUCTUATION = builder
                .comment("Maximum percentage for daily price fluctuation (0.10 = 10%)")
                .defineInRange("maxDailyFluctuation", 0.10, 0.0, 0.50);

        SURPLUS_BURN_RATE = builder
                .comment("Daily mean reversion burn percentage applied to surplus reserves over targetReserve (0.08 = 8%)")
                .defineInRange("surplusBurnRate", 0.08, 0.0, 0.50);

        DAILY_CARRY_FEE_RATE = builder
                .comment("Daily carry fee rate charged for physical commodity warehousing (0.0005 = 0.05%)")
                .defineInRange("dailyCarryFeeRate", 0.0005, 0.0, 0.05);

        ENABLE_MARKET_EVENTS = builder
                .comment("Enable random news and market events affecting commodity prices.")
                .define("enableMarketEvents", true);

        EVENT_INTERVAL_DAYS = builder
                .comment("Check for random market event every N in-game days")
                .defineInRange("eventIntervalDays", 3, 1, 30);

        EVENT_CHANCE = builder
                .comment("Probability of an event triggering on the check day (0.6 = 60%)")
                .defineInRange("eventChance", 0.60, 0.0, 1.0);

        EVENT_DURATION_DAYS = builder
                .comment("Duration of active market events in in-game days")
                .defineInRange("eventDurationDays", 2, 1, 10);

        builder.pop();

        // Reputation Rank Thresholds
        builder.comment("Player Reputation Rank Thresholds").push("reputation");
        RANK_TRADER_SCORE = builder.comment("Score required to reach Trader rank (Level 2)").defineInRange("rankTrader", 500, 0, 1_000_000);
        RANK_BROKER_SCORE = builder.comment("Score required to reach Broker rank (Level 3)").defineInRange("rankBroker", 2500, 0, 1_000_000);
        RANK_INVESTOR_SCORE = builder.comment("Score required to reach Investor rank (Level 4)").defineInRange("rankInvestor", 10000, 0, 1_000_000);
        RANK_OLIGARCH_SCORE = builder.comment("Score required to reach Oligarch/Whale rank (Level 5)").defineInRange("rankOligarch", 50000, 0, 10_000_000);
        builder.pop();

        // Vending Machine Upgrades
        builder.comment("Player Shop (Vending Machine) Upgrade Costs in CBX").push("vending_upgrades");
        SHOP_CAPACITY_128_COST = builder.comment("Upgrade cost for 128 item slot capacity").defineInRange("capacity128Cost", 250.0, 0.0, 1_000_000.0);
        SHOP_CAPACITY_256_COST = builder.comment("Upgrade cost for 256 item slot capacity").defineInRange("capacity256Cost", 500.0, 0.0, 1_000_000.0);
        SHOP_CAPACITY_512_COST = builder.comment("Upgrade cost for 512 item slot capacity").defineInRange("capacity512Cost", 1000.0, 0.0, 1_000_000.0);
        SHOP_CAPACITY_1024_COST = builder.comment("Upgrade cost for 1024 item slot capacity").defineInRange("capacity1024Cost", 2500.0, 0.0, 1_000_000.0);

        SHOP_SLOT_6_COST = builder.comment("Unlock cost for slot #6").defineInRange("slot6Cost", 100.0, 0.0, 1_000_000.0);
        SHOP_SLOT_7_COST = builder.comment("Unlock cost for slot #7").defineInRange("slot7Cost", 200.0, 0.0, 1_000_000.0);
        SHOP_SLOT_8_COST = builder.comment("Unlock cost for slot #8").defineInRange("slot8Cost", 300.0, 0.0, 1_000_000.0);
        SHOP_SLOT_9_COST = builder.comment("Unlock cost for slot #9").defineInRange("slot9Cost", 400.0, 0.0, 1_000_000.0);
        SHOP_SLOT_10_COST = builder.comment("Unlock cost for slot #10").defineInRange("slot10Cost", 500.0, 0.0, 1_000_000.0);

        SHOP_SATELLITE_MODULE_COST = builder.comment("Cost of Satellite network transmitter module").defineInRange("satelliteModuleCost", 500.0, 0.0, 1_000_000.0);
        builder.pop();

        return builder.build();
    }

    public static String getCurrencySymbol() {
        try {
            return (CURRENCY_SYMBOL != null && CURRENCY_SYMBOL.get() != null) ? CURRENCY_SYMBOL.get() : "CBX";
        } catch (Throwable e) {
            return "CBX";
        }
    }

    public static String getCurrencyName() {
        try {
            return (CURRENCY_NAME != null && CURRENCY_NAME.get() != null) ? CURRENCY_NAME.get() : "ChainBX";
        } catch (Throwable e) {
            return "ChainBX";
        }
    }

    public static boolean isResourceResearchRequired() {
        try {
            return REQUIRE_RESOURCE_RESEARCH != null ? REQUIRE_RESOURCE_RESEARCH.get() : true;
        } catch (Throwable e) {
            return true;
        }
    }

    public static double getSurplusBurnRate() {
        try {
            return SURPLUS_BURN_RATE != null ? SURPLUS_BURN_RATE.get() : 0.08;
        } catch (Throwable e) {
            return 0.08;
        }
    }

    public static double getDailyCarryFeeRate() {
        try {
            return DAILY_CARRY_FEE_RATE != null ? DAILY_CARRY_FEE_RATE.get() : 0.0005;
        } catch (Throwable e) {
            return 0.0005;
        }
    }

    public static int getRankThreshold(int level) {
        try {
            return switch (level) {
                case 2 -> (RANK_TRADER_SCORE != null ? RANK_TRADER_SCORE.get() : 500);
                case 3 -> (RANK_BROKER_SCORE != null ? RANK_BROKER_SCORE.get() : 2500);
                case 4 -> (RANK_INVESTOR_SCORE != null ? RANK_INVESTOR_SCORE.get() : 10000);
                case 5 -> (RANK_OLIGARCH_SCORE != null ? RANK_OLIGARCH_SCORE.get() : 50000);
                default -> 0;
            };
        } catch (Throwable e) {
            return switch (level) {
                case 2 -> 500;
                case 3 -> 2500;
                case 4 -> 10000;
                case 5 -> 50000;
                default -> 0;
            };
        }
    }

    public static double getCapacityUpgradeCost(int currentCap) {
        try {
            if (currentCap < 128) return SHOP_CAPACITY_128_COST != null ? SHOP_CAPACITY_128_COST.get() : 250.0;
            if (currentCap < 256) return SHOP_CAPACITY_256_COST != null ? SHOP_CAPACITY_256_COST.get() : 500.0;
            if (currentCap < 512) return SHOP_CAPACITY_512_COST != null ? SHOP_CAPACITY_512_COST.get() : 1000.0;
            return SHOP_CAPACITY_1024_COST != null ? SHOP_CAPACITY_1024_COST.get() : 2500.0;
        } catch (Throwable e) {
            if (currentCap < 128) return 250.0;
            if (currentCap < 256) return 500.0;
            if (currentCap < 512) return 1000.0;
            return 2500.0;
        }
    }

    public static double getSlotUnlockCost(int nextSlot) {
        try {
            return switch (nextSlot) {
                case 6 -> (SHOP_SLOT_6_COST != null ? SHOP_SLOT_6_COST.get() : 100.0);
                case 7 -> (SHOP_SLOT_7_COST != null ? SHOP_SLOT_7_COST.get() : 200.0);
                case 8 -> (SHOP_SLOT_8_COST != null ? SHOP_SLOT_8_COST.get() : 300.0);
                case 9 -> (SHOP_SLOT_9_COST != null ? SHOP_SLOT_9_COST.get() : 400.0);
                case 10 -> (SHOP_SLOT_10_COST != null ? SHOP_SLOT_10_COST.get() : 500.0);
                default -> 100.0;
            };
        } catch (Throwable e) {
            return switch (nextSlot) {
                case 6 -> 100.0;
                case 7 -> 200.0;
                case 8 -> 300.0;
                case 9 -> 400.0;
                case 10 -> 500.0;
                default -> 100.0;
            };
        }
    }

    public static double getSatelliteModuleCost() {
        try {
            return SHOP_SATELLITE_MODULE_COST != null ? SHOP_SATELLITE_MODULE_COST.get() : 500.0;
        } catch (Throwable e) {
            return 500.0;
        }
    }
}
