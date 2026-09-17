package com.ammora.mod.compat.create;

import com.ammora.mod.AmmoraMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder plugin registering Ammora scenes and tags into Create's Ponder Index.
 */
public class AmmoraPonderPlugin implements PonderPlugin {

    public static final ResourceLocation EXCHANGE_TAG = ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "ammora");

    @Override
    public String getModId() {
        return AmmoraMod.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation terminalId = AmmoraMod.EXCHANGE_TERMINAL_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation tradeDockId = AmmoraMod.TRADE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation purchaseDockId = AmmoraMod.PURCHASE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();

        // Terminal scenes
        helper.forComponents(terminalId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "terminal_redstone"),
                        AmmoraPonderScenes::terminalRedstone
                )
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "terminal_display"),
                        AmmoraPonderScenes::terminalDisplay
                );

        // Trade dock scene
        helper.forComponents(tradeDockId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "trade_dock"),
                        AmmoraPonderScenes::tradeDock
                );

        // Purchase dock scene
        helper.forComponents(purchaseDockId)
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "purchase_dock"),
                        AmmoraPonderScenes::purchaseDock
                );
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation terminalId = AmmoraMod.EXCHANGE_TERMINAL_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation tradeDockId = AmmoraMod.TRADE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();
        ResourceLocation purchaseDockId = AmmoraMod.PURCHASE_DOCK_ITEM.get().asItem().builtInRegistryHolder().key().location();

        helper.registerTag(EXCHANGE_TAG)
                .title("ponder.tag.exchange")
                .description("ponder.tag.exchange.description")
                .item(AmmoraMod.EXCHANGE_TERMINAL_ITEM.get())
                .addToIndex()
                .register();

        helper.addToTag(EXCHANGE_TAG)
                .add(terminalId)
                .add(tradeDockId)
                .add(purchaseDockId);
    }
}
