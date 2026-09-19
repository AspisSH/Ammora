package com.ammora.mod.core;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.db.LoanRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.network.ClientboundTradeSyncPayload;
import com.ammora.mod.network.PacketHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Manages an active 2-player P2P secure trade session.
 * Features:
 * - 18 slots per player (3x6 grid matching mockup)
 * - Money offer in CBX
 * - Anti-scam state reset: modifying any item or money immediately unlocks and unconfirms both players
 * - 2-step confirmation: Lock -> Confirm
 * - Inventory capacity validation before executing
 * - Safe return of all items on cancellation or disconnect
 */
public class TradeSession {

    public static final int SLOTS_COUNT = 18;

    private final UUID sessionId;
    private final UUID playerAUuid;
    private final String playerAName;
    private final UUID playerBUuid;
    private final String playerBName;

    private double moneyA = 0.0;
    private double moneyB = 0.0;

    private final ItemStack[] itemsA = new ItemStack[SLOTS_COUNT];
    private final ItemStack[] itemsB = new ItemStack[SLOTS_COUNT];

    private boolean lockedA = false;
    private boolean lockedB = false;
    private boolean confirmedA = false;
    private boolean confirmedB = false;

    private boolean finished = false;

    // P2P Loan mode state
    private boolean isLoanMode = false;
    private boolean isPlayerALender = true;
    private double interestRate = 15.0;
    private int durationHours = 24;

    public synchronized void setTradeMode(ServerPlayer player, boolean loanMode) {
        if (finished) return;
        if (this.isLoanMode != loanMode) {
            this.isLoanMode = loanMode;
            onOfferModified(loanMode ? "key:trade.status_mode_loan" : "key:trade.status_mode_trade");
        }
    }

    public synchronized void setLoanRole(ServerPlayer player, boolean becomeLender) {
        if (finished) return;
        boolean isA = player.getUUID().equals(playerAUuid);
        boolean newALender = isA ? becomeLender : !becomeLender;
        if (this.isPlayerALender != newALender) {
            this.isPlayerALender = newALender;
            String lenderName = newALender ? playerAName : playerBName;
            onOfferModified("key:trade.status_role_changed;" + lenderName);
        }
    }

    public synchronized void setInterestRate(ServerPlayer player, double rate) {
        if (finished) return;
        double rounded = Math.max(0.0, Math.min(1000.0, MarketEngine.round2(rate)));
        if (Math.abs(this.interestRate - rounded) > 0.01) {
            this.interestRate = rounded;
            onOfferModified("key:trade.status_rate_changed;" + rounded);
        }
    }

    public synchronized void setLoanDuration(ServerPlayer player, int hours) {
        if (finished) return;
        int clamped = Math.max(1, Math.min(720, hours));
        if (this.durationHours != clamped) {
            this.durationHours = clamped;
            onOfferModified("key:trade.status_duration_changed;" + clamped);
        }
    }

    public TradeSession(UUID sessionId, ServerPlayer a, ServerPlayer b) {
        this.sessionId = sessionId;
        this.playerAUuid = a.getUUID();
        this.playerAName = a.getName().getString();
        this.playerBUuid = b.getUUID();
        this.playerBName = b.getName().getString();

        Arrays.fill(itemsA, ItemStack.EMPTY);
        Arrays.fill(itemsB, ItemStack.EMPTY);
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getPlayerAUuid() { return playerAUuid; }
    public UUID getPlayerBUuid() { return playerBUuid; }
    public boolean isFinished() { return finished; }

    public synchronized void setMoney(ServerPlayer player, double amount) {
        if (finished) return;
        double rounded = Math.max(0.0, MarketEngine.round2(amount));
        if (player.getUUID().equals(playerAUuid)) {
            if (Math.abs(this.moneyA - rounded) > 0.001) {
                this.moneyA = rounded;
                onOfferModified("key:trade.status_cbx_offer;" + playerAName);
            }
        } else if (player.getUUID().equals(playerBUuid)) {
            if (Math.abs(this.moneyB - rounded) > 0.001) {
                this.moneyB = rounded;
                onOfferModified("key:trade.status_cbx_offer;" + playerBName);
            }
        }
    }

    public synchronized void offerItem(ServerPlayer player, int invSlot) {
        offerItem(player, invSlot, 0);
    }

    public synchronized void offerItem(ServerPlayer player, int invSlot, int count) {
        if (finished) return;
        if (invSlot < 0 || invSlot >= player.getInventory().items.size()) return;
        ItemStack invStack = player.getInventory().getItem(invSlot);
        if (invStack.isEmpty()) return;

        ItemStack[] targetSlots = player.getUUID().equals(playerAUuid) ? itemsA : itemsB;
        int toTransfer = count > 0 ? Math.min(count, invStack.getCount()) : invStack.getCount();
        if (toTransfer <= 0) return;

        if (isLoanMode) {
            boolean isLender = player.getUUID().equals(playerAUuid) ? isPlayerALender : !isPlayerALender;
            if (isLender) {
                return;
            }
            if (!targetSlots[0].isEmpty()) {
                sendSync(player, "key:trade.status_grid_full", true);
                return;
            }
            targetSlots[0] = invStack.split(toTransfer);
            if (invStack.isEmpty()) {
                player.getInventory().setItem(invSlot, ItemStack.EMPTY);
            }
            String playerName = player.getUUID().equals(playerAUuid) ? playerAName : playerBName;
            onOfferModified("key:trade.status_item_added;" + playerName);
            return;
        }

        int remaining = toTransfer;

        // 1. Try to merge into existing matching slots in the trade grid first
        for (int i = 0; i < SLOTS_COUNT && remaining > 0; i++) {
            ItemStack existing = targetSlots[i];
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, invStack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int add = Math.min(space, remaining);
                    existing.grow(add);
                    invStack.shrink(add);
                    remaining -= add;
                }
            }
        }

        // 2. If remaining items need a slot, put into the first empty slot
        if (remaining > 0) {
            int emptyIdx = -1;
            for (int i = 0; i < SLOTS_COUNT; i++) {
                if (targetSlots[i].isEmpty()) {
                    emptyIdx = i;
                    break;
                }
            }

            if (emptyIdx == -1) {
                if (remaining == toTransfer) {
                    sendSync(player, "key:trade.status_grid_full", true);
                    return;
                }
            } else {
                targetSlots[emptyIdx] = invStack.split(remaining);
                remaining = 0;
            }
        }

        if (invStack.isEmpty()) {
            player.getInventory().setItem(invSlot, ItemStack.EMPTY);
        }

        String playerName = player.getUUID().equals(playerAUuid) ? playerAName : playerBName;
        onOfferModified("key:trade.status_item_added;" + playerName);
    }

    public synchronized void removeItem(ServerPlayer player, int tradeSlot) {
        removeItem(player, tradeSlot, 0);
    }

    public synchronized void removeItem(ServerPlayer player, int tradeSlot, int count) {
        if (finished) return;
        if (tradeSlot < 0 || tradeSlot >= SLOTS_COUNT) return;

        ItemStack[] targetSlots = player.getUUID().equals(playerAUuid) ? itemsA : itemsB;
        ItemStack stack = targetSlots[tradeSlot];
        if (stack.isEmpty()) return;

        int toRemove = count > 0 ? Math.min(count, stack.getCount()) : stack.getCount();
        ItemStack returnStack = stack.copyWithCount(toRemove);

        if (!player.getInventory().add(returnStack)) {
            sendSync(player, "key:trade.status_inv_full", true);
            return;
        }

        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            targetSlots[tradeSlot] = ItemStack.EMPTY;
        }

        String playerName = player.getUUID().equals(playerAUuid) ? playerAName : playerBName;
        onOfferModified("key:trade.status_item_removed;" + playerName);
    }

    public synchronized void toggleLock(ServerPlayer player) {
        if (finished) return;
        if (player.getUUID().equals(playerAUuid)) {
            lockedA = !lockedA;
        } else if (player.getUUID().equals(playerBUuid)) {
            lockedB = !lockedB;
        }
        confirmedA = false;
        confirmedB = false;

        playUiSound(player.server, SoundEvents.UI_BUTTON_CLICK, 1.2F);
        syncBoth("", false);
    }

    public synchronized void confirmTrade(ServerPlayer player) {
        if (finished) return;
        if (!lockedA || !lockedB) {
            sendSync(player, "key:trade.status_must_lock", true);
            return;
        }

        if (player.getUUID().equals(playerAUuid)) {
            confirmedA = true;
        } else if (player.getUUID().equals(playerBUuid)) {
            confirmedB = true;
        }

        syncBoth("", false);

        if (confirmedA && confirmedB) {
            executeTrade(player.server);
        }
    }

    private void onOfferModified(String reason) {
        lockedA = false;
        lockedB = false;
        confirmedA = false;
        confirmedB = false;
        syncBoth(reason, false);
    }

    private void executeTrade(MinecraftServer server) {
        ServerPlayer pA = server.getPlayerList().getPlayer(playerAUuid);
        ServerPlayer pB = server.getPlayerList().getPlayer(playerBUuid);

        if (pA == null || pB == null) {
            cancel(server, "key:trade.cancel_player_disconnected");
            return;
        }

        // Validate proximity (max 16 blocks)
        if (pA.distanceToSqr(pB) > 256.0) {
            cancel(server, "key:trade.cancel_too_far");
            return;
        }

        if (isLoanMode) {
            executeLoan(server, pA, pB);
            return;
        }

        // Validate money balances
        try {
            PlayerAccount accA = AmmoraMod.getMarketDAO().getAccount(playerAUuid, playerAName);
            PlayerAccount accB = AmmoraMod.getMarketDAO().getAccount(playerBUuid, playerBName);

            if (moneyA > accA.getBalanceCbx()) {
                confirmedA = confirmedB = false;
                syncBoth("key:trade.status_insufficient_cbx;" + playerAName + ";" + moneyA + " CBX", true);
                return;
            }
            if (moneyB > accB.getBalanceCbx()) {
                confirmedA = confirmedB = false;
                syncBoth("key:trade.status_insufficient_cbx;" + playerBName + ";" + moneyB + " CBX", true);
                return;
            }

            // Validate inventory capacity
            if (!canHoldAll(pA, itemsB)) {
                confirmedA = confirmedB = false;
                syncBoth("key:trade.status_partner_inv_full;" + playerAName, true);
                return;
            }
            if (!canHoldAll(pB, itemsA)) {
                confirmedA = confirmedB = false;
                syncBoth("key:trade.status_partner_inv_full;" + playerBName, true);
                return;
            }

            // --- EXECUTE SWAP ATOMICALLY ---
            finished = true;

            // 1. Swap money
            if (moneyA > 0) {
                accA.withdraw(moneyA);
                accB.deposit(moneyA);
                AmmoraMod.getMarketDAO().saveAccount(accA);
                AmmoraMod.getMarketDAO().saveAccount(accB);
                AmmoraMod.getMarketDAO().recordP2PTransfer(new P2PTransfer(
                        UUID.randomUUID().toString(), playerAUuid, playerAName, playerBUuid, playerBName, moneyA, System.currentTimeMillis()
                ));
            }
            if (moneyB > 0) {
                accB.withdraw(moneyB);
                accA.deposit(moneyB);
                AmmoraMod.getMarketDAO().saveAccount(accB);
                AmmoraMod.getMarketDAO().saveAccount(accA);
                AmmoraMod.getMarketDAO().recordP2PTransfer(new P2PTransfer(
                        UUID.randomUUID().toString(), playerBUuid, playerBName, playerAUuid, playerAName, moneyB, System.currentTimeMillis()
                ));
            }

            // 2. Swap items
            for (ItemStack sB : itemsB) {
                if (!sB.isEmpty()) {
                    pA.getInventory().add(sB);
                }
            }
            for (ItemStack sA : itemsA) {
                if (!sA.isEmpty()) {
                    pB.getInventory().add(sA);
                }
            }
            Arrays.fill(itemsA, ItemStack.EMPTY);
            Arrays.fill(itemsB, ItemStack.EMPTY);

            // 3. Audio and notifications
            pA.level().playSound(null, pA.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            pB.level().playSound(null, pB.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);

            pA.sendSystemMessage(Component.translatable("gui.ammora.trade.chat_success", playerBName));
            pB.sendSystemMessage(Component.translatable("gui.ammora.trade.chat_success", playerAName));

            // Close client screens
            PacketDistributor.sendToPlayer(pA, createPayload(true, "key:trade.status_success", false, false));
            PacketDistributor.sendToPlayer(pB, createPayload(false, "key:trade.status_success", false, false));

        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger("Ammora").error("Failed to execute P2P trade session", e);
            cancel(server, "Database error: " + e.getMessage());
        }
    }

    private void executeLoan(MinecraftServer server, ServerPlayer pA, ServerPlayer pB) {
        ServerPlayer lender = isPlayerALender ? pA : pB;
        ServerPlayer borrower = isPlayerALender ? pB : pA;
        double principal = isPlayerALender ? moneyA : moneyB;
        ItemStack[] borrowerItems = isPlayerALender ? itemsB : itemsA;

        if (principal <= 0.0) {
            confirmedA = confirmedB = false;
            syncBoth("key:trade.status_loan_no_money", true);
            return;
        }

        // Find collateral item in borrower's offered items
        ItemStack collateral = ItemStack.EMPTY;
        int collateralSlot = -1;
        for (int i = 0; i < SLOTS_COUNT; i++) {
            if (!borrowerItems[i].isEmpty()) {
                collateral = borrowerItems[i];
                collateralSlot = i;
                break;
            }
        }

        if (collateral.isEmpty()) {
            confirmedA = confirmedB = false;
            syncBoth("key:trade.status_loan_no_collateral", true);
            return;
        }

        try {
            PlayerAccount lenderAcc = AmmoraMod.getMarketDAO().getAccount(lender.getUUID(), lender.getName().getString());
            if (principal > lenderAcc.getBalanceCbx()) {
                confirmedA = confirmedB = false;
                syncBoth("key:trade.status_insufficient_cbx;" + lender.getName().getString() + ";" + principal + " CBX", true);
                return;
            }

            double totalRepay = MarketEngine.round2(principal * (1.0 + (interestRate / 100.0)));
            long now = System.currentTimeMillis();
            long expiresAt = now + ((long) durationHours * 3600000L);

            String itemNbt = "";
            try {
                net.minecraft.nbt.Tag t = collateral.saveOptional(server.registryAccess());
                if (t != null) itemNbt = t.getAsString();
            } catch (Exception ignored) {}

            String itemId = BuiltInRegistries.ITEM.getKey(collateral.getItem()).toString();
            String displayName = collateral.getHoverName().getString();
            int count = collateral.getCount();

            String loanId = UUID.randomUUID().toString();
            LoanRecord loan = new LoanRecord(
                    loanId,
                    lender.getUUID(),
                    lender.getName().getString(),
                    borrower.getUUID(),
                    borrower.getName().getString(),
                    principal,
                    interestRate,
                    totalRepay,
                    itemId,
                    itemNbt,
                    displayName,
                    count,
                    now,
                    expiresAt,
                    "ACTIVE"
            );

            // Execute loan transfer atomically
            finished = true;

            // 1. Transfer principal money from lender to borrower
            lenderAcc.withdraw(principal);
            PlayerAccount borrowerAcc = AmmoraMod.getMarketDAO().getAccount(borrower.getUUID(), borrower.getName().getString());
            borrowerAcc.deposit(principal);
            AmmoraMod.getMarketDAO().saveAccount(lenderAcc);
            AmmoraMod.getMarketDAO().saveAccount(borrowerAcc);
            AmmoraMod.getMarketDAO().recordP2PTransfer(new P2PTransfer(
                    UUID.randomUUID().toString(), lender.getUUID(), lender.getName().getString(),
                    borrower.getUUID(), borrower.getName().getString(), principal, now
            ));

            // 2. Persist loan to SQLite
            AmmoraMod.getMarketDAO().saveLoan(loan);

            // 3. Clear collateral item so it stays in escrow and isn't returned
            borrowerItems[collateralSlot] = ItemStack.EMPTY;

            // 4. Return any unused offered items back to respective players
            returnOrBufferItems(server, playerAUuid, pA, itemsA);
            returnOrBufferItems(server, playerBUuid, pB, itemsB);
            Arrays.fill(itemsA, ItemStack.EMPTY);
            Arrays.fill(itemsB, ItemStack.EMPTY);

            // 5. Sound & notifications
            pA.level().playSound(null, pA.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            pB.level().playSound(null, pB.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);

            String formattedPrincipal = String.format(java.util.Locale.US, "%.1f CBX", principal);
            String formattedTotal = String.format(java.util.Locale.US, "%.1f CBX", totalRepay);
            lender.sendSystemMessage(Component.translatable("gui.ammora.trade.loan_success_lender", formattedPrincipal, borrower.getName().getString(), formattedTotal));
            borrower.sendSystemMessage(Component.translatable("gui.ammora.trade.loan_success_borrower", formattedPrincipal, lender.getName().getString(), formattedTotal));

            // Close client screens
            PacketDistributor.sendToPlayer(pA, createPayload(true, "key:trade.status_loan_created", false, false));
            PacketDistributor.sendToPlayer(pB, createPayload(false, "key:trade.status_loan_created", false, false));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("Ammora").error("Failed to execute loan", e);
            cancel(server, "key:trade.status_loan_failed");
        }
    }

    private void returnOrBufferItems(MinecraftServer server, UUID playerUuid, ServerPlayer player, ItemStack[] items) {
        for (ItemStack s : items) {
            if (s != null && !s.isEmpty()) {
                if (player != null) {
                    if (!player.getInventory().add(s)) {
                        player.drop(s, false);
                    }
                } else {
                    // Player is offline/disconnected: save to unclaimed delivery buffer so items are never lost!
                    try {
                        String nbt = "";
                        try {
                            net.minecraft.nbt.Tag t = s.saveOptional(server.registryAccess());
                            if (t != null) nbt = t.getAsString();
                        } catch (Exception ignored) {}
                        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                        if (AmmoraMod.getMarketDAO() != null) {
                            AmmoraMod.getMarketDAO().saveUnclaimedDelivery(
                                    UUID.randomUUID().toString(), playerUuid, itemId, s.getCount(), System.currentTimeMillis(), nbt
                            );
                        }
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger("Ammora").error("Failed to buffer offline trade items for " + playerUuid, e);
                    }
                }
            }
        }
    }

    public synchronized void cancel(MinecraftServer server, String reason) {
        if (finished) return;
        finished = true;

        ServerPlayer pA = server.getPlayerList().getPlayer(playerAUuid);
        ServerPlayer pB = server.getPlayerList().getPlayer(playerBUuid);

        // Safely return or buffer items for Player A
        returnOrBufferItems(server, playerAUuid, pA, itemsA);
        if (pA != null) {
            pA.sendSystemMessage(createCancelMessage(reason));
            PacketDistributor.sendToPlayer(pA, createPayload(true, "key:trade.status_cancelled", true, false));
        }

        // Safely return or buffer items for Player B
        returnOrBufferItems(server, playerBUuid, pB, itemsB);
        if (pB != null) {
            pB.sendSystemMessage(createCancelMessage(reason));
            PacketDistributor.sendToPlayer(pB, createPayload(false, "key:trade.status_cancelled", true, false));
        }

        Arrays.fill(itemsA, ItemStack.EMPTY);
        Arrays.fill(itemsB, ItemStack.EMPTY);
    }

    private boolean canHoldAll(ServerPlayer player, ItemStack[] incoming) {
        int freeSlots = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty()) freeSlots++;
        }
        int incomingCount = 0;
        for (ItemStack in : incoming) {
            if (!in.isEmpty()) incomingCount++;
        }
        return freeSlots >= incomingCount;
    }

    public void syncBoth(String message, boolean isError) {
        net.minecraft.server.MinecraftServer s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;
        ServerPlayer pA = s.getPlayerList().getPlayer(playerAUuid);
        ServerPlayer pB = s.getPlayerList().getPlayer(playerBUuid);

        if (pA != null) {
            PacketDistributor.sendToPlayer(pA, createPayload(true, message, isError, true));
        }
        if (pB != null) {
            PacketDistributor.sendToPlayer(pB, createPayload(false, message, isError, true));
        }
    }

    private void sendSync(ServerPlayer player, String message, boolean isError) {
        boolean isA = player.getUUID().equals(playerAUuid);
        PacketDistributor.sendToPlayer(player, createPayload(isA, message, isError, true));
    }

    private ClientboundTradeSyncPayload createPayload(boolean forPlayerA, String msg, boolean isErr, boolean active) {
        List<ItemStack> myItems = new ArrayList<>();
        List<ItemStack> partnerItems = new ArrayList<>();

        ItemStack[] myArr = forPlayerA ? itemsA : itemsB;
        ItemStack[] partArr = forPlayerA ? itemsB : itemsA;

        for (int i = 0; i < SLOTS_COUNT; i++) {
            myItems.add(myArr[i] != null ? myArr[i].copy() : ItemStack.EMPTY);
            partnerItems.add(partArr[i] != null ? partArr[i].copy() : ItemStack.EMPTY);
        }

        double principal = isPlayerALender ? moneyA : moneyB;
        double totalRepay = MarketEngine.round2(principal * (1.0 + (interestRate / 100.0)));

        return new ClientboundTradeSyncPayload(
                active,
                forPlayerA,
                forPlayerA ? playerAName : playerBName,
                forPlayerA ? playerBName : playerAName,
                forPlayerA ? moneyA : moneyB,
                forPlayerA ? moneyB : moneyA,
                myItems,
                partnerItems,
                forPlayerA ? lockedA : lockedB,
                forPlayerA ? lockedB : lockedA,
                forPlayerA ? confirmedA : confirmedB,
                forPlayerA ? confirmedB : confirmedA,
                msg,
                isErr,
                isLoanMode,
                isPlayerALender,
                interestRate,
                durationHours,
                totalRepay
        );
    }

    private void playUiSound(MinecraftServer server, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float pitch) {
        ServerPlayer pA = server.getPlayerList().getPlayer(playerAUuid);
        ServerPlayer pB = server.getPlayerList().getPlayer(playerBUuid);
        if (pA != null) pA.level().playSound(null, pA.blockPosition(), sound.value(), SoundSource.PLAYERS, 0.6F, pitch);
        if (pB != null) pB.level().playSound(null, pB.blockPosition(), sound.value(), SoundSource.PLAYERS, 0.6F, pitch);
    }

    private Component createCancelMessage(String reason) {
        if (reason.startsWith("key:")) {
            String key = "gui.ammora." + reason.substring(4);
            return Component.translatable("gui.ammora.trade.chat_cancelled", Component.translatable(key));
        }
        return Component.translatable("gui.ammora.trade.chat_cancelled", reason);
    }
}
