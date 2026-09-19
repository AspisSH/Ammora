package com.ammora.mod.client.gui;

import com.ammora.mod.network.MarketplaceDataPayload;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.network.ServerboundAuctionActionPayload;
import com.ammora.mod.network.ServerboundBuyRequestPayload;
import com.ammora.mod.network.ServerboundClaimDeliveryPayload;
import com.ammora.mod.network.ServerboundCommunityQuestPayload;
import com.ammora.mod.network.ServerboundCompanyActionPayload;
import com.ammora.mod.network.ServerboundShopPurchasePayload;
import net.minecraft.client.Minecraft;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Date;
import java.util.List;

/**
 * Global Marketplace Tablet Screen.
 * 7 Tabs: Global Market (Drone Delivery), Player Shops Directory (Rust-style), Live Auctions, Buy Requests (RFQ), Quests & Bounties, Delivery Buffer (10 slots), and History Ledger.
 * Features full JEI-like item browser for RFQ, shop filtering, anti-overlap layout, quest board, real-time live auctions, and high-tech styling.
 */
public class MarketplaceScreen extends Screen {

    private MarketplaceDataPayload data;
    private int activeTab = 0; // 0: Catalog, 1: Shops, 2: Auctions, 3: Buy Requests, 4: Quests, 5: Delivery Buffer, 6: History
    private int catalogPage = 0;
    private int shopsPage = 0;
    private int auctionPage = 0;
    private int reqPage = 0;
    private int questsPage = 0;
    private int deliveryPage = 0;
    private int txPage = 0;

    // Filter by specific shop in Catalog tab
    private String filterShopId = null;
    private String filterShopName = null;

    // Live Auctions filter & creation state
    private int auctionFilter = 0; // 0: All, 1: My Lots, 2: My Bids
    private boolean showCreateAuctionModal = false;
    private int createAuctionSlot = -1;
    private EditBox auctionStartPriceBox;
    private EditBox auctionMinStepBox;
    private EditBox auctionBuyoutBox;
    private int selectedAuctionDurationMins = 60; // 60, 360, 720, 1440
    private String lastAuctionStartPrice = "50.0";
    private String lastAuctionMinStep = "5.0";
    private String lastAuctionBuyout = "0.0";

    private EditBox searchBox;
    private String lastCatalogSearch = "";
    private final List<Button> dynamicCatalogButtons = new ArrayList<>();

    private EditBox bountyPriceBox;
    private EditBox bountyCountBox;
    private String lastBountyPrice = "50.0";
    private String lastBountyCount = "1";
    private ItemStack selectedRfqItem = ItemStack.EMPTY;

    // JEI-style Item Picker Modal for RFQ
    private boolean showItemPickerModal = false;
    private EditBox pickerSearchBox;
    private String lastPickerQuery = "";
    private int pickerPage = 0;
    private List<Item> filteredPickerItems = new ArrayList<>();
    private static List<Item> ALL_REGISTERED_ITEMS = null;

    // Community Quests Modal state
    private boolean showCreateQuestModal = false;
    private MarketplaceDataPayload.CommunityQuestItem selectedQuest = null;
    private EditBox questTitleBox;
    private EditBox questRewardBox;
    private EditBox questDescBox;
    private String lastQuestTitle = "";
    private String lastQuestReward = "100.0";
    private String lastQuestDesc = "";

    // Corporate Account & Company Tab State
    private static boolean useCompanyAccount = false;
    private EditBox companyNameInput;
    private String lastCompanyName = "";
    private EditBox companyAmountInput;
    private String lastCompanyAmount = "100.0";
    private EditBox companyInviteInput;
    private String lastCompanyInvite = "";
    private int companyMemberPage = 0;
    private int companyLedgerPage = 0;

    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0L;

    private static final int COLOR_BG = 0xF5060A12;
    private static final int COLOR_PANEL = 0xEE090E18;
    private static final int COLOR_PANEL_HEADER = 0xF00D1422;
    private static final int COLOR_BORDER_CYAN = 0xFFFF9800; // Industrial Amber / Orange
    private static final int COLOR_BORDER_MUTED = 0xFF182333;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF7E8FA4;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    public MarketplaceScreen(MarketplaceDataPayload initialData) {
        super(Component.translatable("gui.ammora.market.title"));
        this.data = initialData;
        checkNotification(initialData);
        initDefaultRfqItem();
    }

    private void initDefaultRfqItem() {
        if (selectedRfqItem.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack off = mc.player.getOffhandItem();
                if (!off.isEmpty()) {
                    selectedRfqItem = off.copyWithCount(1);
                    return;
                }
            }
            selectedRfqItem = new ItemStack(Items.DIAMOND);
        }
    }

    public void updateData(MarketplaceDataPayload newData) {
        this.data = newData;
        checkNotification(newData);
        rebuildWidgets();
    }

    public void triggerLocalNotification(String msg, boolean isError) {
        this.statusNotification = msg;
        this.statusNotificationError = isError;
        this.notificationExpireTime = System.currentTimeMillis() + 4500L;
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    isError ? SoundEvents.VILLAGER_NO : SoundEvents.EXPERIENCE_ORB_PICKUP,
                    1.2F
            ));
        }
    }

    private void checkNotification(MarketplaceDataPayload d) {
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

    private static void ensureItemCache() {
        if (ALL_REGISTERED_ITEMS == null) {
            ALL_REGISTERED_ITEMS = new ArrayList<>();
            for (Item it : BuiltInRegistries.ITEM) {
                if (it != Items.AIR) {
                    ALL_REGISTERED_ITEMS.add(it);
                }
            }
        }
    }

    private void updateFilteredPickerItems(String query) {
        ensureItemCache();
        lastPickerQuery = query == null ? "" : query;
        if (lastPickerQuery.isBlank()) {
            filteredPickerItems = new ArrayList<>(ALL_REGISTERED_ITEMS);
            return;
        }
        String q = lastPickerQuery.trim().toLowerCase();
        if (q.startsWith("@")) {
            String mod = q.substring(1).trim();
            filteredPickerItems = ALL_REGISTERED_ITEMS.stream()
                    .filter(it -> BuiltInRegistries.ITEM.getKey(it).getNamespace().toLowerCase().contains(mod))
                    .toList();
        } else {
            filteredPickerItems = ALL_REGISTERED_ITEMS.stream()
                    .filter(it -> {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
                        if (id.getPath().toLowerCase().contains(q) || id.toString().toLowerCase().contains(q)) return true;
                        String name = it.getDescription().getString().toLowerCase();
                        return name.contains(q);
                    })
                    .toList();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (data == null) return;

        initDefaultRfqItem();

        if (showCreateAuctionModal) {
            initCreateAuctionWidgets();
            return;
        }

        if (showCreateQuestModal) {
            initCreateQuestWidgets();
            return;
        }

        if (selectedQuest != null) {
            initQuestDetailsWidgets();
            return;
        }

        if (showItemPickerModal) {
            initItemPickerWidgets();
            return;
        }

        dynamicCatalogButtons.clear();

        int mw = 400, mh = 260;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Header Account Toggle Button (if player has company)
        var comp = (data != null) ? data.company() : null;
        if (comp != null && comp.hasCompany()) {
            String compName = comp.companyName();
            if (this.font.width(compName) > 46) {
                compName = this.font.plainSubstrByWidth(compName, 40) + "..";
            }
            String toggleText = useCompanyAccount ? "§6🏢 " + compName : "§b👤 " + AmmoraLang.guiStr("account.personal");
            int toggleW = Math.max(54, Math.min(76, this.font.width(toggleText) + 10));
            int toggleX = mx + 130;
            this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
                useCompanyAccount = !useCompanyAccount;
                rebuildWidgets();
            }).bounds(toggleX, my + 4, toggleW, 16)
            .tooltip(Tooltip.create(Component.literal(
                    useCompanyAccount ? AmmoraLang.guiStr("account.switch_to_personal") : AmmoraLang.guiStr("account.switch_to_company")
            )))
            .build());
        }

        // Navigation Tabs (2 rows of 4 tabs, spacious 93px width)
        int tabW = 93;
        int tabH = 16;
        int tabY1 = my + 24;
        int tabY2 = my + 42;

        // Row 1: Market (0), Shops (1), Auctions (2), RFQ (3)
        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 0 ? AmmoraLang.guiStr("market.tab_market_active") : AmmoraLang.guiStr("market.tab_market_inactive")),
                b -> { activeTab = 0; catalogPage = 0; rebuildWidgets(); }
        ).bounds(mx + 8, tabY1, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 1 ? AmmoraLang.guiStr("market.tab_shops_active") : AmmoraLang.guiStr("market.tab_shops_inactive")),
                b -> { activeTab = 1; shopsPage = 0; rebuildWidgets(); }
        ).bounds(mx + 105, tabY1, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 2 ? AmmoraLang.guiStr("market.tab_auctions_active") : AmmoraLang.guiStr("market.tab_auctions_inactive")),
                b -> { activeTab = 2; auctionPage = 0; rebuildWidgets(); }
        ).bounds(mx + 202, tabY1, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 3 ? AmmoraLang.guiStr("market.tab_rfq_active") : AmmoraLang.guiStr("market.tab_rfq_inactive")),
                b -> { activeTab = 3; reqPage = 0; rebuildWidgets(); }
        ).bounds(mx + 299, tabY1, tabW, tabH).build());

        // Row 2: Quests (4), Company (5), Buffer (6), History (7)
        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 4 ? AmmoraLang.guiStr("market.tab_quests_active") : AmmoraLang.guiStr("market.tab_quests_inactive")),
                b -> { activeTab = 4; questsPage = 0; rebuildWidgets(); }
        ).bounds(mx + 8, tabY2, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 5 ? AmmoraLang.guiStr("market.tab_company_active") : AmmoraLang.guiStr("market.tab_company_inactive")),
                b -> { activeTab = 5; rebuildWidgets(); }
        ).bounds(mx + 105, tabY2, tabW, tabH).build());

        int delCount = (data != null && data.deliveries() != null) ? data.deliveries().size() : 0;
        String bufferTabTitle;
        if (delCount > 0) {
            bufferTabTitle = activeTab == 6 ? AmmoraLang.guiStr("market.tab_buffer_count_active", delCount) : AmmoraLang.guiStr("market.tab_buffer_count_inactive", delCount);
        } else {
            bufferTabTitle = activeTab == 6 ? AmmoraLang.guiStr("market.tab_buffer_active") : AmmoraLang.guiStr("market.tab_buffer_inactive");
        }
        this.addRenderableWidget(Button.builder(
                Component.literal(bufferTabTitle),
                b -> { activeTab = 6; deliveryPage = 0; rebuildWidgets(); }
        ).bounds(mx + 202, tabY2, tabW, tabH).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 7 ? AmmoraLang.guiStr("market.tab_history_active") : AmmoraLang.guiStr("market.tab_history_inactive")),
                b -> { activeTab = 7; txPage = 0; rebuildWidgets(); }
        ).bounds(mx + 299, tabY2, tabW, tabH).build());

        if (activeTab == 0) {
            // TAB 0: Global Market Catalog
            int searchW = (filterShopId != null && !filterShopId.isEmpty()) ? 115 : 140;
            searchBox = new EditBox(this.font, mx + 12, my + 62, searchW, 16, Component.literal(AmmoraLang.guiStr("market.search")));
            searchBox.setValue(lastCatalogSearch);
            searchBox.setResponder(val -> {
                lastCatalogSearch = val;
                catalogPage = 0;
                updateDynamicCatalogWidgets();
            });
            this.addRenderableWidget(searchBox);

            if (filterShopId != null && !filterShopId.isEmpty()) {
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_all_shops")), b -> {
                    filterShopId = null;
                    filterShopName = null;
                    catalogPage = 0;
                    rebuildWidgets();
                }).bounds(mx + 132, my + 62, 76, 16).build());
            }

            // Populate dynamic catalog pagination and purchase buttons
            updateDynamicCatalogWidgets();
        } else if (activeTab == 1) {
            // TAB 1: Shops Directory (Rust-style)
            var shops = data.shops();
            int totalPages = Math.max(1, (shops.size() + 4) / 5);

            if (shopsPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    shopsPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 62, 22, 16).build());
            }

            if (shopsPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    shopsPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 62, 22, 16).build());
            }

            int startIndex = shopsPage * 5;
            for (int i = 0; i < 5; i++) {
                int sIdx = startIndex + i;
                if (sIdx < shops.size()) {
                    var shop = shops.get(sIdx);
                    int rowY = my + 82 + i * 32;

                    // Replaced "To chat" with "Goods" to view products of this shop
                    this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_products")), b -> {
                        filterShopId = shop.shopId();
                        filterShopName = shop.shopName();
                        activeTab = 0;
                        catalogPage = 0;
                        rebuildWidgets();
                    }).bounds(mx + mw - 76, rowY + 6, 64, 18).build());
                }
            }
        } else if (activeTab == 2) {
            // TAB 2: Live Auctions
            var filtered = getFilteredAuctions();
            int totalPages = Math.max(1, (filtered.size() + 3) / 4);

            // Filter button: All
            this.addRenderableWidget(Button.builder(
                    Component.literal((auctionFilter == 0 ? "§6§l" : "§7") + AmmoraLang.guiStr("auction.filter_all")),
                    b -> { auctionFilter = 0; auctionPage = 0; rebuildWidgets(); }
            ).bounds(mx + 12, my + 62, 42, 16).build());

            // Filter button: My Lots
            this.addRenderableWidget(Button.builder(
                    Component.literal((auctionFilter == 1 ? "§6§l" : "§7") + AmmoraLang.guiStr("auction.filter_own")),
                    b -> { auctionFilter = 1; auctionPage = 0; rebuildWidgets(); }
            ).bounds(mx + 56, my + 62, 56, 16).build());

            // Filter button: My Bids
            this.addRenderableWidget(Button.builder(
                    Component.literal((auctionFilter == 2 ? "§6§l" : "§7") + AmmoraLang.guiStr("auction.filter_bids")),
                    b -> { auctionFilter = 2; auctionPage = 0; rebuildWidgets(); }
            ).bounds(mx + 114, my + 62, 56, 16).build());

            // [+ Create Lot] button
            this.addRenderableWidget(Button.builder(
                    Component.literal(AmmoraLang.guiStr("auction.create_lot")),
                    b -> { showCreateAuctionModal = true; createAuctionSlot = -1; rebuildWidgets(); }
            ).bounds(mx + 174, my + 62, 88, 16).build());

            if (auctionPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    auctionPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 62, 22, 16).build());
            }

            if (auctionPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    auctionPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 62, 22, 16).build());
            }

            int startIndex = auctionPage * 4;
            for (int i = 0; i < 4; i++) {
                int aIdx = startIndex + i;
                if (aIdx < filtered.size()) {
                    var a = filtered.get(aIdx);
                    int rowY = my + 82 + i * 39;

                    if (a.isOwn()) {
                        if (a.highestBidderUuid() == null) {
                            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("auction.btn_cancel")), b -> {
                                PacketDistributor.sendToServer(new ServerboundAuctionActionPayload(
                                        "CANCEL", a.auctionId(), -1, 0, 0, 0, 0, 0
                                ));
                            }).bounds(mx + mw - 76, rowY + 10, 64, 18).build());
                        }
                    } else {
                        double nextBid = a.getNextMinBid();
                        String bidLabel = AmmoraLang.guiStr("auction.btn_bid", String.format(Locale.US, "%.1f", nextBid));
                        if (a.hasBuyout()) {
                            String buyoutLabel = AmmoraLang.guiStr("auction.btn_buyout", String.format(Locale.US, "%.1f", a.buyoutPrice()));
                            var bidBtn = Button.builder(Component.literal(bidLabel), b -> {
                                PacketDistributor.sendToServer(new ServerboundAuctionActionPayload(
                                        "BID", a.auctionId(), -1, 0, 0, 0, 0, nextBid, useCompanyAccount
                                ));
                            }).bounds(mx + mw - 148, rowY + 10, 70, 18)
                            .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("auction.btn_bid_tooltip", String.format(Locale.US, "%.1f", nextBid)))))
                            .build();
                            this.addRenderableWidget(bidBtn);

                            var buyoutBtn = Button.builder(Component.literal(buyoutLabel), b -> {
                                PacketDistributor.sendToServer(new ServerboundAuctionActionPayload(
                                        "BUYOUT", a.auctionId(), -1, 0, 0, 0, 0, 0, useCompanyAccount
                                ));
                            }).bounds(mx + mw - 76, rowY + 10, 70, 18)
                            .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("auction.btn_buyout_tooltip", String.format(Locale.US, "%.1f", a.buyoutPrice())))))
                            .build();
                            this.addRenderableWidget(buyoutBtn);
                        } else {
                            var bidBtn = Button.builder(Component.literal(bidLabel), b -> {
                                PacketDistributor.sendToServer(new ServerboundAuctionActionPayload(
                                        "BID", a.auctionId(), -1, 0, 0, 0, 0, nextBid, useCompanyAccount
                                ));
                            }).bounds(mx + mw - 80, rowY + 10, 74, 18)
                            .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("auction.btn_bid_tooltip", String.format(Locale.US, "%.1f", nextBid)))))
                            .build();
                            this.addRenderableWidget(bidBtn);
                        }
                    }
                }
            }
        } else if (activeTab == 3) {
            // TAB 3: Buy Requests (RFQ)
            var reqs = data.buyRequests();
            int totalPages = Math.max(1, (reqs.size() + 3) / 4);

            if (reqPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    reqPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 62, 22, 16).build());
            }

            if (reqPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    reqPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 62, 22, 16).build());
            }

            int startIndex = reqPage * 4;
            for (int i = 0; i < 4; i++) {
                int rIdx = startIndex + i;
                if (rIdx < reqs.size()) {
                    var req = reqs.get(rIdx);
                    int rowY = my + 82 + i * 33;

                    if (req.isOwn()) {
                        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_cancel")), b -> {
                            PacketDistributor.sendToServer(new ServerboundBuyRequestPayload(
                                     "CANCEL", req.requestId(), "", "", 0, 0
                            ));
                        }).bounds(mx + mw - 76, rowY + 6, 64, 18).build());
                    } else {
                        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_deliver_1")), b -> {
                            PacketDistributor.sendToServer(new ServerboundBuyRequestPayload(
                                    "FULFILL", req.requestId(), "", "", 0, 1
                            ));
                        }).bounds(mx + mw - 76, rowY + 6, 64, 18).build());
                    }
                }
            }

            // Bottom bar: JEI item search picker and order creation
            int footY = my + mh - 36;

            // Search Item Button (Opens JEI-style Modal)
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_search")), b -> {
                showItemPickerModal = true;
                updateFilteredPickerItems(lastPickerQuery);
                rebuildWidgets();
            }).bounds(mx + 30, footY + 8, 58, 18).build());

            // Offhand item shortcut button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_offhand")), b -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    ItemStack off = this.minecraft.player.getOffhandItem();
                    if (!off.isEmpty()) {
                        selectedRfqItem = off.copyWithCount(1);
                        statusNotification = AmmoraLang.guiStr("market.notif_offhand_selected", selectedRfqItem.getHoverName().getString());
                        statusNotificationError = false;
                        notificationExpireTime = System.currentTimeMillis() + 3000L;
                    } else {
                        statusNotification = AmmoraLang.guiStr("market.notif_offhand_empty");
                        statusNotificationError = true;
                        notificationExpireTime = System.currentTimeMillis() + 3500L;
                    }
                }
            }).bounds(mx + 92, footY + 8, 56, 18).build());

            // Price input box
            bountyPriceBox = new EditBox(this.font, mx + 164, footY + 8, 48, 18, Component.literal(AmmoraLang.guiStr("market.box_price")));
            bountyPriceBox.setMaxLength(8);
            bountyPriceBox.setValue(lastBountyPrice);
            bountyPriceBox.setResponder(val -> lastBountyPrice = val);
            this.addRenderableWidget(bountyPriceBox);

            // Count input box
            bountyCountBox = new EditBox(this.font, mx + 228, footY + 8, 34, 18, Component.literal(AmmoraLang.guiStr("market.box_qty")));
            bountyCountBox.setMaxLength(4);
            bountyCountBox.setValue(lastBountyCount);
            bountyCountBox.setResponder(val -> lastBountyCount = val);
            this.addRenderableWidget(bountyCountBox);

            // Submit Buy Request Button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_create_rfq")), b -> {
                if (selectedRfqItem == null || selectedRfqItem.isEmpty()) {
                    statusNotification = AmmoraLang.guiStr("market.notif_select_item_first");
                    statusNotificationError = true;
                    notificationExpireTime = System.currentTimeMillis() + 3500L;
                    return;
                }
                try {
                    double price = Double.parseDouble(bountyPriceBox.getValue().replace(',', '.'));
                    int count = Integer.parseInt(bountyCountBox.getValue());
                    if (price <= 0 || count <= 0) {
                        statusNotification = AmmoraLang.guiStr("market.notif_invalid_price_qty");
                        statusNotificationError = true;
                        notificationExpireTime = System.currentTimeMillis() + 3500L;
                        return;
                    }
                    String itemId = BuiltInRegistries.ITEM.getKey(selectedRfqItem.getItem()).toString();
                    String name = selectedRfqItem.getHoverName().getString();
                    PacketDistributor.sendToServer(new ServerboundBuyRequestPayload(
                            "CREATE", "", itemId, name, price, count
                    ));
                } catch (NumberFormatException ignored) {
                    statusNotification = AmmoraLang.guiStr("market.notif_invalid_number");
                    statusNotificationError = true;
                    notificationExpireTime = System.currentTimeMillis() + 3500L;
                }
            }).bounds(mx + 266, footY + 8, 126, 18).build());

        } else if (activeTab == 4) {
            // TAB 4: Community Quests & Bounties
            var quests = data.quests();
            int totalPages = Math.max(1, (quests.size() + 3) / 4);

            // [+ Create Quest] button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_new_quest")), b -> {
                showCreateQuestModal = true;
                rebuildWidgets();
            }).bounds(mx + 12, my + 62, 85, 16).build());

            if (questsPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    questsPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 62, 22, 16).build());
            }

            if (questsPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    questsPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 62, 22, 16).build());
            }

            int startIndex = questsPage * 4;
            for (int i = 0; i < 4; i++) {
                int qIdx = startIndex + i;
                if (qIdx < quests.size()) {
                    var q = quests.get(qIdx);
                    int rowY = my + 82 + i * 38;

                    this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_open")), b -> {
                        selectedQuest = q;
                        rebuildWidgets();
                    }).bounds(mx + mw - 76, rowY + 8, 64, 18).build());
                }
            }
        } else if (activeTab == 5) {
            initCompanyTab(mx, my, mw, mh);
        } else if (activeTab == 6) {
            // TAB 6: Delivery Buffer (10-slot persistent storage)
            var deliveries = (data != null && data.deliveries() != null) ? data.deliveries() : List.<MarketplaceDataPayload.DeliveryBufferItem>of();
            int totalPages = Math.max(1, (deliveries.size() + 9) / 10);

            if (deliveryPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    deliveryPage--;
                    rebuildWidgets();
                }).bounds(mx + 194, my + 62, 20, 16).build());
            }

            if (deliveryPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    deliveryPage++;
                    rebuildWidgets();
                }).bounds(mx + 216, my + 62, 20, 16).build());
            }

            // Big Action Button "Claim All"
            var claimAllBtn = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_claim_all_colored")), b -> {
                PacketDistributor.sendToServer(new ServerboundClaimDeliveryPayload("", true));
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }).bounds(mx + mw - 150, my + 62, 140, 16).build();
            claimAllBtn.active = !deliveries.isEmpty();
            this.addRenderableWidget(claimAllBtn);

            // Add [Take] buttons for each visible delivery (up to 10 slots)
            int startIndex = deliveryPage * 10;
            for (int i = 0; i < 10; i++) {
                int idx = startIndex + i;
                if (idx < deliveries.size()) {
                    var del = deliveries.get(idx);
                    int col = i % 2;
                    int row = i / 2;
                    int cardX = (col == 0) ? (mx + 10) : (mx + 204);
                    int cardY = my + 82 + row * 32;
                    int cardW = 186;

                    var takeBtn = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_take")), b -> {
                        PacketDistributor.sendToServer(new ServerboundClaimDeliveryPayload(del.deliveryId(), false));
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                    }).bounds(cardX + cardW - 44, cardY + 5, 40, 18).build();
                    this.addRenderableWidget(takeBtn);
                }
            }
        } else if (activeTab == 7) {
            // TAB 7: History Ledger
            var txs = data.transactions();
            int totalPages = Math.max(1, (txs.size() + 4) / 5);

            if (txPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    txPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 62, 22, 16).build());
            }

            if (txPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    txPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 62, 22, 16).build());
            }
        }
    }

    private void initCreateAuctionWidgets() {
        int modalW = 310, modalH = 200;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
            showCreateAuctionModal = false;
            rebuildWidgets();
        }).bounds(modalX + modalW - 18, modalY + 4, 14, 14).build());

        int rightX = modalX + 184;

        auctionStartPriceBox = new EditBox(this.font, rightX, modalY + 34, 114, 15, Component.literal(AmmoraLang.guiStr("auction.start_price_label")));
        auctionStartPriceBox.setMaxLength(8);
        auctionStartPriceBox.setValue(lastAuctionStartPrice);
        auctionStartPriceBox.setResponder(s -> lastAuctionStartPrice = s);
        this.addRenderableWidget(auctionStartPriceBox);

        auctionMinStepBox = new EditBox(this.font, rightX, modalY + 62, 114, 15, Component.literal(AmmoraLang.guiStr("auction.step_label")));
        auctionMinStepBox.setMaxLength(8);
        auctionMinStepBox.setValue(lastAuctionMinStep);
        auctionMinStepBox.setResponder(s -> lastAuctionMinStep = s);
        this.addRenderableWidget(auctionMinStepBox);

        auctionBuyoutBox = new EditBox(this.font, rightX, modalY + 90, 114, 15, Component.literal(AmmoraLang.guiStr("auction.buyout_label")));
        auctionBuyoutBox.setMaxLength(8);
        auctionBuyoutBox.setValue(lastAuctionBuyout);
        auctionBuyoutBox.setResponder(s -> lastAuctionBuyout = s);
        this.addRenderableWidget(auctionBuyoutBox);

        int[] durations = {60, 360, 720, 1440};
        String[] durLabels = {
                AmmoraLang.guiStr("auction.dur_1h"),
                AmmoraLang.guiStr("auction.dur_6h"),
                AmmoraLang.guiStr("auction.dur_12h"),
                AmmoraLang.guiStr("auction.dur_24h")
        };
        for (int i = 0; i < 4; i++) {
            int dur = durations[i];
            String label = (selectedAuctionDurationMins == dur ? "§6§l" : "§7") + durLabels[i];
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selectedAuctionDurationMins = dur;
                rebuildWidgets();
            }).bounds(rightX + i * 29, modalY + 118, 26, 16).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("auction.btn_publish")), b -> {
            if (this.minecraft == null || this.minecraft.player == null) return;
            if (createAuctionSlot < 0 || createAuctionSlot >= this.minecraft.player.getInventory().items.size()) {
                statusNotification = AmmoraLang.guiStr("auction.notif_no_item");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }
            ItemStack selectedStack = this.minecraft.player.getInventory().getItem(createAuctionSlot);
            if (selectedStack.isEmpty()) {
                statusNotification = AmmoraLang.guiStr("auction.notif_no_item");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }

            double startPrice;
            double minStep;
            double buyout;
            try {
                startPrice = Double.parseDouble(auctionStartPriceBox.getValue().replace(',', '.'));
                minStep = Double.parseDouble(auctionMinStepBox.getValue().replace(',', '.'));
                buyout = Double.parseDouble(auctionBuyoutBox.getValue().replace(',', '.'));
                if (startPrice <= 0 || minStep <= 0 || buyout < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                statusNotification = AmmoraLang.guiStr("auction.notif_invalid_input");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }

            if (buyout > 0.0 && buyout <= startPrice) {
                statusNotification = AmmoraLang.guiStr("auction.notif_buyout_low");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }

            PacketDistributor.sendToServer(new ServerboundAuctionActionPayload(
                    "CREATE", "", createAuctionSlot, startPrice, minStep, buyout, selectedAuctionDurationMins, 0.0
            ));
            showCreateAuctionModal = false;
            createAuctionSlot = -1;
            rebuildWidgets();
        }).bounds(modalX + 12, modalY + 168, modalW - 24, 20).build());
    }

    private void initCreateQuestWidgets() {
        int modalW = 280, modalH = 200;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
            showCreateQuestModal = false;
            rebuildWidgets();
        }).bounds(modalX + modalW - 18, modalY + 4, 14, 14).build());

        questTitleBox = new EditBox(this.font, modalX + 12, modalY + 38, modalW - 24, 16, Component.literal(AmmoraLang.guiStr("market.box_title")));
        questTitleBox.setMaxLength(50);
        questTitleBox.setValue(lastQuestTitle);
        questTitleBox.setResponder(s -> lastQuestTitle = s);
        this.addRenderableWidget(questTitleBox);

        questRewardBox = new EditBox(this.font, modalX + 12, modalY + 76, 90, 16, Component.literal(AmmoraLang.guiStr("market.box_reward")));
        questRewardBox.setMaxLength(8);
        questRewardBox.setValue(lastQuestReward);
        questRewardBox.setResponder(s -> lastQuestReward = s);
        this.addRenderableWidget(questRewardBox);

        questDescBox = new EditBox(this.font, modalX + 12, modalY + 114, modalW - 24, 30, Component.literal(AmmoraLang.guiStr("market.box_desc")));
        questDescBox.setMaxLength(300);
        questDescBox.setValue(lastQuestDesc);
        questDescBox.setResponder(s -> lastQuestDesc = s);
        this.addRenderableWidget(questDescBox);

        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_publish_quest")), b -> {
            String title = questTitleBox.getValue().trim();
            String desc = questDescBox.getValue().trim();
            if (title.isEmpty()) {
                statusNotification = AmmoraLang.guiStr("market.quest_err_title");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }
            if (desc.isEmpty()) {
                statusNotification = AmmoraLang.guiStr("market.quest_err_desc");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }
            double reward;
            try {
                reward = Double.parseDouble(questRewardBox.getValue().replace(',', '.'));
                if (reward < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                statusNotification = AmmoraLang.guiStr("market.quest_err_reward");
                statusNotificationError = true;
                notificationExpireTime = System.currentTimeMillis() + 3500L;
                return;
            }

            PacketDistributor.sendToServer(new ServerboundCommunityQuestPayload(
                    "CREATE", "", title, desc, reward
            ));
            showCreateQuestModal = false;
            lastQuestTitle = "";
            lastQuestDesc = "";
            rebuildWidgets();
        }).bounds(modalX + 12, modalY + 162, modalW - 24, 20).build());
    }

    private void initQuestDetailsWidgets() {
        if (selectedQuest == null) return;
        int modalW = 320, modalH = 210;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Close button [✕]
        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
            selectedQuest = null;
            rebuildWidgets();
        }).bounds(modalX + modalW - 18, modalY + 4, 14, 14).build());

        int btnY = modalY + modalH - 26;

        // If creator: can delete quest
        if (selectedQuest.isOwn()) {
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_delete_quest")), b -> {
                PacketDistributor.sendToServer(new ServerboundCommunityQuestPayload(
                        "DELETE", selectedQuest.questId(), "", "", 0
                ));
                selectedQuest = null;
                rebuildWidgets();
            }).bounds(modalX + 12, btnY, 68, 18).build());
        }

        // If OPEN and NOT own: can accept
        if ("OPEN".equalsIgnoreCase(selectedQuest.status()) && !selectedQuest.isOwn()) {
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_accept_quest")), b -> {
                PacketDistributor.sendToServer(new ServerboundCommunityQuestPayload(
                        "ACCEPT", selectedQuest.questId(), "", "", 0
                ));
                selectedQuest = null;
                rebuildWidgets();
            }).bounds(modalX + modalW - 130, btnY, 118, 18).build());
        }

        // If IN_PROGRESS:
        if ("IN_PROGRESS".equalsIgnoreCase(selectedQuest.status())) {
            // Cancel work (worker or creator)
            if (selectedQuest.isAssignedToMe() || selectedQuest.isOwn()) {
                int cancelX = selectedQuest.isOwn() ? (modalX + 84) : (modalX + 12);
                this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_abandon_quest")), b -> {
                    PacketDistributor.sendToServer(new ServerboundCommunityQuestPayload(
                            "CANCEL_WORK", selectedQuest.questId(), "", "", 0
                    ));
                    selectedQuest = null;
                    rebuildWidgets();
                }).bounds(cancelX, btnY, 80, 18).build());
            }

            // If own: P2P pay reward button
            if (selectedQuest.isOwn()) {
                String payLabel = AmmoraLang.guiStr("market.btn_payout_quest", String.format(Locale.US, "%.0f", selectedQuest.rewardCbx()));
                this.addRenderableWidget(Button.builder(Component.literal(payLabel), b -> {
                    PacketDistributor.sendToServer(new ServerboundCommunityQuestPayload(
                            "PAY_REWARD", selectedQuest.questId(), "", "", 0
                    ));
                    selectedQuest = null;
                    rebuildWidgets();
                }).bounds(modalX + modalW - 146, btnY, 134, 18).build());
            }
        }
    }

    private void initItemPickerWidgets() {
        int modalW = 280, modalH = 190;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        pickerSearchBox = new EditBox(this.font, modalX + 12, modalY + 24, 150, 16, Component.literal(AmmoraLang.guiStr("market.search")));
        pickerSearchBox.setValue(lastPickerQuery);
        pickerSearchBox.setResponder(text -> {
            updateFilteredPickerItems(text);
            pickerPage = 0;
        });
        this.addRenderableWidget(pickerSearchBox);
        this.setInitialFocus(pickerSearchBox);

        // Prev page
        this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (pickerPage > 0) pickerPage--;
        }).bounds(modalX + modalW - 48, modalY + 24, 18, 16).build());

        // Next page
        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            int totalPages = Math.max(1, (filteredPickerItems.size() + 59) / 60);
            if (pickerPage < totalPages - 1) pickerPage++;
        }).bounds(modalX + modalW - 26, modalY + 24, 18, 16).build());

        // Close button [✕]
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
            showItemPickerModal = false;
            rebuildWidgets();
        }).bounds(modalX + modalW - 18, modalY + 4, 14, 14).build());
    }

    private void updateDynamicCatalogWidgets() {
        for (Button b : dynamicCatalogButtons) {
            this.removeWidget(b);
        }
        dynamicCatalogButtons.clear();

        if (data == null || activeTab != 0) return;

        int mw = 400, mh = 260;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        var filtered = getFilteredCatalog();
        int totalPages = Math.max(1, (filtered.size() + 4) / 5);
        if (catalogPage >= totalPages) {
            catalogPage = Math.max(0, totalPages - 1);
        }

        if (catalogPage > 0) {
            var prevBtn = Button.builder(Component.literal("◀"), b -> {
                catalogPage--;
                updateDynamicCatalogWidgets();
            }).bounds(mx + mw - 60, my + 62, 22, 16).build();
            dynamicCatalogButtons.add(prevBtn);
            this.addRenderableWidget(prevBtn);
        }

        if (catalogPage < totalPages - 1) {
            var nextBtn = Button.builder(Component.literal("▶"), b -> {
                catalogPage++;
                updateDynamicCatalogWidgets();
            }).bounds(mx + mw - 34, my + 62, 22, 16).build();
            dynamicCatalogButtons.add(nextBtn);
            this.addRenderableWidget(nextBtn);
        }

        int startIndex = catalogPage * 5;
        for (int i = 0; i < 5; i++) {
            int itemIdx = startIndex + i;
            if (itemIdx < filtered.size()) {
                var item = filtered.get(itemIdx);
                int rowY = my + 82 + i * 32;
                int stock = item.stockCount();

                int maxStack = 64;
                try {
                    Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.itemId()));
                    if (it != null && it != Items.AIR) {
                        maxStack = it.getDefaultInstance().getMaxStackSize();
                    }
                } catch (Exception ignored) {}

                if (stock <= 1 || maxStack <= 1) {
                    double total = calcRemoteTotal(item.priceCbx(), 1);
                    var btn1 = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_buy_1")), b -> {
                        attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 1);
                    }).bounds(mx + mw - 76, rowY + 6, 66, 18)
                    .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_tooltip", String.format(Locale.US, "%.2f", total)))))
                    .build();
                    dynamicCatalogButtons.add(btn1);
                    this.addRenderableWidget(btn1);
                } else if (stock < 16 || maxStack < 16) {
                    int buyAll = Math.min(stock, maxStack);
                    double total1 = calcRemoteTotal(item.priceCbx(), 1);
                    var btn1 = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_buy_1")), b -> {
                        attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 1);
                    }).bounds(mx + mw - 124, rowY + 6, 52, 18)
                    .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_tooltip", String.format(Locale.US, "%.2f", total1)))))
                    .build();
                    dynamicCatalogButtons.add(btn1);
                    this.addRenderableWidget(btn1);

                    double totalAll = calcRemoteTotal(item.priceCbx(), buyAll);
                    var btnAll = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_buy_all", buyAll)), b -> {
                        attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), buyAll);
                    }).bounds(mx + mw - 68, rowY + 6, 58, 18)
                    .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_n_tooltip", buyAll, String.format(Locale.US, "%.2f", totalAll)))))
                    .build();
                    dynamicCatalogButtons.add(btnAll);
                    this.addRenderableWidget(btnAll);
                } else {
                    int maxBuy = Math.min(stock, maxStack);
                    if (maxBuy <= 16) {
                        double total1 = calcRemoteTotal(item.priceCbx(), 1);
                        var btn1 = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_buy_1")), b -> {
                            attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 1);
                        }).bounds(mx + mw - 124, rowY + 6, 52, 18)
                        .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_tooltip", String.format(Locale.US, "%.2f", total1)))))
                        .build();
                        dynamicCatalogButtons.add(btn1);
                        this.addRenderableWidget(btn1);

                        double total16 = calcRemoteTotal(item.priceCbx(), 16);
                        var btn16 = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_buy_16")), b -> {
                            attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 16);
                        }).bounds(mx + mw - 68, rowY + 6, 58, 18)
                        .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_n_tooltip", 16, String.format(Locale.US, "%.2f", total16)))))
                        .build();
                        dynamicCatalogButtons.add(btn16);
                        this.addRenderableWidget(btn16);
                    } else {
                        double total1 = calcRemoteTotal(item.priceCbx(), 1);
                        var btn1 = Button.builder(Component.literal("§a1"), b -> {
                            attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 1);
                        }).bounds(mx + mw - 146, rowY + 6, 38, 18)
                        .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_tooltip", String.format(Locale.US, "%.2f", total1)))))
                        .build();
                        dynamicCatalogButtons.add(btn1);
                        this.addRenderableWidget(btn1);

                        double total16 = calcRemoteTotal(item.priceCbx(), 16);
                        var btn16 = Button.builder(Component.literal("§e16"), b -> {
                            attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), 16);
                        }).bounds(mx + mw - 104, rowY + 6, 42, 18)
                        .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_n_tooltip", 16, String.format(Locale.US, "%.2f", total16)))))
                        .build();
                        dynamicCatalogButtons.add(btn16);
                        this.addRenderableWidget(btn16);

                        String maxLabel = (maxBuy == 64) ? "§664" : ("§6" + maxBuy);
                        double totalMax = calcRemoteTotal(item.priceCbx(), maxBuy);
                        var btnMax = Button.builder(Component.literal(maxLabel), b -> {
                            attemptPurchase(item.shopId(), item.slotIndex(), item.itemId(), maxBuy);
                        }).bounds(mx + mw - 58, rowY + 6, 48, 18)
                        .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("market.btn_buy_n_tooltip", maxBuy, String.format(Locale.US, "%.2f", totalMax)))))
                        .build();
                        dynamicCatalogButtons.add(btnMax);
                        this.addRenderableWidget(btnMax);
                    }
                }
            }
        }
    }

    private void attemptPurchase(String shopId, int slotIndex, String itemId, int count) {
        if (this.minecraft != null && this.minecraft.player != null) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (it != null && it != Items.AIR) {
                ItemStack probe = new ItemStack(it);
                if (!com.ammora.mod.network.PacketHandler.canPlayerHoldItem(this.minecraft.player.getInventory(), probe, count)) {
                    triggerLocalNotification(AmmoraLang.messageStr("inventory_full"), true);
                    return;
                }
            }
        }
        PacketDistributor.sendToServer(new ServerboundShopPurchasePayload(
                shopId, slotIndex, count, true, useCompanyAccount
        ));
    }

    private static double calcRemoteTotal(double unitPrice, int count) {
        double baseTotal = unitPrice * count;
        double fee = Math.max(1.0, Math.round(baseTotal * 0.02 * 100.0) / 100.0);
        return baseTotal + fee;
    }

    private List<MarketplaceDataPayload.MarketplaceSlotItem> getFilteredCatalog() {
        if (data == null) return List.of();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : lastCatalogSearch.toLowerCase();
        var stream = data.catalogSlots().stream();
        if (filterShopId != null && !filterShopId.isEmpty()) {
            stream = stream.filter(s -> s.shopId().equals(filterShopId));
        }
        if (!query.isEmpty()) {
            stream = stream.filter(s -> s.displayName().toLowerCase().contains(query)
                    || s.itemId().toLowerCase().contains(query)
                    || s.ownerName().toLowerCase().contains(query));
        }
        return stream.toList();
    }

    private String truncate(String text, int maxPixelWidth) {
        if (text == null) return "";
        if (this.font.width(text) <= maxPixelWidth) return text;
        return this.font.plainSubstrByWidth(text, maxPixelWidth - 8) + "..";
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderBlurredBackground(partialTicks);
        }

        // Darkened background backdrop
        gg.fill(0, 0, this.width, this.height, 0xAA000000);

        if (data == null) {
            gg.drawCenteredString(this.font, AmmoraLang.guiStr("market.loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        int mw = 400, mh = 260;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        if (showCreateAuctionModal) {
            // Full screen solid dark dim overlay to isolate modal
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            renderCreateAuctionModal(gg, mouseX, mouseY);
            super.render(gg, mouseX, mouseY, partialTicks);
            renderCreateAuctionTooltip(gg, mouseX, mouseY);
            return;
        }

        if (showCreateQuestModal) {
            // Full screen solid dark dim overlay to isolate modal
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            renderCreateQuestModal(gg, mouseX, mouseY);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        if (selectedQuest != null) {
            // Full screen solid dark dim overlay to isolate modal
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            renderQuestDetailsModal(gg, mouseX, mouseY);
            super.render(gg, mouseX, mouseY, partialTicks);
            return;
        }

        if (showItemPickerModal) {
            // Full screen solid dark dim overlay to isolate modal
            gg.fill(0, 0, this.width, this.height, 0xEE04070E);
            // Render modal backdrop and item grid with solid surfaces
            renderItemPickerModal(gg, mouseX, mouseY);
            // Render modal widgets (searchbox, buttons)
            super.render(gg, mouseX, mouseY, partialTicks);
            // Tooltip for item under cursor in modal
            renderPickerHoverTooltip(gg, mouseX, mouseY);
            return;
        }

        // Background
        gg.fill(mx, my, mx + mw, my + mh, COLOR_BG);
        drawOutlinedBox(gg, mx, my, mw, mh, COLOR_BORDER_CYAN);

        // Header Panel
        gg.fill(mx + 1, my + 1, mx + mw - 1, my + 22, COLOR_PANEL_HEADER);
        gg.hLine(mx + 1, mx + mw - 1, my + 22, COLOR_BORDER_MUTED);

        // Separator below 2nd row of tabs
        gg.hLine(mx + 1, mx + mw - 1, my + 60, COLOR_BORDER_MUTED);

        gg.drawString(this.font, "§b✦ " + AmmoraLang.guiStr("market.title") + " ✦", mx + 10, my + 8, 0xFFFFFFFF);
        String balStr;
        if (useCompanyAccount && data.company() != null && data.company().hasCompany()) {
            balStr = "§6🏢 " + String.format(Locale.US, "%.2f CBX", data.company().balanceCbx());
        } else {
            balStr = AmmoraLang.guiStr("market.header_balance_rank", String.format(Locale.US, "%.2f", data.balanceCbx()), data.repLevel());
        }
        gg.drawString(this.font, balStr, mx + mw - this.font.width(balStr) - 10, my + 8, 0xFFFFFFFF);

        MarketplaceDataPayload.MarketplaceSlotItem hoveredCatalogItem = null;
        MarketplaceDataPayload.LiveAuctionItem hoveredAuction = null;
        MarketplaceDataPayload.BuyRequestItem hoveredRfq = null;

        if (activeTab == 0) {
            hoveredCatalogItem = renderCatalogTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 1) {
            renderShopsTab(gg, mx, my, mw, mh);
        } else if (activeTab == 2) {
            hoveredAuction = renderAuctionsTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 3) {
            hoveredRfq = renderBuyRequestsTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 4) {
            renderQuestsTab(gg, mx, my, mw, mh);
        } else if (activeTab == 5) {
            renderCompanyTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 6) {
            renderDeliveryBufferTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 7) {
            renderHistoryTab(gg, mx, my, mw, mh);
        }

        super.render(gg, mouseX, mouseY, partialTicks);

        // Floating status notification toast positioned at top above window (never covers buttons)
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

        // Catalog Tooltip
        if (!showItemPickerModal && !showCreateQuestModal && !showCreateAuctionModal && selectedQuest == null && hoveredCatalogItem != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredCatalogItem.displayName()));
            tooltip.add(Component.literal("§7ID: §8" + hoveredCatalogItem.itemId()));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_shop", hoveredCatalogItem.shopName(), hoveredCatalogItem.ownerName())));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_unit_price", String.format(Locale.US, "%.2f", hoveredCatalogItem.priceCbx()))));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_delivery_fee", String.format(Locale.US, "%.2f", hoveredCatalogItem.deliveryFeeCbx()))));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_in_stock", hoveredCatalogItem.stockCount())));
            for (String l : hoveredCatalogItem.lore()) {
                if (isDurabilityLine(l)) {
                    tooltip.add(Component.literal(l));
                } else {
                    tooltip.add(Component.literal("§d" + l));
                }
            }
            gg.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // Live Auction Tooltip
        if (!showItemPickerModal && !showCreateQuestModal && !showCreateAuctionModal && selectedQuest == null && hoveredAuction != null) {
            ItemStack st = reconstructAuctionStack(hoveredAuction);
            if (!st.isEmpty()) {
                gg.renderTooltip(this.font, st, mouseX, mouseY);
            }
        }

        // RFQ Card Tooltip
        if (!showItemPickerModal && !showCreateQuestModal && !showCreateAuctionModal && selectedQuest == null && hoveredRfq != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredRfq.displayName()));
            tooltip.add(Component.literal("§8ID: " + hoveredRfq.itemId()));
            tooltip.add(Component.literal(hoveredRfq.isOwn() ? AmmoraLang.guiStr("market.tooltip_your_rfq") : AmmoraLang.guiStr("market.tooltip_buyer", hoveredRfq.buyerName())));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_rfq_bounty", String.format(Locale.US, "%.2f", hoveredRfq.unitPrice()))));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_rfq_remaining", hoveredRfq.remainingAmount())));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.tooltip_rfq_escrow", String.format(Locale.US, "%.2f", hoveredRfq.escrowCbx()))));
            gg.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // RFQ Selected Item Tooltip in footer
        if (!showItemPickerModal && !showCreateQuestModal && !showCreateAuctionModal && selectedQuest == null && activeTab == 3) {
            int footY = my + mh - 36;
            if (mouseX >= mx + 8 && mouseX <= mx + 26 && mouseY >= footY + 8 && mouseY <= footY + 26 && selectedRfqItem != null && !selectedRfqItem.isEmpty()) {
                gg.renderTooltip(this.font, selectedRfqItem, mouseX, mouseY);
            }
        }
    }

    private MarketplaceDataPayload.MarketplaceSlotItem renderCatalogTab(GuiGraphics gg, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        var filtered = getFilteredCatalog();
        int totalPages = Math.max(1, (filtered.size() + 4) / 5);

        if (filterShopId != null && !filterShopId.isEmpty()) {
            String shopFilterTitle = AmmoraLang.guiStr("market.filter_shop_offers", truncate(filterShopName, 120), filtered.size());
            gg.drawString(this.font, shopFilterTitle, mx + 212, my + 66, 0xFFFFFFFF);
        } else {
            gg.drawString(this.font, AmmoraLang.guiStr("market.catalog_pages", (catalogPage + 1), totalPages, filtered.size()), mx + 160, my + 66, COLOR_TEXT_MUTED);
        }

        int startIndex = catalogPage * 5;
        MarketplaceDataPayload.MarketplaceSlotItem hovered = null;

        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 82 + i * 32;

            if (idx < filtered.size()) {
                var item = filtered.get(idx);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, COLOR_PANEL);
                drawOutlinedBox(gg, mx + 10, rowY, mw - 20, 30, COLOR_BORDER_MUTED);

                // Render item
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.itemId()));
                if (it != Items.AIR) {
                    ItemStack st = new ItemStack(it, 1);
                    gg.renderItem(st, mx + 16, rowY + 7);
                    gg.renderItemDecorations(this.font, st, mx + 16, rowY + 7, "");
                }

                // Item info without collision
                gg.drawString(this.font, truncate("§f" + item.displayName(), 115), mx + 38, rowY + 5, 0xFFFFFFFF);
                String sub = "§8" + item.shopName() + " (§7" + item.ownerName() + "§8)";
                gg.drawString(this.font, truncate(sub, 115), mx + 38, rowY + 17, 0xFFFFFFFF);

                // Price and delivery fee
                String priceStr = "§e" + String.format("%.2f", item.priceCbx()) + " §8(+§c" + String.format("%.1f", item.deliveryFeeCbx()) + "§8) CBX";
                gg.drawString(this.font, truncate(priceStr, 90), mx + 158, rowY + 5, 0xFFFFFFFF);
                gg.drawString(this.font, AmmoraLang.guiStr("market.tooltip_in_stock", item.stockCount()), mx + 158, rowY + 17, 0xFFFFFFFF);

                if (mouseX >= mx + 10 && mouseX <= mx + 250 && mouseY >= rowY && mouseY <= rowY + 30) {
                    hovered = item;
                }
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, 0x44080D16);
            }
        }

        return hovered;
    }

    private void renderShopsTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var shops = data.shops();
        int totalPages = Math.max(1, (shops.size() + 4) / 5);
        gg.drawString(this.font, AmmoraLang.guiStr("market.shops_online_header", shops.size(), (shopsPage + 1), totalPages), mx + 12, my + 66, 0xFFFFFFFF);

        int startIndex = shopsPage * 5;
        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 82 + i * 32;

            if (idx < shops.size()) {
                var shop = shops.get(idx);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, COLOR_PANEL);
                drawOutlinedBox(gg, mx + 10, rowY, mw - 20, 30, COLOR_BORDER_MUTED);

                gg.drawString(this.font, truncate("§b🏪 " + shop.shopName(), 155), mx + 16, rowY + 5, 0xFFFFFFFF);
                String coords = String.format("§7[%d, %d, %d] §8(%s)", shop.posX(), shop.posY(), shop.posZ(), shop.dimension());
                gg.drawString(this.font, truncate(coords, 155), mx + 16, rowY + 17, 0xFFFFFFFF);

                gg.drawString(this.font, truncate(AmmoraLang.guiStr("market.shop_owner_label", shop.ownerName()), 130), mx + 180, rowY + 5, 0xFFFFFFFF);
                gg.drawString(this.font, AmmoraLang.guiStr("market.shop_stats", shop.activeSlotsCount(), shop.totalSales()), mx + 180, rowY + 17, 0xFFFFFFFF);
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, 0x44080D16);
            }
        }
    }

    private MarketplaceDataPayload.BuyRequestItem renderBuyRequestsTab(GuiGraphics gg, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        var reqs = data.buyRequests();
        int totalPages = Math.max(1, (reqs.size() + 3) / 4);
        gg.drawString(this.font, AmmoraLang.guiStr("market.rfq_header", reqs.size(), (reqPage + 1), totalPages), mx + 12, my + 66, 0xFFFFFFFF);

        int startIndex = reqPage * 4;
        MarketplaceDataPayload.BuyRequestItem hovered = null;

        for (int i = 0; i < 4; i++) {
            int idx = startIndex + i;
            int rowY = my + 82 + i * 33;

            if (idx < reqs.size()) {
                var req = reqs.get(idx);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 31, COLOR_PANEL);
                drawOutlinedBox(gg, mx + 10, rowY, mw - 20, 31, req.isOwn() ? COLOR_BORDER_CYAN : COLOR_BORDER_MUTED);

                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(req.itemId()));
                if (it != Items.AIR) {
                    ItemStack st = new ItemStack(it, 1);
                    gg.renderItem(st, mx + 16, rowY + 7);
                    gg.renderItemDecorations(this.font, st, mx + 16, rowY + 7, "");
                }

                // Clean title without registry ID to prevent collision
                gg.drawString(this.font, truncate("§f" + req.displayName(), 130), mx + 38, rowY + 5, 0xFFFFFFFF);
                String buyerInfo = req.isOwn() ? AmmoraLang.guiStr("market.rfq_your_order") : AmmoraLang.guiStr("market.rfq_from", req.buyerName());
                gg.drawString(this.font, truncate(buyerInfo, 130), mx + 38, rowY + 17, 0xFFFFFFFF);

                String bounty = AmmoraLang.guiStr("market.rfq_price_pcs", String.format(Locale.US, "%.2f", req.unitPrice()));
                gg.drawString(this.font, bounty, mx + 165, rowY + 5, 0xFFFFFFFF);

                String escrow = AmmoraLang.guiStr("market.rfq_escrow_badge", String.format(Locale.US, "%.1f", req.escrowCbx()));
                int escrowW = this.font.width(escrow);
                gg.drawString(this.font, escrow, mx + mw - 82 - escrowW, rowY + 5, 0xFFFFFFFF);

                String needed = AmmoraLang.guiStr("market.rfq_needed", req.remainingAmount());
                gg.drawString(this.font, needed, mx + 165, rowY + 17, 0xFFFFFFFF);

                if (mouseX >= mx + 10 && mouseX <= mx + mw - 78 && mouseY >= rowY && mouseY <= rowY + 31) {
                    hovered = req;
                }
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 31, 0x44080D16);
            }
        }

        // Bottom panel banner for creating request
        int footY = my + mh - 36;
        gg.fill(mx + 1, footY, mx + mw - 1, my + mh - 1, COLOR_PANEL_HEADER);
        gg.hLine(mx + 1, mx + mw - 1, footY, COLOR_BORDER_MUTED);

        // Slot preview box for selected item
        gg.fill(mx + 8, footY + 8, mx + 26, footY + 26, 0xCC090E18);
        drawOutlinedBox(gg, mx + 8, footY + 8, 18, 18, COLOR_BORDER_CYAN);
        if (selectedRfqItem != null && !selectedRfqItem.isEmpty()) {
            gg.renderItem(selectedRfqItem, mx + 9, footY + 9);
            gg.renderItemDecorations(this.font, selectedRfqItem, mx + 9, footY + 9, "");
        }

        gg.drawString(this.font, "§8CBX:", mx + 144, footY + 13, 0xFFFFFFFF);
        gg.drawString(this.font, "§8x:", mx + 216, footY + 13, 0xFFFFFFFF);

        return hovered;
    }

    private void renderQuestsTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var quests = data.quests();
        int totalPages = Math.max(1, (quests.size() + 3) / 4);
        gg.drawString(this.font, AmmoraLang.guiStr("market.quests_header", quests.size(), (questsPage + 1), totalPages), mx + 105, my + 66, 0xFFFFFFFF);

        int startIndex = questsPage * 4;
        for (int i = 0; i < 4; i++) {
            int idx = startIndex + i;
            int rowY = my + 82 + i * 38;

            if (idx < quests.size()) {
                var q = quests.get(idx);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 35, COLOR_PANEL);
                int borderColor = q.isOwn() ? COLOR_BORDER_CYAN : (q.isAssignedToMe() ? COLOR_GREEN : COLOR_BORDER_MUTED);
                drawOutlinedBox(gg, mx + 10, rowY, mw - 20, 35, borderColor);

                // Quest title & badge
                String titleText = (q.isOwn() ? "§b★ " : "§e📋 ") + q.title();
                gg.drawString(this.font, truncate(titleText, 175), mx + 16, rowY + 6, 0xFFFFFFFF);

                // Reward
                String rewardStr = AmmoraLang.guiStr("market.quest_reward", String.format(Locale.US, "%.2f", q.rewardCbx()));
                gg.drawString(this.font, rewardStr, mx + 200, rowY + 6, 0xFFFFFFFF);

                // Line 2: Status & Creator/Worker
                String statusStr;
                if ("OPEN".equalsIgnoreCase(q.status())) {
                    statusStr = AmmoraLang.guiStr("market.quest_status_open");
                } else if ("IN_PROGRESS".equalsIgnoreCase(q.status())) {
                    statusStr = AmmoraLang.guiStr("market.quest_status_in_progress", truncate(q.workerName(), 60));
                } else {
                    statusStr = AmmoraLang.guiStr("market.quest_status_done");
                }
                gg.drawString(this.font, statusStr, mx + 16, rowY + 20, 0xFFFFFFFF);

                String authorStr = q.isOwn() ? AmmoraLang.guiStr("market.quest_your_order") : AmmoraLang.guiStr("market.quest_creator", truncate(q.creatorName(), 75));
                gg.drawString(this.font, authorStr, mx + 200, rowY + 20, 0xFFFFFFFF);
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 35, 0x44080D16);
            }
        }
    }

    private void renderCreateQuestModal(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 280, modalH = 200;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Solid modal panel
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);
        drawOutlinedBox(gg, modalX + 1, modalY + 1, modalW - 2, modalH - 2, 0xFF142032);

        // Header
        gg.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + 22, 0xFF0E1626);
        gg.hLine(modalX + 2, modalX + modalW - 2, modalY + 22, 0xFF1C2C44);
        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_new_title"), modalX + 8, modalY + 7, 0xFFFFFFFF);

        // Labels
        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_short_name"), modalX + 12, modalY + 26, 0xFFFFFFFF);
        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_reward_label"), modalX + 12, modalY + 64, 0xFFFFFFFF);
        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_desc_label"), modalX + 12, modalY + 102, 0xFFFFFFFF);
    }

    private void renderQuestDetailsModal(GuiGraphics gg, int mouseX, int mouseY) {
        if (selectedQuest == null) return;
        int modalW = 320, modalH = 210;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Solid modal panel
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);
        drawOutlinedBox(gg, modalX + 1, modalY + 1, modalW - 2, modalH - 2, 0xFF142032);

        // Header
        gg.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + 22, 0xFF0E1626);
        gg.hLine(modalX + 2, modalX + modalW - 2, modalY + 22, 0xFF1C2C44);
        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_dossier_title"), modalX + 8, modalY + 7, 0xFFFFFFFF);

        // Title
        gg.drawString(this.font, "§e" + truncate(selectedQuest.title(), modalW - 30), modalX + 12, modalY + 28, 0xFFFFFFFF);

        // Meta info line 1
        String authorInfo = AmmoraLang.guiStr("market.quest_dossier_creator", selectedQuest.creatorName()) + (selectedQuest.isOwn() ? AmmoraLang.guiStr("market.quest_dossier_you") : "");
        gg.drawString(this.font, authorInfo, modalX + 12, modalY + 42, 0xFFFFFFFF);

        String rewardInfo = AmmoraLang.guiStr("market.quest_dossier_reward", String.format(Locale.US, "%.2f", selectedQuest.rewardCbx()));
        gg.drawString(this.font, rewardInfo, modalX + 180, modalY + 42, 0xFFFFFFFF);

        // Meta info line 2
        String statusStr;
        if ("OPEN".equalsIgnoreCase(selectedQuest.status())) {
            statusStr = AmmoraLang.guiStr("market.quest_dossier_status_open");
        } else if ("IN_PROGRESS".equalsIgnoreCase(selectedQuest.status())) {
            statusStr = AmmoraLang.guiStr("market.quest_dossier_status_progress", selectedQuest.workerName());
        } else {
            statusStr = AmmoraLang.guiStr("market.quest_dossier_status_done");
        }
        gg.drawString(this.font, statusStr, modalX + 12, modalY + 56, 0xFFFFFFFF);

        // Description box
        int descBoxY = modalY + 72;
        int descBoxH = modalH - 106;
        gg.fill(modalX + 10, descBoxY, modalX + modalW - 10, descBoxY + descBoxH, 0xFF0B111C);
        drawOutlinedBox(gg, modalX + 10, descBoxY, modalW - 20, descBoxH, 0xFF182333);

        gg.drawString(this.font, AmmoraLang.guiStr("market.quest_specs_label"), modalX + 14, descBoxY + 4, 0xFFFFFFFF);
        var wrappedLines = this.font.split(Component.literal(selectedQuest.description()), modalW - 28);
        int lineY = descBoxY + 16;
        for (int i = 0; i < Math.min(wrappedLines.size(), 6); i++) {
            gg.drawString(this.font, wrappedLines.get(i), modalX + 14, lineY + i * 11, 0xFFCCCCCC);
        }
    }

    private void renderDeliveryBufferTab(GuiGraphics gg, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        var deliveries = (data != null && data.deliveries() != null) ? data.deliveries() : List.<MarketplaceDataPayload.DeliveryBufferItem>of();
        int totalPages = Math.max(1, (deliveries.size() + 9) / 10);

        gg.drawString(this.font, AmmoraLang.guiStr("market.buffer_header", deliveries.size(), (deliveryPage + 1), totalPages), mx + 12, my + 66, 0xFFFFFFFF);

        int startIndex = deliveryPage * 10;
        MarketplaceDataPayload.DeliveryBufferItem hoveredItem = null;

        for (int i = 0; i < 10; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = (col == 0) ? (mx + 10) : (mx + 204);
            int cardY = my + 82 + row * 32;
            int cardW = 186;
            int cardH = 28;

            int idx = startIndex + i;
            if (idx < deliveries.size()) {
                var del = deliveries.get(idx);
                gg.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_PANEL);
                drawOutlinedBox(gg, cardX, cardY, cardW, cardH, COLOR_BORDER_CYAN);

                ItemStack st = ItemStack.EMPTY;
                if (del.itemNbt() != null && !del.itemNbt().isEmpty() && Minecraft.getInstance().level != null) {
                    try {
                        net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(del.itemNbt());
                        st = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), tag);
                    } catch (Exception ignored) {}
                }
                if (st.isEmpty()) {
                    Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(del.itemId()));
                    if (it != Items.AIR) {
                        st = new ItemStack(it, 1);
                    }
                }

                if (!st.isEmpty()) {
                    gg.renderItem(st, cardX + 5, cardY + 6);
                    String countStr = del.amount() > 1 ? String.valueOf(del.amount()) : "";
                    gg.renderItemDecorations(this.font, st, cardX + 5, cardY + 6, countStr);
                }

                String nameStr = truncate("§f" + del.displayName(), 88);
                gg.drawString(this.font, nameStr, cardX + 26, cardY + 5, 0xFFFFFFFF);
                gg.drawString(this.font, AmmoraLang.guiStr("market.buffer_qty", del.amount()), cardX + 26, cardY + 16, COLOR_TEXT_MUTED);

                if (mouseX >= cardX + 5 && mouseX <= cardX + 23 && mouseY >= cardY + 6 && mouseY <= cardY + 24) {
                    hoveredItem = del;
                }
            } else {
                // Visual 10-slot placeholder
                gg.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x44080D18);
                drawOutlinedBox(gg, cardX, cardY, cardW, cardH, 0x332A374A);
                gg.drawString(this.font, AmmoraLang.guiStr("market.buffer_slot_empty", (i + 1)), cardX + 50, cardY + 10, 0xFF556677);
            }
        }

        if (hoveredItem != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§e" + hoveredItem.displayName()));
            tooltip.add(Component.literal("§8ID: " + hoveredItem.itemId()));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.buffer_tooltip_qty", hoveredItem.amount())));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.buffer_tooltip_date", TIME_FMT.format(new Date(hoveredItem.timestamp())))));
            tooltip.add(Component.literal(AmmoraLang.guiStr("market.buffer_tooltip_hint")));
            gg.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderHistoryTab(GuiGraphics gg, int mx, int my, int mw, int mh) {
        var txs = data.transactions();
        int totalPages = Math.max(1, (txs.size() + 4) / 5);
        gg.drawString(this.font, AmmoraLang.guiStr("market.ledger_header", txs.size(), (txPage + 1), totalPages), mx + 12, my + 66, 0xFFFFFFFF);

        int startIndex = txPage * 5;
        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 82 + i * 32;

            if (idx < txs.size()) {
                var tx = txs.get(idx);
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, COLOR_PANEL);
                drawOutlinedBox(gg, mx + 10, rowY, mw - 20, 30, COLOR_BORDER_MUTED);

                String time = TIME_FMT.format(new Date(tx.timestamp()));
                String badge = switch (tx.txType()) {
                    case "REMOTE_BUY" -> AmmoraLang.guiStr("market.tag_remote_buy");
                    case "BUY_REQUEST" -> AmmoraLang.guiStr("market.tag_buy_request");
                    default -> AmmoraLang.guiStr("market.tag_local");
                };

                gg.drawString(this.font, truncate(time + " " + badge + " §f" + tx.itemName() + " x" + tx.amount(), 240), mx + 16, rowY + 5, 0xFFFFFFFF);
                String parties = AmmoraLang.guiStr("market.tx_parties", tx.buyerName(), tx.sellerName());
                gg.drawString(this.font, truncate(parties, 240), mx + 16, rowY + 17, 0xFFFFFFFF);

                String money = "§a" + String.format("%.2f", tx.totalCbx()) + " CBX" + (tx.feeCbx() > 0 ? " §8(+§c" + String.format("%.1f", tx.feeCbx()) + "§8)" : "");
                gg.drawString(this.font, money, mx + mw - this.font.width(money) - 20, rowY + 10, 0xFFFFFFFF);
            } else {
                gg.fill(mx + 10, rowY, mx + mw - 10, rowY + 30, 0x44080D16);
            }
        }
    }

    private MarketplaceDataPayload.LiveAuctionItem renderAuctionsTab(GuiGraphics gg, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        var filtered = getFilteredAuctions();
        int totalPages = Math.max(1, (filtered.size() + 3) / 4);

        // Page info in toolbar
        String pageStr = (auctionPage + 1) + "/" + totalPages;
        gg.drawString(this.font, pageStr, mx + mw - 66 - this.font.width(pageStr), my + 66, COLOR_TEXT_MUTED);

        if (filtered.isEmpty()) {
            gg.drawCenteredString(this.font, AmmoraLang.guiStr("auction.empty"), mx + mw / 2, my + 130, COLOR_TEXT_MUTED);
            return null;
        }

        MarketplaceDataPayload.LiveAuctionItem hoveredItem = null;
        int startIndex = auctionPage * 4;
        for (int i = 0; i < 4; i++) {
            int aIdx = startIndex + i;
            int cardY = my + 82 + i * 39;
            int cardW = mw - 20;
            int cardX = mx + 10;

            if (aIdx < filtered.size()) {
                var a = filtered.get(aIdx);
                int bg = a.isLeading() ? 0xEE0B1914 : (a.isOwn() ? 0xEE14110A : COLOR_PANEL);
                int border = a.isLeading() ? COLOR_GREEN : (a.isOwn() ? COLOR_AMBER : COLOR_BORDER_MUTED);

                gg.fill(cardX, cardY, cardX + cardW, cardY + 36, bg);
                drawOutlinedBox(gg, cardX, cardY, cardW, 36, border);

                // Slot box for item
                gg.fill(cardX + 6, cardY + 8, cardX + 26, cardY + 28, 0xFF0D1322);
                drawOutlinedBox(gg, cardX + 6, cardY + 8, 20, 20, COLOR_BORDER_MUTED);

                ItemStack st = reconstructAuctionStack(a);
                if (!st.isEmpty()) {
                    gg.renderItem(st, cardX + 8, cardY + 10);
                    String countStr = a.count() > 1 ? String.valueOf(a.count()) : "";
                    gg.renderItemDecorations(this.font, st, cardX + 8, cardY + 10, countStr);
                }

                if (mouseX >= cardX + 6 && mouseX <= cardX + 26 && mouseY >= cardY + 8 && mouseY <= cardY + 28) {
                    hoveredItem = a;
                }

                // Line 1: Item Name
                String nameStr = truncate("§f" + a.displayName() + (a.count() > 1 ? " x" + a.count() : ""), 140);
                gg.drawString(this.font, nameStr, cardX + 30, cardY + 4, 0xFFFFFFFF);

                // Line 2: Seller & Time left
                String timeStr = formatTimeRemaining(a.expiresAt());
                int timeColor = getTimeColor(a.expiresAt());
                String sellerStr = AmmoraLang.guiStr("auction.seller", a.sellerName());
                gg.drawString(this.font, truncate(sellerStr, 80), cardX + 30, cardY + 15, COLOR_TEXT_MUTED);
                gg.drawString(this.font, "⏱ " + timeStr, cardX + 115, cardY + 15, timeColor);

                // Line 3: Current bid / Start price & Leader badge
                if (a.currentBid() > 0.0) {
                    String bidStr = AmmoraLang.guiStr("auction.current_bid", String.format(Locale.US, "%.1f", a.currentBid()));
                    gg.drawString(this.font, bidStr, cardX + 30, cardY + 26, 0xFFFFFFFF);
                } else {
                    String startStr = AmmoraLang.guiStr("auction.start_price", String.format(Locale.US, "%.1f", a.startPrice()));
                    gg.drawString(this.font, startStr, cardX + 30, cardY + 26, 0xFFFFFFFF);
                }

                if (a.isLeading()) {
                    gg.drawString(this.font, AmmoraLang.guiStr("auction.leading_you"), cardX + 130, cardY + 26, COLOR_GREEN);
                } else if (a.highestBidderName() != null && !a.highestBidderName().isEmpty()) {
                    gg.drawString(this.font, AmmoraLang.guiStr("auction.leading_other", a.highestBidderName()), cardX + 130, cardY + 26, COLOR_TEXT_MUTED);
                } else {
                    gg.drawString(this.font, AmmoraLang.guiStr("auction.no_bids"), cardX + 130, cardY + 26, 0xFF556677);
                }

                // If own lot and has active bids: display "Bids Active" label instead of cancel button
                if (a.isOwn() && a.highestBidderUuid() != null) {
                    gg.drawString(this.font, AmmoraLang.guiStr("auction.active_bids_label"), cardX + cardW - 74, cardY + 14, COLOR_AMBER);
                }
            } else {
                // Visual placeholder
                gg.fill(cardX, cardY, cardX + cardW, cardY + 36, 0x33080D16);
            }
        }

        return hoveredItem;
    }

    private void renderCreateAuctionModal(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 310, modalH = 200;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Solid Frame (100% opaque, zero bleed-through)
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);

        // Header
        gg.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + 20, 0xFF0D1422);
        gg.hLine(modalX + 1, modalX + modalW - 1, modalY + 20, COLOR_BORDER_MUTED);
        gg.drawString(this.font, "§6✦ " + AmmoraLang.guiStr("auction.modal_title") + " ✦", modalX + 8, modalY + 6, 0xFFFFFFFF);

        // Subtitle above inventory
        gg.drawString(this.font, AmmoraLang.guiStr("auction.select_item"), modalX + 12, modalY + 24, COLOR_TEXT_MUTED);

        // Render player's 36 inventory slots:
        // Rows 0..2: slots 9..35 (main inventory)
        // Row 3: slots 0..8 (hotbar)
        int gridX = modalX + 12;
        int gridY = modalY + 36;
        if (this.minecraft != null && this.minecraft.player != null) {
            var inv = this.minecraft.player.getInventory();
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 9; c++) {
                    int slot = (r < 3) ? (9 + r * 9 + c) : c;
                    int sx = gridX + c * 18;
                    int sy = gridY + r * 18;

                    gg.fill(sx, sy, sx + 18, sy + 18, 0xFF0D1322);
                    boolean isSelected = (slot == createAuctionSlot);
                    int borderColor = isSelected ? COLOR_BORDER_CYAN : COLOR_BORDER_MUTED;
                    drawOutlinedBox(gg, sx, sy, 18, 18, borderColor);

                    if (slot < inv.items.size()) {
                        ItemStack st = inv.getItem(slot);
                        if (!st.isEmpty()) {
                            gg.renderItem(st, sx + 1, sy + 1);
                            gg.renderItemDecorations(this.font, st, sx + 1, sy + 1);

                            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                                drawOutlinedBox(gg, sx, sy, 18, 18, 0xFFFFFFFF);
                            }
                        }
                    }
                }
            }
        }

        // Preview box under inventory grid
        int prevSlotX = modalX + 12;
        int prevSlotY = modalY + 114;
        gg.fill(prevSlotX, prevSlotY, prevSlotX + 20, prevSlotY + 20, 0xFF0D1322);
        drawOutlinedBox(gg, prevSlotX, prevSlotY, 20, 20, COLOR_BORDER_MUTED);

        if (this.minecraft != null && this.minecraft.player != null && createAuctionSlot >= 0 && createAuctionSlot < this.minecraft.player.getInventory().items.size()) {
            ItemStack selected = this.minecraft.player.getInventory().getItem(createAuctionSlot);
            if (!selected.isEmpty()) {
                gg.renderItem(selected, prevSlotX + 2, prevSlotY + 2);
                gg.renderItemDecorations(this.font, selected, prevSlotX + 2, prevSlotY + 2);
                String nameStr = truncate("§f" + selected.getHoverName().getString() + " x" + selected.getCount(), 140);
                gg.drawString(this.font, nameStr, modalX + 36, prevSlotY + 6, 0xFFFFFFFF);
            } else {
                gg.drawString(this.font, AmmoraLang.guiStr("auction.selected_item_none"), modalX + 36, prevSlotY + 6, COLOR_TEXT_MUTED);
            }
        } else {
            gg.drawString(this.font, AmmoraLang.guiStr("auction.selected_item_none"), modalX + 36, prevSlotY + 6, COLOR_TEXT_MUTED);
        }

        // Labels on right side
        int rightX = modalX + 184;
        gg.drawString(this.font, AmmoraLang.guiStr("auction.start_price_label"), rightX, modalY + 24, COLOR_TEXT_MUTED);
        gg.drawString(this.font, AmmoraLang.guiStr("auction.step_label"), rightX, modalY + 52, COLOR_TEXT_MUTED);
        gg.drawString(this.font, AmmoraLang.guiStr("auction.buyout_label"), rightX, modalY + 80, COLOR_TEXT_MUTED);
        gg.drawString(this.font, AmmoraLang.guiStr("auction.duration_label"), rightX, modalY + 108, COLOR_TEXT_MUTED);
    }

    private void renderCreateAuctionTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        int modalW = 310, modalH = 200;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;
        int gridX = modalX + 12;
        int gridY = modalY + 36;

        var inv = this.minecraft.player.getInventory();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = (r < 3) ? (9 + r * 9 + c) : c;
                int sx = gridX + c * 18;
                int sy = gridY + r * 18;

                if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                    if (slot < inv.items.size()) {
                        ItemStack st = inv.getItem(slot);
                        if (!st.isEmpty()) {
                            gg.renderTooltip(this.font, st, mouseX, mouseY);
                            return;
                        }
                    }
                }
            }
        }
    }

    private ItemStack reconstructAuctionStack(MarketplaceDataPayload.LiveAuctionItem a) {
        ItemStack st = ItemStack.EMPTY;
        if (a.itemNbt() != null && !a.itemNbt().isEmpty() && this.minecraft != null && this.minecraft.level != null) {
            try {
                net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(a.itemNbt());
                st = ItemStack.parseOptional(this.minecraft.level.registryAccess(), tag);
            } catch (Exception ignored) {}
        }
        if (st.isEmpty()) {
            try {
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(a.itemId()));
                if (it != Items.AIR) {
                    st = new ItemStack(it, a.count());
                }
            } catch (Exception ignored) {}
        }
        return st;
    }

    private String formatTimeRemaining(long expiresAt) {
        long diff = expiresAt - System.currentTimeMillis();
        if (diff <= 0) {
            return AmmoraLang.guiStr("auction.time_expired");
        }
        long seconds = diff / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.US, "%02dm %02ds", minutes, secs);
        } else {
            return String.format(Locale.US, "%02ds", secs);
        }
    }

    private int getTimeColor(long expiresAt) {
        long diff = expiresAt - System.currentTimeMillis();
        if (diff < 60_000L) {
            return COLOR_RED;
        } else if (diff < 600_000L) {
            return COLOR_AMBER;
        } else {
            return COLOR_GREEN;
        }
    }

    private List<MarketplaceDataPayload.LiveAuctionItem> getFilteredAuctions() {
        if (data == null || data.auctions() == null) return List.of();
        var stream = data.auctions().stream();
        if (auctionFilter == 1) {
            stream = stream.filter(MarketplaceDataPayload.LiveAuctionItem::isOwn);
        } else if (auctionFilter == 2) {
            stream = stream.filter(MarketplaceDataPayload.LiveAuctionItem::isLeading);
        }
        return stream.toList();
    }

    private void renderItemPickerModal(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 280, modalH = 190;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Solid Frame (100% opaque, zero bleed-through)
        gg.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF080D18);
        drawOutlinedBox(gg, modalX, modalY, modalW, modalH, COLOR_BORDER_CYAN);

        // Header
        gg.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + 20, 0xFF0D1422);
        gg.hLine(modalX + 1, modalX + modalW - 1, modalY + 20, COLOR_BORDER_MUTED);
        gg.drawString(this.font, AmmoraLang.guiStr("market.picker_title"), modalX + 8, modalY + 6, 0xFFFFFFFF);

        // Page info
        int totalPages = Math.max(1, (filteredPickerItems.size() + 59) / 60);
        String pageStr = (pickerPage + 1) + "/" + totalPages;
        gg.drawString(this.font, pageStr, modalX + modalW - 54 - this.font.width(pageStr), modalY + 28, COLOR_TEXT_MUTED);

        // 12 columns x 5 rows grid
        int gridStartX = modalX + (modalW - 12 * 18) / 2;
        int gridStartY = modalY + 44;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 12; c++) {
                int slotX = gridStartX + c * 18;
                int slotY = gridStartY + r * 18;
                int idx = pickerPage * 60 + r * 12 + c;

                gg.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF0D1322);
                drawOutlinedBox(gg, slotX, slotY, 18, 18, COLOR_BORDER_MUTED);

                if (idx < filteredPickerItems.size()) {
                    Item it = filteredPickerItems.get(idx);
                    ItemStack st = new ItemStack(it);
                    gg.renderItem(st, slotX + 1, slotY + 1);

                    if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                        drawOutlinedBox(gg, slotX, slotY, 18, 18, COLOR_BORDER_CYAN);
                    }
                }
            }
        }

        // Subtitle instructions
        gg.drawString(this.font, AmmoraLang.guiStr("market.picker_found", filteredPickerItems.size()), modalX + 12, modalY + 140, 0xFFFFFFFF);
        gg.drawString(this.font, AmmoraLang.guiStr("market.picker_hint"), modalX + 12, modalY + 152, 0xFFFFFFFF);
    }

    private void renderPickerHoverTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        int modalW = 280, modalH = 190;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;
        int gridStartX = modalX + (modalW - 12 * 18) / 2;
        int gridStartY = modalY + 44;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 12; c++) {
                int slotX = gridStartX + c * 18;
                int slotY = gridStartY + r * 18;
                int idx = pickerPage * 60 + r * 12 + c;

                if (idx < filteredPickerItems.size()) {
                    if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                        ItemStack st = new ItemStack(filteredPickerItems.get(idx));
                        gg.renderTooltip(this.font, st, mouseX, mouseY);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showCreateAuctionModal) {
            int modalW = 310, modalH = 200;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            if (mouseX >= modalX + modalW - 20 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                showCreateAuctionModal = false;
                rebuildWidgets();
                return true;
            }
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                showCreateAuctionModal = false;
                rebuildWidgets();
                return true;
            }

            int gridX = modalX + 12;
            int gridY = modalY + 36;
            if (this.minecraft != null && this.minecraft.player != null) {
                var inv = this.minecraft.player.getInventory();
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 9; c++) {
                        int sx = gridX + c * 18;
                        int sy = gridY + r * 18;
                        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                            int slot = (r < 3) ? (9 + r * 9 + c) : c;
                            if (slot < inv.items.size() && !inv.getItem(slot).isEmpty()) {
                                createAuctionSlot = slot;
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                                return true;
                            }
                        }
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (showCreateQuestModal) {
            int modalW = 280, modalH = 200;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            if (mouseX >= modalX + modalW - 20 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                showCreateQuestModal = false;
                rebuildWidgets();
                return true;
            }
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                showCreateQuestModal = false;
                rebuildWidgets();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectedQuest != null) {
            int modalW = 320, modalH = 210;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            if (mouseX >= modalX + modalW - 20 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                selectedQuest = null;
                rebuildWidgets();
                return true;
            }
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                selectedQuest = null;
                rebuildWidgets();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (showItemPickerModal) {
            int modalW = 280, modalH = 190;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            // Close button [X]
            if (mouseX >= modalX + modalW - 20 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 20) {
                showItemPickerModal = false;
                rebuildWidgets();
                return true;
            }

            // Click inside grid
            int gridStartX = modalX + (modalW - 12 * 18) / 2;
            int gridStartY = modalY + 44;

            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 12; c++) {
                    int slotIdx = pickerPage * 60 + r * 12 + c;
                    if (slotIdx < filteredPickerItems.size()) {
                        int sx = gridStartX + c * 18;
                        int sy = gridStartY + r * 18;
                        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                            Item it = filteredPickerItems.get(slotIdx);
                            selectedRfqItem = new ItemStack(it);
                            if (this.minecraft != null) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            }
                            showItemPickerModal = false;
                            rebuildWidgets();
                            return true;
                        }
                    }
                }
            }

            // Click outside closes modal
            if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                showItemPickerModal = false;
                rebuildWidgets();
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Click on preview item slot in RFQ footer opens search modal
        if (activeTab == 3) {
            int mw = 400, mh = 260;
            int mx = (this.width - mw) / 2;
            int my = (this.height - mh) / 2;
            int footY = my + mh - 36;
            if (mouseX >= mx + 8 && mouseX <= mx + 26 && mouseY >= footY + 8 && mouseY <= footY + 26) {
                showItemPickerModal = true;
                updateFilteredPickerItems(lastPickerQuery);
                rebuildWidgets();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showCreateAuctionModal) {
            if (keyCode == 256) {
                showCreateAuctionModal = false;
                rebuildWidgets();
                return true;
            }
            boolean textFocused = (auctionStartPriceBox != null && auctionStartPriceBox.isFocused())
                    || (auctionMinStepBox != null && auctionMinStepBox.isFocused())
                    || (auctionBuyoutBox != null && auctionBuyoutBox.isFocused());
            if (!textFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                showCreateAuctionModal = false;
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (showCreateQuestModal) {
            if (keyCode == 256) {
                showCreateQuestModal = false;
                rebuildWidgets();
                return true;
            }
            boolean textFocused = (questTitleBox != null && questTitleBox.isFocused())
                    || (questRewardBox != null && questRewardBox.isFocused())
                    || (questDescBox != null && questDescBox.isFocused());
            if (!textFocused && this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                showCreateQuestModal = false;
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (selectedQuest != null) {
            if (keyCode == 256 || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                selectedQuest = null;
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (showItemPickerModal) {
            if (keyCode == 256) {
                showItemPickerModal = false;
                rebuildWidgets();
                return true;
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                if (pickerSearchBox == null || !pickerSearchBox.isFocused()) {
                    showItemPickerModal = false;
                    rebuildWidgets();
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            boolean textFocused = (searchBox != null && searchBox.isFocused())
                    || (bountyPriceBox != null && bountyPriceBox.isFocused())
                    || (bountyCountBox != null && bountyCountBox.isFocused())
                    || (auctionStartPriceBox != null && auctionStartPriceBox.isFocused())
                    || (auctionMinStepBox != null && auctionMinStepBox.isFocused())
                    || (auctionBuyoutBox != null && auctionBuyoutBox.isFocused())
                    || (companyNameInput != null && companyNameInput.isFocused())
                    || (companyAmountInput != null && companyAmountInput.isFocused())
                    || (companyInviteInput != null && companyInviteInput.isFocused());
            if (!textFocused) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void initCompanyTab(int mx, int my, int mw, int mh) {
        var comp = (data != null) ? data.company() : MarketplaceDataPayload.CompanyData.none();
        if (comp == null || !comp.hasCompany()) {
            // UNREGISTERED: Registration Card (widened to 360px so text fits without overflow)
            int cardW = 360, cardH = 150;
            int cx = mx + (mw - cardW) / 2;
            int cy = my + 72;

            companyNameInput = new EditBox(this.font, cx + 20, cy + 64, 206, 18, Component.literal(AmmoraLang.guiStr("company.name_hint")));
            companyNameInput.setMaxLength(24);
            companyNameInput.setValue(lastCompanyName);
            companyNameInput.setResponder(v -> lastCompanyName = v);
            this.addRenderableWidget(companyNameInput);

            double fee = comp != null ? comp.registrationFee() : 500.0;
            var regBtn = Button.builder(Component.literal(AmmoraLang.guiStr("company.btn_create")), b -> {
                String name = companyNameInput.getValue().trim();
                if (name.length() >= 3) {
                    PacketDistributor.sendToServer(ServerboundCompanyActionPayload.register(name));
                }
            }).bounds(cx + 234, cy + 64, 106, 18)
            .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("company.fee_notice", String.format(Locale.US, "%.2f", fee)))))
            .build();
            this.addRenderableWidget(regBtn);
            return;
        }

        // REGISTERED
        // Quick Amount box & Buttons in header bar
        companyAmountInput = new EditBox(this.font, mx + mw - 190, my + 65, 56, 16, Component.literal("100.0"));
        companyAmountInput.setMaxLength(10);
        companyAmountInput.setValue(lastCompanyAmount);
        companyAmountInput.setResponder(v -> lastCompanyAmount = v);
        this.addRenderableWidget(companyAmountInput);

        // Deposit Button (All members)
        this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("company.btn_deposit")), b -> {
            try {
                double amt = Double.parseDouble(companyAmountInput.getValue().replace(',', '.'));
                if (amt > 0) {
                    PacketDistributor.sendToServer(ServerboundCompanyActionPayload.deposit(amt));
                }
            } catch (NumberFormatException ignored) {}
        }).bounds(mx + mw - 130, my + 65, 58, 16).build());

        // Withdraw Button (Owner & Manager)
        if (!comp.isMember()) {
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("company.btn_withdraw")), b -> {
                try {
                    double amt = Double.parseDouble(companyAmountInput.getValue().replace(',', '.'));
                    if (amt > 0) {
                        PacketDistributor.sendToServer(ServerboundCompanyActionPayload.withdraw(amt));
                    }
                } catch (NumberFormatException ignored) {}
            }).bounds(mx + mw - 68, my + 65, 58, 16).build());
        }

        // Left Column: Team Roster
        var members = comp.members();
        int totalMemPages = Math.max(1, (members.size() + 2) / 3);
        if (companyMemberPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                companyMemberPage--;
                rebuildWidgets();
            }).bounds(mx + 130, my + 90, 18, 14).build());
        }
        if (companyMemberPage < totalMemPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                companyMemberPage++;
                rebuildWidgets();
            }).bounds(mx + 152, my + 90, 18, 14).build());
        }

        // Member action buttons
        int startMem = companyMemberPage * 3;
        for (int i = 0; i < 3; i++) {
            int idx = startMem + i;
            if (idx < members.size()) {
                var m = members.get(idx);
                int cardY = my + 108 + i * 36;

                if (comp.isOwner() && !m.playerUuid().equals(comp.ownerUuid())) {
                    boolean isManager = "MANAGER".equalsIgnoreCase(m.role());
                    String roleToggleIcon = isManager ? "§7👤" : "§b👔";
                    String roleToggleTip = isManager ? AmmoraLang.guiStr("company.demote_tooltip") : AmmoraLang.guiStr("company.promote_tooltip");

                    this.addRenderableWidget(Button.builder(Component.literal(roleToggleIcon), b -> {
                        String newRole = isManager ? "MEMBER" : "MANAGER";
                        PacketDistributor.sendToServer(ServerboundCompanyActionPayload.setRole(m.playerUuid(), newRole));
                    }).bounds(mx + 138, cardY + 7, 20, 18)
                    .tooltip(Tooltip.create(Component.literal(roleToggleTip)))
                    .build());

                    this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> {
                        PacketDistributor.sendToServer(ServerboundCompanyActionPayload.kick(m.playerUuid()));
                    }).bounds(mx + 160, cardY + 7, 20, 18)
                    .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("company.kick_tooltip"))))
                    .build());
                }
            }
        }

        // Bottom Left: Invite (Owner) or Leave (Member)
        if (comp.isOwner()) {
            companyInviteInput = new EditBox(this.font, mx + 10, my + 230, 88, 16, Component.literal(AmmoraLang.guiStr("company.invite_hint")));
            companyInviteInput.setMaxLength(16);
            companyInviteInput.setValue(lastCompanyInvite);
            companyInviteInput.setResponder(v -> lastCompanyInvite = v);
            this.addRenderableWidget(companyInviteInput);

            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("company.btn_invite")), b -> {
                String inv = companyInviteInput.getValue().trim();
                if (!inv.isEmpty()) {
                    PacketDistributor.sendToServer(ServerboundCompanyActionPayload.invite(inv));
                    companyInviteInput.setValue("");
                    lastCompanyInvite = "";
                }
            }).bounds(mx + 102, my + 230, 44, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("§c⚠"), b -> {
                PacketDistributor.sendToServer(ServerboundCompanyActionPayload.dissolve());
            }).bounds(mx + 150, my + 230, 30, 16)
            .tooltip(Tooltip.create(Component.literal(AmmoraLang.guiStr("company.dissolve_tooltip"))))
            .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("company.btn_leave")), b -> {
                PacketDistributor.sendToServer(ServerboundCompanyActionPayload.leave());
            }).bounds(mx + 10, my + 230, 170, 16).build());
        }

        // Right Column: Audit Ledger pagination
        var ledger = comp.ledger();
        int totalLedgerPages = Math.max(1, (ledger.size() + 4) / 5);
        if (companyLedgerPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                companyLedgerPage--;
                rebuildWidgets();
            }).bounds(mx + mw - 54, my + 90, 18, 14).build());
        }
        if (companyLedgerPage < totalLedgerPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                companyLedgerPage++;
                rebuildWidgets();
            }).bounds(mx + mw - 32, my + 90, 18, 14).build());
        }
    }

    private void renderCompanyTab(GuiGraphics gg, int mx, int my, int mw, int mh, int mouseX, int mouseY) {
        var comp = (data != null) ? data.company() : MarketplaceDataPayload.CompanyData.none();
        if (comp == null || !comp.hasCompany()) {
            // UNREGISTERED: Registration Card (widened to 360px so text fits cleanly)
            int cardW = 360, cardH = 150;
            int cx = mx + (mw - cardW) / 2;
            int cy = my + 72;

            gg.fill(cx, cy, cx + cardW, cy + cardH, COLOR_PANEL);
            drawOutlinedBox(gg, cx, cy, cardW, cardH, COLOR_BORDER_CYAN);

            gg.drawCenteredString(this.font, "§6§l" + AmmoraLang.guiStr("company.register_title"), cx + cardW / 2, cy + 14, 0xFFFFFFFF);
            gg.drawCenteredString(this.font, "§7" + AmmoraLang.guiStr("company.register_desc1"), cx + cardW / 2, cy + 30, 0xFFFFFFFF);
            gg.drawCenteredString(this.font, "§8" + AmmoraLang.guiStr("company.register_desc2"), cx + cardW / 2, cy + 42, 0xFFFFFFFF);

            double fee = comp != null ? comp.registrationFee() : 500.0;
            gg.drawString(this.font, "§e" + AmmoraLang.guiStr("company.fee_label", String.format(Locale.US, "%.2f", fee)), cx + 20, cy + 96, 0xFFFFFFFF);
            return;
        }

        // REGISTERED: Top Info Bar
        gg.fill(mx + 10, my + 62, mx + mw - 10, my + 84, COLOR_PANEL);
        drawOutlinedBox(gg, mx + 10, my + 62, mw - 20, 22, COLOR_BORDER_MUTED);

        String roleTag = comp.isOwner() ? "§6👑 " + AmmoraLang.guiStr("company.role_owner")
                : (comp.isManager() ? "§b👔 " + AmmoraLang.guiStr("company.role_manager") : "§7👤 " + AmmoraLang.guiStr("company.role_member"));

        String compHeader = "§6🏢 " + comp.companyName() + " §8| " + roleTag + " §8| §e" + String.format(Locale.US, "%.2f CBX", comp.balanceCbx());
        gg.drawString(this.font, compHeader, mx + 16, my + 69, 0xFFFFFFFF);

        // Left: Team Roster
        int leftX = mx + 10;
        int leftW = 175;
        gg.drawString(this.font, "§b👥 " + AmmoraLang.guiStr("company.members_title", comp.members().size()), leftX + 2, my + 92, 0xFFFFFFFF);

        var members = comp.members();
        int startMem = companyMemberPage * 3;
        for (int i = 0; i < 3; i++) {
            int idx = startMem + i;
            int cardY = my + 108 + i * 36;
            if (idx < members.size()) {
                var m = members.get(idx);
                gg.fill(leftX, cardY, leftX + leftW, cardY + 32, COLOR_PANEL);
                drawOutlinedBox(gg, leftX, cardY, leftW, 32, COLOR_BORDER_MUTED);

                String memRole = "OWNER".equalsIgnoreCase(m.role()) ? "§6👑" : ("MANAGER".equalsIgnoreCase(m.role()) ? "§b👔" : "§7👤");
                gg.drawString(this.font, memRole + " §f" + truncate(m.playerName(), 70), leftX + 4, cardY + 5, 0xFFFFFFFF);

                String limStr = "OWNER".equalsIgnoreCase(m.role()) ? "§8" + AmmoraLang.guiStr("company.limit_unlimited")
                        : String.format(Locale.US, "§7%.0f/%.0f", m.spentTodayCbx(), m.dailyLimitCbx());
                gg.drawString(this.font, "§8" + AmmoraLang.guiStr("company.spend_label") + ": " + limStr, leftX + 4, cardY + 18, 0xFFFFFFFF);
            } else {
                gg.fill(leftX, cardY, leftX + leftW, cardY + 32, 0x33080D16);
            }
        }

        // Right: Financial Audit Ledger
        int rightX = mx + 195;
        int rightW = mw - 205;
        gg.drawString(this.font, "§b📜 " + AmmoraLang.guiStr("company.ledger_title"), rightX + 2, my + 92, 0xFFFFFFFF);

        var ledger = comp.ledger();
        int startLedger = companyLedgerPage * 5;
        MarketplaceDataPayload.CompanyLedgerItem hoveredEntry = null;

        for (int i = 0; i < 5; i++) {
            int idx = startLedger + i;
            int rowY = my + 108 + i * 23;
            if (idx < ledger.size()) {
                var l = ledger.get(idx);
                boolean hovered = (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= rowY && mouseY <= rowY + 21);
                if (hovered) hoveredEntry = l;

                gg.fill(rightX, rowY, rightX + rightW, rowY + 21, hovered ? 0x66182333 : COLOR_PANEL);
                drawOutlinedBox(gg, rightX, rowY, rightW, 21, COLOR_BORDER_MUTED);

                String timeStr = TIME_FMT.format(new Date(l.timestamp()));
                String actionTag = switch (l.actionType()) {
                    case "REGISTRATION" -> "§cREG";
                    case "DEPOSIT" -> "§aDEP";
                    case "WITHDRAW" -> "§cWTH";
                    case "TRANSFER_IN" -> "§a+TR";
                    case "TRANSFER_OUT" -> "§c-TR";
                    case "SHOP_REVENUE" -> "§a+SH";
                    case "DOCK_REVENUE" -> "§a+DK";
                    case "DOCK_EXPENSE" -> "§c-DK";
                    case "MARKET_BUY" -> "§eBUY";
                    case "AUCTION_BUY" -> "§eAUC";
                    default -> "§7" + l.actionType();
                };

                String amtStr = (l.amountCbx() > 0) ? String.format(Locale.US, "%.1f", l.amountCbx()) : "";
                gg.drawString(this.font, "§8" + timeStr + " " + actionTag + " §f" + truncate(l.playerName(), 45), rightX + 4, rowY + 3, 0xFFFFFFFF);
                gg.drawString(this.font, "§e" + amtStr, rightX + rightW - this.font.width(amtStr) - 4, rowY + 3, 0xFFFFFFFF);
                gg.drawString(this.font, "§8" + truncate(l.description(), 160), rightX + 4, rowY + 12, 0xFFFFFFFF);
            } else {
                gg.fill(rightX, rowY, rightX + rightW, rowY + 21, 0x22080D16);
            }
        }

        if (hoveredEntry != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§6" + hoveredEntry.actionType() + " §8| §f" + String.format(Locale.US, "%.2f CBX", hoveredEntry.amountCbx())));
            tip.add(Component.literal("§7" + AmmoraLang.guiStr("company.actor") + ": §f" + hoveredEntry.playerName()));
            tip.add(Component.literal("§7" + AmmoraLang.guiStr("company.info") + ": §e" + hoveredEntry.description()));
            tip.add(Component.literal("§8" + new Date(hoveredEntry.timestamp())));
            gg.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
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

    private boolean isDurabilityLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("\u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c") || lower.contains("durability") || (lower.contains("(") && lower.contains("%)"));
    }
}
