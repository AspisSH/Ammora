package com.ammora.mod.client.gui;

import com.ammora.mod.network.ClientboundAdminDataPayload;
import com.ammora.mod.network.ServerboundAdminActionPayload;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * High-tech operator administration console for Ammora financial ecosystem.
 * Features:
 * 1. Player balance inspection and direct modifications (Set, Add, Deduct).
 * 2. Instant economic event triggers and active event management.
 * 3. Live AMM rate influence (Base price P0, daily modifiers, liquidity reserves).
 * 4. Comprehensive transaction ledger logs with real-time filtering.
 */
public class AdminScreen extends Screen {

    private static final int COLOR_BG = 0xEE080D18;
    private static final int COLOR_BORDER_CYAN = 0xFF00D2FF;
    private static final int COLOR_BORDER_MUTED = 0xFF1C2C44;
    private static final int COLOR_BORDER_RED = 0xFFFF4040;
    private static final int COLOR_PANEL = 0xFF0E1626;
    private static final int COLOR_PANEL_HEADER = 0xFF142032;
    private static final int COLOR_GREEN = 0xFF2ECC71;
    private static final int COLOR_RED = 0xFFE74C3C;
    private static final int COLOR_GOLD = 0xFFF1C40F;

    private ClientboundAdminDataPayload data;
    private int activeTab = 0; // 0 = Balances, 1 = Events, 2 = Rates, 3 = Logs

    // Toast notification
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0L;

    // Tab 0: Balances
    private EditBox accountSearchBox;
    private String lastAccountSearch = "";
    private int accountPage = 0;
    private ClientboundAdminDataPayload.AdminAccountItem selectedAccount = null;
    private EditBox balanceAmountInput;

    // Tab 1: Events
    private int eventPage = 0;
    private EditBox eventDurationInput;

    // Tab 2: Rates
    private int resourcePage = 0;
    private ClientboundAdminDataPayload.AdminResourceItem selectedResource = null;
    private EditBox resourceBasePriceInput;
    private EditBox resourceModifierInput;
    private EditBox resourceStockInput;

    // Tab 3: Logs
    private EditBox logSearchBox;
    private String lastLogSearch = "";
    private int logPage = 0;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());

    public AdminScreen(ClientboundAdminDataPayload data) {
        super(Component.translatable("gui.ammora.admin.title"));
        this.data = data;
        if (data != null && !data.statusMessage().isEmpty()) {
            triggerNotification(data.statusMessage(), data.isError());
        }
    }

    public void updateData(ClientboundAdminDataPayload newData) {
        this.data = newData;
        if (newData != null && !newData.statusMessage().isEmpty()) {
            triggerNotification(newData.statusMessage(), newData.isError());
        }
        // Resync selected items
        if (selectedAccount != null && data != null) {
            selectedAccount = data.accounts().stream()
                    .filter(a -> a.playerUuid().equals(selectedAccount.playerUuid()))
                    .findFirst().orElse(null);
        }
        if (selectedResource != null && data != null) {
            selectedResource = data.resources().stream()
                    .filter(r -> r.resourceId().equalsIgnoreCase(selectedResource.resourceId()))
                    .findFirst().orElse(null);
        }
        rebuildWidgets();
    }

    private void triggerNotification(String message, boolean error) {
        this.statusNotification = AmmoraLang.translateNotification(message);
        this.statusNotificationError = error;
        this.notificationExpireTime = System.currentTimeMillis() + 3500L;
    }

    @Override
    protected void init() {
        super.init();
        if (data == null) return;

        int mw = 380;
        int mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Header Navigation Tabs
        int tabW = 86;
        int tabH = 16;
        int tabY = my + 26;

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 0 ? "§b▶ " : "§7").append(AmmoraLang.gui("admin.tab_balances")), b -> {
            activeTab = 0;
            rebuildWidgets();
        }).bounds(mx + 10, tabY, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 1 ? "§b▶ " : "§7").append(AmmoraLang.gui("admin.tab_events")), b -> {
            activeTab = 1;
            rebuildWidgets();
        }).bounds(mx + 100, tabY, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 2 ? "§b▶ " : "§7").append(AmmoraLang.gui("admin.tab_rates")), b -> {
            activeTab = 2;
            rebuildWidgets();
        }).bounds(mx + 190, tabY, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 3 ? "§b▶ " : "§7").append(AmmoraLang.gui("admin.tab_logs")), b -> {
            activeTab = 3;
            rebuildWidgets();
        }).bounds(mx + 280, tabY, tabW, tabH).build());

        // Refresh and Close buttons in header
        this.addRenderableWidget(Button.builder(Component.literal("🔄"), b -> {
            PacketDistributor.sendToServer(new ServerboundAdminActionPayload("REFRESH", "", 0, ""));
        }).bounds(mx + mw - 46, my + 5, 18, 16).tooltip(Tooltip.create(AmmoraLang.gui("admin.refresh_tooltip"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> this.onClose())
                .bounds(mx + mw - 24, my + 5, 18, 16).build());

        // Sub-view init
        if (activeTab == 0) {
            initBalancesTab(mx, my, mw, mh);
        } else if (activeTab == 1) {
            initEventsTab(mx, my, mw, mh);
        } else if (activeTab == 2) {
            initRatesTab(mx, my, mw, mh);
        } else if (activeTab == 3) {
            initLogsTab(mx, my, mw, mh);
        }
    }

    // =========================================================================
    // TAB 0: BALANCES
    // =========================================================================
    private void initBalancesTab(int mx, int my, int mw, int mh) {
        accountSearchBox = new EditBox(this.font, mx + 10, my + 46, 75, 14, AmmoraLang.gui("admin.search_player"));
        accountSearchBox.setValue(lastAccountSearch);
        accountSearchBox.setResponder(val -> {
            lastAccountSearch = val;
            accountPage = 0;
            rebuildWidgets();
        });
        this.addRenderableWidget(accountSearchBox);

        var filtered = getFilteredAccounts();
        int totalPages = Math.max(1, (filtered.size() + 5) / 6);
        if (accountPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                accountPage--;
                rebuildWidgets();
            }).bounds(mx + 135, my + 46, 16, 14).build());
        }
        if (accountPage < totalPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                accountPage++;
                rebuildWidgets();
            }).bounds(mx + 154, my + 46, 16, 14).build());
        }

        // Account list selection buttons
        int startIndex = accountPage * 6;
        for (int i = 0; i < 6; i++) {
            int idx = startIndex + i;
            if (idx < filtered.size()) {
                var acc = filtered.get(idx);
                int btnY = my + 64 + i * 20;
                boolean isSel = selectedAccount != null && selectedAccount.playerUuid().equals(acc.playerUuid());
                String label = (isSel ? "§b● " : "§7") + truncate(acc.playerName(), 12) + " §e" + String.format(Locale.US, "%.1f", acc.balanceCbx());
                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    selectedAccount = acc;
                    if (balanceAmountInput != null) {
                        balanceAmountInput.setValue(String.format(Locale.US, "%.2f", acc.balanceCbx()));
                    }
                    rebuildWidgets();
                }).bounds(mx + 10, btnY, 160, 18).build());
            }
        }

        // Right side: Selected Account details & modification controls
        if (selectedAccount != null) {
            int rx = mx + 180;
            int ry = my + 64;

            balanceAmountInput = new EditBox(this.font, rx, ry + 40, 90, 16, AmmoraLang.gui("admin.amount_cbx"));
            balanceAmountInput.setValue(String.format(Locale.US, "%.2f", selectedAccount.balanceCbx()));
            this.addRenderableWidget(balanceAmountInput);

            // Set exact balance
            this.addRenderableWidget(Button.builder(Component.literal("§e").append(AmmoraLang.gui("admin.btn_set")), b -> {
                double val = parseDouble(balanceAmountInput.getValue());
                if (val >= 0) {
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "SET_BALANCE", selectedAccount.playerUuid().toString(), val, selectedAccount.playerName()
                    ));
                }
            }).bounds(rx + 95, ry + 40, 85, 16).build());

            // Add (+) balance
            this.addRenderableWidget(Button.builder(Component.literal("§a+ ").append(AmmoraLang.gui("admin.btn_add")), b -> {
                double val = parseDouble(balanceAmountInput.getValue());
                if (val > 0) {
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "ADD_BALANCE", selectedAccount.playerUuid().toString(), val, selectedAccount.playerName()
                    ));
                }
            }).bounds(rx, ry + 60, 90, 16).build());

            // Deduct (-) balance
            this.addRenderableWidget(Button.builder(Component.literal("§c- ").append(AmmoraLang.gui("admin.btn_sub")), b -> {
                double val = parseDouble(balanceAmountInput.getValue());
                if (val > 0) {
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "SUB_BALANCE", selectedAccount.playerUuid().toString(), val, selectedAccount.playerName()
                    ));
                }
            }).bounds(rx + 95, ry + 60, 85, 16).build());

            // Quick balance presets
            this.addRenderableWidget(Button.builder(Component.literal("+100"), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "ADD_BALANCE", selectedAccount.playerUuid().toString(), 100.0, selectedAccount.playerName()
                ));
            }).bounds(rx, ry + 80, 42, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("+500"), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "ADD_BALANCE", selectedAccount.playerUuid().toString(), 500.0, selectedAccount.playerName()
                ));
            }).bounds(rx + 45, ry + 80, 42, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("+1000"), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "ADD_BALANCE", selectedAccount.playerUuid().toString(), 1000.0, selectedAccount.playerName()
                ));
            }).bounds(rx + 90, ry + 80, 44, 16).build());

            this.addRenderableWidget(Button.builder(AmmoraLang.gui("admin.btn_reset_zero"), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "SET_BALANCE", selectedAccount.playerUuid().toString(), 0.0, selectedAccount.playerName()
                ));
            }).bounds(rx + 137, ry + 80, 43, 16).build());
        }
    }

    private List<ClientboundAdminDataPayload.AdminAccountItem> getFilteredAccounts() {
        if (data == null) return List.of();
        String q = lastAccountSearch.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return data.accounts();
        return data.accounts().stream()
                .filter(a -> a.playerName().toLowerCase(Locale.ROOT).contains(q) || a.playerUuid().toString().contains(q))
                .toList();
    }

    // =========================================================================
    // TAB 1: EVENTS
    // =========================================================================
    private void initEventsTab(int mx, int my, int mw, int mh) {
        // Stop Active Event button
        if (data.activeEventId() != null && !data.activeEventId().isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("§c⏹ ").append(AmmoraLang.gui("admin.btn_stop_early")), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload("STOP_EVENT", "", 0, ""));
            }).bounds(mx + mw - 142, my + 52, 126, 20).build());
        }

        // Duration Input for new events
        eventDurationInput = new EditBox(this.font, mx + 240, my + 82, 35, 14, AmmoraLang.gui("admin.days"));
        eventDurationInput.setValue("3");
        this.addRenderableWidget(eventDurationInput);

        // Template pagination
        var templates = data.eventTemplates();
        int totalPages = Math.max(1, (templates.size() + 2) / 3);
        if (eventPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                eventPage--;
                rebuildWidgets();
            }).bounds(mx + mw - 46, my + 82, 16, 14).build());
        }
        if (eventPage < totalPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                eventPage++;
                rebuildWidgets();
            }).bounds(mx + mw - 26, my + 82, 16, 14).build());
        }

        // Event templates list
        int startIdx = eventPage * 3;
        for (int i = 0; i < 3; i++) {
            int idx = startIdx + i;
            if (idx < templates.size()) {
                var tmpl = templates.get(idx);
                int cardY = my + 102 + i * 40;
                this.addRenderableWidget(Button.builder(Component.literal("§a▶ ").append(AmmoraLang.gui("admin.btn_launch")), b -> {
                    int days = (int) parseDouble(eventDurationInput != null ? eventDurationInput.getValue() : "3");
                    if (days <= 0) days = 3;
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "TRIGGER_EVENT", tmpl.id(), days, tmpl.title()
                    ));
                }).bounds(mx + mw - 85, cardY + 12, 75, 18).build());
            }
        }
    }

    // =========================================================================
    // TAB 2: RATES
    // =========================================================================
    private void initRatesTab(int mx, int my, int mw, int mh) {
        var resources = data.resources();
        int totalPages = Math.max(1, (resources.size() + 4) / 5);
        if (resourcePage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                resourcePage--;
                rebuildWidgets();
            }).bounds(mx + 135, my + 46, 16, 14).build());
        }
        if (resourcePage < totalPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                resourcePage++;
                rebuildWidgets();
            }).bounds(mx + 154, my + 46, 16, 14).build());
        }

        // Resource list selection buttons
        int startIdx = resourcePage * 5;
        for (int i = 0; i < 5; i++) {
            int idx = startIdx + i;
            if (idx < resources.size()) {
                var res = resources.get(idx);
                int btnY = my + 64 + i * 24;
                boolean isSel = selectedResource != null && selectedResource.resourceId().equalsIgnoreCase(res.resourceId());
                String label = (isSel ? "§b● " : "§f") + truncate(res.displayName(), 11) + " §e" + String.format(Locale.US, "%.1f", res.spotPrice());
                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    selectedResource = res;
                    if (resourceBasePriceInput != null) resourceBasePriceInput.setValue(String.format(Locale.US, "%.2f", res.basePrice()));
                    if (resourceModifierInput != null) resourceModifierInput.setValue(String.format(Locale.US, "%.2f", res.dailyModifier()));
                    if (resourceStockInput != null) resourceStockInput.setValue(String.format(Locale.US, "%.0f", res.currentStock()));
                    rebuildWidgets();
                }).bounds(mx + 10, btnY, 160, 22).build());
            }
        }

        // Right side: Selected Resource Controls
        if (selectedResource != null) {
            int rx = mx + 180;
            int ry = my + 46;

            // 1. Base Price P0 (Label at ry + 32, Input & Button at ry + 44)
            resourceBasePriceInput = new EditBox(this.font, rx, ry + 44, 88, 16, AmmoraLang.gui("admin.base_price_p0"));
            resourceBasePriceInput.setValue(String.format(Locale.US, "%.2f", selectedResource.basePrice()));
            this.addRenderableWidget(resourceBasePriceInput);

            this.addRenderableWidget(Button.builder(AmmoraLang.gui("admin.btn_set_p0"), b -> {
                double val = parseDouble(resourceBasePriceInput.getValue());
                if (val > 0) {
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "SET_BASE_PRICE", selectedResource.resourceId(), val, ""
                    ));
                }
            }).bounds(rx + 94, ry + 44, 84, 16).build());

            // 2. Modifier (+/- multiplier) (Label at ry + 66, Input & Button at ry + 78)
            resourceModifierInput = new EditBox(this.font, rx, ry + 78, 88, 16, AmmoraLang.gui("admin.modifier"));
            resourceModifierInput.setValue(String.format(Locale.US, "%.2f", selectedResource.dailyModifier()));
            this.addRenderableWidget(resourceModifierInput);

            this.addRenderableWidget(Button.builder(AmmoraLang.gui("admin.btn_set_mod"), b -> {
                double val = parseDouble(resourceModifierInput.getValue());
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "SET_MODIFIER", selectedResource.resourceId(), val, ""
                ));
            }).bounds(rx + 94, ry + 78, 84, 16).build());

            // 3. Current Stock Pool (S) (Label at ry + 100, Input & Button at ry + 112)
            resourceStockInput = new EditBox(this.font, rx, ry + 112, 88, 16, AmmoraLang.gui("admin.reserve_stock"));
            resourceStockInput.setValue(String.format(Locale.US, "%.0f", selectedResource.currentStock()));
            this.addRenderableWidget(resourceStockInput);

            this.addRenderableWidget(Button.builder(AmmoraLang.gui("admin.btn_set_stock"), b -> {
                double val = parseDouble(resourceStockInput.getValue());
                if (val > 0) {
                    PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                            "SET_STOCK", selectedResource.resourceId(), val, ""
                    ));
                }
            }).bounds(rx + 94, ry + 112, 84, 16).build());

            // Quick reset modifier button (Button at ry + 136)
            this.addRenderableWidget(Button.builder(AmmoraLang.gui("admin.btn_reset_mod"), b -> {
                PacketDistributor.sendToServer(new ServerboundAdminActionPayload(
                        "SET_MODIFIER", selectedResource.resourceId(), 0.0, ""
                ));
            }).bounds(rx, ry + 136, 178, 16).build());
        }
    }

    // =========================================================================
    // TAB 3: LOGS
    // =========================================================================
    private void initLogsTab(int mx, int my, int mw, int mh) {
        logSearchBox = new EditBox(this.font, mx + 10, my + 46, 160, 14, AmmoraLang.gui("admin.search_logs"));
        logSearchBox.setValue(lastLogSearch);
        logSearchBox.setResponder(val -> {
            lastLogSearch = val;
            logPage = 0;
            rebuildWidgets();
        });
        this.addRenderableWidget(logSearchBox);

        var filtered = getFilteredTransactions();
        int totalPages = Math.max(1, (filtered.size() + 6) / 7);
        if (logPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                logPage--;
                rebuildWidgets();
            }).bounds(mx + mw - 46, my + 46, 16, 14).build());
        }
        if (logPage < totalPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                logPage++;
                rebuildWidgets();
            }).bounds(mx + mw - 26, my + 46, 16, 14).build());
        }
    }

    private List<ClientboundAdminDataPayload.AdminTxItem> getFilteredTransactions() {
        if (data == null) return List.of();
        String q = lastLogSearch.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return data.transactions();
        return data.transactions().stream()
                .filter(t -> t.buyerName().toLowerCase(Locale.ROOT).contains(q)
                        || t.sellerName().toLowerCase(Locale.ROOT).contains(q)
                        || t.itemName().toLowerCase(Locale.ROOT).contains(q)
                        || t.itemId().toLowerCase(Locale.ROOT).contains(q)
                        || t.txType().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        // Darkened background backdrop (crisp rendering without blur shader artifacts)
        gg.fill(0, 0, this.width, this.height, 0xCC000000);

        if (data == null) {
            gg.drawCenteredString(this.font, AmmoraLang.guiStr("admin.loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        int mw = 380;
        int mh = 236;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Main Window Frame
        gg.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        drawOutlinedBox(gg, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header
        gg.fill(mx + 1, my + 1, mx + mw - 1, my + 24, COLOR_PANEL_HEADER);
        gg.hLine(mx + 1, mx + mw - 1, my + 24, COLOR_BORDER_MUTED);
        gg.drawString(this.font, "§c✦ " + AmmoraLang.guiStr("admin.header_title") + " ✦ §8(" + AmmoraLang.guiStr("admin.header_operator") + ")", mx + 10, my + 8, 0xFFFFFFFF);

        // Active Tab Rendering
        if (activeTab == 0) {
            renderBalancesTab(gg, mx, my, mw, mh);
        } else if (activeTab == 1) {
            renderEventsTab(gg, mx, my, mw, mh);
        } else if (activeTab == 2) {
            renderRatesTab(gg, mx, my, mw, mh);
        } else if (activeTab == 3) {
            renderLogsTab(gg, mx, my, mw, mh);
        }

        super.render(gg, mouseX, mouseY, partialTicks);

        // Toast Notification Banner
        if (System.currentTimeMillis() < notificationExpireTime && !statusNotification.isEmpty()) {
            int notifW = this.font.width(statusNotification) + 20;
            int nx = (this.width - notifW) / 2;
            int ny = Math.max(4, my - 24);
            int boxBg = statusNotificationError ? 0xE6501010 : 0xE60E3A20;
            int boxBorder = statusNotificationError ? COLOR_RED : COLOR_GREEN;
            gg.fill(nx, ny, nx + notifW, ny + 18, boxBg);
            drawOutlinedBox(gg, nx, ny, notifW, 18, boxBorder);
            gg.drawCenteredString(this.font, statusNotification, nx + notifW / 2, ny + 5, 0xFFFFFFFF);
        }
    }

    private void renderBalancesTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var filtered = getFilteredAccounts();
        int totalPages = Math.max(1, (filtered.size() + 5) / 6);
        String pageStr = "§8" + (accountPage + 1) + "/" + totalPages;
        gg.drawString(this.font, pageStr, mx + 90, my + 49, 0xFFFFFFFF);

        // Right details panel
        int rx = mx + 180;
        int ry = my + 64;
        gg.fill(rx - 5, ry - 18, mx + mw - 10, my + mh - 10, COLOR_PANEL);
        drawOutlinedBox(gg, rx - 5, ry - 18, (mx + mw - 10) - (rx - 5), (my + mh - 10) - (ry - 18), COLOR_BORDER_MUTED);

        if (selectedAccount != null) {
            gg.drawString(this.font, "§e" + AmmoraLang.guiStr("admin.player_label") + ": §f" + selectedAccount.playerName(), rx, ry - 12, 0xFFFFFFFF);
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.balance_label") + ": §a" + String.format(Locale.US, "%.2f CBX", selectedAccount.balanceCbx()), rx, ry + 2, 0xFFFFFFFF);
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.rep_label") + ": §6Lvl " + selectedAccount.repLevel() + " §8(" + selectedAccount.repPoints() + " pts)", rx, ry + 14, 0xFFFFFFFF);
            gg.drawString(this.font, "§8UUID: " + truncate(selectedAccount.playerUuid().toString(), 22), rx, ry + 26, 0xFFFFFFFF);
        } else {
            gg.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("admin.select_player_prompt"), rx + 95, ry + 50, 0xFFFFFFFF);
        }
    }

    private void renderEventsTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        // Active event block
        int activeBoxY = my + 46;
        gg.fill(mx + 10, activeBoxY, mx + mw - 10, activeBoxY + 32, 0x550E1626);
        drawOutlinedBox(gg, mx + 10, activeBoxY, mw - 20, 32, data.activeEventId().isEmpty() ? COLOR_BORDER_MUTED : COLOR_GOLD);

        if (!data.activeEventId().isEmpty()) {
            String actTitle = "§6⚡ " + data.activeEventTitle() + " §8(" + AmmoraLang.guiStr("admin.active_event_left", data.activeEventRemainingDays()) + "§8)";
            gg.drawString(this.font, actTitle, mx + 16, activeBoxY + 6, 0xFFFFFFFF);
            gg.drawString(this.font, "§7" + truncate(data.activeEventDesc(), 34), mx + 16, activeBoxY + 18, 0xFFFFFFFF);
        } else {
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.no_active_events"), mx + 16, activeBoxY + 12, 0xFFFFFFFF);
        }

        // Templates catalog header
        gg.drawString(this.font, "§b" + AmmoraLang.guiStr("admin.event_templates"), mx + 10, my + 85, 0xFFFFFFFF);
        gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.duration_days"), mx + 165, my + 85, 0xFFFFFFFF);

        var templates = data.eventTemplates();
        int startIdx = eventPage * 3;
        for (int i = 0; i < 3; i++) {
            int idx = startIdx + i;
            if (idx < templates.size()) {
                var tmpl = templates.get(idx);
                int cardY = my + 102 + i * 40;
                gg.fill(mx + 10, cardY, mx + mw - 10, cardY + 36, COLOR_PANEL);
                drawOutlinedBox(gg, mx + 10, cardY, mw - 20, 36, COLOR_BORDER_MUTED);

                String sign = tmpl.multiplier() >= 0 ? "+": "";
                String pct = String.format(Locale.US, "%s%.0f%%", sign, tmpl.multiplier() * 100);

                gg.drawString(this.font, "§f" + tmpl.title() + " §8[" + tmpl.id() + "]", mx + 16, cardY + 6, 0xFFFFFFFF);
                gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.event_effect", pct, tmpl.resourceId()), mx + 16, cardY + 18, 0xFFFFFFFF);
            }
        }
    }

    private void renderRatesTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var resources = data.resources();
        int totalPages = Math.max(1, (resources.size() + 4) / 5);
        String pageStr = "§7" + AmmoraLang.guiStr("admin.page_of", (resourcePage + 1), totalPages, resources.size());
        gg.drawString(this.font, pageStr, mx + 12, my + 49, 0xFFFFFFFF);

        // Right details panel
        int rx = mx + 180;
        int ry = my + 46;
        gg.fill(rx - 5, ry, mx + mw - 10, my + mh - 10, COLOR_PANEL);
        drawOutlinedBox(gg, rx - 5, ry, (mx + mw - 10) - (rx - 5), (my + mh - 10) - ry, COLOR_BORDER_MUTED);

        if (selectedResource != null) {
            gg.drawString(this.font, "§b" + selectedResource.displayName() + " §8(" + selectedResource.resourceId() + ")", rx, ry + 6, 0xFFFFFFFF);
            String spotVal = String.format(Locale.US, "%.2f CBX", selectedResource.spotPrice());
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.spot_label", spotVal, (long) selectedResource.currentStock(), (long) selectedResource.targetReserve()), rx, ry + 18, 0xFFFFFFFF);

            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.base_price_label"), rx, ry + 32, 0xFFFFFFFF);
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.modifier_label"), rx, ry + 66, 0xFFFFFFFF);
            gg.drawString(this.font, "§7" + AmmoraLang.guiStr("admin.reserve_label"), rx, ry + 100, 0xFFFFFFFF);
        } else {
            gg.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("admin.select_resource_prompt1"), rx + 89, ry + 70, 0xFFFFFFFF);
            gg.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("admin.select_resource_prompt2"), rx + 89, ry + 84, 0xFFFFFFFF);
        }
    }

    private void renderLogsTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var filtered = getFilteredTransactions();
        int totalPages = Math.max(1, (filtered.size() + 6) / 7);
        String pageStr = "§8" + AmmoraLang.guiStr("admin.logs_page_of", (logPage + 1), totalPages, filtered.size());
        gg.drawString(this.font, pageStr, mx + mw - this.font.width(pageStr) - 52, my + 49, 0xFFFFFFFF);

        // Table Header
        int tableY = my + 64;
        gg.fill(mx + 10, tableY, mx + mw - 10, tableY + 14, COLOR_PANEL_HEADER);
        gg.hLine(mx + 10, mx + mw - 10, tableY + 14, COLOR_BORDER_MUTED);
        gg.drawString(this.font, "§8" + AmmoraLang.guiStr("admin.col_date"), mx + 14, tableY + 3, 0xFFFFFFFF);
        gg.drawString(this.font, "§8" + AmmoraLang.guiStr("admin.col_type"), mx + 78, tableY + 3, 0xFFFFFFFF);
        gg.drawString(this.font, "§8" + AmmoraLang.guiStr("admin.col_parties"), mx + 134, tableY + 3, 0xFFFFFFFF);
        gg.drawString(this.font, "§8" + AmmoraLang.guiStr("admin.col_item"), mx + 210, tableY + 3, 0xFFFFFFFF);
        gg.drawString(this.font, "§8" + AmmoraLang.guiStr("admin.col_total"), mx + mw - 52, tableY + 3, 0xFFFFFFFF);

        int startIdx = logPage * 7;
        for (int i = 0; i < 7; i++) {
            int idx = startIdx + i;
            int rowY = tableY + 16 + i * 20;

            if (idx < filtered.size()) {
                var tx = filtered.get(idx);
                boolean even = (i % 2 == 0);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 19, even ? 0x220E1626 : 0x440E1626);

                String dateStr = dateFormat.format(new Date(tx.timestamp()));
                gg.drawString(this.font, "§7" + dateStr, mx + 14, rowY + 5, 0xFFFFFFFF);

                String typeTag = switch (tx.txType()) {
                    case "LOCAL_BUY" -> "§a" + AmmoraLang.guiStr("admin.tx_shop");
                    case "REMOTE_BUY" -> "§b" + AmmoraLang.guiStr("admin.tx_delivery");
                    case "BUY_REQUEST" -> "§6" + AmmoraLang.guiStr("admin.tx_rfq");
                    case "BUY" -> "§e" + AmmoraLang.guiStr("admin.tx_exchange_buy");
                    case "SELL" -> "§d" + AmmoraLang.guiStr("admin.tx_exchange_sell");
                    default -> "§7" + AmmoraLang.guiStr("admin.tx_default");
                };
                gg.drawString(this.font, typeTag, mx + 78, rowY + 5, 0xFFFFFFFF);

                String parties = truncate(tx.buyerName() + " ➔ " + tx.sellerName(), 13);
                gg.drawString(this.font, "§f" + parties, mx + 134, rowY + 5, 0xFFFFFFFF);

                String itemStr = truncate(tx.itemName() + " x" + tx.amount(), 15);
                gg.drawString(this.font, "§e" + itemStr, mx + 210, rowY + 5, 0xFFFFFFFF);

                String totalStr = String.format(Locale.US, "%.1f", tx.totalCbx());
                gg.drawString(this.font, "§a" + totalStr, mx + mw - this.font.width(totalStr) - 16, rowY + 5, 0xFFFFFFFF);
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 19, 0x11080D16);
            }
        }
    }

    private void drawOutlinedBox(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.hLine(x, x + w - 1, y, color);
        gg.hLine(x, x + w - 1, y + h - 1, color);
        gg.vLine(x, y, y + h - 1, color);
        gg.vLine(x + w - 1, y, y + h - 1, color);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private double parseDouble(String s) {
        if (s == null) return 0.0;
        try {
            return Double.parseDouble(s.replace(',', '.').trim());
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Overridden to no-op so Minecraft's default Screen#render doesn't apply blur shader over custom GUI
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
