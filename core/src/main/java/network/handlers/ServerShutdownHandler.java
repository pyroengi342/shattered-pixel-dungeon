package network.handlers;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.NetworkManager;

public class ServerShutdownHandler implements MessageHandler {
    private final NetworkManager networkManager;

    public ServerShutdownHandler(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String getType() { return "SERVER_SHUTDOWN"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            networkManager.showMessage("Server has been shut down");
            networkManager.disconnect();
        });
    }
}