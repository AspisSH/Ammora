package com.exchange.mod.blocks;

import com.exchange.mod.ExchangeMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Custom BlockItem for the Exchange Terminal.
 * Can be placed down like a normal block, or right-clicked in the air / shift-clicked on a block
 * to open the mobile cyberpunk trading terminal GUI directly from hand.
 */
public class ExchangeTerminalItem extends BlockItem {

    public ExchangeTerminalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ExchangeMod.openTerminalScreen(serverPlayer, player.blockPosition());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        // If holding Shift and clicking on a block, open the mobile terminal rather than placing it
        if (player != null && player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
                ExchangeMod.openTerminalScreen(serverPlayer, player.blockPosition());
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        // Otherwise, place the block normally
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("exchange_terminal.1"));
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("exchange_terminal.2"));
    }
}
