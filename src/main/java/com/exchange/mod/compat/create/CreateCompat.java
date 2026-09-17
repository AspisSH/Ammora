package com.exchange.mod.compat.create;

import com.exchange.mod.ExchangeMod;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Handles initialization of Create mod features when Create is present.
 */
public class CreateCompat {

    public static void registerDisplaySources(RegisterEvent event) {
        event.register(
                com.simibubi.create.api.registry.CreateRegistries.DISPLAY_SOURCE,
                helper -> {
                    helper.register(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "terminal_quotes"),
                            ExchangeDisplaySource.INSTANCE
                    );
                    ExchangeMod.LOGGER.info("Create integration: Registered ExchangeDisplaySource in RegisterEvent");
                }
        );
    }

    public static void init() {
        try {
            ExchangeMod.LOGGER.info("Create integration: Registering Display Link sources to BY_BLOCK and BY_BLOCK_ENTITY...");
            // Initialized inside try block to isolate classloading
            ExchangeDisplaySource.register();
        } catch (Throwable t) {
            ExchangeMod.LOGGER.warn("Failed to initialize Create Display Link integration", t);
        }

        try {
            ExchangeMod.LOGGER.info("Create integration: Registering Ponder scenes...");
            net.createmod.ponder.foundation.PonderIndex.addPlugin(new ExchangePonderPlugin());
            ExchangeMod.LOGGER.info("Create integration: Registered ExchangePonderPlugin successfully");
        } catch (Throwable t) {
            ExchangeMod.LOGGER.warn("Failed to initialize Create Ponder integration", t);
        }
    }

    /**
     * Calculates transfer speed in ticks based on Create RPM.
     * At 128 RPM or higher, transfer runs at 1 tick/op.
     */
    public static int calculateTransferSpeed(float rpm) {
        float absRpm = Math.abs(rpm);
        if (absRpm <= 0) return 8; // Default hopper speed
        return Math.max(1, Math.round((8.0f * 16.0f) / absRpm));
    }
}
