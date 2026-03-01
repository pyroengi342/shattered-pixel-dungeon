package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class ItemUseHandler implements MessageHandler {
    @Override
    public String getType() { return "ITEM_USE"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            long itemId = bundle.getLong("itemId");
            String action = bundle.getString("action");
            Integer target = bundle.contains("target") ? bundle.getInt("target") : null;

            Item item = Multiplayer.findItemById(itemId);
            if (item == null) return;

            Hero actor = Multiplayer.Players.getHero(senderId);
            if (actor == null || actor == Multiplayer.localHero()) return;

            if (target != null) {
                // item.execute(actor, action, target); // если есть
            } else {
                item.execute(actor, action);
            }
        });
    }

    public static void sendItemUse(Item item, String action, Integer target) {
        Bundle bundle = new Bundle();
        bundle.put("itemId", item.getItemID());
        bundle.put("action", action);
        if (target != null) bundle.put("target", target);
        NetworkManager.sendMessage("ITEM_USE", bundle);
    }
}