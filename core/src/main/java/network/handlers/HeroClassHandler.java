package network.handlers;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.states.ClientSessionState;
import network.states.ClientStateMachine;

public class HeroClassHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CLASS"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            ClientSessionState session = NetworkManager.getSession(senderId);
            if (session == null) return;

            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            Multiplayer.PlayerInfo player = Multiplayer.Players.get(senderId);

            if (player.hero == null) {
                player.hero = new Hero();
            } else {
                player.hero.heroClass = heroClass;
            }
            session.setHero(player.hero);
            System.out.println("Player " + senderId + " selected class: " + heroClassName);
        });
    }
    public static void sendHeroClass(HeroClass heroClass) {
        Bundle bundle = new Bundle();
        bundle.put("heroClass", heroClass.name());
        NetworkManager.sendMessage("HERO_CLASS", bundle);
    }
}