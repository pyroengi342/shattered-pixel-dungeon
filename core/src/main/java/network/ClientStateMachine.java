package network;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;

import java.util.ArrayList;
import java.util.List;

public class ClientStateMachine {

    public enum State {
        OFFLINE,
        CONNECTING, // ждёт ответа от сервера на первичное подключение
        HANDSHAKE, // PLAYER_ASSIGN
        // GAME START
        SEED_RECEIVED, // SEED_INIT
        WAITING_FOR_HERO, // У клиента должен быть выбран герой (проверка)
        HERO_READY, // GAME_READY Готовность к подключению
        IN_GAME,
        ERROR
    }

    private State currentState = State.OFFLINE;
    private final List<StateListener> listeners = new ArrayList<>();

    private static ClientStateMachine instance;
    public static ClientStateMachine getInstance() {
        if (instance == null) instance = new ClientStateMachine();
        return instance;
    }

    private ClientStateMachine() {}

    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void setState(State newState) {
        currentState = newState;
        for (StateListener l : listeners) l.onStateChanged(newState);
    }

    // --- Методы, вызываемые из UI ---
    public void startLocalServer() {
        setState(State.CONNECTING);
        NetworkManager.getInstance().startServer();
    }

    public void connectToServer(String host) {
        setState(State.CONNECTING);
        NetworkManager.getInstance().connectToServer(host);
    }

    // --- Методы, вызываемые из обработчиков сообщений ---
    public void onPlayerAssign() {
        if (currentState == State.CONNECTING) {
            setState(State.HANDSHAKE);
        }
    }

    public void onSeedInit() {
        // Seed уже записан в Dungeon в SeedInitHandler
        if (currentState == State.HANDSHAKE || currentState == State.CONNECTING) {
            setState(State.SEED_RECEIVED);
            // Если класс уже выбран, отправляем его автоматически
            if (GamesInProgress.selectedClass != null) {
                NetworkManager.sendHeroClass(GamesInProgress.selectedClass);
                setState(State.WAITING_FOR_HERO);
            }
        }
    }

    public void onHeroClassSent() {
        // Можно использовать, если хотим явно перейти в WAITING_FOR_HERO после отправки
        if (currentState == State.SEED_RECEIVED) {
            setState(State.WAITING_FOR_HERO);
        }
    }

    private void checkHeroReady() {
        int localId = NetworkManager.getLocalPlayerId();
        if (localId != -1) {
            Multiplayer.PlayerInfo info = Multiplayer.Players.get(localId);
            if (info != null && info.hero != null) {
                setState(State.HERO_READY);
            }
        }
    }
                    // Dungeon.daily = false;
                    // Dungeon.dailyReplay = false;
                    // InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
                    // Game.switchScene(InterlevelScene.class);

    public void onHeroCreated(int playerId) {
        // При получении HERO_CREATED проверяем, относится ли оно к локальному игроку
        if (playerId == NetworkManager.getLocalPlayerId()) {
            checkHeroReady();
        }
    }

    public void onError(String reason) {
        setState(State.ERROR);
        // Можно показать сообщение
        NetworkManager.getInstance().showMessage("Error: " + reason);
    }

    public void reset() {
        currentState = State.OFFLINE;
    }

    // Геттеры
    public State getCurrentState() { return currentState; }

    public interface StateListener {
        void onStateChanged(State newState);
    }
}