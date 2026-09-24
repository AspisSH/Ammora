package com.ammora.mod.client.gui;

import com.ammora.mod.network.PlayerShopDataPayload;
import com.ammora.mod.util.AmmoraLang;
import java.util.Locale;
import com.ammora.mod.network.ServerboundConfigureShopPayload;
import com.ammora.mod.network.ServerboundShopPurchasePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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

import java.util.ArrayList;
import java.util.List;

/**
 * High-tech cyber GUI for player vending machine (Rust-style).
 * Features 10 clean item cards, instant local purchasing, an owner management console,
 * and an interactive player inventory selector for easy product placement.
 */
public class PlayerShopScreen extends Screen {

    private PlayerShopDataPayload data;
    private int selectedSlot = 0;
    private EditBox priceInput;
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0L;
    private boolean showInventoryModal = false;
    private boolean showUpgradesModal = false;
    private EditBox renameInput;

    private static final int COLOR_BG = 0xF5060A12;
    private static final int COLOR_PANEL = 0xEE090E18;
    private static final int COLOR_PANEL_HEADER = 0xF00D1422;
    private static final int COLOR_BORDER_CYAN = 0xFFFF9800; // Industrial Amber / Orange
    private static final int COLOR_BORDER_MUTED = 0xFF182333;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF7E8FA4;

    public PlayerShopScreen(PlayerShopDataPayload initialData) {
        super(Component.translatable("gui.ammora.shop.title"));
        this.data = initialData;
        checkNotification(initialData);
    }

    public void updateData(PlayerShopDataPayload newData) {
        this.data = newData;
        checkNotification(newData);
        rebuildWidgets();
    }

    private void checkNotification(PlayerShopDataPayload d) {
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
        if (data == null) return;

        if (selectedSlot >= data.maxSlots()) {
            selectedSlot = 0;
        }

        int mw = 440, mh = 254;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        if (showUpgradesModal) {
            int modalW = 280, modalH = 224;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                showUpgradesModal = false;
                rebuildWidgets();
            }).bounds(modalX + modalW - 20, modalY + 4, 16, 14).build());

            // Rename edit box & button
            renameInput = new EditBox(this.font, modalX + 12, modalY + 38, 190, 16, Component.literal(AmmoraLang.guiStr("shop.rename_input")));
            renameInput.setMaxLength(32);
            renameInput.setValue(data.shopName());
            this.addRenderableWidget(renameInput);

            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_ok")), b -> {
                String newName = renameInput.getValue();
                if (newName != null && !newName.trim().isEmpty()) {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "RENAME_SHOP", 0, 0, newName.trim()
                    ));
                }
            }).bounds(modalX + 206, modalY + 38, 62, 16).build());

            // Capacity upgrade button
            int curCap = data.slotCapacity();
            if (curCap < 1024) {
                int nextCap = curCap < 128 ? 128 : (curCap < 256 ? 256 : (curCap < 512 ? 512 : 1024));
                double cost = com.ammora.mod.config.AmmoraConfig.getCapacityUpgradeCost(curCap);
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.expand_cap", nextCap, (int)cost)), b -> {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "UPGRADE_CAPACITY", 0, 0, ""
                    ));
                }).bounds(modalX + 12, modalY + 86, modalW - 24, 18).build());
            } else {
                var btn = Button.builder(Component.literal(AmmoraLang.guiStr("shop.cap_max")), b -> {}).bounds(modalX + 12, modalY + 86, modalW - 24, 18).build();
                btn.active = false;
                this.addRenderableWidget(btn);
            }

            // Slot count upgrade button
            int curSlots = data.maxSlots();
            if (curSlots < 10) {
                int nextSlot = curSlots + 1;
                int cost = (int) com.ammora.mod.config.AmmoraConfig.getSlotUnlockCost(nextSlot);
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.unlock_slot", nextSlot, cost)), b -> {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "UNLOCK_SLOT", 0, 0, ""
                    ));
                }).bounds(modalX + 12, modalY + 138, modalW - 24, 18).build());
            } else {
                var btn = Button.builder(Component.literal(AmmoraLang.guiStr("shop.all_slots_unlocked")), b -> {}).bounds(modalX + 12, modalY + 138, modalW - 24, 18).build();
                btn.active = false;
                this.addRenderableWidget(btn);
            }

            // Satellite network module button
            if (!data.networkUnlocked()) {
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.buy_satellite")), b -> {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "UNLOCK_NETWORK", 0, 0, ""
                    ));
                }).bounds(modalX + 12, modalY + 190, modalW - 24, 18).build());
            } else {
                var btn = Button.builder(Component.literal(AmmoraLang.guiStr("shop.satellite_active")), b -> {}).bounds(modalX + 12, modalY + 190, modalW - 24, 18).build();
                btn.active = false;
                this.addRenderableWidget(btn);
            }

            return;
        }

        if (showInventoryModal) {
            int modalW = 210, modalH = 144;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;
            this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                showInventoryModal = false;
                rebuildWidgets();
            }).bounds(modalX + modalW - 20, modalY + 4, 16, 14).build());
            return;
        }

        if (data.isOwner()) {
            // Header buttons for owner on the right side
            int upgradesW = 84;
            int upgradesX = mx + mw - 8 - upgradesW;
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_upgrades_tab")), b -> {
                this.showUpgradesModal = true;
                rebuildWidgets();
            }).bounds(upgradesX, my + 6, upgradesW, 18).build());

            // Company revenue routing button
            boolean hasLinkedComp = data.linkedCompanyName() != null && !data.linkedCompanyName().isEmpty();
            String compShort = hasLinkedComp ? data.linkedCompanyName() : AmmoraLang.guiStr("shop.link_company");
            if (this.font.width(compShort) > 74) {
                compShort = this.font.plainSubstrByWidth(compShort, 66) + "..";
            }
            String compBtnText = (hasLinkedComp ? "§6🏢 " : "§7🏢 ") + compShort;
            int compW = 100;
            int compX = upgradesX - 6 - compW;
            this.addRenderableWidget(Button.builder(Component.literal(compBtnText), b -> {
                PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                        data.shopId(), "TOGGLE_COMPANY", 0, 0, ""
                ));
            }).bounds(compX, my + 6, compW, 18)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    hasLinkedComp ? AmmoraLang.guiStr("shop.linked_company_tooltip", data.linkedCompanyName()) : AmmoraLang.guiStr("shop.link_company_tooltip")
            )))
            .build());

            int footY = my + mh - 48;

            // Price input box
            priceInput = new EditBox(this.font, mx + 102, footY + 4, 52, 16, Component.literal(AmmoraLang.guiStr("shop.price_input")));
            priceInput.setMaxLength(8);
            if (selectedSlot >= 0 && selectedSlot < data.slots().size()) {
                priceInput.setValue(String.format("%.2f", data.slots().get(selectedSlot).priceCbx()).replace(',', '.'));
            }
            this.addRenderableWidget(priceInput);

            // Set price button (sufficient width so text is never truncated)
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_set_price")), b -> {
                try {
                    double p = Double.parseDouble(priceInput.getValue().replace(',', '.'));
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "SET_PRICE", selectedSlot, p, ""
                    ));
                } catch (NumberFormatException ignored) {}
            }).bounds(mx + 158, footY + 4, 76, 16).build());

            // Insert from hand button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_from_hand")), b -> {
                PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                        data.shopId(), "INSERT_HAND", selectedSlot, 0, ""
                ));
            }).bounds(mx + 238, footY + 4, 88, 16).build());

            // Open inventory selector modal button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_inventory")), b -> {
                this.showInventoryModal = true;
                rebuildWidgets();
            }).bounds(mx + 330, footY + 4, 100, 16).build());

            // Extract item back to inventory button
            boolean slotHasItems = selectedSlot >= 0 && selectedSlot < data.slots().size()
                    && data.slots().get(selectedSlot).stockCount() > 0;
            if (slotHasItems) {
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_take_to_inv")), b -> {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "EXTRACT_ITEM", selectedSlot, 0, ""
                    ));
                }).bounds(mx + 10, footY + 24, 126, 16).build());
            }

            // Claim revenue button
            String revText;
            boolean canClaim = data.accumulatedRevenue() > 0.001;
            if (hasLinkedComp && !canClaim) {
                revText = AmmoraLang.guiStr("shop.auto_revenue_company");
            } else {
                revText = AmmoraLang.guiStr("shop.btn_withdraw_rev", data.accumulatedRevenue());
            }
            int claimW = 106;
            int claimX = mx + mw - 10 - claimW;
            var claimBtn = Button.builder(Component.literal(revText), b -> {
                PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                        data.shopId(), "CLAIM_REVENUE", 0, 0, ""
                ));
            }).bounds(claimX, footY + 24, claimW, 16).build();
            claimBtn.active = canClaim;
            if (hasLinkedComp && !canClaim) {
                claimBtn.setTooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("shop.auto_revenue_tooltip", data.linkedCompanyName()))));
            }
            this.addRenderableWidget(claimBtn);

            // Toggle broadcast button
            int bcastW = 100;
            int bcastX = claimX - 6 - bcastW;
            if (data.networkUnlocked()) {
                String bcastText = data.isBroadcast() ? AmmoraLang.guiStr("shop.net_on") : AmmoraLang.guiStr("shop.net_off");
                this.addRenderableWidget(Button.builder(Component.literal(bcastText), b -> {
                    PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                            data.shopId(), "TOGGLE_BROADCAST", 0, 0, ""
                    ));
                }).bounds(bcastX, footY + 24, bcastW, 16).build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("shop.net_locked")), b -> {
                    this.showUpgradesModal = true;
                    rebuildWidgets();
                }).bounds(bcastX, footY + 24, bcastW, 16).build());
            }
        }

        // Action buttons on cards
        int startX = mx + 11;
        int startY = my + 36;
        int cardW = 78;
        int cardH = 76;

        for (int i = 0; i < 10; i++) {
            if (i >= data.maxSlots()) {
                // Locked slot: do not add active buttons
                continue;
            }

            int col = i % 5;
            int row = i / 5;
            int cx = startX + col * (cardW + 7);
            int cy = startY + row * (cardH + 6);
            final int slotIdx = i;

            if (data.isOwner()) {
                // Owner click to select slot
                this.addRenderableWidget(Button.builder(Component.literal(""), b -> {
                    this.selectedSlot = slotIdx;
                    if (priceInput != null && slotIdx < data.slots().size()) {
                        priceInput.setValue(String.format("%.2f", data.slots().get(slotIdx).priceCbx()).replace(',', '.'));
                    }
                    rebuildWidgets();
                }).bounds(cx, cy, cardW, cardH).build()).setAlpha(0.01F);
            } else {
                // Customer buy buttons (1, 16, 64)
                if (slotIdx < data.slots().size()) {
                    var slotItem = data.slots().get(slotIdx);
                    if (slotItem.stockCount() > 0) {
                        int stock = slotItem.stockCount();
                        int btnY = cy + cardH - 18;

                        // 1 pc button
                        double total1 = slotItem.priceCbx();
                        Button btn1 = Button.builder(Component.literal(AmmoraLang.guiStr("shop.btn_buy_1")), b -> {
                            PacketDistributor.sendToServer(new ServerboundShopPurchasePayload(
                                    data.shopId(), slotIdx, 1, false
                            ));
                        }).bounds(cx + 3, btnY, 23, 14)
                          .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("shop.btn_buy_tooltip", String.format(Locale.US, "%.2f", total1)))))
                          .build();
                        btn1.active = stock >= 1;
                        this.addRenderableWidget(btn1);

                        // 16 pcs button
                        double total16 = slotItem.priceCbx() * 16.0;
                        Button btn16 = Button.builder(Component.literal(stock >= 16 ? AmmoraLang.guiStr("shop.btn_buy_16") : "§716"), b -> {
                            PacketDistributor.sendToServer(new ServerboundShopPurchasePayload(
                                    data.shopId(), slotIdx, 16, false
                            ));
                        }).bounds(cx + 28, btnY, 23, 14)
                          .tooltip(Tooltip.create(Component.literal(stock >= 16
                                  ? AmmoraLang.guiStr("shop.btn_buy_n_tooltip", 16, String.format(Locale.US, "%.2f", total16))
                                  : AmmoraLang.guiStr("shop.btn_buy_insufficient", 16))))
                          .build();
                        btn16.active = stock >= 16;
                        this.addRenderableWidget(btn16);

                        // 64 pcs button
                        double total64 = slotItem.priceCbx() * 64.0;
                        Button btn64 = Button.builder(Component.literal(stock >= 64 ? AmmoraLang.guiStr("shop.btn_buy_64") : "§764"), b -> {
                            PacketDistributor.sendToServer(new ServerboundShopPurchasePayload(
                                    data.shopId(), slotIdx, 64, false
                            ));
                        }).bounds(cx + 53, btnY, 23, 14)
                          .tooltip(Tooltip.create(Component.literal(stock >= 64
                                  ? AmmoraLang.guiStr("shop.btn_buy_n_tooltip", 64, String.format(Locale.US, "%.2f", total64))
                                  : AmmoraLang.guiStr("shop.btn_buy_insufficient", 64))))
                          .build();
                        btn64.active = stock >= 64;
                        this.addRenderableWidget(btn64);
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderBlurredBackground(partialTicks);
        }

        // Darkened background backdrop
        gg.fill(0, 0, this.width, this.height, 0xAA000000);

        if (data == null) {
            gg.drawCenteredString(this.font, AmmoraLang.guiStr("shop.loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        if (showUpgradesModal) {
            // Full screen solid dark dim overlay to isolate modal (Zero bleed-through)
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            renderUpgradesModal(gg, mouseX, mouseY);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        if (showInventoryModal) {
            // Full screen solid dark dim overlay to isolate modal (Zero bleed-through)
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            renderInventoryModal(gg, mouseX, mouseY);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        int mw = 440, mh = 254;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Background and border
        gg.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        drawOutlinedBox(gg, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header Panel
        gg.fill(mx + 1, my + 1, mx + mw - 1, my + 30, COLOR_PANEL_HEADER);
        gg.hLine(mx + 1, mx + mw - 1, my + 30, COLOR_BORDER_MUTED);

        // Header Titles
        if (data.isOwner()) {
            String titleStr = "§b⚡ " + truncate(data.shopName(), 24) + (data.isBroadcast() ? " §a●" : " §7○");
            gg.drawString(this.font, titleStr, mx + 10, my + 7, 0xFFFFFFFF);

            String ownerStr = AmmoraLang.guiStr("shop.owner_label", data.ownerName()) + AmmoraLang.guiStr("shop.owner_you");
            String ownerLine = ownerStr + " §8| §e" + String.format(Locale.US, "%.2f CBX", data.buyerBalanceCbx());
            gg.drawString(this.font, ownerLine, mx + 10, my + 18, 0xFFFFFFFF);
        } else {
            String titleStr = "§b⚡ " + truncate(data.shopName(), 32);
            gg.drawString(this.font, titleStr, mx + 10, my + 8, 0xFFFFFFFF);

            String ownerStr = AmmoraLang.guiStr("shop.owner_label", data.ownerName());
            gg.drawString(this.font, ownerStr, mx + 10, my + 19, 0xFFFFFFFF);

            String statusBadge = data.isBroadcast() ? AmmoraLang.guiStr("shop.status_online") : AmmoraLang.guiStr("shop.status_local");
            gg.drawString(this.font, statusBadge, mx + mw - this.font.width(statusBadge) - 10, my + 8, 0xFFFFFFFF);

            String balanceStr = AmmoraLang.guiStr("shop.buyer_balance", String.format(Locale.US, "%.2f", data.buyerBalanceCbx()));
            gg.drawString(this.font, balanceStr, mx + mw - this.font.width(balanceStr) - 10, my + 19, 0xFFFFFFFF);
        }

        // Render 10 Cards
        int startX = mx + 11;
        int startY = my + 36;
        int cardW = 78;
        int cardH = 76;

        PlayerShopDataPayload.ShopSlotItem hoveredSlot = null;

        for (int i = 0; i < 10; i++) {
            int col = i % 5;
            int row = i / 5;
            int cx = startX + col * (cardW + 7);
            int cy = startY + row * (cardH + 6);

            if (i >= data.maxSlots()) {
                gg.fill(cx, cy, cx + cardW, cy + cardH, 0xD0060910);
                drawOutlinedBox(gg, cx, cy, cardW, cardH, 0xFF2A1515);
                gg.drawString(this.font, "§8#" + (i + 1), cx + 4, cy + 4, 0xFFFFFFFF);
                gg.drawCenteredString(this.font, "§c🔒", cx + cardW / 2, cy + 18, 0xFFFFFFFF);
                gg.drawCenteredString(this.font, AmmoraLang.guiStr("shop.slot_closed"), cx + cardW / 2, cy + 34, 0xFFFFFFFF);
                if (data.isOwner()) {
                    gg.drawCenteredString(this.font, AmmoraLang.guiStr("shop.slot_upgrade"), cx + cardW / 2, cy + 48, 0xFFFFFFFF);
                }
                continue;
            }

            boolean isSelected = data.isOwner() && (selectedSlot == i);
            int borderColor = isSelected ? COLOR_BORDER_CYAN : COLOR_BORDER_MUTED;

            gg.fill(cx, cy, cx + cardW, cy + cardH, COLOR_PANEL);
            drawOutlinedBox(gg, cx, cy, cardW, cardH, borderColor);
            if (isSelected) {
                drawOutlinedBox(gg, cx + 1, cy + 1, cardW - 2, cardH - 2, 0x5500D2FF);
            }

            // Slot badge in top-left
            gg.drawString(this.font, (isSelected ? "§b#" : "§8#") + (i + 1), cx + 4, cy + 4, 0xFFFFFFFF);

            if (i < data.slots().size()) {
                var slotItem = data.slots().get(i);

                // Durability percent indicator in card top-right if present
                for (String l : slotItem.lore()) {
                    if (isDurabilityLine(l)) {
                        int openParen = l.lastIndexOf('(');
                        int closeParen = l.lastIndexOf(')');
                        if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                            String durTag = l.substring(openParen + 1, closeParen).replace("§8", "");
                            gg.drawString(this.font, durTag, cx + cardW - this.font.width(durTag) - 4, cy + 4, 0xFFFFFFFF);
                        }
                        break;
                    }
                }

                // Render Item Icon (Centered, clean icon without duplicate count number)
                ItemStack displayStack = ItemStack.EMPTY;
                if (!slotItem.itemId().isEmpty()) {
                    Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slotItem.itemId()));
                    if (it != Items.AIR) {
                        displayStack = new ItemStack(it, 1);
                    }
                }

                if (!displayStack.isEmpty()) {
                    int iconX = cx + (cardW - 16) / 2;
                    int iconY = cy + 12;
                    gg.renderItem(displayStack, iconX, iconY);
                    gg.renderItemDecorations(this.font, displayStack, iconX, iconY, "");
                } else if (data.isOwner()) {
                    gg.drawCenteredString(this.font, AmmoraLang.guiStr("shop.slot_add_item"), cx + cardW / 2, cy + 16, 0xFFFFFFFF);
                }

                // Price Tag
                String priceStr = "§e" + String.format("%.2f", slotItem.priceCbx()) + " CBX";
                gg.drawString(this.font, priceStr, cx + 4, cy + 34, 0xFFFFFFFF);

                // Stock status
                if (slotItem.stockCount() > 0) {
                    gg.drawString(this.font, AmmoraLang.guiStr("shop.slot_pcs", slotItem.stockCount()), cx + 4, cy + 46, 0xFFFFFFFF);
                } else {
                    String outStr = data.isOwner() ? AmmoraLang.guiStr("shop.slot_free") : AmmoraLang.guiStr("shop.slot_out_of_stock");
                    if (this.font.width(outStr) > cardW - 8) {
                        float scale = (float) (cardW - 8) / (float) this.font.width(outStr);
                        gg.pose().pushPose();
                        gg.pose().translate(cx + 4, cy + 46, 0);
                        gg.pose().scale(scale, scale, 1.0F);
                        gg.drawString(this.font, outStr, 0, 0, 0xFFFFFFFF);
                        gg.pose().popPose();
                    } else {
                        gg.drawString(this.font, outStr, cx + 4, cy + 46, 0xFFFFFFFF);
                    }
                }

                // Check Hover (exclude bottom button area when customer buy buttons are present)
                int hoverMaxY = (!data.isOwner() && slotItem.stockCount() > 0) ? (cy + cardH - 20) : (cy + cardH);
                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= hoverMaxY) {
                    hoveredSlot = slotItem;
                }
            }
        }

        // Render Owner console footer
        if (data.isOwner()) {
            int footY = my + mh - 48;
            gg.fill(mx + 1, footY, mx + mw - 1, my + mh - 1, COLOR_PANEL_HEADER);
            gg.hLine(mx + 1, mx + mw - 1, footY, COLOR_BORDER_MUTED);

            String selTitle = AmmoraLang.guiStr("shop.slot_sel_title", selectedSlot + 1);
            gg.drawString(this.font, selTitle, mx + 8, footY + 8, 0xFFFFFFFF);
            gg.drawString(this.font, AmmoraLang.guiStr("shop.slot_price_label"), mx + 66, footY + 8, 0xFFFFFFFF);

            boolean slotHasItems = selectedSlot >= 0 && selectedSlot < data.slots().size()
                    && data.slots().get(selectedSlot).stockCount() > 0;
            if (!slotHasItems) {
                gg.drawString(this.font, AmmoraLang.guiStr("shop.slot_empty_hint"), mx + 10, footY + 28, 0xFFFFFFFF);
            }
        } else {
            // Customer footer: Sales and info
            int footY = my + mh - 26;
            gg.fill(mx + 1, footY, mx + mw - 1, my + mh - 1, COLOR_PANEL_HEADER);
            gg.hLine(mx + 1, mx + mw - 1, footY, COLOR_BORDER_MUTED);
            gg.drawString(this.font, AmmoraLang.guiStr("shop.local_trade_info"), mx + 10, footY + 8, 0xFFFFFFFF);
        }

        super.render(gg, mouseX, mouseY, partialTicks);

        // Notification banner (drawn near top-center so it never covers bottom buttons)
        if (System.currentTimeMillis() < notificationExpireTime && !statusNotification.isEmpty()) {
            int notifW = this.font.width(statusNotification) + 20;
            int nx = (this.width - notifW) / 2;
            int ny = my + 34;
            int boxBg = statusNotificationError ? 0xE6501010 : 0xE60E3A20;
            int boxBorder = statusNotificationError ? COLOR_RED : COLOR_GREEN;
            gg.fill(nx, ny, nx + notifW, ny + 18, boxBg);
            drawOutlinedBox(gg, nx, ny, notifW, 18, boxBorder);
            gg.drawCenteredString(this.font, statusNotification, nx + notifW / 2, ny + 5, 0xFFFFFFFF);
        }

        // Tooltip rendering on hover (rendered after super.render so it's always on top)
        if (!showInventoryModal && !showUpgradesModal && hoveredSlot != null && !hoveredSlot.itemId().isEmpty()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredSlot.displayName()));
            tooltip.add(Component.literal("§7ID: §8" + hoveredSlot.itemId()));
            tooltip.add(Component.literal(AmmoraLang.guiStr("shop.tooltip_price", String.format(Locale.US, "%.2f", hoveredSlot.priceCbx()))));
            tooltip.add(Component.literal(AmmoraLang.guiStr("shop.tooltip_stock", hoveredSlot.stockCount())));
            for (String loreLine : hoveredSlot.lore()) {
                if (isDurabilityLine(loreLine)) {
                    tooltip.add(Component.literal(loreLine));
                } else {
                    tooltip.add(Component.literal("§d" + loreLine));
                }
            }
            gg.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isDurabilityLine(String l) {
        if (l == null) return false;
        String lower = l.toLowerCase(Locale.ROOT);
        return lower.contains("\u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c") || lower.contains("durability") || (lower.contains("(") && lower.contains("%)"));
    }

    private void renderUpgradesModal(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 280, modalH = 224;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // 100% Solid Opaque Modal Background
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);
        drawOutlinedBox(gg, modalX + 1, modalY + 1, modalW - 2, modalH - 2, 0xFF142032);

        // Header
        gg.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + 22, 0xFF0E1626);
        gg.hLine(modalX + 2, modalX + modalW - 2, modalY + 22, 0xFF1C2C44);
        gg.drawString(this.font, AmmoraLang.guiStr("shop.upgrades_header"), modalX + 8, modalY + 7, 0xFFFFFFFF);

        // 1. Rename section
        gg.drawString(this.font, AmmoraLang.guiStr("shop.shop_name_label"), modalX + 12, modalY + 26, 0xFFFFFFFF);

        // 2. Capacity section
        int cap = data.slotCapacity();
        gg.drawString(this.font, AmmoraLang.guiStr("shop.cap_label", cap), modalX + 12, modalY + 60, 0xFFFFFFFF);
        String capDesc = cap >= 1024 ? AmmoraLang.guiStr("shop.cap_maxed") : AmmoraLang.guiStr("shop.cap_next");
        gg.drawString(this.font, capDesc, modalX + 12, modalY + 72, 0xFFFFFFFF);

        // 3. Slot count section
        int slots = data.maxSlots();
        gg.drawString(this.font, AmmoraLang.guiStr("shop.active_slots", slots), modalX + 12, modalY + 112, 0xFFFFFFFF);
        String slotDesc = slots >= 10 ? AmmoraLang.guiStr("shop.all_cells_unlocked") : AmmoraLang.guiStr("shop.next_cell_unlock");
        gg.drawString(this.font, slotDesc, modalX + 12, modalY + 124, 0xFFFFFFFF);

        // 4. Satellite network module
        boolean net = data.networkUnlocked();
        gg.drawString(this.font, AmmoraLang.guiStr("shop.sat_network") + (net ? AmmoraLang.guiStr("shop.sat_installed") : AmmoraLang.guiStr("shop.sat_missing")), modalX + 12, modalY + 164, 0xFFFFFFFF);
        String netDesc = net ? AmmoraLang.guiStr("shop.sat_drones_ready") : AmmoraLang.guiStr("shop.sat_req_remote");
        gg.drawString(this.font, netDesc, modalX + 12, modalY + 176, 0xFFFFFFFF);
    }

    private void renderInventoryModal(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 210, modalH = 144;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Dark dim backdrop over entire screen to prevent any background distractions or shining through
        gg.fill(0, 0, this.width, this.height, 0xDD04070E);

        // 100% Solid Opaque Modal Background (no transparency)
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);
        drawOutlinedBox(gg, modalX + 1, modalY + 1, modalW - 2, modalH - 2, 0xFF142032);

        // Modal Header (100% opaque)
        gg.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + 22, 0xFF0E1626);
        gg.hLine(modalX + 2, modalX + modalW - 2, modalY + 22, 0xFF1C2C44);
        gg.drawString(this.font, AmmoraLang.guiStr("shop.player_inv_header"), modalX + 8, modalY + 7, 0xFFFFFFFF);

        // Subtitle
        gg.drawString(this.font, AmmoraLang.guiStr("shop.click_item_hint", selectedSlot + 1), modalX + 8, modalY + 26, 0xFFFFFFFF);

        // 9x4 Inventory Grid
        int gridStartX = modalX + (modalW - 9 * 18) / 2;
        int gridStartY = modalY + 38;

        ItemStack hoveredInvItem = null;

        if (this.minecraft != null && this.minecraft.player != null) {
            var inv = this.minecraft.player.getInventory();
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = (row < 3) ? (9 + row * 9 + col) : col;
                    int sx = gridStartX + col * 18;
                    int sy = gridStartY + row * 18 + (row == 3 ? 4 : 0);

                    // 100% Opaque Slot Frame
                    gg.fill(sx, sy, sx + 18, sy + 18, 0xFF101928);
                    drawOutlinedBox(gg, sx, sy, 18, 18, 0xFF20324D);

                    ItemStack st = inv.getItem(slotIndex);
                    if (!st.isEmpty()) {
                        gg.renderItem(st, sx + 1, sy + 1);
                        gg.renderItemDecorations(this.font, st, sx + 1, sy + 1);
                    }

                    // Hover check
                    if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                        drawOutlinedBox(gg, sx, sy, 18, 18, COLOR_BORDER_CYAN);
                        if (!st.isEmpty()) {
                            hoveredInvItem = st;
                        }
                    }
                }
            }
        }

        // Render hovered item tooltip inside modal
        if (hoveredInvItem != null && this.minecraft != null && this.minecraft.player != null) {
            gg.renderTooltip(this.font, hoveredInvItem, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showUpgradesModal) {
            int modalW = 280, modalH = 224;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            // Close button [X]
            if (mouseX >= modalX + modalW - 24 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                showUpgradesModal = false;
                rebuildWidgets();
                return true;
            }

            // Click outside closes modal
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                showUpgradesModal = false;
                rebuildWidgets();
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (showInventoryModal) {
            int modalW = 210, modalH = 142;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            // Close button [X]
            if (mouseX >= modalX + modalW - 24 && mouseX <= modalX + modalW - 6 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                showInventoryModal = false;
                rebuildWidgets();
                return true;
            }

            // Click outside closes modal
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                showInventoryModal = false;
                rebuildWidgets();
                return true;
            }

            // 9x4 Inventory Grid click
            int gridStartX = modalX + (modalW - 9 * 18) / 2;
            int gridStartY = modalY + 38;

            if (this.minecraft != null && this.minecraft.player != null) {
                var inv = this.minecraft.player.getInventory();
                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 9; col++) {
                        int slotIndex = (row < 3) ? (9 + row * 9 + col) : col;
                        int sx = gridStartX + col * 18;
                        int sy = gridStartY + row * 18 + (row == 3 ? 4 : 0);

                        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                            ItemStack st = inv.getItem(slotIndex);
                            if (!st.isEmpty()) {
                                PacketDistributor.sendToServer(new ServerboundConfigureShopPayload(
                                        data.shopId(), "INSERT_INVENTORY", selectedSlot, 0, String.valueOf(slotIndex)
                                ));
                                showInventoryModal = false;
                                rebuildWidgets();
                                return true;
                            }
                        }
                    }
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showUpgradesModal) {
            if (keyCode == 256) {
                showUpgradesModal = false;
                rebuildWidgets();
                return true;
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                if (renameInput == null || !renameInput.isFocused()) {
                    showUpgradesModal = false;
                    rebuildWidgets();
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (showInventoryModal) {
            if (keyCode == 256 || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                showInventoryModal = false;
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (priceInput == null || !priceInput.isFocused()) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private void drawOutlinedBox(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.hLine(x, x + w - 1, y, color);
        gg.hLine(x, x + w - 1, y + h - 1, color);
        gg.vLine(x, y, y + h - 1, color);
        gg.vLine(x + w - 1, y, y + h - 1, color);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Overridden to no-op so Minecraft's default Screen#render doesn't apply blur over custom GUI
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
