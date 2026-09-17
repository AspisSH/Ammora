package com.exchange.mod.blocks;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.db.PlayerAccount;
import com.exchange.mod.db.PlayerShopRecord;
import com.exchange.mod.db.ShopSlotRecord;
import com.exchange.mod.network.PacketHandler;
import com.exchange.mod.network.PlayerShopDataPayload;
import com.exchange.mod.util.ExchangeLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * BlockEntity for the Rust-style Player Vending Machine.
 * Holds up to 10 product slots with custom prices, security, and SQLite indexing.
 * Supports paid upgrades: slot capacity (up to 1024), extra slot unlocking (5 to 10), and satellite network access.
 */
public class PlayerShopEntity extends BlockEntity {

    private String shopId = UUID.randomUUID().toString();
    private UUID ownerUuid;
    private String ownerName = "Unknown";
    private String shopName = "Vending Machine";
    private boolean broadcast = false;
    private double accumulatedRevenue = 0.0;
    private int totalSales = 0;
    private final double[] prices = new double[10];

    // Upgrades
    private int maxSlots = 5;
    private int slotCapacity = 64;
    private boolean networkUnlocked = false;

    private final ItemStackHandler inventory = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncSlotToDatabase(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= maxSlots) {
                return false;
            }
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) {
                return true;
            }
            return ItemStack.isSameItemSameComponents(existing, stack);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return slotCapacity;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slotCapacity;
        }
    };

    public PlayerShopEntity(BlockPos pos, BlockState blockState) {
        super(ExchangeMod.PLAYER_SHOP_BE.get(), pos, blockState);
        for (int i = 0; i < 10; i++) {
            prices[i] = 10.0;
        }
    }

    public String getShopId() { return shopId; }
    public UUID getOwnerUuid() {
        if (ownerUuid == null && level != null && !level.isClientSide && ExchangeMod.getMarketDAO() != null) {
            try {
                PlayerShopRecord rec = ExchangeMod.getMarketDAO().getPlayerShop(shopId);
                if (rec != null && rec.getOwnerUuid() != null) {
                    this.ownerUuid = rec.getOwnerUuid();
                    this.ownerName = rec.getOwnerName();
                }
            } catch (Exception ignored) {}
        }
        return ownerUuid;
    }

    public boolean isOwner(@Nullable Player player) {
        if (player == null) return false;
        UUID owner = getOwnerUuid();
        if (owner == null) return false;
        return owner.equals(player.getUUID());
    }

    public String getOwnerName() { return ownerName; }
    public String getShopName() { return shopName; }
    public boolean isBroadcast() { return broadcast; }
    public double getAccumulatedRevenue() { return accumulatedRevenue; }
    public int getTotalSales() { return totalSales; }
    public ItemStackHandler getInventory() { return inventory; }
    public double getPrice(int slot) {
        if (slot >= 0 && slot < 10) return prices[slot];
        return 0.0;
    }

    public int getMaxSlots() { return maxSlots; }
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(5, Math.min(10, maxSlots));
        setChanged();
        syncToDatabase();
    }

    public int getSlotCapacity() { return slotCapacity; }
    public void setSlotCapacity(int slotCapacity) {
        this.slotCapacity = Math.max(64, slotCapacity);
        setChanged();
        syncToDatabase();
    }

    public boolean isNetworkUnlocked() { return networkUnlocked; }
    public void setNetworkUnlocked(boolean networkUnlocked) {
        this.networkUnlocked = networkUnlocked;
        if (!networkUnlocked) this.broadcast = false;
        setChanged();
        syncToDatabase();
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        syncToDatabase();
    }

    public void setShopName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.shopName = name.trim();
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            syncToDatabase();
        }
    }

    public void setBroadcast(boolean broadcast) {
        if (broadcast && !networkUnlocked) {
            this.broadcast = false;
        } else {
            this.broadcast = broadcast;
        }
        setChanged();
        syncToDatabase();
    }

    public void setSlotPrice(int slot, double price) {
        if (slot >= 0 && slot < 10 && price >= 0.01) {
            this.prices[slot] = Math.round(price * 100.0) / 100.0;
            setChanged();
            syncSlotToDatabase(slot);
        }
    }

    public void claimRevenue(ServerPlayer player) {
        if (ownerUuid != null && ownerUuid.equals(player.getUUID()) && accumulatedRevenue > 0.001) {
            try {
                PlayerAccount acc = ExchangeMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                if (acc != null) {
                    acc.deposit(accumulatedRevenue);
                    ExchangeMod.getMarketDAO().saveAccount(acc);
                    player.sendSystemMessage(Component.translatable("message.exchange.shop.revenue_credited", String.format(Locale.US, "%.2f", accumulatedRevenue)));
                    this.accumulatedRevenue = 0.0;
                    setChanged();
                    syncToDatabase();
                }
            } catch (Exception e) {
                ExchangeMod.LOGGER.error("Failed to claim shop revenue", e);
            }
        }
    }

    public void addSale(int count, double totalEarnings) {
        this.totalSales += count;
        this.accumulatedRevenue += totalEarnings;
        setChanged();
        syncToDatabase();
    }

    public void openShopScreen(ServerPlayer player) {
        boolean isOwner = ownerUuid != null && ownerUuid.equals(player.getUUID());
        double balance = 0.0;
        try {
            if (ExchangeMod.getMarketDAO() != null) {
                PlayerAccount acc = ExchangeMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                if (acc != null) balance = acc.getBalanceCbx();
            }
        } catch (Exception ignored) {}

        List<PlayerShopDataPayload.ShopSlotItem> slotItems = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                slotItems.add(new PlayerShopDataPayload.ShopSlotItem(
                        i, "", ExchangeLang.guiStr("shop.empty_slot"), prices[i], 0, List.of()
                ));
            } else {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String name = stack.getHoverName().getString();
                List<String> lore = new ArrayList<>();

                if (stack.isDamageableItem()) {
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = stack.getDamageValue();
                    int remain = maxDmg - curDmg;
                    double pct = Math.round((remain * 100.0 / maxDmg) * 10.0) / 10.0;
                    String color = pct < 25.0 ? "§c" : (pct < 60.0 ? "§e" : "§a");
                    lore.add(ExchangeLang.guiStr("shop.durability", color + remain, "§a" + maxDmg, color + pct + "%"));
                }

                try {
                    List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.Default.NORMAL);
                    for (Component c : lines) {
                        String str = c.getString();
                        if (!str.equalsIgnoreCase(name) && !str.isBlank()) {
                            lore.add(str);
                        }
                    }
                } catch (Exception ignored) {}
                slotItems.add(new PlayerShopDataPayload.ShopSlotItem(
                        i, itemId, name, prices[i], stack.getCount(), lore
                ));
            }
        }

        PacketDistributor.sendToPlayer(player, new PlayerShopDataPayload(
                shopId, shopName, ownerUuid != null ? ownerUuid : player.getUUID(), ownerName, isOwner, broadcast,
                accumulatedRevenue, totalSales, balance, slotItems, maxSlots, slotCapacity, networkUnlocked, "", false
        ));
    }

    public void syncToDatabase() {
        if (level == null || level.isClientSide || ExchangeMod.getMarketDAO() == null) return;
        try {
            PlayerShopRecord shopRecord = new PlayerShopRecord(
                    shopId, ownerUuid != null ? ownerUuid : UUID.randomUUID(), ownerName, shopName,
                    level.dimension().location().toString(),
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    broadcast, totalSales, accumulatedRevenue, System.currentTimeMillis(),
                    maxSlots, slotCapacity, networkUnlocked
            );
            ExchangeMod.getMarketDAO().saveOrUpdatePlayerShop(shopRecord);

            for (int i = 0; i < 10; i++) {
                syncSlotToDatabase(i);
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to sync player shop to DB", e);
        }
    }

    public void syncSlotToDatabase(int slot) {
        if (level == null || level.isClientSide || ExchangeMod.getMarketDAO() == null) return;
        try {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                ShopSlotRecord rec = new ShopSlotRecord(shopId, slot, "", "", ExchangeLang.guiStr("shop.empty_slot"), prices[slot], 0);
                ExchangeMod.getMarketDAO().saveOrUpdateShopSlot(rec);
            } else {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String displayName = stack.getHoverName().getString();
                String nbtStr = "";
                try {
                    net.minecraft.nbt.Tag itemTag = stack.save(level.registryAccess());
                    nbtStr = itemTag.getAsString();
                } catch (Exception ignored) {}
                ShopSlotRecord rec = new ShopSlotRecord(shopId, slot, itemId, nbtStr, displayName, prices[slot], stack.getCount());
                ExchangeMod.getMarketDAO().saveOrUpdateShopSlot(rec);
            }
        } catch (Exception e) {
            ExchangeMod.LOGGER.error("Failed to sync shop slot to DB", e);
        }
    }

    public boolean insertFromHand(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= 10) return false;
        if (slot >= maxSlots) {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_locked", slot + 1));
            return false;
        }
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            hand = player.getOffhandItem();
        }
        if (hand.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.hold_item"));
            return false;
        }

        ItemStack existing = inventory.getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, hand)) {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_occupied"));
            return false;
        }

        ItemStack toInsert = hand.copy();
        ItemStack rem = inventory.insertItem(slot, toInsert, false);
        int inserted = hand.getCount() - rem.getCount();
        if (inserted > 0) {
            hand.shrink(inserted);
            setChanged();
            syncSlotToDatabase(slot);
            openShopScreen(player);
            player.sendSystemMessage(Component.translatable("message.exchange.shop.item_added", slot + 1));
            return true;
        } else {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_overflow", slotCapacity));
        }
        return false;
    }

    public boolean insertFromInventory(ServerPlayer player, int slot, int playerInvSlot) {
        if (slot < 0 || slot >= 10 || playerInvSlot < 0 || playerInvSlot >= player.getInventory().getContainerSize()) return false;
        if (slot >= maxSlots) {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_locked", slot + 1));
            return false;
        }
        ItemStack invStack = player.getInventory().getItem(playerInvSlot);
        if (invStack.isEmpty()) return false;

        ItemStack existing = inventory.getStackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, invStack)) {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_occupied"));
            return false;
        }

        ItemStack toInsert = invStack.copy();
        ItemStack rem = inventory.insertItem(slot, toInsert, false);
        int inserted = invStack.getCount() - rem.getCount();
        if (inserted > 0) {
            invStack.shrink(inserted);
            setChanged();
            syncSlotToDatabase(slot);
            openShopScreen(player);
            player.sendSystemMessage(Component.translatable("message.exchange.shop.item_added", slot + 1));
            return true;
        } else {
            player.sendSystemMessage(Component.translatable("message.exchange.shop.slot_overflow", slotCapacity));
        }
        return false;
    }

    public boolean extractToPlayer(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= 10) return false;
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.exchange.shop.slot_already_empty"));
            return false;
        }

        int totalCount = existing.getCount();
        ItemStack extracted = inventory.extractItem(slot, totalCount, false);
        if (!extracted.isEmpty()) {
            int rem = extracted.getCount();
            int maxStack = extracted.getMaxStackSize();
            while (rem > 0) {
                int stCount = Math.min(rem, maxStack);
                ItemStack piece = extracted.copyWithCount(stCount);
                if (!player.getInventory().add(piece)) {
                    player.drop(piece, false);
                }
                rem -= stCount;
            }
            setChanged();
            syncSlotToDatabase(slot);
            openShopScreen(player);
            player.sendSystemMessage(Component.translatable("gui.exchange.shop.item_returned", totalCount));
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("ShopId", shopId);
        if (ownerUuid != null) tag.putUUID("OwnerUUID", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putString("ShopName", shopName);
        tag.putBoolean("Broadcast", broadcast);
        tag.putDouble("AccumulatedRevenue", accumulatedRevenue);
        tag.putInt("TotalSales", totalSales);
        tag.put("Inventory", inventory.serializeNBT(registries));

        tag.putInt("MaxSlots", maxSlots);
        tag.putInt("SlotCapacity", slotCapacity);
        tag.putBoolean("NetworkUnlocked", networkUnlocked);

        for (int i = 0; i < 10; i++) {
            tag.putDouble("Price_" + i, prices[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ShopId")) shopId = tag.getString("ShopId");
        if (tag.hasUUID("OwnerUUID")) ownerUuid = tag.getUUID("OwnerUUID");
        if (tag.contains("OwnerName")) ownerName = tag.getString("OwnerName");
        if (tag.contains("ShopName")) shopName = tag.getString("ShopName");
        if (tag.contains("Broadcast")) broadcast = tag.getBoolean("Broadcast");
        if (tag.contains("AccumulatedRevenue")) accumulatedRevenue = tag.getDouble("AccumulatedRevenue");
        if (tag.contains("TotalSales")) totalSales = tag.getInt("TotalSales");
        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));

        if (tag.contains("MaxSlots")) maxSlots = Math.max(5, Math.min(10, tag.getInt("MaxSlots")));
        if (tag.contains("SlotCapacity")) slotCapacity = Math.max(64, tag.getInt("SlotCapacity"));
        if (tag.contains("NetworkUnlocked")) networkUnlocked = tag.getBoolean("NetworkUnlocked");

        for (int i = 0; i < 10; i++) {
            if (tag.contains("Price_" + i)) {
                prices[i] = tag.getDouble("Price_" + i);
            } else {
                prices[i] = 10.0;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        loadAdditional(tag, lookupProvider);
    }
}
