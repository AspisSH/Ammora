package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.db.BuyRequestRecord;
import com.ammora.mod.db.CommunityQuestRecord;
import com.ammora.mod.db.MarketTxRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.entity.CourierType;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.util.InventoryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles RFQ buy requests with escrow, community quests/bounties,
 * unclaimed delivery buffers, and marketplace catalog data dispatching.
 */
public final class EscrowPacketHandler {

    private EscrowPacketHandler() {}

    public static void sendMarketplaceData(ServerPlayer player, String statusMsg, boolean isError) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            double balance = acc != null ? acc.getBalanceCbx() : 0.0;
            int repLevel = acc != null ? acc.getRepLevel() : 1;

            var rawSlots = AmmoraMod.getMarketDAO().getAllActiveCatalogSlots();
            List<MarketplaceDataPayload.MarketplaceSlotItem> catalog = new ArrayList<>();
            for (var s : rawSlots) {
                String shopName = s.getShopName() != null ? s.getShopName() : AmmoraLang.guiStr("shop.default_name_anonymous");
                String ownerName = s.getOwnerName() != null ? s.getOwnerName() : AmmoraLang.guiStr("shop.default_owner_anonymous");
                double fee = Math.max(1.0, Math.round(s.getPriceCbx() * 0.02 * 100.0) / 100.0);

                ItemStack itemStack = ItemStack.EMPTY;
                if (s.getItemNbt() != null && !s.getItemNbt().isEmpty()) {
                    try {
                        net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(s.getItemNbt());
                        itemStack = ItemStack.parseOptional(player.serverLevel().registryAccess(), tag);
                    } catch (Exception ignored) {}
                }
                if (itemStack.isEmpty() && s.getItemId() != null && !s.getItemId().isEmpty()) {
                    try {
                        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.getItemId()));
                        if (it != Items.AIR) {
                            itemStack = new ItemStack(it, Math.max(1, s.getStockCount()));
                        }
                    } catch (Exception ignored) {}
                }

                List<String> lore = new ArrayList<>();
                if (!itemStack.isEmpty()) {
                    if (itemStack.isDamageableItem()) {
                        int maxDmg = itemStack.getMaxDamage();
                        int curDmg = itemStack.getDamageValue();
                        int remain = maxDmg - curDmg;
                        double pct = Math.round((remain * 100.0 / maxDmg) * 10.0) / 10.0;
                        String color = pct < 25.0 ? "§c" : (pct < 60.0 ? "§e" : "§a");
                        lore.add(AmmoraLang.guiStr("shop.durability", color + remain, "§a" + maxDmg, color + pct + "%"));
                    }

                    try {
                        List<Component> lines = itemStack.getTooltipLines(
                                Item.TooltipContext.of(player.serverLevel()),
                                player,
                                net.minecraft.world.item.TooltipFlag.Default.NORMAL
                        );
                        for (Component c : lines) {
                            String str = c.getString();
                            if (!str.equalsIgnoreCase(s.getDisplayName()) && !str.isBlank()) {
                                lore.add(str);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                catalog.add(new MarketplaceDataPayload.MarketplaceSlotItem(
                        s.getShopId(), shopName, ownerName, s.getSlotIndex(), s.getItemId(), s.getDisplayName(),
                        s.getPriceCbx(), s.getStockCount(), fee, lore
                ));
            }

            var rawShops = AmmoraMod.getMarketDAO().getAllBroadcastShops();
            List<MarketplaceDataPayload.MarketplaceShopItem> shops = new ArrayList<>();
            for (var sh : rawShops) {
                int activeCount = sh.getActiveSlotCount();
                shops.add(new MarketplaceDataPayload.MarketplaceShopItem(
                        sh.getShopId(), sh.getShopName(), sh.getOwnerUuid(), sh.getOwnerName(),
                        sh.getDimension(), sh.getPosX(), sh.getPosY(), sh.getPosZ(),
                        activeCount, sh.getTotalSales()
                ));
            }

            var rawReqs = AmmoraMod.getMarketDAO().getActiveBuyRequests();
            List<MarketplaceDataPayload.BuyRequestItem> buyReqs = new ArrayList<>();
            for (var r : rawReqs) {
                boolean isOwn = r.getBuyerUuid().equals(player.getUUID());
                buyReqs.add(new MarketplaceDataPayload.BuyRequestItem(
                        r.getRequestId(), r.getBuyerUuid(), r.getBuyerName(), r.getItemId(), r.getDisplayName(),
                        r.getUnitPrice(), r.getRemainingAmount(), r.getEscrowCbx(), isOwn
                ));
            }

            var rawQuests = AmmoraMod.getMarketDAO().getAllQuests();
            List<MarketplaceDataPayload.CommunityQuestItem> questItems = new ArrayList<>();
            for (var q : rawQuests) {
                boolean isOwn = q.getCreatorUuid().equals(player.getUUID());
                boolean isAssignedToMe = q.getWorkerUuid() != null && q.getWorkerUuid().equals(player.getUUID());
                questItems.add(new MarketplaceDataPayload.CommunityQuestItem(
                        q.getQuestId(), q.getCreatorUuid(), q.getCreatorName(),
                        q.getTitle(), q.getDescription(), q.getRewardCbx(),
                        q.getStatus(), q.getWorkerUuid(), q.getWorkerName(),
                        q.getCreatedAt(), isOwn, isAssignedToMe
                ));
            }

            var rawTxs = AmmoraMod.getMarketDAO().getRecentMarketTransactions(30);
            List<MarketplaceDataPayload.MarketTxItem> txs = new ArrayList<>();
            for (var t : rawTxs) {
                txs.add(new MarketplaceDataPayload.MarketTxItem(
                        t.getTxId(), t.getTxType(), t.getBuyerName(), t.getSellerName(),
                        t.getItemName(), t.getAmount(), t.getTotalCbx(), t.getFeeCbx(), t.getTimestamp()
                ));
            }

            var rawDeliveries = AmmoraMod.getMarketDAO().getUnclaimedDeliveries(player.getUUID());
            List<MarketplaceDataPayload.DeliveryBufferItem> deliveries = new ArrayList<>();
            for (var d : rawDeliveries) {
                var it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(d.resourceId()));
                String displayName = (it != null && it != Items.AIR) ? it.getDescription().getString() : d.resourceId();
                deliveries.add(new MarketplaceDataPayload.DeliveryBufferItem(
                        d.deliveryId(), d.resourceId(), displayName, d.amount(), d.timestamp(), d.itemNbt()
                ));
            }

            var rawAuctions = AmmoraMod.getMarketDAO().getActiveAuctions();
            List<MarketplaceDataPayload.LiveAuctionItem> auctionItems = new ArrayList<>();
            for (var a : rawAuctions) {
                boolean isOwn = a.getSellerUuid().equals(player.getUUID());
                boolean isLeading = player.getUUID().equals(a.getHighestBidderUuid());
                auctionItems.add(new MarketplaceDataPayload.LiveAuctionItem(
                        a.getAuctionId(),
                        a.getSellerUuid(),
                        a.getSellerName(),
                        a.getItemId(),
                        a.getItemNbt(),
                        a.getDisplayName(),
                        a.getItemCount(),
                        a.getStartPrice(),
                        a.getCurrentBid(),
                        a.getMinBidStep(),
                        a.getBuyoutPrice(),
                        a.getHighestBidderUuid(),
                        a.getHighestBidderName(),
                        a.getCreatedAt(),
                        a.getExpiresAt(),
                        a.getStatus(),
                        isOwn,
                        isLeading
                ));
            }

            // Load Company Data
            double regFee = AmmoraMod.getMarketDAO().getCompanyRegistrationFee();
            MarketplaceDataPayload.CompanyData companyData = MarketplaceDataPayload.CompanyData.none(regFee);
            try {
                var comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
                if (comp != null) {
                    var member = AmmoraMod.getMarketDAO().getCompanyMember(comp.getCompanyId(), player.getUUID());
                    String myRole = member != null ? member.getRole() : "MEMBER";
                    double myLimit = member != null ? member.getDailyLimitCbx() : 0.0;
                    double mySpent = member != null ? member.getSpentTodayCbx() : 0.0;

                    List<MarketplaceDataPayload.CompanyMemberItem> memberItems = new ArrayList<>();
                    for (var m : AmmoraMod.getMarketDAO().getCompanyMembers(comp.getCompanyId())) {
                        memberItems.add(new MarketplaceDataPayload.CompanyMemberItem(
                                m.getPlayerUuid(),
                                m.getPlayerName(),
                                m.getRole(),
                                m.getDailyLimitCbx(),
                                m.getSpentTodayCbx(),
                                m.getJoinedAt()
                        ));
                    }

                    List<MarketplaceDataPayload.CompanyLedgerItem> ledgerItems = new ArrayList<>();
                    for (var l : AmmoraMod.getMarketDAO().getCompanyLedger(comp.getCompanyId(), 30)) {
                        ledgerItems.add(new MarketplaceDataPayload.CompanyLedgerItem(
                                l.getEntryId(),
                                l.getPlayerUuid(),
                                l.getPlayerName(),
                                l.getActionType(),
                                l.getAmountCbx(),
                                l.getDescription(),
                                l.getTimestamp()
                        ));
                    }

                    String ownerName = "Unknown";
                    for (var m : memberItems) {
                        if (m.playerUuid().equals(comp.getOwnerUuid())) {
                            ownerName = m.playerName();
                            break;
                        }
                    }

                    companyData = new MarketplaceDataPayload.CompanyData(
                            true,
                            comp.getCompanyId(),
                            comp.getCompanyName(),
                            comp.getOwnerUuid(),
                            ownerName,
                            comp.getBalanceCbx(),
                            myRole,
                            myLimit,
                            mySpent,
                            regFee,
                            memberItems,
                            ledgerItems
                    );
                }
            } catch (Exception e) {
                AmmoraMod.LOGGER.error("Failed to load company data for player " + player.getName().getString(), e);
            }

            String activeCourier = "BEE";
            List<String> unlockedCouriers = List.of("BEE");
            List<MarketplaceDataPayload.CourierProgressItem> courierProgress = new ArrayList<>();
            if (AmmoraMod.getMarketDAO() != null) {
                activeCourier = AmmoraMod.getMarketDAO().getActiveCourier(player.getUUID());
                unlockedCouriers = AmmoraMod.getMarketDAO().getUnlockedCouriers(player.getUUID());
                com.ammora.mod.db.MarketDAO.CourierProgressStats stats = AmmoraMod.getMarketDAO().getCourierProgressStats(player.getUUID());
                for (com.ammora.mod.entity.CourierType type : com.ammora.mod.entity.CourierType.values()) {
                    courierProgress.add(new MarketplaceDataPayload.CourierProgressItem(
                            type.getId(),
                            type.getProgress(stats),
                            type.getTargetGoal(),
                            type.isClaimable(stats)
                    ));
                }
            }

            PacketDistributor.sendToPlayer(player, new MarketplaceDataPayload(
                    balance, repLevel, catalog, shops, buyReqs, questItems, txs, deliveries, auctionItems, companyData,
                    activeCourier, unlockedCouriers, courierProgress, statusMsg, isError
            ));
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to send marketplace data to " + player.getName().getString(), e);
        }
    }

    public static void handleBuyRequestAction(ServerPlayer player, ServerboundBuyRequestPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            switch (payload.action()) {
                case "CREATE" -> {
                    double unitPrice = Math.max(0.01, payload.unitPrice());
                    int amount = Math.max(1, payload.amount());
                    double totalEscrow = unitPrice * amount;

                    PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (acc == null || acc.getBalanceCbx() < totalEscrow) {
                        sendMarketplaceData(player, AmmoraLang.notify("rfq_escrow_insufficient", MarketEngine.round2(totalEscrow)), true);
                        return;
                    }

                    acc.withdraw(totalEscrow);
                    AmmoraMod.getMarketDAO().saveAccount(acc);

                    BuyRequestRecord req = new BuyRequestRecord(
                            UUID.randomUUID().toString(),
                            player.getUUID(), player.getName().getString(),
                            payload.itemId(), "", payload.displayName(),
                            unitPrice, amount, 0, totalEscrow, "ACTIVE", System.currentTimeMillis()
                    );
                    AmmoraMod.getMarketDAO().saveBuyRequest(req);
                    sendMarketplaceData(player, AmmoraLang.notify("rfq_created", MarketEngine.round2(totalEscrow)), false);
                }
                case "CANCEL" -> {
                    var req = AmmoraMod.getMarketDAO().getBuyRequest(payload.requestId());
                    if (req == null || !req.getBuyerUuid().equals(player.getUUID()) || !"ACTIVE".equals(req.getStatus())) {
                        sendMarketplaceData(player, AmmoraLang.notify("rfq_cannot_cancel"), true);
                        return;
                    }

                    double refund = req.getEscrowCbx();
                    if (refund > 0.001) {
                        PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                        if (acc != null) {
                            acc.deposit(refund);
                            AmmoraMod.getMarketDAO().saveAccount(acc);
                        }
                    }
                    req.cancel();
                    AmmoraMod.getMarketDAO().updateBuyRequest(req);
                    sendMarketplaceData(player, AmmoraLang.notify("rfq_canceled", MarketEngine.round2(refund)), false);
                }
                case "FULFILL" -> {
                    var req = AmmoraMod.getMarketDAO().getBuyRequest(payload.requestId());
                    if (req == null || !"ACTIVE".equals(req.getStatus())) {
                        sendMarketplaceData(player, AmmoraLang.notify("rfq_not_active"), true);
                        return;
                    }

                    int count = Math.min(Math.max(1, payload.amount()), req.getRemainingAmount());

                    // Verify seller has enough items
                    int found = 0;
                    for (ItemStack st : player.getInventory().items) {
                        if (!st.isEmpty() && BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(req.getItemId())) {
                            found += st.getCount();
                        }
                    }

                    if (found < count) {
                        sendMarketplaceData(player, AmmoraLang.notify("rfq_seller_insufficient", found, count), true);
                        return;
                    }

                    // Remove items from seller
                    int neededToRemove = count;
                    for (int i = 0; i < player.getInventory().items.size(); i++) {
                        ItemStack st = player.getInventory().items.get(i);
                        if (!st.isEmpty() && BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(req.getItemId())) {
                            int toTake = Math.min(neededToRemove, st.getCount());
                            st.shrink(toTake);
                            neededToRemove -= toTake;
                            if (neededToRemove <= 0) break;
                        }
                    }

                    // Payout to seller
                    double payout = req.getUnitPrice() * count;
                    PlayerAccount sellerAcc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (sellerAcc != null) {
                        sellerAcc.deposit(payout);
                        AmmoraMod.getMarketDAO().saveAccount(sellerAcc);
                    }

                    req.fulfill(count);
                    AmmoraMod.getMarketDAO().updateBuyRequest(req);

                    // Deliver to buyer (online or offline unclaimed table)
                    ServerPlayer buyerPlayer = player.getServer().getPlayerList().getPlayer(req.getBuyerUuid());
                    Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(req.getItemId()));
                    ItemStack deliverStack = (it != null && it != Items.AIR) ? new ItemStack(it, count) : ItemStack.EMPTY;

                    if (buyerPlayer != null && InventoryHelper.canPlayerHoldItem(buyerPlayer.getInventory(), deliverStack, count)) {
                        buyerPlayer.getInventory().add(deliverStack);
                        buyerPlayer.sendSystemMessage(Component.translatable("message.ammora.dock.delivered_direct", count, req.getDisplayName()));
                        buyerPlayer.level().playSound(null, buyerPlayer.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                    } else {
                        // Inventory full or buyer offline -> Safe delivery buffer in tablet
                        AmmoraMod.getMarketDAO().saveUnclaimedDelivery(UUID.randomUUID().toString(), req.getBuyerUuid(), req.getItemId(), count, System.currentTimeMillis());
                        if (buyerPlayer != null) {
                            buyerPlayer.sendSystemMessage(Component.translatable("message.ammora.dock.delivered_buffer", count, req.getDisplayName()));
                            buyerPlayer.level().playSound(null, buyerPlayer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        }
                    }

                    // Record transaction
                    AmmoraMod.getMarketDAO().recordMarketTransaction(new MarketTxRecord(
                            UUID.randomUUID().toString(),
                            "BUY_REQUEST",
                            "",
                            req.getBuyerUuid(), req.getBuyerName(),
                            player.getUUID(), player.getName().getString(),
                            req.getItemId(), req.getDisplayName(),
                            count, payout, 0.0, System.currentTimeMillis()
                    ));

                    player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                    sendMarketplaceData(player, AmmoraLang.notify("rfq_fulfilled", MarketEngine.round2(payout)), false);
                }
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to process buy request action", e);
        }
    }

    public static void handleCommunityQuestAction(ServerPlayer player, ServerboundCommunityQuestPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            switch (payload.action()) {
                case "CREATE" -> {
                    String title = payload.title() != null ? payload.title().trim() : "";
                    String desc = payload.description() != null ? payload.description().trim() : "";
                    double reward = Math.max(0.0, payload.rewardCbx());

                    if (title.isEmpty()) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_enter_title"), true);
                        return;
                    }
                    if (title.length() > 60) {
                        title = title.substring(0, 60);
                    }
                    if (desc.isEmpty()) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_enter_desc"), true);
                        return;
                    }
                    if (desc.length() > 500) {
                        desc = desc.substring(0, 500);
                    }

                    CommunityQuestRecord quest = new CommunityQuestRecord(
                            UUID.randomUUID().toString(),
                            player.getUUID(),
                            player.getName().getString(),
                            title,
                            desc,
                            reward,
                            "OPEN",
                            null,
                            "",
                            System.currentTimeMillis()
                    );
                    AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
                    sendMarketplaceData(player, AmmoraLang.notify("quest_published"), false);
                }
                case "ACCEPT" -> {
                    var quest = AmmoraMod.getMarketDAO().getQuest(payload.questId());
                    if (quest == null || !"OPEN".equalsIgnoreCase(quest.getStatus())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_unavailable"), true);
                        return;
                    }
                    if (quest.getCreatorUuid().equals(player.getUUID())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_cannot_accept_own"), true);
                        return;
                    }

                    quest.setStatus("IN_PROGRESS");
                    quest.setWorkerUuid(player.getUUID());
                    quest.setWorkerName(player.getName().getString());
                    AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);

                    // Notify creator if online
                    ServerPlayer creator = player.getServer().getPlayerList().getPlayer(quest.getCreatorUuid());
                    if (creator != null) {
                        creator.sendSystemMessage(Component.translatable("message.ammora.quest.taken", player.getName().getString(), quest.getTitle()));
                        creator.level().playSound(null, creator.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
                    }

                    player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.8F, 1.1F);
                    sendMarketplaceData(player, AmmoraLang.notify("quest_accepted"), false);
                }
                case "CANCEL_WORK" -> {
                    var quest = AmmoraMod.getMarketDAO().getQuest(payload.questId());
                    if (quest == null || !"IN_PROGRESS".equalsIgnoreCase(quest.getStatus())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_not_in_progress"), true);
                        return;
                    }
                    boolean isWorker = quest.getWorkerUuid() != null && quest.getWorkerUuid().equals(player.getUUID());
                    boolean isCreator = quest.getCreatorUuid().equals(player.getUUID());
                    if (!isWorker && !isCreator) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_not_authorized"), true);
                        return;
                    }

                    quest.setStatus("OPEN");
                    quest.setWorkerUuid(null);
                    quest.setWorkerName("");
                    AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);

                    sendMarketplaceData(player, AmmoraLang.notify("quest_work_canceled"), false);
                }
                case "COMPLETE" -> {
                    var quest = AmmoraMod.getMarketDAO().getQuest(payload.questId());
                    if (quest == null || !quest.getCreatorUuid().equals(player.getUUID())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_creator_only_complete"), true);
                        return;
                    }

                    quest.setStatus("COMPLETED");
                    AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);
                    sendMarketplaceData(player, AmmoraLang.notify("quest_marked_completed"), false);
                }
                case "PAY_REWARD" -> {
                    var quest = AmmoraMod.getMarketDAO().getQuest(payload.questId());
                    if (quest == null || !quest.getCreatorUuid().equals(player.getUUID())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_creator_only_reward"), true);
                        return;
                    }
                    if (quest.getWorkerUuid() == null) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_no_worker"), true);
                        return;
                    }
                    double reward = quest.getRewardCbx();
                    if (reward <= 0.001) {
                        quest.setStatus("COMPLETED");
                        AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);
                        sendMarketplaceData(player, AmmoraLang.notify("quest_completed_free"), false);
                        return;
                    }

                    PlayerAccount creatorAcc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
                    if (creatorAcc == null || creatorAcc.getBalanceCbx() < reward) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_insufficient_funds", MarketEngine.round2(reward)), true);
                        return;
                    }

                    PlayerAccount workerAcc = AmmoraMod.getMarketDAO().getAccount(quest.getWorkerUuid(), quest.getWorkerName());

                    creatorAcc.withdraw(reward);
                    workerAcc.deposit(reward);
                    AmmoraMod.getMarketDAO().saveAccount(creatorAcc);
                    AmmoraMod.getMarketDAO().saveAccount(workerAcc);

                    quest.setStatus("COMPLETED");
                    AmmoraMod.getMarketDAO().saveOrUpdateQuest(quest);

                    // Record market transaction
                    AmmoraMod.getMarketDAO().recordMarketTransaction(new MarketTxRecord(
                            UUID.randomUUID().toString(),
                            "QUEST_REWARD",
                            quest.getQuestId(),
                            player.getUUID(), player.getName().getString(),
                            quest.getWorkerUuid(), quest.getWorkerName(),
                            "ammora:quest_reward", AmmoraLang.messageStr("quest_tx_desc", quest.getTitle()),
                            1, reward, 0.0, System.currentTimeMillis()
                    ));

                    // Notify worker if online
                    ServerPlayer workerPlayer = player.getServer().getPlayerList().getPlayer(quest.getWorkerUuid());
                    if (workerPlayer != null) {
                        workerPlayer.sendSystemMessage(Component.translatable("message.ammora.quest.reward_paid", player.getName().getString(), String.format(Locale.US, "%.2f", MarketEngine.round2(reward)), quest.getTitle()));
                        workerPlayer.level().playSound(null, workerPlayer.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    }

                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.0F);
                    sendMarketplaceData(player, AmmoraLang.notify("quest_reward_paid_creator", MarketEngine.round2(reward), quest.getWorkerName()), false);
                }
                case "DELETE" -> {
                    var quest = AmmoraMod.getMarketDAO().getQuest(payload.questId());
                    if (quest == null || !quest.getCreatorUuid().equals(player.getUUID())) {
                        sendMarketplaceData(player, AmmoraLang.notify("quest_creator_only_delete"), true);
                        return;
                    }

                    AmmoraMod.getMarketDAO().deleteQuest(payload.questId());
                    sendMarketplaceData(player, AmmoraLang.notify("quest_deleted"), false);
                }
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to process community quest action", e);
            sendMarketplaceData(player, AmmoraLang.notify("quest_action_error"), true);
        }
    }

    public static void handleClaimDelivery(ServerPlayer player, ServerboundClaimDeliveryPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            var deliveries = AmmoraMod.getMarketDAO().getUnclaimedDeliveries(player.getUUID());
            if (deliveries == null || deliveries.isEmpty()) {
                sendMarketplaceData(player, AmmoraLang.notify("buffer_empty"), false);
                return;
            }

            int totalClaimed = 0;
            boolean hadSpaceIssue = false;

            for (var d : deliveries) {
                if (!payload.claimAll() && !d.deliveryId().equals(payload.deliveryId())) {
                    continue;
                }

                ItemStack stack = ItemStack.EMPTY;
                if (d.itemNbt() != null && !d.itemNbt().isEmpty()) {
                    try {
                        net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(d.itemNbt());
                        stack = ItemStack.parseOptional(player.registryAccess(), tag);
                    } catch (Exception ignored) {}
                }
                if (stack.isEmpty()) {
                    Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(d.resourceId()));
                    if (it != null && it != Items.AIR) {
                        stack = new ItemStack(it);
                    }
                }

                if (stack.isEmpty()) {
                    AmmoraMod.getMarketDAO().deleteUnclaimedDelivery(d.deliveryId());
                    continue;
                }

                int rem = d.amount();
                int maxStack = stack.getMaxStackSize();
                int claimedFromThis = 0;

                while (rem > 0) {
                    int toGive = Math.min(rem, maxStack);
                    ItemStack toAdd = stack.copyWithCount(toGive);
                    if (InventoryHelper.canPlayerHoldItem(player.getInventory(), toAdd, toGive)) {
                        player.getInventory().add(toAdd);
                        claimedFromThis += toGive;
                        rem -= toGive;
                    } else {
                        hadSpaceIssue = true;
                        break;
                    }
                }

                totalClaimed += claimedFromThis;

                if (rem <= 0) {
                    AmmoraMod.getMarketDAO().deleteUnclaimedDelivery(d.deliveryId());
                } else if (claimedFromThis > 0) {
                    AmmoraMod.getMarketDAO().updateUnclaimedDeliveryAmount(d.deliveryId(), rem);
                }

                if (hadSpaceIssue) {
                    break;
                }
            }

            if (totalClaimed > 0) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            }

            String msg;
            boolean isErr = false;
            if (hadSpaceIssue) {
                if (totalClaimed > 0) {
                    msg = AmmoraLang.notify("buffer_claimed_partial", totalClaimed);
                } else {
                    msg = AmmoraLang.notify("message.ammora.inventory_full");
                    isErr = true;
                    player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            } else if (totalClaimed > 0) {
                msg = AmmoraLang.notify("buffer_claimed_all", totalClaimed);
            } else {
                msg = AmmoraLang.notify("buffer_claim_failed");
                isErr = true;
            }

            sendMarketplaceData(player, msg, isErr);
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to claim delivery for " + player.getName().getString(), e);
        }
    }

    public static void handleCourierSkinAction(ServerPlayer player, ServerboundCourierSkinPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        try {
            CourierType type = CourierType.fromId(payload.courierId());
            UUID uuid = player.getUUID();

            if ("BUY".equalsIgnoreCase(payload.action()) || "CLAIM".equalsIgnoreCase(payload.action())) {
                if (AmmoraMod.getMarketDAO().isCourierUnlocked(uuid, type.getId())) {
                    sendMarketplaceData(player, AmmoraLang.notify("courier.already_unlocked"), true);
                    return;
                }

                com.ammora.mod.db.MarketDAO.CourierProgressStats stats = AmmoraMod.getMarketDAO().getCourierProgressStats(uuid);
                boolean isClaimable = type.isClaimable(stats);

                if (isClaimable) {
                    // Free achievement unlock
                    AmmoraMod.getMarketDAO().unlockCourier(uuid, type.getId(), System.currentTimeMillis());
                    AmmoraMod.getMarketDAO().setActiveCourier(uuid, type.getId());

                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.0F);
                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
                    sendMarketplaceData(player, AmmoraLang.notify("courier.achievement_unlocked", AmmoraLang.guiStr(type.getNameKey())), false);
                } else {
                    // Paid buyout with CBX
                    double price = type.getPriceCbx();
                    if (price > 0.0) {
                        PlayerAccount acc = AmmoraMod.getMarketDAO().getAccount(uuid, player.getName().getString());
                        if (acc == null || acc.getBalanceCbx() < price) {
                            sendMarketplaceData(player, AmmoraLang.notify("courier.insufficient_funds_buyout", MarketEngine.round2(price)), true);
                            return;
                        }
                        acc.withdraw(price);
                        AmmoraMod.getMarketDAO().saveAccount(acc);
                        AmmoraMod.getMarketDAO().recordMarketTransaction(new MarketTxRecord(
                                UUID.randomUUID().toString(),
                                "COURIER_BUYOUT",
                                "",
                                uuid,
                                player.getName().getString(),
                                new UUID(0L, 0L),
                                "SYSTEM",
                                "ammora:courier_" + type.getId().toLowerCase(Locale.ROOT),
                                "Courier: " + type.getId(),
                                1,
                                price,
                                0.0,
                                System.currentTimeMillis()
                        ));
                    }

                    AmmoraMod.getMarketDAO().unlockCourier(uuid, type.getId(), System.currentTimeMillis());
                    AmmoraMod.getMarketDAO().setActiveCourier(uuid, type.getId());

                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.5F);
                    sendMarketplaceData(player, AmmoraLang.notify("courier.buyout_success", AmmoraLang.guiStr(type.getNameKey()), MarketEngine.round2(type.getPriceCbx())), false);
                }
            } else if ("SELECT".equalsIgnoreCase(payload.action())) {
                if (!AmmoraMod.getMarketDAO().isCourierUnlocked(uuid, type.getId())) {
                    sendMarketplaceData(player, AmmoraLang.notify("courier.not_unlocked"), true);
                    return;
                }

                AmmoraMod.getMarketDAO().setActiveCourier(uuid, type.getId());
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendMarketplaceData(player, AmmoraLang.notify("courier.selected", AmmoraLang.guiStr(type.getNameKey())), false);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to handle courier skin action for " + player.getName().getString(), e);
        }
    }
}
