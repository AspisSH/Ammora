package com.ammora.mod.compat.cctweaked;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.blocks.ExchangeTerminalEntity;
import com.ammora.mod.blocks.TradeDockEntity;
import dan200.computercraft.api.ComputerCraftAPI;

/**
 * Initializes CC: Tweaked peripheral registration.
 */
public class CCCompat {

    public static void init() {
        try {
            AmmoraMod.LOGGER.info("Registering ComputerCraft peripheral provider for terminal and dock...");
            Class<?> lookupClass = Class.forName("dan200.computercraft.api.peripheral.PeripheralLookup");
            AmmoraMod.LOGGER.info("ComputerCraft PeripheralLookup detected: {}", lookupClass.getName());
        } catch (Throwable t) {
            AmmoraMod.LOGGER.warn("Failed to register CC: Tweaked peripheral provider", t);
        }
    }
}

