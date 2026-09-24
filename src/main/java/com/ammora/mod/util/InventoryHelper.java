package com.ammora.mod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Utility helper for inventory operations, space checking, and physical item transfers.
 */
public final class InventoryHelper {

    private InventoryHelper() {}

    /**
     * Checks if the given player inventory has sufficient space to hold count of target item stack.
     */
    public static boolean canPlayerHoldItem(Inventory inv, ItemStack target, int count) {
        if (target.isEmpty() || count <= 0) return true;
        int remainingNeeded = count;
        int maxStack = target.getMaxStackSize();
        for (int i = 0; i < 36; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) {
                remainingNeeded -= maxStack;
            } else if (ItemStack.isSameItemSameComponents(slot, target)) {
                int space = maxStack - slot.getCount();
                if (space > 0) {
                    remainingNeeded -= space;
                }
            }
            if (remainingNeeded <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely removes the specified amount of items from the player's inventory.
     */
    public static void removePlayerItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        player.containerMenu.broadcastChanges();
    }

    /**
     * Gives items to the player's inventory, dropping any overflow stacks on the ground.
     */
    public static void giveOrDropItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        int maxStack = item.getDefaultInstance().getMaxStackSize();
        while (remaining > 0) {
            int count = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= count;
        }
        player.containerMenu.broadcastChanges();
    }

    /**
     * Gives a specific ItemStack (preserving components/NBT) to the player's inventory, dropping overflow.
     */
    public static void giveOrDropStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.containerMenu.broadcastChanges();
    }

    /**
     * Counts how many items matching the given Item instance are present in player's inventory.
     */
    public static int countPlayerItems(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
