# CC: Tweaked (ComputerCraft) Lua Integration Guide

Ammora provides native integration with **CC: Tweaked** (ComputerCraft).  
Placing an Advanced Computer adjacent to an **Exchange Terminal** or **Trade Dock** exposes the block as a high-level peripheral in Lua.

---

## Connecting to Peripherals

### Wired Modem or Direct Attachment
In Lua, discover the connected exchange peripheral using:
```lua
-- Automatically finds the first connected exchange terminal or dock
local exchange = peripheral.find("exchange_terminal") or peripheral.find("exchange_dock")

if not exchange then
    error("Exchange peripheral not found! Check modem or block adjacency.")
end
```

Peripheral type names:
- `"exchange_terminal"` — When connected to an Exchange Terminal.
- `"exchange_dock"` — When connected to a Trade Dock.

---

## Lua API Method Reference

### 1. `exchange.getPrice(itemId)`
Returns the current spot price, buy price, sell price, and disposal fee for a commodity.

- **Parameters:**
  - `itemId` *(string)*: Registry item identifier (e.g., `"minecraft:iron_ingot"`).
- **Returns:** *(table)*
  ```lua
  {
      resourceId  = "minecraft:iron_ingot",
      displayName = "Iron Ingot",
      spotPrice   = 12.45,   -- Spot equilibrium price in CBX
      buyPrice    = 12.70,   -- Unit buy price for players (including commission)
      sellPrice   = 12.20,   -- Unit sell price (can be negative if overflowing!)
      disposalFee = 0.00     -- Waste disposal fee in CBX
  }
  ```

---

### 2. `exchange.getStock(itemId)`
Queries current reserve levels and capacity metrics for a commodity.

- **Parameters:**
  - `itemId` *(string)*: Registry item identifier.
- **Returns:** *(table)*
  ```lua
  {
      currentStock  = 8450,
      targetReserve = 10000,
      maxReserve    = 15000,
      fillPercent   = 56.33,  -- Percentage of maxReserve
      status        = "NORMAL" -- "NORMAL", "HIGH", or "OVERFLOW"
  }
  ```

---

### 3. `exchange.getAccount(playerUuid)`
Queries player account metrics including CBX balance and reputation tier.

- **Parameters:**
  - `playerUuid` *(string)*: Valid UUID of the player.
- **Returns:** *(table)*
  ```lua
  {
      balanceCBX    = 2450.80, -- Available funds in CBX
      repPoints     = 1850,    -- Total reputation score
      repLevel      = 3,       -- Trader rank (1=Novice, 2=Trader, 3=Broker, 4=Investor, 5=Whale)
      brokerFeeRate = 0.012    -- Active commission rate (1.2%)
  }
  ```

---

### 4. `exchange.buy(playerUuid, itemId, count)`
Executes an automated market purchase, withdrawing CBX from the specified player account.

- **Parameters:**
  - `playerUuid` *(string)*: Player UUID.
  - `itemId` *(string)*: Registry item identifier.
  - `count` *(number)*: Integer amount to purchase (1–2304).
- **Returns:** *(table)*
  ```lua
  {
      success  = true,
      message  = "Purchase successful",
      cbxSpent = 164.50
  }
  ```

---

### 5. `exchange.sell(playerUuid, itemId, count)`
Executes an automated market sale, crediting CBX into the specified player account.

- **Parameters:**
  - `playerUuid` *(string)*: Player UUID.
  - `itemId` *(string)*: Registry item identifier.
  - `count` *(number)*: Integer amount to sell.
- **Returns:** *(table)*
  ```lua
  {
      success   = true,
      message   = "Sale successful",
      cbxPayout = 120.00
  }
  ```

---

### 6. `exchange.getCandles(itemId, limit)`
Fetches historic OHLCV candlestick data for market charting and technical analysis.

- **Parameters:**
  - `itemId` *(string)*: Registry item identifier.
  - `limit` *(number)*: Maximum candles to fetch (up to 50).
- **Returns:** *(array of tables)*
  ```lua
  {
      {
          timestamp = 1718000000000,
          open      = 12.0,
          high      = 13.5,
          low       = 11.8,
          close     = 13.2,
          volume    = 450.0,
          bullish   = true
      },
      ...
  }
  ```

---

### 7. `exchange.getDailyModifier(itemId)`
Fetches the macroeconomic daily random walk and event modifiers.

- **Returns:** *(table)*
  ```lua
  {
      dailyModifier = 0.045,  -- +4.5%
      percent       = 4.5,
      eventModifier = 0.0
  }
  ```

---

### 8. `exchange.getActiveEvent()`
Checks if a global economic event is currently active on the server.

- **Returns:** *(table)*
  ```lua
  {
      active           = true,
      id               = "gold_rush",
      title            = "Gold Rush",
      description      = "Ancient treasury uncovered!",
      affectedResource = "minecraft:gold_ingot",
      priceMultiplier  = 0.70,
      remainingDays    = 2
  }
  ```

---

### 9. `exchange.setStopLoss(price)` *(Trade Dock Only)*
Configures the minimum accepted selling price on an automated Trade Dock. If the market sell price drops below this value, item intake immediately pauses.

---

## Example: Automated Arbitrage / Buy Bot

```lua
-- Simple automated buy bot
local exchange = peripheral.find("exchange_terminal")
local PLAYER_UUID = "YOUR_PLAYER_UUID_HERE"
local ITEM_ID = "minecraft:iron_ingot"
local TARGET_BUY_PRICE = 9.50 -- Buy whenever iron is below 9.50 CBX

while true do
    local price = exchange.getPrice(ITEM_ID)
    local account = exchange.getAccount(PLAYER_UUID)
    
    print(string.format("[%s] %s Spot: %.2f CBX | Balance: %.2f CBX", 
        os.date("%H:%M:%S"), price.displayName, price.spotPrice, account.balanceCBX))
        
    if price.buyPrice <= TARGET_BUY_PRICE and account.balanceCBX >= 200 then
        print("Price target hit! Buying 16 items...")
        local res = exchange.buy(PLAYER_UUID, ITEM_ID, 16)
        if res.success then
            print(string.format("Bought 16x for %.2f CBX", res.cbxSpent))
        else
            print("Buy failed: " .. tostring(res.message))
        end
    end
    
    sleep(10) -- Poll every 10 seconds
end
```
