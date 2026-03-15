package network.handlers.client;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.handlers.MessageHandler;
import network.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;

public class KickNotifyHandler implements MessageHandler {
    @Override
    public String getType() { return "KICK_NOTIFY"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            String reason = bundle.optString("reason", "unknown");
            
            // Показываем сообщение
            String message;
            switch (reason) {
                case "kicked_by_host":
                    message = "You have been kicked from the game by the host.";
                    break;
                case "host_disconnected":
                    message = "Host disconnected.";
                    break;
                default:
                    message = "You were removed from the game.";
            }
            
            // Отключаемся
            NetworkManager.getInstance().disconnect();
            
            if (ShatteredPixelDungeon.scene() != null) {
                ShatteredPixelDungeon.scene().addToFront(new WndMessage(message));
            }
        });
    }
}
