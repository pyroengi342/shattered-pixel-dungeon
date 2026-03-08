package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfRemoveCurse extends InventoryScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_REMCURSE);
		preferredBag = Belongings.Backpack.class;
	}

	@Override
	public void doRead(Hero hero) {

		TormentedSpirit spirit = null;
		for (int i : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + i);
			if (ch instanceof TormentedSpirit) {
				spirit = (TormentedSpirit) ch;
				break;
			}
		}
		if (spirit != null) {
			identify(true);
			AudioWrapper.play(Assets.Sounds.READ, hero.pos);
			readAnimation(hero);

			new Flare(6, 32).show(hero.sprite, 2f);

			if (hero.buff(Degrade.class) != null) {
				Degrade.detach(hero, Degrade.class);
			}

			detach(hero.belongings.backpack);
			if (hero == Multiplayer.localHero()) {
				GLog.p(Messages.get(this, "spirit"));
			}
			spirit.cleanse();
		} else {
			super.doRead(hero);
		}
	}

	@Override
	protected boolean usableOnItem(Item item, Hero hero) {
		return uncursable(item, hero);
	}

	public static boolean uncursable(Item item, Hero hero) {
		// проверяем, что герой передан, иначе экипировка не учитывается
		if (hero != null && item.isEquipped(hero) && hero.buff(Degrade.class) != null) {
			return true;
		}
		if ((item instanceof EquipableItem || item instanceof Wand) && ((!item.isIdentified() && !item.cursedKnown) || item.cursed)) {
			return true;
		} else if (item instanceof Weapon) {
			return ((Weapon) item).hasCurseEnchant();
		} else if (item instanceof Armor) {
			return ((Armor) item).hasCurseGlyph();
		} else {
			return false;
		}
	}

	public static boolean uncursable(Item item) {
		return uncursable(item, null);
	}


	@Override
	protected void onItemSelected(Item item, Hero hero) {
		new Flare(6, 32).show(hero.sprite, 2f);

		boolean procced = uncurse(hero, item);

		if (hero.buff(Degrade.class) != null) {
			Degrade.detach(hero, Degrade.class);
			procced = true;
		}

		if (hero == Multiplayer.localHero()) {
			if (procced) {
				GLog.p(Messages.get(this, "cleansed"));
			} else {
				GLog.i(Messages.get(this, "not_cleansed"));
			}
		}
	}

	public static boolean uncurse(Hero hero, Item... items) {
		boolean procced = false;
		for (Item item : items) {
			if (item != null) {
				item.cursedKnown = true;
				if (item.cursed) {
					procced = true;
					item.cursed = false;
				}
			}
			if (item instanceof Weapon) {
				Weapon w = (Weapon) item;
				if (w.hasCurseEnchant()) {
					w.enchant(null);
					procced = true;
				}
			}
			if (item instanceof Armor) {
				Armor a = (Armor) item;
				if (a.hasCurseGlyph()) {
					a.inscribe(null);
					procced = true;
				}
			}
			if (item instanceof Wand) {
				((Wand) item).updateLevel();
			}
		}

		if (procced) {
			if (hero != null) {
				hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
				hero.updateHT(false); // for ring of might
				updateQuickslot();
			}
			if (hero == Multiplayer.localHero()) {
				Badges.validateClericUnlock();
			}
		}

		return procced;
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}