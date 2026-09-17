package com.ammora.mod.core;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.network.ClientboundTradeSyncPayload;
import com.ammora.mod.network.PacketHandler;
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

    public synchronized void cancel(MinecraftServer server, String reason) {
        if (finished) return;
        finished = true;

        ServerPlayer pA = server.getPlayerList().getPlayer(playerAUuid);
        ServerPlayer pB = server.getPlayerList().getPlayer(playerBUuid);

        // Safely return items to Player A
        if (pA != null) {
            for (ItemStack s : itemsA) {
                if (!s.isEmpty()) {
                    if (!pA.getInventory().add(s)) {
                        pA.drop(s, false);
                    }
                }
            }
            pA.sendSystemMessage(createCancelMessage(reason));
            PacketDistributor.sendToPlayer(pA, createPayload(true, "key:trade.status_cancelled", true, false));
        }

        // Safely return items to Player B
        if (pB != null) {
            for (ItemStack s : itemsB) {
                if (!s.isEmpty()) {
                    if (!pB.getInventory().add(s)) {
                        pB.drop(s, false);
                    }
                }
            }
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
                isErr
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
