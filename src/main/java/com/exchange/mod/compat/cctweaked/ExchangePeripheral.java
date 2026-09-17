package com.exchange.mod.compat.cctweaked;

import com.exchange.mod.ExchangeMod;
import com.exchange.mod.blocks.ExchangeTerminalEntity;
import com.exchange.mod.blocks.TradeDockEntity;
import com.exchange.mod.core.Candle;
import com.exchange.mod.core.MarketEngine;
import com.exchange.mod.core.MarketManager;
import com.exchange.mod.core.MarketResource;
import com.exchange.mod.db.PlayerAccount;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * ComputerCraft peripheral allowing CC computers and turtles to interact with the exchange.
 */
public class ExchangePeripheral implements IPeripheral {

    private final Object targetEntity;
    private final String type;

    public ExchangePeripheral(Object entity) {
        this.targetEntity = entity;
        this.type = (entity instanceof TradeDockEntity) ? "exchange_dock" : "exchange_terminal";
    }

    @Override
    public String getType() {
        return type;
    }

    @LuaFunction
    public final Map<String, Object> getPrice(String itemId) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketManager() == null) {
            map.put("error", "Market not initialized");
            return map;
        }
        MarketResource res = ExchangeMod.getMarketManager().getResource(itemId);
        if (res == null) {
            map.put("error", "Resource not found: " + itemId);
            return map;
        }

        double stock = res.getCurrentStock();
        map.put("resourceId", res.getResourceId());
        map.put("displayName", res.getDisplayName());
        map.put("spotPrice", MarketEngine.round2(MarketEngine.calculateSpotPrice(stock, res)));
        map.put("buyPrice", MarketEngine.round2(MarketEngine.calculateBuyPrice(stock, res)));
        map.put("sellPrice", MarketEngine.round2(MarketEngine.calculateSellPrice(stock, res)));
        map.put("disposalFee", MarketEngine.round2(MarketEngine.calculateDisposalFee(stock, res)));
        return map;
    }

    @LuaFunction
    public final Map<String, Object> getStock(String itemId) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketManager() == null) {
            map.put("error", "Market not initialized");
            return map;
        }
        MarketResource res = ExchangeMod.getMarketManager().getResource(itemId);
        if (res == null) {
            map.put("error", "Resource not found");
            return map;
        }

        double stock = res.getCurrentStock();
        double max = res.getMaxReserve();
        map.put("currentStock", stock);
        map.put("targetReserve", res.getTargetReserve());
        map.put("maxReserve", max);
        map.put("fillPercent", MarketEngine.round2((stock / max) * 100.0));
        map.put("status", stock > max ? "OVERFLOW" : (stock > res.getTargetReserve() ? "HIGH" : "NORMAL"));
        return map;
    }

    @LuaFunction
    public final Map<String, Object> getAccount(String playerUuidStr) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketDAO() == null) {
            map.put("error", "Database not initialized");
            return map;
        }
        try {
            UUID uuid = UUID.fromString(playerUuidStr);
            PlayerAccount acc = ExchangeMod.getMarketDAO().getAccount(uuid, "Trader");
            map.put("balanceCBX", acc.getBalanceCbx());
            map.put("balanceUSDT", acc.getBalanceCbx());
            map.put("repPoints", acc.getRepPoints());
            map.put("repLevel", acc.getRepLevel());
            map.put("brokerFeeRate", acc.getBrokerFeeRate());
        } catch (Exception e) {
            map.put("error", "Invalid UUID or error: " + e.getMessage());
        }
        return map;
    }

    @LuaFunction
    public final Map<String, Object> buy(String playerUuidStr, String itemId, int count) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketManager() == null) {
            map.put("success", false);
            map.put("message", "Market not available");
            return map;
        }
        try {
            UUID uuid = UUID.fromString(playerUuidStr);
            var result = ExchangeMod.getMarketManager().executeBuy(uuid, "Trader", itemId, count);
            map.put("success", result.success());
            map.put("message", result.message());
            map.put("cbxSpent", result.cbxAmount());
            map.put("usdtSpent", result.cbxAmount());
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", e.getMessage());
        }
        return map;
    }

    @LuaFunction
    public final Map<String, Object> sell(String playerUuidStr, String itemId, int count) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketManager() == null) {
            map.put("success", false);
            map.put("message", "Market not available");
            return map;
        }
        try {
            UUID uuid = UUID.fromString(playerUuidStr);
            var result = ExchangeMod.getMarketManager().executeSell(uuid, "Trader", itemId, count);
            map.put("success", result.success());
            map.put("message", result.message());
            map.put("cbxPayout", result.cbxAmount());
            map.put("usdtPayout", result.cbxAmount());
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", e.getMessage());
        }
        return map;
    }

    @LuaFunction
    public final List<Map<String, Object>> getCandles(String itemId, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (ExchangeMod.getMarketDAO() == null) return list;

        try {
            List<Candle> candles = ExchangeMod.getMarketDAO().getRecentCandles(itemId, "1d", Math.min(50, limit));
            for (Candle c : candles) {
                Map<String, Object> m = new HashMap<>();
                m.put("timestamp", c.getTimestamp());
                m.put("open", c.getOpen());
                m.put("high", c.getHigh());
                m.put("low", c.getLow());
                m.put("close", c.getClose());
                m.put("volume", c.getVolume());
                m.put("bullish", c.isBullish());
                list.add(m);
            }
        } catch (Exception ignored) {}
        return list;
    }

    @LuaFunction
    public final boolean setStopLoss(double price) {
        if (targetEntity instanceof TradeDockEntity dock) {
            dock.setStopLossPrice(price);
            return true;
        }
        return false;
    }

    @LuaFunction
    public final Map<String, Object> getDailyModifier(String itemId) {
        Map<String, Object> map = new HashMap<>();
        if (ExchangeMod.getMarketManager() == null) {
            map.put("error", "Market not initialized");
            return map;
        }
        MarketResource res = ExchangeMod.getMarketManager().getResource(itemId);
        if (res == null) {
            map.put("error", "Resource not found");
            return map;
        }
        map.put("dailyModifier", res.getDailyModifier());
        map.put("percent", MarketEngine.round2(res.getDailyModifier() * 100.0));
        map.put("eventModifier", res.getEventModifier());
        return map;
    }

    @LuaFunction
    public final Map<String, Object> getActiveEvent() {
        Map<String, Object> map = new HashMap<>();
        var eventMgr = ExchangeMod.getMarketEventManager();
        if (eventMgr == null || eventMgr.getActiveEvent() == null) {
            map.put("active", false);
            return map;
        }
        var ev = eventMgr.getActiveEvent();
        map.put("active", true);
        map.put("id", ev.getId());
        map.put("title", ev.getTitle());
        map.put("description", ev.getDescription());
        map.put("affectedResource", ev.getAffectedResourceId());
        map.put("priceMultiplier", ev.getPriceMultiplier());
        map.put("remainingDays", ev.getRemainingDays());
        return map;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ExchangePeripheral p && p.targetEntity == this.targetEntity;
    }
}
