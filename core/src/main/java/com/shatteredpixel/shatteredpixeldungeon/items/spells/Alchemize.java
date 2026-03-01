package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEnergizeItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import network.Multiplayer;

import java.util.ArrayList;

public class Alchemize extends Spell {

    {
        setImage(ItemSpriteSheet.ALCHEMIZE);
        talentChance = 1 / (float) Recipe.OUT_QUANTITY;
    }

    @Override
    protected void onCast(Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override
            public String textPrompt() {
                return Messages.get(Alchemize.class, "prompt");
            }

            @Override
            public boolean itemSelectable(Item item) {
                return !(item instanceof Alchemize)
                        && (Shopkeeper.canSell(item, hero) || item.energyVal() > 0);
            }

            @Override
            public void onSelect(Item item) {
                if (item != null) {
                    GameScene.show(new WndAlchemizeItem(item, hero));
                }
            }
        });
    }

    @Override
    public int value() {
        return (int) (20 * (quantity / (float) Recipe.OUT_QUANTITY));
    }

    @Override
    public int energyVal() {
        return (int) (4 * (quantity / (float) Recipe.OUT_QUANTITY));
    }

    public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

        private static final int OUT_QUANTITY = 8;

        @Override
        public boolean testIngredients(ArrayList<Item> ingredients) {
            if (ingredients.size() != 2) return false;
            return (ingredients.get(0) instanceof Plant.Seed && ingredients.get(1) instanceof Runestone)
                    || (ingredients.get(0) instanceof Runestone && ingredients.get(1) instanceof Plant.Seed);
        }

        @Override
        public int cost(ArrayList<Item> ingredients) {
            return 2;
        }

        @Override
        public Item brew(ArrayList<Item> ingredients, Hero hero) {
            ingredients.get(0).quantity(ingredients.get(0).quantity() - 1);
            ingredients.get(1).quantity(ingredients.get(1).quantity() - 1);
            return sampleOutput(null);
        }

        @Override
        public Item sampleOutput(ArrayList<Item> ingredients) {
            return new Alchemize().quantity(OUT_QUANTITY);
        }
    }

    public class WndAlchemizeItem extends WndInfoItem {

        private static final float GAP = 2;
        private static final int BTN_HEIGHT = 18;

        private final Hero hero;

        public WndAlchemizeItem(Item item, Hero hero) {
            super(item);
            this.hero = hero;

            float pos = height;

            if (Shopkeeper.canSell(item, hero)) {
                if (item.quantity() == 1 || (item instanceof MissileWeapon && item.isUpgradable())) {

                    if (item instanceof MissileWeapon && ((MissileWeapon) item).extraThrownLeft) {
                        RenderedTextBlock warn = PixelScene.renderTextBlock(Messages.get(WndUpgrade.class, "thrown_dust"), 6);
                        warn.hardlight(CharSprite.WARNING);
                        warn.maxWidth(width);
                        warn.setPos(0, pos + GAP);
                        add(warn);
                        pos = warn.bottom();
                    }

                    RedButton btnSell = new RedButton(Messages.get(this, "sell", item.value())) {
                        @Override
                        protected void onClick() {
                            WndTradeItem.sell(item, hero);
                            hide();
                            consumeAlchemize(item);
                        }
                    };
                    btnSell.setRect(0, pos + GAP, width, BTN_HEIGHT);
                    btnSell.icon(new ItemSprite(ItemSpriteSheet.GOLD));
                    add(btnSell);
                    pos = btnSell.bottom();

                } else {
                    int priceAll = item.value();
                    RedButton btnSell1 = new RedButton(Messages.get(this, "sell_1", priceAll / item.quantity())) {
                        @Override
                        protected void onClick() {
                            WndTradeItem.sellOne(item, hero);
                            hide();
                            consumeAlchemize(item);
                        }
                    };
                    btnSell1.setRect(0, pos + GAP, width, BTN_HEIGHT);
                    btnSell1.icon(new ItemSprite(ItemSpriteSheet.GOLD));
                    add(btnSell1);
                    RedButton btnSellAll = new RedButton(Messages.get(this, "sell_all", priceAll)) {
                        @Override
                        protected void onClick() {
                            WndTradeItem.sell(item, hero);
                            hide();
                            consumeAlchemize(item);
                        }
                    };
                    btnSellAll.setRect(0, btnSell1.bottom() + 1, width, BTN_HEIGHT);
                    btnSellAll.icon(new ItemSprite(ItemSpriteSheet.GOLD));
                    add(btnSellAll);
                    pos = btnSellAll.bottom();
                }
            }

            if (item.energyVal() > 0) {
                if (item.quantity() == 1) {
                    RedButton btnEnergize = new RedButton(Messages.get(this, "energize", item.energyVal())) {
                        @Override
                        protected void onClick() {
                            if (item instanceof Trinket) {
                                GameScene.show(new WndOptions(new ItemSprite(item), Messages.titleCase(item.name()),
                                        Messages.get(WndEnergizeItem.class, "trinket_warn"),
                                        Messages.get(WndEnergizeItem.class, "trinket_yes"),
                                        Messages.get(WndEnergizeItem.class, "trinket_no")) {
                                    @Override
                                    protected void onSelect(int index) {
                                        if (index == 0) {
                                            WndEnergizeItem.energizeAll(item, hero);
                                            hide();
                                            consumeAlchemize(item);
                                        }
                                    }
                                });
                            } else {
                                WndEnergizeItem.energizeAll(item, hero);
                                hide();
                                consumeAlchemize(item);
                            }
                        }
                    };
                    btnEnergize.setRect(0, pos + GAP, width, BTN_HEIGHT);
                    btnEnergize.icon(new ItemSprite(ItemSpriteSheet.ENERGY));
                    add(btnEnergize);
                    pos = btnEnergize.bottom();

                } else {
                    int energyAll = item.energyVal();
                    RedButton btnEnergize1 = new RedButton(Messages.get(this, "energize_1", energyAll / item.quantity())) {
                        @Override
                        protected void onClick() {
                            WndEnergizeItem.energizeOne(item, hero);
                            hide();
                            consumeAlchemize(item);
                        }
                    };
                    btnEnergize1.setRect(0, pos + GAP, width, BTN_HEIGHT);
                    btnEnergize1.icon(new ItemSprite(ItemSpriteSheet.ENERGY));
                    add(btnEnergize1);
                    RedButton btnEnergizeAll = new RedButton(Messages.get(this, "energize_all", energyAll)) {
                        @Override
                        protected void onClick() {
                            WndEnergizeItem.energizeAll(item, hero);
                            hide();
                            consumeAlchemize(item);
                        }
                    };
                    btnEnergizeAll.setRect(0, btnEnergize1.bottom() + 1, width, BTN_HEIGHT);
                    btnEnergizeAll.icon(new ItemSprite(ItemSpriteSheet.ENERGY));
                    add(btnEnergizeAll);
                    pos = btnEnergizeAll.bottom();
                }
            }

            resize(width, (int) pos);
        }

        private void consumeAlchemize(Item item) {
            Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

            // Удаляем текущее заклинание (Alchemize.this)
            if (Alchemize.this.quantity() <= 1) {
                Alchemize.this.detachAll(hero.belongings.backpack);
            } else {
                Alchemize.this.detach(hero.belongings.backpack);
            }

            // Если после использования остались заклинания, открываем новое окно выбора
            if (Alchemize.this.quantity() > 0) {
                GameScene.selectItem(new WndBag.ItemSelector() {
                    @Override
                    public String textPrompt() {
                        return Messages.get(Alchemize.class, "prompt");
                    }

                    @Override
                    public boolean itemSelectable(Item item) {
                        return !(item instanceof Alchemize)
                                && (Shopkeeper.canSell(item, hero) || item.energyVal() > 0);
                    }

                    @Override
                    public void onSelect(Item item) {
                        if (item != null) {
                            GameScene.show(new WndAlchemizeItem(item, hero));
                        }
                    }
                });
            }

            if (hero == Multiplayer.localHero()) {
                Catalog.countUse(Alchemize.class);
                if (Random.Float() < Alchemize.this.talentChance) {
                    Talent.onScrollUsed(hero, hero.pos, Alchemize.this.talentFactor, Alchemize.class);
                }
            }
        }
    }
}