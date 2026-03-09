package network;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import network.handlers.client.PlayerAssignHandler;
import network.handlers.client.PlayerJoinHandler;
import network.handlers.client.SeedInitHandler;
import network.states.ClientSessionState;
import network.states.PlayerStateMachine;
import network.states.ServerStateMachine;

import java.util.HashMap;
import java.util.Map;

public class ServerAgent {
    private final ServerStateMachine serverState;
    private final Map<Integer, ClientSessionState> connectedClients;

    public ServerAgent(ServerStateMachine serverState, Map<Integer, ClientSessionState> connectedClients) {
        this.serverState = serverState;
        this.connectedClients = connectedClients;
        serverState.addListener(this::onServerStateChanged);
    }

    private interface RequestHandler {
        void handle(ClientSessionState session);
    }

    private final Map<PlayerStateMachine.RequiredData, RequestHandler> requestHandlers = new HashMap<>(); {
        requestHandlers.put(PlayerStateMachine.RequiredData.SEED, session -> {
            if (Dungeon.seed != -1L) {
                SeedInitHandler.send(session.ctx);
                session.markRequestSent(PlayerStateMachine.RequiredData.SEED);
            }
        });

        requestHandlers.put(PlayerStateMachine.RequiredData.HERO_CLASS, session -> {
            // Отправляем запрос только если у клиента уже есть seed
            if (session.hasData(PlayerStateMachine.RequiredData.SEED)) {
                // Если нужно принудительно запросить класс, раскомментируйте:
                // HeroClassHandler.request(session.ctx);
                session.markRequestSent(PlayerStateMachine.RequiredData.HERO_CLASS);
            }
            // Если seed нет, ничего не делаем, запрос не помечаем
        });
    }

    public void onClientConnected(ClientSessionState session) {
        if (serverState.getCurrentState() != ServerStateMachine.State.LOBBY
                || connectedClients.size() >= MPSettings.maxPlayers()) {
            session.ctx.close();
            return;
        }

        int playerId = session.ctx.channel().hashCode();
        session.setPlayerId(playerId);
        session.setName("Player " + playerId);

        connectedClients.put(playerId, session);
        session.stateMachine.addListener(newState -> onClientStateChanged(session));

        initializeNewClient(session);
        checkPlayerProgress(session);
    }

    private void initializeNewClient(ClientSessionState session) {
//        if (session.stateMachine.getCurrentState() != PlayerStateMachine.State.HANDSHAKE) {
//            session.ctx.close();
//            return;
//        }

        Multiplayer.PlayerInfo newPlayer = new Multiplayer.PlayerInfo(session.getPlayerId(), session.getName());
        Multiplayer.Players.add(newPlayer);

        PlayerAssignHandler.send(session.ctx, session.getPlayerId(), session.getName());

        // Сообщаем другим о клиенте
        for (ClientSessionState other : connectedClients.values()) {
            if (other.getPlayerId() != session.getPlayerId()) {
                PlayerJoinHandler.send(other.ctx, session.getPlayerId(), session.getName());
            }
        }

        // Сообщаем клиенту о других
        for (ClientSessionState other : connectedClients.values()) {
            if (other.getPlayerId() != session.getPlayerId()) {
                PlayerJoinHandler.send(session.ctx, other.getPlayerId(), other.getName());
            }
        }
    }

    public void onClientStateChanged(ClientSessionState session) {
        checkAllPlayersReady();
        checkPlayerProgress(session);
    }

    public void onServerStateChanged(ServerStateMachine.State newState) {
        // Опционально
    }

    private void checkPlayerProgress(ClientSessionState session) {
        if (session.ctx == null || !session.ctx.channel().isActive()) return;

        for (PlayerStateMachine.RequiredData data : PlayerStateMachine.RequiredData.values()) {
            // Пропускаем CONNECTION_ID, так как он всегда есть после подключения
            if (data == PlayerStateMachine.RequiredData.CONNECTION_ID) continue;

            if (!session.hasData(data) && !session.isRequestSent(data)) {
                RequestHandler handler = requestHandlers.get(data);
                if (handler != null) {
                    handler.handle(session);
                } else {
                    System.err.println("No request handler for RequiredData: " + data);
                }
            }
        }
    }

    private void checkAllPlayersReady() {
        if (serverState.getCurrentState() != ServerStateMachine.State.LOBBY) return;

        for (ClientSessionState session : connectedClients.values()) {
            if (session.stateMachine.getCurrentState() != PlayerStateMachine.State.GAME_READY) {
                return;
            }
        }
        startGame();
    }

    private void startGame() {
        // serverState.setState(ServerStateMachine.State.IN_GAME);
        // Рассылка стартовых пакетов, инициализация подземелья и т.д.
    }

    public void onGlobalSeedSet() {
        for (ClientSessionState session : connectedClients.values()) {
            checkPlayerProgress(session);
        }
    }
}