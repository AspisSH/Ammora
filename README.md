# Ammora

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.137+-orange.svg)](https://neoforged.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Create Compatibility](https://img.shields.io/badge/Compatibility-Create%200.5.1+-brown.svg)](https://github.com/Creators-of-Create/Create)
[![CC: Tweaked](https://img.shields.io/badge/Compatibility-CC%3A%20Tweaked%201.113+-yellow.svg)](https://github.com/cc-tweaked/CC-Tweaked)

**Ammora** is an advanced financial simulation and economic automation mod for **Minecraft NeoForge 1.21.1**.  
It replaces static shop signs and infinite chest economies with a **living, dynamic Automated Market Maker (AMM)** powered by bonding curve mathematical algorithms.

No more infinite money from automated iron or gold farms: prices continuously react to supply and demand, orders experience realistic curve slippage, market saturation triggers negative ecological disposal fees, and fully automated logistics integrate seamlessly with **Create** and **CC: Tweaked (ComputerCraft)**.

---

## Key Features

- **Unified Standard Currency (CBX)**: A clean, server-wide non-inflationary digital currency used across all exchanges, vending machines, and P2P transfers.
- **Bonding Curve AMM Pricing**: Instant algorithmic spot pricing based on real exchange inventory reserves.
- **Real-Time Candlestick Charts**: Embedded high-performance OHLCV candlestick charts with volume bars, interactive toolbars, and timeframes.
- **Synthetic Commodity Accounts (OMS)**: Speculate on raw material prices (gold, diamonds, netherite) without needing physical chests or vault space. Track real-time PnL and daily carry fees.
- **Server-Side Limit Orders**: Place `BUY_LIMIT` and `SELL_LIMIT` orders. The server automatically fills orders as prices fluctuate, storing items in an offline buffer if needed.
- **Futures & Delivery Contracts**: Accept time-sensitive supply contracts with guaranteed prices, collateral escrow, and reputation rewards.
- **Rust-Style Player Vending Machines**: Deploy customizable in-world shops with 5–10 showcase slots, capacity upgrades (up to 1,024 items/slot), satellite uplink modules, and corporate treasury linking.
- **Marketplace Tablet & RFQ Escrow**: 8-tab handheld trading center: global catalog, shops directory, live auctions, RFQ buy orders with escrow, community quests, company management, delivery buffer, and history ledger.
- **Corporations & Joint Accounts**: Found organizations with shared treasury pools, a 3-tier permission model (`OWNER`, `MANAGER`, `MEMBER`), manager daily spending allowances, and immutable transaction audit ledgers. Toggle personal vs company accounts with 1 click across tablets, shops, and cold wallets.
- **Live Real-Time Auctions**: Host timed lots with incremental bidding, anti-sniping protection (+60s in closing minutes), instant buyout, and escrow collateral holds.
- **Courier Bee Deliveries**: Visualized physical delivery with a courier bee flying items directly into player hands (with automatic safe buffering in SQLite if offline or inventory is full).
- **Secure P2P Direct Trading**: 2-player synchronized escrow trade windows with Anti-Scam safeguards (lock timers, auto-unlock on offer changes, balance checks).
- **Cold Wallet & Proximity Transfers**: Portable NFC wallet with nearby player detection, instant 0-fee transfers, corporate account switching, and a 30-day transaction history ledger.
- **Physical Currency & ATM Terminal**: Banknotes (10, 100, 1000 CBX), money stacks, and decorative cash blocks. Includes an industrial 2-block tall ATM terminal for cash deposits and withdrawals, live cash breakdown calculations, automated inventory deposits, and corporate daily spending limit enforcement.
- **Create & CC:Tweaked Integration**: 
  - **Create**: Kinetic rotational speed scales dock trading frequency; Display Links project live quotes onto flapped display boards; interactive 3D Ponder scenes included.
  - **CC: Tweaked**: Full Lua peripheral API to build algorithmic trading bots, automated arbitrage routers, and wall-mounted stock tickers.
- **Data-Driven & Datapack Support**: Easily add custom commodities, configure delivery contracts, or trigger dynamic market events via standard Minecraft datapacks (`data/ammora/...`).
- **Operator Console & Audit (/ammora admin)**: Complete management GUI for server administrators: inspect and adjust player balances, set global company registration fees, trigger macroeconomic events and crises, tune AMM bonding curves, and audit full transaction ledgers.

---

## Mathematical Model & Economy

### 1. Spot Price Formula (Bonding Curve AMM)
The spot price $P(S)$ for a commodity depends on current reserve stock $S$:

$$P(S) = P_0 \times \left(\frac{S_{target}}{S}\right)^k$$

- $P_0$ — Base equilibrium price in **CBX**.
- $S_{target}$ — Target inventory reserve.
- $S$ — Actual current exchange stock.
- $k$ — Elasticity coefficient ($0.75 - 0.95$).

### 2. Price Slippage & Batch Pricing
Purchasing a large batch of items drives stock down and price up throughout the transaction. Ammora calculates batch cost using the integral midpoint across the AMM curve:
- The UI displays **Spot Unit Price**, **Average Execution Price**, and **Total CBX**.
- The smart `MAX` button uses binary search to compute the exact maximum affordable quantity.

### 3. Surplus Penalty (Negative Disposal Fees)
When exchange reserves exceed the maximum capacity threshold $S_{max}$, the sell price drops below zero and an ecological disposal fee is incurred:

$$C_{disposal} = \alpha \times \left(\frac{S - S_{max}}{S_{target}}\right)^2$$

Selling surplus goods beyond $S_{max}$ charges the player a waste processing fee in CBX, preventing infinite automated farm exploits.

### 4. Reputation & Commission Discounts

| Rank Level | Title | REP Points | Exchange Fee |
| :---: | :---: | :---: | :---: |
| **I** | Novice | 0 – 99 | 2.0% |
| **II** | Trader | 100 – 499 | 1.6% |
| **III** | Broker | 500 – 1,999 | 1.2% |
| **IV** | Investor | 2,000 – 9,999 | 0.8% |
| **V** | Whale | 10,000+ | **0.5%** |

*1 REP point is earned for every 10 CBX in trading volume.*

---

## Blocks & Equipment

| Block / Item | Description |
| :--- | :--- |
| **Exchange Terminal** | Main trading workstation with real-time candlestick charts, order book, OMS management, limit orders, contracts, and redstone output. |
| **ATM Terminal** | 2-block industrial banking terminal for depositing and withdrawing physical CBX banknotes with corporate budget limits. |
| **Trade Dock** | High-speed item import/export automation interface. Connects to hoppers, belts, and funnels. Compatible with Create kinetic shafts and corporate accounts (Shift+RMB). |
| **Purchase Dock** | Automated resource buyer. Transits items from the exchange into local inventory with Stop-High price guards and personal/corporate billing toggle. |
| **Player Shop** | Rust-style vending machine block. Supports custom prices, stock upgrades, satellite broadcasting, and automatic revenue routing to corporate treasury. |
| **Cold Wallet** | Handheld biometric device for CBX account balance, ledger history, personal/corporate account toggle, and wireless P2P player-to-player transfers. |
| **Marketplace Tablet**| Remote marketplace tablet with 8 tabs: catalog shopping, shops directory, live auctions, RFQ escrow orders, community quests, company management, and courier bee deliveries. |
| **CBX Banknotes** | Physical cash currency (10, 100, 1000 CBX), money stacks, and decorative cash blocks with complete crafting recipes. |

---

## Datapack Customization

Ammora supports dynamic registration of commodities, contracts, and market events via vanilla Minecraft datapacks:

### Custom Commodity Example
`data/your_pack/ammora/commodities/titanium.json`:
```json
{
  "resourceId": "modid:titanium_ingot",
  "displayName": "Titanium Ingot",
  "basePrice": 45.0,
  "targetReserve": 4000.0,
  "currentStock": 4000.0,
  "elasticity": 0.85,
  "maxReserve": 8000.0,
  "disposalAlpha": 25.0,
  "feeRate": 0.02,
  "minPriceFloor": 0.50
}
```

*For more details, see [Datapacks Guide](docs/DATAPACKS_GUIDE_EN.md).*

---

## CC: Tweaked (ComputerCraft) Lua API

Place an **Advanced Computer** adjacent to an **Exchange Terminal**, **Trade Dock**, or **Purchase Dock**:

```lua
local exchange = peripheral.find("exchange_terminal")

-- Get real-time spot quote
local quote = exchange.getQuote("minecraft:iron_ingot")
print(string.format("Iron Spot: %.2f CBX | Buy: %.2f CBX", quote.spotPrice, quote.buyPrice))

-- Automated buy order
local result = exchange.buy("minecraft:iron_ingot", 64)
if result.success then
    print(string.format("Purchased 64 iron for %.2f CBX", result.cbxSpent))
end
```

*For complete Lua API specifications and bot templates, see [CC: Tweaked Guide](docs/CC_TWEAKED_GUIDE_EN.md).*

---

## Operator Admin Console (/ammora admin)

Server operators have access to a built-in economic administration console:
```
/ammora admin
```
*Requires operator permissions (OP level 2+).*

The console features 4 management tabs:
1. **Balances**: View all registered server accounts with live player name / UUID search. Set exact balances, grant or deduct CBX, wipe accounts, or use quick presets (+100, +500, +1000 CBX). Online players receive immediate chat notifications upon balance modifications.
2. **Events**: Trigger dynamic macroeconomic crises and booms (Inflation, Gold Rush, Iron Demand Spike) with custom duration in Minecraft days. Active events feature a gold status card with an early termination button (`[⏹ Terminate Early]`).
3. **AMM Rates**: Real-time liquidity pool calibration for any traded commodity. Manually adjust base price $P_0$, daily demand modifiers (+/- %), inventory reserve stock ($S$), or reset modifiers to 0%.
4. **Transaction Logs**: Centralized audit ledger tracking all economic actions (vending machine sales, drone deliveries, RFQ escrow orders, and exchange trades) with filtering by party names and item titles.

---

## Documentation & Guides

- [Player & Engineer Manual (English)](docs/MANUAL_EN.md) / [Руководство игрока (Русский)](MANUAL.md)
- [Datapacks Guide (English)](docs/DATAPACKS_GUIDE_EN.md) / [Гайд по датапакам (Русский)](docs/DATAPACKS_GUIDE_RU.md)
- [ComputerCraft API (English)](docs/CC_TWEAKED_GUIDE_EN.md) / [ComputerCraft API (Русский)](docs/CC_TWEAKED_GUIDE_RU.md)
- [Modded Items Guide (English)](docs/MODDED_ITEMS_GUIDE_EN.md) / [Модовые предметы (Русский)](MODDED_ITEMS_GUIDE.md)
- [Translation & Localization Guide (English)](docs/TRANSLATION_GUIDE_EN.md) / [Гайд по локализации (Русский)](TRANSLATION_GUIDE.md)

---

## Building From Source

Requirements:
- **Java 21 JDK** (Eclipse Temurin or Oracle recommended)
- Git

```bash
# Clone the repository
git clone https://github.com/your-username/Ammora.git
cd Ammora

# Run unit tests
./gradlew test

# Build mod jar
./gradlew build
```

Built jar will be located in `build/libs/exchange-<version>.jar`.

---

## License
This project is licensed under the [MIT License](LICENSE).
Third-party libraries, APIs, and dependencies are acknowledged in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
