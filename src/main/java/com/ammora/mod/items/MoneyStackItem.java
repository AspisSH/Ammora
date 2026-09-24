package com.ammora.mod.items;

import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Money stack (bundle of 9 banknotes of a specific denomination) item.
 */
public class MoneyStackItem extends Item {

    private final int denomination;
    private final int banknoteCount;

    public MoneyStackItem(Properties properties, int denomination, int banknoteCount) {
        super(properties);
        this.denomination = denomination;
        this.banknoteCount = banknoteCount;
    }

    public int getDenomination() {
        return denomination;
    }

    public int getBanknoteCount() {
        return banknoteCount;
    }

    public int getTotalValue() {
        return denomination * banknoteCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(AmmoraLang.tooltip("money_stack.value", getTotalValue(), banknoteCount, denomination));
        tooltipComponents.add(AmmoraLang.tooltip("money_stack.desc"));
    }
}
