package network;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;

import io.netty.channel.ChannelHandlerContext;
import network.handlers.SeedInitHandler;

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

    private ClientStateMachine() {
    }
    }

    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void setState(State newState) {
        if (!isTransitionAllowed(currentState, newState)) {
            throw new IllegalStateException("Invalid transition from " + currentState + " to " + newState);
        }
        currentState = newState;
        for (StateListener l : listeners) l.onStateChanged(newState);
    }
    /**
     * Определяет, разрешён ли переход из состояния from в состояние to.
     * Переходы в ERROR и OFF разрешены всегда (кроме случая, когда уже в OFF).
     */
    private boolean isTransitionAllowed(State from, State to) {
        if (from == to) return true;               // оставаться в том же состоянии допустимо
        if (to == State.OFFLINE || to == State.ERROR) return true; // всегда можно остановить или перейти в ошибку
        switch (from) {
            case OFFLINE:
                return to == State.CONNECTING;
            case CONNECTING:
                return to == State.HANDSHAKE;
            case HANDSHAKE:
                return to == State.WAITING_FOR_HERO || to == State.IN_GAME;
            case WAITING_FOR_HERO:
                return to == State.HERO_READY;
            case HERO_READY:
                return to == State.IN_GAME;
            case IN_GAME:
                return false; // из IN_GAME только в OFF/ERROR (уже обработано выше)
            case ERROR:
                return false; // из ERROR только в OFF (обработано выше)
            default:
                return false;
        }
    }
    // --- Методы, вызываемые из UI ---
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
                SeedInitHandler.sendHeroClass(GamesInProgress.selectedClass);
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