package network.handlers;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;

public class HeroClassHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CLASS"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            if (NetworkManager.getMode() == NetworkManager.Mode.NONE) return;
            String heroClassName = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(heroClassName);

            Multiplayer.PlayerInfo player = Multiplayer.Players.get(senderId);
            if (player == null) {
                player = new Multiplayer.PlayerInfo(senderId, "Player " + senderId);
                Multiplayer.Players.add(player);
            }

            if (player.hero == null) {
                Hero hero = new Hero();
                hero.live();
                heroClass.initHero(hero);
                player.hero = hero;
            } else {
                player.hero.heroClass = heroClass;
            }
            System.out.println("Player " + senderId + " selected class: " + heroClassName);
            // В конце, после создания героя:
            Bundle heroBundle = new Bundle();
            heroBundle.put("playerId", senderId);
            NetworkManager.sendMessage("HERO_CREATED", heroBundle);
        });
        }
}