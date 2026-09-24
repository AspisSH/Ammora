package com.ammora.mod.client.gui;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.network.PurchaseDockDataPayload;
import com.ammora.mod.network.ServerboundUpdatePurchaseDockPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * ASP Cyber-Terminal GUI for Logistics Purchase Dock.
 * Features rank-gated batch sizes and Stop-High price ceiling protection.
 */
public class PurchaseDockScreen extends Screen {

    private PurchaseDockDataPayload data;

    // Active local settings
    private String selectedResourceId;
    private int selectedBatchSize = 1;
    private double selectedMaxBuyPrice = 30.0;

    // Notification banner
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0;

    // Palette tokens (Industrial Amber / ChainBX Rust theme)
    private static final int COLOR_BG = 0xF50D0E12;
    private static final int COLOR_PANEL = 0xF014161C;
    private static final int COLOR_PANEL_HEADER = 0xF01A1D24;
    private static final int COLOR_BORDER_CYAN = 0xFFFF9800; // Industrial Amber / Orange
    private static final int COLOR_BORDER_MUTED = 0xFF2A2724;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9284;

    public PurchaseDockScreen(PurchaseDockDataPayload initialData) {
        super(Component.translatable("gui.ammora.dock.title"));
        this.data = initialData;
        if (initialData != null) {
            this.selectedResourceId = initialData.targetResourceId();
            this.selectedBatchSize = initialData.batchSize();
            this.selectedMaxBuyPrice = initialData.maxBuyPrice();
            checkNewNotification(initialData);
        }
    }

    public void updateData(PurchaseDockDataPayload newData) {
        this.data = newData;
        if (newData != null) {
            this.selectedResourceId = newData.targetResourceId();
            this.selectedBatchSize = newData.batchSize();
            this.selectedMaxBuyPrice = newData.maxBuyPrice();
            checkNewNotification(newData);
        }
        rebuildWidgets();
    }

    private void checkNewNotification(PurchaseDockDataPayload d) {
        if (d != null && d.statusMessage() != null && !d.statusMessage().isEmpty()) {
            this.statusNotification = AmmoraLang.translateNotification(d.statusMessage());
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

        int mw = 440, mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        int leftX = mx + 8;
        int leftW = 142;
        int rightX = mx + 158;
        int rightW = mw - 166;

        // Player Rank badge on right
        String rankTitle = getRankTitle(data.repLevel());
        String rankBadge = AmmoraLang.guiStr("dock.rank_badge", rankTitle, data.repLevel());
        int rankW = this.font.width(rankBadge);
        int rankX = mx + mw - 8 - rankW;

        // Corporate Billing toggle in header (placed strictly to the left of rank badge with zero overlap)
        int compBtnW = 100;
        int compBtnX = rankX - compBtnW - 8;
        boolean linked = data.isCompanyLinked();
        String compName = (data.companyName() != null && !data.companyName().isEmpty()) ? data.companyName() : AmmoraLang.guiStr("account.company");
        if (this.font.width(compName) > 68) {
            compName = this.font.plainSubstrByWidth(compName, 60) + "..";
        }
        String compBtnText = (linked ? "§6🏢 " : "§7👤 ") + compName;
        this.addRenderableWidget(Button.builder(Component.literal(compBtnText), b -> {
            PacketDistributor.sendToServer(new ServerboundUpdatePurchaseDockPayload(
                    data.pos(),
                    selectedResourceId,
                    selectedBatchSize,
                    selectedMaxBuyPrice,
                    true
            ));
        }).bounds(compBtnX, my + 4, compBtnW, 16)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                linked ? AmmoraLang.guiStr("dock.billing_company_tooltip", data.companyName()) : AmmoraLang.guiStr("dock.billing_personal_tooltip")
        )))
        .build());

        // Resource selection buttons on the left
        int resY = my + 38;
        if (data.availableResources() != null) {
            for (int i = 0; i < Math.min(8, data.availableResources().size()); i++) {
                var res = data.availableResources().get(i);
                int btnY = resY + (i * 20);
                boolean isSelected = res.resourceId().equals(selectedResourceId);
                String rawName = getShortName(res.resourceId(), res.displayName());
                int maxTextW = leftW - 24;
                if (this.font.width(rawName) > maxTextW) {
                    rawName = this.font.plainSubstrByWidth(rawName, maxTextW - 6) + "..";
                }
                String label = (isSelected ? "§b▶ " : "") + rawName;

                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    this.selectedResourceId = res.resourceId();
                    rebuildWidgets();
                }).bounds(leftX, btnY, leftW, 18).build());
            }
        }

        // Batch size buttons on the right
        int repLevel = data.repLevel();
        int batchY = my + 78;
        int[] batchSizes = {1, 4, 8, 16, 32, 64};
        int btnW = (rightW - 10) / 6;

        for (int i = 0; i < batchSizes.length; i++) {
            int sz = batchSizes[i];
            boolean isLocked = false;
            if (sz > 16 && repLevel < 4) isLocked = true; // Rank IV Investor
            else if (sz > 1 && repLevel < 2) isLocked = true; // Rank II Trader

            int bx = rightX + (i * (btnW + 2));
            String title = isLocked ? "🔒" : String.valueOf(sz);
            boolean isSel = (selectedBatchSize == sz);

            Button btn = Button.builder(Component.literal(isSel ? "§b§l" + title : title), b -> {
                this.selectedBatchSize = sz;
                rebuildWidgets();
            }).bounds(bx, batchY, btnW, 16).build();

            btn.active = !isLocked;
            this.addRenderableWidget(btn);
        }

        // Price ceiling adjustment buttons (Rank III Broker)
        int guardBoxY = my + 95;
        int priceBtnY = guardBoxY + 27;
        if (repLevel >= 3) {
            int pW = 34;
            this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> adjustMaxPrice(-10.0)).bounds(rightX + 4, priceBtnY, pW, 15).build());
            this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> adjustMaxPrice(-1.0)).bounds(rightX + 42, priceBtnY, 28, 15).build());
            this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> adjustMaxPrice(1.0)).bounds(rightX + 74, priceBtnY, 28, 15).build());
            this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjustMaxPrice(10.0)).bounds(rightX + 106, priceBtnY, pW, 15).build());

            // Market +10% quick align button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("dock.btn_market_plus10")), b -> {
                double spot = getCurrentSelectedSpot();
                this.selectedMaxBuyPrice = MarketEngine.round2(spot * 1.10);
            }).bounds(rightX + 144, priceBtnY, rightW - 148, 15).build());
        }

        // Save & Apply Button
        int applyY = my + mh - 26;
        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("dock.btn_save_settings")), b -> {
            applySettings();
        }).bounds(rightX, applyY, rightW, 18).build());
    }


    private void adjustMaxPrice(double delta) {
        this.selectedMaxBuyPrice = Math.max(0.1, MarketEngine.round2(this.selectedMaxBuyPrice + delta));
    }

    private double getCurrentSelectedSpot() {
        if (data == null || data.availableResources() == null) return 10.0;
        for (var r : data.availableResources()) {
            if (r.resourceId().equals(selectedResourceId)) {
                return r.spotPrice();
            }
        }
        return data.spotPrice();
    }

    private void applySettings() {
        if (data == null || data.pos() == null) return;
        PacketDistributor.sendToServer(new ServerboundUpdatePurchaseDockPayload(
                data.pos(),
                selectedResourceId,
                selectedBatchSize,
                selectedMaxBuyPrice
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
            g.drawCenteredString(this.font, AmmoraLang.guiStr("dock.loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }


        int mw = 440, mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Main window container
        g.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header Panel
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 24, COLOR_PANEL_HEADER);
        g.fill(mx + 1, my + 24, mx + mw - 1, my + 25, COLOR_BORDER_MUTED);

        g.drawString(this.font, AmmoraLang.guiStr("dock.title"), mx + 8, my + 5, 0xFFFFFFFF);
        String sub = AmmoraLang.guiStr("dock.owner_balance", data.ownerName(), MarketEngine.round2(data.userBalanceCbx()));
        g.drawString(this.font, sub, mx + 8, my + 15, 0xFFFFFFFF);

        // Player Rank badge
        String rankTitle = getRankTitle(data.repLevel());
        String rankBadge = AmmoraLang.guiStr("dock.rank_badge", rankTitle, data.repLevel());
        g.drawString(this.font, rankBadge, mx + mw - 8 - this.font.width(rankBadge), my + 5, 0xFFFFFFFF);

        int leftX = mx + 8;
        int leftW = 142;
        int rightX = mx + 158;
        int rightW = mw - 166;

        // Left section: Resource list title
        g.drawString(this.font, AmmoraLang.guiStr("dock.res_list"), leftX, my + 28, 0xFFFFFFFF);

        // Render icons next to resources
        if (data.availableResources() != null) {
            int resY = my + 38;
            for (int i = 0; i < Math.min(8, data.availableResources().size()); i++) {
                var res = data.availableResources().get(i);
                int btnY = resY + (i * 20);
                Item item = getItem(res.resourceId());
                if (item != null) {
                    g.renderItem(new ItemStack(item), leftX + leftW - 18, btnY + 1);
                }
            }
        }

        // Right section: Active Target Resource Display
        g.drawString(this.font, AmmoraLang.guiStr("dock.params_title"), rightX, my + 28, 0xFFFFFFFF);

        // Selected resource card
        g.fill(rightX, my + 38, rightX + rightW, my + 64, COLOR_PANEL);
        renderBorder(g, rightX, my + 38, rightW, 26, COLOR_BORDER_MUTED);

        Item targetItem = getItem(selectedResourceId);
        if (targetItem != null) {
            g.renderItem(new ItemStack(targetItem), rightX + 5, my + 43);
        }
        double currentSpot = getCurrentSelectedSpot();
        g.drawString(this.font, AmmoraLang.guiStr("dock.product_line", getShortName(selectedResourceId, selectedResourceId)), rightX + 26, my + 42, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("dock.spot_cycle_line", String.format(Locale.US, "%.1f", currentSpot), String.format(Locale.US, "%.1f", currentSpot * selectedBatchSize)), rightX + 26, my + 52, 0xFFFFFFFF);

        // Batch Size section
        g.drawString(this.font, AmmoraLang.guiStr("dock.batch_line", selectedBatchSize), rightX, my + 68, 0xFFFFFFFF);

        // Price Ceiling / Stop-High Protection section
        int guardBoxY = my + 95;
        int guardBoxH = 61;
        g.fill(rightX, guardBoxY, rightX + rightW, guardBoxY + guardBoxH, COLOR_PANEL);

        int repLevel = data.repLevel();
        if (repLevel < 3) {
            // LOCKED state for ranks 1 and 2
            renderBorder(g, rightX, guardBoxY, rightW, guardBoxH, 0xFF3E4F66);
            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_locked_1"), rightX + 6, guardBoxY + 8, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_locked_2"), rightX + 6, guardBoxY + 22, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_locked_3"), rightX + 6, guardBoxY + 34, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_locked_4"), rightX + 6, guardBoxY + 46, COLOR_TEXT_MUTED);
        } else {
            // UNLOCKED state for Rank III Broker and above
            boolean isWarning = currentSpot > selectedMaxBuyPrice;
            renderBorder(g, rightX, guardBoxY, rightW, guardBoxH, isWarning ? COLOR_AMBER : COLOR_GREEN);

            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_unlocked_1"), rightX + 6, guardBoxY + 5, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("dock.sh_unlocked_2", String.format(Locale.US, "%.1f", selectedMaxBuyPrice)), rightX + 6, guardBoxY + 16, 0xFFFFFFFF);

            if (isWarning) {
                g.drawString(this.font, AmmoraLang.guiStr("dock.sh_paused"), rightX + 6, guardBoxY + 47, 0xFFFFFFFF);
            } else {
                g.drawString(this.font, AmmoraLang.guiStr("dock.sh_ok"), rightX + 6, guardBoxY + 47, 0xFFFFFFFF);
            }
        }

        // Logistics hint
        int hintY = my + 162;
        g.drawString(this.font, AmmoraLang.guiStr("dock.hint_1"), rightX, hintY, COLOR_TEXT_MUTED);
        g.drawString(this.font, AmmoraLang.guiStr("dock.hint_2"), rightX, hintY + 11, COLOR_TEXT_MUTED);

        // Status Notification Banner
        renderStatusNotification(g, rightX, rightW, my + mh - 44);


        super.render(g, mouseX, mouseY, partialTick);
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

    private Item getItem(String id) {
        if (id == null) return Items.AIR;
        try {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        } catch (Exception e) {
            return Items.AIR;
        }
    }

    private String getShortName(String resId, String defaultName) {
        String key = "commodity.ammora." + resId.replace(':', '.');
        String localized = com.ammora.mod.util.AmmoraLang.str(key);
        if (!key.equals(localized)) {
            return localized;
        }
        return defaultName;
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
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

