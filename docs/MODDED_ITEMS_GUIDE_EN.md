# Integration Guide: Custom & Modded Items in Ammora

This guide explains how to integrate items from other mods (**Create**, **Mekanism**, **Thermal Expansion**, **Botania**, **Applied Energistics 2**, **Farmer's Delight**, etc.) into the spot and derivative markets of **Ammora**.

You can define custom commodities either via **Minecraft Datapacks** (`data/<pack>/ammora/commodities/*.json`) or via the global config file (`config/ammora_custom_items.json`).

---

## Configuration File Location

The configuration file is automatically generated on the first server or client launch:
```text
config/ammora_custom_items.json
```
*(If missing, the mod generates a template with brass, zinc, and steel examples)*.

---

## Item Structure and Parameters

Each commodity is specified as a JSON object within an array:

```json
[
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
    "minPriceFloor": 0.20,
    "enabled": true
  }
]
```

### Minimal Configuration (Auto-Balanced Defaults!):
You don't need to specify every field — the market engine automatically calculates balanced values for missing parameters:
```json
{
  "resourceId": "create:brass_ingot",
  "displayName": "Brass Ingot",
  "basePrice": 28.0,
  "targetReserve": 6000.0
}
```

### AMM Formula Parameters:
| Parameter | Type | Description | Recommended Values |
| :--- | :--- | :--- | :--- |
| `resourceId` | `String` | Registry ID in Minecraft (`modid:item_id`). | Required |
| `displayName` | `String` | Display name in terminal GUIs and notifications. | Optional |
| `basePrice` | `Double` | Base equilibrium price in **CBX** when stock = target. | `1.0` to `5000.0+` |
| `targetReserve`| `Double` | Target exchange reserve ($S_{target}$). At this level, price = `basePrice`. | `250` - `20,000` pcs |
| `currentStock` | `Double` | Initial stock. If omitted, defaults to `targetReserve`. | = `targetReserve` |
| `elasticity` | `Double` | Curve elasticity ($k$). Higher values make price react sharper to scarcity. | `0.75` - `0.95` |
| `maxReserve` | `Double` | Overflow capacity threshold ($S_{max}$). Exceeding this triggers disposal fees. | `targetReserve * 1.5` |
| `disposalAlpha`| `Double` | Waste disposal fee steepness coefficient. | `15.0` - `30.0` |
| `feeRate` | `Double` | Trading fee (0.02 = 2%). | `0.01` - `0.05` |
| `minPriceFloor`| `Double` | Absolute price floor (prevents infinite price collapse). | `0.05` - `1.0` |
| `enabled` | `Boolean`| Whether the item is active (`true`/`false`). | `true` |

---

## Sample Research Mechanic (Anti-Speedrun)

To prevent wealthy players from immediately buying up end-game resources without ever crafting them:
1. Basic resources (**Iron** and **Copper**) are unlocked for all players by default.
2. All other resources (Diamonds, Netherite, and modded commodities) are initially locked with a lock badge `[Locked]`.
3. Players can **sell** harvested or mined items to the exchange at any time.
4. To unlock **purchasing** and OMS derivatives trading, click in the terminal:
   `[RESEARCH SAMPLE (1 pc)]`
   The exchange consumes 1 item from the player's inventory and permanently grants market access.

---

## Tier Balancing Cheat Sheet

| Tier | Examples | Base Price (`basePrice`) | Target Reserve (`targetReserve`) | Elasticity (`elasticity`) |
| :--- | :--- | :--- | :--- | :--- |
| **Tier 1: Basic Ores** | Zinc (`create:zinc_ingot`), Tin, Lead | `10.0 - 15.0 CBX` | `10,000 - 15,000` | `0.75 - 0.80` |
| **Tier 2: Alloys & Mechanics** | Brass (`create:brass_ingot`), Steel (`mekanism:ingot_steel`), Bronze | `25.0 - 45.0 CBX` | `5,000 - 8,000` | `0.80 - 0.85` |
| **Tier 3: Advanced Alloys** | Osmium, Signalum, Enderium, Manasteel | `80.0 - 250.0 CBX` | `1,500 - 3,000` | `0.85 - 0.90` |
| **Tier 4: High-Tech Components**| Refined Obsidian, Nether Star, Terrasteel | `500.0 - 2000.0 CBX` | `300 - 800` | `0.90 - 0.95` |
| **Tier 5: Endgame Elements** | Antimatter, Draconic Cores, Creative Parts | `5000.0 - 50000.0 CBX`| `50 - 150` | `0.95` |

---

## Ready-to-Use AI Prompt (ChatGPT / Claude / DeepSeek)

Copy the prompt below into any AI assistant to generate a balanced JSON file instantly:

```text
You are an experienced Minecraft modpack balance engineer and Automated Market Maker (AMM) financial designer.
I need to add items from various mods into the Ammora configuration file: config/ammora_custom_items.json.

Exchange rules:
- Currency: ChainBX (CBX).
- Vanilla benchmark prices: Copper = 8 CBX, Iron = 12 CBX, Gold = 40 CBX, Diamond = 350 CBX, Netherite = 4500 CBX.
- AMM formula based on targetReserve and elasticity (0.75 for common bulk items, 0.95 for rare endgame goods).
- maxReserve is typically targetReserve * 1.6.
- minPriceFloor is basePrice * 0.01 (minimum 0.05).
- disposalAlpha = 15.0 - 25.0.
- feeRate = 0.02.
- enabled = true.

Here is the list of items and mods I want to add:
[INSERT YOUR ITEM LIST HERE, E.G.:
1. Create: brass ingot (brass_ingot), zinc ingot (zinc_ingot), precision mechanism (precision_mechanism)
2. Mekanism: osmium ingot (ingot_osmium), steel ingot (ingot_steel), enriched alloy (enriched_alloy)
3. Botania: manasteel ingot (manasteel_ingot), terrasteel ingot (terrasteel_ingot)
]

Generate a strictly valid JSON array with no extra wrapper text, ready to paste into config/ammora_custom_items.json.
```
