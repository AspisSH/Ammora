# Localization & Translation Guide: Ammora

This guide explains how **Ammora** internationalization works and how to translate the mod into any language (**Chinese, German, Spanish, French, Japanese, Ukrainian, etc.**) without modifying Java source code.

---

## Language File Locations

All translations reside in standard Minecraft JSON language files:
```text
src/main/resources/assets/exchange/lang/
├── en_us.json    <-- English (Default baseline)
└── ru_ru.json    <-- Russian (Full localization)
```

Translations can also be applied at runtime **without recompiling the mod via a standard Resource Pack**:
```text
your_resourcepack/assets/exchange/lang/<locale_code>.json
```

### Common Locale Codes:
- Simplified Chinese: `zh_cn.json`
- Traditional Chinese: `zh_tw.json`
- German: `de_de.json`
- Spanish: `es_es.json`
- French: `fr_fr.json`
- Ukrainian: `uk_ua.json`
- Japanese: `ja_jp.json`
- Portuguese (Brazil): `pt_br.json`

---

## Translation Key Structure

All keys are strictly categorized by namespace:

| Prefix | Purpose | Example |
| :--- | :--- | :--- |
| `itemGroup.exchange` | Creative inventory tab title | `itemGroup.exchange` |
| `block.exchange.*` | In-world block names | `block.exchange.exchange_terminal` |
| `item.exchange.*` | Item names | `item.exchange.cold_wallet` |
| `tooltip.exchange.*` | Item hover lore and tooltips | `tooltip.exchange.cold_wallet.1` |
| `rank.exchange.*` | Trader reputation tiers | `rank.exchange.4` (Investor) |
| `commodity.exchange.*` | Short ticker names for assets | `commodity.exchange.minecraft.diamond` |
| `gui.exchange.terminal.*` | Terminal workstation interface | Buttons, chart labels, order book |
| `gui.exchange.trade.*` | Secure P2P trade session GUI | Lock toggles, offer columns |
| `gui.exchange.wallet.*` | Cold wallet interface | Nearby players, balance, ledger |
| `gui.exchange.dock.*` | Purchase dock automated buyer | Batch sizes, stop-high guards |
| `gui.exchange.shop.*` | Player vending machine | Slot pricing, stock upgrades |
| `gui.exchange.market.*` | Handheld Marketplace Tablet | RFQs, quests, delivery buffer |
| `message.exchange.*` | Chat feedback and notifications | Error alerts, anti-scam warnings |
| `event.exchange.*` | Macroeconomic event headlines | News broadcasts, event alerts |
| `ponder.exchange.*` | Interactive Create 3D ponders | Kinetic mechanics tutorials |

---

## Formatting Codes and Placeholders

1. **Placeholders**:
   - `%s` — String or formatted number (e.g. `12.50 CBX` or player/commodity name).
   - `%d` — Integer (e.g. quantity or rank number).
   - *Never remove or alter the sequence of `%s` and `%d` placeholders!*

2. **Minecraft Color Codes**:
   - `§a` — Green (positive numbers, buy quotes, success)
   - `§c` — Red (negative numbers, sell quotes, errors)
   - `§e` — Yellow (prices, warnings, headlines)
   - `§b` — Aqua (commodities, selected tabs)
   - `§7` / `§8` — Gray / Dark Gray (secondary labels, hints)
   - `§f` — White (standard values)
   - `§r` — Color reset

---

## AI Translation Prompt (ChatGPT / Claude / DeepSeek)

Copy the prompt below and provide `en_us.json` to any AI assistant:

```text
You are a professional Minecraft mod localization expert.
Translate the following JSON dictionary from English (en_us.json) into [TARGET LANGUAGE, e.g. German/Spanish/Chinese].

Formatting Rules:
1. Preserve all JSON keys exactly as written (do NOT alter key names).
2. Keep all Minecraft color formatting codes intact (§a, §c, §e, §b, §7, §8, §f, §r).
3. Do NOT remove or change the order of format specifiers (%s, %d).
4. The standard currency is "CBX" (do not translate or replace with dollar signs).
5. Ensure natural gaming terminology suited for Minecraft.

Here is the source content of en_us.json:
[PASTE en_us.json CONTENT HERE]
```
