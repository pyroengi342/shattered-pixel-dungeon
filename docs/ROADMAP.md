# SPD Multiplayer Roadmap

## Версии (из build.gradle)

```
Netty: 4.1.119.Final
Kryo: 5.6.2
libGDX: 1.14.0
Java: 11+
```

## Фаза 1: Исследование и анализ ✅ (выполнено)
- [x] Исследование неткода стека → docs/NETCODE.md
- [x] Анализ текущего кода → docs/NETWORK_ARCH.md
- [x] Best practices исследование → docs/NETCODE.md (секция "Исследование")

## Фаза 2: Реализация (выполнено)

### ✅ Приоритет 1: Лобби (в основном выполнено)
- [x] PlayerReadyHandler - работает
- [x] Синхронизация выбора класса - HeroClassHandler + HeroClassSelectedHandler
- [x] Кнопка "Начать игру" - в HeroSelectScene
- [x] Отображение статуса игроков - PlayerBtn показывает класс и готовность

### ✅ Приоритет 2: GameState синхронизация (выполнено)
- [x] Event-based синхронизация - ServerStateMachine/ClientStateMachine
- [x] Turn-based протокол - TurnManager
- [x] MOVE action handler - PlayerMoveHandler (server + client)
- [x] ATTACK action handler - PlayerAttackHandler (server + client)
- [x] ITEM_USE handler - существующий ItemUseHandler

### ✅ Приоритет 3: Надёжность (базово)
- [x] TurnManager с таймаутами

## Фаза 3: Улучшения (в очереди)

### Приоритет 1: Улучшение сериализации
1. Перейти с Bundle→String на прямое Kryo
2. Создать структуры сообщений с type ID

### Приоритет 2: AI и окружение
1. Enemy AI синхронизация
2. Level changes (переходы между уровнями)

### Приоритет 3: Надёжность (опционально)
1. Sequence numbers для сообщений
2. Application-level ACK
3. Hash verification

## Долгосрочные задачи
- P2P соединение (STUN/TURN)
- Оптимизация (delta updates)
- Реконнект
