package com.ammora.mod.db;

import com.ammora.mod.core.Candle;
import com.ammora.mod.core.DeliveryContract;
import com.ammora.mod.core.LedgerEntry;
import com.ammora.mod.core.LimitOrder;
import com.ammora.mod.core.MarketResource;
import com.ammora.mod.core.OMSPosition;
import com.ammora.mod.core.P2PTransfer;
import com.ammora.mod.core.events.MarketEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Data Access Object for SQLite operations.
 */
public class MarketDAO {

    private final DatabaseManager dbManager;

    public MarketDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void upsertMarket(MarketResource res) throws SQLException {
        String sql = """
            INSERT INTO markets (resource_id, display_name, base_price, target_reserve, current_stock, elasticity, max_reserve, disposal_alpha, fee_rate, min_price_floor, daily_modifier, event_modifier)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(resource_id) DO UPDATE SET
                display_name = excluded.display_name,
                base_price = excluded.base_price,
                target_reserve = excluded.target_reserve,
                current_stock = excluded.current_stock,
                elasticity = excluded.elasticity,
                max_reserve = excluded.max_reserve,
                disposal_alpha = excluded.disposal_alpha,
                fee_rate = excluded.fee_rate,
                min_price_floor = excluded.min_price_floor,
                daily_modifier = excluded.daily_modifier,
                event_modifier = excluded.event_modifier;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.getResourceId());
            ps.setString(2, res.getDisplayName());
            ps.setDouble(3, res.getBasePrice());
            ps.setDouble(4, res.getTargetReserve());
            ps.setDouble(5, res.getCurrentStock());
            ps.setDouble(6, res.getElasticity());
            ps.setDouble(7, res.getMaxReserve());
            ps.setDouble(8, res.getDisposalAlpha());
            ps.setDouble(9, res.getFeeRate());
            ps.setDouble(10, res.getMinPriceFloor());
            ps.setDouble(11, res.getDailyModifier());
            ps.setDouble(12, res.getEventModifier());
            ps.executeUpdate();
        }
    }

    public List<MarketResource> loadAllMarkets() throws SQLException {
        List<MarketResource> list = new ArrayList<>();
        String sql = "SELECT * FROM markets;";
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double dailyMod = 0.0;
                double eventMod = 0.0;
                try {
                    dailyMod = rs.getDouble("daily_modifier");
                    eventMod = rs.getDouble("event_modifier");
                } catch (Exception ignored) {}

                list.add(new MarketResource(
                        rs.getString("resource_id"),
                        rs.getString("display_name"),
                        rs.getDouble("base_price"),
                        rs.getDouble("target_reserve"),
                        rs.getDouble("current_stock"),
                        rs.getDouble("elasticity"),
                        rs.getDouble("max_reserve"),
                        rs.getDouble("disposal_alpha"),
                        rs.getDouble("fee_rate"),
                        rs.getDouble("min_price_floor"),
                        dailyMod,
                        eventMod
                ));
            }
        }
        return list;
    }

    public MarketResource getResource(String resourceId) throws SQLException {
        String sql = "SELECT * FROM markets WHERE resource_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double dailyMod = 0.0;
                    double eventMod = 0.0;
                    try {
                        dailyMod = rs.getDouble("daily_modifier");
                        eventMod = rs.getDouble("event_modifier");
                    } catch (Exception ignored) {}
                    return new MarketResource(
                            rs.getString("resource_id"),
                            rs.getString("display_name"),
                            rs.getDouble("base_price"),
                            rs.getDouble("target_reserve"),
                            rs.getDouble("current_stock"),
                            rs.getDouble("elasticity"),
                            rs.getDouble("max_reserve"),
                            rs.getDouble("disposal_alpha"),
                            rs.getDouble("fee_rate"),
                            rs.getDouble("min_price_floor"),
                            dailyMod,
                            eventMod
                    );
                }
            }
        }
        return null;
    }

    public void recordCandle(String resourceId, String timeframe, Candle candle) throws SQLException {
        String sql = """
            INSERT INTO candlesticks (resource_id, timeframe, timestamp, open, high, low, close, volume)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            ps.setLong(3, candle.getTimestamp());
            ps.setDouble(4, candle.getOpen());
            ps.setDouble(5, candle.getHigh());
            ps.setDouble(6, candle.getLow());
            ps.setDouble(7, candle.getClose());
            ps.setDouble(8, candle.getVolume());
            ps.executeUpdate();
        }
    }

    /**
     * Inserts a new candle or updates an existing candle for the exact timestamp slot.
     */
    public void upsertCandle(String resourceId, String timeframe, Candle candle) throws SQLException {
        String checkSql = "SELECT id FROM candlesticks WHERE resource_id = ? AND timeframe = ? AND timestamp = ? LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            ps.setLong(3, candle.getTimestamp());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String updateSql = "UPDATE candlesticks SET open = ?, high = ?, low = ?, close = ?, volume = ? WHERE id = ?;";
                    try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                        ups.setDouble(1, candle.getOpen());
                        ups.setDouble(2, candle.getHigh());
                        ups.setDouble(3, candle.getLow());
                        ups.setDouble(4, candle.getClose());
                        ups.setDouble(5, candle.getVolume());
                        ups.setInt(6, id);
                        ups.executeUpdate();
                    }
                    return;
                }
            }
        }
        recordCandle(resourceId, timeframe, candle);
    }

    /**
     * Updates the current timeframe candle or starts a new one for a live trade.
     */
    public void recordTradePrice(String resourceId, String timeframe, double price, double volume, long stepMs) throws SQLException {
        long now = System.currentTimeMillis();
        long bucket = (now / stepMs) * stepMs;

        String checkSql = "SELECT id, high, low, close, volume FROM candlesticks WHERE resource_id = ? AND timeframe = ? AND timestamp = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            ps.setLong(3, bucket);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    double high = Math.max(rs.getDouble("high"), price);
                    double low = Math.min(rs.getDouble("low"), price);
                    double newVol = rs.getDouble("volume") + volume;
                    String updateSql = "UPDATE candlesticks SET high = ?, low = ?, close = ?, volume = ? WHERE id = ?;";
                    try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                        ups.setDouble(1, high);
                        ups.setDouble(2, low);
                        ups.setDouble(3, price);
                        ups.setDouble(4, newVol);
                        ups.setInt(5, id);
                        ups.executeUpdate();
                    }
                    return;
                }
            }
        }

        double prevClose = price;
        String prevSql = "SELECT close FROM candlesticks WHERE resource_id = ? AND timeframe = ? ORDER BY timestamp DESC LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(prevSql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prevClose = rs.getDouble("close");
                }
            }
        }

        double open = prevClose;
        double high = Math.max(open, price);
        double low = Math.min(open, price);
        recordCandle(resourceId, timeframe, new Candle(bucket, open, high, low, price, volume));
    }

    public List<Candle> getRecentCandles(String resourceId, String timeframe, int limit) throws SQLException {
        List<Candle> list = new ArrayList<>();
        String sql = """
            SELECT timestamp, open, high, low, close, volume
            FROM candlesticks
            WHERE resource_id = ? AND timeframe = ?
            ORDER BY timestamp DESC
            LIMIT ?;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Candle(
                            rs.getLong("timestamp"),
                            rs.getDouble("open"),
                            rs.getDouble("high"),
                            rs.getDouble("low"),
                            rs.getDouble("close"),
                            rs.getDouble("volume")
                    ));
                }
            }
        }
        // Return chronologically ordered (oldest to newest)
        return list.reversed();
    }

    public void deleteFutureCandles(String resourceId, String timeframe, long maxAllowedTimestamp) throws SQLException {
        String sql = "DELETE FROM candlesticks WHERE resource_id = ? AND timeframe = ? AND timestamp > ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            ps.setString(2, timeframe);
            ps.setLong(3, maxAllowedTimestamp);
            ps.executeUpdate();
        }
    }

    public PlayerAccount getAccount(UUID playerUuid, String defaultName) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE player_uuid = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerAccount(
                            playerUuid,
                            rs.getString("player_name"),
                            rs.getDouble("balance_cbx"),
                            rs.getInt("rep_points"),
                            rs.getInt("rep_level"),
                            rs.getLong("updated_at")
                    );
                }
            }
        }
        // Create initial account if absent
        PlayerAccount newAcc = new PlayerAccount(playerUuid, defaultName, 100.0, 0, 1, System.currentTimeMillis());
        saveAccount(newAcc);
        return newAcc;
    }

    public void saveAccount(PlayerAccount account) throws SQLException {
        String sql = """
            INSERT INTO accounts (player_uuid, player_name, balance_cbx, rep_points, rep_level, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                balance_cbx = excluded.balance_cbx,
                rep_points = excluded.rep_points,
                rep_level = excluded.rep_level,
                updated_at = excluded.updated_at;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getPlayerUuid().toString());
            ps.setString(2, account.getPlayerName());
            ps.setDouble(3, account.getBalanceCbx());
            ps.setInt(4, account.getRepPoints());
            ps.setInt(5, account.getRepLevel());
            ps.setLong(6, account.getUpdatedAt());
            ps.executeUpdate();
        }
    }

    public List<PlayerAccount> getAllAccounts() throws SQLException {
        List<PlayerAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY balance_cbx DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PlayerAccount(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getDouble("balance_cbx"),
                        rs.getInt("rep_points"),
                        rs.getInt("rep_level"),
                        rs.getLong("updated_at")
                ));
            }
        }
        return list;
    }

    public void saveOMSPosition(OMSPosition pos) throws SQLException {
        if (pos.getAmountUnits() <= 0.001) {
            deleteOMSPosition(pos.getPositionId());
            return;
        }
        String sql = """
            INSERT INTO oms_positions (position_id, player_uuid, resource_id, amount_units, invested_cbx, avg_buy_price, opened_timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(position_id) DO UPDATE SET
                amount_units = excluded.amount_units,
                invested_cbx = excluded.invested_cbx,
                avg_buy_price = excluded.avg_buy_price;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pos.getPositionId());
            ps.setString(2, pos.getPlayerUuid().toString());
            ps.setString(3, pos.getResourceId());
            ps.setDouble(4, pos.getAmountUnits());
            ps.setDouble(5, pos.getInvestedCbx());
            ps.setDouble(6, pos.getAvgBuyPrice());
            ps.setLong(7, pos.getOpenedTimestamp());
            ps.executeUpdate();
        }
    }

    public void deleteOMSPosition(String positionId) throws SQLException {
        String sql = "DELETE FROM oms_positions WHERE position_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, positionId);
            ps.executeUpdate();
        }
    }

    public List<OMSPosition> getOMSPositions(UUID playerUuid) throws SQLException {
        List<OMSPosition> list = new ArrayList<>();
        String sql = "SELECT * FROM oms_positions WHERE player_uuid = ? AND amount_units > 0.001;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OMSPosition(
                            rs.getString("position_id"),
                            playerUuid,
                            rs.getString("resource_id"),
                            rs.getDouble("amount_units"),
                            rs.getDouble("invested_cbx"),
                            rs.getDouble("avg_buy_price"),
                            rs.getLong("opened_timestamp")
                    ));
                }
            }
        }
        return list;
    }

    public void recordOrder(UUID playerUuid, String resourceId, String type, int amount,
                            double price, double fee, double disposalFee) throws SQLException {
        String sql = """
            INSERT INTO order_history (player_uuid, resource_id, order_type, amount, price, fee, disposal_fee, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, resourceId);
            ps.setString(3, type);
            ps.setInt(4, amount);
            ps.setDouble(5, price);
            ps.setDouble(6, fee);
            ps.setDouble(7, disposalFee);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void saveActiveEvent(MarketEvent event) throws SQLException {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM active_events;");
            if (event != null) {
                String sql = "INSERT INTO active_events (id, title, description, affected_resource, price_multiplier, remaining_days) VALUES (?, ?, ?, ?, ?, ?);";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, event.getId());
                    ps.setString(2, event.getTitle());
                    ps.setString(3, event.getDescription());
                    ps.setString(4, event.getAffectedResourceId());
                    ps.setDouble(5, event.getPriceMultiplier());
                    ps.setInt(6, event.getRemainingDays());
                    ps.executeUpdate();
                }
            }
        }
    }

    public MarketEvent loadActiveEvent() throws SQLException {
        String sql = "SELECT * FROM active_events LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new MarketEvent(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("affected_resource"),
                        rs.getDouble("price_multiplier"),
                        rs.getInt("remaining_days")
                );
            }
        }
        return null;
    }

    public List<OMSPosition> loadAllOMSPositions() throws SQLException {
        List<OMSPosition> list = new ArrayList<>();
        String sql = "SELECT * FROM oms_positions WHERE amount_units > 0.001;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new OMSPosition(
                        rs.getString("position_id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("resource_id"),
                        rs.getDouble("amount_units"),
                        rs.getDouble("invested_cbx"),
                        rs.getDouble("avg_buy_price"),
                        rs.getLong("opened_timestamp")
                ));
            }
        }
        return list;
    }

    public void saveLimitOrder(LimitOrder order) throws SQLException {
        String sql = """
            INSERT INTO limit_orders (order_id, player_uuid, player_name, resource_id, order_type, amount, limit_price, reserved_cbx, created_timestamp, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(order_id) DO UPDATE SET
                status = excluded.status;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getOrderId());
            ps.setString(2, order.getPlayerUuid().toString());
            ps.setString(3, order.getPlayerName());
            ps.setString(4, order.getResourceId());
            ps.setString(5, order.getOrderType());
            ps.setInt(6, order.getAmount());
            ps.setDouble(7, order.getLimitPrice());
            ps.setDouble(8, order.getReservedCbx());
            ps.setLong(9, order.getCreatedTimestamp());
            ps.setString(10, order.getStatus());
            ps.executeUpdate();
        }
    }

    public List<LimitOrder> getActiveLimitOrders() throws SQLException {
        List<LimitOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM limit_orders WHERE status = 'PENDING';";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new LimitOrder(
                        rs.getString("order_id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("resource_id"),
                        rs.getString("order_type"),
                        rs.getInt("amount"),
                        rs.getDouble("limit_price"),
                        rs.getDouble("reserved_cbx"),
                        rs.getLong("created_timestamp"),
                        rs.getString("status")
                ));
            }
        }
        return list;
    }

    public List<LimitOrder> getPlayerLimitOrders(UUID playerUuid) throws SQLException {
        List<LimitOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM limit_orders WHERE player_uuid = ? ORDER BY created_timestamp DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LimitOrder(
                            rs.getString("order_id"),
                            playerUuid,
                            rs.getString("player_name"),
                            rs.getString("resource_id"),
                            rs.getString("order_type"),
                            rs.getInt("amount"),
                            rs.getDouble("limit_price"),
                            rs.getDouble("reserved_cbx"),
                            rs.getLong("created_timestamp"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    public List<LimitOrder> getPlayerActiveLimitOrders(UUID playerUuid) throws SQLException {
        List<LimitOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM limit_orders WHERE player_uuid = ? AND status = 'PENDING' ORDER BY created_timestamp DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LimitOrder(
                            rs.getString("order_id"),
                            playerUuid,
                            rs.getString("player_name"),
                            rs.getString("resource_id"),
                            rs.getString("order_type"),
                            rs.getInt("amount"),
                            rs.getDouble("limit_price"),
                            rs.getDouble("reserved_cbx"),
                            rs.getLong("created_timestamp"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    public LimitOrder getLimitOrder(String orderId) throws SQLException {
        String sql = "SELECT * FROM limit_orders WHERE order_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LimitOrder(
                            rs.getString("order_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("resource_id"),
                            rs.getString("order_type"),
                            rs.getInt("amount"),
                            rs.getDouble("limit_price"),
                            rs.getDouble("reserved_cbx"),
                            rs.getLong("created_timestamp"),
                            rs.getString("status")
                    );
                }
            }
        }
        return null;
    }

    public void updateLimitOrderStatus(String orderId, String status) throws SQLException {
        String sql = "UPDATE limit_orders SET status = ? WHERE order_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, orderId);
            ps.executeUpdate();
        }
    }

    public void saveContract(DeliveryContract contract) throws SQLException {
        String sql = """
            INSERT INTO delivery_contracts (contract_id, title, resource_id, target_amount, delivered_amount, guaranteed_price, collateral_cbx, player_uuid, player_name, deadline_tick, reward_rep, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(contract_id) DO UPDATE SET
                delivered_amount = excluded.delivered_amount,
                player_uuid = excluded.player_uuid,
                player_name = excluded.player_name,
                deadline_tick = excluded.deadline_tick,
                status = excluded.status;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contract.getContractId());
            ps.setString(2, contract.getTitle());
            ps.setString(3, contract.getResourceId());
            ps.setInt(4, contract.getTargetAmount());
            ps.setInt(5, contract.getDeliveredAmount());
            ps.setDouble(6, contract.getGuaranteedUnitPrice());
            ps.setDouble(7, contract.getCollateralCbx());
            ps.setString(8, contract.getAcceptedPlayerUuid() != null ? contract.getAcceptedPlayerUuid().toString() : null);
            ps.setString(9, contract.getAcceptedPlayerName());
            ps.setLong(10, contract.getDeadlineTick());
            ps.setInt(11, contract.getRewardRep());
            ps.setString(12, contract.getStatus());
            ps.executeUpdate();
        }
    }

    public List<DeliveryContract> getAvailableAndPlayerContracts(UUID playerUuid) throws SQLException {
        List<DeliveryContract> list = new ArrayList<>();
        String sql = "SELECT * FROM delivery_contracts WHERE status = 'OPEN' OR (player_uuid = ? AND status = 'ACTIVE');";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid != null ? playerUuid.toString() : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pUuidStr = rs.getString("player_uuid");
                    list.add(new DeliveryContract(
                            rs.getString("contract_id"),
                            rs.getString("title"),
                            rs.getString("resource_id"),
                            rs.getInt("target_amount"),
                            rs.getInt("delivered_amount"),
                            rs.getDouble("guaranteed_price"),
                            rs.getDouble("collateral_cbx"),
                            pUuidStr != null && !pUuidStr.isEmpty() ? UUID.fromString(pUuidStr) : null,
                            rs.getString("player_name"),
                            rs.getLong("deadline_tick"),
                            rs.getInt("reward_rep"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    public List<DeliveryContract> getAllActiveContracts() throws SQLException {
        List<DeliveryContract> list = new ArrayList<>();
        String sql = "SELECT * FROM delivery_contracts WHERE status = 'ACTIVE';";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String pUuidStr = rs.getString("player_uuid");
                list.add(new DeliveryContract(
                        rs.getString("contract_id"),
                        rs.getString("title"),
                        rs.getString("resource_id"),
                        rs.getInt("target_amount"),
                        rs.getInt("delivered_amount"),
                        rs.getDouble("guaranteed_price"),
                        rs.getDouble("collateral_cbx"),
                        pUuidStr != null && !pUuidStr.isEmpty() ? UUID.fromString(pUuidStr) : null,
                        rs.getString("player_name"),
                        rs.getLong("deadline_tick"),
                        rs.getInt("reward_rep"),
                        rs.getString("status")
                ));
            }
        }
        return list;
    }

    public void updateContractStatus(String contractId, String status) throws SQLException {
        String sql = "UPDATE delivery_contracts SET status = ? WHERE contract_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, contractId);
            ps.executeUpdate();
        }
    }

    public DeliveryContract getContract(String contractId) throws SQLException {
        String sql = "SELECT * FROM delivery_contracts WHERE contract_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String pUuidStr = rs.getString("player_uuid");
                    return new DeliveryContract(
                            rs.getString("contract_id"),
                            rs.getString("title"),
                            rs.getString("resource_id"),
                            rs.getInt("target_amount"),
                            rs.getInt("delivered_amount"),
                            rs.getDouble("guaranteed_price"),
                            rs.getDouble("collateral_cbx"),
                            pUuidStr != null && !pUuidStr.isEmpty() ? UUID.fromString(pUuidStr) : null,
                            rs.getString("player_name"),
                            rs.getLong("deadline_tick"),
                            rs.getInt("reward_rep"),
                            rs.getString("status")
                    );
                }
            }
        }
        return null;
    }

    public void recordP2PTransfer(P2PTransfer transfer) throws SQLException {
        String sql = """
            INSERT INTO p2p_transfers (transfer_id, from_uuid, from_name, to_uuid, to_name, amount, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transfer.getTransferId());
            ps.setString(2, transfer.getFromUuid().toString());
            ps.setString(3, transfer.getFromName());
            ps.setString(4, transfer.getToUuid().toString());
            ps.setString(5, transfer.getToName());
            ps.setDouble(6, transfer.getAmount());
            ps.setLong(7, transfer.getTimestamp());
            ps.executeUpdate();
        }
    }

    public List<P2PTransfer> getPlayerTransfers(UUID playerUuid, int limit) throws SQLException {
        List<P2PTransfer> list = new ArrayList<>();
        String sql = "SELECT * FROM p2p_transfers WHERE from_uuid = ? OR to_uuid = ? ORDER BY timestamp DESC LIMIT ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerUuid.toString());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new P2PTransfer(
                            rs.getString("transfer_id"),
                            UUID.fromString(rs.getString("from_uuid")),
                            rs.getString("from_name"),
                            UUID.fromString(rs.getString("to_uuid")),
                            rs.getString("to_name"),
                            rs.getDouble("amount"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        }
        return list;
    }

    public List<LedgerEntry> getPlayerLedger(UUID playerUuid, int limit) throws SQLException {
        List<LedgerEntry> entries = new ArrayList<>();
        String pStr = playerUuid.toString();

        // 1. P2P transfers
        String sqlTransfers = "SELECT * FROM p2p_transfers WHERE from_uuid = ? OR to_uuid = ? ORDER BY timestamp DESC LIMIT ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlTransfers)) {
            ps.setString(1, pStr);
            ps.setString(2, pStr);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean isOutgoing = pStr.equals(rs.getString("from_uuid"));
                    double amt = rs.getDouble("amount");
                    String toName = rs.getString("to_name");
                    String fromName = rs.getString("from_name");
                    long ts = rs.getLong("timestamp");

                    if (isOutgoing) {
                        entries.add(new LedgerEntry("P2P_OUT", "key:wallet.ledger_to;" + toName, -amt, ts));
                    } else {
                        entries.add(new LedgerEntry("P2P_IN", "key:wallet.ledger_from;" + fromName, +amt, ts));
                    }
                }
            }
        }

        // 2. Orders from order_history
        String sqlOrders = "SELECT * FROM order_history WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlOrders)) {
            ps.setString(1, pStr);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String resId = rs.getString("resource_id");
                    String shortName = resId.replace("minecraft:", "");
                    String type = rs.getString("order_type");
                    int amount = rs.getInt("amount");
                    double price = rs.getDouble("price");
                    double total = amount * price;
                    long ts = rs.getLong("timestamp");

                    if ("BUY".equalsIgnoreCase(type) || "BUY_AUTO".equalsIgnoreCase(type)) {
                        entries.add(new LedgerEntry("BUY", "key:wallet.ledger_buy;" + shortName + ";" + amount, -total, ts));
                    } else if ("SELL".equalsIgnoreCase(type)) {
                        entries.add(new LedgerEntry("SELL", "key:wallet.ledger_sell;" + shortName + ";" + amount, +total, ts));
                    } else if ("OPEN_OMS".equalsIgnoreCase(type)) {
                        entries.add(new LedgerEntry("OMS", "key:wallet.ledger_oms_open;" + shortName, -total, ts));
                    } else if ("CLOSE_OMS".equalsIgnoreCase(type)) {
                        entries.add(new LedgerEntry("OMS", "key:wallet.ledger_oms_close;" + shortName, +total, ts));
                    }
                }
            }
        }

        // Sort combined list descending by timestamp
        entries.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public record UnclaimedDelivery(
            String deliveryId,
            UUID playerUuid,
            String resourceId,
            int amount,
            long timestamp,
            String itemNbt
    ) {
        public UnclaimedDelivery(String deliveryId, UUID playerUuid, String resourceId, int amount, long timestamp) {
            this(deliveryId, playerUuid, resourceId, amount, timestamp, "");
        }
    }

    public void saveUnclaimedDelivery(String deliveryId, UUID playerUuid, String resourceId, int amount, long timestamp) throws SQLException {
        saveUnclaimedDelivery(deliveryId, playerUuid, resourceId, amount, timestamp, "");
    }

    public void saveUnclaimedDelivery(String deliveryId, UUID playerUuid, String resourceId, int amount, long timestamp, String itemNbt) throws SQLException {
        String sql = """
            INSERT INTO unclaimed_deliveries (delivery_id, player_uuid, resource_id, amount, timestamp, item_nbt)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deliveryId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, resourceId);
            ps.setInt(4, amount);
            ps.setLong(5, timestamp);
            ps.setString(6, itemNbt != null ? itemNbt : "");
            ps.executeUpdate();
        }
    }

    public List<UnclaimedDelivery> getUnclaimedDeliveries(UUID playerUuid) throws SQLException {
        List<UnclaimedDelivery> list = new ArrayList<>();
        String sql = "SELECT * FROM unclaimed_deliveries WHERE player_uuid = ? ORDER BY timestamp ASC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nbt = "";
                    try {
                        nbt = rs.getString("item_nbt");
                        if (nbt == null) nbt = "";
                    } catch (Exception ignored) {}
                    list.add(new UnclaimedDelivery(
                            rs.getString("delivery_id"),
                            playerUuid,
                            rs.getString("resource_id"),
                            rs.getInt("amount"),
                            rs.getLong("timestamp"),
                            nbt
                    ));
                }
            }
        }
        return list;
    }

    public void updateUnclaimedDeliveryAmount(String deliveryId, int newAmount) throws SQLException {
        String sql = "UPDATE unclaimed_deliveries SET amount = ? WHERE delivery_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newAmount);
            ps.setString(2, deliveryId);
            ps.executeUpdate();
        }
    }

    public void deleteUnclaimedDelivery(String deliveryId) throws SQLException {
        String sql = "DELETE FROM unclaimed_deliveries WHERE delivery_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deliveryId);
            ps.executeUpdate();
        }
    }

    // ==========================================
    // PLAYER SHOPS & VENDING MACHINES
    // ==========================================

    public void saveOrUpdatePlayerShop(PlayerShopRecord shop) throws SQLException {
        String sql = """
            INSERT INTO player_shops (shop_id, owner_uuid, owner_name, shop_name, dimension, pos_x, pos_y, pos_z, is_broadcast, total_sales, revenue_accumulated, created_at, max_slots, slot_capacity, network_unlocked)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(shop_id) DO UPDATE SET
                owner_name = excluded.owner_name,
                shop_name = excluded.shop_name,
                is_broadcast = excluded.is_broadcast,
                total_sales = excluded.total_sales,
                revenue_accumulated = excluded.revenue_accumulated,
                max_slots = excluded.max_slots,
                slot_capacity = excluded.slot_capacity,
                network_unlocked = excluded.network_unlocked;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shop.getShopId());
            ps.setString(2, shop.getOwnerUuid().toString());
            ps.setString(3, shop.getOwnerName());
            ps.setString(4, shop.getShopName());
            ps.setString(5, shop.getDimension());
            ps.setInt(6, shop.getPosX());
            ps.setInt(7, shop.getPosY());
            ps.setInt(8, shop.getPosZ());
            ps.setInt(9, shop.isBroadcast() ? 1 : 0);
            ps.setInt(10, shop.getTotalSales());
            ps.setDouble(11, shop.getAccumulatedRevenue());
            ps.setLong(12, shop.getCreatedAt());
            ps.setInt(13, shop.getMaxSlots());
            ps.setInt(14, shop.getSlotCapacity());
            ps.setInt(15, shop.isNetworkUnlocked() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    private PlayerShopRecord mapPlayerShop(ResultSet rs) throws SQLException {
        int maxSlots = 5;
        int slotCapacity = 64;
        boolean networkUnlocked = false;
        try {
            maxSlots = rs.getInt("max_slots");
            if (maxSlots < 5) maxSlots = 5;
        } catch (SQLException ignored) {}
        try {
            slotCapacity = rs.getInt("slot_capacity");
            if (slotCapacity < 64) slotCapacity = 64;
        } catch (SQLException ignored) {}
        try {
            networkUnlocked = rs.getInt("network_unlocked") == 1;
        } catch (SQLException ignored) {}

        return new PlayerShopRecord(
                rs.getString("shop_id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("owner_name"),
                rs.getString("shop_name"),
                rs.getString("dimension"),
                rs.getInt("pos_x"),
                rs.getInt("pos_y"),
                rs.getInt("pos_z"),
                rs.getInt("is_broadcast") == 1,
                rs.getInt("total_sales"),
                rs.getDouble("revenue_accumulated"),
                rs.getLong("created_at"),
                maxSlots,
                slotCapacity,
                networkUnlocked
        );
    }

    public PlayerShopRecord getPlayerShop(String shopId) throws SQLException {
        String sql = "SELECT * FROM player_shops WHERE shop_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPlayerShop(rs);
                }
            }
        }
        return null;
    }

    public List<PlayerShopRecord> getPlayerShopsByOwner(UUID ownerUuid) throws SQLException {
        List<PlayerShopRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM player_shops WHERE owner_uuid = ? ORDER BY created_at DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPlayerShop(rs));
                }
            }
        }
        return list;
    }

    public List<PlayerShopRecord> getAllBroadcastShops() throws SQLException {
        List<PlayerShopRecord> list = new ArrayList<>();
        String sql = """
            SELECT p.*, (SELECT COUNT(1) FROM player_shop_slots s WHERE s.shop_id = p.shop_id AND s.stock_count > 0) AS active_count
            FROM player_shops p
            WHERE p.is_broadcast = 1
            ORDER BY p.total_sales DESC;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlayerShopRecord rec = mapPlayerShop(rs);
                try {
                    rec.setActiveSlotCount(rs.getInt("active_count"));
                } catch (Exception ignored) {}
                list.add(rec);
            }
        }
        return list;
    }

    // ==========================================
    // COMMUNITY QUESTS & BOUNTIES
    // ==========================================

    public void saveOrUpdateQuest(CommunityQuestRecord quest) throws SQLException {
        String sql = """
            INSERT INTO community_quests (quest_id, creator_uuid, creator_name, title, description, reward_cbx, status, worker_uuid, worker_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(quest_id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                reward_cbx = excluded.reward_cbx,
                status = excluded.status,
                worker_uuid = excluded.worker_uuid,
                worker_name = excluded.worker_name;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quest.getQuestId());
            ps.setString(2, quest.getCreatorUuid().toString());
            ps.setString(3, quest.getCreatorName());
            ps.setString(4, quest.getTitle());
            ps.setString(5, quest.getDescription());
            ps.setDouble(6, quest.getRewardCbx());
            ps.setString(7, quest.getStatus());
            ps.setString(8, quest.getWorkerUuid() != null ? quest.getWorkerUuid().toString() : null);
            ps.setString(9, quest.getWorkerName());
            ps.setLong(10, quest.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public CommunityQuestRecord getQuest(String questId) throws SQLException {
        String sql = "SELECT * FROM community_quests WHERE quest_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapQuest(rs);
                }
            }
        }
        return null;
    }

    public List<CommunityQuestRecord> getAllQuests() throws SQLException {
        List<CommunityQuestRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM community_quests ORDER BY created_at DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapQuest(rs));
            }
        }
        return list;
    }

    public void deleteQuest(String questId) throws SQLException {
        String sql = "DELETE FROM community_quests WHERE quest_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questId);
            ps.executeUpdate();
        }
    }

    private CommunityQuestRecord mapQuest(ResultSet rs) throws SQLException {
        String workerUuidStr = rs.getString("worker_uuid");
        UUID workerUuid = (workerUuidStr != null && !workerUuidStr.isEmpty()) ? UUID.fromString(workerUuidStr) : null;
        return new CommunityQuestRecord(
                rs.getString("quest_id"),
                UUID.fromString(rs.getString("creator_uuid")),
                rs.getString("creator_name"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDouble("reward_cbx"),
                rs.getString("status"),
                workerUuid,
                rs.getString("worker_name"),
                rs.getLong("created_at")
        );
    }

    public void deletePlayerShop(String shopId) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM player_shop_slots WHERE shop_id = ?;")) {
                ps1.setString(1, shopId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM player_shops WHERE shop_id = ?;")) {
                ps2.setString(1, shopId);
                ps2.executeUpdate();
            }
        }
    }

    public void saveOrUpdateShopSlot(ShopSlotRecord slot) throws SQLException {
        String sql = """
            INSERT INTO player_shop_slots (shop_id, slot_index, item_id, item_nbt, display_name, price_cbx, stock_count)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(shop_id, slot_index) DO UPDATE SET
                item_id = excluded.item_id,
                item_nbt = excluded.item_nbt,
                display_name = excluded.display_name,
                price_cbx = excluded.price_cbx,
                stock_count = excluded.stock_count;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slot.getShopId());
            ps.setInt(2, slot.getSlotIndex());
            ps.setString(3, slot.getItemId());
            ps.setString(4, slot.getItemNbt() != null ? slot.getItemNbt() : "");
            ps.setString(5, slot.getDisplayName());
            ps.setDouble(6, slot.getPriceCbx());
            ps.setInt(7, slot.getStockCount());
            ps.executeUpdate();
        }
    }

    public List<ShopSlotRecord> getShopSlots(String shopId) throws SQLException {
        List<ShopSlotRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM player_shop_slots WHERE shop_id = ? ORDER BY slot_index ASC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ShopSlotRecord(
                            rs.getString("shop_id"),
                            rs.getInt("slot_index"),
                            rs.getString("item_id"),
                            rs.getString("item_nbt"),
                            rs.getString("display_name"),
                            rs.getDouble("price_cbx"),
                            rs.getInt("stock_count")
                    ));
                }
            }
        }
        return list;
    }

    public List<ShopSlotRecord> getAllActiveCatalogSlots() throws SQLException {
        List<ShopSlotRecord> list = new ArrayList<>();
        String sql = """
            SELECT s.*, p.shop_name, p.owner_name FROM player_shop_slots s
            JOIN player_shops p ON s.shop_id = p.shop_id
            WHERE p.is_broadcast = 1 AND s.stock_count > 0 AND s.item_id IS NOT NULL AND s.item_id != ''
            ORDER BY s.price_cbx ASC;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ShopSlotRecord rec = new ShopSlotRecord(
                        rs.getString("shop_id"),
                        rs.getInt("slot_index"),
                        rs.getString("item_id"),
                        rs.getString("item_nbt"),
                        rs.getString("display_name"),
                        rs.getDouble("price_cbx"),
                        rs.getInt("stock_count")
                );
                try {
                    rec.setShopName(rs.getString("shop_name"));
                    rec.setOwnerName(rs.getString("owner_name"));
                } catch (Exception ignored) {}
                list.add(rec);
            }
        }
        return list;
    }

    // ==========================================
    // MARKET BUY REQUESTS (ESCROW RFQ)
    // ==========================================

    public void saveBuyRequest(BuyRequestRecord req) throws SQLException {
        String sql = """
            INSERT INTO market_buy_requests (request_id, buyer_uuid, buyer_name, item_id, item_nbt, display_name, unit_price, amount_requested, amount_fulfilled, escrow_cbx, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getRequestId());
            ps.setString(2, req.getBuyerUuid().toString());
            ps.setString(3, req.getBuyerName());
            ps.setString(4, req.getItemId());
            ps.setString(5, req.getItemNbt() != null ? req.getItemNbt() : "");
            ps.setString(6, req.getDisplayName());
            ps.setDouble(7, req.getUnitPrice());
            ps.setInt(8, req.getAmountRequested());
            ps.setInt(9, req.getAmountFulfilled());
            ps.setDouble(10, req.getEscrowCbx());
            ps.setString(11, req.getStatus());
            ps.setLong(12, req.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public void updateBuyRequest(BuyRequestRecord req) throws SQLException {
        String sql = """
            UPDATE market_buy_requests
            SET amount_fulfilled = ?, escrow_cbx = ?, status = ?
            WHERE request_id = ?;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, req.getAmountFulfilled());
            ps.setDouble(2, req.getEscrowCbx());
            ps.setString(3, req.getStatus());
            ps.setString(4, req.getRequestId());
            ps.executeUpdate();
        }
    }

    public BuyRequestRecord getBuyRequest(String requestId) throws SQLException {
        String sql = "SELECT * FROM market_buy_requests WHERE request_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BuyRequestRecord(
                            rs.getString("request_id"),
                            UUID.fromString(rs.getString("buyer_uuid")),
                            rs.getString("buyer_name"),
                            rs.getString("item_id"),
                            rs.getString("item_nbt"),
                            rs.getString("display_name"),
                            rs.getDouble("unit_price"),
                            rs.getInt("amount_requested"),
                            rs.getInt("amount_fulfilled"),
                            rs.getDouble("escrow_cbx"),
                            rs.getString("status"),
                            rs.getLong("created_at")
                    );
                }
            }
        }
        return null;
    }

    public List<BuyRequestRecord> getActiveBuyRequests() throws SQLException {
        List<BuyRequestRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM market_buy_requests WHERE status = 'ACTIVE' ORDER BY created_at DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BuyRequestRecord(
                        rs.getString("request_id"),
                        UUID.fromString(rs.getString("buyer_uuid")),
                        rs.getString("buyer_name"),
                        rs.getString("item_id"),
                        rs.getString("item_nbt"),
                        rs.getString("display_name"),
                        rs.getDouble("unit_price"),
                        rs.getInt("amount_requested"),
                        rs.getInt("amount_fulfilled"),
                        rs.getDouble("escrow_cbx"),
                        rs.getString("status"),
                        rs.getLong("created_at")
                ));
            }
        }
        return list;
    }

    public List<BuyRequestRecord> getPlayerBuyRequests(UUID buyerUuid) throws SQLException {
        List<BuyRequestRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM market_buy_requests WHERE buyer_uuid = ? ORDER BY created_at DESC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, buyerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BuyRequestRecord(
                            rs.getString("request_id"),
                            buyerUuid,
                            rs.getString("buyer_name"),
                            rs.getString("item_id"),
                            rs.getString("item_nbt"),
                            rs.getString("display_name"),
                            rs.getDouble("unit_price"),
                            rs.getInt("amount_requested"),
                            rs.getInt("amount_fulfilled"),
                            rs.getDouble("escrow_cbx"),
                            rs.getString("status"),
                            rs.getLong("created_at")
                    ));
                }
            }
        }
        return list;
    }

    // ==========================================
    // MARKET TRANSACTIONS (LEDGER)
    // ==========================================

    public void recordMarketTransaction(MarketTxRecord tx) throws SQLException {
        String sql = """
            INSERT INTO market_transactions (tx_id, tx_type, shop_id, buyer_uuid, buyer_name, seller_uuid, seller_name, item_id, item_name, amount, total_cbx, fee_cbx, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTxId());
            ps.setString(2, tx.getTxType());
            ps.setString(3, tx.getShopId());
            ps.setString(4, tx.getBuyerUuid().toString());
            ps.setString(5, tx.getBuyerName());
            ps.setString(6, tx.getSellerUuid().toString());
            ps.setString(7, tx.getSellerName());
            ps.setString(8, tx.getItemId());
            ps.setString(9, tx.getItemName());
            ps.setInt(10, tx.getAmount());
            ps.setDouble(11, tx.getTotalCbx());
            ps.setDouble(12, tx.getFeeCbx());
            ps.setLong(13, tx.getTimestamp());
            ps.executeUpdate();
        }
    }

    public List<MarketTxRecord> getRecentMarketTransactions(int limit) throws SQLException {
        List<MarketTxRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM market_transactions ORDER BY timestamp DESC LIMIT ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MarketTxRecord(
                            rs.getString("tx_id"),
                            rs.getString("tx_type"),
                            rs.getString("shop_id"),
                            UUID.fromString(rs.getString("buyer_uuid")),
                            rs.getString("buyer_name"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("seller_name"),
                            rs.getString("item_id"),
                            rs.getString("item_name"),
                            rs.getInt("amount"),
                            rs.getDouble("total_cbx"),
                            rs.getDouble("fee_cbx"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        }
        return list;
    }

    public Set<String> getUnlockedResources(UUID playerUuid) throws SQLException {
        Set<String> set = new HashSet<>();
        String sql = "SELECT resource_id FROM player_unlocked_resources WHERE player_uuid = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString("resource_id"));
                }
            }
        }
        return set;
    }

    public boolean isResourceUnlocked(UUID playerUuid, String resourceId) throws SQLException {
        String sql = "SELECT 1 FROM player_unlocked_resources WHERE player_uuid = ? AND resource_id = ? LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void unlockResource(UUID playerUuid, String resourceId, long timestamp) throws SQLException {
        String sql = "INSERT OR IGNORE INTO player_unlocked_resources (player_uuid, resource_id, unlocked_at) VALUES (?, ?, ?);";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, resourceId);
            ps.setLong(3, timestamp);
            ps.executeUpdate();
        }
    }
}
