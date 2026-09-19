package com.ammora.mod.network;

import com.ammora.mod.AmmoraMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from client to server for cold wallet loan interactions.
 * Actions:
 * - "REPAY": Repay active loan in full (principal + interest) and reclaim collateral
 * - "CLAIM_COLLATERAL": Claim defaulted loan collateral if lender
 */
public record ServerboundLoanActionPayload(String action, String loanId) implements CustomPacketPayload {

    public static final Type<ServerboundLoanActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "loan_action"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundLoanActionPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ServerboundLoanActionPayload::write,
                    ServerboundLoanActionPayload::new
            );

    public ServerboundLoanActionPayload(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(action != null ? action : "");
        buf.writeUtf(loanId != null ? loanId : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
