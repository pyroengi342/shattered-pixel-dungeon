package network.handlers;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.LogicStateMachine;
import network.NetworkManager;

public class SeedInitHandler implements MessageHandler {
    @Override
    public String getType() { return "SEED_INIT"; }

    @Override
//     public void msgHandle(int senderId, Bundle bundle) {
//         Game.runOnRenderThread(() -> {
//             long seed = bundle.getLong("seed");
//             String customSeed = bundle.getString("customSeedText");
//             Dungeon.seed = seed;
//             NetworkManager.setSeedReceived(true);
//             Dungeon.customSeedText = customSeed != null ? customSeed : "";
//             System.out.println("Received seed: " + seed + ", custom: " + customSeed);
//         });
//     }
    public void msgHandle(int senderId, Bundle bundle) {
        long seed = bundle.getLong("seed");
        String custom = bundle.getString("customSeedText");
        Dungeon.seed = seed;
        Dungeon.customSeedText = custom;
        NetworkManager.setSeedReceived(true);
        LogicStateMachine.getInstance().onSeedInit();
    }
}