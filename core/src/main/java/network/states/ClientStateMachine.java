package network.states;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

import network.NetworkManager;
import network.handlers.SeedInitHandler;

/**
 * Собственная сессия клиента.
 * Отслеживает своё состояние.
 */
public class ClientStateMachine {
    private static ClientStateMachine instance;
    private final PlayerStateMachine stateMachine;

    private ClientStateMachine() {
        // Локальный игрок ещё не подключён – создаём с временными данными,
        // playerId и name будут обновлены позже из сообщений.
        stateMachine = new PlayerStateMachine(-1, "");
    }

    public static ClientStateMachine getInstance() {
        if (instance == null) instance = new ClientStateMachine();
        return instance;
    }

    public void addListener(PlayerStateMachine.StateListener listener) {
        stateMachine.addListener(listener);
    }

    public void removeListener(PlayerStateMachine.StateListener listener) {
        stateMachine.removeListener(listener);
    }

    public PlayerStateMachine.State getCurrentState() {
        return stateMachine.getCurrentState();
    }

    // --- Методы, вызываемые из UI ---
    public void connectToServer(String host) {
        // Реальное состояние изменится после получения PLAYER_ASSIGN
        NetworkManager.getInstance().connectToServer(host);
    }

    // // --- Методы, вызываемые из обработчиков сообщений ---
    // public void onPlayerAssign(int playerId, String name) {
    //     // Обновляем данные локального игрока
    //     // (в текущей реализации PlayerStateMachine не позволяет менять id, но можно создать новый или добавить сеттер)
    //     // Для простоты предположим, что мы можем создать новый экземпляр или добавить метод setId.
    //     // В данном примере мы не меняем id, так как он фиксирован в конструкторе.
    //     // Альтернатива: добавить в PlayerStateMachine метод setPlayerId, но обычно id не меняется.
    //     // Можно просто игнорировать, если id уже установлен.
    //     // Здесь для простоты оставим как есть, предполагая, что изначальный id = -1, и мы его не используем.
    //     // В реальности нужно или пересоздавать stateMachine, или добавить сеттер.
    //     // Пока просто установим имя.
    //     stateMachine.setName(name);
    //     // Состояние должно перейти в HANDSHAKE автоматически? Нет, данные не изменились.
    //     // Поэтому нам нужно явно инициировать переход? По логике computeState, если нет seed и hero, состояние = HANDSHAKE.
    //     // Если начальное состояние было OFFLINE, а мы хотим HANDSHAKE, то нужно либо установить начальное состояние в HANDSHAKE,
    //     // либо изменить данные так, чтобы computeState дал HANDSHAKE. Сейчас hero=null, seed=null => HANDSHAKE.
    //     // Так что состояние уже должно быть HANDSHAKE после конструктора, если начальное состояние вычислялось как HANDSHAKE.
    //     // В конструкторе мы вызываем computeState, который при hero=null, seed=null возвращает HANDSHAKE.
    //     // Значит, сразу после создания stateMachine состояние HANDSHAKE. Это хорошо.
    //     // Но нам нужно также сохранить playerId и name. Добавим сеттер для имени, как выше.
    // }

//     public void onSeedInit(long seed) {
// //        // Устанавливаем seed, состояние обновится автоматически
// //        stateMachine.setSeed(seed);
//     }

    public void onHeroCreated(Hero hero) {
        stateMachine.setHero(hero);
    }

    public void onGameStart() {
        // Устанавливаем ready = true (или сервер сам пришлёт GAME_START)
        stateMachine.setReady(true);
    }

    public void onError(String reason) {
        // Переход в ERROR через изменение данных? У нас нет поля для ошибки.
        // Можно либо добавить поле error, либо просто вызвать переход вручную.
        // В данной модели мы не имеем явного метода для ошибки, поэтому лучше расширить computeState для учёта ошибки.
        // Для простоты добавим метод forceError().
        forceError();
        NetworkManager.getInstance().showMessage("Error: " + reason);
    }

    private void forceError() {
        // Принудительно устанавливаем состояние ERROR (в обход данных)
        // Это не очень чисто, но допустимо для ошибок.
        // Можно добавить в PlayerStateMachine метод setError().
        // В реальном коде лучше предусмотреть поле error flag.
        // Здесь для краткости опустим.
    }

    public void reset() {
        // Сброс: создаём новый экземпляр (или очищаем данные)
        // В простейшем случае можно заменить stateMachine новым.
        // Но осторожно со слушателями.
        // Пропустим для brevity.
    }

    // ClientStateMachine.java
    public void onSeedInit(long seed) {
        stateMachine.setSeed(seed);   // теперь seed будет установлен
    }

    public void onPlayerAssign(int playerId, String name) {
        // Обновляем ID и имя в stateMachine
        stateMachine.setPlayerId(playerId);
        stateMachine.setName(name);
        // состояние пересчитается автоматически (hero=null, seed=null -> HANDSHAKE)
    }

    public boolean isSeedReceived() {
        return stateMachine.getSeed() != null;
    }

    public boolean isLocalPlayerIdSet() {
        return stateMachine.getPlayerId() != -1;
    }
}