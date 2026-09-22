package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.core.CompanyInviteManager;
import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.db.CompanyMemberRecord;
import com.ammora.mod.db.CompanyRecord;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.UUID;

/**
 * Handles incoming client network packets for corporate accounts and joint treasuries.
 */
public final class CompanyPacketHandler {

    private CompanyPacketHandler() {}

    public static void handleCompanyAction(ServerPlayer player, ServerboundCompanyActionPayload payload) {
        if (AmmoraMod.getMarketDAO() == null) return;

        try {
            switch (payload.action()) {
                case "REGISTER" -> handleRegister(player, payload);
                case "DEPOSIT" -> handleDeposit(player, payload);
                case "WITHDRAW" -> handleWithdraw(player, payload);
                case "INVITE" -> handleInvite(player, payload);
                case "ACCEPT_INVITE" -> handleAcceptInvite(player, payload);
                case "DECLINE_INVITE" -> handleDeclineInvite(player, payload);
                case "KICK" -> handleKick(player, payload);
                case "SET_ROLE" -> handleSetRole(player, payload);
                case "SET_LIMIT" -> handleSetLimit(player, payload);
                case "LEAVE" -> handleLeave(player);
                case "DISSOLVE" -> handleDissolve(player);
                default -> EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_unknown_action"), true);
            }
        } catch (Exception e) {
            AmmoraMod.LOGGER.error("Failed to execute company action: " + payload.action(), e);
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_generic"), true);
        }
    }

    private static void handleRegister(ServerPlayer player, ServerboundCompanyActionPayload payload) {
        String name = payload.companyName() != null ? payload.companyName().trim() : "";
        if (name.length() < 3 || name.length() > 24) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_name_length"), true);
            return;
        }

        try {
            double regFee = AmmoraMod.getMarketDAO().getCompanyRegistrationFee();
            CompanyRecord created = AmmoraMod.getMarketDAO().createCompany(name, player.getUUID(), player.getName().getString(), regFee);
            if (created != null) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.2F);
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.created_success", name), false);
            } else {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_generic"), true);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("COMPANY_NAME_TAKEN")) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_name_taken"), true);
            } else if (msg.contains("ALREADY_IN_COMPANY")) {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_already_in_company"), true);
            } else if (msg.contains("INSUFFICIENT_FUNDS_FEE")) {
                double regFee = AmmoraMod.getMarketDAO().getCompanyRegistrationFee();
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_fee_insufficient", String.format(Locale.US, "%.2f", regFee)), true);
            } else {
                EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_generic"), true);
            }
        }
    }

    private static void handleDeposit(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_not_in_company"), true);
            return;
        }

        double amount = Math.max(0.01, MarketEngine.round2(payload.amount()));
        boolean ok = AmmoraMod.getMarketDAO().depositToCompany(comp.getCompanyId(), player.getUUID(), player.getName().getString(), amount);
        if (ok) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.deposited_success", String.format(Locale.US, "%.2f", amount)), false);
        } else {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_deposit_insufficient"), true);
        }
    }

    private static void handleWithdraw(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_not_in_company"), true);
            return;
        }

        CompanyMemberRecord member = AmmoraMod.getMarketDAO().getCompanyMember(comp.getCompanyId(), player.getUUID());
        if (member == null || member.isMember()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_withdraw_unauthorized"), true);
            return;
        }

        double amount = Math.max(0.01, MarketEngine.round2(payload.amount()));
        if (!member.canSpend(amount)) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_daily_limit_exceeded"), true);
            return;
        }

        boolean ok = AmmoraMod.getMarketDAO().withdrawFromCompany(comp.getCompanyId(), player.getUUID(), player.getName().getString(), amount);
        if (ok) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.withdrawn_success", String.format(Locale.US, "%.2f", amount)), false);
        } else {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_treasury_insufficient"), true);
        }
    }

    private static void handleInvite(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null || !comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_only"), true);
            return;
        }

        String targetName = payload.targetPlayerName() != null ? payload.targetPlayerName().trim() : "";
        if (targetName.isEmpty()) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_player_not_found", ""), true);
            return;
        }

        if (targetName.equalsIgnoreCase(player.getName().getString())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_cannot_invite_self"), true);
            return;
        }

        ServerPlayer target = player.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_player_not_found", targetName), true);
            return;
        }

        if (AmmoraMod.getMarketDAO().getPlayerCompany(target.getUUID()) != null) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_target_already_in_company", targetName), true);
            return;
        }

        if (CompanyInviteManager.getInstance().hasPendingInvite(target.getUUID(), comp.getCompanyId())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_invite_already_pending", targetName), true);
            return;
        }

        CompanyInviteManager.getInstance().createInvite(target.getUUID(), comp.getCompanyId(), comp.getCompanyName(), player.getUUID(), player.getName().getString());

        PacketDistributor.sendToPlayer(target, new ClientboundCompanyInvitePayload(comp.getCompanyId(), comp.getCompanyName(), player.getName().getString()));
        target.sendSystemMessage(Component.translatable("message.ammora.company.invite_received", comp.getCompanyName(), player.getName().getString()));
        target.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.invite_sent_waiting", target.getName().getString()), false);
    }

    private static void handleAcceptInvite(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        String companyId = payload.companyName();
        var invite = CompanyInviteManager.getInstance().getPendingInvite(player.getUUID());
        if (invite == null || (companyId != null && !companyId.isEmpty() && !invite.companyId().equals(companyId))) {
            player.sendSystemMessage(Component.translatable("message.ammora.company.invite_expired"));
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_invite_expired"), true);
            return;
        }

        CompanyInviteManager.getInstance().removeInvite(player.getUUID());

        if (AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID()) != null) {
            player.sendSystemMessage(Component.translatable("message.ammora.company.err_already_in_company"));
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_already_in_company"), true);
            return;
        }

        CompanyRecord comp = AmmoraMod.getMarketDAO().getCompany(invite.companyId());
        if (comp == null) {
            player.sendSystemMessage(Component.translatable("message.ammora.company.err_company_not_found"));
            return;
        }

        AmmoraMod.getMarketDAO().addCompanyMember(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "MEMBER", 100.0);
        AmmoraMod.getMarketDAO().recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "JOIN", 0.0, player.getName().getString() + " accepted invitation");

        player.sendSystemMessage(Component.translatable("message.ammora.company.invited_welcome", comp.getCompanyName()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);

        ServerPlayer inviter = player.server.getPlayerList().getPlayer(invite.inviterUuid());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("message.ammora.company.invite_accepted_owner", player.getName().getString()));
            inviter.level().playSound(null, inviter.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            EscrowPacketHandler.sendMarketplaceData(inviter, AmmoraLang.notify("company.member_joined_success", player.getName().getString()), false);
        }

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.joined_success", comp.getCompanyName()), false);
    }

    private static void handleDeclineInvite(ServerPlayer player, ServerboundCompanyActionPayload payload) {
        var invite = CompanyInviteManager.getInstance().removeInvite(player.getUUID());
        if (invite != null) {
            player.sendSystemMessage(Component.translatable("message.ammora.company.invite_declined_self"));
            ServerPlayer inviter = player.server.getPlayerList().getPlayer(invite.inviterUuid());
            if (inviter != null) {
                inviter.sendSystemMessage(Component.translatable("message.ammora.company.invite_declined_owner", player.getName().getString()));
                EscrowPacketHandler.sendMarketplaceData(inviter, AmmoraLang.notify("company.invite_declined_by", player.getName().getString()), true);
            }
        }
    }

    private static void handleKick(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null || !comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_only"), true);
            return;
        }

        UUID targetUuid = payload.targetPlayerUuid();
        if (targetUuid == null || targetUuid.equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_cannot_kick_self"), true);
            return;
        }

        CompanyMemberRecord targetMem = AmmoraMod.getMarketDAO().getCompanyMember(comp.getCompanyId(), targetUuid);
        String targetName = targetMem != null ? targetMem.getPlayerName() : "Member";

        AmmoraMod.getMarketDAO().removeCompanyMember(comp.getCompanyId(), targetUuid);
        AmmoraMod.getMarketDAO().recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "KICK", 0.0, "Kicked " + targetName);

        ServerPlayer targetPlayer = player.server.getPlayerList().getPlayer(targetUuid);
        if (targetPlayer != null) {
            targetPlayer.sendSystemMessage(Component.translatable("message.ammora.company.kicked_notify", comp.getCompanyName()));
        }

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.member_kicked_success", targetName), false);
    }

    private static void handleSetRole(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null || !comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_only"), true);
            return;
        }

        UUID targetUuid = payload.targetPlayerUuid();
        if (targetUuid == null || targetUuid.equals(player.getUUID())) return;

        String newRole = payload.role() != null ? payload.role().toUpperCase() : "MEMBER";
        if (!"MANAGER".equals(newRole) && !"MEMBER".equals(newRole)) return;

        AmmoraMod.getMarketDAO().updateMemberRole(comp.getCompanyId(), targetUuid, newRole);
        AmmoraMod.getMarketDAO().recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "SET_ROLE", 0.0, "Changed role of " + targetUuid + " to " + newRole);

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.role_updated_success"), false);
    }

    private static void handleSetLimit(ServerPlayer player, ServerboundCompanyActionPayload payload) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null || !comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_only"), true);
            return;
        }

        UUID targetUuid = payload.targetPlayerUuid();
        if (targetUuid == null) return;

        double limit = Math.max(0.0, MarketEngine.round2(payload.amount()));
        AmmoraMod.getMarketDAO().updateMemberDailyLimit(comp.getCompanyId(), targetUuid, limit);
        AmmoraMod.getMarketDAO().recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "SET_LIMIT", limit, "Updated daily limit for member");

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.limit_updated_success"), false);
    }

    private static void handleLeave(ServerPlayer player) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_not_in_company"), true);
            return;
        }

        if (comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_cannot_leave"), true);
            return;
        }

        AmmoraMod.getMarketDAO().removeCompanyMember(comp.getCompanyId(), player.getUUID());
        AmmoraMod.getMarketDAO().recordCompanyLedger(comp.getCompanyId(), player.getUUID(), player.getName().getString(), "LEAVE", 0.0, player.getName().getString() + " left the company");

        EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.left_success"), false);
    }

    private static void handleDissolve(ServerPlayer player) throws Exception {
        CompanyRecord comp = AmmoraMod.getMarketDAO().getPlayerCompany(player.getUUID());
        if (comp == null || !comp.getOwnerUuid().equals(player.getUUID())) {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_owner_only"), true);
            return;
        }

        boolean ok = AmmoraMod.getMarketDAO().dissolveCompany(comp.getCompanyId(), player.getUUID(), player.getName().getString());
        if (ok) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_DESTROY, SoundSource.PLAYERS, 0.8F, 1.0F);
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.dissolved_success"), false);
        } else {
            EscrowPacketHandler.sendMarketplaceData(player, AmmoraLang.notify("company.err_generic"), true);
        }
    }
}
