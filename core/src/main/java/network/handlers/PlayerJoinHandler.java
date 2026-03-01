package network.handlers;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;

public class PlayerJoinHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_JOIN"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            if (senderId == NetworkManager.getLocalPlayerId()) return;
            String name = bundle.getString("name");
            Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(senderId, name);
            player.isLocal = false;
            Multiplayer.Players.add(player);
            System.out.println("Player joined: " + name + " (ID: " + senderId + ")");
        });
    }
}