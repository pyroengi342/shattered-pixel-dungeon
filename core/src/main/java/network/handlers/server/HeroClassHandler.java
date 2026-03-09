package network.handlers.server;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;
import network.states.ClientSessionState;

public class HeroClassHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CLASS"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // SERVER HANDLER
            ClientSessionState session = NetworkManager.getSession(senderId);
            if (session == null) return;

            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            Multiplayer.PlayerInfo player = Multiplayer.Players.get(senderId);
            if (player.hero == null) {
                player.hero = new Hero(); }
            else { player.hero.heroClass = heroClass;}

            session.setHero(player.hero);
            System.out.println("Player " + senderId + " selected class: " + heroClassName);
        });
    }

    /// SERVER METHODS
    // Серверный метод рассылки всем клиентам
    public static void broadcast(ClientSessionState session) {
        if (session.stateMachine.getHero() == null) return;

        Bundle bundle = new Bundle();
        bundle.put("playerId", session.getPlayerId());
        bundle.put("heroClass", session.stateMachine.getHero().heroClass.name());
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("HERO_CREATED", session.getPlayerId());
        msg.bundleData = bundle.toString();

        // Рассылаем всем клиентам (включая самого создателя)
        NetworkManager.broadcastMessageServer(msg, null);
    }

    // Отправка конкретному клиенту (например, при подключении новому игроку)
    public static void send(ChannelHandlerContext ctx, int playerId, HeroClass heroClass) {
        Bundle bundle = new Bundle();
        bundle.put("playerId", playerId);
        bundle.put("heroClass", heroClass.name());
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("HERO_CREATED", playerId);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }
}