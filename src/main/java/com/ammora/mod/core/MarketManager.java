package com.ammora.mod.core;

import com.ammora.mod.db.CompanyRecord;
import com.ammora.mod.db.MarketDAO;
import com.ammora.mod.db.PlayerAccount;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
* High-level market manager handling operations, caching, and database sync.
*/
public class MarketManager {

    @FunctionalInterface
    public interface ItemDeliveryHandler {
        boolean deliver(UUID playerUuid, String resourceId, int amount, String displayName);
    }

    private final MarketDAO dao;
    private final OMSManager omsManager;
    private final Map<String, MarketResource> markets = new ConcurrentHashMap<>();
    private ItemDeliveryHandler itemDeliveryHandler;
    private boolean requireResourceResearch = true;

    public MarketManager(MarketDAO dao, OMSManager omsManager) {
        this.dao = dao;
        this.omsManager = omsManager;
    }

    public void setItemDeliveryHandler(ItemDeliveryHandler handler) {
        this.itemDeliveryHandler = handler;
    }

    public boolean isRequireResourceResearch() {
        return requireResourceResearch;
    }

    public void setRequireResourceResearch(boolean requireResourceResearch) {
        this.requireResourceResearch = requireResourceResearch;
    }

/**
* Loads markets from database or creates default ones if empty.
*/
public void initialize() throws SQLException {
    List<MarketResource> list = dao.loadAllMarkets();
    if (list.isEmpty()) {
        registerDefaults();
    } else {
        for (MarketResource res : list) {
            markets.put(res.getResourceId(), res);
            generateInitialCandles(res);
        }
    }
    CustomMarketLoader.loadCustomItems(this);
    omsManager.loadPositions(dao.loadAllOMSPositions());
    generateDailyContracts();
}

private void registerDefaults() throws SQLException {
    addAndSave(new MarketResource("minecraft:iron_ingot", "Iron Ingot", 12.0, 10000.0, 10000.0, 0.8, 15000.0, 15.0, 0.02, 0.10));
    addAndSave(new MarketResource("minecraft:gold_ingot", "Gold Ingot", 40.0, 5000.0, 5000.0, 0.85, 8000.0, 20.0, 0.02, 0.20));
    addAndSave(new MarketResource("minecraft:diamond", "Diamond", 350.0, 2000.0, 2000.0, 0.9, 3500.0, 40.0, 0.02, 1.0));
    addAndSave(new MarketResource("minecraft:netherite_ingot", "Netherite Ingot", 4500.0, 250.0, 250.0, 0.95, 500.0, 200.0, 0.02, 10.0));
    addAndSave(new MarketResource("minecraft:copper_ingot", "Copper Ingot", 8.0, 20000.0, 20000.0, 0.75, 30000.0, 8.0, 0.02, 0.05));
    addAndSave(new MarketResource("minecraft:redstone", "Redstone Dust", 18.0, 15000.0, 15000.0, 0.8, 25000.0, 10.0, 0.02, 0.05));
    addAndSave(new MarketResource("minecraft:emerald", "Emerald", 50.0, 8000.0, 8000.0, 0.85, 12000.0, 25.0, 0.02, 0.15));
    addAndSave(new MarketResource("minecraft:lapis_lazuli", "Lapis Lazuli", 15.0, 12000.0, 12000.0, 0.8, 18000.0, 10.0, 0.02, 0.05));
}

public synchronized void registerCustomResource(MarketResource res) throws SQLException {
    MarketResource existing = markets.get(res.getResourceId());
    if (existing == null) {
        addAndSave(res);
    } else {
        existing.setDisplayName(res.getDisplayName());
        existing.setBasePrice(res.getBasePrice());
        existing.setTargetReserve(res.getTargetReserve());
        existing.setMaxReserve(res.getMaxReserve());
        existing.setElasticity(res.getElasticity());
        existing.setDisposalAlpha(res.getDisposalAlpha());
        existing.setFeeRate(res.getFeeRate());
        existing.setMinPriceFloor(res.getMinPriceFloor());
        dao.upsertMarket(existing);
    }
}

public void addAndSave(MarketResource res) throws SQLException {
    markets.put(res.getResourceId(), res);
    dao.upsertMarket(res);
    generateInitialCandles(res);
}

public void generateInitialCandles(MarketResource res) throws SQLException {
if (!dao.getRecentCandles(res.getResourceId(), "1d", 1).isEmpty()) {
return;
}
long now = System.currentTimeMillis();
long step = 30000;
double p = res.getBasePrice();
java.util.Random rnd = new java.util.Random(res.getResourceId().hashCode());
for (int i = 16; i >= 0; i--) {
double change = (rnd.nextDouble() - 0.48) * (p * 0.04);
double open = p;
double close = p + change;
double high = Math.max(open, close) + rnd.nextDouble() * (p * 0.02);
double low = Math.min(open, close) - rnd.nextDouble() * (p * 0.02);
double volume = 100 + rnd.nextInt(500);
p = close;
dao.recordCandle(res.getResourceId(), "1d", new Candle(now - (i * step), open, high, low, close, volume));
}
}


    public MarketDAO getDao() {
        return dao;
    }

    public OMSManager getOmsManager() {
        return omsManager;
    }

    public MarketResource getResource(String resourceId) {
        return markets.get(resourceId);
    }

    public Collection<MarketResource> getAllResources() {
        return markets.values();
    }

    /**
     * Records a sequence of 4 smooth progressive candles transitioning the price from oldPrice to newPrice.
     * Used for daily economic fluctuations.
     * Prevents overwriting real trade history and avoids creating rogue future timestamps.
     */
    public void recordDailyTransitionCandles(MarketResource res, double oldPrice, double newPrice) throws SQLException {
        if (dao == null || res == null) return;
        long now = System.currentTimeMillis();

        // 1. Purge any corrupted future candles from prior sessions (> 10s into future)
        dao.deleteFutureCandles(res.getResourceId(), "1d", now + 10000L);

        // 2. Fetch the actual latest candle
        List<Candle> recent = dao.getRecentCandles(res.getResourceId(), "1d", 1);
        Candle lastCandle = recent.isEmpty() ? null : recent.get(recent.size() - 1);
        double startPrice = oldPrice;

        double delta = newPrice - startPrice;
        double[] progress = { 0.28, 0.58, 0.85, 1.00 };
        Random random = new Random();
        double currentOpen = startPrice;

        long lastTime = (lastCandle != null) ? lastCandle.getTimestamp() : (now - 120000L);
        boolean hasRoomBehind = (now - lastTime) >= 120000L;

        for (int i = 0; i < 4; i++) {
            long timestamp = hasRoomBehind
                    ? (now - (3 - i) * 30000L)
                    : (lastTime + (i + 1) * 1000L);

            double targetClose = startPrice + delta * progress[i];
            double currentClose;
            if (i < 3) {
                double noise = (random.nextDouble() - 0.5) * (Math.abs(delta) * 0.04);
                currentClose = targetClose + noise;
                if (delta > 0 && currentClose <= currentOpen) {
                    currentClose = currentOpen + Math.abs(delta) * 0.05;
                } else if (delta < 0 && currentClose >= currentOpen) {
                    currentClose = currentOpen - Math.abs(delta) * 0.05;
                }
            } else {
                currentClose = newPrice;
            }

            double bodyMax = Math.max(currentOpen, currentClose);
            double bodyMin = Math.min(currentOpen, currentClose);
            double wick = Math.max(startPrice * 0.003, Math.abs(currentClose - currentOpen) * 0.12 + (random.nextDouble() * startPrice * 0.004));
            double high = bodyMax + wick;
            double low = Math.max(res.getMinPriceFloor(), bodyMin - wick);
            double volume = 200.0 + random.nextInt(300);

            Candle candle = new Candle(timestamp, currentOpen, high, low, currentClose, volume);
            dao.recordCandle(res.getResourceId(), "1d", candle);
            currentOpen = currentClose;
        }
    }

    /**
     * Records 1 sharp, dramatic candle representing sudden news or market event shock.
     * Features an impulsive body, volatility wicks, and a high volume surge.
     */
    public void recordMarketEventCandle(MarketResource res, double oldPrice, double newPrice) throws SQLException {
        if (dao == null || res == null) return;
        long now = System.currentTimeMillis();

        // 1. Purge any corrupted future candles from prior sessions (> 10s into future)
        dao.deleteFutureCandles(res.getResourceId(), "1d", now + 10000L);

        List<Candle> recent = dao.getRecentCandles(res.getResourceId(), "1d", 1);
        Candle lastCandle = recent.isEmpty() ? null : recent.get(recent.size() - 1);
        double startPrice = oldPrice;

        long timestamp = (lastCandle != null) ? (lastCandle.getTimestamp() + 1000L) : now;

        double delta = newPrice - startPrice;
        Random random = new Random();
        double high;
        double low;

        if (delta > 0) {
            high = newPrice + Math.abs(delta) * 0.15;
            low = Math.max(res.getMinPriceFloor(), startPrice - Math.abs(delta) * 0.05);
        } else if (delta < 0) {
            high = startPrice + Math.abs(delta) * 0.05;
            low = Math.max(res.getMinPriceFloor(), newPrice - Math.abs(delta) * 0.15);
        } else {
            high = startPrice * 1.01;
            low = Math.max(res.getMinPriceFloor(), startPrice * 0.99);
        }

        double volume = 1500.0 + random.nextInt(1000);

        Candle candle = new Candle(timestamp, startPrice, high, low, newPrice, volume);
        dao.recordCandle(res.getResourceId(), "1d", candle);
    }

/**
* Executes a player market purchase.
*
* @return Result status and total cost in CBX
*/
public synchronized MarketTransactionResult executeBuy(UUID playerUuid, String playerName, String resourceId, int amount) throws SQLException {
if (amount <= 0) {
return new MarketTransactionResult(false, "Amount must be positive", 0);
}
MarketResource res = markets.get(resourceId);
if (res == null) {
return new MarketTransactionResult(false, "Resource not found", 0);
}
if (!isResourceUnlockedForPlayer(playerUuid, resourceId)) {
return new MarketTransactionResult(false, "Resource is locked! Research a sample (1 pc) first.", 0);
}
if (res.getCurrentStock() < amount) {
return new MarketTransactionResult(false, "Insufficient exchange reserves", 0);
}

PlayerAccount account = dao.getAccount(playerUuid, playerName);
double totalCost = MarketEngine.calculateTotalBuyCost(amount, res);

if (!account.withdraw(totalCost)) {
return new MarketTransactionResult(false, "Insufficient CBX balance (Required: " + totalCost + ")", 0);
}

// Adjust stock
res.setCurrentStock(res.getCurrentStock() - amount);
dao.upsertMarket(res);

// Add transaction rep points
        account.addRepPoints((int) (totalCost / 10.0));
        dao.saveAccount(account);



double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
dao.recordOrder(playerUuid, resourceId, "BUY", amount, spotPrice, totalCost * res.getFeeRate(), 0.0);
dao.recordTradePrice(resourceId, "1d", spotPrice, amount, 30000L);

return new MarketTransactionResult(true, "Purchase successful", totalCost);
}

/**
* Executes an automated market purchase (used by Purchase Dock).
* Enforces a Stop-High price guard: fails if current unit buy price exceeds maxBuyPrice.
*
* @return Result status and total cost in CBX
*/
public synchronized MarketTransactionResult executeAutomatedPurchase(UUID playerUuid, String playerName, String resourceId, int amount, double maxBuyPrice) throws SQLException {
if (amount <= 0) {
return new MarketTransactionResult(false, "Amount must be positive", 0);
}
MarketResource res = markets.get(resourceId);
if (res == null) {
return new MarketTransactionResult(false, "Resource not found", 0);
}
if (res.getCurrentStock() < amount) {
return new MarketTransactionResult(false, "Insufficient exchange reserves", 0);
}

double unitBuyPrice = MarketEngine.calculateBuyPrice(res.getCurrentStock(), res);
if (unitBuyPrice > maxBuyPrice) {
return new MarketTransactionResult(false, "Price " + unitBuyPrice + " exceeds max buy limit " + maxBuyPrice, 0);
}

PlayerAccount account = dao.getAccount(playerUuid, playerName);
double totalCost = MarketEngine.calculateTotalBuyCost(amount, res);

if (!account.withdraw(totalCost)) {
return new MarketTransactionResult(false, "Insufficient CBX balance (Required: " + totalCost + ")", 0);
}

// Adjust stock
res.setCurrentStock(res.getCurrentStock() - amount);
dao.upsertMarket(res);

// Add transaction rep points
account.addRepPoints(Math.max(1, (int) (totalCost / 10.0)));
dao.saveAccount(account);

double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
dao.recordOrder(playerUuid, resourceId, "BUY_AUTO", amount, spotPrice, totalCost * res.getFeeRate(), 0.0);
dao.recordTradePrice(resourceId, "1d", spotPrice, amount, 30000L);

return new MarketTransactionResult(true, "Automated purchase successful", totalCost);
}

/**
* Executes a player market sale.
* Supports negative pricing: if payout is negative, player balance is debited for disposal.
*/
public synchronized MarketTransactionResult executeSell(UUID playerUuid, String playerName, String resourceId, int amount) throws SQLException {
if (amount <= 0) {
return new MarketTransactionResult(false, "Amount must be positive", 0);
}
MarketResource res = markets.get(resourceId);
if (res == null) {
return new MarketTransactionResult(false, "Resource not found", 0);
}

PlayerAccount account = dao.getAccount(playerUuid, playerName);
double totalPayout = MarketEngine.calculateTotalSellPayout(amount, res);

if (totalPayout < 0) {
// Negative price! Player must pay waste disposal fee
double feeToPay = Math.abs(totalPayout);
if (!account.withdraw(feeToPay)) {
return new MarketTransactionResult(false, "Negative price! Insufficient CBX to pay disposal fee: " + feeToPay, totalPayout);
}
} else {
account.deposit(totalPayout);
}

// Adjust stock
res.setCurrentStock(res.getCurrentStock() + amount);
dao.upsertMarket(res);

// Add rep points
account.addRepPoints(Math.max(1, (int) (Math.abs(totalPayout) / 10.0)));
dao.saveAccount(account);

double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
double disposal = MarketEngine.calculateDisposalFee(res.getCurrentStock(), res);
dao.recordOrder(playerUuid, resourceId, "SELL", amount, spotPrice, res.getFeeRate(), disposal);
dao.recordTradePrice(resourceId, "1d", spotPrice, amount, 30000L);

return new MarketTransactionResult(true, totalPayout >= 0 ? "Sale successful" : "Disposal fee charged", totalPayout);
}

    public synchronized MarketTransactionResult executeAutomatedPurchase(UUID playerUuid, String playerName, UUID companyId, String resourceId, int amount, double maxBuyPrice) throws SQLException {
        if (companyId == null) {
            return executeAutomatedPurchase(playerUuid, playerName, resourceId, amount, maxBuyPrice);
        }
        if (amount <= 0) {
            return new MarketTransactionResult(false, "Amount must be positive", 0);
        }
        MarketResource res = markets.get(resourceId);
        if (res == null) {
            return new MarketTransactionResult(false, "Resource not found", 0);
        }
        if (res.getCurrentStock() < amount) {
            return new MarketTransactionResult(false, "Insufficient exchange reserves", 0);
        }

        double unitBuyPrice = MarketEngine.calculateBuyPrice(res.getCurrentStock(), res);
        if (unitBuyPrice > maxBuyPrice) {
            return new MarketTransactionResult(false, "Price " + unitBuyPrice + " exceeds max buy limit " + maxBuyPrice, 0);
        }

        CompanyRecord comp = dao.getCompany(companyId.toString());
        if (comp == null) {
            return new MarketTransactionResult(false, "Company not found", 0);
        }

        double totalCost = MarketEngine.calculateTotalBuyCost(amount, res);
        if (!comp.withdraw(totalCost)) {
            return new MarketTransactionResult(false, "Insufficient corporate CBX balance (Required: " + totalCost + ")", 0);
        }

        dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx());
        dao.recordCompanyLedger(comp.getCompanyId(), playerUuid, playerName, "DOCK_EXPENSE", totalCost, "Automated dock purchase: " + amount + "x " + res.getDisplayName());

        // Adjust stock
        res.setCurrentStock(res.getCurrentStock() - amount);
        dao.upsertMarket(res);

        double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
        dao.recordOrder(playerUuid, resourceId, "BUY_AUTO", amount, spotPrice, totalCost * res.getFeeRate(), 0.0);
        dao.recordTradePrice(resourceId, "1d", spotPrice, amount, 30000L);

        return new MarketTransactionResult(true, "Automated purchase successful (Corporate)", totalCost);
    }

    public synchronized MarketTransactionResult executeSell(UUID playerUuid, String playerName, UUID companyId, String resourceId, int amount) throws SQLException {
        if (companyId == null) {
            return executeSell(playerUuid, playerName, resourceId, amount);
        }
        if (amount <= 0) {
            return new MarketTransactionResult(false, "Amount must be positive", 0);
        }
        MarketResource res = markets.get(resourceId);
        if (res == null) {
            return new MarketTransactionResult(false, "Resource not found", 0);
        }

        CompanyRecord comp = dao.getCompany(companyId.toString());
        if (comp == null) {
            return new MarketTransactionResult(false, "Company not found", 0);
        }

        double totalPayout = MarketEngine.calculateTotalSellPayout(amount, res);
        if (totalPayout < 0) {
            double feeToPay = Math.abs(totalPayout);
            if (!comp.withdraw(feeToPay)) {
                return new MarketTransactionResult(false, "Negative price! Insufficient corporate CBX to pay disposal fee: " + feeToPay, totalPayout);
            }
            dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx());
            dao.recordCompanyLedger(comp.getCompanyId(), playerUuid, playerName, "DOCK_EXPENSE", feeToPay, "Disposal fee for " + amount + "x " + res.getDisplayName());
        } else {
            comp.deposit(totalPayout);
            dao.updateCompanyBalance(comp.getCompanyId(), comp.getBalanceCbx());
            dao.recordCompanyLedger(comp.getCompanyId(), playerUuid, playerName, "DOCK_REVENUE", totalPayout, "Trade dock automated sale: " + amount + "x " + res.getDisplayName());
        }

        // Adjust stock
        res.setCurrentStock(res.getCurrentStock() + amount);
        dao.upsertMarket(res);

        double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
        double disposal = MarketEngine.calculateDisposalFee(res.getCurrentStock(), res);
        dao.recordOrder(playerUuid, resourceId, "SELL", amount, spotPrice, res.getFeeRate(), disposal);
        dao.recordTradePrice(resourceId, "1d", spotPrice, amount, 30000L);

        return new MarketTransactionResult(true, totalPayout >= 0 ? "Corporate sale successful" : "Disposal fee charged", totalPayout);
    }


// --- OMS DERIVATIVE METHODS ---

public synchronized MarketTransactionResult executeOpenOMSPosition(UUID playerUuid, String playerName, String resourceId, double cbxAmount) throws SQLException {
if (cbxAmount <= 0) {
return new MarketTransactionResult(false, "Investment amount must be positive", 0);
}
MarketResource res = markets.get(resourceId);
if (res == null) {
return new MarketTransactionResult(false, "Resource not found", 0);
}
if (!isResourceUnlockedForPlayer(playerUuid, resourceId)) {
return new MarketTransactionResult(false, "Resource is locked! Research a sample (1 pc) first.", 0);
}
PlayerAccount account = dao.getAccount(playerUuid, playerName);
if (!account.withdraw(cbxAmount)) {
return new MarketTransactionResult(false, "Insufficient CBX balance", 0);
}

OMSPosition position = omsManager.openPosition(playerUuid, res, cbxAmount);
dao.saveOMSPosition(position);

account.addRepPoints((int) (cbxAmount / 10.0));
dao.saveAccount(account);

double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
dao.recordOrder(playerUuid, resourceId, "OPEN_OMS", (int) position.getAmountUnits(), spotPrice, cbxAmount * 0.005, 0.0);

return new MarketTransactionResult(true, "OMS Position opened successfully", cbxAmount);
}

public synchronized MarketTransactionResult executeCloseOMSPosition(UUID playerUuid, String playerName, String positionId, double unitsToClose) throws SQLException {
List<OMSPosition> positions = omsManager.getPositions(playerUuid);
OMSPosition target = null;
for (OMSPosition p : positions) {
if (p.getPositionId().equals(positionId)) {
target = p;
break;
}
}
if (target == null) {
return new MarketTransactionResult(false, "OMS Position not found", 0);
}
        if (unitsToClose <= 0.0 || unitsToClose >= target.getAmountUnits() - 0.005) {
            unitsToClose = target.getAmountUnits();
        } else if (unitsToClose > target.getAmountUnits() + 0.005) {
            return new MarketTransactionResult(false, "Invalid units to close", 0);
        }
        unitsToClose = Math.min(unitsToClose, target.getAmountUnits());

        MarketResource res = markets.get(target.getResourceId());
        if (res == null) {
            return new MarketTransactionResult(false, "Underlying resource not found", 0);
        }

        double payout = omsManager.closePosition(target, unitsToClose, res);
        PlayerAccount account = dao.getAccount(playerUuid, playerName);
        account.deposit(payout);
        account.addRepPoints((int) (payout / 10.0));
        dao.saveAccount(account);

        if (target.getAmountUnits() <= 0.001) {
            dao.deleteOMSPosition(positionId);
        } else {
            dao.saveOMSPosition(target);
        }

        double spotPrice = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
        dao.recordOrder(playerUuid, target.getResourceId(), "CLOSE_OMS", (int) unitsToClose, spotPrice, payout * 0.005, 0.0);

        return new MarketTransactionResult(true, "OMS Position closed: +" + payout + " CBX", payout);
    }

    public synchronized void applyDailyCarryFee(double rate) throws SQLException {
        omsManager.applyCarryFee(rate);
        for (OMSPosition pos : dao.loadAllOMSPositions()) {
            double deducted = pos.getAmountUnits() * rate;
            pos.setAmountUnits(Math.max(0.0, pos.getAmountUnits() - deducted));
            if (pos.getAmountUnits() <= 0.001) {
                dao.deleteOMSPosition(pos.getPositionId());
            } else {
                dao.saveOMSPosition(pos);
            }
        }
    }

// --- LIMIT ORDERS METHODS ---

public synchronized MarketTransactionResult placeLimitOrder(UUID playerUuid, String playerName, String resourceId, String orderType, int amount, double limitPrice) throws SQLException {
if (amount <= 0 || limitPrice <= 0 || Double.isNaN(limitPrice) || Double.isInfinite(limitPrice)) {
return new MarketTransactionResult(false, "Amount and price must be positive and valid", 0);
}
MarketResource res = markets.get(resourceId);
if (res == null) {
return new MarketTransactionResult(false, "Resource not found", 0);
}
if (LimitOrder.TYPE_BUY.equalsIgnoreCase(orderType) && !isResourceUnlockedForPlayer(playerUuid, resourceId)) {
return new MarketTransactionResult(false, "Resource is locked! Research a sample (1 pc) first.", 0);
}
PlayerAccount account = dao.getAccount(playerUuid, playerName);
double reservedCbx = 0.0;
if (LimitOrder.TYPE_BUY.equalsIgnoreCase(orderType)) {
reservedCbx = amount * limitPrice;
if (!account.withdraw(reservedCbx)) {
return new MarketTransactionResult(false, "Insufficient CBX to reserve order (Required: " + reservedCbx + ")", 0);
}
dao.saveAccount(account);
}

String orderId = UUID.randomUUID().toString();
LimitOrder order = new LimitOrder(
    orderId, playerUuid, playerName, resourceId,
    orderType.toUpperCase(), amount, limitPrice, reservedCbx,
    System.currentTimeMillis(), LimitOrder.STATUS_PENDING
);
dao.saveLimitOrder(order);

// Check if order can be immediately executed
processPendingLimitOrders();

return new MarketTransactionResult(true, "Limit order placed: " + orderType + " " + amount + " @ " + limitPrice + " CBX", reservedCbx);
}

public synchronized MarketTransactionResult cancelLimitOrder(UUID playerUuid, String orderId) throws SQLException {
    List<LimitOrder> orders = dao.getPlayerLimitOrders(playerUuid);
    LimitOrder target = null;
    for (LimitOrder o : orders) {
        if (o.getOrderId().equals(orderId)) {
            target = o;
            break;
        }
    }
    if (target == null || !target.isPending()) {
        return new MarketTransactionResult(false, "Active order not found", 0);
    }

    if (LimitOrder.TYPE_BUY.equalsIgnoreCase(target.getOrderType()) && target.getReservedCbx() > 0) {
        PlayerAccount account = dao.getAccount(playerUuid, target.getPlayerName());
        account.deposit(target.getReservedCbx());
        dao.saveAccount(account);
    }

    dao.updateLimitOrderStatus(orderId, LimitOrder.STATUS_CANCELLED);
    return new MarketTransactionResult(true, "Order cancelled. Refunded: " + target.getReservedCbx() + " CBX", target.getReservedCbx());
}

public synchronized void processPendingLimitOrders() throws SQLException {
    List<LimitOrder> pending = dao.getActiveLimitOrders();
    for (LimitOrder order : pending) {
        MarketResource res = markets.get(order.getResourceId());
        if (res == null) continue;

        if (LimitOrder.TYPE_BUY.equalsIgnoreCase(order.getOrderType())) {
            double currentBuyPrice = MarketEngine.calculateBuyPrice(res.getCurrentStock(), res);
            if (currentBuyPrice <= order.getLimitPrice() && res.getCurrentStock() >= order.getAmount()) {
                double totalCost = MarketEngine.calculateTotalBuyCost(order.getAmount(), res);
                res.setCurrentStock(res.getCurrentStock() - order.getAmount());
                dao.upsertMarket(res);

                double refund = order.getReservedCbx() - totalCost;
                PlayerAccount account = dao.getAccount(order.getPlayerUuid(), order.getPlayerName());
                if (refund > 0) {
                    account.deposit(refund);
                }
                account.addRepPoints((int) (totalCost / 10.0));
                dao.saveAccount(account);

                dao.updateLimitOrderStatus(order.getOrderId(), LimitOrder.STATUS_FILLED);
                double spot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                dao.recordOrder(order.getPlayerUuid(), order.getResourceId(), "LIMIT_BUY_FILLED", order.getAmount(), spot, totalCost * res.getFeeRate(), 0.0);
                dao.recordTradePrice(order.getResourceId(), "1d", spot, order.getAmount(), 30000L);

                // Deliver bought items or save to unclaimed deliveries for offline claim
                boolean delivered = false;
                if (itemDeliveryHandler != null) {
                    try {
                        delivered = itemDeliveryHandler.deliver(order.getPlayerUuid(), order.getResourceId(), order.getAmount(), res.getDisplayName());
                    } catch (Exception ignored) {}
                }
                if (!delivered) {
                    dao.saveUnclaimedDelivery(UUID.randomUUID().toString(), order.getPlayerUuid(), order.getResourceId(), order.getAmount(), System.currentTimeMillis());
                }
            }
        } else if (LimitOrder.TYPE_SELL.equalsIgnoreCase(order.getOrderType())) {
            double currentSellPrice = MarketEngine.calculateSellPrice(res.getCurrentStock(), res);
            if (currentSellPrice >= order.getLimitPrice()) {
                double totalPayout = MarketEngine.calculateTotalSellPayout(order.getAmount(), res);
                if (totalPayout > 0) {
                    PlayerAccount account = dao.getAccount(order.getPlayerUuid(), order.getPlayerName());
                    account.deposit(totalPayout);
                    account.addRepPoints((int) (totalPayout / 10.0));
                    dao.saveAccount(account);

                    res.setCurrentStock(res.getCurrentStock() + order.getAmount());
                    dao.upsertMarket(res);

                    dao.updateLimitOrderStatus(order.getOrderId(), LimitOrder.STATUS_FILLED);
                    double spot = MarketEngine.calculateSpotPrice(res.getCurrentStock(), res);
                    dao.recordOrder(order.getPlayerUuid(), order.getResourceId(), "LIMIT_SELL_FILLED", order.getAmount(), spot, res.getFeeRate(), 0.0);
                    dao.recordTradePrice(order.getResourceId(), "1d", spot, order.getAmount(), 30000L);
                }
            }
        }
    }
}

// --- DELIVERY CONTRACTS METHODS ---

    private final List<com.ammora.mod.datapack.ContractTemplate> contractTemplates = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void setContractTemplates(List<com.ammora.mod.datapack.ContractTemplate> templates) {
        this.contractTemplates.clear();
        this.contractTemplates.addAll(templates);
    }

    public List<com.ammora.mod.datapack.ContractTemplate> getContractTemplates() {
        return Collections.unmodifiableList(contractTemplates);
    }

    public synchronized void generateDailyContracts() throws SQLException {
        List<DeliveryContract> existing = dao.getAvailableAndPlayerContracts(null);
        java.util.Set<String> openResourceIds = new java.util.HashSet<>();
        if (existing != null) {
            for (DeliveryContract c : existing) {
                if (c.isOpen()) {
                    openResourceIds.add(c.getResourceId());
                }
            }
        }

        if (!contractTemplates.isEmpty()) {
            java.util.Random rnd = new java.util.Random();
            for (var tmpl : contractTemplates) {
                if (!openResourceIds.contains(tmpl.resourceId())) {
                    int amt = tmpl.minAmount();
                    if (tmpl.maxAmount() > tmpl.minAmount()) {
                        amt = tmpl.minAmount() + rnd.nextInt(tmpl.maxAmount() - tmpl.minAmount() + 1);
                    }
                    String contractId = "contract_" + tmpl.id() + "_" + System.currentTimeMillis();
                    String title = tmpl.getDisplayTitle();
                    dao.saveContract(new DeliveryContract(
                            contractId,
                            title,
                            tmpl.resourceId(),
                            amt,
                            0,
                            tmpl.pricePerUnit(),
                            tmpl.collateralCbx(),
                            null,
                            null,
                            0L,
                            tmpl.requiredReputation(),
                            DeliveryContract.STATUS_OPEN
                    ));
                    openResourceIds.add(tmpl.resourceId());
                }
            }
        } else {
            // Default fallback if no datapack contracts are loaded yet
            if (!openResourceIds.contains("minecraft:iron_ingot")) {
                dao.saveContract(new DeliveryContract(
                        "contract_iron_" + System.currentTimeMillis(),
                        "State Order: Iron Ingot Batch",
                        "minecraft:iron_ingot",
                        64, 0, 15.0, 100.0, null, null, 0L, 150, DeliveryContract.STATUS_OPEN
                ));
            }
            if (!openResourceIds.contains("minecraft:gold_ingot")) {
                dao.saveContract(new DeliveryContract(
                        "contract_gold_" + (System.currentTimeMillis() + 1),
                        "State Order: Gold Reserve",
                        "minecraft:gold_ingot",
                        32, 0, 50.0, 200.0, null, null, 0L, 250, DeliveryContract.STATUS_OPEN
                ));
            }
            if (!openResourceIds.contains("minecraft:diamond")) {
                dao.saveContract(new DeliveryContract(
                        "contract_dia_" + (System.currentTimeMillis() + 2),
                        "State Order: Strategic Diamonds",
                        "minecraft:diamond",
                        8, 0, 420.0, 350.0, null, null, 0L, 500, DeliveryContract.STATUS_OPEN
                ));
            }
        }
    }

public synchronized MarketTransactionResult acceptContract(UUID playerUuid, String playerName, String contractId, long currentTick) throws SQLException {
List<DeliveryContract> contracts = dao.getAvailableAndPlayerContracts(playerUuid);
DeliveryContract target = null;
for (DeliveryContract c : contracts) {
if (c.getContractId().equals(contractId)) {
target = c;
break;
}
}
if (target == null || !target.isOpen()) {
return new MarketTransactionResult(false, "Contract not available", 0);
}

PlayerAccount account = dao.getAccount(playerUuid, playerName);
if (!account.withdraw(target.getCollateralCbx())) {
return new MarketTransactionResult(false, "Insufficient CBX for collateral (Required: " + target.getCollateralCbx() + ")", 0);
}
dao.saveAccount(account);

target.setAcceptedPlayerUuid(playerUuid);
target.setAcceptedPlayerName(playerName);
target.setStatus(DeliveryContract.STATUS_ACTIVE);
target.setDeadlineTick(currentTick + 72000L); // 3 Minecraft days = 72,000 ticks
dao.saveContract(target);

return new MarketTransactionResult(true, "Contract accepted! Collateral deposited: " + target.getCollateralCbx() + " CBX", target.getCollateralCbx());
}

public synchronized MarketTransactionResult deliverContractItems(UUID playerUuid, String playerName, String contractId, int amount) throws SQLException {
if (amount <= 0) {
return new MarketTransactionResult(false, "Delivery amount must be positive", 0);
}
List<DeliveryContract> contracts = dao.getAvailableAndPlayerContracts(playerUuid);
DeliveryContract target = null;
for (DeliveryContract c : contracts) {
if (c.getContractId().equals(contractId)) {
target = c;
break;
}
}
if (target == null || !target.isActive() || !playerUuid.equals(target.getAcceptedPlayerUuid())) {
return new MarketTransactionResult(false, "Active contract not found", 0);
}

int remainingNeeded = target.getTargetAmount() - target.getDeliveredAmount();
int actualDelivered = Math.min(amount, remainingNeeded);
target.setDeliveredAmount(target.getDeliveredAmount() + actualDelivered);

if (target.getDeliveredAmount() >= target.getTargetAmount()) {
// Completed!
target.setStatus(DeliveryContract.STATUS_COMPLETED);
dao.saveContract(target);

double totalPayout = target.getTotalPayout() + target.getCollateralCbx();
PlayerAccount account = dao.getAccount(playerUuid, playerName);
account.deposit(totalPayout);
account.addRepPoints(target.getRewardRep());
dao.saveAccount(account);

// Add delivered goods to exchange stock
MarketResource res = markets.get(target.getResourceId());
if (res != null) {
res.setCurrentStock(res.getCurrentStock() + target.getTargetAmount());
dao.upsertMarket(res);
}

return new MarketTransactionResult(true, "Contract fulfilled! Payout: " + totalPayout + " CBX (+" + target.getRewardRep() + " REP)", totalPayout);
} else {
dao.saveContract(target);
        return new MarketTransactionResult(true, "Delivered " + actualDelivered + "/" + target.getTargetAmount() + " items.", actualDelivered);
    }
}

public synchronized void processContractTicks(long currentTick) throws SQLException {
    List<DeliveryContract> active = dao.getAllActiveContracts();
    for (DeliveryContract c : active) {
        if (currentTick > c.getDeadlineTick()) {
            c.setStatus(DeliveryContract.STATUS_EXPIRED);
            dao.saveContract(c);
        }
    }
}


    public synchronized MarketTransactionResult executeP2PTransfer(UUID fromUuid, String fromName, UUID toUuid, String toName, double amount) throws SQLException {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return new MarketTransactionResult(false, "Transfer amount must be positive", 0);
        }
        if (fromUuid.equals(toUuid)) {
            return new MarketTransactionResult(false, "Cannot transfer funds to yourself", 0);
        }
        PlayerAccount fromAcc = dao.getAccount(fromUuid, fromName);
        if (!fromAcc.withdraw(amount)) {
            return new MarketTransactionResult(false, "Insufficient CBX balance (Required: " + MarketEngine.round2(amount) + " CBX)", 0);
        }
        PlayerAccount toAcc = dao.getAccount(toUuid, toName);
        toAcc.deposit(amount);

        dao.saveAccount(fromAcc);
        dao.saveAccount(toAcc);

        P2PTransfer transfer = new P2PTransfer(
                UUID.randomUUID().toString(),
                fromUuid, fromName,
                toUuid, toName,
                amount,
                System.currentTimeMillis()
        );
        dao.recordP2PTransfer(transfer);

        return new MarketTransactionResult(true, "Transferred " + MarketEngine.round2(amount) + " CBX to " + toName, amount);
    }

    public boolean isResourceUnlockedForPlayer(UUID playerUuid, String resourceId) {
        if (!requireResourceResearch) {
            return true;
        }
        if ("minecraft:iron_ingot".equals(resourceId) || "minecraft:copper_ingot".equals(resourceId)) {
            return true;
        }
        try {
            return dao.isResourceUnlocked(playerUuid, resourceId);
        } catch (SQLException e) {
            return false;
        }
    }

    public Set<String> getUnlockedResourcesForPlayer(UUID playerUuid) {
        Set<String> set = new HashSet<>();
        set.add("minecraft:iron_ingot");
        set.add("minecraft:copper_ingot");
        if (!requireResourceResearch) {
            for (MarketResource res : markets.values()) {
                set.add(res.getResourceId());
            }
            return set;
        }
        try {
            set.addAll(dao.getUnlockedResources(playerUuid));
        } catch (SQLException ignored) {}
        return set;
    }

    public void unlockResourceForPlayer(UUID playerUuid, String resourceId) throws SQLException {
        dao.unlockResource(playerUuid, resourceId, System.currentTimeMillis());
    }

    public record MarketTransactionResult(boolean success, String message, double cbxAmount) {
        @Deprecated
        public double usdtAmount() {
            return cbxAmount;
        }
    }
}
