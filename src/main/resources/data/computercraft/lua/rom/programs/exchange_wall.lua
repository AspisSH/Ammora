-- Exchange Wall Street Monitor Program
-- Renders real-time candlestick chart and ticker board on CC: Tweaked Advanced Monitors

local monitor = peripheral.find("monitor")
local exchange = peripheral.find("exchange_terminal") or peripheral.find("exchange_dock")

if not monitor then
    print("Error: No Advanced Monitor found!")
    return
end

if not exchange then
    print("Error: No Exchange Terminal or Dock attached!")
    return
end

monitor.setTextScale(0.5)
local w, h = monitor.getSize()

local currentResource = "minecraft:iron_ingot"

local function drawHeader(priceData, stockData)
    monitor.setCursorPos(1, 1)
    monitor.setBackgroundColor(colors.black)
    monitor.setTextColor(colors.cyan)
    monitor.write(string.format("=== [%s] ===", string.upper(priceData.displayName or currentResource)))

    monitor.setCursorPos(1, 2)
    monitor.setTextColor(colors.yellow)
    monitor.write(string.format("SPOT: $%0.2f  BUY: $%0.2f  SELL: $%0.2f", priceData.spotPrice or 0, priceData.buyPrice or 0, priceData.sellPrice or 0))

    monitor.setCursorPos(1, 3)
    if (stockData.fillPercent or 0) >= 100 then
        monitor.setTextColor(colors.magenta)
        monitor.write(string.format("CAPACITY: %d%% [OVERFLOW DUMPING FEE: $%0.2f]", stockData.fillPercent or 0, priceData.disposalFee or 0))
    else
        monitor.setTextColor(colors.lightBlue)
        monitor.write(string.format("CAPACITY: %d%% (Stock: %d / %d)", stockData.fillPercent or 0, stockData.currentStock or 0, stockData.maxReserve or 0))
    end
end

local function drawChart(candles)
    local chartTop = 5
    local chartBottom = h - 2
    local chartHeight = chartBottom - chartTop

    if #candles == 0 then
        monitor.setCursorPos(2, chartTop + 2)
        monitor.setTextColor(colors.gray)
        monitor.write("Waiting for candle data...")
        return
    end

    -- Find min and max price across candles
    local minP = math.huge
    local maxP = -math.huge
    for _, c in ipairs(candles) do
        if c.low < minP then minP = c.low end
        if c.high > maxP then maxP = c.high end
    end
    if maxP <= minP then maxP = minP + 1 end

    local priceRange = maxP - minP

    -- Draw candles
    local startCol = 5
    for i, c in ipairs(candles) do
        local col = startCol + (i * 2)
        if col < w - 1 then
            local openY = math.floor(chartBottom - ((c.open - minP) / priceRange) * chartHeight)
            local closeY = math.floor(chartBottom - ((c.close - minP) / priceRange) * chartHeight)
            local highY = math.floor(chartBottom - ((c.high - minP) / priceRange) * chartHeight)
            local lowY = math.floor(chartBottom - ((c.low - minP) / priceRange) * chartHeight)

            local candleColor = c.bullish and colors.lime or colors.red

            -- Wick
            monitor.setTextColor(candleColor)
            for y = highY, lowY do
                monitor.setCursorPos(col, y)
                monitor.write("|")
            end

            -- Body
            local topY = math.min(openY, closeY)
            local botY = math.max(openY, closeY)
            for y = topY, botY do
                monitor.setCursorPos(col, y)
                monitor.write("#")
            end
        end
    end
end

print("Starting Exchange Wall Street monitor...")
while true do
    monitor.setBackgroundColor(colors.black)
    monitor.clear()

    local pData = exchange.getPrice(currentResource)
    local sData = exchange.getStock(currentResource)
    local candles = exchange.getCandles(currentResource, math.floor(w / 3))

    drawHeader(pData, sData)
    drawChart(candles)

    sleep(3)
end
