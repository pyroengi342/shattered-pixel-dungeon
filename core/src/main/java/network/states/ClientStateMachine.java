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
        if (instance == null) {
            instance = new ClientStateMachine();
        }
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
        stateMachine.forceError();
        NetworkManager.getInstance().showMessage("Error: " + reason);
    }
    public int getPlayerId() {
        return stateMachine.getPlayerId();
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