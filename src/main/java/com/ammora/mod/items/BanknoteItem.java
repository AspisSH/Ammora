package com.ammora.mod.items;

import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Physical currency banknote item in the Ammora financial mod.
 * Denominations: 10, 100, 1,000 CBX.
 */
public class BanknoteItem extends Item {

    private final int denomination;

    public BanknoteItem(Properties properties, int denomination) {
        super(properties);
        this.denomination = denomination;
    }

    public int getDenomination() {
        return denomination;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(AmmoraLang.tooltip("banknote.denomination", denomination));
        tooltipComponents.add(AmmoraLang.tooltip("banknote.desc"));
    }
}
