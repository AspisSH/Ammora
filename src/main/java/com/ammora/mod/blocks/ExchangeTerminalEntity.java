package com.ammora.mod.blocks;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.MarketResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * BlockEntity for Exchange Terminal.
 * Stores configuration, owner info, and manages comparator output.
 */
public class ExchangeTerminalEntity extends BlockEntity {

    // Redstone Modes: 0 = WAREHOUSE_FILL, 1 = PRICE_RATIO, 2 = PRICE_THRESHOLD
    public static final int MODE_WAREHOUSE_FILL = 0;
    public static final int MODE_PRICE_RATIO = 1;
    public static final int MODE_PRICE_THRESHOLD = 2;

    private String monitoredResource = "minecraft:iron_ingot";
    private UUID ownerUuid;
    private int redstoneMode = MODE_WAREHOUSE_FILL;
    private double thresholdPrice = 7.0;
    private boolean thresholdIsLessThan = true;

    public ExchangeTerminalEntity(BlockPos pos, BlockState blockState) {
        super(AmmoraMod.EXCHANGE_TERMINAL_BE.get(), pos, blockState);
    }

    private int lastSignal = -1;
    private int tickCounter = 0;

    public String getMonitoredResource() {
        return monitoredResource;
    }

    public void setMonitoredResource(String resourceId) {
        this.monitoredResource = resourceId;
        updateSignalNeighbors();
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int mode) {
        this.redstoneMode = mode;
        updateSignalNeighbors();
    }

    public double getThresholdPrice() {
        return thresholdPrice;
    }

    public void setThresholdPrice(double price) {
        this.thresholdPrice = price;
        updateSignalNeighbors();
    }

    public boolean isThresholdIsLessThan() {
        return thresholdIsLessThan;
    }

    public void setThresholdIsLessThan(boolean isLessThan) {
        this.thresholdIsLessThan = isLessThan;
        updateSignalNeighbors();
    }

    public void updateSignalNeighbors() {
        this.lastSignal = -1;
        setChanged();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public void serverTick(Level level) {
        tickCounter++;
        if (tickCounter % 10 == 0) {
            int cur = getRedstoneSignal();
            if (cur != lastSignal) {
                lastSignal = cur;
                setChanged();
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwner(UUID uuid) {
        this.ownerUuid = uuid;
        setChanged();
    }

    /**
     * Calculates Redstone signal strength (0-15) based on configured mode:
     * - MODE_WAREHOUSE_FILL (0): Stock / MaxReserve * 15
     * - MODE_PRICE_RATIO (1): SpotPrice / (BasePrice * 2) * 15
     * - MODE_PRICE_THRESHOLD (2): 15 if (isLessThan ? sellPrice < threshold : sellPrice > threshold), else 0
     */
    public int getRedstoneSignal() {
        if (AmmoraMod.getMarketManager() == null) return 0;
        MarketResource res = AmmoraMod.getMarketManager().getResource(monitoredResource);
        if (res == null) return 0;

        switch (redstoneMode) {
            case MODE_PRICE_RATIO -> {
                double spot = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                double ratio = spot / Math.max(0.1, res.getBasePrice() * 2.0);
                return (int) Math.min(15, Math.max(0, Math.round(ratio * 15)));
            }
            case MODE_PRICE_THRESHOLD -> {
                double sellPrice = com.ammora.mod.core.MarketEngine.calculateSellPrice(res.getCurrentStock(), res);
                boolean triggered = thresholdIsLessThan ? (sellPrice < thresholdPrice) : (sellPrice > thresholdPrice);
                return triggered ? 15 : 0;
            }
            case MODE_WAREHOUSE_FILL -> {
                double ratio = res.getCurrentStock() / res.getMaxReserve();
                return (int) Math.min(15, Math.max(0, Math.round(ratio * 15)));
            }
            default -> {
                return 0;
            }
        }
    }

    public void openTerminalMenu(ServerPlayer player) {
        this.ownerUuid = player.getUUID();
        setChanged();
        AmmoraMod.openTerminalScreen(player, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("MonitoredResource", monitoredResource);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putDouble("ThresholdPrice", thresholdPrice);
        tag.putBoolean("ThresholdIsLessThan", thresholdIsLessThan);
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MonitoredResource")) {
            this.monitoredResource = tag.getString("MonitoredResource");
        }
        if (tag.contains("RedstoneMode")) {
            this.redstoneMode = tag.getInt("RedstoneMode");
        }
        if (tag.contains("ThresholdPrice")) {
            this.thresholdPrice = tag.getDouble("ThresholdPrice");
        }
        if (tag.contains("ThresholdIsLessThan")) {
            this.thresholdIsLessThan = tag.getBoolean("ThresholdIsLessThan");
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
        }
    }
}
