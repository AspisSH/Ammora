package com.ammora.mod.client.gui;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.network.ClientboundAtmDataPayload;
import com.ammora.mod.network.ServerboundAtmActionPayload;
import com.ammora.mod.util.AmmoraLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Industrial Cyber-ATM Screen for physical CBX banknote withdrawal and cash deposit.
 * Styled in polished deepslate and Create industrial amber tones.
 */
public class AtmScreen extends Screen {

    private ClientboundAtmDataPayload data;

    // Tabs: 0 = Withdraw, 1 = Deposit
    private int activeTab = 0;

    // Account mode (Personal vs Corporate)
    private static boolean useCompanyAccount = false;

    // Amounts
    private double withdrawAmount = 100.0;
    private double depositAmount = 100.0;

    // UI Widgets
    private EditBox amountBox;
    private Button actionBtn;
    private Button depositAllBtn;

    // Notification banner
    private String statusNotification = "";
    private boolean statusNotificationError = false;
    private long notificationExpireTime = 0;

    // Styling Palette tokens (Polished Deepslate & Create Amber)
    private static final int COLOR_BG = 0xFF14161E;
    private static final int COLOR_PANEL_HEADER = 0xFF1D2029;
    private static final int COLOR_CARD_BG = 0xFF181C26;
    private static final int COLOR_BORDER_AMBER = 0xFFFF9800;
    private static final int COLOR_BORDER_MUTED = 0xFF353642;
    private static final int COLOR_GREEN = 0xFF00E676;
    private static final int COLOR_RED = 0xFFFF5252;
    private static final int COLOR_AMBER = 0xFFFFB300;
    private static final int COLOR_TEXT_MUTED = 0xFF9E9284;
    private static final int COLOR_CYAN = 0xFF38BDF8;

    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 224;

    public AtmScreen(ClientboundAtmDataPayload initialData) {
        super(AmmoraLang.gui("atm.title"));
        this.data = initialData;
        checkNewNotification(initialData);
    }

    public void updateData(ClientboundAtmDataPayload newData) {
        this.data = newData;
        checkNewNotification(newData);
        rebuildWidgets();
    }

    private void checkNewNotification(ClientboundAtmDataPayload p) {
        if (p.statusMessage() != null && !p.statusMessage().isEmpty()) {
            this.statusNotification = AmmoraLang.translateNotification(p.statusMessage());
            this.statusNotificationError = p.isError();
            this.notificationExpireTime = System.currentTimeMillis() + 4500L;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Overridden to no-op so Minecraft's default Screen#render doesn't apply blur shader over custom GUI
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - GUI_WIDTH) / 2;
        int top = (height - GUI_HEIGHT) / 2;

        // 1. Close Button '✕'
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(left + GUI_WIDTH - 24, top + 6, 18, 16)
                .build());

        // 2. Account Switcher Button
        String toggleText;
        if (data.hasCompany()) {
            String compShort = data.companyName();
            if (compShort.length() > 12) compShort = compShort.substring(0, 10) + "..";
            toggleText = useCompanyAccount ? "§6🏢 " + compShort : "§b👤 " + AmmoraLang.guiStr("account.personal");
        } else {
            toggleText = "§b👤 " + AmmoraLang.guiStr("account.personal");
        }

        Button acctBtn = Button.builder(Component.literal(toggleText), b -> {
            if (data.hasCompany()) {
                useCompanyAccount = !useCompanyAccount;
                rebuildWidgets();
            }
        })
                .bounds(left + 16, top + 26, 110, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        data.hasCompany()
                                ? (useCompanyAccount ? AmmoraLang.guiStr("account.switch_to_personal") : AmmoraLang.guiStr("account.switch_to_company"))
                                : AmmoraLang.guiStr("account.personal")
                )))
                .build();
        if (!data.hasCompany()) {
            acctBtn.active = false;
        }
        addRenderableWidget(acctBtn);

        // 3. Tabs: [WITHDRAW] & [DEPOSIT]
        int tabW = 140;
        addRenderableWidget(Button.builder(
                Component.literal((activeTab == 0 ? "§6▶ " : "") + AmmoraLang.guiStr("atm.tab_withdraw")),
                b -> {
                    snapAmountToMultipleOfTen();
                    activeTab = 0;
                    rebuildWidgets();
                })
                .bounds(left + 16, top + 52, tabW, 18)
                .build());

        addRenderableWidget(Button.builder(
                Component.literal((activeTab == 1 ? "§6▶ " : "") + AmmoraLang.guiStr("atm.tab_deposit")),
                b -> {
                    snapAmountToMultipleOfTen();
                    activeTab = 1;
                    rebuildWidgets();
                })
                .bounds(left + 16 + tabW + 8, top + 52, tabW, 18)
                .build());

        // 4. Tab specific content
        int contentY = top + 74;
        if (activeTab == 0) {
            initWithdrawTab(left, contentY);
        } else {
            initDepositTab(left, contentY);
        }
    }

    private void initWithdrawTab(int left, int contentY) {
        // Quick preset buttons: 2 rows of 3 buttons in the left column (left + 16 to left + 156)
        // Row 1 presets at contentY + 16 (height 16): +10, +50, +100
        int[] row1 = {10, 50, 100};
        int px1 = left + 16;
        int pw = 44;
        for (int p : row1) {
            final int addVal = p;
            addRenderableWidget(Button.builder(Component.literal("+" + addVal), b -> {
                withdrawAmount += addVal;
                if (amountBox != null) amountBox.setValue(String.format(Locale.US, "%.0f", withdrawAmount));
            }).bounds(px1, contentY + 16, pw, 16).build());
            px1 += pw + 4;
        }

        // Row 2 presets at contentY + 36 (height 16): +500, +1000, MAX
        int[] row2 = {500, 1000};
        int px2 = left + 16;
        for (int p : row2) {
            final int addVal = p;
            addRenderableWidget(Button.builder(Component.literal("+" + addVal), b -> {
                withdrawAmount += addVal;
                if (amountBox != null) amountBox.setValue(String.format(Locale.US, "%.0f", withdrawAmount));
            }).bounds(px2, contentY + 36, pw, 16).build());
            px2 += pw + 4;
        }

        // "Max" button
        addRenderableWidget(Button.builder(Component.literal(AmmoraLang.guiStr("atm.btn_max")), b -> {
            double currentBal = getCurrentBalance();
            withdrawAmount = Math.max(0, Math.floor(currentBal / 10.0) * 10.0);
            if (amountBox != null) amountBox.setValue(String.format(Locale.US, "%.0f", withdrawAmount));
        }).bounds(px2, contentY + 36, pw, 16).build());

        // Amount input box at contentY + 70 (height 18)
        amountBox = new EditBox(font, left + 16, contentY + 70, 114, 18, Component.literal(""));
        amountBox.setValue(String.format(Locale.US, "%.0f", withdrawAmount));
        amountBox.setResponder(val -> {
            try {
                withdrawAmount = Math.max(0, Double.parseDouble(val.replaceAll("[^0-9]", "")));
            } catch (Exception ignored) {}
        });
        addRenderableWidget(amountBox);

        // Clear button [C]
        addRenderableWidget(Button.builder(Component.literal("C"), b -> {
            withdrawAmount = 0.0;
            if (amountBox != null) amountBox.setValue("0");
        }).bounds(left + 134, contentY + 70, 22, 18).build());

        // Primary Withdraw Button at contentY + 104 (height 22)
        actionBtn = Button.builder(Component.literal("§6⬇ " + AmmoraLang.guiStr("atm.btn_withdraw")), b -> executeWithdraw())
                .bounds(left + 16, contentY + 104, GUI_WIDTH - 32, 22)
                .build();
        addRenderableWidget(actionBtn);
    }

    private void initDepositTab(int left, int contentY) {
        double totalCash = getPlayerInventoryCash();

        // 1. Big DEPOSIT ALL CASH button at contentY + 4 (height 22 -> ends at 26)
        String depositAllText = "§a⬆ " + AmmoraLang.guiStr("atm.btn_deposit_all") + " (" + String.format(Locale.US, "%,.0f", totalCash) + " CBX)";
        depositAllBtn = Button.builder(Component.literal(depositAllText), b -> executeDepositAll())
                .bounds(left + 16, contentY + 4, GUI_WIDTH - 32, 22)
                .build();
        depositAllBtn.active = totalCash > 0;
        addRenderableWidget(depositAllBtn);

        // 2. Presets for depositing specific amount at contentY + 44 (height 18 -> ends at 62)
        int[] presets = {10, 100, 1000};
        int px = left + 16;
        int pw = 52;
        for (int p : presets) {
            final int addVal = p;
            addRenderableWidget(Button.builder(Component.literal("+" + addVal), b -> {
                depositAmount += addVal;
                if (amountBox != null) amountBox.setValue(String.format(Locale.US, "%.0f", depositAmount));
            }).bounds(px, contentY + 44, pw, 18).build());
            px += pw + 6;
        }

        // Amount input box for partial deposit at contentY + 80 (height 18 -> ends at 98)
        amountBox = new EditBox(font, left + 16, contentY + 80, 114, 18, Component.literal(""));
        amountBox.setValue(String.format(Locale.US, "%.0f", depositAmount));
        amountBox.setResponder(val -> {
            try {
                depositAmount = Math.max(0, Double.parseDouble(val.replaceAll("[^0-9]", "")));
            } catch (Exception ignored) {}
        });
        addRenderableWidget(amountBox);

        // Clear button
        addRenderableWidget(Button.builder(Component.literal("C"), b -> {
            depositAmount = 0.0;
            if (amountBox != null) amountBox.setValue("0");
        }).bounds(left + 134, contentY + 80, 22, 18).build());

        // Primary Deposit Amount Button at contentY + 104 (height 22)
        actionBtn = Button.builder(Component.literal("§a⬆ " + AmmoraLang.guiStr("atm.btn_deposit_amount")), b -> executeDepositAmount())
                .bounds(left + 16, contentY + 104, GUI_WIDTH - 32, 22)
                .build();
        addRenderableWidget(actionBtn);
    }

    private void snapAmountToMultipleOfTen() {
        if (activeTab == 0) {
            double current = withdrawAmount;
            if (amountBox != null) {
                try {
                    String raw = amountBox.getValue().replaceAll("[^0-9]", "");
                    if (!raw.isEmpty()) {
                        current = Double.parseDouble(raw);
                    }
                } catch (Exception ignored) {}
            }
            double rounded = current <= 0 ? 0.0 : Math.max(10.0, Math.round(current / 10.0) * 10.0);
            withdrawAmount = rounded;
            if (amountBox != null) {
                amountBox.setValue(String.format(Locale.US, "%.0f", withdrawAmount));
            }
        } else {
            double current = depositAmount;
            if (amountBox != null) {
                try {
                    String raw = amountBox.getValue().replaceAll("[^0-9]", "");
                    if (!raw.isEmpty()) {
                        current = Double.parseDouble(raw);
                    }
                } catch (Exception ignored) {}
            }
            double rounded = current <= 0 ? 0.0 : Math.max(10.0, Math.round(current / 10.0) * 10.0);
            depositAmount = rounded;
            if (amountBox != null) {
                amountBox.setValue(String.format(Locale.US, "%.0f", depositAmount));
            }
        }
    }

    private void executeWithdraw() {
        snapAmountToMultipleOfTen();
        if (withdrawAmount <= 0) return;
        PacketDistributor.sendToServer(new ServerboundAtmActionPayload(
                ServerboundAtmActionPayload.ACTION_WITHDRAW,
                withdrawAmount,
                useCompanyAccount
        ));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void executeDepositAll() {
        if (depositAllBtn != null) {
            depositAllBtn.active = false;
        }
        PacketDistributor.sendToServer(new ServerboundAtmActionPayload(
                ServerboundAtmActionPayload.ACTION_DEPOSIT_ALL,
                0.0,
                useCompanyAccount
        ));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void executeDepositAmount() {
        snapAmountToMultipleOfTen();
        if (depositAmount <= 0) return;
        PacketDistributor.sendToServer(new ServerboundAtmActionPayload(
                ServerboundAtmActionPayload.ACTION_DEPOSIT_AMOUNT,
                depositAmount,
                useCompanyAccount
        ));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private double getCurrentBalance() {
        if (useCompanyAccount && data.hasCompany()) {
            if (!data.isCompanyOwner()) {
                double remaining = Math.max(0.0, data.memberDailyLimit() - data.memberSpentToday());
                return Math.min(remaining, data.companyBalance());
            }
            return data.companyBalance();
        }
        return data.personalBalance();
    }

    private double getPlayerInventoryCash() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0.0;

        int c1000 = countItem(player, AmmoraMod.BANKNOTE_1000.get().asItem());
        int c100 = countItem(player, AmmoraMod.BANKNOTE_100.get().asItem());
        int c10 = countItem(player, AmmoraMod.BANKNOTE_10.get().asItem());

        int s1000 = countItem(player, AmmoraMod.MONEY_STACK_1000.get().asItem());
        int s100 = countItem(player, AmmoraMod.MONEY_STACK_100.get().asItem());
        int s10 = countItem(player, AmmoraMod.MONEY_STACK_10.get().asItem());

        int b1000 = countItem(player, AmmoraMod.MONEY_BLOCK_1000_ITEM.get().asItem());
        int b100 = countItem(player, AmmoraMod.MONEY_BLOCK_100_ITEM.get().asItem());
        int b10 = countItem(player, AmmoraMod.MONEY_BLOCK_10_ITEM.get().asItem());

        return c1000 * 1000.0 + c100 * 100.0 + c10 * 10.0
                + s1000 * 9000.0 + s100 * 900.0 + s10 * 90.0
                + b1000 * 81000.0 + b100 * 8100.0 + b10 * 810.0;
    }

    private int countItem(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) {
                count += s.getCount();
            }
        }
        return count;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        int left = (width - GUI_WIDTH) / 2;
        int top = (height - GUI_HEIGHT) / 2;

        // 1. Solid opaque chassis frame (no world blur or alpha bleeding)
        g.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, COLOR_BG);
        g.renderOutline(left, top, GUI_WIDTH, GUI_HEIGHT, COLOR_BORDER_AMBER);

        // 2. Header panel
        g.fill(left + 2, top + 2, left + GUI_WIDTH - 2, top + 22, COLOR_PANEL_HEADER);
        g.renderOutline(left + 2, top + 2, GUI_WIDTH - 4, 20, COLOR_BORDER_MUTED);

        // Header Title
        g.drawString(font, "§6🏧 §l" + AmmoraLang.guiStr("atm.title"), left + 10, top + 8, COLOR_AMBER, true);

        // 3. Balance & Cash info cards (stacked cleanly on separate lines with drop shadow)
        String balLine;
        if (useCompanyAccount && data.hasCompany() && !data.isCompanyOwner()) {
            String limitStr = String.format(Locale.US, "%.0f/%.0f CBX", data.memberSpentToday(), data.memberDailyLimit());
            balLine = AmmoraLang.guiStr("atm.balance_limit", limitStr);
        } else {
            double currentBal = getCurrentBalance();
            String balStr = String.format(Locale.US, "%,.2f", currentBal);
            balLine = AmmoraLang.guiStr("atm.balance", balStr);
        }
        g.drawString(font, "§f" + balLine, left + 132, top + 26, COLOR_GREEN, true);

        double cashInInv = getPlayerInventoryCash();
        String cashStr = String.format(Locale.US, "%,.0f", cashInInv);
        String cashLine = AmmoraLang.guiStr("atm.cash_in_inventory", cashStr);
        g.drawString(font, "§7" + cashLine, left + 132, top + 38, COLOR_CYAN, true);

        // Render widgets (buttons, editboxes)
        super.render(g, mouseX, mouseY, partialTicks);

        int contentY = top + 74;

        if (activeTab == 0) {
            // Withdraw section:
            // "Быстрый выбор:" at contentY + 4 (above Row 1 at contentY + 16)
            g.drawString(font, "§6§l▸ " + AmmoraLang.guiStr("atm.quick_presets"), left + 16, contentY + 4, COLOR_AMBER, true);

            // "Сумма (CBX):" at contentY + 58 (above amountBox at contentY + 70)
            g.drawString(font, "§6§l▸ " + AmmoraLang.guiStr("atm.amount_label"), left + 16, contentY + 58, COLOR_AMBER, true);

            // Denomination breakdown preview card on right side (width 136, height 84)
            int cardX = left + 168;
            int cardY = contentY + 4;
            int cardW = GUI_WIDTH - 184; // 136
            int cardH = 84;
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_CARD_BG);
            g.renderOutline(cardX, cardY, cardW, cardH, COLOR_BORDER_MUTED);

            g.drawString(font, "§e§l" + AmmoraLang.guiStr("atm.breakdown_title"), cardX + 8, cardY + 6, COLOR_AMBER, true);
            int rem = (int) withdrawAmount;
            int n1000 = rem / 1000; rem %= 1000;
            int n100 = rem / 100; rem %= 100;
            int n10 = rem / 10;

            g.drawString(font, AmmoraLang.guiStr("atm.breakdown_1000", n1000), cardX + 10, cardY + 22, n1000 > 0 ? COLOR_GREEN : COLOR_TEXT_MUTED, true);
            g.drawString(font, AmmoraLang.guiStr("atm.breakdown_100", n100), cardX + 10, cardY + 40, n100 > 0 ? COLOR_GREEN : COLOR_TEXT_MUTED, true);
            g.drawString(font, AmmoraLang.guiStr("atm.breakdown_10", n10), cardX + 10, cardY + 58, n10 > 0 ? COLOR_GREEN : COLOR_TEXT_MUTED, true);
        } else {
            // Deposit section:
            // Dynamic update of depositAllBtn text & active state based on live inventory cash
            if (depositAllBtn != null) {
                double currentCash = getPlayerInventoryCash();
                depositAllBtn.setMessage(Component.literal("§a⬆ " + AmmoraLang.guiStr("atm.btn_deposit_all") + " (" + String.format(Locale.US, "%,.0f", currentCash) + " CBX)"));
                depositAllBtn.active = currentCash > 0;
            }

            // "Быстрый выбор:" at contentY + 32 (above presets at 44)
            g.drawString(font, "§6§l▸ " + AmmoraLang.guiStr("atm.quick_presets"), left + 16, contentY + 32, COLOR_AMBER, true);

            // "Сумма (CBX):" at contentY + 68 (above amountBox at 80)
            g.drawString(font, "§6§l▸ " + AmmoraLang.guiStr("atm.amount_label"), left + 16, contentY + 68, COLOR_AMBER, true);
        }

        // Status Notification Banner placed cleanly outside the GUI chassis (below it)
        if (!statusNotification.isEmpty() && System.currentTimeMillis() < notificationExpireTime) {
            int notifW = GUI_WIDTH - 24;
            int notifH = 18;
            int notifX = left + 12;
            int notifY = top + GUI_HEIGHT + 6;

            int bgCol = statusNotificationError ? 0xFF3D1414 : 0xFF0E381A;
            int textCol = statusNotificationError ? COLOR_RED : COLOR_GREEN;

            g.fill(notifX, notifY, notifX + notifW, notifY + notifH, bgCol);
            g.renderOutline(notifX, notifY, notifW, notifH, textCol);

            String icon = statusNotificationError ? "⚠ " : "✔ ";
            g.drawCenteredString(font, icon + statusNotification, notifX + notifW / 2, notifY + 5, textCol);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.amountBox != null && this.amountBox.isFocused()) {
            if (!this.amountBox.isMouseOver(mouseX, mouseY)) {
                snapAmountToMultipleOfTen();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.amountBox != null && this.amountBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter key
                snapAmountToMultipleOfTen();
                if (activeTab == 0) executeWithdraw();
                else executeDepositAmount();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // Close on 'E' or Esc
        if (keyCode == 256 || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
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
