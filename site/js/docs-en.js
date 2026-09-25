/**
 * Ammora Documentation - English Content (docs-en.js)
 */
window.DOCS_EN = {
  navTitle: "Ammora Documentation",
  searchPlaceholder: "Search documentation... (Ctrl+K)",
  onThisPage: "On this page",
  copied: "Copied!",
  copy: "Copy",
  copyPage: "Copy page",
  copyPageSuccess: "Copied as Markdown!",
  aiPrompt: "prompt.md",
  categories: [
    {
      id: "general",
      title: "Getting Started",
      items: [
        { id: "overview", title: "Overview & Philosophy", icon: "book-open" },
        { id: "amm-economics", title: "AMM Economic Model", icon: "trending-up" },
        { id: "calculator", title: "Interactive AMM Simulator", icon: "activity" }
      ]
    },
    {
      id: "gameplay",
      title: "Gameplay Mechanics",
      items: [
        { id: "blocks-terminal", title: "Exchange Terminal", icon: "monitor" },
        { id: "blocks-logistics", title: "Trade & Purchase Docks", icon: "package" },
        { id: "cold-wallet", title: "Cold Wallet & P2P", icon: "credit-card" },
        { id: "vending-marketplace", title: "Vending Machine & Market", icon: "shopping-bag" },
        { id: "atm-cash", title: "ATM & Physical Cash", icon: "dollar-sign" }
      ]
    },
    {
      id: "integrations",
      title: "Integrations",
      items: [
        { id: "cc-tweaked", title: "CC: Tweaked (ComputerCraft)", icon: "cpu" },
        { id: "create-mod", title: "Create (Kinetics & Displays)", icon: "settings" }
      ]
    },
    {
      id: "configuration",
      title: "Configuration & Server",
      items: [
        { id: "datapacks", title: "Datapacks Guide", icon: "file-text" },
        { id: "custom-items", title: "Custom & Modded Items", icon: "layers" },
        { id: "admin-console", title: "Admin Console (/ammora admin)", icon: "shield" },
        { id: "localization", title: "Localization & i18n", icon: "globe" },
        { id: "faq", title: "FAQ & Troubleshooting", icon: "help-circle" }
      ]
    }
  ],

  sections: {
    "overview": {
      title: "Overview & Philosophy",
      subtitle: "A dynamic financial market for Minecraft powered by Automated Market Maker (AMM) bonding curves.",
      breadcrumbs: ["Getting Started", "Overview & Philosophy"],
      html: `
        <h2>Introduction</h2>
        <p><strong>Ammora</strong> is an advanced financial ecosystem mod for Minecraft (NeoForge 1.21.1) that replaces static, infinite-fund shop chests with an algorithmic liquidity market. Traditional server economies inevitably collapse when players build massive industrial iron or gold farms. Ammora solves this at the root: commodity prices are dictated by real supply and demand governed by mathematical bonding curves.</p>
        <p>When players purchase commodities, reserves decrease and the unit price rises. When surplus commodities are sold, market stock swells and the price drops.</p>

        <div class="callout callout-info">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            <span>Official Currency: CBX</span>
          </div>
          <div class="callout-content">
            All trades and balances use <strong>CBX</strong>. Account balances are persisted atomically in SQLite WAL mode tied to the player's unique UUID. Money is never dropped upon death or held in physical paper bills.
          </div>
        </div>

        <h2>Core Mod Features</h2>
        <div class="cards-grid">
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Bonding Curve AMM</span>
              <span class="badge badge-blue">Algorithm</span>
            </div>
            <div class="doc-card-desc">Quotes dynamically shift according to inventory levels with price elasticity calibration.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Disposal Fees</span>
              <span class="badge badge-amber">Anti-Inflation</span>
            </div>
            <div class="doc-card-desc">Warehouse overfilling triggers waste disposal fees, charging players CBX instead of rewarding infinite dumping.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Create Mod Kinetics</span>
              <span class="badge badge-green">Kinetics</span>
            </div>
            <div class="doc-card-desc">Kinetic acceleration of trade docks and live quotes beamed to Display Boards and Nixie Tubes.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>CC: Tweaked Lua API</span>
              <span class="badge badge-blue">Automation</span>
            </div>
            <div class="doc-card-desc">Full peripheral registration with methods for quote monitors, automated arbitrage bots, and emergency stops.</div>
          </div>
        </div>

        <h2>Persistence & Item Safety</h2>
        <p>The mod adheres to strict item safety and database transactional guarantees:</p>
        <ul>
          <li><strong>Atomic SQLite Operations:</strong> Every balance alteration and transaction is recorded in WAL mode to eliminate desyncs.</li>
          <li><strong>Unclaimed Deliveries Buffer:</strong> If a player is offline or has a full inventory when a limit order or market delivery executes, items are safely stored in an SQL queue and delivered upon next login or terminal interaction.</li>
          <li><strong>P2P Anti-Scam Protocol:</strong> Mutual trade locking with a 3-second countdown before finalizing transfers.</li>
        </ul>
      `
    },

    "amm-economics": {
      title: "AMM Economic Model",
      subtitle: "The mathematics of bonding curves, slippage calculation, ecological fees, and reputation tiers.",
      breadcrumbs: ["Getting Started", "Economic Model"],
      html: `
        <h2>1. AMM Pricing Formula</h2>
        <p>The instantaneous spot price $P(S)$ is computed based on the current warehouse stock $S$ relative to the target stock $S_{target}$:</p>

        <div class="formula-box">
          P(S) = P₀ × ( S_target / S )^k
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Symbol</th>
                <th>Parameter</th>
                <th>Description</th>
                <th>Standard Value</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>P₀</code></td>
                <td>Equilibrium Price</td>
                <td>Target unit price in CBX when warehouse is at exact target capacity.</td>
                <td>10.0 CBX (iron), 150.0 CBX (diamond)</td>
              </tr>
              <tr>
                <td><code>S_target</code></td>
                <td>Target Reserve</td>
                <td>Standard benchmark warehouse volume.</td>
                <td>10,000 units (iron ingots)</td>
              </tr>
              <tr>
                <td><code>S</code></td>
                <td>Current Stock</td>
                <td>Actual available commodity units in the liquidity pool.</td>
                <td>Dynamic count</td>
              </tr>
              <tr>
                <td><code>k</code></td>
                <td>Elasticity Coefficient</td>
                <td>Slope factor of the demand curve. Higher values increase volatility.</td>
                <td>0.75 – 0.95</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Numerical Example</span>
          </div>
          <div class="callout-content">
            Given $P_0 = 10.0$, $S_{target} = 10\\,000$, $k = 0.85$:<br>
            • Stock matches target ($S = 10\\,000$): price is exactly <strong>10.00 CBX</strong>.<br>
            • Stock depleted by half ($S = 5\\,000$): price climbs to <strong>18.03 CBX</strong>.<br>
            • Stock swollen to 15,000 ingots: price drops to <strong>7.08 CBX</strong>.
          </div>
        </div>

        <h2>2. Price Slippage in Batch Orders</h2>
        <p>The spot quote $P(S)$ applies to 1 individual unit. When trading large quantities (64 items or 2,304 items), each unit changes pool reserves. The average executed price differs from the initial quote.</p>
        <p>The exchange calculates the exact order cost via integration over the bonding curve:</p>

        <div class="formula-box">
          Cost(Q) = ∫ [ S to S - Q ] P(x) dx
        </div>

        <p>The terminal's <strong>MAX</strong> button utilizes binary search to compute the exact maximum affordable quantity based on your balance.</p>

        <h2>3. Ecological Disposal Fee & Negative Prices</h2>
        <p>Each commodity defines a maximum reserve limit $S_{max}$ (default $1.5 \\times S_{target}$). If stock surpasses this ceiling, unit prices turn negative:</p>

        <div class="formula-box">
          C_disposal = α × [ (S - S_max) / S_target ]
        </div>

        <div class="callout callout-warning">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>Automated Farm Warning</span>
          </div>
          <div class="callout-content">
            Piping automated farm output directly into an exchange dock without setting a Stop-Loss price will result in <strong>disposal fee deductions</strong> from your balance when warehouses overflow.
          </div>
        </div>

        <h2>4. Surplus Drain & Mean Reversion</h2>
        <p>Every in-game day (24,000 ticks / 20 real minutes), the exchange burns <strong>8%</strong> of surplus inventory above $S_{target}$, simulating municipal infrastructure consumption and returning prices toward equilibrium.</p>

        <h2>5. Trader Reputation & Fee Tiers</h2>
        <p>Trading volume generates Reputation Points (<strong>REP</strong>): <code>1 REP = 10 CBX in trade volume</code>. Higher tiers enjoy up to a 4x reduction in exchange fees:</p>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Tier</th>
                <th>Rank</th>
                <th>REP Required</th>
                <th>Trading Fee</th>
                <th>Perks</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><span class="badge">I</span></td>
                <td><strong>Novice</strong></td>
                <td>0 – 99</td>
                <td>2.0%</td>
                <td>Base terminal access, 1-item purchase dock.</td>
              </tr>
              <tr>
                <td><span class="badge">II</span></td>
                <td><strong>Trader</strong></td>
                <td>100 – 499</td>
                <td>1.6%</td>
                <td>Purchase dock batch size up to 16 items.</td>
              </tr>
              <tr>
                <td><span class="badge">III</span></td>
                <td><strong>Broker</strong></td>
                <td>500 – 1,999</td>
                <td>1.2%</td>
                <td>Stop-High price ceiling safety in purchase docks.</td>
              </tr>
              <tr>
                <td><span class="badge">IV</span></td>
                <td><strong>Investor</strong></td>
                <td>2,000 – 9,999</td>
                <td>0.8%</td>
                <td>Bulk purchase batches up to 64 items (full stack).</td>
              </tr>
              <tr>
                <td><span class="badge badge-green">V</span></td>
                <td><strong>Whale</strong></td>
                <td>10,000+</td>
                <td><strong>0.5%</strong></td>
                <td>Ultra-low trading fee for high-frequency arbitrage.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>6. Unallocated Metal Accounts (OMS)</h2>
        <p>OMS enables synthetic exposure to raw commodities without physical chest storage:</p>
        <ul>
          <li><strong>Position Opening:</strong> Buy virtual units at current spot rates using CBX.</li>
          <li><strong>Live PnL:</strong> Real-time unrealized profit/loss display (green <code>+X.XX CBX</code> / red <code>-X.XX CBX</code>).</li>
          <li><strong>Closing:</strong> Instantly close positions to realize profit directly to your account.</li>
          <li><strong>Carry Fee:</strong> A tiny <code>0.05%</code> daily maintenance fee is charged at midnight.</li>
        </ul>
      `
    },

    "calculator": {
      title: "Interactive AMM Simulator",
      subtitle: "Experiment with Bonding Curve dynamics, batch slippage, and disposal fees live.",
      breadcrumbs: ["Getting Started", "Interactive Calculator"],
      html: `
        <p>Use the interactive tool below to simulate different market conditions: from raw material shortages to warehouse overflows, and evaluate average prices for bulk orders.</p>
        
        <div id="amm-calc-mount"></div>
      `
    },

    "blocks-terminal": {
      title: "Exchange Terminal",
      subtitle: "The primary trading workstation, candlestick charts, portable PDA mode, and 4 order desks.",
      breadcrumbs: ["Gameplay Mechanics", "Exchange Terminal"],
      html: `
        <h2>Overview & Portability</h2>
        <p>The <strong>Exchange Terminal</strong> (<code>ammora:exchange_terminal</code>) is the central interaction point with the market:</p>
        <ul>
          <li><strong>Block Mode:</strong> Placeable in the world and interactable with redstone and ComputerCraft peripherals.</li>
          <li><strong>Handheld PDA Mode:</strong> Hold the terminal item and <strong>Right-Click in the air</strong> to open the full UI anywhere without placing the block!</li>
        </ul>

        <h2>Analog Redstone Output (0–15)</h2>
        <p>The terminal outputs a redstone signal proportional to the warehouse fill percentage of the monitored commodity ($0\\% = 0$, $100\\% = 15$).</p>
        <ul>
          <li>Outputs to adjacent redstone dust, repeaters, and comparators.</li>
          <li>Binds to the commodity selected in the GUI, or by holding an item and <strong>Shift + Right-Clicking</strong> the block in world.</li>
        </ul>

        <h2>Trading Desks</h2>
        <div class="cards-grid">
          <div class="doc-card">
            <div class="doc-card-title"><span>1. SPOT</span></div>
            <div class="doc-card-desc">Direct physical exchange from player inventory with batch buttons (+1, +16, +64, MAX).</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>2. OMS</span></div>
            <div class="doc-card-desc">Synthetic metal accounts with live PnL monitoring and profit-taking buttons.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>3. LIMIT</span></div>
            <div class="doc-card-desc">Order book for BUY_LIMIT and SELL_LIMIT with escrow safety guarantees.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>4. GOV</span></div>
            <div class="doc-card-desc">Government supply contracts offering +15–25% price bonuses.</div>
          </div>
        </div>

        <h2>Real-Time Candlestick Chart</h2>
        <p>The terminal features a Bybit Pro style candlestick chart:</p>
        <ul>
          <li><strong>30-second Candles:</strong> Green for bullish periods, red for bearish drops.</li>
          <li><strong>Volume Bars:</strong> Lower histogram indicates executed trade volume.</li>
          <li><strong>Mouse Wheel Zoom:</strong> Smooth zooming into price history.</li>
        </ul>
      `
    },

    "blocks-logistics": {
      title: "Trade & Purchase Docks",
      subtitle: "Industrial automation blocks for hands-free selling and automated supply chains.",
      breadcrumbs: ["Gameplay Mechanics", "Logistics Docks"],
      html: `
        <h2>1. Trade Dock</h2>
        <p>The <strong>Trade Dock</strong> (<code>ammora:trade_dock</code>) automatically sells incoming items into the AMM liquidity pool:</p>
        <ul>
          <li>Accepts items from hoppers, droppers, Create belts, chutes, and modded pipes.</li>
          <li>Items are immediately sold and CBX proceeds are deposited into the owner's account.</li>
          <li><strong>Stop-Loss Protection:</strong> Configure a minimum price threshold. If market prices fall below this floor (or into disposal penalties), the dock locks and halts intake.</li>
          <li><strong>Comparator Output:</strong> Emits redstone when stop-loss triggers or internal buffer overflows.</li>
        </ul>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Crafting Recipe</span>
          </div>
          <div class="callout-content">
            2 Hoppers + 1 Barrel + 4 Iron Ingots + 1 Exchange Terminal (<code>ammora:exchange_terminal</code>) + 1 Redstone Block.
          </div>
        </div>

        <h2>2. Purchase Dock</h2>
        <p>The <strong>Purchase Dock</strong> (<code>ammora:purchase_dock</code>) purchases configured resources from the market into its 9-slot buffer on redstone pulses:</p>
        <ul>
          <li>Triggers on rising redstone edges or continuously once per second while powered.</li>
          <li>Costs are deducted from the owner's account balance.</li>
        </ul>

        <h3>Rank Gating Safeguards</h3>
        <p>To avoid market disruption, purchase capabilities scale with player reputation tiers:</p>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Reputation Tier</th>
                <th>Max Batch / Pulse</th>
                <th>Unlocked Features</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Tier I (Novice)</td>
                <td>1 item</td>
                <td>Basic single-item acquisition.</td>
              </tr>
              <tr>
                <td>Tier II (Trader)</td>
                <td>Up to 16 items</td>
                <td>Batch buttons: 4, 8, 16 items.</td>
              </tr>
              <tr>
                <td>Tier III (Broker)</td>
                <td>Up to 16 items</td>
                <td><strong>Stop-High Fuse:</strong> Configurable maximum purchase price cap.</td>
              </tr>
              <tr>
                <td>Tier IV (Investor)</td>
                <td><strong>Up to 64 items</strong></td>
                <td>Bulk buttons: 32 and 64 items (full stack per pulse).</td>
              </tr>
            </tbody>
          </table>
        </div>
      `
    },

    "cold-wallet": {
      title: "Cold Wallet & P2P Transfers",
      subtitle: "Portable cryptographic ledger device for wireless payments, proximity radar, and audit history.",
      breadcrumbs: ["Gameplay Mechanics", "Cold Wallet"],
      html: `
        <h2>Cold Wallet Features</h2>
        <p>The <strong>Cold Wallet</strong> (<code>ammora:cold_wallet</code>) is a handheld cyber-terminal for instant financial operations:</p>
        <ul>
          <li><strong>30-Block Proximity Radar:</strong> Automatically scans surrounding players and lists their name and distance in blocks. Clicking any player sets them as the recipient.</li>
          <li><strong>Manual Name Input:</strong> Text edit box to send funds to any player across the server, even outside radar range.</li>
          <li><strong>Zero Fee P2P:</strong> Transfers carry a 0% network fee.</li>
          <li><strong>Quick Amount Grid:</strong> Increment buttons <code>[+10]</code>, <code>[+50]</code>, <code>[+100]</code>, <code>[+500]</code>, <code>[MAX]</code>, and Reset.</li>
        </ul>

        <h2>Unified Ledger Audit</h2>
        <p>The "Ledger History" tab displays a complete chronological audit of all transactions:</p>
        <ul>
          <li>Incoming and outgoing P2P transfers.</li>
          <li>Spot trades executed on terminals.</li>
          <li>OMS investment position results and carry fees.</li>
        </ul>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Crafting Recipe</span>
          </div>
          <div class="callout-content">
            1 Compass + 1 Iron Ingot + 2 Redstone Dust + 1 Gold Nugget.
          </div>
        </div>
      `
    },

    "vending-marketplace": {
      title: "Vending Machine & Global Marketplace",
      subtitle: "Rust-style player vending machines, anti-grief mechanics, TNT raids, and the market tablet.",
      breadcrumbs: ["Gameplay Mechanics", "Vending & Market"],
      html: `
        <h2>1. Player Vending Machine</h2>
        <p>The <strong>Player Vending Machine</strong> (<code>ammora:player_vending_machine</code>) lets players establish private storefronts:</p>
        <ul>
          <li><strong>Owner Binding:</strong> Permanently bound to the creator's UUID.</li>
          <li><strong>10 Showcase Slots:</strong> Configurable item, lot quantity, and price in CBX.</li>
          <li><strong>Revenue Safe:</strong> Earned CBX accumulates safely in the machine until withdrawn by the owner.</li>
        </ul>

        <h3>Rust-Style Protection & Raiding</h3>
        <div class="callout callout-warning">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>Anti-Griefing & TNT Raids</span>
          </div>
          <div class="callout-content">
            The vending machine cannot be broken by hand or pickaxes of other players (bedrock hardness).<br>
            The only method to breach an opponent's machine is via <strong>TNT explosions</strong>. An explosion destroys the machine and drops all stored contents as loose loot! Owners can safely dismantle their own machine anytime.
          </div>
        </div>

        <h2>2. Market Tablet</h2>
        <p>The <strong>Market Tablet</strong> (<code>ammora:market_tablet</code>) provides remote access to all player vending machines worldwide:</p>
        <ul>
          <li>Browse listings from all active vending machines on the server.</li>
          <li>Remote purchases delivered to the secure delivery buffer.</li>
          <li>Post Request-For-Quote (RFQ) buy orders.</li>
        </ul>
      `
    },

    "atm-cash": {
      title: "ATM Terminal & Physical Cash",
      subtitle: "2-block tall industrial banking terminal, physical banknotes, live cash breakdown, and corporate budgets.",
      breadcrumbs: ["Gameplay Mechanics", "ATM & Cash"],
      html: `
        <h2>1. ATM Terminal</h2>
        <p>The <strong>ATM Terminal</strong> (<code>ammora:atm</code>) is a 2-block tall industrial banking station crafted with polished deepslate and brass accents for cash banking:</p>
        <ul>
          <li><strong>World Placement:</strong> 2 blocks high. Placing the bottom block automatically deploys the matching top unit with seamless industrial styling.</li>
          <li><strong>Account Switching:</strong> Easily toggle between personal wallet and corporate treasury in the upper header.</li>
          <li><strong>Corporate Budget Enforcement:</strong> Non-owner corporate members are subject to daily spending limits, displayed dynamically (e.g. <code>5/100 CBX</code>).</li>
        </ul>

        <h2>2. Cash Withdrawal</h2>
        <p>Convert digital CBX account balance into physical banknotes:</p>
        <ul>
          <li><strong>Auto-Adjustment:</strong> Entered amounts automatically snap to multiples of 10 CBX upon pressing Enter, losing focus, or clicking Withdraw.</li>
          <li><strong>Preset Grid:</strong> Quick selection buttons in a clean 2-column layout: <code>[10]</code>, <code>[50]</code>, <code>[100]</code>, <code>[500]</code>, <code>[1,000]</code>, <code>[5,000 CBX]</code>.</li>
          <li><strong>Breakdown Card:</strong> Dynamically calculates and displays the exact count of 1000, 100, and 10 CBX banknotes received.</li>
        </ul>

        <h2>3. Cash Deposit</h2>
        <p>Deposit physical cash back into digital account balances:</p>
        <ul>
          <li><strong>"Deposit All Cash" Button:</strong> Automatically scans player inventory for 10, 100, and 1000 CBX banknotes, tallies total sum on the button, and credits the balance in one click.</li>
          <li><strong>Inventory Sync:</strong> Real-time container sync removes cash from slots and updates button states immediately.</li>
        </ul>

        <h2>4. Physical Currency (Banknotes, Stacks, Blocks)</h2>
        <div class="table-responsive">
          <table class="docs-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Denomination</th>
                <th>Description & Recipes</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>10 CBX Banknote</strong></td>
                <td>10 CBX</td>
                <td>Zinc-tinted industrial bill with CBX insignia. Base unit.</td>
              </tr>
              <tr>
                <td><strong>100 CBX Banknote</strong></td>
                <td>100 CBX</td>
                <td>Cyan-tinted currency note with distinctive monetary emblem.</td>
              </tr>
              <tr>
                <td><strong>1 000 CBX Banknote</strong></td>
                <td>1 000 CBX</td>
                <td>High-value brass/gold bill for large-scale commerce.</td>
              </tr>
              <tr>
                <td><strong>Money Stack</strong></td>
                <td>90 / 900 / 9 000 CBX</td>
                <td>Compact bundle of 9 banknotes (3x3 crafting grid). Crafting back returns 9 banknotes.</td>
              </tr>
              <tr>
                <td><strong>Money Block</strong></td>
                <td>810 / 8 100 / 81 000 CBX</td>
                <td>Solid decorative currency block formed from 9 money stacks for bank vaults. Reversible into 9 stacks.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>5. Corporate Audit Tracking</h2>
        <p>All corporate cash deposits and withdrawals are permanently recorded in the immutable company audit ledger as <code>ATM_WITHDRAW</code> and <code>ATM_DEPOSIT</code> with exact member attribution and amounts.</p>
      `
    },

    "cc-tweaked": {
      title: "CC: Tweaked (ComputerCraft) Integration",
      subtitle: "Connect mod blocks as Lua peripherals, complete API documentation, and ready-to-run automation scripts.",
      breadcrumbs: ["Integrations", "CC: Tweaked"],
      html: `
        <h2>Peripheral Connection</h2>
        <p>Ammora features native peripheral capability for <strong>CC: Tweaked</strong>. Connect via wired modems or direct adjacent placement:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal") or peripheral.find("exchange_dock")

if not exchange then
    error("Ammora peripheral not found! Verify modem cables.")
end
print("Connected to Ammora Exchange!")</code></pre>
        </div>

        <h2>Lua API Reference</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Method</th>
                <th>Parameters</th>
                <th>Returns</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>getPrice(itemId)</code></td>
                <td><code>itemId (string)</code></td>
                <td><code>table</code></td>
                <td>Returns <code>spotPrice</code>, <code>buyPrice</code>, <code>sellPrice</code>, <code>disposalFee</code>.</td>
              </tr>
              <tr>
                <td><code>getStock(itemId)</code></td>
                <td><code>itemId (string)</code></td>
                <td><code>table</code></td>
                <td>Returns <code>currentStock</code>, <code>targetReserve</code>, <code>maxReserve</code>, <code>status</code>.</td>
              </tr>
              <tr>
                <td><code>getAccount(playerUuid)</code></td>
                <td><code>playerUuid (string)</code></td>
                <td><code>table</code></td>
                <td>Returns <code>balance</code>, <code>reputation</code>, <code>rank</code>, <code>feeRate</code>.</td>
              </tr>
              <tr>
                <td><code>buy(itemId, amount)</code></td>
                <td><code>itemId (string), amount (number)</code></td>
                <td><code>boolean, string</code></td>
                <td>Purchases specified item quantity from exchange.</td>
              </tr>
              <tr>
                <td><code>sell(amount)</code></td>
                <td><code>amount (number)</code></td>
                <td><code>boolean, string</code></td>
                <td>Sells items from trade dock buffer into exchange.</td>
              </tr>
              <tr>
                <td><code>getStopLoss()</code></td>
                <td>none</td>
                <td><code>number</code></td>
                <td>Returns configured stop-loss floor on trade dock.</td>
              </tr>
              <tr>
                <td><code>setStopLoss(price)</code></td>
                <td><code>price (number)</code></td>
                <td><code>boolean</code></td>
                <td>Sets stop-loss floor price on trade dock.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>Example 1: Wall Quote Monitor (exchange_wall.lua)</h2>
        <p>Displays live market prices with color-coded alerts on an Advanced Monitor:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal")
local mon = peripheral.find("monitor")

if not exchange or not mon then
    error("Requires exchange_terminal and monitor peripherals!")
end

mon.setTextScale(1)
mon.clear()

local items = {
    "minecraft:iron_ingot",
    "minecraft:gold_ingot",
    "minecraft:diamond",
    "minecraft:copper_ingot"
}

while true do
    mon.clear()
    mon.setCursorPos(1, 1)
    mon.setTextColor(colors.yellow)
    mon.write("=== Ammora Exchange Ticker ===")
    
    for i, id in ipairs(items) do
        local price = exchange.getPrice(id)
        local stock = exchange.getStock(id)
        
        mon.setCursorPos(1, i + 2)
        mon.setTextColor(colors.white)
        mon.write(string.format("%-18s ", price.displayName or id))
        
        if price.sellPrice < 0 then
            mon.setTextColor(colors.purple)
            mon.write(string.format("FEE: %.2f", price.disposalFee))
        else
            mon.setTextColor(colors.green)
            mon.write(string.format("%7.2f CBX", price.spotPrice))
        end
        
        mon.setTextColor(colors.gray)
        mon.write(string.format(" (stock: %d)", stock.currentStock))
    end
    
    sleep(3)
end</code></pre>
        </div>

        <h2>Example 2: Automated Arbitrage Bot (arbitrage_bot.lua)</h2>
        <p>Automatically buys the dip and sells during price rallies:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal")
local TARGET_ITEM = "minecraft:iron_ingot"
local BUY_THRESHOLD = 8.00
local SELL_THRESHOLD = 13.00
local BATCH_SIZE = 64

print("Starting Ammora Trading Bot...")

while true do
    local q = exchange.getPrice(TARGET_ITEM)
    print(string.format("[%s] Spot: %.2f CBX", os.date("%X"), q.spotPrice))

    if q.spotPrice <= BUY_THRESHOLD then
        print("-> BUY SIGNAL! Market dipped.")
        local ok, err = exchange.buy(TARGET_ITEM, BATCH_SIZE)
        if ok then
            print("Purchased 64 units at discount.")
        else
            print("Buy failed: " .. tostring(err))
        end
    elseif q.spotPrice >= SELL_THRESHOLD then
        print("-> SELL SIGNAL! Price spiked.")
        local ok, err = exchange.sell(BATCH_SIZE)
        if ok then
            print("Sold 64 units with profit.")
        else
            print("Sell failed: " .. tostring(err))
        end
    end

    sleep(5)
end</code></pre>
        </div>

        <h2>Example 3: Quarry Safety Shutoff (quarry_safety.lua)</h2>
        <p>Cuts redstone power to an industrial quarry if exchange storage overflows:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>local dock = peripheral.find("exchange_dock")
local REDSTONE_SIDE = "back"
local MIN_SAFE_PRICE = 4.00

while true do
    local stock = dock.getStock("minecraft:iron_ingot")
    local price = dock.getPrice("minecraft:iron_ingot")
    
    if price.sellPrice < MIN_SAFE_PRICE or stock.status == "OVERFLOW" then
        print("[ALARM] Stock overflow! Disabling quarry power.")
        redstone.setOutput(REDSTONE_SIDE, false)
    else
        redstone.setOutput(REDSTONE_SIDE, true)
    end
    
    sleep(10)
end</code></pre>
        </div>
      `
    },

    "create-mod": {
      title: "Create Mod Integration",
      subtitle: "Kinetic acceleration of trade docks, live Display Link flapper boards, and interactive Ponder scenes.",
      breadcrumbs: ["Integrations", "Create Mod"],
      html: `
        <h2>1. Kinetic Dock Acceleration</h2>
        <p>When <strong>Create</strong> is installed, the Trade Dock accepts rotational kinetic power:</p>
        <ul>
          <li>Connect a Shaft to the side of the Trade Dock (Stress Impact: 4.0 SU per 1 RPM).</li>
          <li><strong>Processing Speedup:</strong> Standard dock speed is 1 transaction per second (20 ticks). Rotational speed decreases delay:
            <br><code>Delay (ticks) = max(2, 20 - floor(RPM / 16))</code></li>
          <li>At 256 RPM, items sell every <strong>2 ticks</strong> (10 operations per second), easily consuming output from high-speed Create belts!</li>
        </ul>

        <h2>2. Live Display Link Quotes</h2>
        <p>The Exchange Terminal serves as a native Display Source for Create's Display Link:</p>
        <ul>
          <li>Attach a <strong>Display Link</strong> to the Exchange Terminal and point it at a <strong>Flap Display</strong> or <strong>Nixie Tubes</strong>.</li>
          <li><strong>Available Output Modes:</strong>
            <ul>
              <li><em>Spot Quote:</em> Live price per item in CBX with trend indicator arrows (↑ / ↓).</li>
              <li><em>Stock Inventory:</em> Units remaining on the exchange and capacity percentage.</li>
              <li><em>News Ticker:</em> Active server market headlines and crises.</li>
            </ul>
          </li>
        </ul>

        <h2>3. In-Game 3D Ponder Scenes</h2>
        <p>Hold the <strong>'W'</strong> key while hovering over the <strong>Exchange Terminal</strong> or <strong>Trade Dock</strong> in your inventory to launch an interactive 3D Ponder visualization of block mechanics.</p>
      `
    },

    "datapacks": {
      title: "Datapacks Guide",
      subtitle: "Customize commodities, market events, and supply contracts using standard Minecraft datapacks.",
      breadcrumbs: ["Configuration & Server", "Datapacks"],
      html: `
        <h2>Datapack Directory Structure</h2>
        <p>Ammora supports full economic configuration through standard Minecraft datapacks without modifying Java code. Datapacks reload dynamically via <code>/reload</code>.</p>

        <div class="code-block-wrapper">
          <div class="code-header"><span class="code-lang-label">DIRECTORY TREE</span></div>
          <pre class="code-block"><code>your_datapack/
├── pack.mcmeta
└── data/
    └── <namespace>/
        └── exchange/
            ├── commodities/           &lt;-- Tradable items
            │   ├── brass.json
            │   └── copper.json
            ├── market_events/         &lt;-- News & crisis events
            │   ├── gold_rush.json
            │   └── tech_boom.json
            └── delivery_contracts/    &lt;-- Supply bounties
                ├── steel_delivery.json
                └── food_drive.json</code></pre>
        </div>

        <h2>1. Commodities (commodities/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (brass.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>{
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
}</code></pre>
        </div>

        <h2>2. Market Events (market_events/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (gold_rush.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>{
  "id": "gold_rush",
  "title": "Gold Rush",
  "description": "Ancient treasury uncovered! Massive influx of gold drives market prices down by 30%.",
  "affectedResource": "minecraft:gold_ingot",
  "priceMultiplier": 0.70,
  "durationDays": 2,
  "weight": 10
}</code></pre>
        </div>

        <h2>3. Delivery Contracts (delivery_contracts/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (steel_delivery.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>{
  "id": "railway_expansion",
  "title": "Gov Contract: Railway Infrastructure",
  "description": "Municipality purchases steel ingots with a +20% price bonus.",
  "targetResource": "minecraft:iron_ingot",
  "requiredQuantity": 1024,
  "rewardCBX": 15000.0,
  "collateralCBX": 1500.0,
  "timeLimitDays": 3,
  "minTraderRank": 2
}</code></pre>
        </div>
      `
    },

    "custom-items": {
      title: "Custom & Modded Items",
      subtitle: "Integrate items from Create, Mekanism, Thermal, and Botania via server configuration.",
      breadcrumbs: ["Configuration & Server", "Custom Items"],
      html: `
        <h2>Configuration File</h2>
        <p>Custom items can be added directly via server configuration file:</p>
        <p><code>config/ammora_custom_items.json</code></p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Copy</span>
            </button>
          </div>
          <pre class="code-block"><code>[
  {
    "resourceId": "mekanism:ingot_osmium",
    "displayName": "Osmium Ingot",
    "basePrice": 35.0,
    "targetReserve": 4000.0,
    "elasticity": 0.85
  },
  {
    "resourceId": "thermal:tin_ingot",
    "displayName": "Tin Ingot",
    "basePrice": 8.5,
    "targetReserve": 12000.0
  }
]</code></pre>
        </div>

        <h2>Calibration Reference Table</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Parameter</th>
                <th>Type</th>
                <th>Required</th>
                <th>Recommended</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>resourceId</code></td>
                <td>String</td>
                <td><strong>Yes</strong></td>
                <td><code>modid:item_name</code></td>
                <td>Minecraft registry name.</td>
              </tr>
              <tr>
                <td><code>basePrice</code></td>
                <td>Double</td>
                <td><strong>Yes</strong></td>
                <td>1.0 – 5000.0</td>
                <td>Equilibrium price at 100% target stock.</td>
              </tr>
              <tr>
                <td><code>targetReserve</code></td>
                <td>Double</td>
                <td><strong>Yes</strong></td>
                <td>1,000 – 20,000</td>
                <td>Target warehouse reserve volume.</td>
              </tr>
              <tr>
                <td><code>elasticity</code></td>
                <td>Double</td>
                <td>No</td>
                <td>0.80 (0.70 – 0.95)</td>
                <td>Bonding curve slope coefficient.</td>
              </tr>
              <tr>
                <td><code>maxReserve</code></td>
                <td>Double</td>
                <td>No</td>
                <td><code>targetReserve * 1.5</code></td>
                <td>Disposal fee activation threshold.</td>
              </tr>
            </tbody>
          </table>
        </div>
      `
    },

    "admin-console": {
      title: "Admin Console (/ammora admin)",
      subtitle: "Operator GUI management console, CLI commands, and SQLite transactional audit log.",
      breadcrumbs: ["Configuration & Server", "Admin Console"],
      html: `
        <h2>Access & Security</h2>
        <p>Open the console using <code>/ammora admin</code>. Execution requires Minecraft OP level 2 permissions (<code>player.hasPermissions(2)</code>).</p>

        <h2>Console Modules</h2>
        <ul>
          <li><strong>Balances:</strong> Inspect all player balances, search by name, set/add CBX, and edit reputation scores.</li>
          <li><strong>Events:</strong> Trigger or cancel macroeconomic events and crises manually.</li>
          <li><strong>AMM Rates:</strong> View live warehouse stock for all commodities, adjust $P_0$ and $S_{target}$, or reset reserves.</li>
          <li><strong>Audit Logs:</strong> Paginated transaction ledger directly from SQLite with timestamp and player filters.</li>
        </ul>

        <h2>Operator Chat Commands</h2>
        <div class="code-block-wrapper">
          <div class="code-header"><span class="code-lang-label">BASH / COMMANDS</span></div>
          <pre class="code-block"><code># Set player balance directly
/ammora balance set &lt;player&gt; 50000.0

# Add funds to account
/ammora balance add &lt;player&gt; 2500.0

# Trigger a market event
/ammora event trigger gold_rush

# Reset commodity stock to target
/ammora calibrate reset minecraft:iron_ingot</code></pre>
        </div>
      `
    },

    "localization": {
      title: "Localization & i18n",
      subtitle: "Multilingual architecture, format specifiers, and resource pack translations.",
      breadcrumbs: ["Configuration & Server", "Localization"],
      html: `
        <h2>Localization Standard</h2>
        <p>Ammora maintains bilingual support with 100% key and placeholder parity:</p>
        <ul>
          <li><code>en_us.json</code> — English international reference standard.</li>
          <li><code>ru_ru.json</code> — Full Russian localization for all screens, tooltips, and alerts.</li>
        </ul>

        <h2>Adding Translations via Resource Packs</h2>
        <p>Add new language translations without compiling code by placing your lang JSON into a standard resource pack:</p>
        <p><code>your_resourcepack/assets/ammora/lang/&lt;language_code&gt;.json</code></p>

        <div class="callout callout-info">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            <span>Format Specifier Parity</span>
          </div>
          <div class="callout-content">
            Specifier counts and types (<code>%s</code> for strings, <code>%d</code> for integers, <code>%.2f</code> for decimals) must strictly match <code>en_us.json</code>.
          </div>
        </div>
      `
    },

    "faq": {
      title: "FAQ & Troubleshooting",
      subtitle: "Frequently asked questions regarding mechanics, item safety, and edge cases.",
      breadcrumbs: ["Configuration & Server", "FAQ"],
      html: `
        <h2>What happens to items if a player is offline during order execution?</h2>
        <p>The mod provides an <strong>Unclaimed Deliveries</strong> guarantee. Items are safely buffered in the SQLite database and granted upon the player's next login or financial block interaction.</p>

        <h2>Why did the Trade Dock price turn negative?</h2>
        <p>The warehouse exceeded maximum capacity ($S > S_{max}$) due to surplus item influx. The exchange began charging a disposal fee. Set a <strong>Stop-Loss</strong> price floor in the dock to prevent unintended fees.</p>

        <h2>How are Player Vending Machines protected against griefing?</h2>
        <p>Machines are immune to mining picks and fists of other players. They can only be raided via <strong>TNT explosions</strong>. Build in protected territory or reinforced bunkers.</p>

        <h2>Can Ammora run on servers without Create and ComputerCraft?</h2>
        <p>Yes. Integrations are fully modular and optional. Ammora functions seamlessly as a standalone economy mod.</p>
      `
    }
  }
};
