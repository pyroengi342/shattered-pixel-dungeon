package network.handlers.server;

import com.watabou.utils.Bundle;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import network.NetworkManager;
import network.Multiplayer;
import network.handlers.MessageHandler;

public class HeroClassHandler implements MessageHandler {
    @Override
    public String getType() {
        return "HERO_CLASS";
    }

    // CLIENT sends HERO_CLASS with heroClass name
    public static void sendToServer(HeroClass cls) {
        Bundle bundle = new Bundle();
        bundle.put("heroClass", cls.name());
        NetworkManager.sendMessage("HERO_CLASS", bundle);
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // This server-side handler would process HERO_CLASS from a client if needed.
        // For this task, we keep a placeholder to satisfy imports during compile.
    }
}
