package network.handlers.client;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import io.netty.channel.ChannelHandlerContext;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;
import network.states.ClientSessionState;
import network.states.ClientStateMachine;

public class HeroCreatedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CREATED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // CLIENT HANDLER FROM SERVER
            int playerId = bundle.getInt("playerId");
            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            // Обновляем информацию об игроке
            Multiplayer.PlayerInfo player = Multiplayer.Players.get(playerId);
            if (player == null) {
                player = new Multiplayer.PlayerInfo(playerId, "Player " + playerId);
                Multiplayer.Players.add(player);
            }

            if (player.hero == null) {
                player.hero = new Hero(); }
            player.hero.heroClass = heroClass;

            // Если это локальный игрок, уведомляем ClientStateMachine
            if (playerId == NetworkManager.getLocalPlayerId()) {
                ClientStateMachine.getInstance().onHeroCreated(player.hero); }

            System.out.println("Hero created for player " + playerId + ": " + heroClassName);
        });
    }
    public static void sendHeroClass(HeroClass heroClass) {
        // CLIENT SEND TO SERVER
        Bundle bundle = new Bundle();
        bundle.put("heroClass", heroClass.name());
        NetworkManager.sendMessage("HERO_CLASS", bundle);
    }
}