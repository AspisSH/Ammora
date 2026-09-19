package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.config.AmmoraConfig;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.db.MarketTxRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.util.InventoryHelper;
import com.ammora.mod.entity.CourierBeeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles player shop (vending machine) configuration, slot management, upgrades,
 * remote/local purchases, and inventory extraction.
 */
public final class ShopPacketHandler {

    private ShopPacketHandler() {}

    public static void handleConfigureShop(ServerPlayer player, ServerboundConfigureShopPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            var shop = AmmoraMod.getMarketDAO().getPlayerShop(payload.shopId());
            if (shop == null || !shop.getOwnerUuid().equals(player.getUUID())) return;

            switch (payload.action()) {
                case "SET_PRICE" -> {
                    double p = Math.max(0.01, payload.price());
                    var slots = AmmoraMod.getMarketDAO().getShopSlots(shop.getShopId());
                    var targetSlot = slots.stream().filter(s -> s.getSlotIndex() == payload.slotIndex()).findFirst().orElse(null);
                    if (targetSlot != null) {
                        targetSlot.setPriceCbx(p);
                        AmmoraMod.getMarketDAO().saveOrUpdateShopSlot(targetSlot);
                    }
                }
                case "RENAME_SHOP" -> {
                    String newName = payload.textParam();
                    if (newName != null && !newName.trim().isEmpty()) {
                        shop.setShopName(newName.trim());
                        AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.name_changed", shop.getShopName()));
                    }
                }
                case "UPGRADE_CAPACITY" -> {
                    int curCap = shop.getSlotCapacity();
                    int nextCap;
                    if (curCap < 128) {
                        nextCap = 128;
                    } else if (curCap < 256) {
                        nextCap = 256;
                    } else if (curCap < 512) {
                        nextCap = 512;
                    } else if (curCap < 1024) {
                        nextCap = 1024;
                    } else {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.capacity_max"));
                        break;
                    }
                    double cost = AmmoraConfig.getCapacityUpgradeCost(curCap);
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (acc == null || acc.getBalanceCbx() < cost) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.insufficient_funds", String.format(Locale.US, "%.2f", cost)));
                        break;
                    }
                    acc.withdraw(cost);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    shop.setSlotCapacity(nextCap);
                    AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                    player.sendSystemMessage(Component.translatable("message.ammora.shop.capacity_upgraded", nextCap, String.format(Locale.US, "%.2f", cost)));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                case "UNLOCK_SLOT" -> {
                    int curSlots = shop.getMaxSlots();
                    if (curSlots >= 10) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.slots_all_unlocked"));
                        break;
                    }
                    int nextSlot = curSlots + 1;
                    double cost = AmmoraConfig.getSlotUnlockCost(nextSlot);
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (acc == null || acc.getBalanceCbx() < cost) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.insufficient_funds", String.format(Locale.US, "%.2f", cost)));
                        break;
                    }
                    acc.withdraw(cost);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    shop.setMaxSlots(nextSlot);
                    AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                    player.sendSystemMessage(Component.translatable("message.ammora.shop.slot_unlocked", nextSlot, String.format(Locale.US, "%.2f", cost)));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                case "UNLOCK_NETWORK" -> {
                    if (shop.isNetworkUnlocked()) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.satellite_already_installed"));
                        break;
                    }
                    double cost = AmmoraConfig.getSatelliteModuleCost();
                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (acc == null || acc.getBalanceCbx() < cost) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.insufficient_funds", String.format(Locale.US, "%.2f", cost)));
                        break;
                    }
                    acc.withdraw(cost);
                    AmmoraMod.getMarketDAO().saveAccount(acc);
                    shop.setNetworkUnlocked(true);
                    AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                    player.sendSystemMessage(Component.translatable("message.ammora.shop.satellite_installed"));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                case "TOGGLE_BROADCAST" -> {
                    if (!shop.isNetworkUnlocked()) {
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.satellite_required"));
                        break;
                    }
                    shop.setBroadcast(!shop.isBroadcast());
                    AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                }
                case "CLAIM_REVENUE" -> {
                    double rev = shop.getAccumulatedRevenue();
                    if (rev > 0.001) {
                        PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                        if (acc != null) {
                            acc.deposit(rev);
                            AmmoraMod.getMarketDAO().saveAccount(acc);
                        }
                        shop.clearRevenue();
                        AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);
                        player.sendSystemMessage(Component.translatable("message.ammora.shop.revenue_withdrawn", String.format(Locale.US, "%.2f", MarketEngine.round2(rev))));
                        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                    }
                }
            }

            // Sync with BlockEntity if loaded
            BlockPos pos = new BlockPos(shop.getPosX(), shop.getPosY(), shop.getPosZ());
            ServerLevel shopLevel = player.getServer().getLevel(
                    ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(shop.getDimension())
                    )
            );
            if (shopLevel == null) shopLevel = player.serverLevel();

            var be = shopLevel.getBlockEntity(pos);
            if (be instanceof com.ammora.mod.blocks.PlayerShopEntity shopEntity) {
                shopEntity.setShopName(shop.getShopName());
                shopEntity.setMaxSlots(shop.getMaxSlots());
                shopEntity.setSlotCapacity(shop.getSlotCapacity());
                shopEntity.setNetworkUnlocked(shop.isNetworkUnlocked());
                if ("SET_PRICE".equals(payload.action())) shopEntity.setSlotPrice(payload.slotIndex(), payload.price());
                if ("TOGGLE_BROADCAST".equals(payload.action())) shopEntity.setBroadcast(shop.isBroadcast());
                if ("CLAIM_REVENUE".equals(payload.action())) shopEntity.claimRevenue(player);
                if ("INSERT_HAND".equals(payload.action())) shopEntity.insertFromHand(player, payload.slotIndex());
                if ("INSERT_INVENTORY".equals(payload.action())) {
                    try {
                        int invSlot = Integer.parseInt(payload.textParam());
                        shopEntity.insertFromInventory(player, payload.slotIndex(), invSlot);
                    } catch (Exception ignored) {}
                }
                if ("EXTRACT_ITEM".equals(payload.action())) shopEntity.extractToPlayer(player, payload.slotIndex());
                shopEntity.openShopScreen(player);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to configure shop", e);
        }
    }

    public static void handleShopPurchase(ServerPlayer buyer, ServerboundShopPurchasePayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            var shop = AmmoraMod.getMarketDAO().getPlayerShop(payload.shopId());
            if (shop == null) {
                if (payload.isRemote()) EscrowPacketHandler.sendMarketplaceData(buyer, AmmoraLang.notify("shop_not_found"), true);
                return;
            }

            var slots = AmmoraMod.getMarketDAO().getShopSlots(shop.getShopId());
            var slot = slots.stream().filter(s -> s.getSlotIndex() == payload.slotIndex()).findFirst().orElse(null);
            if (slot == null || slot.getStockCount() <= 0 || slot.getItemId().isEmpty()) {
                if (payload.isRemote()) EscrowPacketHandler.sendMarketplaceData(buyer, AmmoraLang.notify("shop_out_of_stock"), true);
                return;
            }

            int count = Math.min(Math.max(1, payload.amount()), slot.getStockCount());
            double baseTotal = slot.getPriceCbx() * count;
            double fee = payload.isRemote() ? Math.max(1.0, Math.round(baseTotal * 0.02 * 100.0) / 100.0) : 0.0;
            double totalCharge = baseTotal + fee;

            PlayerAccount buyerAcc = AmmoraMod.getMarketDAO().getAccount(buyer.getUUID(), buyer.getName().getString());
            if (buyerAcc == null || buyerAcc.getBalanceCbx() < totalCharge) {
                if (payload.isRemote()) {
                    EscrowPacketHandler.sendMarketplaceData(buyer, "key:message.ammora.shop.insufficient_funds;" + MarketEngine.round2(totalCharge), true);
                } else {
                    buyer.sendSystemMessage(Component.translatable("message.ammora.shop.insufficient_funds", String.format(Locale.US, "%.2f", totalCharge)));
                }
                return;
            }

            // Verify buyer has enough inventory space before charging money or extracting items
            ItemStack previewStack = ItemStack.EMPTY;
            if (slot.getItemNbt() != null && !slot.getItemNbt().isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(slot.getItemNbt());
                    previewStack = ItemStack.parseOptional(buyer.registryAccess(), tag);
                } catch (Exception ignored) {}
            }
            if (previewStack.isEmpty()) {
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slot.getItemId()));
                if (it != null && it != Items.AIR) {
                    previewStack = new ItemStack(it);
                }
            }
            if (!InventoryHelper.canPlayerHoldItem(buyer.getInventory(), previewStack, count)) {
                if (payload.isRemote()) {
                    EscrowPacketHandler.sendMarketplaceData(buyer, "key:message.ammora.inventory_full", true);
                } else {
                    buyer.sendSystemMessage(Component.translatable("message.ammora.inventory_full"));
                    buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
                return;
            }

            // Deduct from buyer
            buyerAcc.withdraw(totalCharge);
            AmmoraMod.getMarketDAO().saveAccount(buyerAcc);

            // Add revenue to shop
            shop.addRevenue(baseTotal);
            shop.incrementSales(count);
            AmmoraMod.getMarketDAO().saveOrUpdatePlayerShop(shop);

            // Decrement stock in DB
            slot.setStockCount(slot.getStockCount() - count);
            AmmoraMod.getMarketDAO().saveOrUpdateShopSlot(slot);

            // Update BlockEntity if loaded & extract real items
            BlockPos pos = new BlockPos(shop.getPosX(), shop.getPosY(), shop.getPosZ());
            ServerLevel shopLevel = buyer.getServer().getLevel(
                    ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(shop.getDimension())
                    )
            );
            if (shopLevel == null) shopLevel = buyer.serverLevel();

            ItemStack extracted = ItemStack.EMPTY;
            var be = shopLevel.getBlockEntity(pos);
            if (be instanceof com.ammora.mod.blocks.PlayerShopEntity shopEntity) {
                extracted = shopEntity.getInventory().extractItem(slot.getSlotIndex(), count, false);
                shopEntity.addSale(count, baseTotal);
            }

            // If block entity was not loaded (remote chunk), deserialize from slot.getItemNbt()
            if (extracted.isEmpty() && slot.getItemNbt() != null && !slot.getItemNbt().isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(slot.getItemNbt());
                    extracted = ItemStack.parseOptional(buyer.registryAccess(), tag);
                    if (!extracted.isEmpty()) {
                        extracted.setCount(count);
                    }
                } catch (Exception ignored) {}
            }

            // Fallback if still empty
            if (extracted.isEmpty()) {
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slot.getItemId()));
                if (it != null && it != Items.AIR) {
                    extracted = new ItemStack(it, count);
                }
            }

            // Give items to buyer preserving full enchantments & durability
            if (!extracted.isEmpty()) {
                List<ItemStack> stacksToDeliver = new ArrayList<>();
                int rem = count;
                int maxStack = extracted.getMaxStackSize();
                while (rem > 0) {
                    int stCount = Math.min(rem, maxStack);
                    stacksToDeliver.add(extracted.copyWithCount(stCount));
                    rem -= stCount;
                }

                if (payload.isRemote()) {
                    dispatchCourierBee(buyer, stacksToDeliver, slot);
                } else {
                    for (ItemStack stack : stacksToDeliver) {
                        if (!buyer.getInventory().add(stack)) {
                            // Safety fallback: save to unclaimed delivery buffer instead of dropping on ground!
                            String nbt = "";
                            try {
                                Tag t = stack.saveOptional(buyer.registryAccess());
                                if (t != null) nbt = t.getAsString();
                            } catch (Exception ignored) {}
                            AmmoraMod.getMarketDAO().saveUnclaimedDelivery(UUID.randomUUID().toString(), buyer.getUUID(), slot.getItemId(), stack.getCount(), System.currentTimeMillis(), nbt);
                        }
                    }
                }
            }

            // Record transaction
            AmmoraMod.getMarketDAO().recordMarketTransaction(new MarketTxRecord(
                    UUID.randomUUID().toString(),
                    payload.isRemote() ? "REMOTE_BUY" : "LOCAL_BUY",
                    shop.getShopId(),
                    buyer.getUUID(), buyer.getName().getString(),
                    shop.getOwnerUuid(), shop.getOwnerName(),
                    slot.getItemId(), slot.getDisplayName(),
                    count, totalCharge, fee, System.currentTimeMillis()
            ));

            buyer.level().playSound(null, buyer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            if (payload.isRemote()) {
                EscrowPacketHandler.sendMarketplaceData(buyer, "key:message.ammora.shop.buy_success;" + count + ";" + slot.getDisplayName() + ";" + MarketEngine.round2(totalCharge), false);
            } else {
                buyer.sendSystemMessage(Component.translatable("message.ammora.shop.buy_success", count, slot.getDisplayName(), MarketEngine.round2(totalCharge)));
                if (be instanceof com.ammora.mod.blocks.PlayerShopEntity shopEntity) {
                    shopEntity.openShopScreen(buyer);
                }
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to execute shop purchase", e);
        }
    }

    private static void dispatchCourierBee(ServerPlayer buyer, List<ItemStack> stacks, com.ammora.mod.db.ShopSlotRecord slot) {
        ServerLevel level = buyer.serverLevel();
        Vec3 playerPos = buyer.position();

        // Calculate spawn position 12-16 blocks away at a dynamic angle
        double rotRad = Math.toRadians(buyer.getYRot() + 180 + (level.random.nextDouble() - 0.5) * 60.0);
        double distance = 12.0 + level.random.nextDouble() * 3.0;
        double spawnX = playerPos.x - Math.sin(rotRad) * distance;
        double spawnZ = playerPos.z + Math.cos(rotRad) * distance;
        double spawnY = playerPos.y + 1.5 + level.random.nextDouble() * 2.0;

        BlockPos testPos = BlockPos.containing(spawnX, spawnY, spawnZ);
        if (!level.getBlockState(testPos).isAir()) {
            // If obstructed, spawn overhead in clear area
            spawnX = playerPos.x + (level.random.nextDouble() - 0.5) * 4.0;
            spawnZ = playerPos.z + (level.random.nextDouble() - 0.5) * 4.0;
            spawnY = playerPos.y + 2.5;
        }

        CourierBeeEntity bee = AmmoraMod.COURIER_BEE.get().create(level);
        if (bee != null) {
            bee.moveTo(spawnX, spawnY, spawnZ, buyer.getYRot(), 0.0F);
            bee.setDeliveryOrder(buyer, stacks);
            level.addFreshEntity(bee);

            level.playSound(null, buyer.blockPosition(), SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.8F, 1.2F);
            buyer.displayClientMessage(AmmoraLang.message("courier.dispatched"), true);
        } else {
            // Direct fallback in case entity creation is disallowed
            for (ItemStack stack : stacks) {
                if (!buyer.getInventory().add(stack)) {
                    String nbt = "";
                    try {
                        Tag t = stack.saveOptional(buyer.registryAccess());
                        if (t != null) nbt = t.getAsString();
                    } catch (Exception ignored) {}
                    try {
                        AmmoraMod.getMarketDAO().saveUnclaimedDelivery(
                                UUID.randomUUID().toString(),
                                buyer.getUUID(),
                                slot.getItemId(),
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
    }
}
