package network.handlers.client;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.handlers.MessageHandler;
import network.NetworkManager;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

/** Client-side handler for HERO_CLASS_SELECTED */
public class HeroClassSelectedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CLASS_SELECTED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            int playerId = bundle.getInt("playerId");
            String className = bundle.getString("heroClass");
            HeroClass heroClass = HeroClass.valueOf(className);

            Multiplayer.PlayerInfo player = Multiplayer.Players.get(playerId);
            if (player == null) {
                // Create new player info if doesn't exist
                player = new Multiplayer.PlayerInfo(playerId, "Player" + playerId);
                Multiplayer.Players.add(player);
            }

            // Create hero object if doesn't exist
            if (player.hero == null) {
                player.hero = new Hero();
            }
            player.hero.heroClass = heroClass;
        });
    }
}
