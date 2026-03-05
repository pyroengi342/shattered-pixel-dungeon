// ==================== ServerStateMachine.java ====================
package network.states;

import java.util.ArrayList;
import java.util.List;

/**
 * Машина состояний для сервера.
 * Изменение данных производится только через специальные методы,
 */
public class ServerStateMachine {
    // Состояния сервера
    public enum State {
        // 
        OFF,
        STARTING,
        // Game Start
        // сервер запущен, принимает подключения, игра не началась
        OPERATIONAL,   
        // игра началась, новые подключения не принимаются (или только наблюдатели)
        
        WAITING_FOR_HERO,   // Для каждого игрока!
        GAME_READY,         // GAME_READY Для каждого игрока!
        // 
        IN_GAME,
        ERROR
    }

    private volatile State currentState = State.OFF;
    private final List<StateListener> listeners = new ArrayList<>();
    private static ServerStateMachine instance;

    public static ServerStateMachine getInstance() {
        if (instance == null) instance = new ServerStateMachine();
        return instance;
    }

    private ServerStateMachine() {}

    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Определяет, разрешён ли переход из состояния from в состояние to.
     * Переходы в ERROR и OFF разрешены всегда (кроме случая, когда уже в OFF).
     */
    private boolean isTransitionAllowed(State from, State to) {
        if (from == to) return true;               // оставаться в том же состоянии допустимо
        if (to == State.OFF || to == State.ERROR) return true; // всегда можно остановить или перейти в ошибку
        switch (from) {
            case OFF:
                return to == State.STARTING;
            case STARTING:
                return to == State.OPERATIONAL;
            case OPERATIONAL:
                return to == State.GAME_READY;
            case GAME_READY:
                return to == State.IN_GAME || to == State.OPERATIONAL;
            case IN_GAME:
                return false; // из IN_GAME только в OFF/ERROR (уже обработано выше)
            case ERROR:
                return false; // из ERROR только в OFF (обработано выше)
            default:
                return false;
        }
    }

    private void setState(State newState) {
        if (!isTransitionAllowed(currentState, newState)) {
            throw new IllegalStateException("Invalid transition from " + currentState + " to " + newState);
        }
        currentState = newState;
        for (StateListener l : listeners) l.onStateChanged(newState);
    }

    // Методы, вызываемые из NetworkManager (ServerCore)
    public void onServerStarting() {
        setState(State.STARTING);
    }

    public void onServerStarted() {
        setState(State.OPERATIONAL);
    }
    public void onGameStarted() {
        setState(State.IN_GAME);
    }
    public void onError(String reason) {
        setState(State.ERROR);
        // Можно залогировать, но UI обычно нет
    }

    public void reset() {
        setState(State.OFF);
    }
    public State getCurrentState() { return currentState; }

    public interface StateListener {
        void onStateChanged(State newState);
    }
}