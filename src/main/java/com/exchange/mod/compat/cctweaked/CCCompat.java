package com.exchange.mod.compat.cctweaked;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.blocks.ExchangeTerminalEntity;
import com.exchange.mod.blocks.TradeDockEntity;
import dan200.computercraft.api.ComputerCraftAPI;

/**
 * Initializes CC: Tweaked peripheral registration.
 */
public class CCCompat {

    public static void init() {
        try {
            ExchangeMod.LOGGER.info("Registering ComputerCraft peripheral provider for terminal and dock...");
            Class<?> lookupClass = Class.forName("dan200.computercraft.api.peripheral.PeripheralLookup");
            ExchangeMod.LOGGER.info("ComputerCraft PeripheralLookup detected: {}", lookupClass.getName());
        } catch (Throwable t) {
            ExchangeMod.LOGGER.warn("Failed to register CC: Tweaked peripheral provider", t);
        }
    }
}

