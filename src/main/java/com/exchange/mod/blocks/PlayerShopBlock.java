package com.exchange.mod.blocks;

import com.exchange.mod.ExchangeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Player Vending Machine Block (Rust-style).
 */
public class PlayerShopBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public PlayerShopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                shop.setOwner(player.getUUID(), player.getName().getString());
                shop.setShopName(Component.translatable("gui.exchange.shop.default_name", player.getName().getString()).getString());
                player.sendSystemMessage(Component.translatable("message.exchange.shop.placed"));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                shop.openShopScreen(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                if (!shop.isOwner(player)) {
                    return 0.0F;
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                if (!shop.isOwner(player)) {
                    player.sendSystemMessage(Component.translatable("message.exchange.shop_owned_by", shop.getOwnerName()));
                    return false;
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                if (!shop.isOwner(player)) {
                    player.displayClientMessage(Component.translatable("message.exchange.shop_owned_by", shop.getOwnerName()), true);
                }
            }
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                if (!shop.isOwner(player)) {
                    player.sendSystemMessage(Component.translatable("message.exchange.shop_owned_by", shop.getOwnerName()));
                    return state;
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (isDynamiteExplosion(explosion)) {
            return 4.0F;
        }
        return 3600000.0F;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!isDynamiteExplosion(explosion)) {
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    public static boolean isDynamiteExplosion(@Nullable Explosion explosion) {
        if (explosion == null) return true;
        Entity direct = explosion.getDirectSourceEntity();
        if (direct instanceof PrimedTnt || direct instanceof MinecartTNT) {
            return true;
        }
        if (direct != null) {
            String typeStr = direct.getType().getDescriptionId().toLowerCase(Locale.ROOT);
            if (typeStr.contains("tnt") || typeStr.contains("dynamite")) {
                return true;
            }
        }
        Entity indirect = explosion.getIndirectSourceEntity();
        if (indirect != null) {
            String indirectType = indirect.getType().getDescriptionId().toLowerCase(Locale.ROOT);
            if (indirectType.contains("tnt") || indirectType.contains("dynamite")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerShopEntity shop) {
                // Drop stored inventory items
                for (int i = 0; i < shop.getInventory().getSlots(); i++) {
                    ItemStack stack = shop.getInventory().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                // Remove from database
                try {
                    if (ExchangeMod.getMarketDAO() != null) {
                        ExchangeMod.getMarketDAO().deletePlayerShop(shop.getShopId());
                    }
                } catch (Exception e) {
                    ExchangeMod.LOGGER.error("Failed to delete shop from DB on remove", e);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerShopEntity(pos, state);
    }
}
