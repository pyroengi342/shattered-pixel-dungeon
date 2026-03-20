package network.handlers.server;

import com.watabou.utils.Bundle;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import network.NetworkManager;
import network.Multiplayer;
import network.handlers.MessageHandler;

public class HeroClassHandler implements MessageHandler {
    @Override
    public String getType() {
        return "HERO_CLASS";
    }

    // CLIENT sends HERO_CLASS to server with heroClass name
    public static void sendToServer(HeroClass cls) {
        Bundle bundle = new Bundle();
        bundle.put("heroClass", cls.name());
        NetworkManager.sendMessage("HERO_CLASS", bundle);
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // SERVER: Process hero class from client and broadcast to all
        if (NetworkManager.getMode() != NetworkManager.Mode.SERVER) {
            return;
        }

        String className = bundle.getString("heroClass");
        HeroClass heroClass = HeroClass.valueOf(className);

        // Update server-side PlayerInfo
        Multiplayer.PlayerInfo player = Multiplayer.Players.get(senderId);
        if (player == null) {
            // Create new PlayerInfo if doesn't exist
            player = new Multiplayer.PlayerInfo(senderId, "Player" + senderId);
            Multiplayer.Players.add(player);
        }

        // Create hero if not exists, or update class
        if (player.hero == null) {
            player.hero = new Hero();
        }
        player.hero.heroClass = heroClass;

        // Broadcast to all clients (including sender)
        broadcastHeroClass(senderId, heroClass);
    }

    private void broadcastHeroClass(int playerId, HeroClass heroClass) {
        Bundle bundle = new Bundle();
        bundle.put("playerId", playerId);
        bundle.put("heroClass", heroClass.name());

        // Create message for broadcast
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("HERO_CLASS_SELECTED", playerId);
        msg.bundleData = bundle.toString();
        NetworkManager.broadcastMessageServer(msg, null);
    }
}
