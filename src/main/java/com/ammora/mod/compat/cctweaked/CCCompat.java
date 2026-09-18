package com.ammora.mod.compat.cctweaked;

import com.ammora.mod.AmmoraMod;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Initializes CC: Tweaked peripheral registration.
 */
public class CCCompat {

    public static final BlockCapability<IPeripheral, Direction> PERIPHERAL_CAPABILITY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("computercraft", "peripheral"), IPeripheral.class);

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        try {
            event.registerBlockEntity(
                    PERIPHERAL_CAPABILITY,
                    AmmoraMod.EXCHANGE_TERMINAL_BE.get(),
                    (terminal, side) -> new AmmoraPeripheral(terminal)
            );
            event.registerBlockEntity(
                    PERIPHERAL_CAPABILITY,
                    AmmoraMod.TRADE_DOCK_BE.get(),
                    (dock, side) -> new AmmoraPeripheral(dock)
            );
            AmmoraMod.LOGGER.info("CC: Tweaked peripheral capability registered for Terminal and Trade Dock.");
        } catch (Throwable t) {
            AmmoraMod.LOGGER.warn("Failed to register CC: Tweaked peripheral provider", t);
        }
    }

    public static void init() {
        AmmoraMod.LOGGER.info("CC: Tweaked integration initialized.");
    }
}
