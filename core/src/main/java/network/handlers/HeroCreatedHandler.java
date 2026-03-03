package network.handlers;

import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.NetworkManager;
import network.states.ClientStateMachine;

public class HeroCreatedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CREATED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        int playerId = bundle.getInt("playerId");
    }

    // Отправка конкретному клиенту (например, новому игроку информация о существующем)
    public static void send(ChannelHandlerContext ctx, int playerId, String name) {
        Bundle bundle = new Bundle();
        bundle.put("name", name);
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("PLAYER_JOIN", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);

    }
}

