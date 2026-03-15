package network.handlers.server;

import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.Multiplayer;
import network.handlers.MessageHandler;
import network.states.ClientSessionState;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

public class PlayerKickHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_KICK"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // Проверяем что запрос от хоста
        if (!Multiplayer.isHost) return;
        
        int targetPlayerId = bundle.getInt("player_id");
        
        // Получаем сессию целевого игрока
        ClientSessionState session = NetworkManager.getSession(targetPlayerId);
        if (session != null && session.ctx != null) {
            // Отправляем игроку сообщение о кике
            Bundle msgBundle = new Bundle();
            msgBundle.put("reason", "kicked_by_host");
            
            BundleMessage msg = new BundleMessage("KICK_NOTIFY", targetPlayerId);
            msg.bundleData = msgBundle.toString();
            session.ctx.writeAndFlush(msg);
            
            // Закрываем соединение
            session.ctx.close();
        }
        
        // Удаляем из списка игроков
        Multiplayer.Players.remove(targetPlayerId);
    }
    
    public static void sendKick(int playerId) {
        Bundle bundle = new Bundle();
        bundle.put("player_id", playerId);
        NetworkManager.sendMessage("PLAYER_KICK", bundle);
    }
}
