package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import network.AudioWrapper;
import network.Multiplayer;

import java.util.ArrayList;

public class LiquidMetal extends Item {

    {
        image = ItemSpriteSheet.LIQUID_METAL;
        stackable = true;
        defaultAction = AC_APPLY;
        bones = true;
    }

    private static final String AC_APPLY = "APPLY";

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_APPLY);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_APPLY)) {
            setCurrent(hero); // устанавливаем curUser = hero
            // Передаём героя в itemSelector через замыкание или поле. Создадим анонимный класс, захватывающий hero.
            GameScene.selectItem(new LiquidMetalSelector(hero, this));
        }
    }

    @Override
    protected void onThrow(int cell) {
        if (Dungeon.level.map[cell] == Terrain.WELL || Dungeon.level.pit[cell]) {
            super.onThrow(cell);
        } else {
            Dungeon.level.pressCell(cell);
            Hero local = Multiplayer.localHero();
            if (local != null && local.fieldOfView != null && local.fieldOfView[cell]) {
                GLog.i(Messages.get(Potion.class, "shatter"));
                AudioWrapper.play(Assets.Sounds.SHATTER, cell);
                Splash.at(cell, 0xBFBFBF, 5);
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

    @Override
    public int value() {
        return quantity;
    }

    // Внутренний класс, реализующий ItemSelector с сохранённым героем
    private class LiquidMetalSelector extends WndBag.ItemSelector {
        private Hero hero;
        private LiquidMetal item;

        LiquidMetalSelector(Hero hero, LiquidMetal item) {
            this.hero = hero;
            this.item = item;
        }

        @Override
        public String textPrompt() {
            return Messages.get(LiquidMetal.class, "prompt");
        }

        @Override
        public Class<? extends Bag> preferredBag() {
            return MagicalHolster.class;
        }

        @Override
        public boolean itemSelectable(Item item) {
            return item instanceof MissileWeapon && !(item instanceof Dart);
        }

        @Override
        public void onSelect(Item selected) {
            if (selected != null && selected instanceof MissileWeapon) {
                MissileWeapon m = (MissileWeapon) selected;

                float maxToUse = 5 * (m.tier + 1);
                maxToUse *= Math.pow(1.35f, m.level());

                float durabilityPerMetal = 100 / maxToUse;

                float percentDurabilityLost = 0.999f - (m.durabilityLeft() / 100f);
                int toUse = (int) Math.ceil(maxToUse * percentDurabilityLost);
                if (toUse == 0 ||
                        Math.ceil(m.durabilityLeft() / m.durabilityPerUse()) >= Math.ceil(m.MAX_DURABILITY / m.durabilityPerUse())) {

                    if (m.quantity() < m.defaultQuantity()) {
                        if (item.quantity() * durabilityPerMetal >= m.durabilityPerUse()) {
                            m.quantity(m.quantity() + 1);
                            if (maxToUse < item.quantity()) {
                                Catalog.countUses(LiquidMetal.class, (int) Math.ceil(maxToUse));
                                if (hero == Multiplayer.localHero()) {
                                    GLog.i(Messages.get(LiquidMetal.class, "apply", (int) Math.ceil(maxToUse)));
                                }
                                item.quantity(item.quantity() - (int) Math.ceil(maxToUse));
                            } else {
                                Catalog.countUses(LiquidMetal.class, item.quantity());
                                m.damage(100f);
                                m.repair(item.quantity() * durabilityPerMetal - 1);
                                if (hero == Multiplayer.localHero()) {
                                    GLog.i(Messages.get(LiquidMetal.class, "apply", item.quantity()));
                                }
                                item.detachAll(hero.belongings.backpack);
                            }
                        } else {
                            if (hero == Multiplayer.localHero()) {
                                GLog.w(Messages.get(LiquidMetal.class, "already_fixed"));
                            }
                            return;
                        }
                    } else {
                        if (hero == Multiplayer.localHero()) {
                            GLog.w(Messages.get(LiquidMetal.class, "already_fixed"));
                        }
                        return;
                    }
                } else if (toUse < item.quantity()) {
                    Catalog.countUses(LiquidMetal.class, toUse);
                    m.repair(maxToUse * durabilityPerMetal);
                    item.quantity(item.quantity() - toUse);
                    if (hero == Multiplayer.localHero()) {
                        GLog.i(Messages.get(LiquidMetal.class, "apply", toUse));
                    }
                } else {
                    Catalog.countUses(LiquidMetal.class, item.quantity());
                    m.repair(item.quantity() * durabilityPerMetal);
                    if (hero == Multiplayer.localHero()) {
                        GLog.i(Messages.get(LiquidMetal.class, "apply", item.quantity()));
                    }
                    item.detachAll(hero.belongings.backpack);
                }

                hero.sprite.operate(hero.pos);
                if (hero == Multiplayer.localHero()) {
                    AudioWrapper.play(Assets.Sounds.DRINK, hero.pos);
                }
                updateQuickslot();
                if (hero == Multiplayer.localHero()) {
                    hero.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.1f, 10);
                }
            }
        }
    }

    public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

        @Override
        public boolean testIngredients(ArrayList<Item> ingredients) {
            return ingredients.size() == 1
                    && ingredients.get(0) instanceof MissileWeapon
                    && ingredients.get(0).cursedKnown
                    && !ingredients.get(0).cursed;
        }

        @Override
        public int cost(ArrayList<Item> ingredients) {
            return 3;
        }

		@Override
		public Item brew(ArrayList<Item> ingredients, Hero hero) {
			Item result = sampleOutput(ingredients);
			MissileWeapon m = (MissileWeapon) ingredients.get(0);
			if (!m.levelKnown) {
				result.quantity(metalQuantity(m));
			}
			m.quantity(0);
			// Используем переданного героя
			if (hero != null) {
				Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class, hero)
						.levelThresholds.put(m.setID, Integer.MAX_VALUE);
			}
			return result;
		}

        @Override
        public Item sampleOutput(ArrayList<Item> ingredients) {
            MissileWeapon m = (MissileWeapon) ingredients.get(0);
            if (m.levelKnown) {
                return new LiquidMetal().quantity(metalQuantity(m));
            } else {
                return new LiquidMetal();
            }
        }

        private int metalQuantity(MissileWeapon m) {
            float quantityPerWeapon = 5 * (m.tier + 1);
            if (m.defaultQuantity() != 3) {
                quantityPerWeapon = 3f / m.defaultQuantity();
            }
            quantityPerWeapon *= Math.pow(1.35f, Math.min(5, m.level()));

            float quantity = m.quantity() - 1;
            quantity += 0.25f + 0.0075f * m.durabilityLeft();

            return Math.round(quantity * quantityPerWeapon);
        }
    }
}