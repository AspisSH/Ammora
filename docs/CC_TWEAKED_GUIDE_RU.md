# Гайд по интеграции с CC: Tweaked (ComputerCraft)

Ammora имеет встроенную нативную совместимость с **CC: Tweaked** (ComputerCraft).  
При установке компьютера рядом с **Биржевым Терминалом** или **Торговым Доком** блок регистрируется в Lua как полноценное периферийное устройство.

---

## Подключение к периферии

### Через проводные модемы или вплотную
```lua
local exchange = peripheral.find("exchange_terminal") or peripheral.find("exchange_dock")

if not exchange then
    error("Периферия биржи не найдена! Проверьте подключение кабеля или модема.")
end
```

Типы устройств:
- `"exchange_terminal"` — при подключении к Биржевому Терминалу.
- `"exchange_dock"` — при подключении к Торговому Доку.

---

## Справочник методов Lua API

### 1. `exchange.getPrice(itemId)`
Возвращает текущие котировки: спотовую цену, цену покупки, цену продажи и плату за утилизацию.

- **Параметры:**
  - `itemId` *(string)*: Идентификатор предмета (например: `"minecraft:iron_ingot"`).
- **Возвращает:** *(table)*
  ```lua
  {
      resourceId  = "minecraft:iron_ingot",
      displayName = "Iron Ingot",
      spotPrice   = 12.45,   -- Спотовая цена в CBX
      buyPrice    = 12.70,   -- Цена покупки игроком с учетом комиссии
      sellPrice   = 12.20,   -- Цена продажи (может быть отрицательной при переполнении!)
      disposalFee = 0.00     -- Штраф за утилизацию в CBX
  }
  ```

---

### 2. `exchange.getStock(itemId)`
Возвращает складские остатки биржи.

- **Параметры:**
  - `itemId` *(string)*: Идентификатор предмета.
- **Возвращает:** *(table)*
  ```lua
  {
      currentStock  = 8450,
      targetReserve = 10000,
      maxReserve    = 15000,
      fillPercent   = 56.33,  -- Процент заполнения от maxReserve
      status        = "NORMAL" -- "NORMAL", "HIGH" или "OVERFLOW"
  }
  ```

---

### 3. `exchange.getAccount(playerUuid)`
Возвращает баланс и торговый ранг игрока.

- **Параметры:**
  - `playerUuid` *(string)*: Строковый UUID игрока.
- **Возвращает:** *(table)*
  ```lua
  {
      balanceCBX    = 2450.80, -- Доступный баланс в CBX
      repPoints     = 1850,    -- Очки репутации
      repLevel      = 3,       -- Ранг (1=Новичок, 2=Трейдер, 3=Брокер, 4=Инвестор, 5=Кит)
      brokerFeeRate = 0.012    -- Текущая ставка комиссии (1.2%)
  }
  ```

---

### 4. `exchange.buy(playerUuid, itemId, count)`
Программная покупка предметов на бирже со списанием CBX со счета игрока.

- **Параметры:**
  - `playerUuid` *(string)*: UUID игрока.
  - `itemId` *(string)*: Идентификатор предмета.
  - `count` *(number)*: Количество для покупки (1–2304).
- **Возвращает:** *(table)*
  ```lua
  {
      success  = true,
      message  = "Purchase successful",
      cbxSpent = 164.50
  }
  ```

---

### 5. `exchange.sell(playerUuid, itemId, count)`
Программная продажа предметов с зачислением CBX на счет игрока.

- **Параметры:**
  - `playerUuid` *(string)*: UUID игрока.
  - `itemId` *(string)*: Идентификатор предмета.
  - `count` *(number)*: Количество для продажи.
- **Возвращает:** *(table)*
  ```lua
  {
      success   = true,
      message   = "Sale successful",
      cbxPayout = 120.00
  }
  ```

---

### 6. `exchange.getCandles(itemId, limit)`
Возвращает историю свечей OHLCV для построения графиков на мониторах ComputerCraft.

- **Возвращает:** массив таблиц с полями `timestamp`, `open`, `high`, `low`, `close`, `volume`, `bullish`.

---

### 7. `exchange.getDailyModifier(itemId)`
Возвращает суточный случайный коэффициент цены и модификатор события.

---

### 8. `exchange.getActiveEvent()`
Возвращает данные о текущем глобальном экономическом событии.

---

### 9. `exchange.setStopLoss(price)` *(Только для Торгового Дока)*
Устанавливает минимальную цену продажи. Если рыночная цена падает ниже этого порога, док прекращает прием предметов.

---

## Пример: Торговый бот с авто-закупкой на спаде

```lua
local exchange = peripheral.find("exchange_terminal")
local PLAYER_UUID = "YOUR_PLAYER_UUID_HERE"
local ITEM_ID = "minecraft:iron_ingot"
local BUY_TARGET_PRICE = 9.50 -- Покупать, если железо дешевле 9.50 CBX

while true do
    local quote = exchange.getPrice(ITEM_ID)
    local account = exchange.getAccount(PLAYER_UUID)
    
    print(string.format("[%s] %s Спот: %.2f CBX | Баланс: %.2f CBX", 
        os.date("%H:%M:%S"), quote.displayName, quote.spotPrice, account.balanceCBX))
        
    if quote.buyPrice <= BUY_TARGET_PRICE and account.balanceCBX >= 200 then
        print("Целевая цена достигнута! Закупка 16 шт...")
        local res = exchange.buy(PLAYER_UUID, ITEM_ID, 16)
        if res.success then
            print(string.format("Успешно куплено за %.2f CBX", res.cbxSpent))
        else
            print("Ошибка покупки: " .. tostring(res.message))
        end
    end
    
    sleep(10)
end
```
