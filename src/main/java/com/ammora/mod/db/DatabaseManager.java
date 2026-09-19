package com.ammora.mod.db;

import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection and schema migrations.
 */
public class DatabaseManager {

    private final SQLiteDataSource dataSource;
    private Connection keepAliveConnection;

    public DatabaseManager(File dbFile) {
        this.dataSource = new SQLiteDataSource();
        if (dbFile != null) {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            this.dataSource.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } else {
            String memUrl = "jdbc:sqlite:file:memdb_" + System.nanoTime() + "?mode=memory&cache=shared";
            this.dataSource.setUrl(memUrl);
            try {
                this.keepAliveConnection = this.dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize in-memory SQLite database", e);
            }
        }
    }

    private final java.util.concurrent.ExecutorService asyncExecutor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Exchange-DB-Worker");
        t.setDaemon(true);
        return t;
    });

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public <T> java.util.concurrent.CompletableFuture<T> supplyAsync(java.util.concurrent.Callable<T> task) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException re) throw re;
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }

    public java.util.concurrent.CompletableFuture<Void> runAsync(Runnable task) {
        return java.util.concurrent.CompletableFuture.runAsync(task, asyncExecutor);
    }

    public void close() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
            }
        }
        if (keepAliveConnection != null) {
            try {
                keepAliveConnection.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Initializes database tables if they do not already exist.
     */
    public void initializeTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");

            // Markets table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS markets (
                    resource_id TEXT PRIMARY KEY,
                    display_name TEXT NOT NULL,
                    base_price REAL NOT NULL,
                    target_reserve REAL NOT NULL,
                    current_stock REAL NOT NULL,
                    elasticity REAL NOT NULL,
                    max_reserve REAL NOT NULL,
                    disposal_alpha REAL NOT NULL,
                    fee_rate REAL NOT NULL,
                    min_price_floor REAL NOT NULL,
                    daily_modifier REAL DEFAULT 0.0,
                    event_modifier REAL DEFAULT 0.0
                );
            """);

            // Safe column migrations for existing databases
            try {
                stmt.execute("ALTER TABLE markets ADD COLUMN daily_modifier REAL DEFAULT 0.0;");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE markets ADD COLUMN event_modifier REAL DEFAULT 0.0;");
            } catch (SQLException ignored) {}

            // Active Market Events table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS active_events (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    affected_resource TEXT NOT NULL,
                    price_multiplier REAL NOT NULL,
                    remaining_days INTEGER NOT NULL
                );
            """);

            // Candlesticks table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS candlesticks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    resource_id TEXT NOT NULL,
                    timeframe TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    open REAL NOT NULL,
                    high REAL NOT NULL,
                    low REAL NOT NULL,
                    close REAL NOT NULL,
                    volume REAL NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_candles ON candlesticks (resource_id, timeframe, timestamp);");

            // Player Accounts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    balance_cbx REAL NOT NULL,
                    rep_points INTEGER NOT NULL,
                    rep_level INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                );
            """);

            // OMS Positions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS oms_positions (
                    position_id TEXT PRIMARY KEY,
                    player_uuid TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    amount_units REAL NOT NULL,
                    invested_cbx REAL NOT NULL,
                    avg_buy_price REAL NOT NULL,
                    opened_timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_oms_player ON oms_positions (player_uuid);");
            stmt.execute("DELETE FROM oms_positions WHERE amount_units <= 0.001;");

            // Order History table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_history (
                    order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    order_type TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    price REAL NOT NULL,
                    fee REAL NOT NULL,
                    disposal_fee REAL NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_order_history_player ON order_history (player_uuid, timestamp DESC);");

            // Limit Orders table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS limit_orders (
                    order_id TEXT PRIMARY KEY,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    order_type TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    limit_price REAL NOT NULL,
                    reserved_cbx REAL NOT NULL,
                    created_timestamp INTEGER NOT NULL,
                    status TEXT NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_limit_player ON limit_orders (player_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_limit_status ON limit_orders (status);");

            // Delivery Contracts table (Futures/Bounties)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS delivery_contracts (
                    contract_id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    target_amount INTEGER NOT NULL,
                    delivered_amount INTEGER NOT NULL,
                    guaranteed_price REAL NOT NULL,
                    collateral_cbx REAL NOT NULL,
                    player_uuid TEXT,
                    player_name TEXT,
                    deadline_tick INTEGER NOT NULL,
                    reward_rep INTEGER NOT NULL,
                    status TEXT NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_contracts_player ON delivery_contracts (player_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_contracts_status ON delivery_contracts (status);");

            // P2P Transfers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS p2p_transfers (
                    transfer_id TEXT PRIMARY KEY,
                    from_uuid TEXT NOT NULL,
                    from_name TEXT NOT NULL,
                    to_uuid TEXT NOT NULL,
                    to_name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transfers_from ON p2p_transfers (from_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transfers_to ON p2p_transfers (to_uuid);");

            // Unclaimed Deliveries table (for limit buy orders filled while player is offline)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS unclaimed_deliveries (
                    delivery_id TEXT PRIMARY KEY,
                    player_uuid TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_unclaimed_player ON unclaimed_deliveries (player_uuid);");
            try {
                stmt.execute("ALTER TABLE unclaimed_deliveries ADD COLUMN item_nbt TEXT DEFAULT '';");
            } catch (Exception ignored) {}

            // Player Shops (Rust-style Vending Machines with Upgrades)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_shops (
                    shop_id TEXT PRIMARY KEY,
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    shop_name TEXT NOT NULL,
                    dimension TEXT NOT NULL,
                    pos_x INTEGER NOT NULL,
                    pos_y INTEGER NOT NULL,
                    pos_z INTEGER NOT NULL,
                    is_broadcast INTEGER NOT NULL DEFAULT 0,
                    total_sales INTEGER NOT NULL DEFAULT 0,
                    revenue_accumulated REAL NOT NULL DEFAULT 0.0,
                    created_at INTEGER NOT NULL,
                    max_slots INTEGER NOT NULL DEFAULT 5,
                    slot_capacity INTEGER NOT NULL DEFAULT 64,
                    network_unlocked INTEGER NOT NULL DEFAULT 0
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shops_owner ON player_shops (owner_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shops_broadcast ON player_shops (is_broadcast);");

            // Safe column migrations for existing player_shops
            try { stmt.execute("ALTER TABLE player_shops ADD COLUMN max_slots INTEGER NOT NULL DEFAULT 5;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE player_shops ADD COLUMN slot_capacity INTEGER NOT NULL DEFAULT 64;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE player_shops ADD COLUMN network_unlocked INTEGER NOT NULL DEFAULT 0;"); } catch (SQLException ignored) {}

            // Safe migrations: Rename USDT columns to CBX
            try { stmt.execute("ALTER TABLE accounts RENAME COLUMN balance_usdt TO balance_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE oms_positions RENAME COLUMN invested_usdt TO invested_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE limit_orders RENAME COLUMN reserved_usdt TO reserved_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE delivery_contracts RENAME COLUMN collateral_usdt TO collateral_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE player_shop_slots RENAME COLUMN price_usdt TO price_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE market_buy_requests RENAME COLUMN escrow_usdt TO escrow_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE market_transactions RENAME COLUMN total_usdt TO total_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE market_transactions RENAME COLUMN fee_usdt TO fee_cbx;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE community_quests RENAME COLUMN reward_usdt TO reward_cbx;"); } catch (SQLException ignored) {}

            // Player Shop Slots (Catalog indexing for remote search)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_shop_slots (
                    shop_id TEXT NOT NULL,
                    slot_index INTEGER NOT NULL,
                    item_id TEXT NOT NULL,
                    item_nbt TEXT,
                    display_name TEXT NOT NULL,
                    price_cbx REAL NOT NULL,
                    stock_count INTEGER NOT NULL,
                    PRIMARY KEY (shop_id, slot_index)
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shop_slots_item ON player_shop_slots (item_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shop_slots_active ON player_shop_slots (shop_id, stock_count);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shop_slots_catalog ON player_shop_slots (stock_count, price_cbx);");

            // Market Buy Requests (Escrow RFQ / Bounties)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS market_buy_requests (
                    request_id TEXT PRIMARY KEY,
                    buyer_uuid TEXT NOT NULL,
                    buyer_name TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    item_nbt TEXT,
                    display_name TEXT NOT NULL,
                    unit_price REAL NOT NULL,
                    amount_requested INTEGER NOT NULL,
                    amount_fulfilled INTEGER NOT NULL DEFAULT 0,
                    escrow_cbx REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_buy_req_buyer ON market_buy_requests (buyer_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_buy_req_status ON market_buy_requests (status);");

            // Market Transactions (Public Ledger)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS market_transactions (
                    tx_id TEXT PRIMARY KEY,
                    tx_type TEXT NOT NULL,
                    shop_id TEXT,
                    buyer_uuid TEXT NOT NULL,
                    buyer_name TEXT NOT NULL,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    item_name TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    total_cbx REAL NOT NULL,
                    fee_cbx REAL NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON market_transactions (timestamp DESC);");

            // Community Quests & Bounties
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS community_quests (
                    quest_id TEXT PRIMARY KEY,
                    creator_uuid TEXT NOT NULL,
                    creator_name TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    reward_cbx REAL NOT NULL,
                    status TEXT NOT NULL,
                    worker_uuid TEXT,
                    worker_name TEXT,
                    created_at INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_quests_status ON community_quests (status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_quests_creator ON community_quests (creator_uuid);");

            // Player Unlocked Resources (Research / License System)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_unlocked_resources (
                    player_uuid TEXT NOT NULL,
                    resource_id TEXT NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, resource_id)
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_unlocked_player ON player_unlocked_resources (player_uuid);");

            // Live Auctions table (Real-time English Auction with Buyout & Escrow)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS live_auctions (
                    auction_id TEXT PRIMARY KEY,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    item_nbt TEXT,
                    display_name TEXT NOT NULL,
                    item_count INTEGER NOT NULL,
                    start_price REAL NOT NULL,
                    current_bid REAL NOT NULL,
                    min_bid_step REAL NOT NULL,
                    buyout_price REAL NOT NULL,
                    highest_bidder_uuid TEXT,
                    highest_bidder_name TEXT,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE'
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_status ON live_auctions (status, expires_at);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_seller ON live_auctions (seller_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_bidder ON live_auctions (highest_bidder_uuid);");

            // Companies / Corporate Accounts (Joint Accounts)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS companies (
                    company_id TEXT PRIMARY KEY,
                    company_name TEXT NOT NULL UNIQUE,
                    owner_uuid TEXT NOT NULL,
                    balance_cbx REAL NOT NULL DEFAULT 0.0,
                    created_at INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_companies_owner ON companies (owner_uuid);");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS company_members (
                    company_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    daily_limit_cbx REAL NOT NULL DEFAULT 100.0,
                    spent_today_cbx REAL NOT NULL DEFAULT 0.0,
                    last_spent_day INTEGER NOT NULL DEFAULT 0,
                    joined_at INTEGER NOT NULL,
                    PRIMARY KEY (company_id, player_uuid)
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_comp_mem_player ON company_members (player_uuid);");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS company_ledger (
                    entry_id TEXT PRIMARY KEY,
                    company_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    amount_cbx REAL NOT NULL,
                    description TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_comp_ledger ON company_ledger (company_id, timestamp DESC);");

            // Safe column migration for player_shops: add company_id
            try { stmt.execute("ALTER TABLE player_shops ADD COLUMN company_id TEXT DEFAULT NULL;"); } catch (SQLException ignored) {}

            // System Configuration key-value table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS system_config (
                    config_key TEXT PRIMARY KEY,
                    config_value TEXT NOT NULL
                );
            """);
        }
    }
}
