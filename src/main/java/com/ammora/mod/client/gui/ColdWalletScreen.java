package com.ammora.mod.client.gui;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.client.ClientPacketHandler;
import com.ammora.mod.network.ColdWalletDataPayload;
import com.ammora.mod.network.ServerboundP2PTransferPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * ASP Cyber-Terminal GUI for Cold Wallet item.
 * Supports wireless P2P CBX transfers within 30 blocks, manual recipient input, and persistent ledger auditing.
 */
public class ColdWalletScreen extends Screen {

    private ColdWalletDataPayload data;

    // Tabs: 0 = P2P Transfers, 1 = Ledger History
    private int activeTab = 0;

    // P2P State
    private UUID selectedRecipientUuid;
    private String selectedRecipientName = "";
    private double transferAmount = 10.0;

    // UI Widgets
    private EditBox recipientBox;
    private Button sendBtn;

    // Notification banner
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0;

    // Incoming trade invite
    private UUID incomingInviteUuid;
    private String incomingInviteName = "";

    // Date formatter for ledger
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    // Palette tokens (Industrial Amber / Rust & Create theme)
    private static final int COLOR_BG = 0xF50D0E12;
    private static final int COLOR_PANEL = 0xF014161C;
    private static final int COLOR_PANEL_HEADER = 0xF01A1D24;
    private static final int COLOR_BORDER_CYAN = 0xFFFF9800; // Industrial Amber / Orange
    private static final int COLOR_BORDER_MUTED = 0xFF2A2724;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9284;

    public void setIncomingTradeInvite(UUID senderUuid, String senderName) {
        this.incomingInviteUuid = senderUuid;
        this.incomingInviteName = senderName;
        rebuildWidgets();
    }

    public ColdWalletScreen(ColdWalletDataPayload initialData) {
        super(Component.literal("Cold Wallet"));
        this.data = initialData;
        if (this.incomingInviteUuid == null && ClientPacketHandler.hasActiveTradeInvite()) {
            this.incomingInviteUuid = ClientPacketHandler.getPendingTradeInviteUuid();
            this.incomingInviteName = ClientPacketHandler.getPendingTradeInviteName();
        }
        checkNewNotification(initialData);
    }

    public void updateData(ColdWalletDataPayload newData) {
        this.data = newData;
        if (this.incomingInviteUuid == null && ClientPacketHandler.hasActiveTradeInvite()) {
            this.incomingInviteUuid = ClientPacketHandler.getPendingTradeInviteUuid();
            this.incomingInviteName = ClientPacketHandler.getPendingTradeInviteName();
        }
        checkNewNotification(newData);
        rebuildWidgets();
    }

    private void checkNewNotification(ColdWalletDataPayload d) {
        if (d != null && d.statusMessage() != null && !d.statusMessage().isEmpty()) {
            this.statusNotification = translateNotification(d.statusMessage());
            this.statusNotificationError = d.isError();
            this.notificationExpireTime = System.currentTimeMillis() + 4500L;
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        statusNotificationError ? SoundEvents.VILLAGER_NO : SoundEvents.EXPERIENCE_ORB_PICKUP,
                        1.2F
                ));
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        if (data == null) return;

        int mw = 384, mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Tab switcher buttons
        int tabW = 100;
        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 0 ? AmmoraLang.guiStr("wallet.tab_p2p_active") : AmmoraLang.guiStr("wallet.tab_p2p_inactive")),
                b -> {
                    activeTab = 0;
                    rebuildWidgets();
                }
        ).bounds(mx + 8, my + 26, tabW, 16).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 1 ? AmmoraLang.guiStr("wallet.tab_ledger_active") : AmmoraLang.guiStr("wallet.tab_ledger_inactive")),
                b -> {
                    activeTab = 1;
                    rebuildWidgets();
                }
        ).bounds(mx + 112, my + 26, tabW, 16).build());

        // Incoming Trade Invite banner in tab row if active
        if (incomingInviteUuid != null) {
            int invX = mx + 216;
            int invW = mw - 224; // 160 px
            this.addRenderableWidget(Button.builder(Component.literal("§a🤝 " + incomingInviteName), b -> {
                PacketDistributor.sendToServer(new com.ammora.mod.network.ServerboundTradeInviteResponsePayload(incomingInviteUuid, true));
                ClientPacketHandler.clearPendingTradeInvite();
                this.incomingInviteUuid = null;
                rebuildWidgets();
            }).bounds(invX, my + 26, invW - 24, 16)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(AmmoraLang.guiStr("wallet.btn_accept_trade", incomingInviteName))))
            .build());

            this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                PacketDistributor.sendToServer(new com.ammora.mod.network.ServerboundTradeInviteResponsePayload(incomingInviteUuid, false));
                ClientPacketHandler.clearPendingTradeInvite();
                this.incomingInviteUuid = null;
                rebuildWidgets();
            }).bounds(invX + invW - 22, my + 26, 22, 16).build());
        }

        if (activeTab == 0) {
            initP2PTab(mx, my, mw, mh);
        }
    }

    private void initP2PTab(int mx, int my, int mw, int mh) {
        int leftX = mx + 8;
        int leftW = 140;
        int rightX = mx + 156;
        int rightW = mw - 164; // 220 pixels

        // Player selection & trade buttons
        int listY = my + 62;
        if (data.nearbyPlayers() != null && !data.nearbyPlayers().isEmpty()) {
            for (int i = 0; i < Math.min(6, data.nearbyPlayers().size()); i++) {
                var p = data.nearbyPlayers().get(i);
                int btnY = listY + (i * 20);
                boolean isSel = p.uuid().equals(selectedRecipientUuid) || p.name().equalsIgnoreCase(selectedRecipientName);
                String label = (isSel ? "§6▶ " : "") + AmmoraLang.guiStr("wallet.distance_meters", p.name(), (int) p.distance());

                // Transfer select button
                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    this.selectedRecipientUuid = p.uuid();
                    this.selectedRecipientName = p.name();
                    if (recipientBox != null) {
                        recipientBox.setValue(p.name());
                    }
                    if (sendBtn != null) {
                        sendBtn.active = (!selectedRecipientName.isEmpty() && transferAmount > 0);
                    }
                    rebuildWidgets();
                }).bounds(leftX, btnY, leftW - 26, 18).build());

                // Direct P2P trade invite button
                this.addRenderableWidget(Button.builder(Component.literal("§6🤝"), b -> {
                    PacketDistributor.sendToServer(new com.ammora.mod.network.ServerboundTradeInvitePayload(p.uuid()));
                    this.statusNotification = AmmoraLang.guiStr("wallet.notif_trade_sent", p.name());
                    this.statusNotificationError = false;
                    this.notificationExpireTime = System.currentTimeMillis() + 4500L;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    }
                }).bounds(leftX + leftW - 24, btnY, 24, 18).build());
            }
        }

        // Recipient text box
        recipientBox = new EditBox(this.font, rightX, my + 60, rightW, 16, Component.literal(AmmoraLang.guiStr("wallet.recipient_box")));
        recipientBox.setMaxLength(16);
        recipientBox.setValue(selectedRecipientName);
        recipientBox.setHint(Component.literal(AmmoraLang.guiStr("wallet.recipient_hint")));
        recipientBox.setResponder(val -> {
            this.selectedRecipientName = val.trim();
            this.selectedRecipientUuid = null;
            if (data != null && data.nearbyPlayers() != null) {
                for (var np : data.nearbyPlayers()) {
                    if (np.name().equalsIgnoreCase(selectedRecipientName)) {
                        this.selectedRecipientUuid = np.uuid();
                        break;
                    }
                }
            }
            if (sendBtn != null) {
                sendBtn.active = (!selectedRecipientName.isEmpty() && transferAmount > 0);
            }
        });
        this.addRenderableWidget(recipientBox);

        // Amount buttons on the right (2 rows of 4 buttons)
        int amtY = my + 94;
        int btnW4 = (rightW - 6) / 4;

        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjustAmount(10)).bounds(rightX, amtY, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("+50"), b -> adjustAmount(50)).bounds(rightX + btnW4 + 2, amtY, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("+100"), b -> adjustAmount(100)).bounds(rightX + (btnW4 + 2) * 2, amtY, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("+500"), b -> adjustAmount(500)).bounds(rightX + (btnW4 + 2) * 3, amtY, btnW4, 15).build());

        int amtY2 = amtY + 17;
        this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> adjustAmount(-10)).bounds(rightX, amtY2, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("-50"), b -> adjustAmount(-50)).bounds(rightX + btnW4 + 2, amtY2, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("MAX"), b -> {
            if (data != null) transferAmount = Math.max(1.0, MarketEngine.round2(data.balanceCbx()));
            rebuildWidgets();
        }).bounds(rightX + (btnW4 + 2) * 2, amtY2, btnW4, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("wallet.btn_reset")), b -> {
            transferAmount = 10.0;
            rebuildWidgets();
        }).bounds(rightX + (btnW4 + 2) * 3, amtY2, btnW4, 15).build());

        // Send P2P Transfer Button
        int execY = my + mh - 24;
        sendBtn = Button.builder(Component.literal(AmmoraLang.guiStr("wallet.btn_send_transfer")), b -> {
            executeTransfer();
        }).bounds(rightX, execY, rightW, 18).build();

        sendBtn.active = (!selectedRecipientName.isEmpty() && transferAmount > 0);
        this.addRenderableWidget(sendBtn);
    }

    private void adjustAmount(double delta) {
        this.transferAmount = Math.max(1.0, MarketEngine.round2(this.transferAmount + delta));
    }

    private void executeTransfer() {
        if (selectedRecipientName == null || selectedRecipientName.trim().isEmpty() || transferAmount <= 0) return;
        UUID uuidToSend = selectedRecipientUuid != null ? selectedRecipientUuid : new UUID(0L, 0L);
        PacketDistributor.sendToServer(new ServerboundP2PTransferPayload(
                uuidToSend,
                selectedRecipientName.trim(),
                transferAmount
        ));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderBlurredBackground(partialTick);
        }

        // Darkened background backdrop
        g.fill(0, 0, this.width, this.height, 0xAA000000);

        if (data == null) {
            g.drawCenteredString(this.font, AmmoraLang.guiStr("wallet.loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        int mw = 384, mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Main window container
        g.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header Panel
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 24, COLOR_PANEL_HEADER);
        g.fill(mx + 1, my + 24, mx + mw - 1, my + 25, COLOR_BORDER_MUTED);

        g.drawString(this.font, "§6§l" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.title"), mx + 8, my + 5, 0xFFFFFFFF);
        String sub = "§7" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.balance", MarketEngine.round2(data.balanceCbx())) + " §8| §7REP: §e" + data.repPoints();
        g.drawString(this.font, sub, mx + 8, my + 15, 0xFFFFFFFF);

        // Rank badge
        String rankTitle = getRankTitle(data.repLevel());
        String rankBadge = AmmoraLang.guiStr("wallet.rank_lvl", rankTitle, data.repLevel());
        g.drawString(this.font, rankBadge, mx + mw - 8 - this.font.width(rankBadge), my + 5, 0xFFFFFFFF);

        if (activeTab == 0) {
            renderP2PTab(g, mx, my, mw, mh);
        } else {
            renderLedgerTab(g, mx, my, mw, mh);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderP2PTab(GuiGraphics g, int mx, int my, int mw, int mh) {
        int leftX = mx + 8;
        int leftW = 140;
        int rightX = mx + 156;
        int rightW = mw - 164;

        // Left section: Nearby players
        g.drawString(this.font, "§6§l" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.nearby_players"), leftX, my + 48, 0xFFFFFFFF);

        if (data.nearbyPlayers() == null || data.nearbyPlayers().isEmpty()) {
            g.drawString(this.font, "§8" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.no_nearby"), leftX, my + 66, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("wallet.p2p_radius"), leftX, my + 78, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("wallet.enter_name_hint"), leftX, my + 94, COLOR_BORDER_CYAN);
        }

        // Right section: Transfer controls
        g.drawString(this.font, "§6§l" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.transfer_title"), rightX, my + 48, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.amount_line", MarketEngine.round2(transferAmount)), rightX, my + 80, 0xFFFFFFFF);

        // P2P benefits (neatly within width)
        int infoY = my + 132;
        g.drawString(this.font, AmmoraLang.guiStr("wallet.zero_fee"), rightX, infoY, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.bullet_instant"), rightX, infoY + 11, COLOR_TEXT_MUTED);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.bullet_secure"), rightX, infoY + 22, COLOR_TEXT_MUTED);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.bullet_audit"), rightX, infoY + 33, COLOR_TEXT_MUTED);

        // Status Notification
        renderStatusNotification(g, rightX, rightW, my + mh - 44);
    }

    private void renderLedgerTab(GuiGraphics g, int mx, int my, int mw, int mh) {
        int contentX = mx + 8;
        int contentW = mw - 16;
        int topY = my + 48;

        g.drawString(this.font, AmmoraLang.guiStr("wallet.ledger_header"), contentX, topY, 0xFFFFFFFF);

        int tableY = topY + 14;
        int rowH = 14;
        int maxRows = 10;

        // Table Header
        g.fill(contentX, tableY, contentX + contentW, tableY + 13, 0xFF17202E);
        renderBorder(g, contentX, tableY, contentW, 13, COLOR_BORDER_MUTED);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.th_time"), contentX + 4, tableY + 3, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.th_type"), contentX + 56, tableY + 3, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("wallet.th_desc"), contentX + 104, tableY + 3, 0xFFFFFFFF);
        String amtHdr = AmmoraLang.guiStr("wallet.th_amount");
        g.drawString(this.font, amtHdr, contentX + contentW - 4 - this.font.width(amtHdr), tableY + 3, 0xFFFFFFFF);

        if (data.ledgerEntries() == null || data.ledgerEntries().isEmpty()) {
            g.drawCenteredString(this.font, AmmoraLang.guiStr("wallet.empty_ledger"), contentX + contentW / 2, tableY + 30, COLOR_TEXT_MUTED);
            return;
        }

        int curY = tableY + 14;
        for (int i = 0; i < Math.min(maxRows, data.ledgerEntries().size()); i++) {
            var entry = data.ledgerEntries().get(i);
            int rowBg = (i % 2 == 0) ? 0x18FF9800 : 0x0CFF9800;
            g.fill(contentX, curY, contentX + contentW, curY + rowH, rowBg);

            // Timestamp
            String timeStr = TIME_FMT.format(new Date(entry.timestamp()));
            g.drawString(this.font, "§8" + timeStr, contentX + 4, curY + 3, 0xFFFFFFFF);

            // Badge Type
            String badge = switch (entry.type()) {
                case "P2P_IN" -> AmmoraLang.guiStr("wallet.tag_p2p_in");
                case "P2P_OUT" -> AmmoraLang.guiStr("wallet.tag_p2p_out");
                case "BUY" -> AmmoraLang.guiStr("wallet.tag_buy");
                case "SELL" -> AmmoraLang.guiStr("wallet.tag_sell");
                case "OMS" -> AmmoraLang.guiStr("wallet.tag_oms");
                default -> "§7[" + entry.type() + "]";
            };
            g.drawString(this.font, badge, contentX + 56, curY + 3, 0xFFFFFFFF);

            // Title
            String title = AmmoraLang.translateNotification(entry.title());
            if (this.font.width(title) > 150) {
                title = this.font.plainSubstrByWidth(title, 146) + "..";
            }
            g.drawString(this.font, "§f" + title, contentX + 104, curY + 3, 0xFFFFFFFF);

            // Amount
            double amt = entry.amountCbx();
            String amtStr = (amt >= 0 ? "§a+" : "§c-") + MarketEngine.round2(Math.abs(amt)) + " CBX";
            g.drawString(this.font, amtStr, contentX + contentW - 4 - this.font.width(amtStr), curY + 3, 0xFFFFFFFF);

            curY += rowH;
        }

        g.drawCenteredString(this.font, AmmoraLang.guiStr("wallet.audit_footer"), contentX + contentW / 2, my + mh - 12, COLOR_TEXT_MUTED);
    }

    private void renderStatusNotification(GuiGraphics g, int x, int w, int y) {
        if (statusNotification != null && !statusNotification.isEmpty() && System.currentTimeMillis() < notificationExpireTime) {
            int notifBg = statusNotificationError ? 0xEE4A121A : 0xEE0D3B20;
            int notifBorder = statusNotificationError ? COLOR_RED : COLOR_GREEN;

            g.fill(x, y, x + w, y + 16, notifBg);
            renderBorder(g, x, y, w, 16, notifBorder);

            String trimmed = statusNotification;
            if (this.font.width(trimmed) > w - 8) {
                trimmed = this.font.plainSubstrByWidth(trimmed, w - 12) + "..";
            }
            g.drawCenteredString(this.font, (statusNotificationError ? "§c" : "§a") + trimmed, x + w / 2, y + 4, 0xFFFFFFFF);
        }
    }

    private void renderBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private String getRankTitle(int level) {
        return com.ammora.mod.util.AmmoraLang.str("rank.ammora." + level);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Overridden to no-op so Minecraft's default Screen#render doesn't apply blur over custom GUI
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (recipientBox == null || !recipientBox.isFocused()) {
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String translateNotification(String msg) {
        return AmmoraLang.translateNotification(msg);
    }
}
