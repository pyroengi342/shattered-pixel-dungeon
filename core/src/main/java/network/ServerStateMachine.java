// ==================== ServerStateMachine.java ====================
package network;

import java.util.ArrayList;
import java.util.List;

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
        HERO_READY,         // Для каждого игрока!
        // 
        IN_GAME,
        ERROR
    }

    private State currentState = State.OFF;
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

    private void setState(State newState) {
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