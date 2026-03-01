package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEnergizeItem;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class EnergizeHandler implements MessageHandler {
    @Override
    public String getType() {
        return "ENERGIZE_ITEM";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // Игнорируем собственное сообщение (действие уже выполнено локально)
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            long itemId = bundle.getLong("itemId");
            boolean energizeAll = bundle.getBoolean("energizeAll"); // true - все, false - один

            Item item = Multiplayer.findItemById(itemId);
            if (item == null) return;

            if (energizeAll) {
                WndEnergizeItem.energizeAll(item, hero);
            } else {
                WndEnergizeItem.energizeOne(item, hero);
            }
        });
    }

    // Статический метод отправки сообщения
    public static void send(Item item, boolean energizeAll) {
        Bundle bundle = new Bundle();
        bundle.put("itemId", item.getItemID());
        bundle.put("energizeAll", energizeAll);
        NetworkManager.sendMessage("ENERGIZE_ITEM", bundle);
    }
}