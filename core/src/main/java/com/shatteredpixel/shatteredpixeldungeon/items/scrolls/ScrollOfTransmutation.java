package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Transmuting;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

import network.Multiplayer;

public class ScrollOfTransmutation extends InventoryScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_TRANSMUTE);
		bones = true;
		talentFactor = 2f;
	}

	@Override
	protected boolean usableOnItem(Item item, Hero hero) {
		//all melee weapons, except pickaxe when in a mining level
		if (item instanceof MeleeWeapon){
			return !(item instanceof Pickaxe && Dungeon.level instanceof MiningLevel);
			//all missile weapons except untipped darts
		} else if (item instanceof MissileWeapon){
			return item.getClass() != Dart.class;
			//all regular or exotic potions. No brews or elixirs
		} else if (item instanceof Potion){
			return !(item instanceof Elixir || item instanceof Brew);
			//all regular or exotic scrolls, except itself (unless un-ided, in which case it was already consumed)
		} else if (item instanceof Scroll) {
			return item != this || item.quantity() > 1 || identifiedByUse;
			//all non-unique artifacts (no holy tome or cloak of shadows, basically)
		} else if (item instanceof Artifact) {
			return !item.unique;
			//all rings, wands, trinkets, seeds, and runestones
		} else {
			return item instanceof Ring || item instanceof Wand || item instanceof Trinket
					|| item instanceof Plant.Seed || item instanceof Runestone;
		}
	}

	@Override
	protected void onItemSelected(Item item, Hero hero) {

		Item result = changeItem(item, hero);

		if (result == null){
			GLog.n( Messages.get(this, "nothing") );
			// если ничего не вышло, возвращаем свиток обратно (если он был изъят)
			// но в InventoryScroll уже есть логика, что свиток удаляется, поэтому здесь нужно восстановить
			// В оригинале тут был curItem.collect(curUser.belongings.backpack) – но curItem у нас нет, вместо этого мы можем попробовать вернуть свиток, но он уже может быть удалён.
			// В текущей логике свиток уже удалён до вызова onItemSelected, так что просто ничего не делаем.
		} else {
			if (result != item) {
				int slot = hero.quickslot.getSlot(item);
				if (item.isEquipped(hero)) {
					item.cursed = false; // to allow it to be unequipped
					if (item instanceof Artifact && result instanceof Ring){
						// if we turned an equipped artifact into a ring, ring goes into inventory
						((EquipableItem) item).doUnequip(hero, false);
						if (!result.collect(hero)){
							Dungeon.level.drop(result, hero.pos).sprite.drop();
						}
					} else if (item instanceof KindOfWeapon && hero.belongings.secondWep() == item){
						((EquipableItem) item).doUnequip(hero, false);
						((KindOfWeapon) result).equipSecondary(hero);
					} else {
						((EquipableItem) item).doUnequip(hero, false);
						((EquipableItem) result).doEquip(hero);
					}
					hero.spend(-hero.cooldown()); // cancel equip/unequip time
				} else {
					if (item instanceof MissileWeapon && !(item instanceof TippedDart)){
						item.detachAll(hero.belongings.backpack);
					} else {
						item.detach(hero.belongings.backpack);
					}
					if (!result.collect(hero)) {
						Dungeon.level.drop(result, hero.pos).sprite.drop();
					} else if (result.stackable && hero.belongings.getSimilar(result) != null){
						result = hero.belongings.getSimilar(result);
					}
				}
				if (slot != -1
						&& result.defaultAction() != null
						&& !hero.quickslot.isNonePlaceholder(slot)
						&& hero.belongings.contains(result)){
					hero.quickslot.setSlot(slot, result);
				}
			}
			if (result.isIdentified()){
				if (hero == Multiplayer.localHero()) {
					Catalog.setSeen(result.getClass());
					Statistics.itemTypesDiscovered.add(result.getClass());
				}
			}
			Transmuting.show(hero, item, result);
			hero.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10);
			GLog.p( Messages.get(this, "morph") );
		}
	}

	public static Item changeItem( Item item, Hero hero ){
		if (item instanceof MagesStaff) {
			return changeStaff((MagesStaff) item, hero);
		}else if (item instanceof TippedDart){
			return changeTippedDart( (TippedDart)item, hero );
		} else if (item instanceof MeleeWeapon || item instanceof MissileWeapon) {
			return changeWeapon( (Weapon)item, hero );
		} else if (item instanceof Scroll) {
			return changeScroll( (Scroll)item, hero );
		} else if (item instanceof Potion) {
			return changePotion( (Potion)item, hero );
		} else if (item instanceof Ring) {
			return changeRing( (Ring)item, hero );
		} else if (item instanceof Wand) {
			return changeWand( (Wand)item, hero );
		} else if (item instanceof Plant.Seed) {
			return changeSeed((Plant.Seed) item, hero);
		} else if (item instanceof Runestone) {
			return changeStone((Runestone) item, hero);
		} else if (item instanceof Artifact) {
			Artifact a = changeArtifact( (Artifact)item, hero );
			if (a == null){
				// if no artifacts are left, generate a random ring with shared ID/curse state
				Item result = Generator.randomUsingDefaults(Generator.Category.RING);
				result.levelKnown = item.levelKnown;
				result.cursed = item.cursed;
				result.cursedKnown = item.cursedKnown;
				if (item.visiblyUpgraded() == 10){
					result.level(2);
				} else if (item.visiblyUpgraded() >= 5){
					result.level(1);
				} else {
					result.level(0);
				}
				return result;
			} else {
				return a;
			}
		} else if (item instanceof Trinket) {
			return changeTrinket( (Trinket)item, hero );
		} else {
			return null;
		}
	}

	private static MagesStaff changeStaff( MagesStaff staff, Hero hero ){
		Class<?extends Wand> wandClass = staff.wandClass();

		if (wandClass == null){
			return null;
		} else {
			Wand n;
			do {
				n = (Wand) Generator.randomUsingDefaults(Generator.Category.WAND);
			} while (Challenges.isItemBlocked(n) || n.getClass() == wandClass);
			n.cursed = false;
			n.level(0);
			n.identify();
			staff.imbueWand(n, null);
		}

		return staff;
	}

	private static TippedDart changeTippedDart( TippedDart dart, Hero hero ){
		TippedDart n;
		do {
			n = TippedDart.randomTipped(1);
		} while (n.getClass() == dart.getClass());

		return n;
	}

	private static Weapon changeWeapon( Weapon w, Hero hero ) {
		Weapon n;
		Generator.Category c;
		if (w instanceof MeleeWeapon) {
			c = Generator.wepTiers[((MeleeWeapon)w).tier - 1];
		} else {
			c = Generator.misTiers[((MissileWeapon)w).tier - 1];
		}

		do {
			n = (Weapon)Generator.randomUsingDefaults(c);
		} while (Challenges.isItemBlocked(n) || n.getClass() == w.getClass());

		n.level(0);
		n.quantity(w.quantity());
		int level = w.trueLevel();
		if (level > 0) {
			n.upgrade( level );
		} else if (level < 0) {
			n.degrade( -level );
		}

		n.enchantment = w.enchantment;
		n.curseInfusionBonus = w.curseInfusionBonus;
		n.masteryPotionBonus = w.masteryPotionBonus;
		n.levelKnown = w.levelKnown;
		n.cursedKnown = w.cursedKnown;
		n.cursed = w.cursed;
		n.augment = w.augment;
		n.enchantHardened = w.enchantHardened;

		// technically a new set, ensure old one is destroyed (except for darts)
		if (w instanceof MissileWeapon && w.isUpgradable()){
			Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class, hero).levelThresholds.put(((MissileWeapon) w).setID, Integer.MAX_VALUE);
			// also extra missile weapon properties
			((MissileWeapon) n).damage(100 - ((MissileWeapon)w).durabilityLeft());
		}

		return n;

	}

	private static Ring changeRing( Ring r, Hero hero ) {
		Ring n;
		do {
			n = (Ring)Generator.randomUsingDefaults( Generator.Category.RING );
		} while (Challenges.isItemBlocked(n) || n.getClass() == r.getClass());

		n.level(0);

		int level = r.level();
		if (level > 0) {
			n.upgrade( level );
		} else if (level < 0) {
			n.degrade( -level );
		}

		n.levelKnown = r.levelKnown;
		n.cursedKnown = r.cursedKnown;
		n.cursed = r.cursed;

		return n;
	}

	private static Artifact changeArtifact( Artifact a, Hero hero ) {
		Artifact n;
		do {
			n = Generator.randomArtifact();
		} while ( n != null && (Challenges.isItemBlocked(n) || n.getClass() == a.getClass()));

		if (n != null){

			if (a instanceof DriedRose){
				if (((DriedRose) a).ghostWeapon() != null){
					Dungeon.level.drop(((DriedRose) a).ghostWeapon(), hero.pos);
				}
				if (((DriedRose) a).ghostArmor() != null){
					Dungeon.level.drop(((DriedRose) a).ghostArmor(), hero.pos);
				}
			}

			n.cursedKnown = a.cursedKnown;
			n.cursed = a.cursed;
			n.levelKnown = a.levelKnown;
			n.transferUpgrade(a.visiblyUpgraded());
			return n;
		}

		return null;
	}

	private static Trinket changeTrinket( Trinket t, Hero hero ){
		Trinket n;
		do {
			n = (Trinket)Generator.random(Generator.Category.TRINKET);
		} while ( Challenges.isItemBlocked(n) || n.getClass() == t.getClass());

		n.level(t.trueLevel());
		n.levelKnown = t.levelKnown;
		n.cursedKnown = t.cursedKnown;
		n.cursed = t.cursed;

		return n;
	}

	private static Wand changeWand( Wand w, Hero hero ) {
		Wand n;
		do {
			n = (Wand)Generator.randomUsingDefaults( Generator.Category.WAND );
		} while ( Challenges.isItemBlocked(n) || n.getClass() == w.getClass());

		n.level( 0 );
		int level = w.trueLevel();
		n.upgrade( level );

		n.levelKnown = w.levelKnown;
		n.curChargeKnown = w.curChargeKnown;
		n.cursedKnown = w.cursedKnown;
		n.cursed = w.cursed;
		n.curseInfusionBonus = w.curseInfusionBonus;
		n.resinBonus = w.resinBonus;

		n.curCharges =  w.curCharges;
		n.updateLevel();

		return n;
	}

	private static Plant.Seed changeSeed( Plant.Seed s, Hero hero ) {
		Plant.Seed n;

		do {
			n = (Plant.Seed)Generator.randomUsingDefaults( Generator.Category.SEED );
		} while (n.getClass() == s.getClass());

		return n;
	}

	private static Runestone changeStone( Runestone r, Hero hero ) {
		Runestone n;

		do {
			n = (Runestone) Generator.randomUsingDefaults( Generator.Category.STONE );
		} while (n.getClass() == r.getClass());

		return n;
	}

	private static Scroll changeScroll( Scroll s, Hero hero ) {
		if (s instanceof ExoticScroll) {
			return Reflection.newInstance(ExoticScroll.exoToReg.get(s.getClass()));
		} else {
			return Reflection.newInstance(ExoticScroll.regToExo.get(s.getClass()));
		}
	}

	private static Potion changePotion( Potion p, Hero hero ) {
		if	(p instanceof ExoticPotion) {
			return Reflection.newInstance(ExoticPotion.exoToReg.get(p.getClass()));
		} else {
			return Reflection.newInstance(ExoticPotion.regToExo.get(p.getClass()));
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