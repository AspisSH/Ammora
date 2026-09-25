# Ammora — Dynamic Resource Exchange & Living Economy

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.137+-orange.svg)](https://neoforged.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Create Compatibility](https://img.shields.io/badge/Create-Compatible-brown.svg)](https://github.com/Creators-of-Create/Create)
[![CC: Tweaked](https://img.shields.io/badge/CC%3A%20Tweaked-Compatible-yellow.svg)](https://github.com/cc-tweaked/CC-Tweaked)

> **Ammora** turns static Minecraft server economies into a living, self-regulating financial market.  
> No more infinite money from automated iron or gold farms: prices continuously react to supply and demand via bonding curve algorithms, players build corporations and vending networks, and market oversupply incurs ecological disposal fees.

---

## Key Features

### 1. Algorithmic Commodity Exchange (AMM Bonding Curve)
* **Dynamic Spot Pricing**: Commodity prices (iron, gold, diamonds, and more) smoothly fluctuate based on real exchange reserves. The more players sell, the lower the price; the more they buy, the higher it climbs.
* **Anti-Inflation Safeguard**: When reserves exceed capacity, a progressive disposal fee is charged, preventing automated farm exploits from breaking the server economy.
* **Exchange Terminal**: In-world workstation featuring real-time candlestick charts (OHLCV), trading volumes, and server-side limit orders (`BUY_LIMIT`, `SELL_LIMIT`).
* **Synthetic Commodity Accounts (OMS)**: Invest in raw material value without hoarding chests of physical ores.

### 2. Player Corporations & Shared Treasuries
* **Found Companies**: Team up with friends and establish organizations with a pooled corporate treasury.
* **Permission Roles**: 3-tier hierarchy (`OWNER`, `MANAGER`, `MEMBER`) with custom daily spending allowances per member.
* **Audit Ledger**: Comprehensive, transparent transaction logging tracking every purchase, ATM withdrawal, and company expense.

### 3. Player Commerce & Auctions
* **Vending Machines (Player Shops)**: Deploy in-world storefronts with custom pricing, capacity upgrades (up to 1,024 items/slot), satellite broadcasting, and automatic profit routing to personal or corporate accounts.
* **Marketplace Tablet**: Remote handheld access to the global catalog, server shops directory, live auctions, and RFQ buy orders with secure escrow holds.
* **Live Auctions**: Real-time open bidding with anti-sniping extensions (+60s on last-minute bids) and instant buyout options.
* **Animated Deliveries**: A courier bee physically flies purchased items straight to your hands (with offline buffer backup).
* **Cold Wallet**: NFC biometric device for instant, zero-fee player-to-player transfers.

### 4. Physical Currency & ATM Terminal
* **Physical CBX Cash**: Banknotes (10, 100, 1000 CBX), money stacks, and decorative cash blocks.
* **Industrial ATM**: 2-block tall terminal for cash deposits and withdrawals with instant balance conversion (personal or corporate).
* **Budget Enforcement**: Company employees cannot withdraw cash beyond their daily corporate spending allowance.

### 5. Automation & Integrations
* **Create Mod**: Kinetic rotational speed scales dock trading frequency, Display Links project real-time quotes onto flapped display boards, and interactive 3D Ponder scenes are included.
* **CC: Tweaked (ComputerCraft)**: Comprehensive Lua peripheral API to build custom market tickers, automated arbitrage routers, and trading bots.
* **Data-Driven**: Full datapack support (`data/ammora/...`) to register custom modded commodities, delivery contracts, or dynamic economic events.

---

## Blocks & Equipment

| Block / Item | Description |
| :--- | :--- |
| **Exchange Terminal** | Main trader workstation: live quotes, candlestick charts, limit orders, and OMS accounts. |
| **ATM Terminal** | 2-block industrial terminal for depositing and withdrawing physical CBX banknotes. |
| **Player Shop** | Secure vending machine block for selling items to other players. |
| **Trade Dock** | High-speed item export dock for selling farm output to the exchange via hoppers or belts. |
| **Purchase Dock** | Automated resource buyer with Stop-High price guards and corporate billing support. |
| **Marketplace Tablet** | Remote handheld trading hub: catalog, shop directory, auctions, and company management. |
| **Cold Wallet** | Pocket NFC device for balance checks and instant P2P transfers to nearby players. |
| **CBX Banknotes** | Physical cash currency (10, 100, 1000 CBX), money stacks, and decorative blocks. |

---

## Requirements & Installation

* **Platform**: Minecraft **1.21.1**, **NeoForge** (`21.1.137` or newer).
* **Java**: **Java 21**.
* **Dependencies**: Standalone (SQLite database driver is bundled via Jar-in-Jar).
* **Optional Integrations**: [Create](https://modrinth.com/mod/create) (0.5.1+), [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) (1.113+).

---

## License
Licensed under the open-source **[MIT License](LICENSE)**. Free to use in modpacks and server distributions.
