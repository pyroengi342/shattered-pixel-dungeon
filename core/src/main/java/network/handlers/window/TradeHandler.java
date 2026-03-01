package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class TradeHandler implements MessageHandler {

    @Override
    public String getType() {
        return "TRADE"; // Можно использовать общий тип с подтипами внутри Bundle
    }

    @Override
    public void msgHandle(int senderId, Bundle data) {
        if (data == null) return;
        String action = data.getString("action");
        Hero actor = Multiplayer.Players.getHero(senderId);
        if (actor == null || actor == Multiplayer.localHero()) return; // не обрабатываем свои действия

        switch (action) {
            case "SELL":
                handleSell(actor, data);
                break;
            case "BUY":
                handleBuy(actor, data);
                break;
            case "STEAL":
                handleSteal(actor, data);
                break;
        }
    }

    private void handleSell(Hero seller, Bundle data) {
        long itemId = data.getLong("itemId");
        int quantity = data.getInt("quantity");

        Item item = Multiplayer.findItemById(itemId);
        if (item == null) return;

        Shopkeeper shop = findShopkeeper();
        if (shop == null) return;

        if (quantity == item.quantity()) {
            WndTradeItem.sell(item, shop, seller);
        } else {
            WndTradeItem.sellOne(item, shop, seller);
        }
    }

    private void handleBuy(Hero buyer, Bundle data) {
        int heapPos = data.getInt("heapPos");
        Heap heap = Dungeon.level.heaps.get(heapPos);
        if (heap == null) return;

        Item item = heap.pickUp();
        if (item == null) return;
        int price = Shopkeeper.sellPrice(item);
        Dungeon.gold -= price;
        Catalog.countUses(Gold.class, price);
        if (!item.doPickUp(buyer)) {
            Dungeon.level.drop(item, heapPos).sprite.drop();
        }
    }

    private void handleSteal(Hero thief, Bundle data) {
        boolean success = data.getBoolean("success");
        int heapPos = data.getInt("heapPos");
        if (success) {
            long itemId = data.getLong("itemId");
            Heap heap = Dungeon.level.heaps.get(heapPos);
            if (heap == null) return;
            Item item = heap.pickUp();
            if (item == null) return;
            if (!item.doPickUp(thief)) {
                Dungeon.level.drop(item, heapPos).sprite.drop();
            }
        } else {
            // Неудачная кража – магазин убегает
            for (Mob mob : Dungeon.level.mobs) {
                if (mob instanceof Shopkeeper) {
                    mob.yell(Messages.get(mob, "thief"));
                    ((Shopkeeper) mob).flee();
                    break;
                }
            }
        }
    }

    private Shopkeeper findShopkeeper() {
        for (Char ch : Actor.chars()) {
            if (ch instanceof Shopkeeper) {
                return (Shopkeeper) ch;
            }
        }
        return null;
    }

    public static void sendSell(Item item, int quantity, Hero hero) {
        Bundle bundle = new Bundle();
        bundle.put("action", "SELL");
        bundle.put("itemId", item.getItemID());
        bundle.put("quantity", quantity);
        sendTradeMessage(bundle);
    }

    public static void sendBuy(Heap heap, Hero hero) {
        Bundle bundle = new Bundle();
        bundle.put("action", "BUY");
        bundle.put("heapPos", heap.pos);
        sendTradeMessage(bundle);
    }

    public static void sendSteal(Item item, boolean success, Hero hero, Heap heap) {
        Bundle bundle = new Bundle();
        bundle.put("action", "STEAL");
        bundle.put("itemId", item.getItemID());
        bundle.put("success", success);
        bundle.put("heapPos", heap.pos);
        sendTradeMessage(bundle);
    }

    private static void sendTradeMessage(Bundle bundle) {
        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("TRADE", NetworkManager.getLocalPlayerId());
        msg.bundleData = bundle.toString();
        if (NetworkManager.getMode() == NetworkManager.Mode.CLIENT) {
            NetworkManager.sendToServer(msg);
        } else if (NetworkManager.getMode() == NetworkManager.Mode.SERVER) {
            NetworkManager.broadcastMessageServer(msg);
        }
    }
}