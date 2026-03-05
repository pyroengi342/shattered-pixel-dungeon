package network.handlers;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.Multiplayer;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

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

    // Отправка конкретному клиенту
    public static void send(ChannelHandlerContext ctx, int playerId, String name) {
        Bundle bundle = new Bundle();
        bundle.put("name", name);
        BundleMessage msg = new BundleMessage("PLAYER_JOIN", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }

    // Широковещательная отправка всем, кроме отправителя (используется в ServerCore)
    public static void broadcast(Multiplayer.PlayerInfo player, ChannelHandlerContext ignore) {
        Bundle bundle = new Bundle();
        bundle.put("name", player.name);
        BundleMessage msg = new BundleMessage("PLAYER_JOIN", player.connectionID);
        msg.bundleData = bundle.toString();
        NetworkManager.broadcastMessageServer(msg, ignore);
    }
}