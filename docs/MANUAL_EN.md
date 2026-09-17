# Ammora: Complete Player and Automation Engineer Manual

Welcome to **Ammora** — a mod that transforms the Minecraft economy into a dynamic financial market powered by Automated Market Maker (AMM) algorithms. There are no static shop chests with infinite funds: prices are governed by real supply and demand, orders experience curve slippage, inflation from automated farms is countered by ecological disposal fees, and full automation is supported via **Create** and **CC: Tweaked (ComputerCraft)**.

---

## Table of Contents
1. [Philosophy & Economic Model](#1-philosophy--economic-model)
   - [AMM Pricing Formula](#11-amm-pricing-formula)
   - [Price Slippage](#12-price-slippage)
   - [Disposal Fees & Negative Prices](#13-disposal-fees--negative-prices)
   - [Surplus Drain & Mean Reversion](#14-surplus-drain--mean-reversion)
   - [Trader Reputation & Tiers](#15-trader-reputation--tiers)
   - [Unallocated Metal Accounts (OMS)](#16-unallocated-metal-accounts-oms)
2. [Blocks & Gameplay Mechanics](#2-blocks--gameplay-mechanics)
   - [Exchange Terminal](#21-exchange-terminal)
   - [Trade Dock](#22-trade-dock)
   - [Purchase Dock](#23-purchase-dock)
   - [Cold Wallet](#24-cold-wallet)
   - [Player Vending Machine](#25-player-vending-machine)
   - [Market Tablet & Global Marketplace](#26-market-tablet--global-marketplace)
3. [Create Mod Integration](#3-create-mod-integration)
   - [Kinetic Dock Acceleration](#31-kinetic-dock-acceleration)
   - [Display Link: Live Quotes on Flap Boards](#32-display-link-live-quotes-on-flap-boards)
4. [ComputerCraft (CC: Tweaked) Integration](#4-computercraft-cc-tweaked-integration)
   - [Peripheral Connection](#41-peripheral-connection)
   - [Lua API Reference](#42-lua-api-reference)
   - [Automated Trading Bots](#43-automated-trading-bots)
5. [Tips & Frequently Asked Questions (FAQ)](#5-tips--frequently-asked-questions-faq)

---

## 1. Philosophy & Economic Model

In traditional Minecraft server economies, shops use fixed prices. A single player building an industrial iron or gold farm quickly floods the server with millions of ingots, causing hyperinflation and rendering the currency worthless.

In **Ammora**, the exchange behaves like a decentralized liquidity pool (Bonding Curve AMM):
- When players **buy** a commodity, exchange stock decreases and the unit price **rises**.
- When players **sell** a commodity, exchange stock increases and the unit price **falls**.
- If players overwhelm the exchange beyond its maximum capacity, prices turn negative — the exchange charges an **ecological disposal fee**.

### 1.1. AMM Pricing Formula
The current spot price $P(S)$ depends on available reserves $S$:

$$P(S) = P_0 \times \left(\frac{S_{target}}{S}\right)^k$$

- $P_0$ — Base equilibrium price in **CBX**.
- $S_{target}$ — Target inventory reserve (e.g., 10,000 iron ingots).
- $S$ — Actual current stock on the exchange.
- $k$ — Elasticity coefficient ($0.75 - 0.95$).

### 1.2. Price Slippage
The spot price represents the cost of 1 single unit. When buying a stack (64 pcs) or a crate (2,304 pcs), each subsequent item is purchased from a progressively depleted pool. Therefore, the average unit price is higher than the initial spot quote.

In the Ammora terminal:
- You always see **Spot Unit Price**, **Average Price**, and **Total CBX**.
- The smart `MAX` button uses binary search to compute the exact maximum affordable quantity.

### 1.3. Disposal Fees & Negative Prices
Every commodity has a maximum reserve threshold $S_{max}$. If inventory exceeds this limit, the price falls below zero:

$$C_{disposal} = \alpha \times \left(\frac{S - S_{max}}{S_{target}}\right)^2$$

Selling items during an overflow charges the player a waste processing bill in CBX. If the player lacks sufficient funds, the transaction is rejected.

> [!WARNING]
> Do not pipe automated farm output directly into an exchange dock without setting a stop-loss price! Overfilled warehouses will drain your CBX account.

### 1.4. Surplus Drain & Mean Reversion
Every in-game day (24,000 ticks / 20 real minutes), the exchange burns **8%** of surplus inventory over $S_{target}$, simulating municipal consumption and infrastructure use. Prices gradually normalize back to $P_0$.

### 1.5. Trader Reputation & Tiers
Trading volume generates Reputation Points (**REP**): $1 \text{ REP} = 10 \text{ CBX in trade volume}$.

| Level | Rank | REP Score | Fee Rate |
| :---: | :---: | :---: | :---: |
| **I** | Novice | 0 – 99 | 2.0% |
| **II** | Trader | 100 – 499 | 1.6% |
| **III** | Broker | 500 – 1,999 | 1.2% |
| **IV** | Investor | 2,000 – 9,999 | 0.8% |
| **V** | Whale | 10,000+ | **0.5%** |

### 1.6. Unallocated Metal Accounts (OMS)
OMS allows players to invest directly in raw commodities without chest storage:
- Invest CBX to buy synthetic units at the current spot price.
- Live PnL tracking in the GUI (`+X.XX CBX` / `-X.XX CBX`).
- Close positions anytime to withdraw accumulated profit back to your CBX balance.
- A tiny daily carry fee (0.05%) applies at midnight to reward active swing trading.

---

## 2. Blocks & Gameplay Mechanics

### 2.1. Exchange Terminal
The primary trading station:
- Interactive candlestick charts with volume bars.
- Instant market buy and sell controls.
- Synthetic OMS position manager.
- Limit order placement (`BUY_LIMIT`, `SELL_LIMIT`).
- Delivery contracts / futures boards.
- Configurable redstone comparator output.

### 2.2. Trade Dock
Automated item import and export interface:
- Connects directly to hoppers, belts, and funnels.
- Set a **Stop-Loss Price** to protect against market crashes.
- Items piped in are automatically sold, crediting the owner's CBX balance.

### 2.3. Purchase Dock
Automated resource buyer:
- Select a target commodity and batch size (1, 4, 8, 16, 32, 64).
- Set a **Stop-High Guard** (available to Broker rank and higher).
- Automatically draws items from the exchange into adjacent chests whenever prices are favorable.

### 2.4. Cold Wallet
Handheld biometric NFC wallet:
- Check account balance and reputation rank anywhere.
- Wireless P2P transfers to players within 30 blocks.
- 30-day transaction history ledger.

### 2.5. Player Vending Machine
In-world vending shop block:
- Up to 10 independent showcase slots with individual CBX pricing.
- Upgradable slot capacities (64 → 128 → 256 → 512 → 1,024 items).
- Install a Satellite Uplink module to broadcast stock globally to the Marketplace Tablet.

### 2.6. Market Tablet & Global Marketplace
Handheld digital marketplace:
- Remote shopping from broadcasted player vending machines.
- **RFQ Escrow Bounties**: Post public buy requests with escrow funds.
- **Community Quests**: Post jobs, work assignments, and custom task bounties.
- **Delivery Buffer**: 10-slot safe storage buffer for goods received while offline or when inventory was full.

---

## 3. Create Mod Integration

### 3.1. Kinetic Dock Acceleration
Connecting a Create rotational shaft to the side of a **Trade Dock** dramatically accelerates trading frequency:
- At 0 RPM: standard hopper speed (1 item every 8 ticks).
- At 128+ RPM: near-instant bulk batch intake!

### 3.2. Display Link
Attach a Create **Display Link** to an Exchange Terminal to display live stock quotes, price changes, and active news events directly onto Flap Displays and Nixie Tubes.

---

## 4. ComputerCraft (CC: Tweaked) Integration

Refer to the [CC: Tweaked Guide](CC_TWEAKED_GUIDE_EN.md) for complete Lua method references, event documentation, and automated trading bot scripts.

---

## 5. Tips & FAQ

- **Q: Why is iron price negative?**  
  *A: The exchange warehouse is overflowing beyond its maximum capacity ($S_{max}$). Buy up the excess iron for cheap to restore positive prices!*
- **Q: Where did my purchased items go when my inventory was full?**  
  *A: Open your Market Tablet and check the **Deliveries Buffer** tab to claim your items safely.*
- **Q: How do I lower trading fees?**  
  *A: Trade frequently to accumulate REP points. Higher reputation tiers reduce fees down to 0.5%.*
