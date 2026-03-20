# SPD Multiplayer - Network Stack Documentation

## Зависимости и версии

### Core Dependencies
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| Netty | 4.1.119.Final | Сетевой фреймворк (TCP) |
| Kryo | 5.6.2 | Сериализация объектов |
| libGDX | 1.14.0 | Игровой фреймворк |

### Дополнительные
- KryoSerializers (optional)
- Netty TCNative (для OpenSSL, опционально)

## Архитектура

### Общая схема
```
[Game Code] → [MessageDispatcher] → [MessageHandler]
                   ↓
            [NetworkManager]
                   ↓
            [Kryo Encoder/Decoder]
                   ↓
            [Netty Channel] ← TCP/NIO
```

### Паттерны использования

#### 1. Netty Server (NIO)
```java
// Boss group - Accepts connections
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
// Worker group - Handles I/O
EventLoopGroup workerGroup = new NioEventLoopGroup();

ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 .childHandler(new ChannelInitializer<SocketChannel>() {
     protected void initChannel(SocketChannel ch) {
         ChannelPipeline p = ch.pipeline();
         // Length field для frame detection
         p.addLast(new LengthFieldPrepender(4));
         p.addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
         // Kryo serialization
         p.addLast(new KryoDecoder(kryo));
         p.addLast(new KryoEncoder(kryo));
         // Business logic
         p.addLast(new ServerHandler(...));
     }
 })
 .option(ChannelOption.SO_BACKLOG, 128)
 .childOption(ChannelOption.SO_KEEPALIVE, true)
 .childOption(ChannelOption.TCP_NODELAY, true);
```

#### 2. Netty Client
```java
EventLoopGroup group = new NioEventLoopGroup();
Bootstrap b = new Bootstrap();
b.group(group)
 .channel(NioSocketChannel.class)
 .handler(new ChannelInitializer<SocketChannel>() {
     protected void initChannel(SocketChannel ch) {
         ChannelPipeline p = ch.pipeline();
         p.addLast(new LengthFieldPrepender(4));
         p.addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
         p.addLast(new KryoDecoder(kryo));
         p.addLast(new KryoEncoder(kryo));
         p.addLast(new ClientHandler(...));
     }
 });
```

#### 3. Kryo Сериализация
```java
Kryo kryo = new Kryo();
// Регистрация классов (обязательно для эффективности)
kryo.register(BundleMessage.class);
kryo.register(String.class);
kryo.register(int.class);
kryo.register(HashMap.class);
kryo.register(ArrayList.class);
// Регистрация своих классов
kryo.register(MyClass.class, new MyClassSerializer());

// Или через ID (нужно согласовывать на клиенте и сервере)
kryo.register(MyClass.class, 100);
```

#### 4. Message Handler паттерн
```java
public class MyHandler implements MessageHandler {
    @Override
    public String getType() { return "MY_MESSAGE"; }
    
    @Override
    public void msgHandle(int senderId, Bundle data) {
        // Обработка на UI thread через UiThreadExecutor
        UiThreadExecutor.run(() -> {
            // Логика
        });
    }
}
```

#### 5. Broadcast на сервере
```java
public void broadcast(NetworkManager.BundleMessage msg, ChannelHandlerContext ignore) {
    for (ClientSessionState session : connectedClients.values()) {
        ChannelHandlerContext ctx = session.ctx;
        if (ctx != ignore && ctx.channel().isActive()) {
            ctx.writeAndFlush(msg);
        }
    }
}
```

## Формат сообщений

### BundleMessage (текущая реализация)
```java
public static class BundleMessage {
    public String bundleData;  // Bundle serialized to String
    public int playerId;       // ID отправителя
    public String type;       // Тип сообщения (определяет handler)
}
```

### Bundle сериализация
```java
// Отправка
Bundle bundle = new Bundle();
bundle.put("key", value);
String data = bundle.toString();  // Внимание: toString() может отличаться

// Получение
Bundle bundle = Bundle.read(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
```

## Важные замечания

### 1. Thread Safety
- Netty I/O в отдельных thread pools
- UI обновления через `UiThreadExecutor.run()` или `Game.runOnRenderThread()`
- Shared state (Map) использует `ConcurrentHashMap`

### 2. Kryo совместимость
- **Важно**: Kryo версия должна совпадать на клиенте и сервере
- Регистрация классов в одинаковом порядке для consistent IDs
- Использовать `KryoSerializable` или `Serializer` для сложных объектов

### 3. Netty Channel Options
- `TCP_NODELAY` = true - отключить Nagle (меньше latency)
- `SO_KEEPALIVE` = true - detect disconnect
- `SO_BACKLOG` = 128 - queue размер для connect()

### 4. Android/iOS совместимость
- Netty 4.1 работает на Android 5.0+
- Kryo 5.x требует Java 8+
- Использовать `netty-all` для Android (включает everything)

## Текущие компоненты

### Файлы в `network/`
| Файл | Назначение |
|------|------------|
| NetworkManager.java | Центральный менеджер, статические методы |
| Multiplayer.java | Игровое состояние, игроки |
| MessageDispatcher.java | Маршрутизация сообщений по типам |
| SMTH/MultiplayerServer.java | Серверная часть |
| SMTH/MultiplayerClient.java | Клиентская часть |
| codec/KryoDecoder.java | Netty decoder |
| codec/KryoEncoder.java | Netty encoder |
| codec/ChannelPipelineFactory.java | Pipeline setup |
| states/ClientStateMachine.java | Состояние клиента |
| states/ServerStateMachine.java | Состояние сервера |
| states/PlayerStateMachine.java | Состояние игрока |
| handlers/MessageHandler.java | Интерфейс обработчика |
| handlers/client/*.java | Клиентские обработчики |
| handlers/server/*.java | Серверные обработчики |
| handlers/window/*.java | Оконные обработчики |

## TODO для исследований

1. **GameState синхронизация** - как синхронизировать состояние между клиентами
2. **Turn-based протокол** - порядок ходов, ожидание игроков
3. **Application-level ACK** - подтверждение получения сообщений
4. **Delta updates** - передача только изменений, не всего state
5. **Hash-based state** - контрольные суммы для проверки синхронизации

## Ссылки
- [Netty 4.1 Docs](https://netty.io/4.1/api/)
- [Kryo GitHub](https://github.com/EsotericSoftware/kryo)
- [Kryo Serializers](https://github.com/EsotericSoftware/kryo-serializers)

---

# Исследование: Best Practices vs Текущая реализация

## Сравнение подходов

### Текущая реализация (Netty + Kryo)

| Аспект | Текущее | Оценка |
|--------|---------|--------|
| Транспорт | TCP (Netty NIO) | ✅ Для turn-based - ок |
| Сериализация | Kryo 5.6.2 | ✅ Хорошо |
| Формат сообщений | Bundle → String | ⚠️ Неэффективно |
| Надёжность | TCP level only | ❌ Нужно ACK |
| Архитектура | Authoritative Server | ✅ Правильно |
| State sync | Отсутствует | ❌ Нужно делать |

### Best Practices для Turn-Based

#### 1. Архитектуры синхронизации

**Authoritative Server (рекомендуется для turn-based)**
- Сервер хранит "истину"
- Клиенты шлют **actions** (не state)
- Сервер применяет → рассылает результат
- +: Античит, простая логика
- -: Задержка на round-trip

**Deterministic Lockstep**
- Все игроки выполняют одни и те же действия в одном порядке
- +: Точная синхронизация без отправки state
- -: Требует детерминистичности (сложно в Java float)

**Snapshot Interpolation**
- Сервер шлёт полный/дельта state
- Клиент интерполирует
- +: Плавно для real-time

#### 2. Сообщения - Best Practices

**Проблема текущей реализации:**
```java
// Сейчас: Bundle → String → Kryo (избыточно)
bundle.toString()  // String сериализация - плохо
```

**Рекомендуется:**
```java
// Прямая сериализация в byte[]
public class GameMessage {
    public int type;
    public int senderId;
    public byte[] payload;  // Kryo сериализует напрямую
}

// Или через ID + payload
public class ActionMessage {
    public int actionId;    // enum: MOVE, ATTACK, USE_ITEM...
    public int playerId;
    public Object[] params; // параметры
}
```

#### 3. Надёжность - Application-level ACK

**Зачем?**
- TCP гарантирует доставку, но не **обработку**
- Сообщение может дойти, но обработка упасть
- Нет понимания "client подтвердил action"

**Паттерн:**
```java
// Сообщение с sequence number
public class ReliableMessage {
    public long sequenceId;
    public int playerId;
    public String actionType;
    public byte[] data;
}

// ACK
public class MessageAck {
    public long sequenceId;
    public boolean success;
}

// Server: хранит pending messages
// Client: resend если нет ACK за N ms
```

**Ресурсы:**
- [Gaffer On Games: Reliable Messages](https://gafferongames.com/post/reliable_ordered_messages/)
- [Reliable UDP](https://medium.com/my-games-company/unity-realtime-multiplayer-part-3-reliable-udp-protocol-94fbffe8c72c)

#### 4. State Synchronization

**Паттерн для Turn-Based:**

1. **Event-based (рекомендуется):**
```java
// Вместо полного state - только изменения
public class GameEvent {
    public enum EventType { HERO_MOVED, ENEMY_DIED, ITEM_PICKED... }
    public EventType type;
    public int actorId;
    public Object[] params;
}
```

2. **Delta Compression:**
```java
// Полный state раз в N ходов + дельты
public class StateSnapshot {
    public long tick;
    public byte[] fullState;  // каждые 100 ходов
    
    public Map<Integer, HeroDelta> heroDeltas;  // между snapshot
}
```

3. **Hash Verification:**
```java
// Контрольная сумма для проверки десинхронизации
public class StateHash {
    public long tick;
    public int hashCode;  // xxhash или similar
    
    // Сервер рассылает, клиенты сверяют
}
```

#### 5. Turn-Based Протокол

```java
// Состояния
enum TurnState {
    WAITING_FOR_PLAYERS,
    AWAITING_TURN,      // Ожидание хода игрока
    PROCESSING,         // Сервер обрабатывает
    SYNCING,           // Рассылка результатов
    NEXT_TURN
}

// Server logic
public void onPlayerAction(int playerId, Action action) {
    if (currentTurn != playerId) return; // Не твой ход
    
    // Применить action
    applyAction(action);
    
    // Разослать результат
    broadcast(new TurnResult(action, newState));
    
    // Следующий ход
    nextPlayer();
}
```

### Библиотеки для сравнения

| Библиотека | Плюсы | Минусы |
|------------|-------|--------|
| **KryoNet** (над Kryo) | Проще чем Netty, готовое решение | Устаревает |
| **SocKit** (Java) | Turn-based специализирован | WebSocket only |
| **Mirror** (C#) | Популярный, Unity | Не Java |
| **ENet** (C) | Легендарный, RUDP | C, не для Java |
| **kcp2k** (C) | KCP протокол, RUDP | Порт на Java возможен |

### KryoNet vs Netty+Kryo

**KryoNet** = Kryo + Netty в одной библиотеке
```java
// KryoNet пример
Connection connection = listener.connect(host, port);
connection.addListener(new ConnectionListener() {
    @Override
    public void received(Connection c, Object o) { }
});
```

**Текущий подход (Netty + Kryo руками):**
- + Больше контроля
- + Можно настроить pipeline детально
- - Больше boilerplate

### Рекомендации для проекта

1. **Сейчас (TCP):** Оставить как есть, для turn-based достаточно
2. **Сериализация:** Перейти с Bundle→String на прямое Kryo
3. **ACK:** Добавить sequence numbers для критических сообщений
4. **State Sync:** Event-based, не full-state
5. **Hash:** Добавить проверку синхронизации (detection, не prevention)

### Ссылки по теме

- [Game Networking Demystified (Ruoyu Sun)](https://ruoyusun.com/2019/03/28/game-networking-1.html)
- [Gaffer On Games - Reliability](https://gafferongames.com/post/reliability_ordering_and_congestion_avoidance_over_udp/)
- [Lockstep Java Library](https://github.com/njlr/Lockstep)
- [SocKit - Turn-Based Engine](https://sockit.io/)
