package com.exchange.mod.compat.create;

import com.exchange.mod.ExchangeMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder plugin registering Ammora scenes and tags into Create's Ponder Index.
 */
public class ExchangePonderPlugin implements PonderPlugin {

    public static final ResourceLocation EXCHANGE_TAG = ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "exchange");

    @Override
    public String getModId() {
        return ExchangeMod.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation terminalId = ExchangeMod.EXCHANGE_TERMINAL_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation tradeDockId = ExchangeMod.TRADE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation purchaseDockId = ExchangeMod.PURCHASE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();

        // Terminal scenes
        helper.forComponents(terminalId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "terminal_redstone"),
                        ExchangePonderScenes::terminalRedstone
                )
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "terminal_display"),
                        ExchangePonderScenes::terminalDisplay
                );

        // Trade dock scene
        helper.forComponents(tradeDockId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "trade_dock"),
                        ExchangePonderScenes::tradeDock
                );

        // Purchase dock scene
        helper.forComponents(purchaseDockId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(ExchangeMod.MODID, "purchase_dock"),
                        ExchangePonderScenes::purchaseDock
                );
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation terminalId = ExchangeMod.EXCHANGE_TERMINAL_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation tradeDockId = ExchangeMod.TRADE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation purchaseDockId = ExchangeMod.PURCHASE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();

        helper.registerTag(EXCHANGE_TAG)
                .title("ponder.tag.exchange")
                .description("ponder.tag.exchange.description")
                .item(ExchangeMod.EXCHANGE_TERMINAL_ITEM.get())
                .addToIndex()
                .register();

        helper.addToTag(EXCHANGE_TAG)
                .add(terminalId)
                .add(tradeDockId)
                .add(purchaseDockId);
    }
}
