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
    private static final int MAX_DELIVERY_TICKS = 400; // 20 seconds fallback timeout

    public CourierBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.FLYING_SPEED, 0.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
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
            // Ascend smoothly, spawn farewell poof particles and disappear
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.06, 0));
            if (this.deliveryTicks > 30) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 12, 0.2, 0.2, 0.2, 0.05);
                }
                this.discard();
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

        // Safety fallback timeout: if courier is stuck in walls/doors for > 20s, complete delivery immediately
        if (this.deliveryTicks > MAX_DELIVERY_TICKS) {
            completeDelivery(target);
        }
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

        this.isDelivered = true;
        this.deliveryTicks = 0;
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
