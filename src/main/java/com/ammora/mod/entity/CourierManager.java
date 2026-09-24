package com.ammora.mod.entity;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Central coordinator for spawning and dispatching customized couriers (Bee, Allay, Bumblebee, Phantom) to players.
 */
public class CourierManager {

    public static void dispatchToPlayer(ServerPlayer recipient, ItemStack stack) {
        if (stack.isEmpty() || recipient == null) return;
        dispatchToPlayer(recipient, List.of(stack));
    }

    public static void dispatchToPlayer(ServerPlayer recipient, List<ItemStack> items) {
        if (recipient == null || items == null || items.isEmpty()) return;
        List<ItemStack> nonNullItems = items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
        if (nonNullItems.isEmpty()) return;

        ServerLevel level = recipient.serverLevel();
        UUID playerUuid = recipient.getUUID();

        String courierId = "BEE";
        if (AmmoraMod.getMarketDAO() != null) {
            courierId = AmmoraMod.getMarketDAO().getActiveCourier(playerUuid);
        }
        CourierType type = CourierType.fromId(courierId);

        net.minecraft.world.phys.Vec3 playerPos = recipient.position();
        double rotRad = Math.toRadians(recipient.getYRot() + 180 + (level.random.nextDouble() - 0.5) * 60.0);
        double distance = 12.0 + level.random.nextDouble() * 3.0;
        double spawnX = playerPos.x - Math.sin(rotRad) * distance;
        double spawnZ = playerPos.z + Math.cos(rotRad) * distance;
        double spawnY = playerPos.y + 1.5 + level.random.nextDouble() * 2.0;

        BlockPos testPos = BlockPos.containing(spawnX, spawnY, spawnZ);
        if (!level.getBlockState(testPos).isAir()) {
            spawnX = playerPos.x + (level.random.nextDouble() - 0.5) * 4.0;
            spawnZ = playerPos.z + (level.random.nextDouble() - 0.5) * 4.0;
            spawnY = playerPos.y + 2.5;
        }

        ICourierEntity courier = null;
        SoundEvent dispatchSound = SoundEvents.BEE_LOOP;

        switch (type) {
            case ALLAY -> {
                CourierAllayEntity allay = AmmoraMod.COURIER_ALLAY.get().create(level);
                if (allay != null) {
                    allay.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    allay.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(allay);
                    courier = allay;
                    dispatchSound = SoundEvents.ALLAY_AMBIENT_WITH_ITEM;
                }
            }
            case HEAVY_BEE -> {
                CourierBeeEntity bee = AmmoraMod.COURIER_BEE.get().create(level);
                if (bee != null) {
                    bee.setHeavy(true);
                    bee.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    bee.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(bee);
                    courier = bee;
                    dispatchSound = SoundEvents.BEE_LOOP;
                }
            }
            case PHANTOM -> {
                CourierPhantomEntity phantom = AmmoraMod.COURIER_PHANTOM.get().create(level);
                if (phantom != null) {
                    phantom.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    phantom.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(phantom);
                    courier = phantom;
                    dispatchSound = SoundEvents.PHANTOM_FLAP;
                }
            }
            case PARROT -> {
                CourierParrotEntity parrot = AmmoraMod.COURIER_PARROT.get().create(level);
                if (parrot != null) {
                    parrot.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    parrot.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(parrot);
                    courier = parrot;
                    dispatchSound = SoundEvents.PARROT_FLY;
                }
            }
            case DRONE -> {
                CourierDroneEntity drone = AmmoraMod.COURIER_DRONE.get().create(level);
                if (drone != null) {
                    drone.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    drone.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(drone);
                    courier = drone;
                    dispatchSound = SoundEvents.BEACON_ACTIVATE;
                }
            }
            case BEE -> {
                CourierBeeEntity bee = AmmoraMod.COURIER_BEE.get().create(level);
                if (bee != null) {
                    bee.setHeavy(false);
                    bee.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
                    bee.setDeliveryOrder(recipient, nonNullItems);
                    level.addFreshEntity(bee);
                    courier = bee;
                    dispatchSound = SoundEvents.BEE_LOOP;
                }
            }
        }

        if (courier != null) {
            float pitch = (type == CourierType.HEAVY_BEE) ? 0.6F : (type == CourierType.DRONE ? 1.6F : 1.1F);
            level.playSound(null, recipient.blockPosition(), dispatchSound, SoundSource.PLAYERS, 0.8F, pitch);
            recipient.displayClientMessage(AmmoraLang.message("courier.dispatched"), true);
        } else {
            // Spawning failed, fallback to player inventory or database
            for (ItemStack st : nonNullItems) {
                if (!recipient.getInventory().add(st.copy())) {
                    CourierBeeEntity.saveFallbackToBuffer(st, recipient.getUUID(), recipient.registryAccess());
                }
            }
        }
    }
}
