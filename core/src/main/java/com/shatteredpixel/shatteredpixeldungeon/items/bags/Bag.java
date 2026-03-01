package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuickBag;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Iterator;

public class Bag extends Item implements Iterable<Item> {

    public static final String AC_OPEN = "OPEN";

    {
        setImage(11);
        defaultAction = AC_OPEN;
        unique = true;
    }

    public Char owner;
    public ArrayList<Item> items = new ArrayList<>();
    public Item quickUseItem;

    // флаг загрузки, чтобы не трогать быстрые слоты
    private boolean loading = false;

    public int capacity() {
        return 20; // default container size
    }

    @Override
    public int targetingPos(Hero user, int dst) {
        if (quickUseItem != null) {
            return quickUseItem.targetingPos(user, dst);
        } else {
            return super.targetingPos(user, dst);
        }
    }

    @Override
    public void execute(Hero hero, String action) {
        quickUseItem = null;
        super.execute(hero, action);
        if (action.equals(AC_OPEN) && !items.isEmpty()) {
            GameScene.show(new WndQuickBag(this));
        }
    }

    @Override
    public boolean collect(Bag container) {
        // Сначала добавляем сумку в контейнер (это установит owner = container.owner)
        if (!super.collect(container)) {
            return false;
        }

        // Теперь у сумки есть корректный владелец
        Hero heroOwner = (owner instanceof Hero) ? (Hero) owner : null;

        // Перемещаем подходящие предметы из контейнера в сумку
        if (heroOwner != null && !loading) {
            grabItems(container);
        } else {
            // Если нет владельца или идёт загрузка, просто перемещаем без обновления слотов
            simpleGrabItems(container);
        }

        // Обновляем быстрые слоты для всех предметов в сумке
        if (heroOwner != null && !loading) {
            for (Item item : items) {
                heroOwner.quickslot.replacePlaceholder(item);
            }
        }

        Badges.validateAllBagsBought(this);
        return true;
    }

    @Override
    public void onDetach() {
        // Сохраняем владельца перед обнулением
        Hero heroOwner = (owner instanceof Hero) ? (Hero) owner : null;

        if (heroOwner != null && !loading) {
            for (Item item : items) {
                heroOwner.quickslot.clearItem(item);
            }
        }

        this.owner = null;
        updateQuickslot();
    }

    // Версия grabItems без использования быстрых слотов (для загрузки)
    private void simpleGrabItems(Bag container) {
        for (Item item : container.items.toArray(new Item[0])) {
            if (canHold(item)) {
                item.detachAll(container); // detachAll должен работать без обновления слотов, если owner null
                if (!item.collect(this)) {
                    item.collect(container);
                }
            }
        }
    }

    public void grabItems() {
        if (owner instanceof Hero && this != ((Hero) owner).belongings.backpack) {
            grabItems(((Hero) owner).belongings.backpack);
        }
    }

    public void grabItems(Bag container) {
        if (loading) return; // при загрузке не трогаем слоты
        Hero heroOwner = (owner instanceof Hero) ? (Hero) owner : null;
        if (heroOwner == null) return;

        for (Item item : container.items.toArray(new Item[0])) {
            if (canHold(item)) {
                int slot = heroOwner.quickslot.getSlot(item);
                item.detachAll(container);
                if (!item.collect(this)) {
                    item.collect(container);
                }
                if (slot != -1) {
                    heroOwner.quickslot.setSlot(slot, item);
                }
            }
        }
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    public void clear() {
        items.clear();
    }

    public void resurrect() {
        for (Item item : items.toArray(new Item[0])) {
            if (!item.unique) items.remove(item);
        }
    }

    private static final String ITEMS = "inventory";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(ITEMS, items);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        loading = true;
        for (Bundlable item : bundle.getCollection(ITEMS)) {
            if (item != null) {
                if (!((Item) item).collect(this)) {
                    items.add((Item) item);
                }
            }
        }
        loading = false;
    }

    public boolean contains(Item item) {
        for (Item i : items) {
            if (i == item) {
                return true;
            } else if (i instanceof Bag && ((Bag) i).contains(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean canHold(Item item) {
        if (!loading && owner != null && owner.buff(LostInventory.class) != null
                && !item.keptThroughLostInventory()) {
            return false;
        }

        if (items.contains(item) || item instanceof Bag || items.size() < capacity()) {
            return true;
        } else if (item.stackable) {
            for (Item i : items) {
                if (item.isSimilar(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<Item> iterator() {
        return new ItemIterator();
    }

    private class ItemIterator implements Iterator<Item> {

        private int index = 0;
        private Iterator<Item> nested = null;

        @Override
        public boolean hasNext() {
            if (nested != null) {
                return nested.hasNext() || index < items.size();
            } else {
                return index < items.size();
            }
        }

        @Override
        public Item next() {
            if (nested != null && nested.hasNext()) {
                return nested.next();
            } else {
                nested = null;
                Item item = items.get(index++);
                if (item instanceof Bag) {
                    nested = ((Bag) item).iterator();
                }
                return item;
            }
        }

        @Override
        public void remove() {
            if (nested != null) {
                nested.remove();
            } else {
                items.remove(index);
            }
        }
    }
}