# Ammora AI Agent Setup & Context Prompt

> Official AI Agent & LLM instructions for working with the **Ammora** financial mod codebase, APIs, datapacks, and automation scripts.

---

## Agent System Instructions

When assisting users with the **Ammora** ecosystem (Minecraft NeoForge 1.21.1), you are an expert Minecraft mod developer, financial systems engineer, and Lua automation specialist.

Adhere to the following rules:
1. **Never hardcode strings or text**: Always use translation keys (`AmmoraLang.translatable(...)` or `AmmoraLang.message(...)`) and maintain 100% parity between `en_us.json` and `ru_ru.json`.
2. **Currency standard**: The native in-game currency is **CBX** (represented as `double` with 2 decimal places in UI).
3. **Database concurrency**: All persistent state is managed via SQLite in WAL mode. Never execute long-running or blocking SQL operations on the main Minecraft server thread; use `MarketDAO` helper methods and atomic transaction overloads.
4. **Compatibility**: Target **Minecraft 1.21.1**, **NeoForge 21.1.176+**, **Java 21**, with optional integrations for **Create 6.0.x** and **CC: Tweaked 1.120.x**.

---

## 1. Core Philosophy & Economic Model

Traditional Minecraft economies suffer from hyperinflation caused by automated iron, gold, and raid farms. Ammora solves this by replacing static server shops with an **Automated Market Maker (Bonding Curve AMM)**.

### 1.1. Pricing Formula
The spot price $P(S)$ for a commodity depends on current exchange reserves $S$:

$$P(S) = P_0 \times \left(\frac{S_{target}}{S}\right)^k$$

- $P_0$ — Base equilibrium price in **CBX**.
- $S_{target}$ — Target inventory reserve (e.g. 10,000 iron ingots).
- $S$ — Current physical inventory on the exchange.
- $k$ — Elasticity coefficient ($0.75 - 0.95$).

### 1.2. Price Slippage
Buying in bulk progressively depletes reserves, increasing the average execution price compared to the initial spot quote. Terminal and Lua orders calculate average prices across the curve.

### 1.3. Disposal Fees & Negative Prices
If physical reserves exceed the maximum capacity threshold $S_{max}$, the unit price turns negative. Selling items to an overflowing warehouse incurs an **ecological waste processing fee** charged in CBX.

### 1.4. Mean Reversion & Surplus Burn
Every 24,000 game ticks (1 in-game day), the exchange consumes **8%** of surplus reserves above $S_{target}$, simulating municipal consumption and driving prices back toward equilibrium.

### 1.5. Trader Reputation Tiers
Trading volume generates Reputation Points (1 REP = 10 CBX in trade volume):
- **Level I (Novice)**: 0–99 REP | 2.0% fee
- **Level II (Trader)**: 100–499 REP | 1.6% fee
- **Level III (Broker)**: 500–1,999 REP | 1.2% fee
- **Level IV (Investor)**: 2,000–9,999 REP | 0.8% fee
- **Level V (Whale)**: 10,000+ REP | **0.5% fee**

### 1.6. Unallocated Metal Accounts (OMS)
Synthetic commodity accounts: players can invest CBX into raw commodities without chest storage, tracking live PnL and closing positions for CBX profit. A 0.05% daily carry fee applies at midnight.

---

## 2. Block Architecture & Player Mechanics

### 2.1. Exchange Terminal (`ammora:exchange_terminal`)
- Candlestick charts with volume bars.
- Instant market buy/sell, limit orders (`BUY_LIMIT`, `SELL_LIMIT`), and futures delivery contracts.
- Redstone comparator output reflecting commodity price bands or inventory levels.

### 2.2. Trade Dock (`ammora:trade_dock`)
- Automatic item import via hoppers, Create belts, and funnels.
- Stop-loss price guard to protect against selling at a loss during market crashes.
- Account billing selector: toggles crediting personal balance vs. **corporate treasury**.

### 2.3. Purchase Dock (`ammora:purchase_dock`)
- Automated item extraction from the exchange into adjacent inventories.
- Stop-high price guard prevents purchasing during unexpected price spikes.
- Account billing selector: funds purchases from personal or **corporate balance**.

### 2.4. Cold Wallet (`ammora:cold_wallet`)
- Handheld NFC biometric balance checker and 30-day transaction audit ledger.
- Wireless P2P player-to-player transfers within 30 blocks.
- Corporate account toggle: allows Owners and Managers to inspect and transfer company funds.

### 2.5. Player Vending Machine (`ammora:player_shop`)
- 10 showcase slots with customizable CBX pricing and capacity upgrades (64 to 1,024 items).
- Optional Satellite Uplink module for global broadcast to the Marketplace Tablet.
- **Corporate Linking**: can be linked to a registered Corporation (`/ammora shop link-company <id>`). Customer payments automatically credit the **corporate treasury directly**.

### 2.6. Marketplace Tablet (`ammora:marketplace_tablet`)
- High-resolution 8-tab interface (`[🛒 Shop]`, `[🔨 Auctions]`, `[📋 Bounties]`, `[📜 Quests]`, `[📦 Deliveries]`, `[🏢 Corporation]`, `[📈 Exchange]`, `[⚙ Settings]`).
- Top account switcher: `[Personal] / [🏢 Corporation]`.
- RFQ Escrow Bounties: post public purchase orders with locked escrow funds.
- Community Quests: custom task and labor bounties.
- Offline Deliveries Buffer: 10-slot safe storage buffer for items received while offline or when inventory was full.

---

## 3. Corporations & Joint Accounts

Players can form corporate entities with shared capital and automated operational routing:
- **Registration**: Performed via the `[🏢 Corporation]` tab on the tablet. Requires a server-configured registration fee (default: 500.0 CBX).
- **Roles**:
  - `OWNER`: Full control, staff invitations, role promotions, budget limits, withdrawals, company dissolution.
  - `MANAGER`: Spend corporate funds up to a daily limit (`daily_limit_cbx`), execute contracts, link vending machines. Cannot invite or kick members.
  - `MEMBER`: Deposit personal funds into corporate treasury, view financial reports and ledger audit.
- **Daily Manager Spending Limit**: Quota resetting every 24 real hours.
- **Audit Ledger**: Immutable transaction history logged in SQLite (`company_ledger`).
- **Automation Integration**: Trade Docks, Purchase Docks, and Player Vending Machines can route all cash flows directly through the corporate treasury.

---

## 4. Live Multiplayer Auction House

Integrated directly into the Marketplace Tablet:
- **Escrow Bidding**: Placing a bid locks CBX in escrow. When outbid, funds are instantly refunded to the bidder's balance.
- **Instant Buyout (`Buyout Price`)**: Sellers can set an optional buyout price. Paying it awards the lot immediately.
- **Anti-Sniping Protection**: Bids placed in the final 60 seconds automatically extend the countdown by +60 seconds.
- **Lot Listing**: Specifiable starting bid, buyout price, and duration (1h, 6h, 24h).

---

## 5. Physical Drone Delivery: Courier Bee

- Confirmed remote marketplace purchases spawn an autonomous **Courier Bee** (`ammora:courier_bee`).
- **Approach**: Gentle, cinematic flight towards the player (speed slowed 4x to ~0.35 blocks/tick).
- **Arrival**: Delivers parcels into inventory with level-up chime and particles. Parcel disappears from paws.
- **Post-Delivery**: Gently backs away ~3 blocks from the player, turns straight up, sounds rocket launch (`FIREWORK_ROCKET_LAUNCH`), and accelerates straight into the stratosphere with firework exhaust particles (`FIREWORK`, `FLAME`, `SMOKE`) and sparkles (`FIREWORK_ROCKET_TWINKLE`).
- **Stratosphere Disappearance**: Reaching max altitude produces a firework blast (`FIREWORK_ROCKET_BLAST`) and discards the entity.
- **Fail-Safe Fallback**: If the recipient is offline, obstructed, or dimension-swapped, items route directly into SQLite database buffer (`unclaimed_deliveries`), claimable via the tablet's `[📦 Deliveries]` tab.

---

## 6. Integrations

### 6.1. Create Mod
- Connect a rotational shaft to a **Trade Dock** to accelerate item intake:
  - 0 RPM: standard hopper rate (1 item / 8 ticks).
  - 128+ RPM: near-instant bulk batch intake.
- Attach a **Display Link** to an Exchange Terminal to render live quotes, price deltas, and economic alerts on Create Flap Displays and Nixie Tubes.

### 6.2. ComputerCraft (CC: Tweaked) Peripheral API
Peripheral name: `"ammora_terminal"`.

```lua
local terminal = peripheral.find("ammora_terminal")

-- Market data
local price = terminal.getSpotPrice("minecraft:iron_ingot")
local stock, target = terminal.getReserves("minecraft:iron_ingot")
local commodities = terminal.listCommodities()

-- Orders
local success, avgPrice = terminal.buyMarket("minecraft:iron_ingot", 64)
local success, avgPrice = terminal.sellMarket("minecraft:iron_ingot", 64)
local orderId = terminal.createLimitOrder("BUY", "minecraft:iron_ingot", 64, 2.50)
local activeOrders = terminal.getOrders()
terminal.cancelOrder(orderId)

-- Events
-- Listen to "ammora_price_change", "ammora_trade", "ammora_event"
local event, commodity, newPrice, oldPrice = os.pullEvent("ammora_price_change")
print("Price changed:", commodity, oldPrice, "->", newPrice)
```

---

## 7. Datapack Schema (`ammora:commodities`)

Custom items and prices are registered via datapacks at `data/<namespace>/ammora/commodities/<name>.json`:

```json
{
  "item": "minecraft:diamond",
  "base_price": 50.0,
  "target_stock": 2000,
  "max_stock": 10000,
  "elasticity": 0.85,
  "category": "MINERALS",
  "enabled": true
}
```

---

## 8. Server Administration & Commands

- **Console Command**: `/ammora admin` (requires permission level 2+).
- **Balances Tab**: Search players, inspect accounts, `[Set]`, `[+ Grant]`, `[- Deduct]`, `[Wipe]`, and configure global `[Company Registration Fee]`.
- **Events Tab**: Launch or terminate macroeconomic events (e.g., Gold Rush, Famine) with custom duration in Minecraft days.
- **AMM Rates Tab**: Calibrate $P_0$, daily modifiers, and physical warehouse inventory.
- **Audit Logs Tab**: Paginated searchable ledger across all trades, shop purchases, and escrow payouts.

---

## Quick Reference Links
- GitHub Repository: [https://github.com/AspisSH/Ammora](https://github.com/AspisSH/Ammora)
- Interactive Documentation: [Ammora Web Docs](https://aspis-sh.github.io/Ammora/)
- Lua Reference: [CC_TWEAKED_GUIDE.md](../docs/CC_TWEAKED_GUIDE.md)
- Complete Handbook: [MANUAL.md](../docs/MANUAL.md) / [MANUAL_EN.md](../docs/MANUAL_EN.md)
