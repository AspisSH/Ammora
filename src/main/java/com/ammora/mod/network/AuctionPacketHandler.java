package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.db.AuctionRecord;
import com.ammora.mod.db.MarketTxRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.entity.CourierBeeEntity;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Handles incoming client network actions for Live Auctions:
 * Creating listings, placing bids with escrow, instant buyout, and lot cancellation.
 */
public final class AuctionPacketHandler {

    private AuctionPacketHandler() {}

    public static void handleAuctionAction(ServerPlayer player, ServerboundAuctionActionPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;

        try {
            switch (payload.action()) {
                case "CREATE" -> handleCreate(player, payload);
                case "BID" -> handleBid(player, payload);
                case "BUYOUT" -> handleBuyout(player, payload);
                case "CANCEL" -> handleCancel(player, payload);
                default -> EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_unknown_action"), true);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to execute auction action: " + payload.action(), e);
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_action_error"), true);
        }
    }

    private static void handleCreate(ServerPlayer player, ServerboundAuctionActionPayload payload) throws Exception {
        int slot = payload.slotIndex();
        if (slot < 0 || slot >= player.getInventory().items.size()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_invalid_slot"), true);
            return;
        }

        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_empty_item"), true);
            return;
        }

        double startPrice = Math.max(1.0, Math.round(payload.startPrice() * 100.0) / 100.0);
        double minStep = Math.max(0.5, Math.round(payload.minBidStep() * 100.0) / 100.0);
        double buyoutPrice = payload.buyoutPrice() > 0.0 ? Math.round(payload.buyoutPrice() * 100.0) / 100.0 : 0.0;

        if (buyoutPrice > 0.0 && buyoutPrice <= startPrice) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_buyout_too_low"), true);
            return;
        }

        int durationMins = Math.clamp(payload.durationMinutes(), 10, 2880); // 10 min to 48 hours
        long now = System.currentTimeMillis();
        long expiresAt = now + (durationMins * 60_000L);

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String nbt = "";
        try {
            Tag t = stack.saveOptional(player.serverLevel().registryAccess());
            if (t != null) nbt = t.getAsString();
        } catch (Exception ignored) {}

        String displayName = stack.getHoverName().getString();
        int count = stack.getCount();

        // Extract item from player inventory safely
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.containerMenu.broadcastChanges();

        AuctionRecord auction = new AuctionRecord(
                UUID.randomUUID().toString(),
                player.getUUID(),
                player.getName().getString(),
                itemId,
                nbt,
                displayName,
                count,
                startPrice,
                0.0,
                minStep,
                buyoutPrice,
                null,
                "",
                now,
                expiresAt,
                "ACTIVE"
        );

        AmmoraMod.getMarketDAO().saveOrUpdateAuction(auction);

        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_created", displayName, String.valueOf(count)), false);
    }

    private static void handleBid(ServerPlayer player, ServerboundAuctionActionPayload payload) throws Exception {
        AuctionRecord auction = AmmoraMod.getMarketDAO().getAuction(payload.auctionId());
        if (auction == null || !"ACTIVE".equals(auction.getStatus()) || System.currentTimeMillis() >= auction.getExpiresAt()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_not_active"), true);
            return;
        }

        if (auction.getSellerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_cannot_bid_own"), true);
            return;
        }

        double bidAmount = Math.round(payload.bidAmount() * 100.0) / 100.0;
        double minRequired = auction.getNextMinBid();
        if (bidAmount < minRequired) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_bid_too_low", MarketEngine.round2(minRequired)), true);
            return;
        }

        com.ammora.mod.db.CompanyRecord bidderCompany = null;
        com.ammora.mod.db.CompanyMemberRecord bidderMember = null;
        PlayerAccount bidderAcc = null;

        if (payload.fromCompanyAccount()) {
            bidderCompany = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
            if (bidderCompany == null) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_not_in_company"), true);
                return;
            }
            bidderMember = AmmoraMod.getMarketDAO().getCompanyMember(bidderCompany.getCompanyId(), player.getUUID());
            if (bidderMember == null || bidderMember.isMember()) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_withdraw_unauthorized"), true);
                return;
            }
            if (!bidderMember.canSpend(bidAmount)) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_daily_limit_exceeded"), true);
                return;
            }
            if (bidderCompany.getBalanceCbx() < bidAmount) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_treasury_insufficient"), true);
                return;
            }
            bidderCompany.withdraw(bidAmount);
            bidderMember.recordSpend(bidAmount);
            AmmoraMod.getMarketDAO().updateCompanyBalance(bidderCompany.getCompanyId(), bidderCompany.getBalanceCbx());
            AmmoraMod.getMarketDAO().saveCompanyMember(bidderMember);
            AmmoraMod.getMarketDAO().recordCompanyLedger(bidderCompany.getCompanyId(), player.getUUID(), player.getName().getString(),
                    "AUCTION_BID", bidAmount, "Placed bid on lot " + auction.getDisplayName());
        } else {
            bidderAcc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            if (bidderAcc == null || bidderAcc.getBalanceCbx() < bidAmount) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_insufficient_funds", MarketEngine.round2(bidAmount)), true);
                return;
            }
            // Deduct escrow from bidder
            bidderAcc.withdraw(bidAmount);
            AmmoraMod.getMarketDAO().saveAccount(bidderAcc);
        }

        // Refund previous bidder if one existed
        if (auction.getHighestBidderUuid() != null && auction.getCurrentBid() > 0.0) {
            UUID prevUuid = auction.getHighestBidderUuid();
            double refund = auction.getCurrentBid();
            PlayerAccount prevAcc = AmmoraMod.getMarketDAO().getAccount(prevUuid, auction.getHighestBidderName());
            if (prevAcc != null) {
                prevAcc.deposit(refund);
                AmmoraMod.getMarketDAO().saveAccount(prevAcc);
            }
            // Notify previous bidder if online
            ServerPlayer prevOnline = player.getServer().getPlayerList().getPlayer(prevUuid);
            if (prevOnline != null) {
                prevOnline.sendSystemMessage(AmmoraLang.message("auction.outbid", auction.getDisplayName(), MarketEngine.round2(refund)));
                prevOnline.level().playSound(null, prevOnline.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        }

        // Place bid & apply anti-sniping
        auction.placeBid(player.getUUID(), player.getName().getString(), bidAmount);
        AmmoraMod.getMarketDAO().saveOrUpdateAuction(auction);

        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_bid_success", MarketEngine.round2(bidAmount)), false);
    }

    private static void handleBuyout(ServerPlayer player, ServerboundAuctionActionPayload payload) throws Exception {
        AuctionRecord auction = AmmoraMod.getMarketDAO().getAuction(payload.auctionId());
        if (auction == null || !"ACTIVE".equals(auction.getStatus()) || System.currentTimeMillis() >= auction.getExpiresAt()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_not_active"), true);
            return;
        }

        if (!auction.hasBuyout()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_no_buyout"), true);
            return;
        }

        if (auction.getSellerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_cannot_buyout_own"), true);
            return;
        }

        double buyout = auction.getBuyoutPrice();
        com.ammora.mod.db.CompanyRecord buyerCompany = null;
        com.ammora.mod.db.CompanyMemberRecord buyerMember = null;
        PlayerAccount buyerAcc = null;

        if (payload.fromCompanyAccount()) {
            buyerCompany = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
            if (buyerCompany == null) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_not_in_company"), true);
                return;
            }
            buyerMember = AmmoraMod.getMarketDAO().getCompanyMember(buyerCompany.getCompanyId(), player.getUUID());
            if (buyerMember == null || buyerMember.isMember()) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_withdraw_unauthorized"), true);
                return;
            }
            if (!buyerMember.canSpend(buyout)) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_daily_limit_exceeded"), true);
                return;
            }
            if (buyerCompany.getBalanceCbx() < buyout) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_treasury_insufficient"), true);
                return;
            }
            buyerCompany.withdraw(buyout);
            buyerMember.recordSpend(buyout);
            AmmoraMod.getMarketDAO().updateCompanyBalance(buyerCompany.getCompanyId(), buyerCompany.getBalanceCbx());
            AmmoraMod.getMarketDAO().saveCompanyMember(buyerMember);
            AmmoraMod.getMarketDAO().recordCompanyLedger(buyerCompany.getCompanyId(), player.getUUID(), player.getName().getString(),
                    "AUCTION_BUY", buyout, "Instant buyout for lot " + auction.getDisplayName());
        } else {
            buyerAcc = AmmoraMod.getMarketDAO().getAccount(player.getUUID(), player.getName().getString());
            if (buyerAcc == null || buyerAcc.getBalanceCbx() < buyout) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_insufficient_funds", MarketEngine.round2(buyout)), true);
                return;
            }
            // Deduct buyout from buyer
            buyerAcc.withdraw(buyout);
            AmmoraMod.getMarketDAO().saveAccount(buyerAcc);
        }

        // Refund previous bidder if one existed
        if (auction.getHighestBidderUuid() != null && auction.getCurrentBid() > 0.0) {
            UUID prevUuid = auction.getHighestBidderUuid();
            double refund = auction.getCurrentBid();
            PlayerAccount prevAcc = AmmoraMod.getMarketDAO().getAccount(prevUuid, auction.getHighestBidderName());
            if (prevAcc != null) {
                prevAcc.deposit(refund);
                AmmoraMod.getMarketDAO().saveAccount(prevAcc);
            }
            ServerPlayer prevOnline = player.getServer().getPlayerList().getPlayer(prevUuid);
            if (prevOnline != null) {
                prevOnline.sendSystemMessage(AmmoraLang.message("auction.outbid", auction.getDisplayName(), MarketEngine.round2(refund)));
            }
        }

        // Pay seller (minus 2% market fee)
        double fee = Math.max(1.0, Math.round(buyout * 0.02 * 100.0) / 100.0);
        double sellerPayout = buyout - fee;
        PlayerAccount sellerAcc = AmmoraMod.getMarketDAO().getAccount(auction.getSellerUuid(), auction.getSellerName());
        if (sellerAcc != null) {
            sellerAcc.deposit(sellerPayout);
            AmmoraMod.getMarketDAO().saveAccount(sellerAcc);
        }
        ServerPlayer sellerOnline = player.getServer().getPlayerList().getPlayer(auction.getSellerUuid());
        if (sellerOnline != null) {
            sellerOnline.sendSystemMessage(AmmoraLang.message("auction.sold_buyout", auction.getDisplayName(), MarketEngine.round2(sellerPayout)));
            sellerOnline.level().playSound(null, sellerOnline.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        }

        // Complete auction
        auction.complete();
        AmmoraMod.getMarketDAO().saveOrUpdateAuction(auction);

        // Record transaction
        AmmoraMod.getMarketDAO().recordMarketTransaction(new MarketTxRecord(
                UUID.randomUUID().toString(),
                "AUCTION_BUYOUT",
                auction.getAuctionId(),
                player.getUUID(), player.getName().getString(),
                auction.getSellerUuid(), auction.getSellerName(),
                auction.getItemId(), auction.getDisplayName(),
                auction.getItemCount(), buyout, fee, System.currentTimeMillis()
        ));

        // Deliver item to buyer via Courier Bee
        ItemStack deliveryStack = parseAuctionStack(auction, player);
        CourierBeeEntity.dispatchToPlayer(player, deliveryStack);

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_buyout_success", auction.getDisplayName(), MarketEngine.round2(buyout)), false);
    }

    private static void handleCancel(ServerPlayer player, ServerboundAuctionActionPayload payload) throws Exception {
        AuctionRecord auction = AmmoraMod.getMarketDAO().getAuction(payload.auctionId());
        if (auction == null || !"ACTIVE".equals(auction.getStatus())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_not_active"), true);
            return;
        }

        if (!auction.getSellerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_not_owner"), true);
            return;
        }

        if (auction.getHighestBidderUuid() != null && auction.getCurrentBid() > 0.0) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_cannot_cancel_with_bids"), true);
            return;
        }

        auction.cancel();
        AmmoraMod.getMarketDAO().saveOrUpdateAuction(auction);

        // Return item to seller
        ItemStack returnStack = parseAuctionStack(auction, player);
        CourierBeeEntity.dispatchToPlayer(player, returnStack);

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("auction_cancelled"), false);
    }

    public static ItemStack parseAuctionStack(AuctionRecord auction, ServerPlayer player) {
        ItemStack stack = ItemStack.EMPTY;
        if (auction.getItemNbt() != null && !auction.getItemNbt().isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(auction.getItemNbt());
                stack = ItemStack.parseOptional(player.serverLevel().registryAccess(), tag);
            } catch (Exception ignored) {}
        }
        if (stack.isEmpty() && auction.getItemId() != null && !auction.getItemId().isEmpty()) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(auction.getItemId()));
            if (it != null && it != Items.AIR) {
                stack = new ItemStack(it, auction.getItemCount());
            }
        }
        return stack;
    }
}
