package com.ammora.mod.client.gui;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.network.ClientboundTradeSyncPayload;
import com.ammora.mod.network.ServerboundTradeActionPayload;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * ASP Secure P2P Trade Screen.
 * Layout strictly matches the design diagram:
 * - Left column: "Player A" (You) with money input, 3x6 (18) slots grid, and "Confirm Trade" button.
 * - Right column: "Player B" (Partner) with money display, 3x6 (18) slots grid, and partner status indicator.
 * - Bottom area: Player's inventory slots (36 items) on left, info & notifications panel on right (0 overlap).
 * - Full anti-scam indicators and automatic lock reset on modifications.
 */
public class SecureTradeScreen extends Screen {

    private ClientboundTradeSyncPayload data;

    private EditBox moneyInputBox;
    private EditBox rateInputBox;
    private Button confirmBtn;
    private Button cancelBtn;

    // Palette tokens (Industrial Amber / Rust & Create theme)
    private static final int COLOR_BG = 0xF50D0E12;
    private static final int COLOR_PANEL = 0xF014161C;
    private static final int COLOR_PANEL_HEADER = 0xF01A1D24;
    private static final int COLOR_BORDER_AMBER = 0xFFFF9800;
    private static final int COLOR_BORDER_MUTED = 0xFF2A2724;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9284;

    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0;

    public SecureTradeScreen(ClientboundTradeSyncPayload initialData) {
        super(Component.translatable("gui.ammora.trade.title"));
        this.data = initialData;
        checkNotification(initialData);
    }

    public void updateData(ClientboundTradeSyncPayload newData) {
        boolean rebuild = (this.data == null) ||
                (this.data.isLoanMode() != newData.isLoanMode()) ||
                (this.data.isPlayerALender() != newData.isPlayerALender());
        this.data = newData;
        checkNotification(newData);
        if (rebuild) {
            rebuildWidgets();
        } else {
            if (confirmBtn != null) {
                confirmBtn.setMessage(Component.literal(getConfirmButtonText()));
            }
            if (moneyInputBox != null && !moneyInputBox.isFocused()) {
                moneyInputBox.setValue(String.format(Locale.US, "%.1f", newData.myMoney()));
            }
            if (rateInputBox != null && !rateInputBox.isFocused()) {
                rateInputBox.setValue(String.format(Locale.US, "%.0f", newData.interestRate()));
            }
        }
    }

    private void checkNotification(ClientboundTradeSyncPayload d) {
        if (d != null && d.statusMessage() != null && !d.statusMessage().isEmpty()) {
            this.statusNotification = AmmoraLang.translateNotification(d.statusMessage());
            this.statusNotificationError = d.isErrorMessage();
            this.notificationExpireTime = System.currentTimeMillis() + 4500L;
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        statusNotificationError ? SoundEvents.VILLAGER_NO : SoundEvents.EXPERIENCE_ORB_PICKUP,
                        1.2F
                ));
            }
        }
    }

    private String getConfirmButtonText() {
        if (data == null) return "";
        if (!data.myLocked()) {
            return AmmoraLang.guiStr("trade.btn_lock");
        } else if (!data.myConfirmed()) {
            return data.partnerLocked()
                    ? AmmoraLang.guiStr("trade.btn_confirm")
                    : AmmoraLang.guiStr("trade.status_waiting");
        } else {
            return AmmoraLang.guiStr("trade.status_confirmed");
        }
    }

    @Override
    protected void init() {
        super.init();
        if (data == null) return;

        int mw = 420, mh = 286;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        int colW = 196;
        int col1X = mx + 8;

        // Mode switch button in header (right before Cancel button)
        int modeBtnW = 90;
        int modeBtnX = mx + mw - 24 - modeBtnW - 6;
        String modeText = (data.isLoanMode() ? "§6◆ " : "§a◆ ") + (data.isLoanMode() ? AmmoraLang.guiStr("trade.mode_loan") : AmmoraLang.guiStr("trade.mode_trade"));
        this.addRenderableWidget(Button.builder(Component.literal(modeText), b -> {
            commitMoneyInput();
            commitRateInput();
            PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_TRADE_MODE", data.isLoanMode() ? 0 : 1, 0));
        }).bounds(modeBtnX, my + 2, modeBtnW, 14)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(AmmoraLang.guiStr("trade.mode_tooltip"))))
        .build());

        boolean amILender = data.isLoanMode() && (data.isPlayerA() ? data.isPlayerALender() : !data.isPlayerALender());

        if (data.isLoanMode()) {
            if (!amILender) {
                // Borrower: button to switch to becoming Lender
                int btnW = 96;
                this.addRenderableWidget(Button.builder(Component.literal("§6" + AmmoraLang.guiStr("trade.btn_become_lender")), b -> {
                    PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_LOAN_ROLE", 1, 0));
                }).bounds(col1X + colW - btnW - 4, my + 22, btnW, 13).build());
            } else {
                // Lender: Quick money adjustment buttons
                int btnW = 28;
                int btnH = 13;
                int btnY = my + 22;
                int btn2X = col1X + colW - btnW - 4;
                int btn1X = btn2X - btnW - 3;
                this.addRenderableWidget(Button.builder(Component.literal("+50"), b -> adjustMoney(50))
                        .bounds(btn1X, btnY, btnW, btnH).build());
                this.addRenderableWidget(Button.builder(Component.literal("+200"), b -> adjustMoney(200))
                        .bounds(btn2X, btnY, btnW, btnH).build());

                // Money EditBox on Lender side
                String offerLabel = "§a" + AmmoraLang.guiStr("trade.money_offer");
                int labelW = this.font.width(offerLabel);
                int boxX = col1X + 6 + labelW + 4;
                int boxW = Math.max(46, Math.min(76, (col1X + colW - 6) - boxX));
                moneyInputBox = new EditBox(this.font, boxX, my + 37, boxW, 14, AmmoraLang.gui("trade.money_offer"));
                moneyInputBox.setMaxLength(10);
                moneyInputBox.setValue(String.format(Locale.US, "%.1f", data.myMoney()));
                this.addRenderableWidget(moneyInputBox);

                // Rate % EditBox and preset buttons
                String rLabel = "§e" + AmmoraLang.guiStr("trade.label_rate");
                int rLabelW = this.font.width(rLabel);
                int rBoxX = col1X + 6 + rLabelW + 4;
                rateInputBox = new EditBox(this.font, rBoxX, my + 54, 26, 13, Component.literal("Rate"));
                rateInputBox.setMaxLength(5);
                rateInputBox.setValue(String.format(Locale.US, "%.0f", data.interestRate()));
                rateInputBox.setResponder(val -> commitRateInput());
                this.addRenderableWidget(rateInputBox);

                int presetW = 24;
                int curRate = (int) Math.round(data.interestRate());
                this.addRenderableWidget(Button.builder(Component.literal((curRate == 10 ? "§6" : "") + "10%"), b -> setRate(10)).bounds(rBoxX + 36, my + 54, presetW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((curRate == 15 ? "§6" : "") + "15%"), b -> setRate(15)).bounds(rBoxX + 63, my + 54, presetW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((curRate == 25 ? "§6" : "") + "25%"), b -> setRate(25)).bounds(rBoxX + 90, my + 54, presetW, 13).build());

                // Duration selector buttons (6h, 12h, 24h, 3d, 7d)
                int durY = my + 70;
                int dBtnW = 32;
                int gap = 3;
                int startX = col1X + 12;
                String d6 = AmmoraLang.guiStr("trade.dur_6h");
                String d12 = AmmoraLang.guiStr("trade.dur_12h");
                String d24 = AmmoraLang.guiStr("trade.dur_24h");
                String d3d = AmmoraLang.guiStr("trade.dur_3d");
                String d7d = AmmoraLang.guiStr("trade.dur_7d");
                this.addRenderableWidget(Button.builder(Component.literal((data.durationHours() == 6 ? "§6" : "") + d6), b -> setDuration(6)).bounds(startX, durY, dBtnW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((data.durationHours() == 12 ? "§6" : "") + d12), b -> setDuration(12)).bounds(startX + (dBtnW + gap), durY, dBtnW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((data.durationHours() == 24 ? "§6" : "") + d24), b -> setDuration(24)).bounds(startX + (dBtnW + gap) * 2, durY, dBtnW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((data.durationHours() == 72 ? "§6" : "") + d3d), b -> setDuration(72)).bounds(startX + (dBtnW + gap) * 3, durY, dBtnW, 13).build());
                this.addRenderableWidget(Button.builder(Component.literal((data.durationHours() == 168 ? "§6" : "") + d7d), b -> setDuration(168)).bounds(startX + (dBtnW + gap) * 4, durY, dBtnW, 13).build());
            }
        } else {
            // Normal Trade Mode:
            // Row 1: Quick money adjustment buttons on the right side of Player A column
            int btnW = 32;
            int btnH = 13;
            int btnY = my + 22;
            this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjustMoney(10))
                    .bounds(col1X + colW - (btnW * 2) - 8, btnY, btnW, btnH).build());
            this.addRenderableWidget(Button.builder(Component.literal("+50"), b -> adjustMoney(50))
                    .bounds(col1X + colW - btnW - 4, btnY, btnW, btnH).build());

            // Row 2: Money EditBox on Player A side
            String offerLabel = "§a" + AmmoraLang.guiStr("trade.money_offer");
            int labelW = this.font.width(offerLabel);
            int boxX = col1X + 6 + labelW + 4;
            int boxW = Math.max(50, Math.min(80, (col1X + colW - 6) - boxX));
            moneyInputBox = new EditBox(this.font, boxX, my + 37, boxW, 14, AmmoraLang.gui("trade.money_offer"));
            moneyInputBox.setMaxLength(10);
            moneyInputBox.setValue(String.format(Locale.US, "%.1f", data.myMoney()));
            this.addRenderableWidget(moneyInputBox);
        }

        // Confirm Button for Player A (Bottom of left column)
        int confirmBtnY = my + 170;
        confirmBtn = Button.builder(Component.literal(getConfirmButtonText()), b -> {
            commitMoneyInput();
            commitRateInput();
            if (!data.myLocked()) {
                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("TOGGLE_LOCK", 0, 0));
            } else if (!data.myConfirmed()) {
                if (data.partnerLocked()) {
                    PacketDistributor.sendToServer(new ServerboundTradeActionPayload("CONFIRM", 0, 0));
                }
            } else {
                // Unlock
                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("TOGGLE_LOCK", 0, 0));
            }
        }).bounds(col1X + 4, confirmBtnY, colW - 8, 16).build();

        this.addRenderableWidget(confirmBtn);

        // Cancel / Exit Button (Top right of header bar)
        cancelBtn = Button.builder(Component.literal("§c✕"), b -> {
            PacketDistributor.sendToServer(new ServerboundTradeActionPayload("CANCEL", 0, 0));
            this.onClose();
        }).bounds(mx + mw - 24, my + 2, 20, 14).build();

        this.addRenderableWidget(cancelBtn);
    }

    private void commitMoneyInput() {
        if (data == null || moneyInputBox == null) return;
        try {
            double amt = Math.max(0.0, MarketEngine.round2(Double.parseDouble(moneyInputBox.getValue().trim())));
            if (Math.abs(amt - data.myMoney()) > 0.001) {
                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_MONEY", 0, amt));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void commitRateInput() {
        if (data == null || rateInputBox == null) return;
        try {
            double rate = Math.max(0.0, Math.min(1000.0, Double.parseDouble(rateInputBox.getValue().trim())));
            if (Math.abs(rate - data.interestRate()) > 0.01) {
                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_INTEREST_RATE", 0, rate));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void setRate(double rate) {
        if (data == null) return;
        PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_INTEREST_RATE", 0, rate));
        if (rateInputBox != null) {
            rateInputBox.setValue(String.format(Locale.US, "%.0f", rate));
        }
    }

    private void setDuration(int hours) {
        if (data == null) return;
        PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_LOAN_DURATION", 0, hours));
    }

    private void adjustMoney(double delta) {
        if (data == null) return;
        double current = data.myMoney();
        if (moneyInputBox != null) {
            try {
                current = Double.parseDouble(moneyInputBox.getValue().trim());
            } catch (NumberFormatException ignored) {}
        }
        double next = Math.max(0.0, MarketEngine.round2(current + delta));
        PacketDistributor.sendToServer(new ServerboundTradeActionPayload("SET_MONEY", 0, next));
        if (moneyInputBox != null) {
            moneyInputBox.setValue(String.format(Locale.US, "%.1f", next));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        if (data == null || !data.activeSession()) {
            g.drawCenteredString(this.font, AmmoraLang.guiStr("trade.waiting_session"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        int mw = 420, mh = 286;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        int colW = 196;
        int colH = 168;
        int col1X = mx + 8;
        int col2X = mx + mw - colW - 8;

        // Window background
        g.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_AMBER);

        // Header Title (Separate from player names to eliminate overlapping)
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 18, COLOR_PANEL_HEADER);
        g.drawString(this.font, "§6§l" + AmmoraLang.guiStr("trade.title"), mx + 8, my + 5, 0xFFFFFFFF);

        // --- LEFT COLUMN: Player A (You) ---
        int leftBorderColor = data.myConfirmed() ? COLOR_GREEN : (data.myLocked() ? COLOR_AMBER : COLOR_RED);
        g.fill(col1X, my + 20, col1X + colW, my + 20 + colH, COLOR_PANEL);
        renderBorder(g, col1X, my + 20, colW, colH, leftBorderColor);

        // --- RIGHT COLUMN: Player B (Partner) ---
        int rightBorderColor = data.partnerConfirmed() ? COLOR_GREEN : (data.partnerLocked() ? COLOR_AMBER : COLOR_RED);
        g.fill(col2X, my + 20, col2X + colW, my + 20 + colH, COLOR_PANEL);
        renderBorder(g, col2X, my + 20, colW, colH, rightBorderColor);

        boolean amILender = data.isLoanMode() && (data.isPlayerA() ? data.isPlayerALender() : !data.isPlayerALender());

        if (data.isLoanMode()) {
            renderLoanColumns(g, col1X, col2X, my, colW, colH, amILender, mouseX, mouseY);
        } else {
            // Line 1: Player Name (bounded so it never overlaps the +10/+50 buttons)
            String myTitle = AmmoraLang.guiStr("trade.you", data.myName());
            int maxTitleW = (col1X + colW - (32 * 2) - 12) - (col1X + 6);
            if (this.font.width(myTitle) > maxTitleW) {
                myTitle = this.font.plainSubstrByWidth(myTitle, maxTitleW - 8) + "..";
            }
            g.drawString(this.font, "§e" + myTitle, col1X + 6, my + 25, 0xFFFFFFFF);

            // Line 2: Money label (NO extra colon appended to avoid :: bug)
            String offerLabel = "§a" + AmmoraLang.guiStr("trade.money_offer");
            g.drawString(this.font, offerLabel, col1X + 6, my + 40, 0xFFFFFFFF);

            // Slot grid 3x6 (18 slots) for Player A
            renderTradeSlotGrid(g, col1X + 10, my + 54, data.myItems(), mouseX, mouseY, true);

            // --- RIGHT COLUMN: Player B (Partner) ---
            String partnerTitle = AmmoraLang.guiStr("trade.partner", data.partnerName());
            if (this.font.width(partnerTitle) > colW - 12) {
                partnerTitle = this.font.plainSubstrByWidth(partnerTitle, colW - 20) + "..";
            }
            g.drawString(this.font, "§e" + partnerTitle, col2X + 6, my + 25, 0xFFFFFFFF);

            g.drawString(this.font, "§a" + AmmoraLang.guiStr("trade.money_offer") + " §f" + String.format(Locale.US, "%.1f", data.partnerMoney()) + " CBX", col2X + 6, my + 40, 0xFFFFFFFF);

            renderTradeSlotGrid(g, col2X + 10, my + 54, data.partnerItems(), mouseX, mouseY, false);
        }

        // Partner status display badge at bottom of right column
        int statusY = my + 170;
        String pStatus = data.partnerConfirmed()
                ? AmmoraLang.guiStr("trade.status_confirmed")
                : (data.partnerLocked()
                    ? AmmoraLang.guiStr("trade.status_locked")
                    : AmmoraLang.guiStr("trade.status_waiting"));
        g.fill(col2X + 4, statusY, col2X + colW - 4, statusY + 16, COLOR_PANEL_HEADER);
        renderBorder(g, col2X + 4, statusY, colW - 8, 16, rightBorderColor);
        g.drawCenteredString(this.font, pStatus, col2X + colW / 2, statusY + 4, 0xFFFFFFFF);

        // --- BOTTOM SECTION: Player Inventory (Left) & Info / Notification Panel (Right) ---
        int invSectionY = my + 192;
        int invSectionH = 88;

        // Left Panel: Player Inventory (9 cols x 4 rows)
        int invBoxW = 186;
        g.fill(mx + 8, invSectionY, mx + 8 + invBoxW, invSectionY + invSectionH, COLOR_PANEL);
        renderBorder(g, mx + 8, invSectionY, invBoxW, invSectionH, COLOR_BORDER_MUTED);

        g.drawString(this.font, AmmoraLang.guiStr("trade.your_inventory"), mx + 12, invSectionY + 4, COLOR_TEXT_MUTED);
        renderPlayerInventoryGrid(g, mx + 12, invSectionY + 14, mouseX, mouseY);

        // Right Panel: Trading Guide & Notifications (Zero overlap with inventory!)
        int panelX = mx + 198;
        int panelW = mw - 206;
        g.fill(panelX, invSectionY, panelX + panelW, invSectionY + invSectionH, COLOR_PANEL);
        renderBorder(g, panelX, invSectionY, panelW, invSectionH, COLOR_BORDER_MUTED);

        boolean hasNotification = statusNotification != null && !statusNotification.isEmpty() && System.currentTimeMillis() < notificationExpireTime;
        if (hasNotification) {
            int notifBg = statusNotificationError ? 0xEE4A121A : 0xEE0D3B20;
            int notifBorder = statusNotificationError ? COLOR_RED : COLOR_GREEN;
            g.fill(panelX + 3, invSectionY + 3, panelX + panelW - 3, invSectionY + invSectionH - 3, notifBg);
            renderBorder(g, panelX + 3, invSectionY + 3, panelW - 6, invSectionH - 6, notifBorder);

            g.drawString(this.font, (statusNotificationError ? "§c✖ " : "§a✔ ") + AmmoraLang.guiStr("trade.notification_title"), panelX + 8, invSectionY + 8, 0xFFFFFFFF);
            var lines = this.font.split(Component.literal((statusNotificationError ? "§c" : "§a") + statusNotification), panelW - 16);
            int ty = invSectionY + 24;
            for (var line : lines) {
                g.drawString(this.font, line, panelX + 8, ty, 0xFFFFFFFF);
                ty += 11;
                if (ty > invSectionY + invSectionH - 12) break;
            }
        } else if (data.isLoanMode()) {
            g.drawString(this.font, "§6★ " + AmmoraLang.guiStr("trade.tips_title"), panelX + 6, invSectionY + 7, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.loan_tip_1"), panelX + 6, invSectionY + 23, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.loan_tip_2"), panelX + 6, invSectionY + 39, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.loan_tip_3"), panelX + 6, invSectionY + 55, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.loan_tip_4"), panelX + 6, invSectionY + 71, 0xFFFFFFFF);
        } else {
            g.drawString(this.font, "§6★ " + AmmoraLang.guiStr("trade.tips_title"), panelX + 6, invSectionY + 7, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_lmb"), panelX + 6, invSectionY + 23, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_shift"), panelX + 6, invSectionY + 39, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_lock"), panelX + 6, invSectionY + 55, 0xFFFFFFFF);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Render tooltips for hovered slots
        if (data.isLoanMode()) {
            int boxW = 160;
            int slotSize = 28;
            if (amILender) {
                int boxX = col2X + (colW - boxW) / 2;
                int csX = boxX + (boxW - slotSize) / 2;
                int csY = my + 54 + 22;
                if (mouseX >= csX && mouseX < csX + slotSize && mouseY >= csY && mouseY < csY + slotSize) {
                    if (data.partnerItems() != null && !data.partnerItems().isEmpty() && !data.partnerItems().get(0).isEmpty()) {
                        g.renderTooltip(this.font, data.partnerItems().get(0), mouseX, mouseY);
                    }
                }
            } else {
                int boxX = col1X + (colW - boxW) / 2;
                int csX = boxX + (boxW - slotSize) / 2;
                int csY = my + 54 + 22;
                if (mouseX >= csX && mouseX < csX + slotSize && mouseY >= csY && mouseY < csY + slotSize) {
                    if (data.myItems() != null && !data.myItems().isEmpty() && !data.myItems().get(0).isEmpty()) {
                        g.renderTooltip(this.font, data.myItems().get(0), mouseX, mouseY);
                    }
                }
            }
        } else {
            renderHoveredTooltips(g, col1X + 10, my + 54, data.myItems(), mouseX, mouseY);
            renderHoveredTooltips(g, col2X + 10, my + 54, data.partnerItems(), mouseX, mouseY);
        }
        renderPlayerInventoryTooltips(g, mx + 12, invSectionY + 14, mouseX, mouseY);
    }

    private void renderTradeSlotGrid(GuiGraphics g, int startX, int startY, List<ItemStack> items, int mouseX, int mouseY, boolean isMine) {
        int slotSize = 18;
        // 3 columns x 6 rows = 18 slots
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                int sx = startX + (c * (slotSize + 2));
                int sy = startY + (r * (slotSize + 1));

                boolean hovered = mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize;
                g.fill(sx, sy, sx + slotSize, sy + slotSize, hovered ? 0x33FFAA00 : 0xFF17202E);
                renderBorder(g, sx, sy, slotSize, slotSize, hovered ? COLOR_BORDER_AMBER : 0xFF2A3649);

                if (items != null && idx < items.size()) {
                    ItemStack stack = items.get(idx);
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, sx + 1, sy + 1);
                        g.renderItemDecorations(this.font, stack, sx + 1, sy + 1);
                    }
                }
            }
        }
    }

    private void renderPlayerInventoryGrid(GuiGraphics g, int startX, int startY, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        var inv = this.minecraft.player.getInventory();

        int slotSize = 18;
        // Render 4 rows of 9 slots (Main inventory rows 0-2, Hotbar row 3)
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 9; c++) {
                int invIdx = (r == 3) ? c : (r + 1) * 9 + c; // Minecraft standard ordering
                if (invIdx >= inv.items.size()) continue;

                int sx = startX + (c * (slotSize + 2));
                int sy = startY + (r * (slotSize + 1));

                boolean hovered = mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize;
                g.fill(sx, sy, sx + slotSize, sy + slotSize, hovered ? 0x3300E676 : 0xFF141E2D);
                renderBorder(g, sx, sy, slotSize, slotSize, hovered ? COLOR_GREEN : 0xFF233042);

                ItemStack stack = inv.getItem(invIdx);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, sx + 1, sy + 1);
                    g.renderItemDecorations(this.font, stack, sx + 1, sy + 1);
                }
            }
        }
    }

    private void renderHoveredTooltips(GuiGraphics g, int startX, int startY, List<ItemStack> items, int mouseX, int mouseY) {
        if (items == null) return;
        int slotSize = 18;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                if (idx >= items.size()) continue;
                int sx = startX + (c * (slotSize + 2));
                int sy = startY + (r * (slotSize + 1));

                if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                    ItemStack stack = items.get(idx);
                    if (!stack.isEmpty()) {
                        g.renderTooltip(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
    }

    private void renderPlayerInventoryTooltips(GuiGraphics g, int startX, int startY, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        var inv = this.minecraft.player.getInventory();
        int slotSize = 18;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 9; c++) {
                int invIdx = (r == 3) ? c : (r + 1) * 9 + c;
                if (invIdx >= inv.items.size()) continue;
                int sx = startX + (c * (slotSize + 2));
                int sy = startY + (r * (slotSize + 1));

                if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                    ItemStack stack = inv.getItem(invIdx);
                    if (!stack.isEmpty()) {
                        g.renderTooltip(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data != null && data.activeSession()) {
            if (moneyInputBox != null && moneyInputBox.isFocused()) {
                if (!moneyInputBox.isMouseOver(mouseX, mouseY)) {
                    commitMoneyInput();
                }
            }

            int mw = 420, mh = 286;
            int mx = (this.width - mw) / 2;
            int my = (this.height - mh) / 2;
            int col1X = mx + 8;
            int colW = 196;
            int invSectionY = my + 192;
            int slotSize = 18;

            if (data.isLoanMode()) {
                boolean amILender = data.isPlayerA() ? data.isPlayerALender() : !data.isPlayerALender();
                if (!amILender) {
                    // Borrower:
                    // 1. Check click on my Collateral Slot (left column)
                    int boxW = 160;
                    int boxX = col1X + (colW - boxW) / 2;
                    int slotSizeCS = 28;
                    int csX = boxX + (boxW - slotSizeCS) / 2;
                    int csY = my + 54 + 22;

                    if (mouseX >= csX && mouseX < csX + slotSizeCS && mouseY >= csY && mouseY < csY + slotSizeCS) {
                        if (data.myItems() != null && !data.myItems().isEmpty() && !data.myItems().get(0).isEmpty()) {
                            PacketDistributor.sendToServer(new ServerboundTradeActionPayload("REMOVE_ITEM", 0, 0));
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                            }
                            return true;
                        }
                    }

                    // 2. Check click on player inventory to offer as collateral
                    if (this.minecraft != null && this.minecraft.player != null) {
                        var inv = this.minecraft.player.getInventory();
                        int invStartX = mx + 12;
                        int invStartY = invSectionY + 14;
                        for (int r = 0; r < 4; r++) {
                            for (int c = 0; c < 9; c++) {
                                int invIdx = (r == 3) ? c : (r + 1) * 9 + c;
                                if (invIdx >= inv.items.size()) continue;

                                int sx = invStartX + (c * (slotSize + 2));
                                int sy = invStartY + (r * (slotSize + 1));

                                if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                                    if (!inv.getItem(invIdx).isEmpty()) {
                                        PacketDistributor.sendToServer(new ServerboundTradeActionPayload("OFFER_ITEM", invIdx, 0));
                                        if (this.minecraft != null) {
                                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
                                        }
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 1. Check click on my trade slots (remove item: 1 item normally, full stack with Shift)
                int myStartX = col1X + 10;
                int myStartY = my + 54;
                for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < 3; c++) {
                        int idx = r * 3 + c;
                        int sx = myStartX + (c * (slotSize + 2));
                        int sy = myStartY + (r * (slotSize + 1));

                        if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                            if (data.myItems() != null && idx < data.myItems().size() && !data.myItems().get(idx).isEmpty()) {
                                boolean shift = hasShiftDown();
                                int count = shift ? 0 : 1; // 0 = entire stack, 1 = 1 piece
                                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("REMOVE_ITEM", idx, count));
                                if (this.minecraft != null) {
                                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                                }
                                return true;
                            }
                        }
                    }
                }

                // 2. Check click on player inventory slots (offer item: 1 item normally, full stack with Shift)
                if (this.minecraft != null && this.minecraft.player != null) {
                    var inv = this.minecraft.player.getInventory();
                    int invStartX = mx + 12;
                    int invStartY = invSectionY + 14;
                    for (int r = 0; r < 4; r++) {
                        for (int c = 0; c < 9; c++) {
                            int invIdx = (r == 3) ? c : (r + 1) * 9 + c;
                            if (invIdx >= inv.items.size()) continue;

                            int sx = invStartX + (c * (slotSize + 2));
                            int sy = invStartY + (r * (slotSize + 1));

                            if (mouseX >= sx && mouseX < sx + slotSize && mouseY >= sy && mouseY < sy + slotSize) {
                                if (!inv.getItem(invIdx).isEmpty()) {
                                    boolean shift = hasShiftDown();
                                    int count = shift ? 0 : 1; // 0 = entire stack, 1 = 1 piece
                                    PacketDistributor.sendToServer(new ServerboundTradeActionPayload("OFFER_ITEM", invIdx, count));
                                    if (this.minecraft != null) {
                                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPlayerRoleTitle(GuiGraphics g, boolean isYou, String name, String roleName, String roleColor, int x, int y, int maxW) {
        String roleSuffix = " " + roleColor + "[" + roleName + "]";
        int roleW = this.font.width(roleSuffix);
        int maxTitleW = maxW - roleW;

        String baseText = isYou ? AmmoraLang.guiStr("trade.you", name) : AmmoraLang.guiStr("trade.partner", name);
        if (this.font.width(baseText) > maxTitleW) {
            int tagW = this.font.width(isYou ? AmmoraLang.guiStr("trade.you", "") : AmmoraLang.guiStr("trade.partner", ""));
            int availNameW = Math.max(12, maxTitleW - tagW);
            String truncatedName = this.font.plainSubstrByWidth(name, availNameW) + "..";
            baseText = isYou ? AmmoraLang.guiStr("trade.you", truncatedName) : AmmoraLang.guiStr("trade.partner", truncatedName);
        }
        g.drawString(this.font, "§e" + baseText + roleSuffix, x, y, 0xFFFFFFFF);
    }

    private void renderLoanColumns(GuiGraphics g, int col1X, int col2X, int my, int colW, int colH, boolean amILender, int mouseX, int mouseY) {
        int hours = data.durationHours();
        String durStr = (hours >= 72 && hours % 24 == 0)
                ? AmmoraLang.guiStr("trade.days_fmt", hours / 24)
                : AmmoraLang.guiStr("trade.hours_fmt", hours);

        if (amILender) {
            // --- LEFT COLUMN: You (Lender) ---
            int maxTitleW = (col1X + colW - 60 - 4) - (col1X + 6);
            renderPlayerRoleTitle(g, true, data.myName(), AmmoraLang.guiStr("trade.role_lender"), "§6", col1X + 6, my + 25, maxTitleW);

            String offerLabel = "§a" + AmmoraLang.guiStr("trade.money_offer");
            g.drawString(this.font, offerLabel, col1X + 6, my + 40, 0xFFFFFFFF);

            String rLabel = "§e" + AmmoraLang.guiStr("trade.label_rate");
            int rLabelW = this.font.width(rLabel);
            int rBoxX = col1X + 6 + rLabelW + 4;
            g.drawString(this.font, rLabel, col1X + 6, my + 56, 0xFFFFFFFF);
            g.drawString(this.font, "§7%", rBoxX + 28, my + 56, 0xFFFFFFFF);

            // Calculation Card
            int cardX = col1X + 6, cardY = my + 88, cardW = colW - 12, cardH = 76;
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF10141C);
            renderBorder(g, cardX, cardY, cardW, cardH, COLOR_BORDER_MUTED);

            g.drawString(this.font, "§6★ " + AmmoraLang.guiStr("trade.loan_summary"), cardX + 6, cardY + 5, 0xFFFFFFFF);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.loan_amount", "§a" + String.format(Locale.US, "%.1f CBX", data.myMoney())), cardX + 6, cardY + 18, 0xFFFFFFFF);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.label_rate") + " §e" + String.format(Locale.US, "%.0f%%", data.interestRate()) + " §7| §f" + durStr, cardX + 6, cardY + 31, 0xFFFFFFFF);
            g.drawString(this.font, "§6" + AmmoraLang.guiStr("trade.total_repay_lbl", "§e§l" + String.format(Locale.US, "%.1f CBX", data.totalRepayAmount())), cardX + 6, cardY + 45, 0xFFFFFFFF);
            double profit = Math.max(0.0, data.totalRepayAmount() - data.myMoney());
            g.drawString(this.font, "§a" + AmmoraLang.guiStr("trade.loan_profit", String.format(Locale.US, "+%.1f CBX", profit)), cardX + 6, cardY + 59, 0xFFFFFFFF);

            // --- RIGHT COLUMN: Partner (Borrower) ---
            renderPlayerRoleTitle(g, false, data.partnerName(), AmmoraLang.guiStr("trade.role_borrower"), "§b", col2X + 6, my + 25, colW - 12);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.loan_amount", "§a" + String.format(Locale.US, "%.1f CBX", data.myMoney())), col2X + 6, my + 40, 0xFFFFFFFF);

            renderCollateralBox(g, col2X, my + 54, colW, data.partnerItems(), mouseX, mouseY, false);

        } else {
            // --- LEFT COLUMN: You (Borrower) ---
            int maxTitleW = (col1X + colW - 100 - 4) - (col1X + 6);
            renderPlayerRoleTitle(g, true, data.myName(), AmmoraLang.guiStr("trade.role_borrower"), "§b", col1X + 6, my + 25, maxTitleW);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.loan_amount", "§a" + String.format(Locale.US, "%.1f CBX", data.partnerMoney())), col1X + 6, my + 40, 0xFFFFFFFF);

            renderCollateralBox(g, col1X, my + 54, colW, data.myItems(), mouseX, mouseY, true);

            // --- RIGHT COLUMN: Partner (Lender) ---
            renderPlayerRoleTitle(g, false, data.partnerName(), AmmoraLang.guiStr("trade.role_lender"), "§6", col2X + 6, my + 25, colW - 12);

            int cardX = col2X + 6, cardY = my + 42, cardW = colW - 12, cardH = 120;
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF10141C);
            renderBorder(g, cardX, cardY, cardW, cardH, COLOR_BORDER_MUTED);

            g.drawString(this.font, "§6★ " + AmmoraLang.guiStr("trade.loan_summary_cond"), cardX + 6, cardY + 6, 0xFFFFFFFF);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.loan_amount", "§a" + String.format(Locale.US, "%.1f CBX", data.partnerMoney())), cardX + 6, cardY + 22, 0xFFFFFFFF);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.label_rate") + " §e" + String.format(Locale.US, "%.0f%%", data.interestRate()), cardX + 6, cardY + 38, 0xFFFFFFFF);
            g.drawString(this.font, "§7" + AmmoraLang.guiStr("trade.label_duration") + " §f" + durStr, cardX + 6, cardY + 54, 0xFFFFFFFF);
            g.drawString(this.font, "§6§l" + AmmoraLang.guiStr("trade.total_repay_lbl", "§e§l" + String.format(Locale.US, "%.1f CBX", data.totalRepayAmount())), cardX + 6, cardY + 72, 0xFFFFFFFF);
            g.drawString(this.font, "§8" + AmmoraLang.guiStr("trade.loan_tip_2"), cardX + 6, cardY + 92, 0xFFFFFFFF);
            g.drawString(this.font, "§8" + AmmoraLang.guiStr("trade.loan_tip_3"), cardX + 6, cardY + 104, 0xFFFFFFFF);
        }
    }

    private void renderCollateralBox(GuiGraphics g, int colX, int startY, int colW, List<ItemStack> items, int mouseX, int mouseY, boolean isMine) {
        int boxW = 160;
        int boxH = 104;
        int boxX = colX + (colW - boxW) / 2;
        int boxY = startY;
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF0E121A);

        ItemStack colStack = (items != null && !items.isEmpty()) ? items.get(0) : ItemStack.EMPTY;
        int borderColor = !colStack.isEmpty() ? COLOR_AMBER : (isMine ? COLOR_RED : 0xFF2A3445);
        renderBorder(g, boxX, boxY, boxW, boxH, borderColor);

        g.drawCenteredString(this.font, "§6" + AmmoraLang.guiStr("trade.collateral_slot_lbl"), boxX + boxW / 2, boxY + 8, 0xFFFFFFFF);

        int slotSize = 28;
        int slotX = boxX + (boxW - slotSize) / 2;
        int slotY = boxY + 22;
        boolean hovered = mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize;
        g.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, hovered ? 0x44FFAA00 : 0xFF141A24);
        renderBorder(g, slotX, slotY, slotSize, slotSize, hovered ? COLOR_BORDER_AMBER : 0xFF455670);

        if (!colStack.isEmpty()) {
            g.renderItem(colStack, slotX + 6, slotY + 6);
            g.renderItemDecorations(this.font, colStack, slotX + 6, slotY + 6);

            String itemName = colStack.getHoverName().getString();
            if (this.font.width(itemName) > boxW - 12) {
                itemName = this.font.plainSubstrByWidth(itemName, boxW - 20) + "..";
            }
            g.drawCenteredString(this.font, "§f" + itemName, boxX + boxW / 2, boxY + 56, 0xFFFFFFFF);

            if (isMine) {
                g.drawCenteredString(this.font, "§e" + AmmoraLang.guiStr("trade.collateral_click_remove"), boxX + boxW / 2, boxY + 74, 0xFFFFFFFF);
            } else {
                g.drawCenteredString(this.font, "§7" + AmmoraLang.guiStr("trade.loan_tip_2"), boxX + boxW / 2, boxY + 74, 0xFFFFFFFF);
            }
        } else {
            g.drawCenteredString(this.font, "§c" + AmmoraLang.guiStr("trade.no_collateral"), boxX + boxW / 2, boxY + 56, 0xFFFFFFFF);
            if (isMine) {
                g.drawCenteredString(this.font, "§7" + AmmoraLang.guiStr("trade.collateral_hint"), boxX + boxW / 2, boxY + 74, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (moneyInputBox != null && moneyInputBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
                commitMoneyInput();
                moneyInputBox.setFocused(false);
                return true;
            }
        }
        if (this.minecraft != null && (this.minecraft.options.keyInventory.matches(keyCode, scanCode) || keyCode == 256)) {
            if (moneyInputBox == null || !moneyInputBox.isFocused()) {
                PacketDistributor.sendToServer(new ServerboundTradeActionPayload("CANCEL", 0, 0));
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No-op: prevent vanilla Screen#render from applying blur over custom GUI
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
