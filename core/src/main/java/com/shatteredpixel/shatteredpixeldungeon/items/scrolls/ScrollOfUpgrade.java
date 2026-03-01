// ScrollOfUpgrade.java (исправленный)
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade;

import network.Multiplayer;

public class ScrollOfUpgrade extends InventoryScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_UPGRADE);
		preferredBag = Belongings.Backpack.class;

		unique = true;

		talentFactor = 2f;
	}

	@Override
	protected boolean usableOnItem(Item item, Hero hero) {
		return item.isUpgradable();
	}

	@Override
	protected void onItemSelected(Item item, Hero hero) {
		GameScene.show(new WndUpgrade(this, item, hero, identifiedByUse));
	}

	public void reShowSelector(Hero hero, boolean force){
		identifiedByUse = force;
		GameScene.selectItem(new InventoryItemSelector(this, hero));
	}

	public Item upgradeItem(Item item, Hero hero) {
		upgrade(hero);

		Degrade.detach(hero, Degrade.class);

		if (item instanceof Weapon) {
			Weapon w = (Weapon) item;
			boolean wasCursed = w.cursed;
			boolean wasHardened = w.enchantHardened;
			boolean hadCursedEnchant = w.hasCurseEnchant();
			boolean hadGoodEnchant = w.hasGoodEnchant();

			item = w.upgrade();

			if (w.cursedKnown && hadCursedEnchant && !w.hasCurseEnchant()) {
				removeCurse(hero);
			} else if (w.cursedKnown && wasCursed && !w.cursed) {
				weakenCurse(hero);
			}
			if (wasHardened && !w.enchantHardened) {
				if (hero == Multiplayer.localHero()) GLog.w(Messages.get(Weapon.class, "hardening_gone"));
			} else if (hadGoodEnchant && !w.hasGoodEnchant()) {
				if (hero == Multiplayer.localHero()) GLog.w(Messages.get(Weapon.class, "incompatible"));
			}

		} else if (item instanceof Armor) {
			Armor a = (Armor) item;
			boolean wasCursed = a.cursed;
			boolean wasHardened = a.glyphHardened;
			boolean hadCursedGlyph = a.hasCurseGlyph();
			boolean hadGoodGlyph = a.hasGoodGlyph();

			item = a.upgrade();

			if (a.cursedKnown && hadCursedGlyph && !a.hasCurseGlyph()) {
				removeCurse(hero);
			} else if (a.cursedKnown && wasCursed && !a.cursed) {
				weakenCurse(hero);
			}
			if (wasHardened && !a.glyphHardened) {
				if (hero == Multiplayer.localHero()) GLog.w(Messages.get(Armor.class, "hardening_gone"));
			} else if (hadGoodGlyph && !a.hasGoodGlyph() && hero == Multiplayer.localHero()) GLog.w(Messages.get(Armor.class, "incompatible"));


		} else if (item instanceof Wand || item instanceof Ring) {
			boolean wasCursed = item.cursed;

			item = item.upgrade();

			if (item.cursedKnown && wasCursed && !item.cursed) {
				removeCurse(hero);
			}

		} else {
			item = item.upgrade();
		}

		Badges.validateItemLevelAquired(item);
		Statistics.upgradesUsed++;
		Badges.validateMageUnlock();

		if (hero == Multiplayer.localHero()) {
			Catalog.countUse(item.getClass());
		}

		return item;
	}

	public static void upgrade(Hero hero) {
		if (hero == Multiplayer.localHero()) {
			hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
		}
	}

	public static void weakenCurse(Hero hero) {
		if (hero == Multiplayer.localHero()) {
			GLog.p(Messages.get(ScrollOfUpgrade.class, "weaken_curse"));
			hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 5);
		}
	}

	public static void removeCurse(Hero hero) {
		if (hero == Multiplayer.localHero()) {
			GLog.p(Messages.get(ScrollOfUpgrade.class, "remove_curse"));
			hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
			Badges.validateClericUnlock();
		}
	}

	@Override
	public int value() {
		return isKnown() ? 50 * quantity : super.value();
	}

	@Override
	public int energyVal() {
		return isKnown() ? 10 * quantity : super.energyVal();
	}
}