# Datapacks Guide: Commodities, Events & Contracts

Ammora allows server administrators and modpack creators to customize the economy without writing any Java code using standard **Minecraft Datapacks**.

Datapacks reload seamlessly whenever the server starts or when an administrator executes `/reload`.

---

## Datapack Directory Structure

Inside your datapack (`world/datapacks/your_pack/` or inside a mod jar):
```text
your_datapack/
├── pack.mcmeta
└── data/
    └── <namespace>/
        └── exchange/
            ├── commodities/           <-- Commodities traded on the AMM
            │   ├── copper.json
            │   └── brass.json
            ├── market_events/         <-- Dynamic macroeconomic events
            │   ├── gold_rush.json
            │   └── tech_boom.json
            └── delivery_contracts/    <-- Supply futures & bounties
                ├── steel_delivery.json
                └── food_drive.json
```

> [!TIP]
> You can place files under any namespace (e.g. `data/exchange/exchange/commodities/` or `data/my_pack/exchange/commodities/`). The mod scans all active namespaces for `exchange/commodities`, `exchange/market_events`, and `exchange/delivery_contracts`.

---

## 1. Custom Commodities (`commodities/*.json`)

Every commodity added here appears automatically on the **Exchange Terminal**, **Trade Dock**, **Purchase Dock**, and in **CC: Tweaked** peripherals.

### JSON Schema & Example
```json
{
  "resourceId": "create:brass_ingot",
  "displayName": "Brass Ingot",
  "basePrice": 28.0,
  "targetReserve": 6000.0,
  "currentStock": 6000.0,
  "elasticity": 0.85,
  "maxReserve": 10000.0,
  "disposalAlpha": 20.0,
  "feeRate": 0.02,
  "minPriceFloor": 0.20
}
```

### Parameter Reference
| Parameter | Type | Required | Description | Default / Recommended |
| :--- | :---: | :---: | :--- | :--- |
| `resourceId` | String | **Yes** | Minecraft registry name (`modid:item_id`). | e.g. `minecraft:iron_ingot` |
| `displayName` | String | No | Human-readable name displayed in GUIs. | Translated name if omitted |
| `basePrice` | Double | **Yes** | Equilibrium price $P_0$ in **CBX** when stock equals target. | `1.0` - `5000.0` |
| `targetReserve`| Double | **Yes** | Optimal reserve stock $S_{target}$. | `500` - `20000` |
| `currentStock` | Double | No | Initial stock on first registration. | Equals `targetReserve` |
| `elasticity` | Double | No | Bonding curve slope coefficient $k$. | `0.80` (range: `0.70` - `0.95`) |
| `maxReserve` | Double | No | Threshold $S_{max}$ where negative prices kick in. | `targetReserve * 1.5` |
| `disposalAlpha`| Double | No | Waste penalty steepness multiplier $\alpha$. | `15.0` (range: `10.0` - `40.0`) |
| `feeRate` | Double | No | Exchange trading fee rate. | `0.02` (2.0%) |
| `minPriceFloor`| Double | No | Absolute price floor (cannot drop below this). | `0.05` CBX |

---

## 2. Dynamic Market Events (`market_events/*.json`)

Market events dynamically alter prices, shift AMM multipliers, and notify players via chat headlines and terminal alert tickers.

### JSON Schema & Example
```json
{
  "id": "gold_rush",
  "title": "Gold Rush",
  "description": "Ancient treasury uncovered! Massive influx of gold drives market prices down.",
  "affectedResource": "minecraft:gold_ingot",
  "priceMultiplier": 0.70,
  "durationDays": 2,
  "weight": 10
}
```

### Parameter Reference
| Parameter | Type | Required | Description |
| :--- | :---: | :---: | :--- |
| `id` | String | **Yes** | Unique event identifier. |
| `title` | String | **Yes** | Headline shown in the top ticker bar of the terminal. |
| `description` | String | **Yes** | Full explanatory news broadcast sent to players. |
| `affectedResource`| String | **Yes** | Resource ID whose price multiplier is modified. |
| `priceMultiplier` | Double | **Yes** | Relative price modifier (e.g. `1.35` for +35% boom, `0.70` for -30% slump). |
| `durationDays` | Integer| No | How many in-game days (24,000 ticks each) the event lasts (default: 2). |
| `weight` | Integer| No | Relative selection chance in the random event lottery (default: 10). |

---

## 3. Delivery Contracts (`delivery_contracts/*.json`)

Delivery contracts represent government and corporate futures bounties that players can sign at the Exchange Terminal.

### JSON Schema & Example
```json
{
  "contractId": "bridge_reinforcement",
  "title": "Grand Bridge Construction",
  "resourceId": "minecraft:iron_ingot",
  "targetAmount": 1280,
  "guaranteedUnitPrice": 15.5,
  "collateralCbx": 500.0,
  "deadlineTicks": 48000,
  "rewardRep": 150
}
```

### Parameter Reference
| Parameter | Type | Required | Description |
| :--- | :---: | :---: | :--- |
| `contractId` | String | **Yes** | Unique contract identifier. |
| `title` | String | **Yes** | Name of the contract displayed in the terminal list. |
| `resourceId` | String | **Yes** | Item required to fulfill the contract. |
| `targetAmount` | Integer| **Yes** | Total count of items that must be delivered. |
| `guaranteedUnitPrice`| Double| **Yes** | Payout in **CBX** per delivered item (independent of market drops!). |
| `collateralCbx`| Double | No | Collateral escrow deposited by the player upon accepting (default: 0). |
| `deadlineTicks`| Integer| No | Time limit in server ticks (`24000` = 1 in-game day = 20 minutes). |
| `rewardRep` | Integer| No | Trader reputation points awarded on successful completion. |

---

## Testing and Verification

To verify that your datapack loaded properly:
1. Run `/reload` in your server console or with operator permissions.
2. Check the server log for messages:
   ```text
   [Ammora] Loaded X custom commodities from datapacks.
   [Ammora] Loaded Y market events from datapacks.
   [Ammora] Loaded Z delivery contracts from datapacks.
   ```
3. Open the **Exchange Terminal** or check the commodity catalog in the **Marketplace Tablet**.
