package com.exchange.mod.items;

import com.exchange.mod.network.PacketHandler;
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
 * Tablet item for browsing the global marketplace, ordering remote deliveries,
 * checking player shop locations, and participating in buy bounties (RFQ).
 */
public class MarketTabletItem extends Item {

    public MarketTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            com.exchange.mod.ExchangeMod.deliverPendingClaims(serverPlayer);
            PacketHandler.sendMarketplaceData(serverPlayer, "", false);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("market_tablet.1"));
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("market_tablet.2"));
        tooltipComponents.add(com.exchange.mod.util.ExchangeLang.tooltip("market_tablet.3"));
    }
}
