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

## Фаза 2: Реализация (в очереди)

### Приоритет 1: Улучшение сериализации
1. Перейти с Bundle→String на прямое Kryo
2. Создать структуры сообщений с type ID

### Приоритет 2: Лобби
1. Доработать PlayerReadyHandler
2. Синхронизация выбора класса
3. Кнопка "Начать игру" для хоста
4. Отображение статуса игроков

### Приоритет 3: GameState синхронизация
1. Event-based синхронизация (не full-state)
2. Turn-based протокол
3. ActionHandler

### Приоритет 4: Надёжность (опционально)
1. Sequence numbers для сообщений
2. Application-level ACK
3. Hash verification

### Приоритет 5: AI и окружение
1. Enemy AI синхронизация
2. Level changes

## Долгосрочные задачи
- P2P соединение (STUN/TURN)
- Оптимизация (delta updates)
- Реконнект
