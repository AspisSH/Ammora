package com.ammora.mod.items;

import com.ammora.mod.network.PacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Cold Wallet item for P2P contactless CBX transactions and ledger auditing.
 */
public class ColdWalletItem extends Item {

    public ColdWalletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            com.ammora.mod.AmmoraMod.deliverPendingClaims(serverPlayer);
            PacketHandler.sendColdWalletData(serverPlayer, "", false);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(com.ammora.mod.util.AmmoraLang.tooltip("cold_wallet.1"));
        tooltipComponents.add(com.ammora.mod.util.AmmoraLang.tooltip("cold_wallet.2"));
        tooltipComponents.add(com.ammora.mod.util.AmmoraLang.tooltip("cold_wallet.3"));
        tooltipComponents.add(com.ammora.mod.util.AmmoraLang.tooltip("cold_wallet.4"));
    }
}
