package network;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.noosa.Game;
import network.handlers.PlayerAssignHandler;
import network.handlers.PlayerJoinHandler;
import network.handlers.SeedInitHandler;
import network.states.ClientSessionState;
import network.states.PlayerStateMachine;
import network.states.ServerStateMachine;

import java.util.Map;

/**
 * Централизованная логика обработки событий на сервере.
 * Подписан на изменения состояния сервера и всех клиентов.
 * Принимает решения о рассылке сообщений и переходе между глобальными состояниями.
 */
public class ServerCallflow {
    private final ServerStateMachine serverState;
    private final Map<Integer, ClientSessionState> connectedClients;

    public ServerCallflow(ServerStateMachine serverState, Map<Integer, ClientSessionState> connectedClients) {
        this.serverState = serverState;
        this.connectedClients = connectedClients;
    }

    // --- Обработка подключения нового клиента ---
    public void onClientConnected(ClientSessionState session) {
        switch (serverState.getCurrentState()) {
            case OPERATIONAL:
                handleClientInLobby(session);
                break;
            case IN_GAME:
                handleClientInGame(session);
                break;
            default:
                // Сервер не принимает новых игроков (например, выключается)
                session.ctx.close();
                break;
        }
    }

    private void handleClientInLobby(ClientSessionState session) {
        // У нового клиента ожидаем состояние HANDSHAKE (hero=null, seed=null)
        PlayerStateMachine.State clientState = session.stateMachine.getCurrentState();
        if (clientState == PlayerStateMachine.State.HANDSHAKE) {
            performHandshake(session);
        } else {
            // Если состояние не соответствует, закрываем соединение
            session.ctx.close();
        }
    }

    private void performHandshake(ClientSessionState session) {
        // 1. Отправляем PLAYER_ASSIGN
        PlayerAssignHandler.send(session.ctx, session.playerId, session.name);

        // 2. Добавляем игрока в глобальный список
        Multiplayer.PlayerInfo newPlayer = new Multiplayer.PlayerInfo(session.playerId, session.name);
        Multiplayer.Players.add(newPlayer);

        // 3. Рассылаем PLAYER_JOIN остальным клиентам (уведомляем их о новом игроке)
        for (ClientSessionState other : connectedClients.values()) {
            if (other.playerId != session.playerId) {
                PlayerJoinHandler.send(other.ctx, session.playerId, session.name);
            }
        }

        // 4. Отправляем новому клиенту информацию о существующих игроках
        for (ClientSessionState other : connectedClients.values()) {
            if (other.playerId != session.playerId) {
                PlayerJoinHandler.send(session.ctx, other.playerId, other.name);
            }
        }

        // 5. Если игра уже инициализирована (seed есть), отправляем его и обновляем состояние сессии
        if (Dungeon.seed != 0) {
            SeedInitHandler.send(session.ctx);
            session.stateMachine.setSeed(Dungeon.seed); // автоматически обновит состояние
        }
    }

    private void handleClientInGame(ClientSessionState session) {
        // Логика для подключения к уже идущей игре (наблюдатель или реконнект)
        // Пока просто закрываем соединение
        session.ctx.close();
    }

    // --- Обработка изменения состояния клиента ---
    public void onClientStateChanged(ClientSessionState session) {
        // Например, проверяем, все ли клиенты готовы к старту игры
        checkAllPlayersReady();
    }

    // --- Обработка изменения глобального состояния сервера ---
    public void onServerStateChanged(ServerStateMachine.State newState) {
        // Можно добавить реакции на смену состояния сервера
        // Например, при переходе в IN_GAME запретить новые подключения и т.д.
    }

    // --- Проверка готовности всех игроков ---
    private void checkAllPlayersReady() {
        if (serverState.getCurrentState() != ServerStateMachine.State.OPERATIONAL) {
            return; // проверка имеет смысл только в лобби
        }
        for (ClientSessionState session : connectedClients.values()) {
            if (session.stateMachine.getCurrentState() != PlayerStateMachine.State.HERO_READY) {
                return; // не все готовы
            }
        }
        // Все клиенты в HERO_READY – можно начинать игру
        // Здесь вызываем метод старта игры (например, в ServerStateMachine или напрямую)
        startGame();
    }

    private void startGame() {
        // Переводим сервер в состояние STARTING (или сразу в IN_GAME)
//        serverState.onGameStarting(); // предположим, есть такой метод
        // Генерируем seed, создаём героев, рассылаем SEED_INIT и т.д.
        // Эту логику можно реализовать здесь или вынести в отдельный метод
    }
}