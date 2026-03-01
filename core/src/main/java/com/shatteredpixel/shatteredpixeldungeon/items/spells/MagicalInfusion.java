package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import network.Multiplayer;

public class MagicalInfusion extends InventorySpell {

	{
		setImage(ItemSpriteSheet.MAGIC_INFUSE);
		unique = true;
		talentFactor = 2;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item.isUpgradable();
	}

	@Override
	protected void onItemSelected(Item item, Hero hero) {
		GameScene.show(new WndUpgrade(this, item, hero, false));
	}

	public void reShowSelector(Hero hero) {
		GameScene.selectItem(new InventorySpellSelector(this, hero));
	}

	public void useAnimation(Hero hero) {
		hero.spend(1f);
		hero.busy();
		hero.sprite.operate(hero.pos);

		Sample.INSTANCE.play(Assets.Sounds.READ);
		Invisibility.dispel(hero);

		if (hero == Multiplayer.localHero()) {
			Catalog.countUse(getClass());
			if (Random.Float() < talentChance) {
				Talent.onScrollUsed(hero, hero.pos, talentFactor, getClass());
			}
		}
	}

	public Item upgradeItem(Item item, Hero hero) {
		ScrollOfUpgrade.upgrade(hero);

		Degrade.detach(hero, Degrade.class);

		if (item instanceof Weapon && ((Weapon) item).enchantment != null) {
			item = ((Weapon) item).upgrade(true);
		} else if (item instanceof Armor && ((Armor) item).glyph != null) {
			item = ((Armor) item).upgrade(true);
		} else {
			boolean wasCursed = item.cursed;
			boolean wasCurseInfused = item instanceof Wand && ((Wand) item).curseInfusionBonus;
			item = item.upgrade();
			if (wasCursed) item.cursed = true;
			if (wasCurseInfused) ((Wand) item).curseInfusionBonus = true;
		}

		if (hero == Multiplayer.localHero()) {
			Catalog.countUse(item.getClass());
			Statistics.upgradesUsed++;
			GLog.p(Messages.get(this, "infuse"));
			Badges.validateItemLevelAquired(item);
		}

		return item;
	}

	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public int energyVal() {
		return 12 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ScrollOfUpgrade.class};
			inQuantity = new int[]{1};
			cost = 12;
			output = MagicalInfusion.class;
			outQuantity = 1;
		}
	}
}