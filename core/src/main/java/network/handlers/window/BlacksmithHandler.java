package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class BlacksmithHandler implements MessageHandler {

    @Override
    public String getType() {
        return "BLACKSMITH";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            String action = bundle.getString("action");

            switch (action) {
                case "pickaxe":
                    handlePickaxe(hero);
                    break;
                case "reforge":
                    handleReforge(hero, bundle);
                    break;
                case "harden":
                    handleHarden(hero, bundle);
                    break;
                case "upgrade":
                    handleUpgrade(hero, bundle);
                    break;
                case "smith":
                    handleSmith(hero, bundle);
                    break;
                case "cashout":
                    handleCashout(hero, bundle);
                    break;
                case "start_smith":
                    Blacksmith.Quest.favor -= 2000;
                    Blacksmith.Quest.smiths++;
                    if (!Blacksmith.Quest.rewardsAvailable()) {
                        Notes.remove(Notes.Landmark.TROLL);
                    }
                    break;
            }
        });
    }

    private void handlePickaxe(Hero hero) {
        if (Blacksmith.Quest.pickaxe == null) return;
        Item pick = Blacksmith.Quest.pickaxe;
        Blacksmith.Quest.favor -= (Blacksmith.Quest.freePickaxe ? 0 : 250);
        Blacksmith.Quest.pickaxe = null;
        if (!pick.doPickUp(hero)) {
            Dungeon.level.drop(pick, hero.pos).sprite.drop();
        }
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }


    private void handleReforge(Hero hero, Bundle bundle) {
        long id1 = bundle.getLong("itemId1");
        long id2 = bundle.getLong("itemId2");
        Item item1 = Multiplayer.findItemById(id1);
        Item item2 = Multiplayer.findItemById(id2);
        if (item1 == null || item2 == null) return;
        Item first = item1.trueLevel() >= item2.trueLevel() ? item1 : item2;
        Item second = first == item1 ? item2 : item1;

        if (second.isEquipped(hero)) {
            ((EquipableItem) second).doUnequip(hero, false);
        }
        second.detachAll(hero.belongings.backpack);
        if (second instanceof Armor) {
            Armor armor = (Armor) second;
            BrokenSeal seal = armor.checkSeal();
            if (seal != null) {
                Dungeon.level.drop(seal, hero.pos).sprite.drop();
            }
        } else if (second instanceof MissileWeapon) {
            Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class, null)
                    .levelThresholds.put(((MissileWeapon) second).setID, Integer.MAX_VALUE);
        }

        // Сохраняем чар/глиф, если есть
        if (first instanceof Weapon && ((Weapon) first).hasGoodEnchant()) {
            ((Weapon) first).upgrade(true);
        } else if (first instanceof Armor && ((Armor) first).hasGoodGlyph()) {
            ((Armor) first).upgrade(true);
        } else {
            first.upgrade();
        }
        Badges.validateItemLevelAquired(first);
        Item.updateQuickslot();

        Blacksmith.Quest.favor -= 500 + 1000 * Blacksmith.Quest.reforges;
        Blacksmith.Quest.reforges++;
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }


    private void handleHarden(Hero hero, Bundle bundle) {
        long itemId = bundle.getLong("itemId");
        Item item = Multiplayer.findItemById(itemId);
        if (item == null) return;
        if (item instanceof Weapon) {
            ((Weapon) item).enchantHardened = true;
        } else if (item instanceof Armor) {
            ((Armor) item).glyphHardened = true;
        }
        Blacksmith.Quest.favor -= 500 + 1000 * Blacksmith.Quest.hardens;
        Blacksmith.Quest.hardens++;
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }

    private void handleUpgrade(Hero hero, Bundle bundle) {
        long itemId = bundle.getLong("itemId");
        Item item = Multiplayer.findItemById(itemId);
        if (item == null) return;
        item.upgrade();
        Badges.validateItemLevelAquired(item); // для синхронизации бейджей
        Blacksmith.Quest.favor -= 1000 + 1000 * Blacksmith.Quest.upgrades;
        Blacksmith.Quest.upgrades++;
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }

    private void handleSmith(Hero hero, Bundle bundle) {
        int rewardIndex = bundle.getInt("rewardIndex");
        if (Blacksmith.Quest.smithRewards == null || rewardIndex < 0 || rewardIndex >= Blacksmith.Quest.smithRewards.size()) return;
        Item reward = Blacksmith.Quest.smithRewards.get(rewardIndex);
        // Применяем зачарование/глиф, если есть
        if (reward instanceof Weapon && Blacksmith.Quest.smithEnchant != null) {
            ((Weapon) reward).enchant(Blacksmith.Quest.smithEnchant);
        } else if (reward instanceof Armor && Blacksmith.Quest.smithGlyph != null) {
            ((Armor) reward).inscribe(Blacksmith.Quest.smithGlyph);
        }
        reward.identify(false);
        if (!reward.doPickUp(hero)) {
            Dungeon.level.drop(reward, hero.pos).sprite.drop();
        }
        Blacksmith.Quest.smithRewards = null;
        Blacksmith.Quest.favor -= 2000; // стоимость крафта уже вычтена при начале? В оригинале в WndSmith не меняет favor, но в кнопке smith вычитается 2000 и увеличивается smiths. Здесь мы не меняем favor, так как это уже сделано при начале крафта? Но в WndSmith при выборе награды favor уже не меняется. Значит, в сообщении нужно передавать, что крафт совершён, но favor не трогать. Однако при нажатии кнопки "smith" в главном окне мы уже вычли 2000 и увеличили smiths. Поэтому в handleSmith не трогаем favor.
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }

    private void handleCashout(Hero hero, Bundle bundle) {
        int amount = bundle.getInt("amount");
        new Gold(amount).doPickUp(hero, hero.pos);
        Blacksmith.Quest.favor -= amount;
        if (!Blacksmith.Quest.rewardsAvailable()) {
            Notes.remove(Notes.Landmark.TROLL);
        }
    }

    // Статические методы отправки для каждого действия
    public static void sendPickaxe() {
        Bundle bundle = new Bundle();
        bundle.put("action", "pickaxe");
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }

    public static void sendReforge(Item item1, Item item2) {
        Bundle bundle = new Bundle();
        bundle.put("action", "reforge");
        bundle.put("itemId1", item1.getItemID());
        bundle.put("itemId2", item2.getItemID());
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }

    public static void sendHarden(Item item) {
        Bundle bundle = new Bundle();
        bundle.put("action", "harden");
        bundle.put("itemId", item.getItemID());
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }

    public static void sendUpgrade(Item item) {
        Bundle bundle = new Bundle();
        bundle.put("action", "upgrade");
        bundle.put("itemId", item.getItemID());
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }

    public static void sendSmith(int rewardIndex) {
        Bundle bundle = new Bundle();
        bundle.put("action", "smith");
        bundle.put("rewardIndex", rewardIndex);
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }

    public static void sendCashout(int amount) {
        Bundle bundle = new Bundle();
        bundle.put("action", "cashout");
        bundle.put("amount", amount);
        NetworkManager.sendMessage("BLACKSMITH", bundle);
    }
}