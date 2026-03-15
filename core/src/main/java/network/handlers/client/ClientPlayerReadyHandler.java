package network.handlers.client;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.handlers.MessageHandler;
import network.NetworkManager;

public class ClientPlayerReadyHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_READY"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            int playerId = bundle.getInt("playerId");
            boolean ready = bundle.getBoolean("player_ready");
            
            Multiplayer.Players.setReady(playerId, ready);
            
            // Обновление UI будет вызвано через updatePlayerList в HeroSelectScene
        });
    }
}
