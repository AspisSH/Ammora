package com.ammora.mod.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Common interface for all flying courier delivery entities (Bee, Allay, Phantom, etc.).
 */
public interface ICourierEntity {

    void setDeliveryOrder(Player player, List<ItemStack> items);

    void setDeliveryOrder(Player player, ItemStack singleItem);

    ItemStack getDeliveredItem();

    UUID getTargetPlayerUuid();

    boolean isDelivered();

    void completeDelivery(Player player);

    void backupToDatabase();

    Mob asMob();
}
