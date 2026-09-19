package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client-to-server payload for corporate and joint account operations:
 * Company registration, treasury deposit/withdraw, member management, and role/limit adjustments.
 */
public record ServerboundCompanyActionPayload(
        String action,
        String companyName,
        String targetPlayerName,
        UUID targetPlayerUuid,
        String role,
        double amount
) implements CustomPacketPayload {

    public static final Type<ServerboundCompanyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "company_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundCompanyActionPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ServerboundCompanyActionPayload::write,
            ServerboundCompanyActionPayload::new
    );

    public ServerboundCompanyActionPayload(FriendlyByteBuf buf) {
        this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean() ? buf.readUUID() : null,
                buf.readUtf(),
                buf.readDouble()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(companyName != null ? companyName : "");
        buf.writeUtf(targetPlayerName != null ? targetPlayerName : "");
        buf.writeBoolean(targetPlayerUuid != null);
        if (targetPlayerUuid != null) {
            buf.writeUUID(targetPlayerUuid);
        }
        buf.writeUtf(role != null ? role : "");
        buf.writeDouble(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ServerboundCompanyActionPayload register(String companyName) {
        return new ServerboundCompanyActionPayload("REGISTER", companyName, "", null, "", 0.0);
    }

    public static ServerboundCompanyActionPayload deposit(double amount) {
        return new ServerboundCompanyActionPayload("DEPOSIT", "", "", null, "", amount);
    }

    public static ServerboundCompanyActionPayload withdraw(double amount) {
        return new ServerboundCompanyActionPayload("WITHDRAW", "", "", null, "", amount);
    }

    public static ServerboundCompanyActionPayload invite(String targetPlayerName) {
        return new ServerboundCompanyActionPayload("INVITE", "", targetPlayerName, null, "", 0.0);
    }

    public static ServerboundCompanyActionPayload kick(UUID targetPlayerUuid) {
        return new ServerboundCompanyActionPayload("KICK", "", "", targetPlayerUuid, "", 0.0);
    }

    public static ServerboundCompanyActionPayload setRole(UUID targetPlayerUuid, String role) {
        return new ServerboundCompanyActionPayload("SET_ROLE", "", "", targetPlayerUuid, role, 0.0);
    }

    public static ServerboundCompanyActionPayload setLimit(UUID targetPlayerUuid, double limit) {
        return new ServerboundCompanyActionPayload("SET_LIMIT", "", "", targetPlayerUuid, "", limit);
    }

    public static ServerboundCompanyActionPayload leave() {
        return new ServerboundCompanyActionPayload("LEAVE", "", "", null, "", 0.0);
    }

    public static ServerboundCompanyActionPayload dissolve() {
        return new ServerboundCompanyActionPayload("DISSOLVE", "", "", null, "", 0.0);
    }
}

