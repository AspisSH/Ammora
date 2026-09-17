package com.exchange.mod.blocks;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

/**
 * BlockEntity for Trade Dock.
 * Automatically ingests items and sells them on the market for the owner.
 */
public class TradeDockEntity extends BlockEntity {

    private UUID ownerUuid;
    private String ownerName = "Unknown";
    private double stopLossPrice = 0.50; // Default floor: do not sell below 0.50 CBX
    private int cooldown = 0;
    private int transferSpeedTicks = 8; // Default hopper speed (8 ticks)

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public TradeDockEntity(BlockPos pos, BlockState blockState) {
        super(ExchangeMod.TRADE_DOCK_BE.get(), pos, blockState);
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

    public double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
        setChanged();
    }

    public int getTransferSpeedTicks() {
        return transferSpeedTicks;
    }

    public void setTransferSpeedTicks(int ticks) {
        this.transferSpeedTicks = Math.max(1, ticks);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public String getActiveResourceId() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            }
        }
        return "minecraft:iron_ingot";
    }

    public void serverTick(Level level) {
        if (ownerUuid == null || ExchangeMod.getMarketManager() == null) {
            return;
        }

        cooldown++;
        if (cooldown < transferSpeedTicks) {
            return;
        }
        cooldown = 0;

        // Process first non-empty slot
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String resId = itemId.toString();

                MarketResource res = ExchangeMod.getMarketManager().getResource(resId);
                if (res != null) {
                    double currentSellPrice = MarketEngine.calculateSellPrice(res.getCurrentStock(), res);

                    // Stop-Loss safety check: do not sell if price is below limit
                    if (currentSellPrice < stopLossPrice) {
                        continue; // Hold items safely in inventory
                    }

                    int toSell = stack.getCount();
                    try {
                        MarketManager.MarketTransactionResult result = ExchangeMod.getMarketManager()
                                .executeSell(ownerUuid, ownerName, resId, toSell);

                        if (result.success()) {
                            inventory.extractItem(i, toSell, false);
                            break;
                        }
                    } catch (Exception e) {
                        // Fail gracefully without crashing server tick
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
        tag.putString("OwnerName", ownerName);
        tag.putDouble("StopLossPrice", stopLossPrice);
        tag.putInt("TransferSpeedTicks", transferSpeedTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("OwnerName")) {
            this.ownerName = tag.getString("OwnerName");
        }
        if (tag.contains("StopLossPrice")) {
            this.stopLossPrice = tag.getDouble("StopLossPrice");
        }
        if (tag.contains("TransferSpeedTicks")) {
            this.transferSpeedTicks = tag.getInt("TransferSpeedTicks");
        }
    }
}
