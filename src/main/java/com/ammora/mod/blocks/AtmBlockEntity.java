package com.ammora.mod.blocks;

import com.ammora.mod.AmmoraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * BlockEntity for the Automated Teller Machine (ATM).
 * Attached to the lower half of the 2-block tall ATM structure.
 */
public class AtmBlockEntity extends BlockEntity {

    private UUID ownerUuid;
    private String customName = "";

    public AtmBlockEntity(BlockPos pos, BlockState blockState) {
        super(AmmoraMod.ATM_BE.get(), pos, blockState);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        setChanged();
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName != null ? customName : "";
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }
        if (customName != null && !customName.isEmpty()) {
            tag.putString("CustomName", customName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("CustomName")) {
            this.customName = tag.getString("CustomName");
        }
    }
}
