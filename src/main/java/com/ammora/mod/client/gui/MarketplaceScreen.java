package com.ammora.mod.client.gui;

import com.ammora.mod.network.MarketplaceDataPayload;
import com.ammora.mod.util.AmmoraLang;
import com.ammora.mod.network.ServerboundBuyRequestPayload;
import com.ammora.mod.network.ServerboundClaimDeliveryPayload;
import com.ammora.mod.network.ServerboundCommunityQuestPayload;
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
 * 6 Tabs: Global Market (Drone Delivery), Player Shops Directory (Rust-style), Buy Requests (RFQ), Quests & Bounties, Delivery Buffer (10 slots), and History Ledger.
 * Features full JEI-like item browser for RFQ, shop filtering, anti-overlap layout, quest board, and high-tech styling.
 */
public class MarketplaceScreen extends Screen {

    private MarketplaceDataPayload data;
    private int activeTab = 0; // 0: Catalog, 1: Shops, 2: Buy Requests, 3: Quests, 4: Delivery Buffer, 5: History
    private int catalogPage = 0;
    private int shopsPage = 0;
    private int reqPage = 0;
    private int questsPage = 0;
    private int deliveryPage = 0;
    private int txPage = 0;

    // Filter by specific shop in Catalog tab
    private String filterShopId = null;
    private String filterShopName = null;

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
        super(Component.literal("Marketplace"));
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

        int mw = 400, mh = 240;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

        // Navigation Tabs (6 tabs)
        int tabW = 62;
        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 0 ? AmmoraLang.guiStr("market.tab_market_active") : AmmoraLang.guiStr("market.tab_market_inactive")),
                b -> { activeTab = 0; catalogPage = 0; rebuildWidgets(); }
        ).bounds(mx + 8, my + 26, tabW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 1 ? AmmoraLang.guiStr("market.tab_shops_active") : AmmoraLang.guiStr("market.tab_shops_inactive")),
                b -> { activeTab = 1; shopsPage = 0; rebuildWidgets(); }
        ).bounds(mx + 72, my + 26, tabW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 2 ? AmmoraLang.guiStr("market.tab_rfq_active") : AmmoraLang.guiStr("market.tab_rfq_inactive")),
                b -> { activeTab = 2; reqPage = 0; rebuildWidgets(); }
        ).bounds(mx + 136, my + 26, tabW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 3 ? AmmoraLang.guiStr("market.tab_quests_active") : AmmoraLang.guiStr("market.tab_quests_inactive")),
                b -> { activeTab = 3; questsPage = 0; rebuildWidgets(); }
        ).bounds(mx + 200, my + 26, tabW, 18).build());

        int delCount = (data != null && data.deliveries() != null) ? data.deliveries().size() : 0;
        String bufferTabTitle;
        if (delCount > 0) {
            bufferTabTitle = activeTab == 4 ? AmmoraLang.guiStr("market.tab_buffer_count_active", delCount) : AmmoraLang.guiStr("market.tab_buffer_count_inactive", delCount);
        } else {
            bufferTabTitle = activeTab == 4 ? AmmoraLang.guiStr("market.tab_buffer_active") : AmmoraLang.guiStr("market.tab_buffer_inactive");
        }
        this.addRenderableWidget(Button.builder(
                Component.literal(bufferTabTitle),
                b -> { activeTab = 4; deliveryPage = 0; rebuildWidgets(); }
        ).bounds(mx + 264, my + 26, 64, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(activeTab == 5 ? AmmoraLang.guiStr("market.tab_history_active") : AmmoraLang.guiStr("market.tab_history_inactive")),
                b -> { activeTab = 5; txPage = 0; rebuildWidgets(); }
        ).bounds(mx + 330, my + 26, tabW, 18).build());

        if (activeTab == 0) {
            // TAB 0: Global Market Catalog
            int searchW = (filterShopId != null && !filterShopId.isEmpty()) ? 115 : 140;
            searchBox = new EditBox(this.font, mx + 12, my + 48, searchW, 16, Component.literal(AmmoraLang.guiStr("market.search")));
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
                }).bounds(mx + 132, my + 48, 76, 16).build());
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
                }).bounds(mx + mw - 60, my + 48, 22, 16).build());
            }

            if (shopsPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    shopsPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 48, 22, 16).build());
            }

            int startIndex = shopsPage * 5;
            for (int i = 0; i < 5; i++) {
                int sIdx = startIndex + i;
                if (sIdx < shops.size()) {
                    var shop = shops.get(sIdx);
                    int rowY = my + 68 + i * 32;

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
            // TAB 2: Buy Requests (RFQ)
            var reqs = data.buyRequests();
            int totalPages = Math.max(1, (reqs.size() + 3) / 4);

            if (reqPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    reqPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 48, 22, 16).build());
            }

            if (reqPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    reqPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 48, 22, 16).build());
            }

            int startIndex = reqPage * 4;
            for (int i = 0; i < 4; i++) {
                int rIdx = startIndex + i;
                if (rIdx < reqs.size()) {
                    var req = reqs.get(rIdx);
                    int rowY = my + 68 + i * 33;

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

        } else if (activeTab == 3) {
            // TAB 3: Community Quests & Bounties
            var quests = data.quests();
            int totalPages = Math.max(1, (quests.size() + 3) / 4);

            // [+ Create Quest] button
            this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_new_quest")), b -> {
                showCreateQuestModal = true;
                rebuildWidgets();
            }).bounds(mx + 12, my + 48, 85, 16).build());

            if (questsPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    questsPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 48, 22, 16).build());
            }

            if (questsPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    questsPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 48, 22, 16).build());
            }

            int startIndex = questsPage * 4;
            for (int i = 0; i < 4; i++) {
                int qIdx = startIndex + i;
                if (qIdx < quests.size()) {
                    var q = quests.get(qIdx);
                    int rowY = my + 70 + i * 38;

                    this.addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_open")), b -> {
                        selectedQuest = q;
                        rebuildWidgets();
                    }).bounds(mx + mw - 76, rowY + 8, 64, 18).build());
                }
            }
        } else if (activeTab == 4) {
            // TAB 4: Delivery Buffer (10-slot persistent storage)
            var deliveries = (data != null && data.deliveries() != null) ? data.deliveries() : List.<MarketplaceDataPayload.DeliveryBufferItem>of();
            int totalPages = Math.max(1, (deliveries.size() + 9) / 10);

            if (deliveryPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    deliveryPage--;
                    rebuildWidgets();
                }).bounds(mx + 194, my + 48, 20, 16).build());
            }

            if (deliveryPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    deliveryPage++;
                    rebuildWidgets();
                }).bounds(mx + 216, my + 48, 20, 16).build());
            }

            // Big Action Button "Claim All"
            var claimAllBtn = Button.builder(Component.literal(AmmoraLang.guiStr("market.btn_claim_all_colored")), b -> {
                PacketDistributor.sendToServer(new ServerboundClaimDeliveryPayload("", true));
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }).bounds(mx + mw - 150, my + 48, 140, 16).build();
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
                    int cardY = my + 68 + row * 32;
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
        } else if (activeTab == 5) {
            // TAB 5: History Ledger
            var txs = data.transactions();
            int totalPages = Math.max(1, (txs.size() + 4) / 5);

            if (txPage > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    txPage--;
                    rebuildWidgets();
                }).bounds(mx + mw - 60, my + 48, 22, 16).build());
            }

            if (txPage < totalPages - 1) {
                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    txPage++;
                    rebuildWidgets();
                }).bounds(mx + mw - 34, my + 48, 22, 16).build());
            }
        }
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

        int mw = 400, mh = 240;
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
            }).bounds(mx + mw - 60, my + 48, 22, 16).build();
            dynamicCatalogButtons.add(prevBtn);
            this.addRenderableWidget(prevBtn);
        }

        if (catalogPage < totalPages - 1) {
            var nextBtn = Button.builder(Component.literal("▶"), b -> {
                catalogPage++;
                updateDynamicCatalogWidgets();
            }).bounds(mx + mw - 34, my + 48, 22, 16).build();
            dynamicCatalogButtons.add(nextBtn);
            this.addRenderableWidget(nextBtn);
        }

        int startIndex = catalogPage * 5;
        for (int i = 0; i < 5; i++) {
            int itemIdx = startIndex + i;
            if (itemIdx < filtered.size()) {
                var item = filtered.get(itemIdx);
                int rowY = my + 68 + i * 32;
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
                shopId, slotIndex, count, true
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

        int mw = 400, mh = 240;
        int mx = (this.width - mw) / 2;
        int my = (this.height - mh) / 2;

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
        gg.fill(mx + 1, my + 1, mx + mw - 1, my + 24, COLOR_PANEL_HEADER);
        gg.hLine(mx + 1, mx + mw - 1, my + 24, COLOR_BORDER_MUTED);

        gg.drawString(this.font, "§b✦ " + AmmoraLang.guiStr("market.title") + " ✦", mx + 10, my + 8, 0xFFFFFFFF);
        String balStr = AmmoraLang.guiStr("market.header_balance_rank", String.format(Locale.US, "%.2f", data.balanceCbx()), data.repLevel());
        gg.drawString(this.font, balStr, mx + mw - this.font.width(balStr) - 10, my + 8, 0xFFFFFFFF);

        MarketplaceDataPayload.MarketplaceSlotItem hoveredCatalogItem = null;
        MarketplaceDataPayload.BuyRequestItem hoveredRfq = null;

        if (activeTab == 0) {
            hoveredCatalogItem = renderCatalogTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 1) {
            renderShopsTab(gg, mx, my, mw, mh);
        } else if (activeTab == 2) {
            hoveredRfq = renderBuyRequestsTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 3) {
            renderQuestsTab(gg, mx, my, mw, mh);
        } else if (activeTab == 4) {
            renderDeliveryBufferTab(gg, mx, my, mw, mh, mouseX, mouseY);
        } else if (activeTab == 5) {
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
        if (!showItemPickerModal && !showCreateQuestModal && selectedQuest == null && hoveredCatalogItem != null) {
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

        // RFQ Card Tooltip
        if (!showItemPickerModal && !showCreateQuestModal && selectedQuest == null && hoveredRfq != null) {
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
        if (!showItemPickerModal && !showCreateQuestModal && selectedQuest == null && activeTab == 2) {
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
            gg.drawString(this.font, shopFilterTitle, mx + 212, my + 52, 0xFFFFFFFF);
        } else {
            gg.drawString(this.font, AmmoraLang.guiStr("market.catalog_pages", (catalogPage + 1), totalPages, filtered.size()), mx + 160, my + 52, COLOR_TEXT_MUTED);
        }

        int startIndex = catalogPage * 5;
        MarketplaceDataPayload.MarketplaceSlotItem hovered = null;

        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 68 + i * 32;

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
        gg.drawString(this.font, AmmoraLang.guiStr("market.shops_online_header", shops.size(), (shopsPage + 1), totalPages), mx + 12, my + 52, 0xFFFFFFFF);

        int startIndex = shopsPage * 5;
        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 68 + i * 32;

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
        gg.drawString(this.font, AmmoraLang.guiStr("market.rfq_header", reqs.size(), (reqPage + 1), totalPages), mx + 12, my + 52, 0xFFFFFFFF);

        int startIndex = reqPage * 4;
        MarketplaceDataPayload.BuyRequestItem hovered = null;

        for (int i = 0; i < 4; i++) {
            int idx = startIndex + i;
            int rowY = my + 68 + i * 33;

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
        gg.drawString(this.font, AmmoraLang.guiStr("market.quests_header", quests.size(), (questsPage + 1), totalPages), mx + 105, my + 52, 0xFFFFFFFF);

        int startIndex = questsPage * 4;
        for (int i = 0; i < 4; i++) {
            int idx = startIndex + i;
            int rowY = my + 70 + i * 38;

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

        gg.drawString(this.font, AmmoraLang.guiStr("market.buffer_header", deliveries.size(), (deliveryPage + 1), totalPages), mx + 12, my + 52, 0xFFFFFFFF);

        int startIndex = deliveryPage * 10;
        MarketplaceDataPayload.DeliveryBufferItem hoveredItem = null;

        for (int i = 0; i < 10; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = (col == 0) ? (mx + 10) : (mx + 204);
            int cardY = my + 68 + row * 32;
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
        gg.drawString(this.font, AmmoraLang.guiStr("market.ledger_header", txs.size(), (txPage + 1), totalPages), mx + 12, my + 52, 0xFFFFFFFF);

        int startIndex = txPage * 5;
        for (int i = 0; i < 5; i++) {
            int idx = startIndex + i;
            int rowY = my + 68 + i * 32;

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
        if (activeTab == 2) {
            int mw = 400, mh = 240;
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
                    || (bountyCountBox != null && bountyCountBox.isFocused());
            if (!textFocused) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
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
