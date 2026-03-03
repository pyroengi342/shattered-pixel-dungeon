package network.handlers;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.ClientStateMachine;
import network.NetworkManager;
import network.NetworkManager.BundleMessage;

public class SeedInitHandler implements MessageHandler {
    @Override
    public String getType() { return "SEED_INIT"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        long seed = bundle.getLong("seed");
        String custom = bundle.getString("customSeedText");
        Dungeon.seed = seed;
        Dungeon.customSeedText = custom;
        ClientStateMachine.getInstance().onSeedInit();
    }

    // Отправка seed конкретному клиенту
    public static void send(ChannelHandlerContext ctx) {
        if (Dungeon.seed == 0) return;
        Bundle bundle = new Bundle();
        bundle.put("seed", Dungeon.seed);
        bundle.put("customSeedText", Dungeon.customSeedText);
        BundleMessage msg = new BundleMessage("SEED_INIT", -1);
        msg.bundleData = bundle.toString();
        ctx.writeAndFlush(msg);
    }

    // public void sendHeroClassImpl(HeroClass heroClass) {
    //     Bundle bundle = new Bundle();
    //     bundle.put("heroClass", heroClass.name());
    //     sendMessage("HERO_CLASS", bundle);
    // }
}