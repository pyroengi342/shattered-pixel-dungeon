# SPD Multiplayer - Network Architecture

## Текущее состояние

### Реализовано ✅

#### 1. Сетевая инфраструктура
- [x] Netty server/client (NIO)
- [x] Kryo сериализация
- [x] MessageDispatcher маршрутизация
- [x] Length-based frame detection

#### 2. Подключение
- [x] Создание сервера (порт из настроек)
- [x] Подключение к серверу (IP/порт из настроек)
- [x] Состояния подключения (CONNECTED, DISCONNECTED)
- [x] Обработка отключений

#### 3. Лобби (базово)
- [x] WndMultiplayer UI (настройки хоста/клиента)
- [x] Отображение списка игроков в HeroSelectScene
- [x] PlayerJoinHandler - присоединение игроков
- [x] PlayerAssignHandler - назначение ID

#### 4. Интеграция с игрой
- [x] HeroSelectScene - кнопка мультиплеера
- [x] Dungeon - поддержка нескольких героев
- [x] Multiplayer утилиты (позиции, ближайший герой)

#### 5. Обработчики окон (частично)
- [x] UpgradeHandler
- [x] TradeHandler
- [x] AbilityHandler
- [x] BlacksmithHandler
- [x] GhostRewardHandler
- [x] ItemUseHandler
- [x] MonkAbilityHandler
- [x] ComboHandler
- [x] SubclassHandler
- [x] EnergizeHandler

### Недостающее ❌

#### 1. Лобби
- [ ] Статус "готов" игроков (PlayerReadyHandler не дописан)
- [ ] Синхронизация выбора класса героя
- [ ] Кнопка "Начать игру" для хоста
- [ ] Отображение выбранного класса других игроков

#### 2. Запуск игры
- [ ] Seed синхронизация (SeedInitHandler - базовый)
- [ ] Передача героев (HeroCreatedHandler - базовый)
- [ ] Старт игры после ready от всех

#### 3. Синхронизация игры
- [ ] Движение героев
- [ ] Атаки
- [ ] AI врагов
- [ ] Переходы между уровнями
- [ ] Ловушки, двери

#### 4. Отсутствующие обработчики окон
- [ ] WndSellItem
- [ ] WndIdentify  
- [ ] WndDegrade
- [ ] WndReward
- [ ] NPC обработчики (Shepherd, Imp, Ghost полностью)

#### 5. Надёжность
- [ ] Application-level ACK
- [ ] Reconnect
- [ ] Ping/latency мониторинг

## Паттерны в коде

### Состояние игрока (PlayerStateMachine)
```
OFFLINE → HANDSHAKE → WAITING_FOR_SEED → WAITING_FOR_HERO → GAME_READY → IN_GAME
                                    ↓                    ↓
                                 ERROR ←─────────────── READY
```

### Обработка сообщений
1. Сообщение приходит в ServerHandler/ClientHandler
2. Kryo декодирует → BundleMessage
3. MessageDispatcher.dispatch() → по типу → конкретный Handler
4. Handler обрабатывает (на UI thread через UiThreadExecutor)

### Отправка сообщений
```java
// Через NetworkManager
Bundle bundle = new Bundle();
bundle.put("key", value);
NetworkManager.sendMessage("TYPE", bundle);

// На сервере - broadcast
NetworkManager.broadcastMessageServer(msg, ignoreCtx);
```

## Файлы требующие внимания

### Критические
- `network/states/PlayerStateMachine.java` - состояния
- `network/states/ClientStateMachine.java` - логика клиента
- `network/handlers/server/PlayerReadyHandler.java` - готовность
- `network/handlers/client/HeroCreatedHandler.java` - передача героя

### Для синхронизации (создать)
- `network/handlers/ActionHandler.java` - действия игроков
- `network/handlers/GameStateHandler.java` - синхронизация state
- `network/messages/GameStateMessage.java` - структура state

## Где искать код

| Компонент | Файл |
|-----------|------|
| Главное меню | `windows/WndMultiplayer.java` |
| Выбор героя | `scenes/HeroSelectScene.java` |
| Сетевой менеджер | `network/NetworkManager.java` |
| Игроки | `network/Multiplayer.java` |
| Состояния | `network/states/*.java` |
| Обработчики | `network/handlers/**/*.java` |

## Интеграция с game code

### Dungeon.java
- Импортирует `network.Multiplayer`
- Цикл по `Multiplayer.Players.getAll()` для синхронизации
- `Multiplayer.calculateHeroPositions()` - расстановка

### Actors (Hero, Enemy)
- Проверка `Multiplayer.isMultiplayer`
- В MP - действия отправляются на сервер

### Windows
- Обработчики в `handlers/window/`
- Синхронизация открытия/закрытия окон
