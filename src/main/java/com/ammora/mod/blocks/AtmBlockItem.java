package com.ammora.mod.blocks;

import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * BlockItem for the 2-block tall Automated Teller Machine (ATM).
 */
public class AtmBlockItem extends BlockItem {

    public AtmBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(AmmoraLang.tooltip("atm.1"));
        tooltipComponents.add(AmmoraLang.tooltip("atm.2"));
        tooltipComponents.add(AmmoraLang.tooltip("atm.3"));
    }
}
