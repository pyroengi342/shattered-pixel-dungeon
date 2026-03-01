package network.handlers;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;

public class PlayerLeaveHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_LEAVE"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> Multiplayer.Players.remove(senderId));
    }
}