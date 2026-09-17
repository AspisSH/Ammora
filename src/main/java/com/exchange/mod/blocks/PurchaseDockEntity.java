package com.exchange.mod.blocks;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.db.PlayerAccount;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

/**
 * BlockEntity for Purchase Dock.
 * Automatically buys resources from the exchange for CBX when redstone-powered
 * and places them into its buffer inventory for extraction.
 */
public class PurchaseDockEntity extends BlockEntity {

    private UUID ownerUuid;
    private String ownerName = "Unknown";
    private String targetResourceId = "minecraft:iron_ingot";
    private double maxBuyPrice = 30.0; // Stop-High price guard
    private int batchSize = 1;
    private int cooldown = 0;
    private int transferSpeedTicks = 20; // 1 second interval when powered continuously
    private boolean lastPowered = false;

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public PurchaseDockEntity(BlockPos pos, BlockState blockState) {
        super(ExchangeMod.PURCHASE_DOCK_BE.get(), pos, blockState);
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
        setChanged();
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTargetResourceId() {
        return targetResourceId;
    }

    public void setTargetResourceId(String id) {
        this.targetResourceId = id;
        setChanged();
    }

    public double getMaxBuyPrice() {
        return maxBuyPrice;
    }

    public void setMaxBuyPrice(double maxPrice) {
        this.maxBuyPrice = maxPrice;
        setChanged();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, Math.min(64, batchSize));
        setChanged();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void onNeighborChanged(Level level, BlockPos pos) {
        boolean isPowered = level.hasNeighborSignal(pos);
        if (isPowered && !lastPowered) {
            // Rising edge trigger
            tryExecutePurchase(level);
        }
        lastPowered = isPowered;
    }

    public void serverTick(Level level) {
        if (ownerUuid == null || ExchangeMod.getMarketManager() == null) {
            return;
        }

        boolean isPowered = level.hasNeighborSignal(worldPosition);
        if (isPowered) {
            cooldown++;
            if (cooldown >= transferSpeedTicks) {
                cooldown = 0;
                tryExecutePurchase(level);
            }
        } else {
            cooldown = 0;
        }
        lastPowered = isPowered;
    }

    /**
     * Executes a purchase if conditions (stock, balance, maxPrice, inventory space) are met.
     * @return true if purchase was successful
     */
    public boolean tryExecutePurchase(Level level) {
        if (ownerUuid == null) return false;
        MarketManager marketManager = ExchangeMod.getMarketManager();
        if (marketManager == null) return false;

        Item item;
        try {
            item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(targetResourceId));
        } catch (Exception e) {
            return false;
        }
        if (item == null) return false;

        ItemStack stackToInsert = new ItemStack(item, batchSize);

        // Check if inventory has room before executing purchase
        if (!canInsert(stackToInsert)) {
            return false;
        }

        try {
            MarketManager.MarketTransactionResult result = marketManager.executeAutomatedPurchase(
                    ownerUuid, ownerName, targetResourceId, batchSize, maxBuyPrice
            );

            if (!result.success()) {
                return false;
            }

            // Insert into buffer inventory
            insertStack(stackToInsert);

            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            setChanged();
            return true;
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("PurchaseDock transaction failed", e);
            return false;
        }
    }

    private boolean canInsert(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, true);
            if (remaining.isEmpty()) return true;
        }
        return false;
    }

    private void insertStack(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) break;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putString("TargetResource", targetResourceId);
        tag.putDouble("MaxBuyPrice", maxBuyPrice);
        tag.putInt("BatchSize", batchSize);
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
        tag.putString("OwnerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("TargetResource")) {
            this.targetResourceId = tag.getString("TargetResource");
        }
        if (tag.contains("MaxBuyPrice")) {
            this.maxBuyPrice = tag.getDouble("MaxBuyPrice");
        }
        if (tag.contains("BatchSize")) {
            this.batchSize = tag.getInt("BatchSize");
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("OwnerName")) {
            this.ownerName = tag.getString("OwnerName");
        }
    }
}
