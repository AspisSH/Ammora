package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.db.CompanyMemberRecord;
import com.ammora.mod.db.CompanyRecord;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.util.InventoryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import com.ammora.mod.core.P2PTransfer;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles ATM cash withdrawal and deposit network requests on the server side.
 */
public final class AtmPacketHandler {

    private AtmPacketHandler() {}

    /**
     * Sends the current balance and ATM state to the client.
     */
    public static void sendAtmDataToClient(ServerPlayer player, String statusMsg, boolean isError) {
        if (AmmoraMod.getMarketDAO() == null) return;

        try {
            MarketDAO dao = AmmoraMod.getMarketDAO();
            PlayerAccount acc = dao.getAccount(player.getUUID(), player.getName().getString());

            boolean hasComp = false;
            String compId = "";
            String compName = "";
            double compBal = 0.0;
            boolean isCompOwner = false;
            double memberDailyLimit = 0.0;
            double memberSpentToday = 0.0;

            try {
                CompanyRecord comp = dao.getPlayerCompany(player.getUUID());
                if (comp != null) {
                    hasComp = true;
                    compId = comp.getCompanyId();
                    compName = comp.getCompanyName();
                    compBal = comp.getBalanceCbx();
                    isCompOwner = comp.getOwnerUuid().equals(player.getUUID());
                    CompanyMemberRecord member = dao.getCompanyMember(comp.getCompanyId(), player.getUUID());
                    if (member != null) {
                        member.checkAndResetDailyLimit();
                        memberDailyLimit = member.getDailyLimitCbx();
                        memberSpentToday = member.getSpentTodayCbx();
                    }
                }
            } catch (Exception ignored) {}

            ClientboundAtmDataPayload payload = new ClientboundAtmDataPayload(
                    acc.getBalanceCbx(),
                    hasComp,
                    compId,
                    compName,
                    compBal,
                    isCompOwner,
                    memberDailyLimit,
                    memberSpentToday,
                    statusMsg != null ? statusMsg : "",
                    isError
            );

            PacketDistributor.sendToPlayer(player, payload);
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to send ATM data to player {}", player.getName().getString(), e);
        }
    }

    /**
     * Processes ATM actions: withdrawal, deposit all cash, or deposit specific amount.
     */
    public static void handleAtmAction(ServerPlayer player, ServerboundAtmActionPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;
        MarketDAO dao = AmmoraMod.getMarketDAO();

        try {
            switch (payload.action()) {
                case ServerboundAtmActionPayload.ACTION_WITHDRAW -> handleWithdraw(player, dao, payload);
                case ServerboundAtmActionPayload.ACTION_DEPOSIT_ALL -> handleDepositAll(player, dao, payload);
                case ServerboundAtmActionPayload.ACTION_DEPOSIT_AMOUNT -> handleDepositAmount(player, dao, payload);
                default -> sendAtmDataToClient(player, AmmoraLang.notify("atm.invalid_amount"), true);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Error processing ATM action for player {}", player.getName().getString(), e);
        }
    }

    private static void handleWithdraw(ServerPlayer player, MarketDAO dao, ServerboundAtmActionPayload payload) throws Exception {
        double amt = payload.amount();
        if (amt <= 0 || amt % 10 != 0 || Double.isNaN(amt) || Double.isInfinite(amt) || amt > 100_000_000.0) {
            sendAtmDataToClient(player, AmmoraLang.notify("atm.invalid_amount"), true);
            return;
        }

        boolean useCompany = payload.useCompanyAccount();
        CompanyRecord comp = null;
        CompanyMemberRecord member = null;
        PlayerAccount acc = null;

        if (useCompany) {
            comp = dao.getPlayerCompany(player.getUUID());
            if (comp == null || comp.getBalanceCbx() < amt) {
                sendAtmDataToClient(player, AmmoraLang.notify("atm.insufficient_funds"), true);
                return;
            }
            boolean isOwner = comp.getOwnerUuid().equals(player.getUUID());
            if (!isOwner) {
                member = dao.getCompanyMember(comp.getCompanyId(), player.getUUID());
                if (member == null) {
                    sendAtmDataToClient(player, AmmoraLang.notify("atm.insufficient_funds"), true);
                    return;
                }
                member.checkAndResetDailyLimit();
                if (!member.canSpend(amt)) {
                    sendAtmDataToClient(player, AmmoraLang.notify("company.err_daily_limit_exceeded"), true);
                    return;
                }
            }
        } else {
            acc = dao.getAccount(player.getUUID(), player.getName().getString());
            if (acc.getBalanceCbx() < amt) {
                sendAtmDataToClient(player, AmmoraLang.notify("atm.insufficient_funds"), true);
                return;
            }
        }

        String formattedAmt = String.format(Locale.US, "%,.0f", amt);

        // Calculate breakdown into 1000, 100, 10
        int remaining = (int) amt;
        int n1000 = remaining / 1000;
        remaining %= 1000;
        int n100 = remaining / 100;
        remaining %= 100;
        int n10 = remaining / 10;

        // Deduct balance
        if (useCompany) {
            dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx() - amt);
            if (member != null) {
                member.recordSpend(amt);
                dao.saveCompanyMember(member);
            }
            // Audit ledger entry for company expense
            try {
                String desc = AmmoraLang.str("gui.ammora.atm.audit_withdraw", formattedAmt);
                dao.recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(),
                        "ATM_WITHDRAW", amt, desc);
            } catch (Exception ignored) {}
        } else {
            acc.setBalanceCbx(acc.getBalanceCbx() - amt);
            dao.saveAccount(acc);
        }

        // Give banknotes
        if (n1000 > 0) {
            InventoryHelper.giveOrDropItems(player, AmmoraMod.BANKNOTE_1000.get(), n1000);
        }
        if (n100 > 0) {
            InventoryHelper.giveOrDropItems(player, AmmoraMod.BANKNOTE_100.get(), n100);
        }
        if (n10 > 0) {
            InventoryHelper.giveOrDropItems(player, AmmoraMod.BANKNOTE_10.get(), n10);
        }

        // Audio feedback
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.2F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.0F);

        // Record in ledger
        try {
            dao.recordP2PTransfer(new P2PTransfer(
                    UUID.randomUUID().toString(),
                    player.getUUID(),
                    player.getName().getString(),
                    new UUID(0L, 0L),
                    AmmoraLang.str("gui.ammora.atm.withdraw_ledger"),
                    amt,
                    System.currentTimeMillis()
            ));
        } catch (Exception ignored) {}

        player.containerMenu.broadcastChanges();
        sendAtmDataToClient(player, AmmoraLang.notify("atm.withdraw_success", formattedAmt), false);
    }

    private static void handleDepositAll(ServerPlayer player, MarketDAO dao, ServerboundAtmActionPayload payload) throws Exception {
        int count1000 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_1000.get());
        int count100 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_100.get());
        int count10 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_10.get());

        int stack1000 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_STACK_1000.get());
        int stack100 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_STACK_100.get());
        int stack10 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_STACK_10.get());

        int block1000 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_BLOCK_1000_ITEM.get());
        int block100 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_BLOCK_100_ITEM.get());
        int block10 = InventoryHelper.countPlayerItems(player, AmmoraMod.MONEY_BLOCK_10_ITEM.get());

        double totalValue = count1000 * 1000.0 + count100 * 100.0 + count10 * 10.0
                + stack1000 * 9000.0 + stack100 * 900.0 + stack10 * 90.0
                + block1000 * 81000.0 + block100 * 8100.0 + block10 * 810.0;

        if (totalValue <= 0) {
            sendAtmDataToClient(player, AmmoraLang.notify("atm.no_cash_in_inventory"), true);
            return;
        }

        // Remove all cash items
        if (count1000 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_1000.get(), count1000);
        if (count100 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_100.get(), count100);
        if (count10 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_10.get(), count10);

        if (stack1000 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_STACK_1000.get(), stack1000);
        if (stack100 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_STACK_100.get(), stack100);
        if (stack10 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_STACK_10.get(), stack10);

        if (block1000 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_BLOCK_1000_ITEM.get(), block1000);
        if (block100 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_BLOCK_100_ITEM.get(), block100);
        if (block10 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.MONEY_BLOCK_10_ITEM.get(), block10);

        // Credit balance
        boolean useCompany = payload.useCompanyAccount();
        if (useCompany) {
            CompanyRecord comp = dao.getPlayerCompany(player.getUUID());
            if (comp != null) {
                dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx() + totalValue);
                try {
                    String desc = AmmoraLang.str("gui.ammora.atm.audit_deposit", String.format(Locale.US, "%,.0f", totalValue));
                    dao.recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(),
                            "ATM_DEPOSIT", totalValue, desc);
                } catch (Exception ignored) {}
            } else {
                PlayerAccount acc = dao.getAccount(player.getUUID(), player.getName().getString());
                acc.setBalanceCbx(acc.getBalanceCbx() + totalValue);
                dao.saveAccount(acc);
            }
        } else {
            PlayerAccount acc = dao.getAccount(player.getUUID(), player.getName().getString());
            acc.setBalanceCbx(acc.getBalanceCbx() + totalValue);
            dao.saveAccount(acc);
        }

        // Audio feedback
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.4F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);

        // Record in ledger
        try {
            dao.recordP2PTransfer(new P2PTransfer(
                    UUID.randomUUID().toString(),
                    new UUID(0L, 0L),
                    AmmoraLang.str("gui.ammora.atm.deposit_ledger"),
                    player.getUUID(),
                    player.getName().getString(),
                    totalValue,
                    System.currentTimeMillis()
            ));
        } catch (Exception ignored) {}

        String formattedAmt = String.format(Locale.US, "%,.0f", totalValue);
        player.containerMenu.broadcastChanges();
        sendAtmDataToClient(player, AmmoraLang.notify("atm.deposit_success", formattedAmt), false);
    }

    private static void handleDepositAmount(ServerPlayer player, MarketDAO dao, ServerboundAtmActionPayload payload) throws Exception {
        double req = payload.amount();
        if (req <= 0 || req % 10 != 0 || Double.isNaN(req) || Double.isInfinite(req)) {
            sendAtmDataToClient(player, AmmoraLang.notify("atm.invalid_amount"), true);
            return;
        }

        int count1000 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_1000.get());
        int count100 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_100.get());
        int count10 = InventoryHelper.countPlayerItems(player, AmmoraMod.BANKNOTE_10.get());

        double totalCash = count1000 * 1000.0 + count100 * 100.0 + count10 * 10.0;
        if (totalCash < req) {
            sendAtmDataToClient(player, AmmoraLang.notify("atm.not_enough_cash"), true);
            return;
        }

        // Greedily consume denominations from 1000, 100, 10
        int needed = (int) req;
        int take1000 = Math.min(needed / 1000, count1000);
        needed -= take1000 * 1000;

        int take100 = Math.min(needed / 100, count100);
        needed -= take100 * 100;

        int take10 = Math.min(needed / 10, count10);
        needed -= take10 * 10;

        if (needed > 0) {
            // Player has enough total, but cannot make exact change with larger bills
            sendAtmDataToClient(player, AmmoraLang.notify("atm.not_enough_cash"), true);
            return;
        }

        if (take1000 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_1000.get(), take1000);
        if (take100 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_100.get(), take100);
        if (take10 > 0) InventoryHelper.removePlayerItems(player, AmmoraMod.BANKNOTE_10.get(), take10);

        boolean useCompany = payload.useCompanyAccount();
        if (useCompany) {
            CompanyRecord comp = dao.getPlayerCompany(player.getUUID());
            if (comp != null) {
                dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx() + req);
                try {
                    String desc = AmmoraLang.str("gui.ammora.atm.audit_deposit", String.format(Locale.US, "%,.0f", req));
                    dao.recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(),
                            "ATM_DEPOSIT", req, desc);
                } catch (Exception ignored) {}
            } else {
                PlayerAccount acc = dao.getAccount(player.getUUID(), player.getName().getString());
                acc.setBalanceCbx(acc.getBalanceCbx() + req);
                dao.saveAccount(acc);
            }
        } else {
            PlayerAccount acc = dao.getAccount(player.getUUID(), player.getName().getString());
            acc.setBalanceCbx(acc.getBalanceCbx() + req);
            dao.saveAccount(acc);
        }

        // Audio feedback
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.4F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);

        // Record in ledger
        try {
            dao.recordP2PTransfer(new P2PTransfer(
                    UUID.randomUUID().toString(),
                    new UUID(0L, 0L),
                    AmmoraLang.str("gui.ammora.atm.deposit_ledger"),
                    player.getUUID(),
                    player.getName().getString(),
                    req,
                    System.currentTimeMillis()
            ));
        } catch (Exception ignored) {}

        String formattedAmt = String.format(Locale.US, "%,.0f", req);
        player.containerMenu.broadcastChanges();
        sendAtmDataToClient(player, AmmoraLang.notify("atm.deposit_success", formattedAmt), false);
    }
}
