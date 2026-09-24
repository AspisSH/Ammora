package com.ammora.mod.blocks;

import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * BlockItem for Money Blocks (prestigious cubes composed of 81 banknotes = 9 stacks).
 */
public class MoneyBlockItem extends BlockItem {

    private final int denomination;
    private final int totalBanknotes;

    public MoneyBlockItem(Block block, Properties properties, int denomination, int totalBanknotes) {
        super(block, properties);
        this.denomination = denomination;
        this.totalBanknotes = totalBanknotes;
    }

    public int getDenomination() {
        return denomination;
    }

    public int getTotalBanknotes() {
        return totalBanknotes;
    }

    public int getTotalValue() {
        return denomination * totalBanknotes;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(AmmoraLang.tooltip("money_block.value", getTotalValue(), totalBanknotes, denomination));
        tooltipComponents.add(AmmoraLang.tooltip("money_block.desc"));
    }
}
