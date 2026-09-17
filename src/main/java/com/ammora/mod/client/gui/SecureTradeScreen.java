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
        super(Component.literal("Secure P2P Trade"));
        this.data = initialData;
        checkNotification(initialData);
    }

    public void updateData(ClientboundTradeSyncPayload newData) {
        this.data = newData;
        checkNotification(newData);
        if (confirmBtn != null) {
            confirmBtn.setMessage(Component.literal(getConfirmButtonText()));
        }
        if (moneyInputBox != null && !moneyInputBox.isFocused()) {
            moneyInputBox.setValue(String.format(Locale.US, "%.1f", newData.myMoney()));
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
        int col2X = mx + mw - colW - 8;

        // Row 1: Quick money adjustment buttons on the right side of Player A column
        int btnW = 32;
        int btnH = 13;
        int btnY = my + 22;
        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjustMoney(10))
                .bounds(col1X + colW - (btnW * 2) - 8, btnY, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+50"), b -> adjustMoney(50))
                .bounds(col1X + colW - btnW - 4, btnY, btnW, btnH).build());

        // Row 2: Money EditBox on Player A side (dynamically positioned right after label to guarantee 0 overlap)
        String offerLabel = "§a" + AmmoraLang.guiStr("trade.money_offer");
        int labelW = this.font.width(offerLabel);
        int boxX = col1X + 6 + labelW + 4;
        int boxW = Math.max(50, Math.min(80, (col1X + colW - 6) - boxX));
        moneyInputBox = new EditBox(this.font, boxX, my + 37, boxW, 14, AmmoraLang.gui("trade.money_offer"));
        moneyInputBox.setMaxLength(10);
        moneyInputBox.setValue(String.format(Locale.US, "%.1f", data.myMoney()));
        // Note: Do NOT send on every keystroke! Sent on Enter, click, adjust buttons, or lock.
        this.addRenderableWidget(moneyInputBox);

        // Confirm Button for Player A (Bottom of left column)
        int confirmBtnY = my + 170;
        confirmBtn = Button.builder(Component.literal(getConfirmButtonText()), b -> {
            commitMoneyInput();
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
        int rightBorderColor = data.partnerConfirmed() ? COLOR_GREEN : (data.partnerLocked() ? COLOR_AMBER : COLOR_RED);
        g.fill(col2X, my + 20, col2X + colW, my + 20 + colH, COLOR_PANEL);
        renderBorder(g, col2X, my + 20, colW, colH, rightBorderColor);

        // Line 1: Partner Name
        String partnerTitle = AmmoraLang.guiStr("trade.partner", data.partnerName());
        if (this.font.width(partnerTitle) > colW - 12) {
            partnerTitle = this.font.plainSubstrByWidth(partnerTitle, colW - 20) + "..";
        }
        g.drawString(this.font, "§e" + partnerTitle, col2X + 6, my + 25, 0xFFFFFFFF);

        // Line 2: Partner Money display (NO extra colon appended to avoid :: bug)
        g.drawString(this.font, "§a" + AmmoraLang.guiStr("trade.money_offer") + " §f" + String.format(Locale.US, "%.1f", data.partnerMoney()) + " CBX", col2X + 6, my + 40, 0xFFFFFFFF);

        // Slot grid 3x6 (18 slots) for Player B
        renderTradeSlotGrid(g, col2X + 10, my + 54, data.partnerItems(), mouseX, mouseY, false);

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
        } else {
            g.drawString(this.font, "§6⚡ " + AmmoraLang.guiStr("trade.tips_title"), panelX + 6, invSectionY + 7, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_lmb"), panelX + 6, invSectionY + 23, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_shift"), panelX + 6, invSectionY + 39, 0xFFFFFFFF);
            g.drawString(this.font, "§7• " + AmmoraLang.guiStr("trade.tip_lock"), panelX + 6, invSectionY + 55, 0xFFFFFFFF);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Render tooltips for hovered slots
        renderHoveredTooltips(g, col1X + 10, my + 54, data.myItems(), mouseX, mouseY);
        renderHoveredTooltips(g, col2X + 10, my + 54, data.partnerItems(), mouseX, mouseY);
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
            int invSectionY = my + 192;
            int slotSize = 18;

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
        return super.mouseClicked(mouseX, mouseY, button);
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
