package network.handlers.server;

import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.handlers.MessageHandler;
import network.states.ClientSessionState;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

public class PlayerReadyHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_READY"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // Server handler
        ClientSessionState session = NetworkManager.getSession(senderId);
        if (session == null) return;

        boolean plrReady = bundle.getBoolean("player_ready");
        session.setReady(plrReady);
    }
    public static void sendReady(boolean ready) {
        // CLIENT SEND TO SERVER
        Bundle bundle = new Bundle();
        bundle.put("player_ready", ready);
        NetworkManager.sendMessage("PLAYER_READY", bundle);
    }

    public static void send(ChannelHandlerContext ctx, int playerId, boolean player_ready) {
        // Сервер -> клиент
        Bundle bundle = new Bundle();
        bundle.put("playerId", playerId);
        bundle.put("player_ready", player_ready);

        BundleMessage msg = new BundleMessage("PLAYER_READY", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }
}