package network.states;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import java.util.ArrayList;
import java.util.List;

/**
 * Машина состояний для одного игрока. Базовый метод.
 * Состояние вычисляется на основе текущих данных (hero, seed, ready).
 * Изменение данных производится только через специальные методы,
 * которые автоматически обновляют состояние и уведомляют слушателей.
 */
public class PlayerStateMachine {
    public enum State {
        OFFLINE,            // не в сети (начальное состояние)
        HANDSHAKE,           // после PLAYER_ASSIGN, но без данных
        WAITING_FOR_SEED,    // есть герой, но нет seed (может быть при реконнекте)
        WAITING_FOR_HERO,    // seed получен, но герой ещё не создан
        GAME_READY,          // и seed, и герой есть, но игрок ещё не нажал "готов"
        IN_GAME,             // всё есть и ready = true
        ERROR
    }

    // Данные игрока
    private int playerId;
    private String name;
    private Hero hero;           // null, если ещё не создан
    private Long seed;           // null, если ещё не получен
    private boolean ready;       // флаг готовности к игре
    private boolean isHost;

    private State currentState;
    private final List<StateListener> listeners = new ArrayList<>();

    public PlayerStateMachine(int playerId, String name) {
        this.playerId = playerId;
        this.name = name;
        this.hero = null;
        this.seed = null;
        this.ready = false;
        this.isHost = false;
        this.currentState = computeState();
    }

    // --- Геттеры для данных (при необходимости) ---
    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public Hero getHero() { return hero; }
    public Long getSeed() { return seed; }
    public boolean isReady() { return ready; }
    public boolean isHost() { return isHost; }

    // --- Методы для изменения данных (только через них) ---

    public void setPlayerId(int playerId) {
//        if (this.playerId != -1 && this.playerId != playerId) {
//            throw new IllegalStateException("Player ID already set to " + this.playerId);
//        }
        this.playerId = playerId;
        updateState();
    }
    public void forceError() {
        currentState = State.ERROR;
        notifyListeners();
    }

    public void setHero(Hero hero) {
        // Эта проверка убрана, т.к. на локалке setHero осуществляет и SessionState и StateMachine
//        if (this.hero != null) {
//            throw new IllegalStateException("Hero already set for player " + playerId);
//        }
        this.hero = hero;
        updateState();
    }

    public void setSeed(long seed) {
//        if (this.seed != null) {
//            throw new IllegalStateException("Seed already set for player " + playerId);
//        }
        this.seed = seed;
        updateState();
    }

    public void setReady(boolean ready) {
        if (this.ready == ready) return;
        this.ready = ready;
        updateState();
    }

    public void setHost(boolean host) {
        this.isHost = host;
        updateState();
        // состояние не зависит от isHost, но можно учесть, если нужно
    }

    // Для обновления имени (например, если игрок переименовался)
    public void setName(String name) {
        this.name = name;
        updateState();
        // состояние не меняется
    }

    // --- Вычисление состояния на основе текущих данных ---
    private State computeState() {
        if (playerId == -1){
            return State.OFFLINE;
        }
        if (hero == null && seed == null) {
            return State.HANDSHAKE;
        }
        if (seed == null) {
            return State.WAITING_FOR_SEED;   // есть герой, но нет seed (возможно после реконнекта)
        }
        if (hero == null) {
            return State.WAITING_FOR_HERO;    // seed есть, героя нет
        }
        if (true) {
            return State.GAME_READY;          // всё есть, но не готов
        }
        return State.HANDSHAKE;                 // всё есть и готов
    }

    private void updateState() {
        State newState = computeState();
        if (newState == currentState) return;
        currentState = newState;
        notifyListeners();
    }

    // --- Управление слушателями ---
    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (StateListener l : listeners) {
            l.onStateChanged(currentState);
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public interface StateListener {
        void onStateChanged(State newState);
    }
}