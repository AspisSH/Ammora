package com.ammora.mod.entity;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.ai.CourierFlyToPlayerGoal;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Friendly flying courier bee delivering remote marketplace orders to players.
 * Protected from damage and guarantees item delivery without loss even on disconnect.
 */
public class CourierBeeEntity extends Bee {

    private static final EntityDataAccessor<ItemStack> DELIVERED_ITEM =
            SynchedEntityData.defineId(CourierBeeEntity.class, EntityDataSerializers.ITEM_STACK);

    private UUID targetPlayerUuid;
    private final List<ItemStack> deliveryItems = new ArrayList<>();
    private boolean isDelivered = false;
    private int deliveryTicks = 0;
    private static final int MAX_DELIVERY_TICKS = 800; // 40 seconds fallback timeout
    private Vec3 departureDir = Vec3.ZERO;

    public CourierBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.FLYING_SPEED, 0.2D)     // Slowed down 4x from 0.8D for gentle delivery approach
                .add(Attributes.MOVEMENT_SPEED, 0.0875D) // Slowed down 4x from 0.35D
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DELIVERED_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Clear all vanilla bee goals (pollinate, enter/locate hives, wander, sting, etc.)
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        // Only float on water and navigate to the target player
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CourierFlyToPlayerGoal(this));
    }

    @Override
    public boolean hasHive() {
        return false;
    }

    @Override
    public boolean hasSavedFlowerPos() {
        return false;
    }

    public void setDeliveryOrder(Player player, List<ItemStack> items) {
        this.targetPlayerUuid = player.getUUID();
        this.deliveryItems.clear();
        for (ItemStack st : items) {
            if (!st.isEmpty()) {
                this.deliveryItems.add(st.copy());
            }
        }
        if (!this.deliveryItems.isEmpty()) {
            this.entityData.set(DELIVERED_ITEM, this.deliveryItems.get(0).copy());
        }
        this.setCustomName(AmmoraLang.translatable("entity.ammora.courier_bee"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public void setDeliveryOrder(Player player, ItemStack singleItem) {
        NonNullList<ItemStack> list = NonNullList.create();
        list.add(singleItem);
        setDeliveryOrder(player, list);
    }

    public ItemStack getDeliveredItem() {
        return this.entityData.get(DELIVERED_ITEM);
    }

    public UUID getTargetPlayerUuid() {
        return this.targetPlayerUuid;
    }

    public boolean isDelivered() {
        return this.isDelivered;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Complete invulnerability to prevent parcel theft or accidental death
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        this.deliveryTicks++;

        if (this.isDelivered) {
            this.setNoGravity(true);
            if (!(this.level() instanceof ServerLevel serverLevel)) return;

            // Phase 1: Back away gently from player (ticks 0 - 25, ~1.25s)
            if (this.deliveryTicks <= 25) {
                if (this.departureDir == null || this.departureDir.lengthSqr() < 0.001) {
                    this.departureDir = new Vec3(0, 0, 1);
                }
                // Smoothly drift away from the player horizontally and slightly upward
                Vec3 backMovement = this.departureDir.scale(0.12D).add(0, 0.03D, 0);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D).add(backMovement));

                // Turn face in departure direction
                float yaw = (float) (Math.atan2(-this.departureDir.x, this.departureDir.z) * (180.0D / Math.PI));
                this.setYRot(yaw);
                this.setYHeadRot(yaw);
                this.setXRot(0.0F);

                // Small gentle particles while backing away
                if (this.deliveryTicks % 6 == 0) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.2, getZ(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // Phase 2: Rocket launch into stratosphere with firework flight effects (ticks 26+)
            else {
                int launchTicks = this.deliveryTicks - 25;

                // Pass cleanly through ceilings and blocks straight into the stratosphere
                this.noPhysics = true;

                if (launchTicks == 1) {
                    // Firework rocket launch sound!
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.4F, 1.0F);
                }

                // Face straight up into the stratosphere
                this.setXRot(-90.0F);

                // Rapid firework rocket acceleration upwards
                Vec3 current = this.getDeltaMovement();
                double upwardSpeed = Math.min(0.25D + (launchTicks * 0.07D), 2.5D);
                // Zero out horizontal drift so it launches vertically
                this.setDeltaMovement(current.x * 0.65D, upwardSpeed, current.z * 0.65D);

                // Firework particle exhaust under the bee (sparks, flames, smoke)
                serverLevel.sendParticles(ParticleTypes.FIREWORK, getX(), getY() - 0.25, getZ(), 4, 0.06, 0.06, 0.06, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.25, getZ(), 2, 0.04, 0.04, 0.04, 0.02);
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() - 0.3, getZ(), 2, 0.05, 0.05, 0.05, 0.01);

                // Twinkle sound occasionally during rocket ascent
                if (launchTicks % 10 == 0) {
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 0.9F, 1.2F);
                }

                // Stratosphere exit: reached max build height or 65 ticks (~3.2s) of high-speed ascent
                if (launchTicks > 65 || this.getY() >= serverLevel.getMaxBuildHeight() - 5) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.12);
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.8F, 1.3F);
                    this.discard();
                }
            }
            return;
        }

        // Validate player status
        ServerPlayer target = this.targetPlayerUuid != null
                ? (ServerPlayer) this.level().getPlayerByUUID(this.targetPlayerUuid)
                : null;

        // If player left the game or switched dimensions, store items safely into SQLite database
        if (target == null || target.level() != this.level()) {
            backupToDatabase();
            this.discard();
            return;
        }

        // Safety fallback timeout: if courier is stuck in walls/doors for > 40s, complete delivery immediately
        if (this.deliveryTicks > MAX_DELIVERY_TICKS) {
            completeDelivery(target);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return this.isDelivered ? null : super.getAmbientSound();
    }

    /**
     * Transfers the delivered goods into the player's inventory,
     * or buffers them in SQLite if player inventory is full.
     */
    public void completeDelivery(Player player) {
        if (this.isDelivered) return;

        if (!this.deliveryItems.isEmpty()) {
            ItemStack primary = this.deliveryItems.get(0);
            String itemName = primary.getHoverName().getString();
            int totalCount = 0;

            for (ItemStack stack : this.deliveryItems) {
                if (stack.isEmpty()) continue;
                totalCount += stack.getCount();
                if (!player.getInventory().add(stack)) {
                    // Buffer overflowing items safely in DB
                    saveItemToBuffer(stack, player.getUUID());
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        AmmoraLang.message("courier.delivered", itemName, String.valueOf(totalCount)),
                        true
                );
            }

            this.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.8F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.3, getZ(), 16, 0.3, 0.3, 0.3, 0.1);
                serverLevel.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.5, getZ(), 4, 0.2, 0.2, 0.2, 0.0);
            }
        }

        this.deliveryItems.clear();
        this.entityData.set(DELIVERED_ITEM, ItemStack.EMPTY);
        this.isDelivered = true;
        this.deliveryTicks = 0;

        // Compute departure direction (away from player horizontally)
        Vec3 away = this.position().subtract(player.position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 0.01) {
            away = player.getLookAngle().scale(-1.0);
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() < 0.01) {
                away = new Vec3(0, 0, 1);
            }
        }
        this.departureDir = away.normalize();
    }

    public void backupToDatabase() {
        if (this.targetPlayerUuid == null) return;
        for (ItemStack stack : this.deliveryItems) {
            if (!stack.isEmpty()) {
                saveItemToBuffer(stack, this.targetPlayerUuid);
            }
        }
        this.deliveryItems.clear();
        this.entityData.set(DELIVERED_ITEM, ItemStack.EMPTY);
    }

    private void saveItemToBuffer(ItemStack stack, UUID recipientUuid) {
        if (stack.isEmpty() || recipientUuid == null) return;
        String nbt = "";
        try {
            Tag t = stack.saveOptional(this.registryAccess());
            if (t != null) nbt = t.getAsString();
        } catch (Exception ignored) {}

        if (AmmoraMod.getMarketDAO() != null) {
            try {
                AmmoraMod.getMarketDAO().saveUnclaimedDelivery(
                        UUID.randomUUID().toString(),
                        recipientUuid,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        stack.getCount(),
                        System.currentTimeMillis(),
                        nbt
                );
            } catch (Exception e) {
                AmmoraMod.LOGGER.error("Failed to backup undelivered courier item", e);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetPlayerUuid != null) {
            tag.putUUID("TargetPlayer", this.targetPlayerUuid);
        }
        tag.putBoolean("IsDelivered", this.isDelivered);
        tag.putInt("DeliveryTicks", this.deliveryTicks);
        if (this.departureDir != null) {
            tag.putDouble("DepartureX", this.departureDir.x);
            tag.putDouble("DepartureZ", this.departureDir.z);
        }

        ListTag itemList = new ListTag();
        for (ItemStack st : this.deliveryItems) {
            if (!st.isEmpty()) {
                Tag itemTag = st.saveOptional(this.registryAccess());
                if (itemTag instanceof CompoundTag compound) {
                    itemList.add(compound);
                }
            }
        }
        tag.put("DeliveryItems", itemList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.targetPlayerUuid = tag.getUUID("TargetPlayer");
        }
        this.isDelivered = tag.getBoolean("IsDelivered");
        this.deliveryTicks = tag.getInt("DeliveryTicks");
        if (tag.contains("DepartureX") && tag.contains("DepartureZ")) {
            this.departureDir = new Vec3(tag.getDouble("DepartureX"), 0, tag.getDouble("DepartureZ"));
        }

        this.deliveryItems.clear();
        if (tag.contains("DeliveryItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("DeliveryItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag ct = list.getCompound(i);
                ItemStack st = ItemStack.parseOptional(this.registryAccess(), ct);
                if (!st.isEmpty()) {
                    this.deliveryItems.add(st);
                }
            }
        }
        if (!this.deliveryItems.isEmpty()) {
            this.entityData.set(DELIVERED_ITEM, this.deliveryItems.get(0).copy());
        }
    }

    public static void dispatchToPlayer(ServerPlayer recipient, ItemStack stack) {
        if (stack.isEmpty() || recipient == null) return;
        ServerLevel level = recipient.serverLevel();
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

        CourierBeeEntity bee = AmmoraMod.COURIER_BEE.get().create(level);
        if (bee != null) {
            bee.moveTo(spawnX, spawnY, spawnZ, recipient.getYRot(), 0.0F);
            bee.setDeliveryOrder(recipient, stack);
            level.addFreshEntity(bee);

            level.playSound(null, recipient.blockPosition(), SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.8F, 1.2F);
            recipient.displayClientMessage(AmmoraLang.message("courier.dispatched"), true);
        } else {
            if (!recipient.getInventory().add(stack.copy())) {
                saveFallbackToBuffer(stack, recipient.getUUID(), recipient.registryAccess());
            }
        }
    }

    public static void saveFallbackToBuffer(ItemStack stack, UUID recipientUuid, net.minecraft.core.HolderLookup.Provider registryAccess) {
        if (stack.isEmpty() || recipientUuid == null) return;
        String nbt = "";
        try {
            Tag t = stack.saveOptional(registryAccess);
            if (t != null) nbt = t.getAsString();
        } catch (Exception ignored) {}

        if (AmmoraMod.getMarketDAO() != null) {
            try {
                AmmoraMod.getMarketDAO().saveUnclaimedDelivery(
                        UUID.randomUUID().toString(),
                        recipientUuid,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        stack.getCount(),
                        System.currentTimeMillis(),
                        nbt
                );
            } catch (Exception e) {
                AmmoraMod.LOGGER.error("Failed to save delivery fallback", e);
            }
        }
    }
}
