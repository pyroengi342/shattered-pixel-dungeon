package network.handlers.server;

import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.handlers.MessageHandler;
import network.states.ClientSessionState;
import network.states.ServerStateMachine;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

public class PlayerReadyHandler implements MessageHandler {

    @Override
    public String getType() {
        return "PLAYER_READY";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // Server handler only
        if (NetworkManager.getMode() != NetworkManager.Mode.SERVER) {
            return;
        }

        ClientSessionState session = NetworkManager.getSession(senderId);
        if (session == null) return;

        // Only process if in LOBBY state
        ServerStateMachine.State currentState = ServerStateMachine.getInstance().getCurrentState();
        if (currentState != ServerStateMachine.State.LOBBY) {
            return;
        }

        boolean plrReady = bundle.getBoolean("player_ready");
        session.setReady(plrReady);

        // Update PlayerInfo
        Multiplayer.Players.setReady(senderId, plrReady);

        // Broadcast to all clients (including sender for confirmation)
        broadcast(senderId, plrReady);

        // Check if all players ready and notify host
        if (Multiplayer.Players.allReady()) {
            // Transition to LOBBY_GAME_READY state
            ServerStateMachine.getInstance().onAllPlayersReady();
            NetworkManager.getInstance().showMessage("All players ready! Press Start to begin.");
        } else if (!plrReady) {
            // A player became not ready - transition back to LOBBY
            ServerStateMachine.getInstance().onPlayerNotReady();
        }
    }

    private void broadcast(int playerId, boolean ready) {
        Bundle bundle = new Bundle();
        bundle.put("playerId", playerId);
        bundle.put("player_ready", ready);

        BundleMessage msg = new BundleMessage("PLAYER_READY", playerId);
        msg.bundleData = bundle.toString();

        // Broadcast to all connected clients
        NetworkManager.broadcastMessageServer(msg, null);
    }

    /**
     * CLIENT SIDE: Send ready state to server
     */
    public static void sendReady(boolean ready) {
        if (NetworkManager.getMode() == NetworkManager.Mode.CLIENT
                || NetworkManager.getMode() == NetworkManager.Mode.LOCALHOST) {
            Bundle bundle = new Bundle();
            bundle.put("player_ready", ready);
            NetworkManager.sendMessage("PLAYER_READY", bundle);
        }
    }
}