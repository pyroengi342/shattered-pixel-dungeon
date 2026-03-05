package network;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;

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
                sendSeedIfAny();
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
        switch (clientState){
            case HANDSHAKE:
                performHandshake(session);
                break;
            default:
                session.ctx.close();
                break;
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
    }
    private void sendSeedIfAny() {
        Long seed = Dungeon.seed;               // локальная переменная, чтобы избежать проблем с изменением поля
        if (seed == null || seed == -1L) return; // проверяем и null, и маркер -1 (если используется)
        for (ClientSessionState clients : connectedClients.values()) {
            if (clients.stateMachine.getSeed() == null){
                SeedInitHandler.send(clients.ctx);
                clients.stateMachine.setSeed(Dungeon.seed); // автоматически обновит состояние
            }
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
            if (session.stateMachine.getCurrentState() != PlayerStateMachine.State.GAME_READY) {
                return; // не все готовы
            }
        }
        // Все клиенты в GAME_READY – можно начинать игру
        // Здесь вызываем метод старта игры (например, в ServerStateMachine или напрямую)
        startGame();
    }

    private void startGame() {
    }
}