package com.ammora.mod.client.gui;

import com.ammora.mod.network.ServerboundCompanyActionPayload;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Confirmation dialog shown to an invited player when an invitation to join a corporation is received.
 * Prevents players from being added to companies without their consent.
 */
public class CompanyInviteScreen extends Screen {

    private static final int COLOR_PANEL = 0xFF10141D;
    private static final int COLOR_PANEL_HEADER = 0xFF171E2B;
    private static final int COLOR_BORDER_CYAN = 0xFF00E5FF;
    private static final int COLOR_BORDER_MUTED = 0xFF2A3649;

    private final String companyId;
    private final String companyName;
    private final String inviterName;

    public CompanyInviteScreen(String companyId, String companyName, String inviterName) {
        super(Component.literal("Company Invitation"));
        this.companyId = companyId != null ? companyId : "";
        this.companyName = companyName != null ? companyName : "Company";
        this.inviterName = inviterName != null ? inviterName : "Player";
    }

    @Override
    protected void init() {
        super.init();

        int mw = 360, mh = 165;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Accept Button
        this.addRenderableWidget(Button.builder(Component.literal("§a✔ " + AmmoraLang.guiStr("company.invite_modal_accept")), b -> {
            PacketDistributor.sendToServer(ServerboundCompanyActionPayload.acceptInvite(companyId));
            this.onClose();
        }).bounds(mx + 25, my + mh - 30, 145, 20).build());

        // Decline Button
        this.addRenderableWidget(Button.builder(Component.literal("§c✖ " + AmmoraLang.guiStr("company.invite_modal_decline")), b -> {
            PacketDistributor.sendToServer(ServerboundCompanyActionPayload.declineInvite(companyId));
            this.onClose();
        }).bounds(mx + mw - 170, my + mh - 30, 145, 20).build());

        // Close button in header
        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
            PacketDistributor.sendToServer(ServerboundCompanyActionPayload.declineInvite(companyId));
            this.onClose();
        }).bounds(mx + mw - 22, my + 4, 18, 14).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dim background
        g.fill(0, 0, this.width, this.height, 0xCC060910);

        int mw = 360, mh = 165;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Modal Panel
        g.fill(mx, my, mx + mw, my + mh, COLOR_PANEL);

        // Header Panel & separator
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 22, COLOR_PANEL_HEADER);
        g.hLine(mx + 1, mx + mw - 2, my + 22, COLOR_BORDER_MUTED);

        // Outer Frame Border drawn on top (clean 1px cyan outline including top)
        g.renderOutline(mx, my, mw, mh, COLOR_BORDER_CYAN);

        g.drawString(this.font, "§6🏢 " + AmmoraLang.guiStr("company.invite_modal_title"), mx + 10, my + 7, 0xFFFFFFFF);

        // Content
        g.drawCenteredString(this.font, "§7" + AmmoraLang.guiStr("company.invite_modal_desc"), mx + mw / 2, my + 34, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "§e§l«" + companyName + "»", mx + mw / 2, my + 48, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "§7" + AmmoraLang.guiStr("company.invite_modal_inviter", "§f" + inviterName), mx + mw / 2, my + 68, 0xFFFFFFFF);

        g.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("company.invite_modal_info"), mx + mw / 2, my + 92, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("company.invite_modal_info2"), mx + mw / 2, my + 104, 0xFFFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            PacketDistributor.sendToServer(ServerboundCompanyActionPayload.declineInvite(companyId));
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No-op to avoid vanilla screen background blur
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
