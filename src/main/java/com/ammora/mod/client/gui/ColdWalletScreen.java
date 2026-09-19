package com.ammora.mod.client.gui;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.client.ClientPacketHandler;
import com.ammora.mod.network.ColdWalletDataPayload;
import com.ammora.mod.network.ServerboundLoanActionPayload;
import com.ammora.mod.network.ServerboundP2PTransferPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ASP Cyber-Terminal GUI for Cold Wallet item.
 * Supports wireless P2P CBX transfers within 30 blocks, manual recipient input, and persistent ledger auditing.
 */
public class ColdWalletScreen extends Screen {

    private ColdWalletDataPayload data;

    // Tabs: 0 = P2P Transfers, 1 = Ledger History, 2 = Loans
    private int activeTab = 0;

    // Loans State
    private int loanSubTab = 0; // 0 = Borrowed, 1 = Lent
    private int loanPage = 0;

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

    // Account mode (Personal vs Corporate)
    private static boolean useCompanyAccount = false;

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
        super(Component.translatable("gui.ammora.wallet.title"));
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

        // Tab switcher buttons: 3 tabs
        int tabW = 76;
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
        ).bounds(mx + 88, my + 26, tabW, 16).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 2 ? AmmoraLang.guiStr("wallet.tab_loans_active") : AmmoraLang.guiStr("wallet.tab_loans_inactive")),
                b -> {
                    activeTab = 2;
                    loanPage = 0;
                    rebuildWidgets();
                }
        ).bounds(mx + 168, my + 26, tabW, 16).build());

        // Incoming Trade Invite banner in tab row if active
        if (incomingInviteUuid != null) {
            int invX = mx + 248;
            int invW = mw - 256;
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

        // Corporate Account toggle in top header if player has company
        if (data.hasCompany()) {
            String compShort = data.companyName();
            if (this.font.width(compShort) > 46) {
                compShort = this.font.plainSubstrByWidth(compShort, 40) + "..";
            }
            String toggleText = useCompanyAccount ? "§6🏢 " + compShort : "§b👤 " + AmmoraLang.guiStr("account.personal");
            int toggleW = Math.max(54, Math.min(76, this.font.width(toggleText) + 10));
            int toggleX = mx + mw - toggleW - 8;
            this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
                useCompanyAccount = !useCompanyAccount;
                rebuildWidgets();
            }).bounds(toggleX, my + 4, toggleW, 16)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    useCompanyAccount ? AmmoraLang.guiStr("account.switch_to_personal") : AmmoraLang.guiStr("account.switch_to_company")
            )))
            .build());
        }

        if (activeTab == 0) {
            initP2PTab(mx, my, mw, mh);
        } else if (activeTab == 2) {
            initLoansTab(mx, my, mw, mh);
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
                transferAmount,
                useCompanyAccount
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
        String sub;
        if (data.hasCompany() && useCompanyAccount) {
            String roleBadge = "OWNER".equalsIgnoreCase(data.companyRole()) ? "👑" : ("MANAGER".equalsIgnoreCase(data.companyRole()) ? "👔" : "👤");
            sub = "§6🏢 " + data.companyName() + " §8| §e" + MarketEngine.round2(data.companyBalance()) + " CBX " + roleBadge;
        } else {
            sub = "§7" + com.ammora.mod.util.AmmoraLang.guiStr("wallet.balance", MarketEngine.round2(data.balanceCbx())) + " §8| §7REP: §e" + data.repPoints();
        }
        g.drawString(this.font, sub, mx + 8, my + 15, 0xFFFFFFFF);

        // Rank badge (only if no corporate toggle overlaying)
        if (!data.hasCompany()) {
            String rankTitle = getRankTitle(data.repLevel());
            String rankBadge = AmmoraLang.guiStr("wallet.rank_lvl", rankTitle, data.repLevel());
            g.drawString(this.font, rankBadge, mx + mw - 8 - this.font.width(rankBadge), my + 5, 0xFFFFFFFF);
        }

        if (activeTab == 0) {
            renderP2PTab(g, mx, my, mw, mh);
        } else if (activeTab == 1) {
            renderLedgerTab(g, mx, my, mw, mh);
        } else {
            renderLoansTab(g, mx, my, mw, mh, mouseX, mouseY);
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
            int curTextY = my + 66;
            for (var line : this.font.split(Component.literal("§8" + AmmoraLang.guiStr("wallet.no_nearby")), leftW)) {
                g.drawString(this.font, line, leftX, curTextY, COLOR_TEXT_MUTED);
                curTextY += 11;
            }
            curTextY += 1;
            for (var line : this.font.split(Component.literal(AmmoraLang.guiStr("wallet.p2p_radius")), leftW)) {
                g.drawString(this.font, line, leftX, curTextY, COLOR_TEXT_MUTED);
                curTextY += 11;
            }
            curTextY += 3;
            for (var line : this.font.split(Component.literal(AmmoraLang.guiStr("wallet.enter_name_hint")), leftW)) {
                g.drawString(this.font, line, leftX, curTextY, COLOR_BORDER_CYAN);
                curTextY += 11;
            }
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

    private void initLoansTab(int mx, int my, int mw, int mh) {
        int leftX = mx + 8;
        int subTabW = 86;
        // SubTab 0: Borrowed
        this.addRenderableWidget(Button.builder(
                Component.literal((loanSubTab == 0 ? "§6▶ " : "") + AmmoraLang.guiStr("wallet.loans_borrowed")),
                b -> {
                    loanSubTab = 0;
                    loanPage = 0;
                    rebuildWidgets();
                }
        ).bounds(leftX, my + 46, subTabW, 15).build());

        // SubTab 1: Lent
        this.addRenderableWidget(Button.builder(
                Component.literal((loanSubTab == 1 ? "§6▶ " : "") + AmmoraLang.guiStr("wallet.loans_lent")),
                b -> {
                    loanSubTab = 1;
                    loanPage = 0;
                    rebuildWidgets();
                }
        ).bounds(leftX + subTabW + 4, my + 46, subTabW, 15).build());

        List<ColdWalletDataPayload.LoanItem> filtered = getFilteredLoans();
        int pageSize = 3;
        int maxPage = Math.max(0, (filtered.size() - 1) / pageSize);
        if (loanPage > maxPage) loanPage = maxPage;

        // Pagination buttons
        if (maxPage > 0) {
            int navW = 20;
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                if (loanPage > 0) {
                    loanPage--;
                    rebuildWidgets();
                }
            }).bounds(mx + mw - 52, my + 46, navW, 15).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                if (loanPage < maxPage) {
                    loanPage++;
                    rebuildWidgets();
                }
            }).bounds(mx + mw - 28, my + 46, navW, 15).build());
        }

        // Action buttons on visible cards
        int startIdx = loanPage * pageSize;
        int endIdx = Math.min(startIdx + pageSize, filtered.size());
        int cardH = 46;
        int startY = my + 65;

        for (int i = startIdx; i < endIdx; i++) {
            var loan = filtered.get(i);
            int cardY = startY + ((i - startIdx) * (cardH + 4));
            int btnX = mx + mw - 76;
            int btnY = cardY + 13;
            int btnW = 60;
            int btnH = 20;

            if (loanSubTab == 0 && "ACTIVE".equalsIgnoreCase(loan.status())) {
                // Repay button for Borrower
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("wallet.btn_repay")), b -> {
                    PacketDistributor.sendToServer(new ServerboundLoanActionPayload("REPAY", loan.loanId()));
                    this.statusNotification = AmmoraLang.guiStr("wallet.loan_repaying");
                    this.statusNotificationError = false;
                    this.notificationExpireTime = System.currentTimeMillis() + 4500L;
                }).bounds(btnX, btnY, btnW, btnH)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        AmmoraLang.guiStr("trade.total_repay_lbl", String.format(Locale.US, "%.1f CBX", loan.totalRepayCbx()))
                )))
                .build());
            } else if (loanSubTab == 1 && "DEFAULTED".equalsIgnoreCase(loan.status())) {
                // Claim button for Lender
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("wallet.btn_claim")), b -> {
                    PacketDistributor.sendToServer(new ServerboundLoanActionPayload("CLAIM_COLLATERAL", loan.loanId()));
                }).bounds(btnX, btnY, btnW, btnH).build());
            }
        }
    }

    private void renderLoansTab(GuiGraphics g, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        List<ColdWalletDataPayload.LoanItem> filtered = getFilteredLoans();
        int pageSize = 3;
        int maxPage = Math.max(0, (filtered.size() - 1) / pageSize);
        if (loanPage > maxPage) loanPage = maxPage;

        // Page indicator text
        if (maxPage > 0) {
            String pageStr = AmmoraLang.guiStr("wallet.loan_page", loanPage + 1, maxPage + 1);
            g.drawString(this.font, pageStr, mx + mw - 60 - this.font.width(pageStr), my + 50, 0xFFFFFFFF);
        }

        if (filtered.isEmpty()) {
            g.drawCenteredString(this.font, AmmoraLang.guiStr("wallet.no_loans"), mx + mw / 2, my + 110, COLOR_TEXT_MUTED);
            renderStatusNotification(g, mx + 8, mw - 16, my + mh - 26);
            return;
        }

        int startIdx = loanPage * pageSize;
        int endIdx = Math.min(startIdx + pageSize, filtered.size());
        int cardH = 46;
        int startY = my + 65;

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int i = startIdx; i < endIdx; i++) {
            var loan = filtered.get(i);
            int cardX = mx + 8;
            int cardW = mw - 16;
            int cardY = startY + ((i - startIdx) * (cardH + 4));

            int borderCol = "ACTIVE".equalsIgnoreCase(loan.status())
                    ? COLOR_AMBER
                    : ("REPAID".equalsIgnoreCase(loan.status()) ? COLOR_GREEN : COLOR_RED);

            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_PANEL);
            renderBorder(g, cardX, cardY, cardW, cardH, borderCol);

            // Collateral Item Box
            int itemBoxX = cardX + 5;
            int itemBoxY = cardY + 5;
            int itemBoxSize = 36;
            boolean itemHovered = mouseX >= itemBoxX && mouseX < itemBoxX + itemBoxSize && mouseY >= itemBoxY && mouseY < itemBoxY + itemBoxSize;

            g.fill(itemBoxX, itemBoxY, itemBoxX + itemBoxSize, itemBoxY + itemBoxSize, itemHovered ? 0x44FFAA00 : 0xFF192230);
            renderBorder(g, itemBoxX, itemBoxY, itemBoxSize, itemBoxSize, itemHovered ? COLOR_BORDER_CYAN : COLOR_BORDER_MUTED);

            ItemStack st = reconstructLoanStack(loan);
            if (!st.isEmpty()) {
                g.renderItem(st, itemBoxX + 10, itemBoxY + 10);
                g.renderItemDecorations(this.font, st, itemBoxX + 10, itemBoxY + 10);
                if (itemHovered) {
                    hoveredStack = st;
                }
            }

            // Info rows
            int textX = cardX + 46;
            // Row 1: Item display name
            String displayName = loan.displayName();
            if (loan.itemCount() > 1) displayName += " x" + loan.itemCount();
            if (this.font.width(displayName) > 170) {
                displayName = this.font.plainSubstrByWidth(displayName, 164) + "..";
            }
            g.drawString(this.font, "§f§l" + displayName, textX, cardY + 6, 0xFFFFFFFF);

            // Row 2: Counterparty
            String cp = (loanSubTab == 0)
                    ? AmmoraLang.guiStr("wallet.loan_lender", "§e" + loan.lenderName())
                    : AmmoraLang.guiStr("wallet.loan_borrower", "§b" + loan.borrowerName());
            g.drawString(this.font, "§7" + cp, textX, cardY + 18, 0xFFFFFFFF);

            // Row 3: Financials
            String fin = AmmoraLang.guiStr("wallet.loan_card_fin",
                    String.format(Locale.US, "%.1f", loan.principalCbx()),
                    String.format(Locale.US, "%.0f", loan.interestRate()),
                    String.format(Locale.US, "%.1f", loan.totalRepayCbx())
            );
            if (this.font.width(fin) > 190) {
                fin = this.font.plainSubstrByWidth(fin, 184) + "..";
            }
            g.drawString(this.font, fin, textX, cardY + 30, 0xFFFFFFFF);

            // Status Badge / Countdown Timer
            int badgeX = cardX + cardW - 146;
            if ("REPAID".equalsIgnoreCase(loan.status())) {
                g.drawString(this.font, "§a" + AmmoraLang.guiStr("wallet.loan_status_repaid"), badgeX, cardY + 18, 0xFFFFFFFF);
            } else if ("DEFAULTED".equalsIgnoreCase(loan.status())) {
                g.drawString(this.font, "§c" + AmmoraLang.guiStr("wallet.loan_status_defaulted"), badgeX, cardY + 18, 0xFFFFFFFF);
            } else {
                // ACTIVE: show timer
                g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.label_duration"), badgeX, cardY + 12, 0xFFFFFFFF);
                String rem = formatTimeRemaining(loan.expiresAt());
                g.drawString(this.font, rem, badgeX, cardY + 24, 0xFFFFFFFF);
            }
        }

        renderStatusNotification(g, mx + 8, mw - 16, my + mh - 26);

        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private List<ColdWalletDataPayload.LoanItem> getFilteredLoans() {
        List<ColdWalletDataPayload.LoanItem> res = new ArrayList<>();
        if (data != null && data.loans() != null) {
            for (var l : data.loans()) {
                if (loanSubTab == 0 && l.isBorrower()) {
                    res.add(l);
                } else if (loanSubTab == 1 && !l.isBorrower()) {
                    res.add(l);
                }
            }
        }
        return res;
    }

    private ItemStack reconstructLoanStack(ColdWalletDataPayload.LoanItem loan) {
        ItemStack st = ItemStack.EMPTY;
        if (loan.itemNbt() != null && !loan.itemNbt().isEmpty() && this.minecraft != null && this.minecraft.level != null) {
            try {
                net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(loan.itemNbt());
                st = ItemStack.parseOptional(this.minecraft.level.registryAccess(), tag);
            } catch (Exception ignored) {}
        }
        if (st.isEmpty()) {
            try {
                net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(loan.itemId()));
                if (it != net.minecraft.world.item.Items.AIR) {
                    st = new ItemStack(it, loan.itemCount());
                }
            } catch (Exception ignored) {}
        }
        return st;
    }

    private String formatTimeRemaining(long expiresAt) {
        long diff = expiresAt - System.currentTimeMillis();
        if (diff <= 0) {
            return "§c" + AmmoraLang.guiStr("wallet.loan_status_defaulted");
        }
        long seconds = diff / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (hours >= 24) {
            long days = hours / 24;
            hours = hours % 24;
            return "§e" + days + "d " + hours + "h";
        } else if (hours > 0) {
            return "§e" + hours + "h " + minutes + "m";
        } else {
            return "§e" + minutes + "m " + (seconds % 60L) + "s";
        }
    }

    private String translateNotification(String msg) {
        return AmmoraLang.translateNotification(msg);
    }
}
