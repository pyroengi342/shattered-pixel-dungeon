package network.handlers.client;

import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.handlers.MessageHandler;
import network.states.ClientStateMachine;
import network.Multiplayer;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

public class PlayerAssignHandler implements MessageHandler {
    @Override
    public String getType() { return "PLAYER_ASSIGN"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // CLIENT HANDLER
        int assignedId = bundle.getInt("assignedId");
        String name = bundle.getString("name");
        NetworkManager.setLocalPlayerId(assignedId);

        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(assignedId, name);
        player.isLocal = true;
        Multiplayer.Players.add(player);
        ClientStateMachine.getInstance().onPlayerAssign(assignedId, name);
    }

    /// SERVER METHODS
    public static void send(ChannelHandlerContext ctx, int playerId, String name) {
        // SERVER SENDS
        Bundle bundle = new Bundle();
        bundle.put("assignedId", playerId);
        bundle.put("name", name);
        BundleMessage msg = new BundleMessage("PLAYER_ASSIGN", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }

}