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
   - [Live Auction House](#27-live-auction-house)
   - [Corporations & Joint Accounts](#28-corporations--joint-accounts)
   - [Courier Bee Delivery](#29-courier-bee-delivery)
   - [ATM Terminal](#210-atm-terminal)
   - [Physical Currency (Banknotes, Stacks, Blocks)](#211-physical-currency-banknotes-stacks-blocks)
3. [Create Mod Integration](#3-create-mod-integration)
   - [Kinetic Dock Acceleration](#31-kinetic-dock-acceleration)
   - [Display Link: Live Quotes on Flap Boards](#32-display-link-live-quotes-on-flap-boards)
4. [ComputerCraft (CC: Tweaked) Integration](#4-computercraft-cc-tweaked-integration)
   - [Peripheral Connection](#41-peripheral-connection)
   - [Lua API Reference](#42-lua-api-reference)
   - [Automated Trading Bots](#43-automated-trading-bots)
5. [Operator & Admin Console (/ammora admin)](#5-operator--admin-console-ammora-admin)
   - [Security & Access](#51-security--access)
   - [Balances & Economy Settings](#52-balances--economy-settings)
   - [Economic Events](#53-economic-events)
   - [AMM Rates & Reserve Calibration](#54-amm-rates--reserve-calibration)
   - [Transaction Audit Logs](#55-transaction-audit-logs)
6. [Tips & Frequently Asked Questions (FAQ)](#6-tips--frequently-asked-questions-faq)

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
- **Corporate Account Routing:** Toggle billing between your personal account and your corporation treasury.

### 2.3. Purchase Dock
Automated resource buyer:
- Select a target commodity and batch size (1, 4, 8, 16, 32, 64).
- Set a **Stop-High Guard** (available to Broker rank and higher).
- Automatically draws items from the exchange into adjacent chests whenever prices are favorable.
- **Corporate Account Billing:** Fund automated purchases directly from corporate reserves.

### 2.4. Cold Wallet
Handheld biometric NFC wallet:
- Check account balance and reputation rank anywhere.
- Wireless P2P transfers to players within 30 blocks.
- **Account Switcher:** Seamlessly toggle between `[Personal]` and `[🏢 Corporation]` accounts to audit or transfer corporate funds (available to Owners and Managers).
- 30-day transaction history ledger.

### 2.5. Player Vending Machine
In-world vending shop block:
- Up to 10 independent showcase slots with individual CBX pricing.
- Upgradable slot capacities (64 → 128 → 256 → 512 → 1,024 items).
- Install a Satellite Uplink module to broadcast stock globally to the Marketplace Tablet.
- **Corporate Link:** Bind the vending machine to your registered Corporation (`/ammora shop link-company <id>` or in the owner UI). When linked, customer purchases bypass offline restrictions and **automatically credit the corporate treasury** directly.

### 2.6. Market Tablet & Global Marketplace
Handheld digital marketplace:
- **Two-Tier 8-Tab Navigation:** High-resolution expanded terminal layout featuring 8 distinct tabs:
  - `[🛒 Shop]`: Browse remote player vending machines with live search and category filters.
  - `[🔨 Auctions]`: Real-time multiplayer bidding and instant buyouts.
  - `[📋 Bounties]`: Public buy requests funded with escrow CBX.
  - `[📜 Quests]`: Community tasks and work assignments.
  - `[📦 Deliveries]`: 10-slot safe storage buffer for goods received while offline or full inventory.
  - `[🏢 Corporation]`: Company hub, employee management, daily limits, and financial ledger.
  - `[📈 Exchange]`: Remote access to the live AMM commodity exchange.
  - `[⚙ Settings]`: Preferences and telemetry settings.
- **Account Switcher:** Header toggle `[Personal] / [🏢 Corporation]` allows buying goods, funding bounties, or placing bids using company treasury funds.

### 2.7. Live Auction House
Real-time multiplayer competitive bidding system integrated into the Market Tablet:
- **Escrow-Backed Bidding:** Placing a bid locks the required CBX in server escrow. If outbid, the funds are instantly refunded to the player or corporate balance.
- **Instant Buyout (`Buyout Price`):** Sellers can specify an optional instant buyout price. Paying this price immediately terminates the auction and awards the lot.
- **Anti-Sniping Protection:** Bids placed in the final 60 seconds automatically extend the auction countdown by +60 seconds, preventing last-second bid sniping.
- **Lot Creation Modal:** List any held item by specifying a starting bid, optional buyout price, and auction duration (e.g., 1 hour, 6 hours, 24 hours).

### 2.8. Corporations & Joint Accounts
Create business entities, share capital, and delegate enterprise operations:
- **Company Registration:** Open the `[🏢 Corporation]` tab on your Market Tablet. Players pay a registration fee in CBX (calibrated by server operators) to establish a new registered corporation.
- **Role Hierarchy:**
  - `OWNER`: Full administrative control, employee invitations/expulsions, role promotions, budget limit management, and treasury withdrawals.
  - `MANAGER`: Authorized to spend company funds up to a configurable daily spending limit (`daily_limit`), link machines, and execute purchasing contracts. Cannot invite new members or promote staff.
  - `MEMBER`: View corporate financial metrics, audit transaction history, and participate in corporate projects without access to direct treasury withdrawals.
- **Daily Member Limit & Live Tracking:** Owners can enforce per-member daily spending quotas (`daily_limit`) resetting every 24 hours. When switching to a corporate account in the Tablet or ATM, members see their active spending budget: `5/100 CBX` (spent today / daily limit).
- **Company Invite Dialog (CompanyInviteScreen):** Invited players receive a dedicated modal window detailing company name, inviter, and role terms before accepting or declining.
- **Full Audit Ledger:** Every deposit, withdrawal, automated dock transfer, tablet purchase, and ATM transaction (`ATM_WITHDRAW`, `ATM_DEPOSIT`) is logged in the company ledger with exact timestamps and member attribution.
- **Full Automation Integration:** Trade Docks, Purchase Docks, Player Shops, Cold Wallets, and ATM terminals can all be linked to corporate accounts.

### 2.9. Courier Bee Delivery
Physical courier delivery mechanic with fail-safe buffering:
- When a remote purchase is confirmed on the Market Tablet, an autonomous **Courier Bee** spawns in the world and flies directly to the buyer's coordinates.
- **Fail-Safe Fallback:** If the player is offline, across dimensions, or obstructed, items are safely routed to the **Deliveries Buffer** tab in the Market Tablet, where they can be claimed anytime.

### 2.10. ATM Terminal
Industrial 2-block tall terminal built with polished deepslate and brass accents for cash banking:
- **Personal & Corporate Accounts:** Easily toggle between `[Personal]` and `[Corporate]` accounts. Non-owner corporate members are subject to daily spending limits.
- **Cash Withdrawal:** Numeric input field with automatic round-up to multiples of 10 CBX upon Enter or unfocus. Includes 2-column quick preset buttons (`[10]`, `[50]`, `[100]`, `[500]`, `[1,000]`, `[5,000 CBX]`) and a real-time denomination breakdown calculator card (1000, 100, 10 CBX notes).
- **Cash Deposit:** Single-click **"Deposit All Cash"** button automatically tallies and deposits all CBX banknotes from player inventory, instantly updating slots and balances.
- **Audit Tracking:** All corporate cash withdrawals and deposits are permanently recorded in the company audit ledger.

### 2.11. Physical Currency (Banknotes, Stacks, Blocks)
Material CBX currency for physical player trading:
- **Banknotes (10, 100, 1000 CBX):** Distinctive industrial bills (Zinc 10, Cyan 100, Brass 1000).
- **Money Stacks:** Compact 9-note bundles (3x3 crafting grid). Unpackable back to 9 banknotes.
- **Money Blocks:** Solid decorative currency blocks for high-value vault storage (9 money stacks in crafting table). Unpackable back to 9 money stacks.

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

## 5. Operator & Admin Console (/ammora admin)

Ammora provides server operators with a secure, graphical command center to monitor and orchestrate the economy:

### 5.1. Security & Access
- **Command:** `/ammora admin`.
- **Permission:** Operator permission level 2+ (`source.hasPermission(2)`). Non-operators cannot execute the command or access GUI payloads.
- Critical operations are logged to the dedicated server console.

### 5.2. Balances & Economy Settings
- **Search:** Search all server player accounts by username or string UUID.
- **Account Inspection:** View real-time balance in CBX, reputation tier (Lvl I–V), REP points, and UUID.
- **Actions:**
  - `[Set]`: Overwrite account balance with an exact CBX amount.
  - `[+ Grant]`: Increment balance by specified amount.
  - `[- Deduct]`: Decrement balance safely (clamped to 0.00).
  - `[Wipe]`: Reset account balance to 0.00 CBX.
  - **Quick Presets:** Instant `[+100]`, `[+500]`, `[+1000]` reward shortcuts.
- **Notifications:** Online players immediately receive a personal system message notifying them of balance adjustments.
- **Corporation Registration Fee Calibration:** At the top of the Balances tab, operators can inspect and configure the global cost to establish a new company (`[Set Fee]`). Changes are saved immediately to the server configuration.

### 5.3. Economic Events
- **Active Event Card:** When an event is active, a golden banner displays the event title, days remaining, and modifier details.
- **Early Termination `[⏹ Terminate Early]`:** Abruptly cancel active anomalies to restore normal market parameters.
- **Template Catalog:** Browse available macroeconomic events (e.g., Gold Rush, Diamond Famine) with multiplier percentages.
- **Trigger `[▶ Launch]`:** Specify duration in Minecraft days and trigger server-wide economic events.

### 5.4. AMM Rates & Reserve Calibration
- **Commodity Directory:** Inspect all traded resources with current live spot prices.
- **Reserve Inspection:** View exact spot price, warehouse stock ($S$), and target reserve ($S_{target}$).
- **Parameter Controls:**
  - `[Set P₀]`: Calibrate base equilibrium price ($P_0$).
  - `[Set Mod]`: Apply temporary daily demand modifiers (+/- %).
  - `[Set Stock]`: Adjust physical warehouse inventory ($S$) to test or balance liquidity pools.
  - `[Reset Modifier (0%)]`: Instantly reset daily modifiers to neutral 0%.

### 5.5. Transaction Audit Logs
- **Comprehensive Ledger:** Unified chronological audit of local shop sales, drone deliveries, RFQ escrow fulfillment, and exchange trades.
- **Filter:** Search by buyer/seller username, item name, or transaction category.
- **Pagination:** Smooth paginated review across server trade history.

---

## 6. Tips & FAQ

- **Q: Why is iron price negative?**  
  *A: The exchange warehouse is overflowing beyond its maximum capacity ($S_{max}$). Buy up the excess iron for cheap to restore positive prices!*
- **Q: Where did my purchased items go when my inventory was full?**  
  *A: Open your Market Tablet and check the **Deliveries Buffer** tab to claim your items safely.*
- **Q: How do I lower trading fees?**  
  *A: Trade frequently to accumulate REP points. Higher reputation tiers reduce fees down to 0.5%.*
