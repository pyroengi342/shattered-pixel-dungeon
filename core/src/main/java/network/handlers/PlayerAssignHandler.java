package network.handlers;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.ClientStateMachine;
import network.Multiplayer;
import network.NetworkManager;

public class PlayerAssignHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_ASSIGN"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        int assignedId = bundle.getInt("assignedId");
        String name = bundle.getString("name");
        NetworkManager.setLocalPlayerId(assignedId);
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(assignedId, name);
        player.isLocal = true;
        Multiplayer.Players.add(player);
        ClientStateMachine.getInstance().onPlayerAssign();
    }
    // public void msgHandle(int senderId, Bundle bundle) {
    //     Game.runOnRenderThread(() -> {
    //         int assignedId = bundle.getInt("assignedId");
    //         String name = bundle.getString("name");
    //         Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(assignedId, name);
    //         player.isLocal = true; // помечаем как локального
    //         Multiplayer.Players.add(player);
    //         System.out.println("Assigned as player: " + name + " (ID: " + assignedId + ")");
    //     });
    // }
}