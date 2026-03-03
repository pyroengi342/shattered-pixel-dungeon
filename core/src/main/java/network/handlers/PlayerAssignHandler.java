package network.handlers;

import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.states.ClientStateMachine;
import network.Multiplayer;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

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

        ClientStateMachine.getInstance().onPlayerAssign(assignedId, name);
    }

    // Статический метод для отправки сообщения конкретному клиенту ОТ СЕРВЕРА
    public static void send(ChannelHandlerContext ctx, int playerId, String name) {
        Bundle bundle = new Bundle();
        bundle.put("assignedId", playerId);
        bundle.put("name", name);
        BundleMessage msg = new BundleMessage("PLAYER_ASSIGN", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }

}