package com.ammora.mod.entity;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.ai.CourierFlyToPlayerGoal;
import com.ammora.mod.util.AmmoraLang;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Friendly flying courier Parrot wearing a postal uniform cap delivering marketplace parcels.
 * Protected from damage and guarantees item delivery without loss.
 */
public class CourierParrotEntity extends Parrot implements ICourierEntity {

    private static final EntityDataAccessor<ItemStack> DELIVERED_ITEM =
            SynchedEntityData.defineId(CourierParrotEntity.class, EntityDataSerializers.ITEM_STACK);

    private UUID targetPlayerUuid;
    private final List<ItemStack> deliveryItems = new ArrayList<>();
    private boolean isDelivered = false;
    private int deliveryTicks = 0;
    private static final int MAX_DELIVERY_TICKS = 800; // 40 seconds fallback timeout
    private Vec3 departureDir = Vec3.ZERO;

    public CourierParrotEntity(EntityType<? extends Parrot> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        if (!level.isClientSide) {
            // Randomize parrot plumage variant
            this.setVariant(Parrot.Variant.byId(this.random.nextInt(5)));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Parrot.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DELIVERED_ITEM, ItemStack.EMPTY);
    }

    @Override
    public Mob asMob() {
        return this;
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
        // Clear all vanilla parrot goals (sitting, wandering, perching on shoulders)
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CourierFlyToPlayerGoal(this));
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
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
        this.setCustomName(AmmoraLang.translatable("entity.ammora.courier_parrot"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    @Override
    public void setDeliveryOrder(Player player, ItemStack singleItem) {
        NonNullList<ItemStack> list = NonNullList.create();
        list.add(singleItem);
        setDeliveryOrder(player, list);
    }

    @Override
    public ItemStack getDeliveredItem() {
        return this.entityData.get(DELIVERED_ITEM);
    }

    @Override
    public UUID getTargetPlayerUuid() {
        return this.targetPlayerUuid;
    }

    @Override
    public boolean isDelivered() {
        return this.isDelivered;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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

            // Phase 1: Swoop away gently from player (ticks 0 - 25)
            if (this.deliveryTicks <= 25) {
                if (this.departureDir == null || this.departureDir.lengthSqr() < 0.001) {
                    this.departureDir = new Vec3(0, 0, 1);
                }
                Vec3 backMovement = this.departureDir.scale(0.12D).add(0, 0.03D, 0);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D).add(backMovement));

                float yaw = (float) (Math.atan2(-this.departureDir.x, this.departureDir.z) * (180.0D / Math.PI));
                this.setYRot(yaw);
                this.setYHeadRot(yaw);
                this.setXRot(0.0F);

                if (this.deliveryTicks % 6 == 0) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.2, getZ(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // Phase 2: Rocket launch into stratosphere with firework flight effects (ticks 26+)
            else {
                int launchTicks = this.deliveryTicks - 25;
                this.noPhysics = true;

                if (launchTicks == 1) {
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.2F, 1.2F);
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.1F);
                }

                this.setXRot(-90.0F);

                Vec3 current = this.getDeltaMovement();
                double upwardSpeed = Math.min(0.25D + (launchTicks * 0.07D), 2.5D);
                this.setDeltaMovement(current.x * 0.65D, upwardSpeed, current.z * 0.65D);

                serverLevel.sendParticles(ParticleTypes.FIREWORK, getX(), getY() - 0.25, getZ(), 3, 0.06, 0.06, 0.06, 0.04);
                serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() - 0.25, getZ(), 2, 0.04, 0.04, 0.04, 0.02);

                if (launchTicks > 65 || this.getY() >= serverLevel.getMaxBuildHeight() - 5) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.12);
                    serverLevel.playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.8F, 1.3F);
                    this.discard();
                }
            }
            return;
        }

        ServerPlayer target = this.targetPlayerUuid != null
                ? (ServerPlayer) this.level().getPlayerByUUID(this.targetPlayerUuid)
                : null;

        if (target == null || target.level() != this.level()) {
            backupToDatabase();
            this.discard();
            return;
        }

        if (this.deliveryTicks > MAX_DELIVERY_TICKS) {
            completeDelivery(target);
        }
    }

    @Override
    public SoundEvent getAmbientSound() {
        return this.isDelivered ? null : SoundEvents.PARROT_AMBIENT;
    }

    @Override
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
                    saveItemToBuffer(stack, player.getUUID());
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        AmmoraLang.message("courier.delivered", itemName, String.valueOf(totalCount)),
                        true
                );
            }

            this.level().playSound(null, player.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.2F);
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

    @Override
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
}
