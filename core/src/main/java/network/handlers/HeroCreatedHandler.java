package network.handlers;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import io.netty.channel.ChannelHandlerContext;
import network.Multiplayer;
import network.NetworkManager;
import network.states.ClientSessionState;
import network.states.ClientStateMachine;

public class HeroCreatedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CREATED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            int playerId = bundle.getInt("playerId");
            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            // Обновляем информацию об игроке
            Multiplayer.PlayerInfo player = Multiplayer.Players.get(playerId);
            if (player == null) {
                // Если игрок ещё не известен (например, мы только что подключились),
                // можно создать временную запись, но обычно PLAYER_JOIN приходит раньше.
                // Для надёжности создадим с именем из сессии (если есть) или "Unknown".
                player = new Multiplayer.PlayerInfo(playerId, "Player " + playerId);
                Multiplayer.Players.add(player);
            }

            if (player.hero == null) {
                player.hero = new Hero();
            }
            player.hero.heroClass = heroClass;

            // Если это локальный игрок, уведомляем ClientStateMachine
            if (playerId == NetworkManager.getLocalPlayerId()) {
                ClientStateMachine.getInstance().onHeroCreated(player.hero);
            }

            System.out.println("Hero created for player " + playerId + ": " + heroClassName);
        });
    }

    // Серверный метод рассылки всем клиентам
    public static void broadcast(ClientSessionState session) {
        // Предполагаем, что у session есть доступ к hero
        if (session.stateMachine.getHero() == null) return;

        Bundle bundle = new Bundle();
        bundle.put("playerId", session.playerId);
        bundle.put("heroClass", session.stateMachine.getHero().heroClass.name());
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("HERO_CREATED", session.playerId);
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