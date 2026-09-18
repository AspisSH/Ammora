package com.ammora.mod;

import java.util.UUID;
import com.ammora.mod.blocks.ExchangeTerminalBlock;
import com.ammora.mod.blocks.ExchangeTerminalEntity;
import com.ammora.mod.blocks.PurchaseDockBlock;
import com.ammora.mod.blocks.PurchaseDockEntity;
import com.ammora.mod.blocks.PurchaseDockItem;
import com.ammora.mod.blocks.TradeDockBlock;
import com.ammora.mod.blocks.TradeDockEntity;
import com.ammora.mod.blocks.TradeDockItem;
import com.ammora.mod.compat.cctweaked.CCCompat;
import com.ammora.mod.compat.create.CreateCompat;
import com.ammora.mod.config.AmmoraConfig;
import com.ammora.mod.core.MarketManager;
import com.ammora.mod.core.OMSManager;
import com.ammora.mod.core.events.MarketEventManager;
import com.ammora.mod.datapack.CommodityReloadListener;
import com.ammora.mod.datapack.MarketEventReloadListener;
import com.ammora.mod.datapack.ContractReloadListener;
import com.ammora.mod.db.DatabaseManager;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;
import com.ammora.mod.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.io.File;

/**
 * Main entry point for the Ammora mod on NeoForge 1.21.1.
 * <p>
 * Ammora implements an Automated Market Maker (AMM) financial ecosystem for Minecraft,
 * featuring dynamic bonding curve pricing, non-fungible resource reserves, candlestick chart analysis,
 * offline delivery buffer, player vending machines with upgrade modules, and deep integrations with
 * Create mod (kinetics & display links) and CC:Tweaked (peripherals & Lua API).
 * </p>
 */
@Mod(AmmoraMod.MODID)
public class AmmoraMod {

    public static final String MODID = "ammora";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Blocks & Items
    public static final DeferredBlock<ExchangeTerminalBlock> EXCHANGE_TERMINAL = BLOCKS.register(
            "exchange_terminal",
            () -> new ExchangeTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredItem<BlockItem> EXCHANGE_TERMINAL_ITEM = ITEMS.register(
            "exchange_terminal",
            () -> new com.ammora.mod.blocks.ExchangeTerminalItem(EXCHANGE_TERMINAL.get(), new Item.Properties())
    );

    public static final DeferredBlock<TradeDockBlock> TRADE_DOCK = BLOCKS.register(
            "trade_dock",
            () -> new TradeDockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredItem<TradeDockItem> TRADE_DOCK_ITEM = ITEMS.register(
            "trade_dock",
            () -> new com.ammora.mod.blocks.TradeDockItem(TRADE_DOCK.get(), new Item.Properties())
    );

    public static final DeferredBlock<PurchaseDockBlock> PURCHASE_DOCK = BLOCKS.register(
            "purchase_dock",
            () -> new PurchaseDockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredItem<PurchaseDockItem> PURCHASE_DOCK_ITEM = ITEMS.register(
            "purchase_dock",
            () -> new com.ammora.mod.blocks.PurchaseDockItem(PURCHASE_DOCK.get(), new Item.Properties())
    );

    public static final DeferredBlock<com.ammora.mod.blocks.PlayerShopBlock> PLAYER_SHOP = BLOCKS.register(
            "player_shop",
            () -> new com.ammora.mod.blocks.PlayerShopBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(4.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredItem<com.ammora.mod.blocks.PlayerShopItem> PLAYER_SHOP_ITEM = ITEMS.register(
            "player_shop",
            () -> new com.ammora.mod.blocks.PlayerShopItem(PLAYER_SHOP.get(), new Item.Properties())
    );

    public static final DeferredItem<com.ammora.mod.items.ColdWalletItem> COLD_WALLET = ITEMS.register(
            "cold_wallet",
            () -> new com.ammora.mod.items.ColdWalletItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<com.ammora.mod.items.MarketTabletItem> MARKET_TABLET = ITEMS.register(
            "market_tablet",
            () -> new com.ammora.mod.items.MarketTabletItem(new Item.Properties().stacksTo(1))
    );

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExchangeTerminalEntity>> EXCHANGE_TERMINAL_BE =
            BLOCK_ENTITIES.register("exchange_terminal", () ->
                    BlockEntityType.Builder.of(ExchangeTerminalEntity::new, EXCHANGE_TERMINAL.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TradeDockEntity>> TRADE_DOCK_BE =
            BLOCK_ENTITIES.register("trade_dock", () ->
                    BlockEntityType.Builder.of(TradeDockEntity::new, TRADE_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurchaseDockEntity>> PURCHASE_DOCK_BE =
            BLOCK_ENTITIES.register("purchase_dock", () ->
                    BlockEntityType.Builder.of(PurchaseDockEntity::new, PURCHASE_DOCK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.ammora.mod.blocks.PlayerShopEntity>> PLAYER_SHOP_BE =
            BLOCK_ENTITIES.register("player_shop", () ->
                    BlockEntityType.Builder.of(com.ammora.mod.blocks.PlayerShopEntity::new, PLAYER_SHOP.get()).build(null)
            );

    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXCHANGE_TAB = CREATIVE_MODE_TABS.register(
            "ammora_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ammora"))
                    .icon(() -> EXCHANGE_TERMINAL_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(EXCHANGE_TERMINAL_ITEM.get());
                        output.accept(TRADE_DOCK_ITEM.get());
                        output.accept(PURCHASE_DOCK_ITEM.get());
                        output.accept(PLAYER_SHOP_ITEM.get());
                        output.accept(COLD_WALLET.get());
                        output.accept(MARKET_TABLET.get());
                    })
                    .build()
    );


    // Datapack Reload Listeners
    private static final CommodityReloadListener commodityListener = new CommodityReloadListener();
    private static final MarketEventReloadListener marketEventListener = new MarketEventReloadListener();
    private static final ContractReloadListener contractListener = new ContractReloadListener();

    // Market Engine Singletons
    private static DatabaseManager dbManager;
    private static MarketDAO marketDAO;
    private static OMSManager omsManager;
    private static MarketManager marketManager;
    private static final MarketEventManager marketEventManager = new MarketEventManager();
    private static final java.util.Random RANDOM = new java.util.Random();

    private static long lastTrackedDay = -1L;

    public AmmoraMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, AmmoraConfig.SPEC);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(PacketHandler::register);
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Ammora mod initialized with config.");
    }

    private void onRegister(net.neoforged.neoforge.registries.RegisterEvent event) {
        if (ModList.get().isLoaded("create")) {
            try {
                CreateCompat.registerDisplaySources(event);
            } catch (Throwable t) {
                LOGGER.warn("Failed to register Create display source in RegisterEvent", t);
            }
        }
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("create")) {
                LOGGER.info("Create mod detected! Registering Create Display Link integration in commonSetup...");
                CreateCompat.init();
            }
            if (ModList.get().isLoaded("computercraft")) {
                LOGGER.info("CC: Tweaked detected! Registering ComputerCraft peripheral integration in commonSetup...");
                CCCompat.init();
            }
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TRADE_DOCK_BE.get(),
                (dock, side) -> dock.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                PURCHASE_DOCK_BE.get(),
                (dock, side) -> dock.getInventory()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                PLAYER_SHOP_BE.get(),
                (shop, side) -> shop.getInventory()
        );
        if (ModList.get().isLoaded("computercraft")) {
            com.ammora.mod.compat.cctweaked.CCCompat.registerCapabilities(event);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        com.ammora.mod.command.AmmoraCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        lastTrackedDay = -1L;
        MinecraftServer server = event.getServer();
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File dbFile = new File(worldDir, "data" + File.separator + "ammora.db");
        File oldDbFile = new File(worldDir, "data" + File.separator + "exchange.db");
        if (!dbFile.exists() && oldDbFile.exists()) {
            oldDbFile.renameTo(dbFile);
        }

        LOGGER.info("Connecting to exchange database at: {}", dbFile.getAbsolutePath());
        try {
            dbManager = new DatabaseManager(dbFile);
            dbManager.initializeTables();
            marketDAO = new MarketDAO(dbManager);
            omsManager = new OMSManager();
            marketManager = new MarketManager(marketDAO, omsManager);
            marketManager.setRequireResourceResearch(AmmoraConfig.isResourceResearchRequired());
            PlayerAccount.setRankThresholds(
                    AmmoraConfig.getRankThreshold(2),
                    AmmoraConfig.getRankThreshold(3),
                    AmmoraConfig.getRankThreshold(4),
                    AmmoraConfig.getRankThreshold(5)
            );
            marketManager.initialize();

            marketManager.setItemDeliveryHandler((playerUuid, resourceId, amount, displayName) -> {
                net.minecraft.server.MinecraftServer s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (s == null) return false;
                ServerPlayer p = s.getPlayerList().getPlayer(playerUuid);
                if (p == null) return false;
                net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(resourceId));
                if (it == null) return false;

                int rem = amount;
                int maxStack = it.getDefaultInstance().getMaxStackSize();
                int delivered = 0;
                while (rem > 0) {
                    int count = Math.min(rem, maxStack);
                    net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(it, count);
                    if (com.ammora.mod.network.PacketHandler.canPlayerHoldItem(p.getInventory(), stack, count)) {
                        p.getInventory().add(stack);
                        delivered += count;
                        rem -= count;
                    } else {
                        break;
                    }
                }
                if (delivered > 0) {
                    p.sendSystemMessage(Component.translatable("message.ammora.limit_buy_filled", delivered, displayName));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                if (rem > 0) {
                    try {
                        marketDAO.saveUnclaimedDelivery(UUID.randomUUID().toString(), playerUuid, resourceId, rem, System.currentTimeMillis());
                        p.sendSystemMessage(Component.translatable("message.ammora.buffer_saved", rem));
                    } catch (Exception ignored) {}
                }
                return true;
            });

            // Restore active event from DB if any
            try {
                var savedEvent = marketDAO.loadActiveEvent();
                if (savedEvent != null) {
                    marketEventManager.setActiveEvent(savedEvent, marketManager);
                    LOGGER.info("Restored active market event: {}", savedEvent.getTitle());
                }
            } catch (Exception ignored) {}

            // Apply loaded datapack definitions to MarketManager and MarketEventManager
            commodityListener.applyTo(marketManager);
            marketEventListener.applyTo(marketEventManager);
            contractListener.applyTo(marketManager);

            LOGGER.info("Ammora market engine and database successfully started.");

        } catch (Exception e) {
            LOGGER.error("Failed to start Ammora market engine", e);
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(commodityListener);
        event.addListener(marketEventListener);
        event.addListener(contractListener);
        LOGGER.info("Registered Ammora datapack reload listeners (commodities, market_events, delivery_contracts).");
    }

    @SubscribeEvent
    public void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getPlayer() != null && !event.getPlayer().isCreative()) {
            net.minecraft.world.level.block.entity.BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be instanceof com.ammora.mod.blocks.PlayerShopEntity shop) {
                if (!shop.isOwner(event.getPlayer())) {
                    event.setCanceled(true);
                    event.getPlayer().sendSystemMessage(Component.translatable("message.ammora.shop_owned_by", shop.getOwnerName()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getEntity() != null && !event.getEntity().isCreative()) {
            net.minecraft.world.level.block.entity.BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be instanceof com.ammora.mod.blocks.PlayerShopEntity shop) {
                if (!shop.isOwner(event.getEntity())) {
                    event.setCanceled(true);
                    event.getEntity().displayClientMessage(Component.translatable("message.ammora.shop_owned_by", shop.getOwnerName()), true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null || marketManager == null) return;

        var overworld = server.overworld();
        if (overworld != null) {
            long currentDay = overworld.getDayTime() / 24000L;
            if (lastTrackedDay == -1L) {
                lastTrackedDay = currentDay;
            } else if (currentDay != lastTrackedDay) {
                lastTrackedDay = currentDay;
                LOGGER.info("Running daily economic cycle for day {} (mean reversion, fluctuations, events)...", currentDay);

                // 1. Mean-reversion burning
                for (var res : marketManager.getAllResources()) {
                    com.ammora.mod.core.MarketEngine.applyMeanReversion(res, AmmoraConfig.getSurplusBurnRate());
                }

                // 2. Daily price fluctuations (+-10%)
                boolean fluctuationsEnabled = AmmoraConfig.ENABLE_DAILY_FLUCTUATIONS.get();
                double maxFluc = AmmoraConfig.MAX_DAILY_FLUCTUATION.get();
                for (var res : marketManager.getAllResources()) {
                    double oldSpotPrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                    if (fluctuationsEnabled) {
                        double delta = (RANDOM.nextDouble() * 2.0 - 1.0) * maxFluc;
                        delta = Math.round(delta * 1000.0) / 1000.0;
                        res.setDailyModifier(delta);
                    } else {
                        res.setDailyModifier(0.0);
                    }
                    double newSpotPrice = com.ammora.mod.core.MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                    try {
                        marketManager.recordDailyTransitionCandles(res, oldSpotPrice, newSpotPrice);
                    } catch (Exception e) {
                        LOGGER.error("Failed to record daily transition candles for " + res.getResourceId(), e);
                    }
                }

                // 3. Market Events lifecycle & check
                boolean eventsEnabled = AmmoraConfig.ENABLE_MARKET_EVENTS.get();
                double chance = AmmoraConfig.EVENT_CHANCE.get();
                int duration = AmmoraConfig.EVENT_DURATION_DAYS.get();
                String announcement = marketEventManager.onDayChanged(marketManager, eventsEnabled, chance, duration);

                if (announcement != null) {
                    server.getPlayerList().broadcastSystemMessage(Component.literal(announcement), false);
                }

                // 4. Save state to database
                try {
                    marketDAO.saveActiveEvent(marketEventManager.getActiveEvent());
                    for (var res : marketManager.getAllResources()) {
                        marketDAO.upsertMarket(res);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to save daily market state", e);
                }

                // 5. Carry fee deductions & daily contract generation
                try {
                    marketManager.applyDailyCarryFee(AmmoraConfig.getDailyCarryFeeRate());
                    marketManager.generateDailyContracts();
                } catch (Exception e) {
                    LOGGER.error("Failed to apply daily derivatives updates", e);
                }
            }
        }

        // Periodic processing every 20 ticks (1 second) for limit orders and contracts
        if (server.getTickCount() % 20 == 0) {
            try {
                marketManager.processPendingLimitOrders();
                marketManager.processContractTicks(event.getServer().overworld().getGameTime());
            } catch (Exception e) {
                LOGGER.error("Error processing periodic market derivatives", e);
            }
        }
    }

    public static void deliverPendingClaims(ServerPlayer player) {
        if (marketDAO == null) return;
        try {
            var unclaimed = marketDAO.getUnclaimedDeliveries(player.getUUID());
            if (unclaimed == null || unclaimed.isEmpty()) return;

            int totalDelivered = 0;
            boolean inventoryFull = false;

            for (var d : unclaimed) {
                net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
                if (d.itemNbt() != null && !d.itemNbt().isEmpty()) {
                    try {
                        net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag(d.itemNbt());
                        stack = net.minecraft.world.item.ItemStack.parseOptional(player.registryAccess(), tag);
                    } catch (Exception ignored) {}
                }
                if (stack.isEmpty()) {
                    net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(d.resourceId()));
                    if (it != null && it != net.minecraft.world.item.Items.AIR) {
                        stack = new net.minecraft.world.item.ItemStack(it);
                    }
                }
                if (stack.isEmpty()) {
                    marketDAO.deleteUnclaimedDelivery(d.deliveryId());
                    continue;
                }

                int rem = d.amount();
                int maxStack = stack.getMaxStackSize();
                int given = 0;

                while (rem > 0) {
                    int count = Math.min(rem, maxStack);
                    net.minecraft.world.item.ItemStack toAdd = stack.copyWithCount(count);
                    if (com.ammora.mod.network.PacketHandler.canPlayerHoldItem(player.getInventory(), toAdd, count)) {
                        player.getInventory().add(toAdd);
                        given += count;
                        rem -= count;
                    } else {
                        inventoryFull = true;
                        break;
                    }
                }

                totalDelivered += given;
                if (rem <= 0) {
                    marketDAO.deleteUnclaimedDelivery(d.deliveryId());
                } else if (given > 0) {
                    marketDAO.updateUnclaimedDeliveryAmount(d.deliveryId(), rem);
                }

                if (inventoryFull) break;
            }

            if (totalDelivered > 0) {
                player.sendSystemMessage(Component.translatable("message.ammora.buffer_delivered", totalDelivered));
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            if (inventoryFull) {
                player.sendSystemMessage(Component.translatable("message.ammora.inventory_full_buffer"));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to deliver pending claims to " + player.getName().getString(), e);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deliverPendingClaims(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.ammora.mod.core.TradeSessionManager.getInstance().onPlayerDisconnect(player);
        }
    }

    public static void openTerminalScreen(ServerPlayer player, BlockPos pos) {
        deliverPendingClaims(player);
        String resId = "minecraft:iron_ingot";
        if (pos != null && player.level().getBlockEntity(pos) instanceof ExchangeTerminalEntity terminal) {
            resId = terminal.getMonitoredResource();
        }
        PacketHandler.sendMarketDataToClient(player, resId, "", false);
    }

    public static MarketManager getMarketManager() {
        return marketManager;
    }

    public static MarketDAO getMarketDAO() {
        return marketDAO;
    }

    public static OMSManager getOmsManager() {
        return omsManager;
    }

    public static MarketEventManager getMarketEventManager() {
        return marketEventManager;
    }

    public static CommodityReloadListener getCommodityListener() {
        return commodityListener;
    }

    public static MarketEventReloadListener getMarketEventListener() {
        return marketEventListener;
    }

    public static ContractReloadListener getContractListener() {
        return contractListener;
    }
}
