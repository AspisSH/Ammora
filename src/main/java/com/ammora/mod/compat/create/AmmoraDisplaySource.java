package com.ammora.mod.compat.create;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.blocks.ExchangeTerminalEntity;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.core.MarketResource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

/**
 * Display Link source providing ticker prices and warehouse alerts to Create Display Boards and Nixie Tubes.
 */
public class AmmoraDisplaySource extends DisplaySource {

    public static final AmmoraDisplaySource INSTANCE = new AmmoraDisplaySource();

    public static void register() {
        try {
            DisplaySource.BY_BLOCK_ENTITY.register(AmmoraMod.EXCHANGE_TERMINAL_BE.get(), List.of(INSTANCE));
            DisplaySource.BY_BLOCK.register(AmmoraMod.EXCHANGE_TERMINAL.get(), List.of(INSTANCE));
            DisplaySource.BY_BLOCK_ENTITY.register(AmmoraMod.TRADE_DOCK_BE.get(), List.of(INSTANCE));
            DisplaySource.BY_BLOCK.register(AmmoraMod.TRADE_DOCK.get(), List.of(INSTANCE));
            DisplaySource.BY_BLOCK_ENTITY.register(AmmoraMod.PURCHASE_DOCK_BE.get(), List.of(INSTANCE));
            DisplaySource.BY_BLOCK.register(AmmoraMod.PURCHASE_DOCK.get(), List.of(INSTANCE));
            AmmoraMod.LOGGER.info("Create integration: Registered AmmoraDisplaySource.INSTANCE to BY_BLOCK and BY_BLOCK_ENTITY");
        } catch (Throwable t) {
            AmmoraMod.LOGGER.error("Create integration: Failed to register DisplaySource to BY_BLOCK/BY_BLOCK_ENTITY", t);
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("display_source.ammora.market_quotes");
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 40; // Refresh every 2 seconds
    }

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        String resId = "minecraft:iron_ingot";
        if (context.getSourceBlockEntity() instanceof ExchangeTerminalEntity terminal) {
            resId = terminal.getMonitoredResource();
        } else if (context.getSourceBlockEntity() instanceof com.ammora.mod.blocks.TradeDockEntity dock) {
            resId = dock.getActiveResourceId();
        } else if (context.getSourceBlockEntity() instanceof com.ammora.mod.blocks.PurchaseDockEntity pdock) {
            resId = pdock.getTargetResourceId();
        }

        if (AmmoraMod.getMarketManager() != null) {
            MarketResource res = AmmoraMod.getMarketManager().getResource(resId);
            if (res != null) {
                double spot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                double buy = MarketEngine.calculateBuyPrice(res.getCurrentStock(), res);
                double sell = MarketEngine.calculateSellPrice(res.getCurrentStock(), res);
                double fill = (res.getCurrentStock() / res.getMaxReserve()) * 100.0;

                String name = res.getDisplayName().toUpperCase(Locale.ROOT);
                // Line 1: Ticker and prices
                String ticker = name + ": " + MarketEngine.round2(spot) + " CBX";
                // Line 2: Buy / Sell spread
                MutableComponent spread = Component.translatable("display_source.ammora.spread",
                        String.valueOf(MarketEngine.round2(buy)), String.valueOf(MarketEngine.round2(sell)));
                // Line 3: Stock fill status
                MutableComponent stockStatus = Component.translatable("display_source.ammora.stock_fill",
                        (int) Math.round(fill), (int) res.getCurrentStock());

                return List.of(
                        Component.literal(ticker),
                        spread,
                        stockStatus
                );
            }
        }
        return List.of(Component.translatable("display_source.ammora.no_data"));
    }

    @Override
    public List<List<MutableComponent>> provideFlapDisplayText(DisplayLinkContext context, DisplayTargetStats stats) {
        return provideText(context, stats).stream().map(List::of).toList();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AmmoraDisplaySource;
    }

    @Override
    public int hashCode() {
        return AmmoraMod.MODID.hashCode();
    }
}
