package com.ammora.mod.client.gui;

import com.ammora.mod.core.MarketEngine;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.network.MarketDataPayload;
import com.ammora.mod.network.ServerboundContractPayload;
import com.ammora.mod.network.ServerboundExecuteOrderPayload;
import com.ammora.mod.network.ServerboundLimitOrderPayload;
import com.ammora.mod.network.ServerboundOMSPayload;
import com.ammora.mod.network.ServerboundSelectResourcePayload;
import com.ammora.mod.network.ServerboundUpdateRedstoneSettingsPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Professional trading terminal GUI combining Bybit layout with Cyber-Terminal
 * pixel aesthetics.
 * Features:
 * - Collapsible Multi-Asset Selector sidebar (Iron, Gold, Diamond, Netherite,
 * Copper, Redstone, Emerald, Lapis)
 * - Interactive Candlestick Chart with mouse wheel Zoom (6-45 candles) and
 * horizontal drag Panning
 * - Sound effects on trades and status notifications (audio juice & polish)
 * - Multi-tier quantity adjustment (+/-1, +/-16, +/-64, MAX, x2, Reset)
 * - In-GUI live validation & server response notification banners
 * - Sharp pixel rendering with 3D world blur backdrop
 */
public class TerminalScreen extends Screen {

    private MarketDataPayload data;
    private boolean isBuyMode = true;
    private int tradeAmount = 1;

    // Multi-Asset Selector & Navigation State
    private boolean showAssetSidebar = true;
    private int visibleCandles = 0; // 0 = auto-calculate based on plot width
    private int panOffset = 0;

    // Investor Rank info modal
    private boolean showRankModal = false;

    // ASP Terminal Guide Modal (GUIDE)
    private boolean showGuideModal = false;
    private int guideTab = 0; // 0: SPOT, 1: OMS, 2: LIMIT, 3: CONTRACTS, 4: CHART, 5: REDSTONE

    // Redstone Configuration Modal
    private boolean showRedstoneModal = false;
    private int tempRedstoneMode = 0;
    private double tempThresholdPrice = 7.0;
    private boolean tempThresholdIsLessThan = true;

    // Desk Mode (0: SPOT, 1: OMS, 2: LIMIT, 3: CONTRACTS)
    public static int deskMode = 0;
    private double omsInvestAmount = 100.0;
    private boolean isLimitBuy = true;
    private double limitPrice = 10.0;
    private int limitAmount = 16;

    // Status notification displayed directly in the GUI
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0;
    private int assetScrollOffset = 0;

    // Palette tokens (Industrial Amber / ChainBX Rust theme)
    private static final int COLOR_BG = 0xF50D0E12;
    private static final int COLOR_PANEL = 0xF014161C;
    private static final int COLOR_PANEL_HEADER = 0xF01A1D24;
    private static final int COLOR_BORDER_CYAN = 0xFFFF9800; // Industrial Amber / Orange
    private static final int COLOR_BORDER_MUTED = 0xFF2A2724;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_PURPLE_DANGER = 0xFFE040FB;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9284;

    public TerminalScreen(MarketDataPayload initialData) {
        super(Component.literal("Exchange Terminal"));
        this.data = initialData;
        if (initialData != null) {
            this.tempRedstoneMode = initialData.redstoneMode();
            this.tempThresholdPrice = initialData.thresholdPrice();
            this.tempThresholdIsLessThan = initialData.thresholdIsLessThan();
        }
        checkNewNotification(initialData);
    }

    public void updateMarketData(MarketDataPayload newData) {
        this.data = newData;
        if (!showRedstoneModal && newData != null) {
            this.tempRedstoneMode = newData.redstoneMode();
            this.tempThresholdPrice = newData.thresholdPrice();
            this.tempThresholdIsLessThan = newData.thresholdIsLessThan();
        }
        checkNewNotification(newData);
        rebuildWidgets();
    }

    private void checkNewNotification(MarketDataPayload payload) {
        if (payload != null && payload.statusMessage() != null && !payload.statusMessage().isEmpty()) {
            this.statusNotification = AmmoraLang.translateNotification(payload.statusMessage());
            this.statusNotificationError = payload.isStatusError();
            this.notificationExpireTime = System.currentTimeMillis() + 8000L;
            if (this.minecraft != null) {
                if (payload.isStatusError()) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8F));
                } else {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F));
                }
            }
        }
    }

    private int countPlayerItems() {
        if (this.minecraft == null || this.minecraft.player == null || data == null)
            return 0;
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.resourceId()));
            if (item == null)
                return 0;
            int total = 0;
            for (ItemStack stack : this.minecraft.player.getInventory().items) {
                if (stack.is(item))
                    total += stack.getCount();
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (visibleCandles <= 0) {
            int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
            int rightX = this.width - panelW - 8;
            int sidebarW = showAssetSidebar ? 84 : 0;
            int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
            int chartW = rightX - chartX - 4;
            int plotW = chartW - 54;
            visibleCandles = Math.max(16, Math.min(65, plotW / 14));
        }
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
        int rightX = this.width - panelW - 8;
        int topY = 44;
        int contentX = rightX + 6;
        int contentW = panelW - 12;

        // 0. Asset sidebar toggle button in header
        this.addRenderableWidget(Button.builder(
                Component.literal(showAssetSidebar ? "◀" : "▶"),
                b -> {
                    showAssetSidebar = !showAssetSidebar;
                    rebuildWidgets();
                }).bounds(12, 14, 16, 18).build());

        // Mode selector buttons (SPOT | OMS | LIMIT | CONTRACTS | ?)
        int helpBtnW = 14;
        int modeBtnW = (contentW - 8 - helpBtnW) / 4;
        int modeY = topY + 4;
        this.addRenderableWidget(Button.builder(
                Component.literal(deskMode == 0 ? AmmoraLang.guiStr("terminal.tab_spot_active") : AmmoraLang.guiStr("terminal.tab_spot_inactive")),
                b -> {
                    deskMode = 0;
                    rebuildWidgets();
                }).bounds(contentX, modeY, modeBtnW, 14).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(deskMode == 1 ? AmmoraLang.guiStr("terminal.tab_oms_active") : AmmoraLang.guiStr("terminal.tab_oms_inactive")),
                b -> {
                    deskMode = 1;
                    rebuildWidgets();
                }).bounds(contentX + modeBtnW + 2, modeY, modeBtnW, 14).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(deskMode == 2 ? AmmoraLang.guiStr("terminal.tab_limit_active") : AmmoraLang.guiStr("terminal.tab_limit_inactive")),
                b -> {
                    deskMode = 2;
                    if (data != null && limitPrice <= 0.0)
                        limitPrice = data.spotPrice();
                    rebuildWidgets();
                }).bounds(contentX + (modeBtnW + 2) * 2, modeY, modeBtnW, 14).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(deskMode == 3 ? AmmoraLang.guiStr("terminal.tab_gov_active") : AmmoraLang.guiStr("terminal.tab_gov_inactive")),
                b -> {
                    deskMode = 3;
                    rebuildWidgets();
                }).bounds(contentX + (modeBtnW + 2) * 3, modeY, modeBtnW, 14).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("§e?"),
                b -> {
                    showGuideModal = true;
                    guideTab = deskMode;
                }).bounds(contentX + (modeBtnW + 2) * 4, modeY, helpBtnW, 14).build());

        int subY = topY + 20;
        if (deskMode == 0) {
            rebuildSpotWidgets(contentX, contentW, subY);
        } else if (deskMode == 1) {
            rebuildOMSWidgets(contentX, contentW, subY);
        } else if (deskMode == 2) {
            rebuildLimitWidgets(contentX, contentW, subY);
        } else if (deskMode == 3) {
            rebuildContractWidgets(contentX, contentW, subY);
        }
    }

    private void rebuildSpotWidgets(int contentX, int contentW, int topY) {
        int tabW = (contentW - 4) / 2;
        int tabY = topY + 2;
        this.addRenderableWidget(Button.builder(
                Component.literal(isBuyMode ? AmmoraLang.guiStr("terminal.btn_buy_active") : AmmoraLang.guiStr("terminal.btn_buy_inactive")),
                b -> {
                    isBuyMode = true;
                    rebuildWidgets();
                }).bounds(contentX, tabY, tabW, 16).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(!isBuyMode ? AmmoraLang.guiStr("terminal.btn_sell_active") : AmmoraLang.guiStr("terminal.btn_sell_inactive")),
                b -> {
                    isBuyMode = false;
                    rebuildWidgets();
                }).bounds(contentX + tabW + 4, tabY, tabW, 16).build());

        // Redstone Pin Button & Config Gear (Top right of order desk)
        boolean isPinned = data != null && data.pinnedResourceId() != null
                && data.resourceId().equals(data.pinnedResourceId());
        this.addRenderableWidget(Button.builder(
                Component.literal(isPinned ? AmmoraLang.guiStr("terminal.btn_pin_active") : AmmoraLang.guiStr("terminal.btn_pin_inactive")),
                b -> {
                    if (data != null) {
                        PacketDistributor.sendToServer(new ServerboundSelectResourcePayload(data.resourceId(), true));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
                        }
                    }
                }).bounds(contentX + contentW - 74, topY + 20, 54, 13).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("⚙"),
                b -> {
                    showRedstoneModal = !showRedstoneModal;
                    if (showRedstoneModal && data != null) {
                        tempRedstoneMode = data.redstoneMode();
                        tempThresholdPrice = data.thresholdPrice();
                        tempThresholdIsLessThan = data.thresholdIsLessThan();
                    }
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                    }
                }).bounds(contentX + contentW - 18, topY + 20, 16, 13).build());

        // Quick amount buttons
        int btnW3 = (contentW - 4) / 3;
        int amtY1 = topY + 54;

        this.addRenderableWidget(
                Button.builder(Component.literal("+1"), b -> tradeAmount = Math.min(2304, tradeAmount + 1))
                        .bounds(contentX, amtY1, btnW3, 14).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("+16"), b -> tradeAmount = Math.min(2304, tradeAmount + 16))
                        .bounds(contentX + btnW3 + 2, amtY1, btnW3, 14).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("+64"), b -> tradeAmount = Math.min(2304, tradeAmount + 64))
                        .bounds(contentX + (btnW3 + 2) * 2, amtY1, btnW3, 14).build());

        int amtY2 = amtY1 + 15;
        this.addRenderableWidget(
                Button.builder(Component.literal("-1"), b -> tradeAmount = Math.max(1, tradeAmount - 1))
                        .bounds(contentX, amtY2, btnW3, 14).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("-16"), b -> tradeAmount = Math.max(1, tradeAmount - 16))
                        .bounds(contentX + btnW3 + 2, amtY2, btnW3, 14).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("-64"), b -> tradeAmount = Math.max(1, tradeAmount - 64))
                        .bounds(contentX + (btnW3 + 2) * 2, amtY2, btnW3, 14).build());

        int amtY3 = amtY2 + 15;
        this.addRenderableWidget(Button.builder(Component.literal("MAX"), b -> applyMaxAmount())
                .bounds(contentX, amtY3, btnW3, 14).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("x2"), b -> tradeAmount = Math.min(2304, tradeAmount * 2))
                        .bounds(contentX + btnW3 + 2, amtY3, btnW3, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("terminal.btn_reset")), b -> tradeAmount = 1)
                .bounds(contentX + (btnW3 + 2) * 2, amtY3, btnW3, 14).build());

        int execY = topY + 152;
        boolean isUnlocked = data == null || data.isCurrentResourceUnlocked();
        if (isBuyMode && !isUnlocked) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(AmmoraLang.guiStr("terminal.btn_research_sample")),
                    b -> {
                        if (data != null) {
                            PacketDistributor.sendToServer(new com.ammora.mod.network.ServerboundUnlockResourcePayload(data.resourceId()));
                        }
                    }).bounds(contentX, execY, contentW, 20).build());
        } else {
            String actionText;
            if (isBuyMode) {
                actionText = AmmoraLang.guiStr("terminal.btn_execute_buy");
            } else {
                double unitPrice = data != null ? data.sellPrice() : 0.0;
                actionText = (unitPrice < 0) ? AmmoraLang.guiStr("terminal.btn_execute_recycle") : AmmoraLang.guiStr("terminal.btn_execute_sell");
            }

            this.addRenderableWidget(Button.builder(
                    Component.literal(actionText),
                    b -> executeTrade()).bounds(contentX, execY, contentW, 20).build());
        }
    }

    private void rebuildOMSWidgets(int contentX, int contentW, int topY) {
        int btnW4 = (contentW - 6) / 4;
        int y1 = topY + 34;

        // Row 1: Presets
        this.addRenderableWidget(Button.builder(Component.literal("25"), b -> {
            omsInvestAmount = 25.0;
            rebuildWidgets();
        }).bounds(contentX, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("50"), b -> {
            omsInvestAmount = 50.0;
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("100"), b -> {
            omsInvestAmount = 100.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("500"), b -> {
            omsInvestAmount = 500.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y1, btnW4, 13).build());

        // Row 2: Increments
        int y2 = y1 + 15;
        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> {
            omsInvestAmount = MarketEngine.round2(omsInvestAmount + 10.0);
            rebuildWidgets();
        }).bounds(contentX, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+25"), b -> {
            omsInvestAmount = MarketEngine.round2(omsInvestAmount + 25.0);
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+50"), b -> {
            omsInvestAmount = MarketEngine.round2(omsInvestAmount + 50.0);
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+100"), b -> {
            omsInvestAmount = MarketEngine.round2(omsInvestAmount + 100.0);
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y2, btnW4, 13).build());

        // Row 3: Decrements and Controls
        int y3 = y2 + 15;
        this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> {
            omsInvestAmount = Math.max(10.0, MarketEngine.round2(omsInvestAmount - 10.0));
            rebuildWidgets();
        }).bounds(contentX, y3, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("-50"), b -> {
            omsInvestAmount = Math.max(10.0, MarketEngine.round2(omsInvestAmount - 50.0));
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y3, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("MAX"), b -> {
            if (data != null && data.userBalanceCbx() > 0)
                omsInvestAmount = Math.max(10.0, Math.floor(data.userBalanceCbx()));
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y3, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("terminal.btn_reset")), b -> {
            omsInvestAmount = 50.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y3, btnW4, 13).build());

        int openY = y3 + 17;
        this.addRenderableWidget(Button.builder(
                Component.literal(AmmoraLang.guiStr("terminal.oms_open_btn", (int) omsInvestAmount)),
                b -> {
                    if (data != null) {
                        PacketDistributor.sendToServer(
                                new ServerboundOMSPayload("OPEN", "", data.resourceId(), omsInvestAmount));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                        }
                    }
                }).bounds(contentX, openY, contentW, 18).build());

        if (data != null && data.omsPositions() != null) {
            int posY = openY + 36;
            for (int i = 0; i < Math.min(3, data.omsPositions().size()); i++) {
                var pos = data.omsPositions().get(i);
                int btnY = posY + (i * 24);
                this.addRenderableWidget(Button.builder(
                        Component.literal(AmmoraLang.guiStr("terminal.oms_close_btn")),
                        b -> {
                            PacketDistributor.sendToServer(
                                    new ServerboundOMSPayload("CLOSE", pos.positionId(), pos.resourceId(), -1.0));
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager()
                                        .play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.1F));
                            }
                        }).bounds(contentX + contentW - 54, btnY, 54, 14).build());
            }
        }
    }

    private void rebuildLimitWidgets(int contentX, int contentW, int topY) {
        int tabW = (contentW - 4) / 2;
        int y0 = topY + 2;
        this.addRenderableWidget(Button.builder(
                Component.literal(isLimitBuy ? AmmoraLang.guiStr("terminal.limit_buy_active") : AmmoraLang.guiStr("terminal.limit_buy_inactive")),
                b -> {
                    isLimitBuy = true;
                    rebuildWidgets();
                }).bounds(contentX, y0, tabW, 14).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(!isLimitBuy ? AmmoraLang.guiStr("terminal.limit_sell_active") : AmmoraLang.guiStr("terminal.limit_sell_inactive")),
                b -> {
                    isLimitBuy = false;
                    rebuildWidgets();
                }).bounds(contentX + tabW + 4, y0, tabW, 14).build());

        // Quick Spot button to sync limitPrice to current spot price
        int spotBtnW = 38;
        this.addRenderableWidget(Button.builder(
                Component.literal(AmmoraLang.guiStr("terminal.limit_btn_spot")),
                b -> {
                    if (data != null) {
                        limitPrice = MarketEngine.round2(isLimitBuy ? data.buyPrice() : data.sellPrice());
                        rebuildWidgets();
                    }
                }).bounds(contentX + contentW - spotBtnW, topY + 17, spotBtnW, 12).build());

        // Price adjustment buttons
        int btnW4 = (contentW - 6) / 4;
        int y1 = topY + 31;
        // Large steps: -10, -1, +1, +10
        this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> {
            limitPrice = Math.max(0.01, Math.round((limitPrice - 10.0) * 100.0) / 100.0);
            rebuildWidgets();
        }).bounds(contentX, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> {
            limitPrice = Math.max(0.01, Math.round((limitPrice - 1.0) * 100.0) / 100.0);
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> {
            limitPrice = Math.round((limitPrice + 1.0) * 100.0) / 100.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y1, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> {
            limitPrice = Math.round((limitPrice + 10.0) * 100.0) / 100.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y1, btnW4, 13).build());

        // Micro steps: -0.10, -0.01, +0.01, +0.10 (allows exact pricing like 7.88)
        int y1_micro = y1 + 14;
        this.addRenderableWidget(Button.builder(Component.literal("-.10"), b -> {
            limitPrice = Math.max(0.01, Math.round((limitPrice - 0.10) * 100.0) / 100.0);
            rebuildWidgets();
        }).bounds(contentX, y1_micro, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("-.01"), b -> {
            limitPrice = Math.max(0.01, Math.round((limitPrice - 0.01) * 100.0) / 100.0);
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y1_micro, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+.01"), b -> {
            limitPrice = Math.round((limitPrice + 0.01) * 100.0) / 100.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y1_micro, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("+.10"), b -> {
            limitPrice = Math.round((limitPrice + 0.10) * 100.0) / 100.0;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y1_micro, btnW4, 13).build());

        // Volume adjustment buttons
        int y2 = y1_micro + 26;
        this.addRenderableWidget(Button.builder(Component.literal("1"), b -> {
            limitAmount = 1;
            rebuildWidgets();
        }).bounds(contentX, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("16"), b -> {
            limitAmount = 16;
            rebuildWidgets();
        }).bounds(contentX + btnW4 + 2, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("64"), b -> {
            limitAmount = 64;
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 2, y2, btnW4, 13).build());

        this.addRenderableWidget(Button.builder(Component.literal("MAX"), b -> {
            if (isLimitBuy) {
                if (data != null && limitPrice > 0)
                    limitAmount = Math.max(1, Math.min(2304, (int) (data.userBalanceCbx() / limitPrice)));
            } else {
                limitAmount = Math.max(1, Math.min(2304, countPlayerItems()));
            }
            rebuildWidgets();
        }).bounds(contentX + (btnW4 + 2) * 3, y2, btnW4, 13).build());

        int placeY = y2 + 16;
        Button placeBtn = Button.builder(
                Component.literal(isLimitBuy ? AmmoraLang.guiStr("terminal.limit_place_buy") : AmmoraLang.guiStr("terminal.limit_place_sell")),
                b -> {
                    if (data != null) {
                        PacketDistributor.sendToServer(new ServerboundLimitOrderPayload(
                                "PLACE", "", data.resourceId(), isLimitBuy ? "BUY" : "SELL", limitAmount, limitPrice));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                        }
                    }
                }).bounds(contentX, placeY, contentW, 17).build();

        if (data != null) {
            if (isLimitBuy) {
                placeBtn.active = (data.userBalanceCbx() >= (limitPrice * limitAmount) && limitAmount > 0);
            } else {
                placeBtn.active = (countPlayerItems() >= limitAmount && limitAmount > 0);
            }
        }
        this.addRenderableWidget(placeBtn);

        if (data != null && data.limitOrders() != null) {
            int listY = placeY + 27;
            for (int i = 0; i < Math.min(3, data.limitOrders().size()); i++) {
                var ord = data.limitOrders().get(i);
                int btnY = listY + (i * 18);
                this.addRenderableWidget(Button.builder(
                        Component.literal("§c❌"),
                        b -> {
                            PacketDistributor.sendToServer(
                                    new ServerboundLimitOrderPayload("CANCEL", ord.orderId(), "", "", 0, 0));
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager()
                                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                            }
                        }).bounds(contentX + contentW - 24, btnY, 24, 14).build());
            }
        }
    }

    private void rebuildContractWidgets(int contentX, int contentW, int topY) {
        if (data == null || data.contracts() == null)
            return;
        int curY = topY + 18;
        for (int i = 0; i < Math.min(3, data.contracts().size()); i++) {
            var c = data.contracts().get(i);
            int cardH = 42;
            int btnY = curY + 23;
            if (c.isAcceptedByMe()) {
                if ("ACTIVE".equals(c.status())) {
                    int needed = Math.max(0, c.targetAmount() - c.deliveredAmount());
                    int batch = Math.min(64, needed);
                    String btnText = AmmoraLang.guiStr("terminal.contracts_deliver_btn", batch);
                    this.addRenderableWidget(Button.builder(
                            Component.literal(btnText),
                            b -> {
                                PacketDistributor
                                        .sendToServer(new ServerboundContractPayload("DELIVER", c.contractId(), batch));
                                if (this.minecraft != null) {
                                    this.minecraft.getSoundManager()
                                            .play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F));
                                }
                            }).bounds(contentX + contentW - 64, btnY, 62, 15).build());
                }
            } else if ("OPEN".equals(c.status())) {
                this.addRenderableWidget(Button.builder(
                        Component.literal(AmmoraLang.guiStr("terminal.contracts_accept_btn")),
                        b -> {
                            PacketDistributor.sendToServer(new ServerboundContractPayload("ACCEPT", c.contractId(), 0));
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager()
                                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                            }
                        }).bounds(contentX + contentW - 54, btnY, 52, 15).build());
            }
            curY += cardH + 4;
        }
    }

    private void applyMaxAmount() {
        if (data == null)
            return;
        if (isBuyMode) {
            MarketResource res = data.toMarketResource();
            double balance = data.userBalanceCbx();
            int maxStock = (int) data.currentStock();
            if (balance <= 0 || maxStock <= 0) {
                tradeAmount = 1;
                return;
            }
            int low = 1;
            int high = Math.min(2304, maxStock);
            int best = 1;
            while (low <= high) {
                int mid = (low + high) / 2;
                double cost = MarketEngine.calculateTotalBuyCost(mid, res);
                if (cost <= balance) {
                    best = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            tradeAmount = best;
        } else {
            int inInv = countPlayerItems();
            tradeAmount = Math.max(1, inInv);
        }
    }

    private void executeTrade() {
        if (data == null)
            return;

        MarketResource res = data.toMarketResource();

        if (isBuyMode) {
            double totalCost = MarketEngine.calculateTotalBuyCost(tradeAmount, res);
            if (data.userBalanceCbx() < totalCost) {
                statusNotification = AmmoraLang.guiStr("terminal.err_no_cbx");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 5000L;
                playWarningSound();
                return;
            }
            if (data.currentStock() < tradeAmount) {
                statusNotification = AmmoraLang.guiStr("terminal.err_no_stock");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 5000L;
                playWarningSound();
                return;
            }
        } else {
            int inInv = countPlayerItems();
            if (inInv < tradeAmount) {
                statusNotification = AmmoraLang.guiStr("terminal.err_no_items", inInv);
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 5000L;
                playWarningSound();
                return;
            }
            double totalPayout = MarketEngine.calculateTotalSellPayout(tradeAmount, res);
            if (totalPayout < 0 && data.userBalanceCbx() < Math.abs(totalPayout)) {
                statusNotification = AmmoraLang.guiStr("terminal.err_no_eco_fee");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 5000L;
                playWarningSound();
                return;
            }
        }

        String type = isBuyMode ? "BUY" : "SELL";
        PacketDistributor.sendToServer(new ServerboundExecuteOrderPayload(data.resourceId(), type, tradeAmount));
    }

    private void playWarningSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int sidebarW = showAssetSidebar ? 84 : 0;
        if (showAssetSidebar && mouseX >= 8 && mouseX <= 8 + sidebarW && mouseY >= 44 && mouseY <= this.height - 8) {
            List<MarketDataPayload.MarketSummaryItem> markets = (data != null && data.availableMarkets() != null
                    && !data.availableMarkets().isEmpty())
                            ? data.availableMarkets()
                            : getDefaultMarkets();
            int rowH = 22;
            int visibleH = this.height - 44 - 8 - 20;
            int totalContentH = markets.size() * rowH;
            int maxScroll = Math.max(0, totalContentH - visibleH);
            if (scrollY > 0) {
                assetScrollOffset = Math.max(0, assetScrollOffset - rowH);
            } else if (scrollY < 0) {
                assetScrollOffset = Math.min(maxScroll, assetScrollOffset + rowH);
            }
            return true;
        }

        int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
        int rightX = this.width - panelW - 8;
        int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
        int chartW = rightX - chartX - 4;
        int chartY = 44;
        int chartH = this.height - chartY - 8;

        if (mouseX >= chartX && mouseX <= chartX + chartW && mouseY >= chartY && mouseY <= chartY + chartH) {
            if (scrollY > 0) {
                visibleCandles = Math.max(6, visibleCandles - 2);
            } else if (scrollY < 0) {
                visibleCandles = Math.min(85, visibleCandles + 2);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
        int rightX = this.width - panelW - 8;
        int sidebarW = showAssetSidebar ? 84 : 0;
        int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
        int chartW = rightX - chartX - 4;
        int chartY = 44;
        int chartH = this.height - chartY - 8;

        if (mouseX >= chartX && mouseX <= chartX + chartW && mouseY >= chartY && mouseY <= chartY + chartH) {
            if (data != null && !data.candles().isEmpty()) {
                int maxPan = Math.max(0, data.candles().size() - 6);
                int delta = (int) Math.round(dragX / 4.0);
                if (delta != 0) {
                    panOffset = Math.max(0, Math.min(maxPan, panOffset + delta));
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.showRankModal) {
            if (keyCode == 256
                    || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                this.showRankModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }
            return true;
        }

        if (this.showRedstoneModal) {
            if (keyCode == 256
                    || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                this.showRedstoneModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }
            return true;
        }

        if (this.showGuideModal) {
            if (keyCode == 256
                    || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                this.showGuideModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }
            return true;
        }

        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showRankModal) {
            int mw = 290, mh = 190;
            int mx = (this.width - mw) / 2;
            int my = (this.height - mh) / 2;

            // Close [X] button
            if (button == 0 && mouseX >= mx + mw - 20 && mouseX <= mx + mw - 2 && mouseY >= my + 2
                    && mouseY <= my + 18) {
                showRankModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            // Click outside modal
            if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                showRankModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            return true;
        }

        if (showRedstoneModal) {
            int mw = 290, mh = 195;
            int mx = (this.width - mw) / 2;
            int my = (this.height - mh) / 2;

            // Close [X] button
            if (button == 0 && mouseX >= mx + mw - 20 && mouseX <= mx + mw - 2 && mouseY >= my + 2
                    && mouseY <= my + 18) {
                showRedstoneModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            // Click outside modal
            if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                showRedstoneModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            int curLvl = data != null ? data.userRepLevel() : 1;

            // Mode selection buttons at my + 46 (h: 16)
            if (button == 0 && mouseY >= my + 46 && mouseY <= my + 62) {
                if (mouseX >= mx + 10 && mouseX <= mx + 96) {
                    tempRedstoneMode = 0;
                    if (this.minecraft != null)
                        this.minecraft.getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    return true;
                }
                if (mouseX >= mx + 100 && mouseX <= mx + 186) {
                    if (curLvl >= 3) {
                        tempRedstoneMode = 1;
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    } else {
                        playWarningSound();
                    }
                    return true;
                }
                if (mouseX >= mx + 190 && mouseX <= mx + 280) {
                    if (curLvl >= 3) {
                        tempRedstoneMode = 2;
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    } else {
                        playWarningSound();
                    }
                    return true;
                }
            }

            // Threshold controls (if tempRedstoneMode == 2)
            if (button == 0 && tempRedstoneMode == 2) {
                int boxY = my + 68;
                int opX = mx + 16, opY = boxY + 32;
                // Operator toggle (< vs >)
                if (mouseX >= opX && mouseX <= opX + 44 && mouseY >= opY && mouseY <= opY + 16) {
                    tempThresholdIsLessThan = !tempThresholdIsLessThan;
                    if (this.minecraft != null)
                        this.minecraft.getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
                    return true;
                }

                // Adjust buttons: -10, -1, +1, +10
                int adjX = opX + 50 + 76;
                if (mouseY >= opY && mouseY <= opY + 16) {
                    if (mouseX >= adjX && mouseX <= adjX + 20) {
                        tempThresholdPrice = Math.max(0.1, Math.round((tempThresholdPrice - 10.0) * 10.0) / 10.0);
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                        return true;
                    }
                    if (mouseX >= adjX + 22 && mouseX <= adjX + 42) {
                        tempThresholdPrice = Math.max(0.1, Math.round((tempThresholdPrice - 1.0) * 10.0) / 10.0);
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                        return true;
                    }
                    if (mouseX >= adjX + 44 && mouseX <= adjX + 64) {
                        tempThresholdPrice = Math.round((tempThresholdPrice + 1.0) * 10.0) / 10.0;
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                        return true;
                    }
                    if (mouseX >= adjX + 66 && mouseX <= adjX + 86) {
                        tempThresholdPrice = Math.round((tempThresholdPrice + 10.0) * 10.0) / 10.0;
                        if (this.minecraft != null)
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
                        return true;
                    }
                }
            }

            // Apply button at my + 154 (h: 22)
            int applyY = my + 154;
            if (button == 0 && mouseX >= mx + 10 && mouseX <= mx + mw - 10 && mouseY >= applyY
                    && mouseY <= applyY + 22) {
                if (curLvl >= 3 || tempRedstoneMode == 0) {
                    net.neoforged.neoforge.network.PacketDistributor
                            .sendToServer(new ServerboundUpdateRedstoneSettingsPayload(
                                    data != null ? data.terminalPos() : null,
                                    tempRedstoneMode,
                                    tempThresholdPrice,
                                    tempThresholdIsLessThan,
                                    data != null ? data.resourceId() : "minecraft:iron_ingot"));
                    showRedstoneModal = false;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F));
                    }
                } else {
                    playWarningSound();
                }
                return true;
            }

            return true;
        }

        if (showGuideModal) {
            int mw = Math.min(384, this.width - 16);
            int mh = Math.min(210, this.height - 16);
            int mx = (this.width - mw) / 2;
            int my = (this.height - mh) / 2;

            // Close [X] button
            if (button == 0 && mouseX >= mx + mw - 20 && mouseX <= mx + mw - 2 && mouseY >= my + 2
                    && mouseY <= my + 18) {
                showGuideModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            // Click outside modal
            if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                showGuideModal = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9F));
                }
                return true;
            }

            // Tab clicks at my + 22 (height 15)
            if (button == 0 && mouseY >= my + 22 && mouseY <= my + 37) {
                int tabW = (mw - 16 - 10) / 6;
                for (int i = 0; i < 6; i++) {
                    int tx = mx + 8 + (i * (tabW + 2));
                    if (mouseX >= tx && mouseX <= tx + tabW) {
                        guideTab = i;
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                        }
                        return true;
                    }
                }
            }

            return true;
        }

        // Check click on [?] (Rank) or [? Info] (Guide) button in header
        if (button == 0 && data != null) {
            int headerX = 8;
            int headerW = this.width - 16;
            int colW = (headerW - 24) / 4;
            int x3 = headerX + 28 + (colW * 3) + 6;
            String rankStr = AmmoraLang.guiStr("terminal.rank_label", getRankTitle(data.userRepLevel()));
            int rankW = this.font.width(rankStr);
            int btnX = x3 + rankW + 2;
            int qRankW = this.font.width(" §6[?]");
            if (mouseX >= btnX && mouseX <= btnX + qRankW && mouseY >= 18 && mouseY <= 32) {
                showRankModal = true;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }

            int infoBtnX = btnX + qRankW;
            int infoBtnW = this.font.width(" " + AmmoraLang.guiStr("terminal.info_button"));
            if (mouseX >= infoBtnX && mouseX <= infoBtnX + infoBtnW + 4 && mouseY >= 18 && mouseY <= 32) {
                showGuideModal = true;
                guideTab = deskMode;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }
        }

        if (showAssetSidebar && button == 0) {
            int sidebarX = 8;
            int sidebarY = 44;
            int sidebarW = 84;
            int sidebarH = this.height - sidebarY - 8;

            List<MarketDataPayload.MarketSummaryItem> markets = (data != null && data.availableMarkets() != null
                    && !data.availableMarkets().isEmpty())
                            ? data.availableMarkets()
                            : getDefaultMarkets();

            int rowH = 22;
            int visibleH = sidebarH - 20;
            int totalContentH = markets.size() * rowH;
            int maxScroll = Math.max(0, totalContentH - visibleH);
            boolean hasScrollbar = totalContentH > visibleH;
            int contentW = hasScrollbar ? sidebarW - 6 : sidebarW;

            if (mouseX >= sidebarX && mouseX <= sidebarX + contentW && mouseY >= sidebarY + 18 && mouseY <= sidebarY + sidebarH - 2) {
                int clickedY = (int) (mouseY - (sidebarY + 18)) + assetScrollOffset;
                int idx = clickedY / rowH;
                if (idx >= 0 && idx < markets.size()) {
                    var chosen = markets.get(idx);
                    int boxX = sidebarX + contentW - 12;
                    boolean clickCheckbox = mouseX >= boxX;
                    if (clickCheckbox) {
                        PacketDistributor.sendToServer(new ServerboundSelectResourcePayload(chosen.resourceId(), true));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
                        }
                        return true;
                    } else if (data == null || !chosen.resourceId().equals(data.resourceId())) {
                        PacketDistributor
                                .sendToServer(new ServerboundSelectResourcePayload(chosen.resourceId(), false));
                        tradeAmount = 1;
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager()
                                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                        }
                        return true;
                    }
                }
            }
        }

        // Check click on [? Help] above chart
        if (button == 0) {
            int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
            int rightX = this.width - panelW - 8;
            int sidebarW = showAssetSidebar ? 84 : 0;
            int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
            int chartW = rightX - chartX - 4;
            int chartY = 44;
            String chartHeader = getChartHeaderString();
            int guideBtnX = chartX + 8 + this.font.width(chartHeader) + 6;
            int guideBtnW = this.font.width(AmmoraLang.guiStr("terminal.chart_help_button"));
            if (mouseX >= guideBtnX - 2 && mouseX <= guideBtnX + guideBtnW + 4 && mouseY >= chartY + 3
                    && mouseY <= chartY + 16) {
                showGuideModal = true;
                guideTab = 4; // Open Chart tab
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }

            // Click on news ticker banner to open Guide on Events tab
            if (data != null && data.activeEventTitle() != null && !data.activeEventTitle().isEmpty()) {
                int bannerX = chartX + 6;
                int bannerY = chartY + 18;
                int bannerW = chartW - 12;
                int bannerH = 12;

                if (mouseX >= bannerX && mouseX <= bannerX + bannerW && mouseY >= bannerY
                        && mouseY <= bannerY + bannerH) {
                    showGuideModal = true;
                    guideTab = 4; // Tab 4: CHART & EVENTS
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    }
                    return true;
                }
            }
        }

        // Reset chart pan offset if clicked near chart title indicator
        if (button == 0 && panOffset > 0) {
            int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
            int rightX = this.width - panelW - 8;
            int sidebarW = showAssetSidebar ? 80 : 0;
            int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
            if (mouseX >= chartX + 8 && mouseX <= chartX + 220 && mouseY >= 44 && mouseY <= 58) {
                panOffset = 0;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderBlurredBackground(partialTick);
        }

        g.fill(0, 0, this.width, this.height, COLOR_BG);

        renderHeader(g);
        if (showAssetSidebar) {
            renderAssetSidebar(g, mouseX, mouseY);
        }
        renderChart(g, mouseX, mouseY);
        renderOrderPanel(g);

        super.render(g, mouseX, mouseY, partialTick);

        // Tooltip for news ticker banner
        if (!showRankModal && !showRedstoneModal && !showGuideModal && data != null && data.activeEventTitle() != null
                && !data.activeEventTitle().isEmpty()) {
            int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
            int rightX = this.width - panelW - 8;
            int sidebarW = showAssetSidebar ? 84 : 0;
            int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
            int chartW = rightX - chartX - 4;
            int chartY = 44;
            int bannerX = chartX + 6;
            int bannerY = chartY + 18;
            int bannerW = chartW - 12;
            int bannerH = 12;

            if (mouseX >= bannerX && mouseX <= bannerX + bannerW && mouseY >= bannerY && mouseY <= bannerY + bannerH) {
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal("§6§l⚡ " + data.activeEventTitle()),
                        Component.literal("§f" + data.activeEventDescription()),
                        Component.literal(AmmoraLang.guiStr("terminal.click_for_guide"))
                ), mouseX, mouseY);
            }
        }

        if (showRankModal) {
            renderRankModal(g, mouseX, mouseY);
        } else if (showRedstoneModal) {
            renderRedstoneModal(g, mouseX, mouseY);
        } else if (showGuideModal) {
            renderGuideModal(g, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Overridden to no-op so Minecraft's default Screen#render doesn't apply blur
    }

    private void renderHeader(GuiGraphics g) {
        int h = 34;
        int headerX = 8;
        int headerW = this.width - 16;

        g.fill(headerX, 6, headerX + headerW, 6 + h, COLOR_PANEL_HEADER);
        renderBorder(g, headerX, 6, headerW, h, COLOR_BORDER_CYAN);

        if (data == null)
            return;

        int colW = (headerW - 24) / 4;

        // Col 1: Ticker & Spot Price (offset slightly to accommodate toggle button)
        int x0 = headerX + 32;
        String name = data.displayName().toUpperCase();
        boolean isPinned = data.pinnedResourceId() != null && data.resourceId().equals(data.pinnedResourceId());
        String pinBadge = isPinned ? " §c[✔ R]" : "";
        if (this.font.width(name + pinBadge) > colW - 14) {
            name = this.font.plainSubstrByWidth(name, colW - 22 - this.font.width(pinBadge)) + "..";
        }
        g.drawString(this.font, "§b§l" + name + pinBadge, x0, 11, 0xFFFFFFFF);
        double mod = data.dailyModifier();
        String modStr = mod >= 0 ? ("§a+" + String.format(Locale.US, "%.1f", mod * 100.0) + "%")
                : ("§c" + String.format(Locale.US, "%.1f", mod * 100.0) + "%");
        g.drawString(this.font, AmmoraLang.guiStr("terminal.spot_prefix") + data.spotPrice() + " CBX §8| " + modStr, x0, 22, 0xFFFFFFFF);

        // Col 2: Market Quotes
        int x1 = headerX + 28 + colW + 6;
        g.drawString(this.font, AmmoraLang.guiStr("terminal.buy_header", data.buyPrice() + " CBX"), x1, 11, 0xFFFFFFFF);
        if (data.sellPrice() >= 0) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.sell_header", data.sellPrice() + " CBX"), x1, 22, 0xFFFFFFFF);
        } else {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.recycle_header", "-" + Math.abs(data.sellPrice()) + " CBX"), x1, 22, 0xFFFFFFFF);
        }

        // Col 3: Capacity gauge & progress bar
        int x2 = headerX + 28 + (colW * 2) + 6;
        double fill = (data.currentStock() / data.maxReserve()) * 100.0;
        int gaugeColor = fill >= 100 ? COLOR_PURPLE_DANGER : (fill >= 75 ? COLOR_AMBER : COLOR_GREEN);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.stock_header", Math.round(fill) + "%", (int) data.currentStock()), x2, 11,
                gaugeColor);

        int barW = Math.min(colW - 14, 65);
        int barH = 3;
        int filledW = (int) Math.min(barW, (fill / 100.0) * barW);
        g.fill(x2, 24, x2 + barW, 24 + barH, 0xFF1C2738);
        g.fill(x2, 24, x2 + filledW, 24 + barH, gaugeColor);

        // Col 4: Balance & Rank
        int x3 = headerX + 28 + (colW * 3) + 6;
        g.drawString(this.font, "§6" + String.format(Locale.US, "%.1f", data.userBalanceCbx()) + " CBX", x3, 11,
                0xFFFFFFFF);
        String rankStr = AmmoraLang.guiStr("terminal.rank_label", getRankTitle(data.userRepLevel()));
        g.drawString(this.font, rankStr, x3, 22, COLOR_TEXT_MUTED);
        int rankW = this.font.width(rankStr);
        g.drawString(this.font, " §6[?]", x3 + rankW, 22, COLOR_BORDER_CYAN);
        int qRankW = this.font.width(" §6[?]");
        g.drawString(this.font, " " + AmmoraLang.guiStr("terminal.info_button"), x3 + rankW + qRankW, 22, COLOR_BORDER_CYAN);
    }

    private void renderAssetSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int sidebarX = 8;
        int sidebarY = 44;
        int sidebarW = 84;
        int sidebarH = this.height - sidebarY - 8;
        int visibleH = sidebarH - 20;

        g.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, COLOR_PANEL);
        renderBorder(g, sidebarX, sidebarY, sidebarW, sidebarH, COLOR_BORDER_MUTED);

        g.drawString(this.font, AmmoraLang.guiStr("terminal.assets_title"), sidebarX + 6, sidebarY + 5, 0xFFFFFFFF);

        List<MarketDataPayload.MarketSummaryItem> markets = (data != null && data.availableMarkets() != null
                && !data.availableMarkets().isEmpty())
                        ? data.availableMarkets()
                        : getDefaultMarkets();

        int rowH = 22;
        int totalContentH = markets.size() * rowH;
        int maxScroll = Math.max(0, totalContentH - visibleH);
        assetScrollOffset = Math.max(0, Math.min(maxScroll, assetScrollOffset));

        boolean hasScrollbar = totalContentH > visibleH;
        int contentW = hasScrollbar ? sidebarW - 6 : sidebarW;

        g.enableScissor(sidebarX + 1, sidebarY + 18, sidebarX + sidebarW - 1, sidebarY + sidebarH - 1);

        for (int i = 0; i < markets.size(); i++) {
            var m = markets.get(i);
            int rowY = sidebarY + 18 + (i * rowH) - assetScrollOffset;
            if (rowY + rowH < sidebarY + 18 || rowY > sidebarY + sidebarH) continue;

            boolean isSelected = data != null && m.resourceId().equals(data.resourceId());
            boolean isHovered = mouseX >= sidebarX && mouseX <= sidebarX + contentW && mouseY >= rowY
                    && mouseY <= rowY + rowH && mouseY >= sidebarY + 18 && mouseY <= sidebarY + sidebarH - 1;

            if (isSelected) {
                g.fill(sidebarX + 1, rowY, sidebarX + contentW - 1, rowY + rowH - 1, 0x33FF9800);
                renderBorder(g, sidebarX + 1, rowY, contentW - 2, rowH - 1, COLOR_BORDER_CYAN);
            } else if (isHovered) {
                g.fill(sidebarX + 1, rowY, sidebarX + contentW - 1, rowY + rowH - 1, 0x1AFFFFFF);
            }

            try {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(m.resourceId()));
                if (item != null) {
                    g.renderItem(new ItemStack(item), sidebarX + 3, rowY + (rowH - 16) / 2);
                }
            } catch (Exception ignored) {
            }

            String shortName = getShortName(m.resourceId(), m.displayName());
            double rowMod = m.dailyModifier();
            String rowModStr = rowMod >= 0 ? ("§a+" + String.format(Locale.US, "%.0f", rowMod * 100.0) + "%%")
                    : ("§c" + String.format(Locale.US, "%.0f", rowMod * 100.0) + "%%");
            String lockBadge = m.isUnlocked() ? "" : " §8🔒";

            // Redstone checkbox indicator on the right of each asset row
            boolean isRowPinned = data != null && data.pinnedResourceId() != null
                    && m.resourceId().equals(data.pinnedResourceId());
            int boxX = sidebarX + contentW - 12;
            int boxY = rowY + (rowH - 9) / 2;
            g.fill(boxX, boxY, boxX + 9, boxY + 9, isRowPinned ? 0x88FF1744 : 0xFF141E2D);
            renderBorder(g, boxX, boxY, 9, 9, isRowPinned ? COLOR_RED : 0xFF3E4F66);
            if (isRowPinned) {
                g.drawString(this.font, "§c✔", boxX + 1, boxY, 0xFFFFFFFF);
            }

            // Line 1: Resource Name
            int maxNameW = boxX - (sidebarX + 22) - 2;
            String trimmedName = this.font.plainSubstrByWidth(shortName, maxNameW);
            g.drawString(this.font, (isSelected ? "§6" : (m.isUnlocked() ? "§f" : "§7")) + trimmedName + lockBadge, sidebarX + 22, rowY + 2, 0xFFFFFFFF);

            // Line 2: Price + Modifier (Placed on row 2 so it never collides with checkbox!)
            String priceAndMod = "§e" + String.format(Locale.US, "%.1f", m.spotPrice()) + " CBX " + rowModStr;
            g.drawString(this.font, priceAndMod, sidebarX + 22, rowY + 12, 0xFFFFFFFF);
        }

        g.disableScissor();

        // Render scrollbar on right edge if needed
        if (hasScrollbar) {
            int barX = sidebarX + sidebarW - 4;
            int barY = sidebarY + 19;
            int barW = 3;
            int barH = visibleH - 2;
            g.fill(barX, barY, barX + barW, barY + barH, 0x44000000);

            int thumbH = Math.max(12, (int) ((float) visibleH / totalContentH * barH));
            int thumbY = barY + (int) ((float) assetScrollOffset / maxScroll * (barH - thumbH));
            g.fill(barX, thumbY, barX + barW, thumbY + thumbH, COLOR_BORDER_CYAN);
        }
    }

    private String getChartHeaderString() {
        String chartHeader = AmmoraLang.guiStr("terminal.chart_title");
        if (panOffset > 0) {
            chartHeader += " " + AmmoraLang.guiStr("terminal.chart_archive", panOffset);
        } else {
            chartHeader += " " + AmmoraLang.guiStr("terminal.chart_zoom", visibleCandles);
        }
        return chartHeader;
    }

    private void renderChart(GuiGraphics g, int mouseX, int mouseY) {
        int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
        int rightX = this.width - panelW - 8;
        int sidebarW = showAssetSidebar ? 84 : 0;
        int chartX = 8 + (showAssetSidebar ? sidebarW + 4 : 0);
        int chartW = rightX - chartX - 4;
        int chartY = 44;
        int chartH = this.height - chartY - 8;

        g.fill(chartX, chartY, chartX + chartW, chartY + chartH, COLOR_PANEL);
        renderBorder(g, chartX, chartY, chartW, chartH, COLOR_BORDER_MUTED);

        String chartHeader = getChartHeaderString();
        g.drawString(this.font, chartHeader, chartX + 8, chartY + 6, COLOR_TEXT_MUTED);
        int guideBtnX = chartX + 8 + this.font.width(chartHeader) + 6;
        g.drawString(this.font, AmmoraLang.guiStr("terminal.chart_help_button"), guideBtnX, chartY + 6, COLOR_AMBER);

        if (data != null && data.activeEventTitle() != null && !data.activeEventTitle().isEmpty()) {
            int bannerX = chartX + 6;
            int bannerY = chartY + 18;
            int bannerW = chartW - 12;
            int bannerH = 12;

            boolean isHovered = mouseX >= bannerX && mouseX <= bannerX + bannerW && mouseY >= bannerY
                    && mouseY <= bannerY + bannerH;

            g.fill(bannerX, bannerY, bannerX + bannerW, bannerY + bannerH, isHovered ? 0x66FFAA00 : 0x33FFAA00);
            renderBorder(g, bannerX, bannerY, bannerW, bannerH, isHovered ? 0xFFFFAA00 : 0x88FFAA00);

            String eventText = AmmoraLang.guiStr("terminal.event_prefix") + " §e" + data.activeEventTitle() + ": §f" + data.activeEventDescription();
            String separator = "     §8✦§r     ";
            int textW = this.font.width(eventText);
            int sepW = this.font.width(separator);
            int itemSpan = textW + sepW;
            int loopWidth = Math.max(bannerW + 60, itemSpan);

            long nowMs = System.currentTimeMillis();
            // Scroll at 35ms per pixel (~28.5px/sec) - smooth, readable pace
            int scrollOffset = (int) ((nowMs / 35L) % loopWidth);

            g.enableScissor(bannerX + 1, bannerY + 1, bannerX + bannerW - 1, bannerY + bannerH - 1);
            int drawX = bannerX + 4 - scrollOffset;
            while (drawX < bannerX + bannerW) {
                g.drawString(this.font, eventText, drawX, bannerY + 2, 0xFFFFFFFF);
                g.drawString(this.font, separator, drawX + textW, bannerY + 2, 0xFF888888);
                drawX += loopWidth;
            }
            g.disableScissor();
        }

        if (data == null || data.candles().isEmpty()) {
            g.drawCenteredString(this.font, AmmoraLang.guiStr("terminal.gathering_chart_data"), chartX + chartW / 2, chartY + chartH / 2,
                    0xFFFFFFFF);
            return;
        }

        List<MarketDataPayload.CandleItem> rawCandles = data.candles();
        int total = rawCandles.size();
        int actualPan = Math.min(Math.max(0, total - 4), panOffset);
        int endIndex = Math.max(1, total - actualPan);
        int startIndex = Math.max(0, endIndex - visibleCandles);
        List<MarketDataPayload.CandleItem> candles = rawCandles.subList(startIndex, endIndex);

        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        double maxVolume = 1.0;
        for (var c : candles) {
            if (c.low() < minPrice)
                minPrice = c.low();
            if (c.high() > maxPrice)
                maxPrice = c.high();
            if (c.volume() > maxVolume)
                maxVolume = c.volume();
        }
        if (data.spotPrice() < minPrice)
            minPrice = data.spotPrice();
        if (data.spotPrice() > maxPrice)
            maxPrice = data.spotPrice();

        double margin = Math.max(0.4, (maxPrice - minPrice) * 0.08);
        minPrice -= margin;
        maxPrice += margin;
        double range = Math.max(0.1, maxPrice - minPrice);

        double lastPrice = data.spotPrice();
        String tag = "§a" + lastPrice + " CBX";
        if (!candles.isEmpty()) {
            var lastCandle = candles.get(candles.size() - 1);
            tag = (lastCandle.close() >= lastCandle.open() ? "§a▲ " : "§c▼ ") + lastPrice + " CBX";
        }
        g.drawString(this.font, tag, chartX + chartW - font.width(tag) - 8, chartY + 6, 0xFFFFFFFF);

        int plotY = chartY + 20;
        int plotH = chartH - 36;
        int plotW = chartW - 52;

        for (int step = 1; step <= 3; step++) {
            int gridY = plotY + (plotH * step / 4);
            g.fill(chartX + 4, gridY, chartX + chartW - 4, gridY + 1, 0xFF141E2D);
            double gridPrice = maxPrice - (range * step / 4.0);
            g.drawString(this.font, "§8" + String.format(Locale.US, "%.1f", gridPrice), chartX + chartW - 46,
                    gridY - 4, 0xFFFFFFFF);
        }

        int currentPriceY = plotY + plotH - (int) (((data.spotPrice() - minPrice) / range) * plotH);
        if (currentPriceY >= plotY && currentPriceY <= plotY + plotH) {
            for (int dx = chartX + 4; dx < chartX + chartW - 48; dx += 6) {
                g.fill(dx, currentPriceY, dx + 3, currentPriceY + 1, 0x8800D2FF);
            }
        }

        int numCandles = candles.size();
        if (numCandles == 0)
            return;

        // Responsive slot width that seamlessly spans the full chart width on 1x, 2x, 3x, and 4x GUI scales
        double slotW = (double) (plotW - 8) / Math.max(1, Math.min(visibleCandles, numCandles));
        int candleWidth = Math.max(3, (int) Math.round(slotW * 0.72));
        double rightEdge = chartX + plotW - 2;

        for (int i = 0; i < numCandles; i++) {
            var c = candles.get(i);
            int distFromRight = (numCandles - 1) - i;
            double slotLeft = rightEdge - (distFromRight + 1) * slotW;
            int wickX = (int) Math.round(slotLeft + (slotW / 2.0));
            int bodyX = (int) Math.round(wickX - (candleWidth / 2.0));

            if (bodyX + candleWidth < chartX + 4 || bodyX > rightEdge + 4)
                continue;

            int highY = plotY + plotH - (int) (((c.high() - minPrice) / range) * plotH);
            int lowY = plotY + plotH - (int) (((c.low() - minPrice) / range) * plotH);
            int openY = plotY + plotH - (int) (((c.open() - minPrice) / range) * plotH);
            int closeY = plotY + plotH - (int) (((c.close() - minPrice) / range) * plotH);

            boolean isBullish = c.close() >= c.open();
            int color = isBullish ? COLOR_GREEN : COLOR_RED;

            g.fill(wickX, Math.min(highY, lowY), wickX + 1, Math.max(highY, lowY) + 1, color);

            int bodyTop = Math.min(openY, closeY);
            int bodyBottom = Math.max(openY, closeY);
            if (bodyBottom == bodyTop)
                bodyBottom++;
            g.fill(bodyX, bodyTop, bodyX + candleWidth, bodyBottom, color);

            int volH = Math.max(2, (int) ((c.volume() / maxVolume) * 16));
            int volColor = isBullish ? 0x5500E676 : 0x55FF5252;
            g.fill(bodyX, chartY + chartH - volH - 2, bodyX + candleWidth, chartY + chartH - 2, volColor);
        }

        g.drawString(this.font, "§8" + String.format(Locale.US, "%.1f", maxPrice), chartX + chartW - 46, plotY - 4,
                0xFFFFFFFF);
        g.drawString(this.font, "§8" + String.format(Locale.US, "%.1f", minPrice), chartX + chartW - 46,
                plotY + plotH - 6, 0xFFFFFFFF);
    }

    private void renderOrderPanel(GuiGraphics g) {
        int panelW = Math.max(152, Math.min(180, (int) (this.width * 0.30f)));
        int rightX = this.width - panelW - 8;
        int topY = 44;
        int panelH = this.height - topY - 8;
        int contentX = rightX + 6;
        int contentW = panelW - 12;

        int borderColor = switch (deskMode) {
            case 1 -> COLOR_AMBER;
            case 2 -> COLOR_BORDER_CYAN;
            case 3 -> COLOR_PURPLE_DANGER;
            default -> isBuyMode ? COLOR_GREEN : COLOR_RED;
        };
        g.fill(rightX, topY, rightX + panelW, topY + panelH, COLOR_PANEL);
        renderBorder(g, rightX, topY, panelW, panelH, borderColor);

        if (data == null)
            return;

        int subY = topY + 20;
        if (deskMode == 0) {
            renderSpotPanel(g, contentX, contentW, subY);
        } else if (deskMode == 1) {
            renderOMSPanel(g, contentX, contentW, subY);
        } else if (deskMode == 2) {
            renderLimitPanel(g, contentX, contentW, subY);
        } else if (deskMode == 3) {
            renderContractPanel(g, contentX, contentW, subY);
        }

        renderStatusNotification(g, contentX, contentW, topY, panelH);
    }

    private void renderSpotPanel(GuiGraphics g, int contentX, int contentW, int topY) {
        // Mode & item count
        g.drawString(this.font, isBuyMode ? AmmoraLang.guiStr("terminal.buying_label") : AmmoraLang.guiStr("terminal.selling_label"), contentX, topY + 20, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.amount_label", tradeAmount), contentX, topY + 31, 0xFFFFFFFF);

        int inInventory = countPlayerItems();
        if (isBuyMode) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.stock_label", (int) data.currentStock()), contentX, topY + 42,
                    0xFFFFFFFF);
        } else {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.inventory_label", inInventory), contentX, topY + 42,
                    inInventory >= tradeAmount ? 0xFFFFFFFF : COLOR_RED);
        }

        // Price details below amount buttons (which end at topY + 98)
        if (isBuyMode && !data.isCurrentResourceUnlocked()) {
            int ty = topY + 103;
            g.fill(contentX, ty, contentX + contentW, ty + 36, 0x33FF9800);
            renderBorder(g, contentX, ty, contentW, 36, COLOR_BORDER_CYAN);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.resource_locked_badge"), contentX + 6, ty + 5, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.resource_locked_hint1"), contentX + 6, ty + 16, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.resource_locked_hint2"), contentX + 6, ty + 25, COLOR_AMBER);
            return;
        }

        int ty = topY + 103;
        g.fill(contentX, ty, contentX + contentW, ty + 1, 0xFF1C2738);
        ty += 4;

        MarketResource res = data.toMarketResource();
        double estTotal = isBuyMode
                ? MarketEngine.calculateTotalBuyCost(tradeAmount, res)
                : MarketEngine.calculateTotalSellPayout(tradeAmount, res);
        double spotUnitPrice = isBuyMode ? data.buyPrice() : data.sellPrice();
        double avgUnitPrice = tradeAmount > 0 ? (Math.abs(estTotal) / tradeAmount) : Math.abs(spotUnitPrice);

        g.drawString(this.font, AmmoraLang.guiStr("terminal.spot_price_label", Math.abs(spotUnitPrice)), contentX, ty, 0xFFFFFFFF);
        ty += 10;

        if (tradeAmount > 1) {
            double priceDiff = avgUnitPrice - Math.abs(spotUnitPrice);
            String diffSign = priceDiff >= 0 ? "+" : "-";
            g.drawString(this.font, AmmoraLang.guiStr("terminal.avg_price_diff", String.format(Locale.US, "%.2f", avgUnitPrice), diffSign,
                    String.format(Locale.US, "%.2f CBX", Math.abs(priceDiff))), contentX, ty, 0xFFFFFFFF);
        } else {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.avg_price_label", Math.abs(spotUnitPrice)), contentX, ty, COLOR_TEXT_MUTED);
        }
        ty += 10;

        String totalStr;
        if (isBuyMode) {
            totalStr = AmmoraLang.guiStr("terminal.total_to_pay", String.format(Locale.US, "%.2f", Math.abs(estTotal)));
        } else if (estTotal >= 0) {
            totalStr = AmmoraLang.guiStr("terminal.total_to_receive", String.format(Locale.US, "%.2f", Math.abs(estTotal)));
        } else {
            totalStr = AmmoraLang.guiStr("terminal.total_recycle_fee", String.format(Locale.US, "%.2f", Math.abs(estTotal)));
        }
        g.drawString(this.font, totalStr, contentX, ty,
                0xFFFFFFFF);

        // Validation warning cleanly spaced above execution button (which starts at topY + 152)
        ty = topY + 139;
        String validationWarning = null;
        if (isBuyMode) {
            if (estTotal > data.userBalanceCbx()) {
                validationWarning = AmmoraLang.guiStr("terminal.warn_insufficient_funds",
                        String.format(Locale.US, "%.1f", estTotal - data.userBalanceCbx()));
            } else if (tradeAmount > data.currentStock()) {
                validationWarning = AmmoraLang.guiStr("terminal.warn_out_of_stock");
            }
        } else {
            if (inInventory < tradeAmount) {
                validationWarning = AmmoraLang.guiStr("terminal.warn_in_bag_only", inInventory);
            } else if (estTotal < 0 && Math.abs(estTotal) > data.userBalanceCbx()) {
                validationWarning = AmmoraLang.guiStr("terminal.warn_fee_funds");
            }
        }

        if (validationWarning != null) {
            g.drawString(this.font, validationWarning, contentX, ty, 0xFFFFFFFF);
        } else if (!isBuyMode && estTotal < 0) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.warn_surplus_penalty"), contentX, ty, COLOR_PURPLE_DANGER);
        }
    }

    private void renderOMSPanel(GuiGraphics g, int contentX, int contentW, int topY) {
        g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_panel_title"), contentX, topY + 2, 0xFFFFFFFF);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_panel_desc"), contentX, topY + 12, COLOR_TEXT_MUTED);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_investment", String.format(Locale.US, "%.1f", omsInvestAmount)), contentX,
                topY + 22, 0xFFFFFFFF);

        // Separator below open button
        int sepY = topY + 103;
        g.fill(contentX, sepY, contentX + contentW, sepY + 1, 0xFF1C2738);

        g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_open_positions"), contentX, sepY + 4, 0xFFFFFFFF);

        int listY = topY + 117;
        if (data.omsPositions() == null || data.omsPositions().isEmpty()) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_no_positions"), contentX, listY + 6, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.oms_carry_fee"), contentX, listY + 18, COLOR_TEXT_MUTED);
        } else {
            for (int i = 0; i < Math.min(3, data.omsPositions().size()); i++) {
                var pos = data.omsPositions().get(i);
                int rowY = listY + (i * 24);
                double pnl = pos.currentPnL();
                String pnlStr = (pnl >= 0 ? "§a+" : "§c-") + String.format(Locale.US, "%.2f CBX", Math.abs(pnl));
                String resName = getShortName(pos.resourceId(), pos.resourceId());
                g.drawString(this.font,
                        "§b" + resName + ": §f" + AmmoraLang.guiStr("terminal.oms_units", String.format(Locale.US, "%.2f", pos.units())), contentX,
                        rowY + 1, 0xFFFFFFFF);
                g.drawString(this.font,
                        AmmoraLang.guiStr("terminal.oms_entry_line", String.format(Locale.US, "%.2f", pos.avgBuyPrice()), pnlStr), contentX,
                        rowY + 11, COLOR_TEXT_MUTED);
            }
        }
    }

    private void renderLimitPanel(GuiGraphics g, int contentX, int contentW, int topY) {
        g.drawString(this.font, AmmoraLang.guiStr("terminal.limit_target", String.format(Locale.US, "%.2f", limitPrice)), contentX, topY + 19,
                0xFFFFFFFF);

        String volStr = AmmoraLang.guiStr("terminal.limit_volume", limitAmount);
        if (isLimitBuy) {
            volStr += AmmoraLang.guiStr("terminal.limit_reserved", String.format(Locale.US, "%.2f", limitPrice * limitAmount));
        } else {
            int inInv = countPlayerItems();
            volStr += AmmoraLang.guiStr("terminal.limit_in_bag", (inInv >= limitAmount ? "§a" : "§c"), inInv);
        }
        g.drawString(this.font, volStr, contentX, topY + 60, 0xFFFFFFFF);

        // Separator below place button
        int sepY = topY + 107;
        g.fill(contentX, sepY, contentX + contentW, sepY + 1, 0xFF1C2738);

        g.drawString(this.font, AmmoraLang.guiStr("terminal.limit_your_orders"), contentX, sepY + 4, 0xFFFFFFFF);

        int listY = topY + 119;
        if (data.limitOrders() == null || data.limitOrders().isEmpty()) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.limit_no_orders"), contentX, listY + 6, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.limit_auto_exec"), contentX, listY + 18, COLOR_TEXT_MUTED);
        } else {
            for (int i = 0; i < Math.min(3, data.limitOrders().size()); i++) {
                var ord = data.limitOrders().get(i);
                int rowY = listY + (i * 18);
                boolean isBuy = "BUY".equalsIgnoreCase(ord.type());
                String tag = isBuy ? "§aBUY" : "§cSELL";
                g.drawString(this.font,
                        tag + " §f" + ord.amount() + "x §e" + String.format(Locale.US, "%.2f", ord.limitPrice()) + " CBX",
                        contentX, rowY + 3, 0xFFFFFFFF);
            }
        }
    }

    private void renderContractPanel(GuiGraphics g, int contentX, int contentW, int topY) {
        g.drawString(this.font, AmmoraLang.guiStr("terminal.contracts_panel_title"), contentX, topY + 4, 0xFFFFFFFF);

        int curY = topY + 18;
        if (data.contracts() == null || data.contracts().isEmpty()) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.contracts_no_available"), contentX, curY + 6, COLOR_TEXT_MUTED);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.contracts_midnight"), contentX, curY + 18, COLOR_TEXT_MUTED);
        } else {
            for (int i = 0; i < Math.min(3, data.contracts().size()); i++) {
                var c = data.contracts().get(i);
                int cardH = 42;
                g.fill(contentX, curY, contentX + contentW, curY + cardH, 0x221A2433);
                renderBorder(g, contentX, curY, contentW, cardH, c.isAcceptedByMe() ? 0xFF00E676 : 0xFF3E4F66);

                String shortName = getShortName(c.resourceId(), c.title());
                g.drawString(this.font, "§e" + shortName + " §8(+" + c.rewardRep() + " REP)", contentX + 3, curY + 3,
                        0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("terminal.contracts_price_collateral", String.format(Locale.US, "%.1f", c.guaranteedPrice()), (int) c.collateral()), contentX + 3, curY + 14, 0xFFFFFFFF);
                int needed = Math.max(0, c.targetAmount() - c.deliveredAmount());
                String statusStr = c.isAcceptedByMe()
                        ? AmmoraLang.guiStr("terminal.contracts_accepted_status", c.deliveredAmount(), c.targetAmount(), needed)
                        : AmmoraLang.guiStr("terminal.contracts_batch_size", c.targetAmount());
                g.drawString(this.font, statusStr, contentX + 3, curY + 26, 0xFFFFFFFF);

                curY += cardH + 4;
            }
        }
    }

    private void renderStatusNotification(GuiGraphics g, int contentX, int contentW, int topY, int panelH) {
        if (statusNotification != null && !statusNotification.isEmpty()
                && System.currentTimeMillis() < notificationExpireTime) {
            int notifY = topY + panelH - 24;
            int notifBg = statusNotificationError ? 0xEE4A121A : 0xEE0D3B20;
            int notifBorder = statusNotificationError ? COLOR_RED : COLOR_GREEN;

            g.fill(contentX, notifY, contentX + contentW, notifY + 18, notifBg);
            renderBorder(g, contentX, notifY, contentW, 18, notifBorder);

            String trimmed = statusNotification;
            if (this.font.width(trimmed) > contentW - 8) {
                trimmed = this.font.plainSubstrByWidth(trimmed, contentW - 12) + "..";
            }
            g.drawCenteredString(this.font, (statusNotificationError ? "§c" : "§a") + trimmed, contentX + contentW / 2,
                    notifY + 5, 0xFFFFFFFF);
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

    private String getShortName(String resId, String defaultName) {
        String key = "commodity.ammora." + resId.replace(':', '.');
        String localized = AmmoraLang.str(key);
        if (!key.equals(localized)) {
            return localized;
        }
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resId));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                String iname = item.getDescription().getString();
                if (iname != null && !iname.isEmpty()) {
                    return (iname.length() > 7 ? iname.substring(0, 6) + ".." : iname);
                }
            }
        } catch (Exception ignored) {}
        return (defaultName.length() > 7 ? defaultName.substring(0, 6) + ".." : defaultName);
    }

    private List<MarketDataPayload.MarketSummaryItem> getDefaultMarkets() {
        return List.of(
                new MarketDataPayload.MarketSummaryItem("minecraft:iron_ingot", "Iron Ingot", 12.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:gold_ingot", "Gold Ingot", 40.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:diamond", "Diamond", 350.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:netherite_ingot", "Netherite Ingot", 4500.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:copper_ingot", "Copper Ingot", 8.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:redstone", "Redstone Dust", 18.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:emerald", "Emerald", 50.0, 50.0, 0.0, true),
                new MarketDataPayload.MarketSummaryItem("minecraft:lapis_lazuli", "Lapis Lazuli", 15.0, 50.0, 0.0, true));
    }

    private void renderRankModal(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        int mw = 290, mh = 190;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        g.fill(mx, my, mx + mw, my + mh, 0xFF0F141C);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 18, 0xFF17202E);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.rank_modal_title"), mx + 8, my + 5, 0xFFFFFFFF);

        // Close [X] button
        boolean hoverClose = mouseX >= mx + mw - 18 && mouseX <= mx + mw - 4 && mouseY >= my + 3 && mouseY <= my + 17;
        g.drawString(this.font, hoverClose ? "§c[X]" : "§7[X]", mx + mw - 16, my + 5, 0xFFFFFFFF);

        // Player stats & REP bar
        int curLvl = data != null ? data.userRepLevel() : 1;
        int curPts = data != null ? data.userRepPoints() : 0;
        g.drawString(this.font, AmmoraLang.guiStr("terminal.rank_status", getRankTitle(curLvl), curLvl), mx + 10, my + 23,
                0xFFFFFFFF);
        String repLabel = "§6" + curPts + " §7REP";
        g.drawString(this.font, repLabel, mx + mw - 10 - this.font.width(repLabel), my + 23, 0xFFFFFFFF);

        int nextTarget = switch (curLvl) {
            case 1 -> 500;
            case 2 -> 2500;
            case 3 -> 10000;
            default -> 50000;
        };
        int prevTarget = switch (curLvl) {
            case 1 -> 0;
            case 2 -> 500;
            case 3 -> 2500;
            case 4 -> 10000;
            default -> 50000;
        };
        double prog = (curLvl >= 5) ? 1.0
                : Math.min(1.0, Math.max(0.0, (double) (curPts - prevTarget) / Math.max(1, nextTarget - prevTarget)));
        int barW = mw - 20;
        g.fill(mx + 10, my + 34, mx + 10 + barW, my + 38, 0xFF1C2738);
        g.fill(mx + 10, my + 34, mx + 10 + (int) (barW * prog), my + 38, COLOR_BORDER_CYAN);
        String progText = (curLvl >= 5) ? AmmoraLang.guiStr("terminal.rank_max")
                : AmmoraLang.guiStr("terminal.rank_next_target", (curLvl + 1), Math.max(0, nextTarget - curPts));
        g.drawString(this.font, progText, mx + 10, my + 41, 0xFF888888);

        // 5 rank levels
        String[][] ranks = {
                { AmmoraLang.guiStr("rank.lvl.1.title"), AmmoraLang.guiStr("rank.lvl.1.fee"), AmmoraLang.guiStr("rank.lvl.1.perks") },
                { AmmoraLang.guiStr("rank.lvl.2.title"), AmmoraLang.guiStr("rank.lvl.2.fee"), AmmoraLang.guiStr("rank.lvl.2.perks") },
                { AmmoraLang.guiStr("rank.lvl.3.title"), AmmoraLang.guiStr("rank.lvl.3.fee"), AmmoraLang.guiStr("rank.lvl.3.perks") },
                { AmmoraLang.guiStr("rank.lvl.4.title"), AmmoraLang.guiStr("rank.lvl.4.fee"), AmmoraLang.guiStr("rank.lvl.4.perks") },
                { AmmoraLang.guiStr("rank.lvl.5.title"), AmmoraLang.guiStr("rank.lvl.5.fee"), AmmoraLang.guiStr("rank.lvl.5.perks") }
        };

        int listY = my + 53;
        for (int i = 0; i < 5; i++) {
            int lvl = i + 1;
            boolean isUnlocked = curLvl >= lvl;
            int rowY = listY + (i * 24);
            int rowBg = isUnlocked ? 0x2200D2FF : 0x111C2738;
            g.fill(mx + 8, rowY, mx + mw - 8, rowY + 22, rowBg);
            renderBorder(g, mx + 8, rowY, mw - 16, 22, isUnlocked ? 0x4400D2FF : 0xFF2A374A);

            String badge = isUnlocked ? "§a✔" : "§8🔒";
            g.drawString(this.font, badge + " §e" + ranks[i][0] + " §8| §7" + ranks[i][1], mx + 12, rowY + 3,
                    0xFFFFFFFF);
            g.drawString(this.font, "   §f" + ranks[i][2], mx + 12, rowY + 12, isUnlocked ? 0xFFCCCCCC : 0xFF666666);
        }

        g.drawCenteredString(this.font, AmmoraLang.guiStr("terminal.modal_close_hint"), mx + mw / 2, my + mh - 11,
                0xFF888888);
    }

    private void renderRedstoneModal(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        int mw = 290, mh = 195;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        g.fill(mx, my, mx + mw, my + mh, 0xFF0F141C);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header bar
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 18, 0xFF17202E);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_modal_title"), mx + 8, my + 5, 0xFFFFFFFF);

        // Close [X]
        boolean hoverClose = mouseX >= mx + mw - 18 && mouseX <= mx + mw - 4 && mouseY >= my + 3 && mouseY <= my + 17;
        g.drawString(this.font, hoverClose ? "§c[X]" : "§7[X]", mx + mw - 16, my + 5, 0xFFFFFFFF);

        // Rank check & status
        int curLvl = data != null ? data.userRepLevel() : 1;
        if (curLvl < 3) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_req_broker"), mx + 10, my + 23,
                    0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_status_locked", getRankTitle(curLvl)),
                    mx + 10, my + 33, 0xFF888888);
        } else {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_status_unlocked", getRankTitle(curLvl)), mx + 10,
                    my + 23, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_choose_mode"), mx + 10, my + 33,
                    0xFF888888);
        }

        // Mode buttons at my + 46 (height 16)
        // Mode 0: Stock (0-15)
        int btn0X = mx + 10, btn0W = 86;
        boolean sel0 = tempRedstoneMode == 0;
        g.fill(btn0X, my + 46, btn0X + btn0W, my + 62, sel0 ? 0x4400D2FF : 0xFF1A2433);
        renderBorder(g, btn0X, my + 46, btn0W, 16, sel0 ? COLOR_BORDER_CYAN : 0xFF2A374A);
        g.drawCenteredString(this.font, (sel0 ? "§b§l" : "§7") + AmmoraLang.guiStr("terminal.redstone_mode_stock"), btn0X + btn0W / 2, my + 50,
                0xFFFFFFFF);

        // Mode 1: Price (0-15)
        int btn1X = mx + 100, btn1W = 86;
        boolean sel1 = tempRedstoneMode == 1;
        g.fill(btn1X, my + 46, btn1X + btn1W, my + 62, sel1 ? 0x4400D2FF : 0xFF1A2433);
        renderBorder(g, btn1X, my + 46, btn1W, 16, sel1 ? COLOR_BORDER_CYAN : 0xFF2A374A);
        g.drawCenteredString(this.font, (sel1 ? "§b§l" : "§7") + AmmoraLang.guiStr("terminal.redstone_mode_price"), btn1X + btn1W / 2, my + 50, 0xFFFFFFFF);

        // Mode 2: Threshold (H/L)
        int btn2X = mx + 190, btn2W = 90;
        boolean sel2 = tempRedstoneMode == 2;
        g.fill(btn2X, my + 46, btn2X + btn2W, my + 62, sel2 ? 0x4400D2FF : 0xFF1A2433);
        renderBorder(g, btn2X, my + 46, btn2W, 16, sel2 ? COLOR_BORDER_CYAN : 0xFF2A374A);
        g.drawCenteredString(this.font, (sel2 ? "§b§l" : "§7") + AmmoraLang.guiStr("terminal.redstone_mode_threshold"), btn2X + btn2W / 2, my + 50,
                0xFFFFFFFF);

        // Description Box
        int boxY = my + 68;
        int boxH = 80;
        g.fill(mx + 10, boxY, mx + mw - 10, boxY + boxH, 0xFF121A26);
        renderBorder(g, mx + 10, boxY, mw - 20, boxH, 0xFF1E2C3D);

        if (tempRedstoneMode == 0) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_stock_1"), mx + 16, boxY + 8, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_stock_2"), mx + 16, boxY + 22,
                    0xFF888888);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_formula_stock"), mx + 16, boxY + 34, 0xFF666666);
            double ratio = data != null ? (data.currentStock() / Math.max(1.0, data.maxReserve())) : 0.5;
            int sig = (int) Math.min(15, Math.max(0, Math.round(ratio * 15)));
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_current_signal", sig), mx + 16, boxY + 54,
                    0xFFFFFFFF);
        } else if (tempRedstoneMode == 1) {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_price_1"), mx + 16, boxY + 8, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_price_2"), mx + 16, boxY + 22,
                    0xFF888888);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_formula_price"), mx + 16, boxY + 34, 0xFF666666);
            double spot = data != null ? data.spotPrice() : 12.0;
            double base = data != null ? data.basePrice() : 12.0;
            int sig = (int) Math.min(15, Math.max(0, Math.round((spot / Math.max(0.1, base * 2.0)) * 15)));
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_current_signal", sig), mx + 16, boxY + 54,
                    0xFFFFFFFF);
        } else {
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_thresh_1"), mx + 16, boxY + 8, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_desc_thresh_2"), mx + 16, boxY + 20, 0xFF888888);

            // Condition operator button
            int opX = mx + 16, opY = boxY + 32;
            g.fill(opX, opY, opX + 44, opY + 16, 0xFF1C2738);
            renderBorder(g, opX, opY, 44, 16, COLOR_BORDER_CYAN);
            g.drawCenteredString(this.font, tempThresholdIsLessThan ? AmmoraLang.guiStr("terminal.redstone_op_less") : AmmoraLang.guiStr("terminal.redstone_op_greater"), opX + 22, opY + 4,
                    0xFFFFFFFF);

            // Threshold price value & adjustment buttons
            int valX = opX + 50;
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_threshold_val", String.format(Locale.US, "%.1f", tempThresholdPrice)), valX,
                    opY + 4, 0xFFFFFFFF);

            // Buttons: -10, -1, +1, +10
            int adjX = valX + 76;
            drawMicroButton(g, adjX, opY, "-10", mouseX, mouseY);
            drawMicroButton(g, adjX + 22, opY, "-1", mouseX, mouseY);
            drawMicroButton(g, adjX + 44, opY, "+1", mouseX, mouseY);
            drawMicroButton(g, adjX + 66, opY, "+10", mouseX, mouseY);

            // Trigger preview
            double curPrice = data != null ? data.sellPrice() : 0.0;
            boolean triggered = tempThresholdIsLessThan ? (curPrice < tempThresholdPrice)
                    : (curPrice > tempThresholdPrice);
            String trigText = triggered ? AmmoraLang.guiStr("terminal.redstone_trig_on") : AmmoraLang.guiStr("terminal.redstone_trig_off");
            g.drawString(this.font,
                    AmmoraLang.guiStr("terminal.redstone_condition_status", (tempThresholdIsLessThan ? "<" : ">"), String.format(Locale.US, "%.1f", tempThresholdPrice), String.format(Locale.US, "%.1f", curPrice)),
                    mx + 16, boxY + 54, 0xFFFFFFFF);
            g.drawString(this.font, AmmoraLang.guiStr("terminal.redstone_output_status", trigText), mx + 16, boxY + 66, 0xFFFFFFFF);
        }

        // Apply Button
        int applyY = my + 154;
        boolean canApply = curLvl >= 3 || tempRedstoneMode == 0;
        g.fill(mx + 10, applyY, mx + mw - 10, applyY + 22, canApply ? 0xFF144D29 : 0xFF2B1C1C);
        renderBorder(g, mx + 10, applyY, mw - 20, 22, canApply ? 0xFF00E676 : 0xFF662222);
        String applyText = canApply ? AmmoraLang.guiStr("terminal.redstone_apply_btn") : AmmoraLang.guiStr("terminal.redstone_req_broker_btn");
        g.drawCenteredString(this.font, applyText, mx + mw / 2, applyY + 7, 0xFFFFFFFF);

        g.drawCenteredString(this.font, AmmoraLang.guiStr("terminal.modal_close_hint_x"), mx + mw / 2, my + mh - 11,
                0xFF888888);
    }

    private void renderGuideModal(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        int mw = Math.min(384, this.width - 16);
        int mh = Math.min(210, this.height - 16);
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        g.fill(mx, my, mx + mw, my + mh, 0xFF0F141C);
        renderBorder(g, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header bar
        g.fill(mx + 1, my + 1, mx + mw - 1, my + 18, 0xFF17202E);
        g.drawString(this.font, AmmoraLang.guiStr("terminal.guide_modal_title"), mx + 8, my + 5, 0xFFFFFFFF);

        // Close [X]
        boolean hoverClose = mouseX >= mx + mw - 18 && mouseX <= mx + mw - 4 && mouseY >= my + 3 && mouseY <= my + 17;
        g.drawString(this.font, hoverClose ? "§c[X]" : "§7[X]", mx + mw - 16, my + 5, 0xFFFFFFFF);

        // 6 Navigation tabs at my + 22 (height 15)
        String[] tabNames = { AmmoraLang.guiStr("terminal.guide_tab_0"), AmmoraLang.guiStr("terminal.guide_tab_1"), AmmoraLang.guiStr("terminal.guide_tab_2"), AmmoraLang.guiStr("terminal.guide_tab_3"), AmmoraLang.guiStr("terminal.guide_tab_4"), AmmoraLang.guiStr("terminal.guide_tab_5") };
        int tabW = (mw - 16 - 10) / 6;
        for (int i = 0; i < 6; i++) {
            int tx = mx + 8 + (i * (tabW + 2));
            boolean isSel = guideTab == i;
            boolean isHov = mouseX >= tx && mouseX <= tx + tabW && mouseY >= my + 22 && mouseY <= my + 37;

            int tabBg = isSel ? 0x4400D2FF : (isHov ? 0x22FFFFFF : 0xFF161E2B);
            int tabBorder = isSel ? COLOR_BORDER_CYAN : (isHov ? 0xFF4A5F7A : 0xFF2A374A);
            g.fill(tx, my + 22, tx + tabW, my + 37, tabBg);
            renderBorder(g, tx, my + 22, tabW, 15, tabBorder);

            String title = (isSel ? "§b§l" : (isHov ? "§f" : "§7")) + tabNames[i];
            g.drawCenteredString(this.font, title, tx + tabW / 2, my + 25, 0xFFFFFFFF);
        }

        // Content container
        int boxY = my + 40;
        int boxH = 146;
        g.fill(mx + 8, boxY, mx + mw - 8, boxY + boxH, 0xFF121924);
        renderBorder(g, mx + 8, boxY, mw - 16, boxH, 0xFF2A374A);

        int cx = mx + 14;
        switch (guideTab) {
            case 0 -> { // SPOT
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line3"), cx, boxY + 46, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line4"), cx, boxY + 59, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line5"), cx, boxY + 70, 0xFF888888);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line6"), cx, boxY + 83, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line7"), cx, boxY + 96, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.spot.line8"), cx, boxY + 109, COLOR_TEXT_MUTED);
            }
            case 1 -> { // OMS / UMA
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line3"), cx, boxY + 46, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line4"), cx, boxY + 59, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line5"), cx, boxY + 72, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line6"), cx, boxY + 83, 0xFF888888);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line7"), cx, boxY + 96, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.oms.line8"), cx, boxY + 109, COLOR_TEXT_MUTED);
            }
            case 2 -> { // LIMIT
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line3"), cx, boxY + 44, 0xFF888888);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line4"), cx, boxY + 57, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line5"), cx, boxY + 70, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line6"), cx, boxY + 83, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line7"), cx, boxY + 96, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.limit.line8"), cx, boxY + 109, COLOR_TEXT_MUTED);
            }
            case 3 -> { // CONTRACTS
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line3"), cx, boxY + 46, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line4"), cx, boxY + 59, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line5"), cx, boxY + 72, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line6"), cx, boxY + 85, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.contracts.line7"), cx, boxY + 98, COLOR_TEXT_MUTED);
            }
            case 4 -> { // CHART
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line3"), cx, boxY + 46, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line4"), cx, boxY + 59, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line5"), cx, boxY + 72, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line6"), cx, boxY + 85, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line7"), cx, boxY + 96, 0xFF888888);
                g.drawString(this.font, AmmoraLang.guiStr("guide.chart.line8"), cx, boxY + 109, COLOR_TEXT_MUTED);
            }
            case 5 -> { // REDSTONE
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.title"), cx, boxY + 6, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line1"), cx, boxY + 20, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line2"), cx, boxY + 33, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line3"), cx, boxY + 46, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line4"), cx, boxY + 59, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line5"), cx, boxY + 70, 0xFF888888);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line6"), cx, boxY + 83, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line7"), cx, boxY + 96, 0xFFFFFFFF);
                g.drawString(this.font, AmmoraLang.guiStr("guide.redstone.line8"), cx, boxY + 109, COLOR_TEXT_MUTED);
            }
        }

        g.drawCenteredString(this.font, AmmoraLang.guiStr("terminal.modal_close_hint"), mx + mw / 2, my + mh - 11,
                0xFF888888);
    }

    private void drawMicroButton(GuiGraphics g, int x, int y, String text, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 16;
        g.fill(x, y, x + 20, y + 16, hover ? 0xFF2A3A4D : 0xFF17202E);
        renderBorder(g, x, y, 20, 16, hover ? COLOR_BORDER_CYAN : 0xFF2B3A4F);
        g.drawCenteredString(this.font, text, x + 10, y + 4, hover ? 0xFFFFFFFF : 0xFFCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
