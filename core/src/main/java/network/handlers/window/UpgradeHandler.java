package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

import static network.Multiplayer.findItemById;

public class UpgradeHandler implements MessageHandler {
    @Override
    public String getType() { return "UPGRADE_ITEM"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            long upgraderId = bundle.getLong("upgraderId");
            long targetId = bundle.getLong("targetId");
            boolean force = bundle.getBoolean("force");

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null || hero == Multiplayer.localHero()) return;

            Item upgrader = findItemById(upgraderId);
            Item target = findItemById(targetId);
            if (upgrader == null || target == null) return;

            if (upgrader instanceof ScrollOfUpgrade) {
                ((ScrollOfUpgrade) upgrader).upgradeItem(target, hero);
            } else if (upgrader instanceof MagicalInfusion) {
                ((MagicalInfusion) upgrader).upgradeItem(target, hero);
            } else return;

            if (!force) upgrader.detach(hero.belongings.backpack);
        });
    }

    // Статический метод отправки
    public static void sendUpgrade(Item upgrader, Item target, boolean force) {
        Bundle bundle = new Bundle();
        bundle.put("upgraderId", upgrader.getItemID());
        bundle.put("targetId", target.getItemID());
        bundle.put("force", force);
        NetworkManager.sendMessage("UPGRADE_ITEM", bundle);
    }
}