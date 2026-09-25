/**
 * Ammora Documentation - Russian Content (docs-ru.js)
 */
window.DOCS_RU = {
  navTitle: "Документация Ammora",
  searchPlaceholder: "Поиск по документации... (Ctrl+K)",
  onThisPage: "На этой странице",
  copied: "Скопировано!",
  copy: "Копировать",
  copyPage: "Копировать страницу",
  copyPageSuccess: "Скопировано в Markdown!",
  aiPrompt: "prompt.md",
  categories: [
    {
      id: "general",
      title: "Начало работы",
      items: [
        { id: "overview", title: "Обзор и концепция", icon: "book-open" },
        { id: "amm-economics", title: "Экономическая модель AMM", icon: "trending-up" },
        { id: "calculator", title: "Интерактивный симулятор AMM", icon: "activity" }
      ]
    },
    {
      id: "gameplay",
      title: "Игровая механика",
      items: [
        { id: "blocks-terminal", title: "Биржевой Терминал", icon: "monitor" },
        { id: "blocks-logistics", title: "Торговый и Закупочный Доки", icon: "package" },
        { id: "cold-wallet", title: "Холодный Кошелек и P2P", icon: "credit-card" },
        { id: "vending-marketplace", title: "Торговые Автоматы и Маркетплейс", icon: "shopping-bag" },
        { id: "atm-cash", title: "Банкомат и наличные CBX", icon: "dollar-sign" }
      ]
    },
    {
      id: "integrations",
      title: "Интеграции",
      items: [
        { id: "cc-tweaked", title: "CC: Tweaked (ComputerCraft)", icon: "cpu" },
        { id: "create-mod", title: "Create (Кинетика и Табло)", icon: "settings" }
      ]
    },
    {
      id: "configuration",
      title: "Конфигурация и Сервер",
      items: [
        { id: "datapacks", title: "Руководство по датапакам", icon: "file-text" },
        { id: "custom-items", title: "Кастомные и модовые предметы", icon: "layers" },
        { id: "admin-console", title: "Панель администратора (/ammora admin)", icon: "shield" },
        { id: "localization", title: "Локализация и перевод", icon: "globe" },
        { id: "faq", title: "Частые вопросы (FAQ)", icon: "help-circle" }
      ]
    }
  ],

  sections: {
    "overview": {
      title: "Обзор и концепция мода",
      subtitle: "Живая рыночная экономика Minecraft на алгоритмах автоматического маркетмейкера (AMM).",
      breadcrumbs: ["Начало работы", "Обзор и концепция"],
      html: `
        <h2>Введение</h2>
        <p><strong>Ammora</strong> — комплексная экономическая модификация для Minecraft (NeoForge 1.21.1), кардинально меняющая принцип взаимодействия игроков с торговлей. Традиционные серверные магазины страдают от фундаментальной проблемы: фиксированные цены и бесконечные сундуки скупки неизбежно приводят к гиперинфляции, когда игроки создают автоматические фермы железа или золота.</p>
        <p>В Ammora биржа функционирует как децентрализованный пул ликвидности на базе кривых связывания (Bonding Curve AMM). Здесь нет фиксированных цен — котировки динамически формируются реальным балансом спроса и предложения игроков сервера.</p>

        <div class="callout callout-info">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            <span>Официальная валюта: CBX</span>
          </div>
          <div class="callout-content">
            Единой расчетной единицей биржи является <strong>CBX</strong>. Баланс хранится в транзакционной базе данных SQLite (режим WAL) и привязан к UUID игрока. Баланс не теряется при смерти и не занимает слоты инвентаря.
          </div>
        </div>

        <h2>Ключевые возможности мода</h2>
        <div class="cards-grid">
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Динамический AMM</span>
              <span class="badge badge-blue">Алгоритм</span>
            </div>
            <div class="doc-card-desc">Цены автоматически растут при покупках и падают при оптовых продажах по математической кривой эластичности.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Защита от ферм</span>
              <span class="badge badge-amber">Экология</span>
            </div>
            <div class="doc-card-desc">При переполнении складов биржа вводит утилизационный сбор, списывая деньги за утилизацию мусора вместо выплат.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>Интеграция с Create</span>
              <span class="badge badge-green">Кинетика</span>
            </div>
            <div class="doc-card-desc">Кинетическое ускорение торговых доков и вывод живых котировок на информационные табло через Display Link.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title">
              <span>CC: Tweaked API</span>
              <span class="badge badge-blue">Lua API</span>
            </div>
            <div class="doc-card-desc">Полная поддержка ComputerCraft: настенные мониторы, автоматические торговые боты и аварийные стоп-лоссы.</div>
          </div>
        </div>

        <h2>Архитектура безопасности</h2>
        <p>Мод построен с акцентом на надежность транзакций и предотвращение дюпов:</p>
        <ul>
          <li><strong>Атомарность SQLite:</strong> Все списания и начисления средств происходят в транзакционном режиме. Сбои сервера не приводят к рассинхронизации балансов.</li>
          <li><strong>Буфер неполученных доставок (Unclaimed Deliveries):</strong> Если предмет должен быть выдан игроку при исполнении отложенного ордера, когда игрок находится оффлайн или с полным инвентарем, предмет никогда не выбрасывается в пустоту. Он помещается в защищенный буфер базы данных и выдается при следующем входе.</li>
          <li><strong>Anti-Scam P2P шлюз:</strong> Защищенные окна обмена между игроками с двухсторонней фиксацией условий и 3-секундным таймером подтверждения.</li>
        </ul>
      `
    },

    "amm-economics": {
      title: "Экономическая модель AMM",
      subtitle: "Математический аппарат ценообразования, расчет проскальзывания, экологический сбор и ранги.",
      breadcrumbs: ["Начало работы", "Экономическая модель"],
      html: `
        <h2>1. Формула ценообразования AMM</h2>
        <p>Текущая спотовая цена ресурса $P(S)$ вычисляется относительно фактического объема товаров на биржевом складе $S$:</p>

        <div class="formula-box">
          P(S) = P₀ × ( S_target / S )^k
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Символ</th>
                <th>Параметр</th>
                <th>Описание</th>
                <th>Типовое значение</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>P₀</code></td>
                <td>Базовая цена</td>
                <td>Равновесная цена предмета в CBX при нормативном запасе склада.</td>
                <td>10.0 CBX (железо), 150.0 CBX (алмаз)</td>
              </tr>
              <tr>
                <td><code>S_target</code></td>
                <td>Целевой запас</td>
                <td>Нормативный объем сырья на складе биржи.</td>
                <td>10 000 шт. (слитки железа)</td>
              </tr>
              <tr>
                <td><code>S</code></td>
                <td>Фактический запас</td>
                <td>Текущее количество предметов на складе в данный момент.</td>
                <td>Динамическое значение</td>
              </tr>
              <tr>
                <td><code>k</code></td>
                <td>Коэффициент эластичности</td>
                <td>Наклон кривой спроса. Чем выше $k$, тем сильнее колебания цен.</td>
                <td>0.75 – 0.95</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Пример расчета</span>
          </div>
          <div class="callout-content">
            При $P_0 = 10.0$, $S_{target} = 10\\,000$, $k = 0.85$:<br>
            • Склад равен норме ($S = 10\\,000$): цена ровно <strong>10.00 CBX</strong>.<br>
            • Игроки скупили половину склада ($S = 5\\,000$): цена вырастает до <strong>18.03 CBX</strong>.<br>
            • Склад наполнен на 15 000 слитков: цена падает до <strong>7.08 CBX</strong>.
          </div>
        </div>

        <h2>2. Проскальзывание цены (Slippage)</h2>
        <p>Спотовая котировка $P(S)$ показывает цену ровно за 1 штуку. Если игрок покупает или продает крупную партию (стак 64 шт., ящик 2304 шт.), каждая последующая единица покупается из измененного пула. Средняя цена партии отличается от начальной спот-цены.</p>
        <p>Биржевой терминал рассчитывает точную сумму сделки через интеграл кривой ценообразования:</p>

        <div class="formula-box">
          Cost(Q) = ∫ [ S к S - Q ] P(x) dx
        </div>

        <p>Умная кнопка <strong>MAX</strong> в терминале выполняет бинарный поиск, позволяя игроку потратить весь доступный баланс с учетом постоянного роста цены внутри ордера.</p>

        <h2>3. Экологический сбор и отрицательные цены</h2>
        <p>Каждый ресурс имеет максимальный порог вместимости хранилища $S_{max}$ (по умолчанию $1.5 \\times S_{target}$). При превышении этого лимита спотовая цена уходит в отрицательную зону:</p>

        <div class="formula-box">
          C_disposal = α × [ (S - S_max) / S_target ]
        </div>

        <div class="callout callout-warning">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>Внимание: Опасность для автоматических ферм</span>
          </div>
          <div class="callout-content">
            Если подключить автоматическую ферму железа напрямую к Торговому Доку без настройки стоп-лосса, при переполнении склада биржа начнет <strong>списывать CBX с баланса игрока</strong> за переработку избыточного сырья. Если баланс иссякнет, сделка отклонится.
          </div>
        </div>

        <h2>4. Ежесуточный дренаж избытка (Mean Reversion)</h2>
        <p>Каждые игровые сутки (24 000 игровых тиков / 20 минут реального времени) биржа выполняет процедуру сглаживания избыточных запасов:</p>
        <ul>
          <li>Склады с запасом $S > S_{target}$ списывают <strong>8%</strong> избыточного объема, имитируя муниципальное потребление и развитие инфраструктуры.</li>
          <li>Цены на перепроизведенные товары плавно возвращаются к базовой отметке $P_0$.</li>
        </ul>

        <h2>5. Ранги трейдера и скидки на комиссию</h2>
        <p>Торговый оборот приносит очки репутации (<strong>REP</strong>): <code>1 REP = 10 CBX оборота</code>. С ростом ранга комиссия биржи уменьшается в 4 раза:</p>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Уровень</th>
                <th>Ранг</th>
                <th>Требуемый REP</th>
                <th>Ставка комиссии</th>
                <th>Привилегии</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><span class="badge">I</span></td>
                <td><strong>Новичок (Novice)</strong></td>
                <td>0 – 99</td>
                <td>2.0%</td>
                <td>Базовый доступ к терминалу, закупка до 1 шт.</td>
              </tr>
              <tr>
                <td><span class="badge">II</span></td>
                <td><strong>Трейдер (Trader)</strong></td>
                <td>100 – 499</td>
                <td>1.6%</td>
                <td>Закупочный док: партии до 16 шт.</td>
              </tr>
              <tr>
                <td><span class="badge">III</span></td>
                <td><strong>Брокер (Broker)</strong></td>
                <td>500 – 1 999</td>
                <td>1.2%</td>
                <td>Предохранитель Stop-High в закупочном доке.</td>
              </tr>
              <tr>
                <td><span class="badge">IV</span></td>
                <td><strong>Инвестор (Investor)</strong></td>
                <td>2 000 – 9 999</td>
                <td>0.8%</td>
                <td>Оптовые закупки до 64 шт за импульс.</td>
              </tr>
              <tr>
                <td><span class="badge badge-green">V</span></td>
                <td><strong>Кит (Whale)</strong></td>
                <td>10 000+</td>
                <td><strong>0.5%</strong></td>
                <td>Минимальная биржевая комиссия для арбитража.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>6. Обезличенные металлические счета (ОМС)</h2>
        <p>ОМС позволяют спекулировать на стоимости металлов и алмазов без физического перемещения слитков:</p>
        <ul>
          <li><strong>Покупка объема:</strong> Игрок фиксирует виртуальную позицию по текущему споту.</li>
          <li><strong>Мониторинг PnL:</strong> В интерфейсе терминала отображается плавающая прибыль или убыток (зеленый <code>+X.XX CBX</code> / красный <code>-X.XX CBX</code>).</li>
          <li><strong>Закрытие:</strong> При закрытии позиции баланс игрока моментально пополняется прибылью.</li>
          <li><strong>Carry Fee:</strong> За перенос позиции через полночь взимается символическая плата за хранение <code>0.05%</code>.</li>
        </ul>
      `
    },

    "calculator": {
      title: "Интерактивный симулятор AMM",
      subtitle: "Протестируйте алгоритм Bonding Curve, проскальзывание цены и экологический сбор в реальном времени.",
      breadcrumbs: ["Начало работы", "Интерактивный калькулятор"],
      html: `
        <p>Используйте интерактивный симулятор для моделирования рыночных ситуаций: от дефицита сырья до переполнения хранилищ и расчета средней цены оптовой закупки.</p>
        
        <div id="amm-calc-mount"></div>
      `
    },

    "blocks-terminal": {
      title: "Биржевой Терминал (Exchange Terminal)",
      subtitle: "Центральный блок биржи, свечные графики, мобильный режим и 4 торговых деска.",
      breadcrumbs: ["Игровая механика", "Биржевой Терминал"],
      html: `
        <h2>Обзор и портативность</h2>
        <p><strong>Биржевой Терминал</strong> (<code>ammora:exchange_terminal</code>) — основной блок взаимодействия игрока с рынком. Терминал имеет уникальное свойство:</p>
        <ul>
          <li><strong>Стационарный режим:</strong> Блок размещается в мире и подключается к редстоун-схемам и периферии ComputerCraft.</li>
          <li><strong>Мобильный режим (КПК):</strong> Терминал можно взять в руку и нажать <strong>ПКМ в воздух</strong>. Полный графический интерфейс откроется прямо из инвентаря.</li>
        </ul>

        <h2>Аналоговый сигнал редстоуна (0–15)</h2>
        <p>Биржевой терминал выдает аналоговый редстоун-сигнал, прямо пропорциональный проценту заполнения склада отслеживаемого ресурса ($0\\% = 0$, $100\\% = 15$).</p>
        <ul>
          <li><strong>Вывод сигнала:</strong> Напрямую в прилегающую редстоун-пыль и повторители, а также через компаратор.</li>
          <li><strong>Привязка ресурса:</strong> Терминал автоматически отслеживает ресурс, выбранный в GUI. Также можно нажать <strong>Shift + ПКМ</strong> с предметом в руке (например, железным слитком) по блоку терминала.</li>
        </ul>

        <h2>Торговые режимы терминала</h2>
        <div class="cards-grid">
          <div class="doc-card">
            <div class="doc-card-title"><span>1. СПОТ (Spot)</span></div>
            <div class="doc-card-desc">Физическая покупка и продажа предметов из инвентаря с быстрым выбором стаков (+1, +16, +64, MAX).</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>2. ОМС (Metals)</span></div>
            <div class="doc-card-desc">Инвестиционные счета без сундуков с живым расчетом PnL и кнопками фиксации прибыли.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>3. ЛИМ (Limit Orders)</span></div>
            <div class="doc-card-desc">Книга лимитных заявок BUY_LIMIT и SELL_LIMIT с полным защитным эскроу депозитов.</div>
          </div>
          <div class="doc-card">
            <div class="doc-card-title"><span>4. ГОС (Gov Contracts)</span></div>
            <div class="doc-card-desc">Срочные государственные контракты на поставку сырья с надбавкой +15–25% к цене.</div>
          </div>
        </div>

        <h2>Свечной график (Candlestick Chart)</h2>
        <p>Терминал оснащен профессиональным графиком реального времени:</p>
        <ul>
          <li><strong>Свечи (30 сек):</strong> Зеленые свечи отображают рост курса, красные — падение.</li>
          <li><strong>Объемы торгов:</strong> Нижняя гистограмма показывает объем совершенных сделок.</li>
          <li><strong>Масштабирование:</strong> Прокрутка колесика мыши в области графика плавно приближает или отдаляет историю цен.</li>
        </ul>
      `
    },

    "blocks-logistics": {
      title: "Торговый и Закупочный Доки",
      subtitle: "Блоки промышленной автоматизации сбыта и снабжения производственных линий.",
      breadcrumbs: ["Игровая механика", "Логистические доки"],
      html: `
        <h2>1. Торговый Док (Trade Dock)</h2>
        <p><strong>Торговый Док</strong> (<code>ammora:trade_dock</code>) предназначен для автоматической продажи ресурсов на бирже:</p>
        <ul>
          <li>Поддерживает подключение воронок, выбрасывателей, конвейеров Create и труб любых технических модов.</li>
          <li>Поступающие предметы моментально продаются на бирже, а выручка CBX зачисляется на счет владельца.</li>
          <li><strong>Предохранитель Stop-Loss:</strong> В интерфейсе блока настраивается минимальная допустимая цена продажи. Если рыночная цена упадет ниже порога (или начнется утилизационный сбор), док немедленно заблокирует прием предметов.</li>
          <li><strong>Редстоун-компаратор:</strong> Выдает сигнал при срабатывании стоп-лосса или переполнении входного буфера.</li>
        </ul>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Рецепт крафта Торгового Дока</span>
          </div>
          <div class="callout-content">
            2 воронки + 1 бочка + 4 слитка железа + 1 биржевой терминал (<code>ammora:exchange_terminal</code>) + 1 блок редстоуна.
          </div>
        </div>

        <h2>2. Логистический Блок Закупки (Purchase Dock)</h2>
        <p><strong>Блок Закупки</strong> (<code>ammora:purchase_dock</code>) автоматически скупает выбранный ресурс с биржи и помещает в свой 9-слотовый буфер:</p>
        <ul>
          <li>Срабатывает на каждый входящий редстоун-импульс или циклично при постоянном сигнале (раз в 1 секунду).</li>
          <li>Средства списываются напрямую с баланса аккаунта владельца.</li>
        </ul>

        <h3>Система ранговых ограничений (Rank Gating)</h3>
        <p>Для предотвращения манипуляций рынком возможности блока закупки открываются по мере роста ранга репутации игрока:</p>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Ранг игрока</th>
                <th>Макс. партия за импульс</th>
                <th>Доступные функции</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Ранг I (Новичок)</td>
                <td>1 шт.</td>
                <td>Базовая закупка по 1 предмету.</td>
              </tr>
              <tr>
                <td>Ранг II (Трейдер)</td>
                <td>До 16 шт.</td>
                <td>Кнопки выбора партий: 4, 8, 16 шт.</td>
              </tr>
              <tr>
                <td>Ранг III (Брокер)</td>
                <td>До 16 шт.</td>
                <td><strong>Предохранитель Stop-High:</strong> Настройка потолка цены, выше которой закупка блокируется.</td>
              </tr>
              <tr>
                <td>Ранг IV (Инвестор)</td>
                <td><strong>До 64 шт.</strong></td>
                <td>Оптовые кнопки: 32 и 64 шт (целый стак за 1 редстоун-тик).</td>
              </tr>
            </tbody>
          </table>
        </div>
      `
    },

    "cold-wallet": {
      title: "Холодный Кошелек и P2P переводы",
      subtitle: "Портативное устройство для беспроводных расчетов, радар игроков и журнал транзакций.",
      breadcrumbs: ["Игровая механика", "Холодный Кошелек"],
      html: `
        <h2>Возможности Холодного Кошелька</h2>
        <p><strong>Холодный Кошелек</strong> (<code>ammora:cold_wallet</code>) — ручной портативный прибор для управления финансами в любой точке мира:</p>
        <ul>
          <li><strong>Радар игроков (30 блоков):</strong> При открытии интерфейса кошелек автоматически сканирует пространство вокруг и выводит список игроков поблизости с указанием дистанции в блоках. Клик по игроку выбирает его в качестве получателя.</li>
          <li><strong>Ручной ввод никнейма:</strong> Текстовое поле ввода позволяет отправить средства любому игроку сервера, даже если он находится вне зоны сканирования радара.</li>
          <li><strong>Мгновенные переводы с 0% комиссии:</strong> Переводы CBX происходят без потерь и без физических банкнот.</li>
          <li><strong>Быстрая сетка сумм:</strong> Кнопки приращения <code>[+10]</code>, <code>[+50]</code>, <code>[+100]</code>, <code>[+500]</code>, кнопка <code>[MAX]</code> и сброс.</li>
        </ul>

        <h2>Единый журнал Ledger</h2>
        <p>Вкладка «История Ledger» предоставляет полную выписку по финансовым операциям игрока:</p>
        <ul>
          <li>Входящие и исходящие P2P переводы с указанием отправителя и получателя.</li>
          <li>Исполненные спотовые сделки на терминале.</li>
          <li>Прибыль и закрытие позиций ОМС счетов.</li>
        </ul>

        <div class="callout callout-tip">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>Рецепт крафта Холодного Кошелька</span>
          </div>
          <div class="callout-content">
            1 компас + 1 слиток железа + 2 редстоуна + 1 кусочек золота в верстаке.
          </div>
        </div>
      `
    },

    "vending-marketplace": {
      title: "Торговые Автоматы и Маркетплейс",
      subtitle: "Магазины игроков в стиле Rust, защита от грифа, взрывы TNT и удаленный планшет.",
      breadcrumbs: ["Игровая механика", "Магазины и Маркетплейс"],
      html: `
        <h2>1. Торговый Автомат игрока (Player Vending Machine)</h2>
        <p><strong>Торговый Автомат</strong> (<code>ammora:player_vending_machine</code>) позволяет создавать торговые лавки игроков с защитой от несанкционированного доступа:</p>
        <ul>
          <li><strong>Привязка к владельцу:</strong> При установке блок навсегда привязывается к UUID установившего игрока.</li>
          <li><strong>10 слотов витрины:</strong> Индивидуальная настройка продаваемого предмета, количества в лоте и цены в CBX.</li>
          <li><strong>Накопительный сейф выручки:</strong> Заработанные CBX накапливаются в кассе автомата и могут быть сняты владельцем в один клик.</li>
        </ul>

        <h3>Механика защиты и рейдов (Rust-Style)</h3>
        <div class="callout callout-warning">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>Защита от инструментов и взрывы TNT</span>
          </div>
          <div class="callout-content">
            Автомат полностью неуязвим к киркам и ручному копанию других игроков (прочность бедрока).<br>
            Единственный способ разрушить чужой автомат — <strong>взрыв динамита (TNT)</strong>. При взрыве TNT автомат разрушается, а содержащиеся внутри товары выпадают на землю. Сам владелец может в любой момент демонтировать автомат без потерь.
          </div>
        </div>

        <h2>2. Торговый Планшет (Market Tablet)</h2>
        <p><strong>Торговый Планшет</strong> (<code>ammora:market_tablet</code>) предоставляет удаленный доступ к общесерверному маркетплейсу:</p>
        <ul>
          <li>Просмотр предложений всех активных торговых автоматов мира в едином каталоге.</li>
          <li>Удаленная покупка предметов с мгновенной доставкой в защищенный буфер доставок.</li>
          <li>Выставление заявок на скупку (RFQ) для других игроков.</li>
        </ul>
      `
    },

    "atm-cash": {
      title: "Банкомат и наличные CBX",
      subtitle: "Двухблочный банковский терминал, физические банкноты, расчет купюр и корпоративные лимиты.",
      breadcrumbs: ["Игровая механика", "Банкомат и Наличные"],
      html: `
        <h2>1. Банкомат (ATM Terminal)</h2>
        <p><strong>Банкомат</strong> (<code>ammora:atm</code>) — двухблочный индустриальный аппарат из полированного сланца и латуни для внесения и снятия наличных банкнот CBX.</p>
        <ul>
          <li><strong>Размещение в мире:</strong> Занимает 2 блока в высоту. При установке нижнего блока верхняя часть появляется автоматически с монолитной стыковкой текстуры.</li>
          <li><strong>Переключатель аккаунтов:</strong> В шапке окна можно переключаться между личным кошельком и казной вашей корпорации.</li>
          <li><strong>Суточные лимиты:</strong> Для сотрудников компании выводится остаток суточного бюджета (например, <code>5/100 CBX</code>), не позволяя снимать наличные сверх нормы.</li>
        </ul>

        <h2>2. Снятие наличных (Withdraw)</h2>
        <p>Позволяет конвертировать безналичные CBX в физические банкноты:</p>
        <ul>
          <li><strong>Автокоррекция суммы:</strong> Введенная сумма автоматически округляется до кратной 10 при нажатии Enter, выходе из поля или клике «Снять».</li>
          <li><strong>Пресеты быстрых сумм:</strong> Удобная двухколоночная сетка кнопок: <code>[10]</code>, <code>[50]</code>, <code>[100]</code>, <code>[500]</code>, <code>[1 000]</code>, <code>[5 000 CBX]</code>.</li>
          <li><strong>Карточка разбивки купюр:</strong> В реальном времени рассчитывает выдачу: количество купюр по 1000, 100 и 10 CBX.</li>
        </ul>

        <h2>3. Внесение наличных (Deposit)</h2>
        <p>Мгновенная инкассация заработанных или найденных купюр на баланс:</p>
        <ul>
          <li><strong>Кнопка «Внести все наличные»:</strong> Автоматически сканирует инвентарь игрока, подсчитывает общую сумму всех банкнот (10, 100, 1000 CBX) и в один клик переводит их на баланс.</li>
          <li><strong>Синхронизация инвентаря:</strong> Купюры мгновенно удаляются из слотов с синхронизацией контейнера, а кнопка динамически обновляет своё состояние.</li>
        </ul>

        <h2>4. Физическая валюта (Банкноты, Пачки, Блоки)</h2>
        <div class="table-responsive">
          <table class="docs-table">
            <thead>
              <tr>
                <th>Предмет</th>
                <th>Номинал</th>
                <th>Описание и крафт</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>Купюра 10 CBX</strong></td>
                <td>10 CBX</td>
                <td>Банкнота цвета цинка с гербом CBX. Базовый номинал.</td>
              </tr>
              <tr>
                <td><strong>Купюра 100 CBX</strong></td>
                <td>100 CBX</td>
                <td>Светло-бирюзовая банкнота со знаком валюты.</td>
              </tr>
              <tr>
                <td><strong>Купюра 1 000 CBX</strong></td>
                <td>1 000 CBX</td>
                <td>Золотисто-латунная банкнота высокого номинала.</td>
              </tr>
              <tr>
                <td><strong>Пачка денег</strong></td>
                <td>90 / 900 / 9 000 CBX</td>
                <td>Компактная стопка из 9 купюр одного достоинства (крафт 3х3 в верстаке). Разбирается обратно на 9 купюр.</td>
              </tr>
              <tr>
                <td><strong>Блок денег</strong></td>
                <td>810 / 8 100 / 81 000 CBX</td>
                <td>Тяжелый декоративный блок из 9 пачек денег (крафт 3х3 в верстаке) для банковских хранилищ. Разбирается на 9 пачек.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>5. Корпоративный аудит</h2>
        <p>Каждая операция снятия или внесения наличных через банкомат с корпоративного счета фиксируется в неизменяемом журнале аудита компании как <code>ATM_WITHDRAW</code> или <code>ATM_DEPOSIT</code> с указанием ника игрока и суммы.</p>
      `
    },

    "cc-tweaked": {
      title: "Интеграция с CC: Tweaked (ComputerCraft)",
      subtitle: "Подключение блоков мода как Lua-периферии, полный справочник API и готовые скрипты.",
      breadcrumbs: ["Интеграции", "CC: Tweaked"],
      html: `
        <h2>Подключение к периферии</h2>
        <p>Ammora имеет встроенную поддержку <strong>CC: Tweaked</strong>. Компьютеры и черепахи могут подключаться к <strong>Биржевому Терминалу</strong> и <strong>Торговому Доку</strong> вплотную или через проводные модемы:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal") or peripheral.find("exchange_dock")

if not exchange then
    error("Периферия Ammora не найдена! Проверьте подключение кабеля или модема.")
end
print("Успешное подключение к бирже!")</code></pre>
        </div>

        <h2>Справочник методов Lua API</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Метод</th>
                <th>Параметры</th>
                <th>Возвращаемое значение</th>
                <th>Описание</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>getPrice(itemId)</code></td>
                <td><code>itemId (string)</code></td>
                <td><code>table</code></td>
                <td>Возвращает <code>spotPrice</code>, <code>buyPrice</code>, <code>sellPrice</code>, <code>disposalFee</code>.</td>
              </tr>
              <tr>
                <td><code>getStock(itemId)</code></td>
                <td><code>itemId (string)</code></td>
                <td><code>table</code></td>
                <td>Возвращает <code>currentStock</code>, <code>targetReserve</code>, <code>maxReserve</code>, <code>status</code>.</td>
              </tr>
              <tr>
                <td><code>getAccount(playerUuid)</code></td>
                <td><code>playerUuid (string)</code></td>
                <td><code>table</code></td>
                <td>Возвращает <code>balance</code>, <code>reputation</code>, <code>rank</code>, <code>feeRate</code>.</td>
              </tr>
              <tr>
                <td><code>buy(itemId, amount)</code></td>
                <td><code>itemId (string), amount (number)</code></td>
                <td><code>boolean, string</code></td>
                <td>Покупка указанного количества предметов с биржи.</td>
              </tr>
              <tr>
                <td><code>sell(amount)</code></td>
                <td><code>amount (number)</code></td>
                <td><code>boolean, string</code></td>
                <td>Продажа предметов из буфера торгового дока на биржу.</td>
              </tr>
              <tr>
                <td><code>getStopLoss()</code></td>
                <td>нет</td>
                <td><code>number</code></td>
                <td>Текущий установленный порог стоп-лосса торгового дока.</td>
              </tr>
              <tr>
                <td><code>setStopLoss(price)</code></td>
                <td><code>price (number)</code></td>
                <td><code>boolean</code></td>
                <td>Установка порога цены стоп-лосса торгового дока.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h2>Пример 1: Настенный монитор котировок (exchange_wall.lua)</h2>
        <p>Скрипт выводит живые биржевые курсы с цветовой подсветкой статуса на большой монитор CC: Tweaked:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal")
local mon = peripheral.find("monitor")

if not exchange or not mon then
    error("Требуется подключение exchange_terminal и monitor!")
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
    mon.write("=== ChainBX: Котировки Биржи ===")
    
    for i, id in ipairs(items) do
        local price = exchange.getPrice(id)
        local stock = exchange.getStock(id)
        
        mon.setCursorPos(1, i + 2)
        mon.setTextColor(colors.white)
        mon.write(string.format("%-18s ", price.displayName or id))
        
        if price.sellPrice < 0 then
            mon.setTextColor(colors.purple)
            mon.write(string.format("УТИЛЬ: %.2f", price.disposalFee))
        else
            mon.setTextColor(colors.green)
            mon.write(string.format("%7.2f CBX", price.spotPrice))
        end
        
        mon.setTextColor(colors.gray)
        mon.write(string.format(" (склад: %d)", stock.currentStock))
    end
    
    sleep(3)
end</code></pre>
        </div>

        <h2>Пример 2: Автоматический торговый бот (arbitrage_bot.lua)</h2>
        <p>Бот скупает железо при снижении цены ниже 8.0 CBX и продает его, когда рынок восстанавливается выше 13.0 CBX:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>local exchange = peripheral.find("exchange_terminal")
local TARGET_ITEM = "minecraft:iron_ingot"
local BUY_THRESHOLD = 8.00    -- Покупать если ниже
local SELL_THRESHOLD = 13.00  -- Продавать если выше
local BATCH_SIZE = 64

print("Запуск торгового бота Ammora...")

while true do
    local q = exchange.getPrice(TARGET_ITEM)
    print(string.format("[%s] Спот: %.2f CBX", os.date("%X"), q.spotPrice))

    if q.spotPrice <= BUY_THRESHOLD then
        print("-> Сигнал ПОКУПКИ! Цена ниже порога.")
        local ok, err = exchange.buy(TARGET_ITEM, BATCH_SIZE)
        if ok then
            print("Куплен стак сырья по выгодной цене.")
        else
            print("Ошибка покупки: " .. tostring(err))
        end
    elseif q.spotPrice >= SELL_THRESHOLD then
        print("-> Сигнал ПРОДАЖИ! Рыночный памп.")
        local ok, err = exchange.sell(BATCH_SIZE)
        if ok then
            print("Продан стак сырья с прибылью.")
        else
            print("Ошибка продажи: " .. tostring(err))
        end
    end

    sleep(5)
end</code></pre>
        </div>

        <h2>Пример 3: Аварийная защита карьера (quarry_safety.lua)</h2>
        <p>Отключает редстоун-сигнал питания буровой установки, если склад биржи переполнен и цена падает ниже порога:</p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">LUA</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>local dock = peripheral.find("exchange_dock")
local REDSTONE_SIDE = "back"
local MIN_SAFE_PRICE = 4.00

while true do
    local stock = dock.getStock("minecraft:iron_ingot")
    local price = dock.getPrice("minecraft:iron_ingot")
    
    if price.sellPrice < MIN_SAFE_PRICE or stock.status == "OVERFLOW" then
        print("[ТРЕВОГА] Склад переполнен! Отключение карьера.")
        redstone.setOutput(REDSTONE_SIDE, false) -- Остановка карьера
    else
        redstone.setOutput(REDSTONE_SIDE, true)  -- Карьер работает
    end
    
    sleep(10)
end</code></pre>
        </div>
      `
    },

    "create-mod": {
      title: "Интеграция с Create",
      subtitle: "Кинетическое ускорение доков, вывод котировок на табло через Display Link и Ponder сцены.",
      breadcrumbs: ["Интеграции", "Create Mod"],
      html: `
        <h2>1. Кинетическое ускорение Торгового Дока</h2>
        <p>При установке мода <strong>Create</strong> Торговый Док Ammora принимает кинетическую энергию вращения:</p>
        <ul>
          <li>Подключение вала (Shaft) к боковой стороне Торгового Дока передает вращающий момент (Stress Impact: 4.0 SU на 1 RPM).</li>
          <li><strong>Ускорение обработки:</strong> Базовая скорость продажи составляет 1 операцию в секунду (20 тиков). При подаче кинетической энергии задержка сокращается пропорционально скорости вращения:
            <br><code>Задержка (тиков) = max(2, 20 - floor(RPM / 16))</code></li>
          <li>На скорости 256 RPM док сбрасывает и продает предметы <strong>каждые 2 тика</strong> (10 операций в секунду), легко справляясь с потоками от скоростных лент Create!</li>
        </ul>

        <h2>2. Вывод котировок на табло через Display Link</h2>
        <p>Биржевой Терминал выступает полноценным источником данных (Display Source) для Create Display Link:</p>
        <ul>
          <li>Установите <strong>Display Link</strong> на Биржевой Терминал, а второй конец направьте на <strong>Flap Display (Перекидное табло)</strong> или <strong>Nixie Tubes (Газоразрядные индикаторы)</strong>.</li>
          <li><strong>Режимы вывода:</strong>
            <ul>
              <li><em>Спотовая котировка:</em> Текущий курс за единицу товара в CBX с динамической стрелкой тренда (↑ / ↓).</li>
              <li><em>Складской резерв:</em> Количество единиц на складе биржи и статус заполнения.</li>
              <li><em>Бегущая строка новостей:</em> Активные рыночные события и новости.</li>
            </ul>
          </li>
        </ul>

        <h2>3. Интерактивные Ponder-сцены</h2>
        <p>Мод включает встроенные обучающие 3D-руководства Ponder:</p>
        <p>Наведите курсор на <strong>Биржевой Терминал</strong> или <strong>Торговый Док</strong> в инвентаре и зажмите клавишу <strong>'W'</strong> для запуска интерактивной визуализации работы блока.</p>
      `
    },

    "datapacks": {
      title: "Руководство по датапакам",
      subtitle: "Добавление товаров, экономических событий и контрактов через стандартные датапаки Minecraft.",
      breadcrumbs: ["Конфигурация и Сервер", "Датапаки"],
      html: `
        <h2>Структура каталогов датапака</h2>
        <p>Ammora полностью поддерживает настройку экономики через датапаки без компиляции Java-кода. Датапаки автоматически перезагружаются при выполнении команды <code>/reload</code>.</p>

        <div class="code-block-wrapper">
          <div class="code-header"><span class="code-lang-label">DIRECTORY TREE</span></div>
          <pre class="code-block"><code>your_datapack/
├── pack.mcmeta
└── data/
    └── <namespace>/
        └── exchange/
            ├── commodities/           &lt;-- Товары на спотовом рынке
            │   ├── brass.json
            │   └── copper.json
            ├── market_events/         &lt;-- Рыночные новости и кризисы
            │   ├── gold_rush.json
            │   └── tech_boom.json
            └── delivery_contracts/    &lt;-- Срочные контракты и госзаказы
                ├── steel_delivery.json
                └── food_drive.json</code></pre>
        </div>

        <h2>1. Настройка товаров (commodities/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (brass.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>{
  "resourceId": "create:brass_ingot",
  "displayName": "Латунный слиток",
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

        <h2>2. Рыночные события (market_events/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (gold_rush.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>{
  "id": "gold_rush",
  "title": "Золотая лихорадка",
  "description": "Обнаружена древняя сокровищница! Приток золота временно снижает биржевой курс на 30%.",
  "affectedResource": "minecraft:gold_ingot",
  "priceMultiplier": 0.70,
  "durationDays": 2,
  "weight": 10
}</code></pre>
        </div>

        <h2>3. Поставочные контракты (delivery_contracts/*.json)</h2>
        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON (steel_delivery.json)</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>{
  "id": "railway_expansion",
  "title": "Госзаказ: Развитие железных дорог",
  "description": "Муниципалитет закупает рельсы и сталь с премией 20% к базовой цене.",
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
      title: "Кастомные и модовые предметы",
      subtitle: "Подключение предметов из сторонних модов (Create, Mekanism, Thermal) через единый конфиг.",
      breadcrumbs: ["Конфигурация и Сервер", "Кастомные предметы"],
      html: `
        <h2>Файл конфигурации</h2>
        <p>Кастомные предметы можно добавлять напрямую через конфигурационный файл сервера:</p>
        <p><code>config/ammora_custom_items.json</code></p>

        <div class="code-block-wrapper">
          <div class="code-header">
            <span class="code-lang-label">JSON</span>
            <button class="code-copy-btn" onclick="copySnippet(this)">
              <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              <span>Копировать</span>
            </button>
          </div>
          <pre class="code-block"><code>[
  {
    "resourceId": "mekanism:ingot_osmium",
    "displayName": "Осмиевый слиток",
    "basePrice": 35.0,
    "targetReserve": 4000.0,
    "elasticity": 0.85
  },
  {
    "resourceId": "thermal:tin_ingot",
    "displayName": "Оловянный слиток",
    "basePrice": 8.5,
    "targetReserve": 12000.0
  }
]</code></pre>
        </div>

        <h2>Справочник параметров калибровки</h2>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Параметр</th>
                <th>Тип</th>
                <th>Обязательный</th>
                <th>Рекомендуемые значения</th>
                <th>Описание</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code>resourceId</code></td>
                <td>String</td>
                <td><strong>Да</strong></td>
                <td><code>modid:item_name</code></td>
                <td>Идентификатор предмета в реестре игры.</td>
              </tr>
              <tr>
                <td><code>basePrice</code></td>
                <td>Double</td>
                <td><strong>Да</strong></td>
                <td>1.0 – 5000.0</td>
                <td>Равновесная цена в CBX при заполнении склада на 100%.</td>
              </tr>
              <tr>
                <td><code>targetReserve</code></td>
                <td>Double</td>
                <td><strong>Да</strong></td>
                <td>1 000 – 20 000</td>
                <td>Нормативный размер складского запаса биржи.</td>
              </tr>
              <tr>
                <td><code>elasticity</code></td>
                <td>Double</td>
                <td>Нет</td>
                <td>0.80 (0.70 – 0.95)</td>
                <td>Крутизна реакции цены на изменение запасов.</td>
              </tr>
              <tr>
                <td><code>maxReserve</code></td>
                <td>Double</td>
                <td>Нет</td>
                <td><code>targetReserve * 1.5</code></td>
                <td>Порог включения утилизационного сбора.</td>
              </tr>
            </tbody>
          </table>
        </div>
      `
    },

    "admin-console": {
      title: "Панель администратора (/ammora admin)",
      subtitle: "Графическая консоль управления экономикой, команды оператора и аудит базы данных.",
      breadcrumbs: ["Конфигурация и Сервер", "Панель администратора"],
      html: `
        <h2>Доступ и безопасность</h2>
        <p>Панель администратора открывается командой <code>/ammora admin</code>. Доступ строго защищен проверкой прав оператора сервера (уровень доступа 2 и выше: <code>player.hasPermissions(2)</code>).</p>

        <h2>Вкладки консоли управления</h2>
        <ul>
          <li><strong>Балансы:</strong> Просмотр балансов всех игроков сервера, поиск по нику, ручная установка или начисление CBX, изменение очков торговой репутации (REP).</li>
          <li><strong>События:</strong> Ручной запуск или досрочное прекращение рыночных кризисов и экономических новостей.</li>
          <li><strong>Курсы AMM:</strong> Мониторинг складских остатков по всем товарам, калибровка параметров $P_0$ и $S_{target}$, аварийный сброс склада до нормативного.</li>
          <li><strong>Логи сделок:</strong> Полный постраничный аудит финансовых транзакций из базы данных SQLite с фильтрацией по времени и игроку.</li>
        </ul>

        <h2>Команды оператора в чате</h2>
        <div class="code-block-wrapper">
          <div class="code-header"><span class="code-lang-label">BASH / CHAT COMMANDS</span></div>
          <pre class="code-block"><code># Установить точный баланс игрока
/ammora balance set &lt;player&gt; 50000.0

# Начислить средства
/ammora balance add &lt;player&gt; 2500.0

# Принудительно запустить рыночное событие
/ammora event trigger gold_rush

# Сбросить склад ресурса к целевому резерву
/ammora calibrate reset minecraft:iron_ingot</code></pre>
        </div>
      `
    },

    "localization": {
      title: "Локализация и перевод",
      subtitle: "Архитектура мультиязычности, паритет языковых файлов и добавление переводов через ресурс-паки.",
      breadcrumbs: ["Конфигурация и Сервер", "Локализация"],
      html: `
        <h2>Стандарт локализации</h2>
        <p>Ammora изначально поддерживает два эталонных языка со 100% паритетом ключей и спецификаторов форматирования:</p>
        <ul>
          <li><code>en_us.json</code> — Международный английский язык.</li>
          <li><code>ru_ru.json</code> — Русский язык (полная локализация всех GUI, тултипов, Ponder и сообщений).</li>
        </ul>

        <h2>Добавление своего языка через Resource Pack</h2>
        <p>Перевести мод на любой язык (испанский, китайский, немецкий и др.) можно без сборки jar-файла, поместив языковой JSON в обычный ресурс-пак:</p>
        <p><code>your_resourcepack/assets/ammora/lang/&lt;language_code&gt;.json</code></p>

        <div class="callout callout-info">
          <div class="callout-header">
            <svg class="callout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            <span>Правило формата плейсхолдеров</span>
          </div>
          <div class="callout-content">
            Количество и тип спецификаторов (<code>%s</code> для строк, <code>%d</code> для целых чисел, <code>%.2f</code> для сумм CBX) должны строго совпадать с эталонным <code>en_us.json</code>.
          </div>
        </div>
      `
    },

    "faq": {
      title: "Часто задаваемые вопросы (FAQ)",
      subtitle: "Ответы на распространенные технические и игровые вопросы по моду Ammora.",
      breadcrumbs: ["Конфигурация и Сервер", "FAQ"],
      html: `
        <h2>Что происходит с предметами, если игрок оффлайн при исполнении ордера?</h2>
        <p>Мод реализует гарантию сохранности предметов <strong>Unclaimed Deliveries</strong>. Если при исполнении лимитного ордера или покупке через маркетплейс игрок находится оффлайн или его инвентарь полон, предметы не падают на землю. Они буферизуются в таблице базы данных SQLite и автоматически выдаются в инвентарь в момент входа в игру или открытия любого биржевого терминала.</p>

        <h2>Почему цена на Торговом Доке ушла в минус?</h2>
        <p>Склад биржи превысил лимит максимальной вместимости $S_{max}$ из-за избыточных поставок сырья. Включился экологический утилизационный сбор. Чтобы этого избежать, установите в интерфейсе Торгового Дока защитный порог <strong>Stop-Loss</strong> (например, <code>5.0 CBX</code>).</p>

        <h2>Как защитить Торговый Автомат от воровства?</h2>
        <p>Торговый автомат полностью неуязвим для кирок и кулаков других игроков. Украсть товары можно только при помощи подрыва динамитом (TNT). Размещайте автоматы в защищенных приватах или укрепленных зонах.</p>

        <h2>Можно ли использовать мод на серверах без Create и ComputerCraft?</h2>
        <p>Да. Модификации Create и CC: Tweaked являются опциональными. Ammora полноценно работает как самостоятельный автономный финансовый мод с терминалами, кошельками и P2P обменом.</p>
      `
    }
  }
};
