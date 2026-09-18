/**
 * Ammora Interactive AMM Bonding Curve & Slippage Simulator
 */
(function() {
  const calcState = {
    p0: 10.0,
    sTarget: 10000,
    stock: 8500,
    k: 0.85,
    orderQty: 64,
    orderType: "BUY" // "BUY" or "SELL"
  };

  const PRESETS = {
    iron: { p0: 10.0, sTarget: 10000, stock: 9200, k: 0.85, qty: 64 },
    gold: { p0: 35.0, sTarget: 4000, stock: 3800, k: 0.85, qty: 64 },
    diamond: { p0: 150.0, sTarget: 1000, stock: 850, k: 0.90, qty: 16 },
    overflow: { p0: 10.0, sTarget: 10000, stock: 16200, k: 0.85, qty: 64 },
    shortage: { p0: 10.0, sTarget: 10000, stock: 1800, k: 0.85, qty: 64 }
  };

  const I18N = {
    ru: {
      title: "Интерактивный симулятор AMM и калькулятор проскальзывания",
      presets: "Пресеты:",
      presetIron: "Железо",
      presetGold: "Золото",
      presetDiamond: "Алмазы",
      presetOverflow: "Переполнение склада",
      presetShortage: "Острый дефицит",
      basePrice: "Базовая цена (P₀):",
      targetReserve: "Целевой резерв (S_target):",
      currentStock: "Текущий склад (S):",
      elasticity: "Эластичность (k):",
      orderType: "Тип сделки:",
      buy: "Покупка (BUY)",
      sell: "Продажа (SELL)",
      orderQty: "Размер партии (шт):",
      outputSpot: "Спотовая котировка:",
      outputAvg: "Средняя цена за единицу:",
      outputTotal: "Итоговая сумма:",
      outputSlippage: "Проскальзывание (Slippage):",
      outputStockFill: "Заполнение склада:",
      statusNormal: "Склад в норме",
      statusLow: "Дефицит — цена повышена",
      statusOverflow: "ПЕРЕПОЛНЕНИЕ! Взимается сбор за утилизацию",
      disposalFee: "Утилизационный сбор:"
    },
    en: {
      title: "Interactive AMM Bonding Curve & Slippage Simulator",
      presets: "Presets:",
      presetIron: "Iron Ingot",
      presetGold: "Gold Ingot",
      presetDiamond: "Diamond",
      presetOverflow: "Stock Overflow",
      presetShortage: "Severe Shortage",
      basePrice: "Equilibrium Price (P₀):",
      targetReserve: "Target Reserve (S_target):",
      currentStock: "Current Stock (S):",
      elasticity: "Elasticity (k):",
      orderType: "Order Type:",
      buy: "Buy (BUY)",
      sell: "Sell (SELL)",
      orderQty: "Order Quantity (units):",
      outputSpot: "Instant Spot Quote:",
      outputAvg: "Average Executed Price:",
      outputTotal: "Total Order Cost:",
      outputSlippage: "Price Slippage:",
      outputStockFill: "Warehouse Stock Level:",
      statusNormal: "Healthy inventory reserve",
      statusLow: "Shortage — elevated prices",
      statusOverflow: "OVERFLOW! Waste disposal fee active",
      disposalFee: "Waste disposal fee:"
    }
  };

  function computePricing() {
    const { p0, sTarget, stock, k, orderQty, orderType } = calcState;
    const maxReserve = sTarget * 1.5;
    const alpha = 20.0;

    // Spot price
    let spot = p0 * Math.pow(sTarget / Math.max(1, stock), k);
    let isOverflow = stock > maxReserve;
    let disposalFee = 0;
    if (isOverflow) {
      disposalFee = alpha * ((stock - maxReserve) / sTarget);
    }

    // Integral calculation for batch order
    let totalCost = 0;
    let avgPrice = 0;
    let slippagePercent = 0;

    if (orderType === "BUY") {
      const q = Math.min(orderQty, Math.max(0, stock - 1));
      if (q > 0) {
        // Integrate P(x) from (stock - q) to stock
        if (Math.abs(k - 1.0) < 0.001) {
          totalCost = p0 * sTarget * Math.log(stock / (stock - q));
        } else {
          const mult = (p0 * Math.pow(sTarget, k)) / (1.0 - k);
          totalCost = mult * (Math.pow(stock, 1.0 - k) - Math.pow(stock - q, 1.0 - k));
        }
        avgPrice = totalCost / q;
        slippagePercent = ((avgPrice - spot) / spot) * 100;
      }
    } else {
      // SELL
      const q = orderQty;
      if (isOverflow) {
        totalCost = -disposalFee * q;
        avgPrice = -disposalFee;
        slippagePercent = 0;
      } else {
        // Integrate P(x) from stock to (stock + q)
        if (Math.abs(k - 1.0) < 0.001) {
          totalCost = p0 * sTarget * Math.log((stock + q) / stock);
        } else {
          const mult = (p0 * Math.pow(sTarget, k)) / (1.0 - k);
          totalCost = mult * (Math.pow(stock + q, 1.0 - k) - Math.pow(stock, 1.0 - k));
        }
        avgPrice = totalCost / q;
        slippagePercent = ((spot - avgPrice) / spot) * 100;
      }
    }

    const fillRatio = (stock / maxReserve) * 100;

    return {
      spot,
      avgPrice,
      totalCost,
      slippagePercent,
      isOverflow,
      disposalFee,
      fillRatio
    };
  }

  function renderCalculator(mountEl, lang) {
    if (!mountEl) return;
    const t = I18N[lang] || I18N.ru;
    const results = computePricing();

    mountEl.innerHTML = `
      <div class="calculator-container">
        <div class="calculator-header">
          <div class="calculator-title">
            <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            <span>${t.title}</span>
          </div>
        </div>

        <div class="calculator-presets">
          <span class="preset-label">${t.presets}</span>
          <button class="preset-btn" data-preset="iron">${t.presetIron}</button>
          <button class="preset-btn" data-preset="gold">${t.presetGold}</button>
          <button class="preset-btn" data-preset="diamond">${t.presetDiamond}</button>
          <button class="preset-btn" data-preset="overflow">${t.presetOverflow}</button>
          <button class="preset-btn" data-preset="shortage">${t.presetShortage}</button>
        </div>

        <div class="calc-grid">
          <div class="calc-inputs">
            <!-- P0 -->
            <div class="calc-field">
              <div class="calc-label-row">
                <span class="calc-label-title">${t.basePrice}</span>
                <span class="calc-label-val" id="calc-p0-val">${calcState.p0.toFixed(1)} CBX</span>
              </div>
              <div class="calc-input-row">
                <input type="range" class="calc-slider" id="calc-p0" min="1" max="250" step="0.5" value="${calcState.p0}">
                <input type="number" class="calc-number-input" id="calc-p0-num" value="${calcState.p0}" step="0.5">
              </div>
            </div>

            <!-- Target Reserve -->
            <div class="calc-field">
              <div class="calc-label-row">
                <span class="calc-label-title">${t.targetReserve}</span>
                <span class="calc-label-val" id="calc-target-val">${calcState.sTarget}</span>
              </div>
              <div class="calc-input-row">
                <input type="range" class="calc-slider" id="calc-target" min="500" max="20000" step="250" value="${calcState.sTarget}">
                <input type="number" class="calc-number-input" id="calc-target-num" value="${calcState.sTarget}" step="250">
              </div>
            </div>

            <!-- Current Stock -->
            <div class="calc-field">
              <div class="calc-label-row">
                <span class="calc-label-title">${t.currentStock}</span>
                <span class="calc-label-val" id="calc-stock-val">${calcState.stock}</span>
              </div>
              <div class="calc-input-row">
                <input type="range" class="calc-slider" id="calc-stock" min="100" max="25000" step="100" value="${calcState.stock}">
                <input type="number" class="calc-number-input" id="calc-stock-num" value="${calcState.stock}" step="100">
              </div>
            </div>

            <!-- Elasticity k -->
            <div class="calc-field">
              <div class="calc-label-row">
                <span class="calc-label-title">${t.elasticity}</span>
                <span class="calc-label-val" id="calc-k-val">${calcState.k.toFixed(2)}</span>
              </div>
              <div class="calc-input-row">
                <input type="range" class="calc-slider" id="calc-k" min="0.50" max="1.15" step="0.01" value="${calcState.k}">
                <input type="number" class="calc-number-input" id="calc-k-num" value="${calcState.k}" step="0.01">
              </div>
            </div>

            <!-- Order Type -->
            <div class="calc-field">
              <span class="calc-label-title">${t.orderType}</span>
              <div class="calc-toggle-group">
                <button class="calc-toggle-btn ${calcState.orderType === 'BUY' ? 'active' : ''}" id="calc-btn-buy">${t.buy}</button>
                <button class="calc-toggle-btn ${calcState.orderType === 'SELL' ? 'active' : ''}" id="calc-btn-sell">${t.sell}</button>
              </div>
            </div>

            <!-- Order Qty -->
            <div class="calc-field">
              <div class="calc-label-row">
                <span class="calc-label-title">${t.orderQty}</span>
                <span class="calc-label-val" id="calc-qty-val">${calcState.orderQty} шт</span>
              </div>
              <div class="calc-input-row">
                <input type="range" class="calc-slider" id="calc-qty" min="1" max="1024" step="1" value="${calcState.orderQty}">
                <input type="number" class="calc-number-input" id="calc-qty-num" value="${calcState.orderQty}" step="1">
              </div>
            </div>
          </div>

          <!-- Outputs Card -->
          <div class="calc-outputs">
            <div class="output-stat-group">
              <div class="output-row">
                <span class="output-label">${t.outputSpot}</span>
                <span class="output-value ${results.isOverflow ? 'danger-text' : 'highlight'}" id="calc-out-spot">
                  ${results.isOverflow ? '-' + results.disposalFee.toFixed(2) + ' CBX' : results.spot.toFixed(2) + ' CBX'}
                </span>
              </div>

              <div class="output-row">
                <span class="output-label">${t.outputAvg}</span>
                <span class="output-value" id="calc-out-avg">
                  ${results.avgPrice < 0 ? results.avgPrice.toFixed(2) + ' CBX' : results.avgPrice.toFixed(2) + ' CBX'}
                </span>
              </div>

              <div class="output-row">
                <span class="output-label">${t.outputTotal}</span>
                <span class="output-value highlight" id="calc-out-total">
                  ${results.totalCost < 0 ? results.totalCost.toFixed(2) + ' CBX' : results.totalCost.toFixed(2) + ' CBX'}
                </span>
              </div>

              <div class="output-row">
                <span class="output-label">${t.outputSlippage}</span>
                <span class="output-value ${results.slippagePercent > 5 ? 'warning-text' : ''}" id="calc-out-slippage">
                  ${results.slippagePercent > 0 ? '+' + results.slippagePercent.toFixed(1) + '%' : results.slippagePercent.toFixed(1) + '%'}
                </span>
              </div>

              <div class="output-row">
                <span class="output-label">${t.outputStockFill}</span>
                <span class="output-value" id="calc-out-fill">
                  ${results.fillRatio.toFixed(1)}%
                </span>
              </div>
            </div>

            <!-- Dynamic Status Badge -->
            <div class="output-badge-status ${results.isOverflow ? 'badge-red' : (results.stock < calcState.sTarget * 0.4 ? 'badge-amber' : 'badge-green')}" id="calc-out-status">
              <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                ${results.isOverflow 
                  ? '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>'
                  : '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 14 14"/>'}
              </svg>
              <span>${results.isOverflow ? t.statusOverflow : (results.stock < calcState.sTarget * 0.4 ? t.statusLow : t.statusNormal)}</span>
            </div>
          </div>
        </div>
      </div>
    `;

    bindEvents(mountEl, lang);
  }

  function bindEvents(mountEl, lang) {
    const p0Slider = mountEl.querySelector("#calc-p0");
    const p0Num = mountEl.querySelector("#calc-p0-num");
    const targetSlider = mountEl.querySelector("#calc-target");
    const targetNum = mountEl.querySelector("#calc-target-num");
    const stockSlider = mountEl.querySelector("#calc-stock");
    const stockNum = mountEl.querySelector("#calc-stock-num");
    const kSlider = mountEl.querySelector("#calc-k");
    const kNum = mountEl.querySelector("#calc-k-num");
    const qtySlider = mountEl.querySelector("#calc-qty");
    const qtyNum = mountEl.querySelector("#calc-qty-num");
    const buyBtn = mountEl.querySelector("#calc-btn-buy");
    const sellBtn = mountEl.querySelector("#calc-btn-sell");

    function update() {
      renderCalculator(mountEl, lang);
    }

    function sync(slider, num, prop, parseFn) {
      slider.addEventListener("input", e => {
        calcState[prop] = parseFn(e.target.value);
        update();
      });
      num.addEventListener("change", e => {
        calcState[prop] = parseFn(e.target.value);
        update();
      });
    }

    sync(p0Slider, p0Num, "p0", parseFloat);
    sync(targetSlider, targetNum, "sTarget", parseInt);
    sync(stockSlider, stockNum, "stock", parseInt);
    sync(kSlider, kNum, "k", parseFloat);
    sync(qtySlider, qtyNum, "orderQty", parseInt);

    buyBtn.addEventListener("click", () => {
      calcState.orderType = "BUY";
      update();
    });

    sellBtn.addEventListener("click", () => {
      calcState.orderType = "SELL";
      update();
    });

    mountEl.querySelectorAll(".preset-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const key = btn.dataset.preset;
        if (PRESETS[key]) {
          const p = PRESETS[key];
          calcState.p0 = p.p0;
          calcState.sTarget = p.sTarget;
          calcState.stock = p.stock;
          calcState.k = p.k;
          calcState.orderQty = p.qty;
          update();
        }
      });
    });
  }

  window.initAmmCalculator = function(mountId, lang) {
    const el = document.getElementById(mountId);
    if (el) {
      renderCalculator(el, lang);
    }
  };
})();
