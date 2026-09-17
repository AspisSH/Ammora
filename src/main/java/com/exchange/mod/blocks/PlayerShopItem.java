package com.exchange.mod.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Item for placing the Player Vending Machine.
 */
public class PlayerShopItem extends BlockItem {

    public PlayerShopItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("player_shop.1"));
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("player_shop.2"));
    }
}
