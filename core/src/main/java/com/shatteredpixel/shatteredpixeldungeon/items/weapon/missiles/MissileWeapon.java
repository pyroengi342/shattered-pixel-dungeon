package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

import network.Multiplayer;

abstract public class MissileWeapon extends Weapon {

	{
		stackable = true;
		quantity = defaultQuantity();
		
		bones = true;

		defaultAction = AC_THROW;
		usesTargeting = true;
	}

	public long setID = new SecureRandom().nextLong();

	public boolean spawnedForEffect = false;
	
	protected boolean sticky = true;
	
	public static final float MAX_DURABILITY = 100;
	protected float durability = MAX_DURABILITY;
	protected float baseUses = 8;
	
	public boolean holster;
	
	public MissileWeapon parent;
	
	public int tier;

	protected int usesToID(){
		return 10;
	}
	
	// Новые методы с параметром владельца для расчёта урона с учётом кольца
	public int min(Char owner) {
		int lvl = buffedLvl();
		if (owner instanceof Hero) {
			return Math.max(0, min(lvl) + RingOfSharpshooting.levelDamageBonus(owner));
		} else {
			return Math.max(0, min(lvl));
		}
	}
	
	public int max(Char owner) {
		int lvl = buffedLvl();
		if (owner instanceof Hero) {
			return Math.max(0, max(lvl) + RingOfSharpshooting.levelDamageBonus(owner));
		} else {
			return Math.max(0, max(lvl));
		}
	}
	
	// Старые методы для обратной совместимости (используют curUser)
	// @Override
	// public int min() {
	// 	if (curUser != null) {
	// 		return min(curUser);
	// 	} else {
	// 		return Math.max(0, min(buffedLvl()));
	// 	}
	// }
	
	@Override
	public int min(int lvl) {
		return 2 * tier + lvl;
	}
	
	// @Override
	// public int max() {
	// 	if (curUser != null) {
	// 		return max(curUser);
	// 	} else {
	// 		return Math.max(0, max(buffedLvl()));
	// 	}
	// }
	
	@Override
	public int max(int lvl) {
		return 5 * tier + tier * lvl;
	}
	
	public int STRReq(int lvl){
		int req = STRReq(tier, lvl) - 1;
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	public int buffedLvl(){
		if (parent != null) {
			return parent.buffedLvl();
		} else {
			return super.buffedLvl();
		}
	}

	public Item upgrade( boolean enchant ) {
		if (!bundleRestoring) {
			durability = MAX_DURABILITY;
			extraThrownLeft = false;
			quantity = defaultQuantity();
			if (curUser != null) {
				Buff.affect(curUser, UpgradedSetTracker.class, this).levelThresholds.put(setID, trueLevel()+1);
			}
		}
		boolean wasCursed = cursed;
		super.upgrade( enchant );
		if (wasCursed && hasCurseEnchant()){
			cursed = wasCursed;
		}
		return this;
	}
	
	@Override
	public Item upgrade() {
		if (!bundleRestoring) {
			durability = MAX_DURABILITY;
			extraThrownLeft = false;
			quantity = defaultQuantity();
			if (curUser != null) {
				Buff.affect(curUser, UpgradedSetTracker.class, this).levelThresholds.put(setID, trueLevel()+1);
			}
		}
		return super.upgrade();
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.remove( AC_EQUIP );
		return actions;
	}
	
	@Override
	public boolean collect(Bag container) {
		if (container instanceof MagicalHolster) holster = true;
		return super.collect(container);
	}

	public boolean isSimilar( Item item ) {
		return trueLevel() == item.trueLevel() && getClass() == item.getClass() && setID == (((MissileWeapon) item).setID);
	}
	
	@Override
	public int throwPos(Hero user, int dst) {
		int projecting = 0;
		if (hasEnchant(Projecting.class, user)){
			projecting += 4;
		}
		if ((!(this instanceof SpiritBow.SpiritArrow) && Random.Int(3) < user.pointsInTalent(Talent.SHARED_ENCHANTMENT))){
			SpiritBow bow = user.belongings.getItem(SpiritBow.class); // заменили curUser на user
			if (bow != null && bow.hasEnchant(Projecting.class, user)) {
				projecting += 4;
			}
		}

		if (projecting > 0
				&& (Dungeon.level.passable[dst] || Dungeon.level.avoid[dst] || Actor.findChar(dst) != null)
				&& Dungeon.level.distance(user.pos, dst) <= Math.round(projecting * Enchantment.genericProcChanceMultiplier(user))){
			return dst;
		} else {
			return super.throwPos(user, dst);
		}
	}

	@Override
	public float accuracyFactor(Char owner, Char target) {
		float accFactor = super.accuracyFactor(owner, target);
		accFactor *= adjacentAccFactor(owner, target);
		return accFactor;
	}

	protected float adjacentAccFactor(Char owner, Char target){
		if (Dungeon.level.adjacent( owner.pos, target.pos )) {
			if (owner instanceof Hero){
				return (0.5f + 0.25f*((Hero) owner).pointsInTalent(Talent.POINT_BLANK));
			} else {
				return 0.5f;
			}
		} else {
			return 1.5f;
		}
	}

	@Override
	public void doThrow(Hero hero) {
		parent = null;
		if (((levelKnown && level() > 0) || hasGoodEnchant() || masteryPotionBonus || enchantHardened)
				&& !extraThrownLeft && quantity() == 1 && durabilityLeft() <= durabilityPerUse(level(), hero)){
			GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(title()),
					Messages.get(MissileWeapon.class, "break_upgraded_warn_desc"),
					Messages.get(MissileWeapon.class, "break_upgraded_warn_yes"),
					Messages.get(MissileWeapon.class, "break_upgraded_warn_no")){
				@Override
				protected void onSelect(int index) {
					if (index == 0){
						MissileWeapon.super.doThrow(hero);
					} else {
						QuickSlotButton.cancel();
						InventoryPane.cancelTargeting();
					}
				}
				@Override
				public void onBackPressed() {
					super.onBackPressed();
					QuickSlotButton.cancel();
					InventoryPane.cancelTargeting();
				}
			});
		} else {
			super.doThrow(hero);
		}
	}

	@Override
	protected void onThrow( int cell ) {
		Char enemy = Actor.findChar( cell );
		if (enemy == null || enemy == curUser) {
			parent = null;

			// используем curUser, который должен быть установлен перед броском
			if (curUser != null && curUser.hasTalent(Talent.SEER_SHOT)
					&& curUser.heroClass != HeroClass.HUNTRESS
					&& curUser.buff(Talent.SeerShotCooldown.class) == null){
				if (Actor.findChar(cell) == null) {
					RevealedArea a = Buff.affect(curUser, RevealedArea.class, 5 * curUser.pointsInTalent(Talent.SEER_SHOT));
					a.depth = Dungeon.depth;
					a.pos = cell;
					Buff.affect(curUser, Talent.SeerShotCooldown.class, 20f);
				}
			}

			if (!spawnedForEffect) super.onThrow( cell );
		} else {
			if (curUser != null && !curUser.shoot( enemy, this )) {
				rangedMiss( cell );
			} else {
				rangedHit( enemy, cell );
			}
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		Hero heroAttacker = (attacker instanceof Hero) ? (Hero) attacker : null;
		if (heroAttacker != null && Random.Int(3) < heroAttacker.pointsInTalent(Talent.SHARED_ENCHANTMENT)){
			SpiritBow bow = heroAttacker.belongings.getItem(SpiritBow.class);
			if (bow != null && bow.enchantment != null && heroAttacker.buff(MagicImmune.class) == null) {
				damage = bow.enchantment.proc(this, attacker, defender, damage);
			}
		}

		if ((cursed || hasCurseEnchant()) && !cursedKnown){
			if (heroAttacker == Multiplayer.localHero()) {
				GLog.n(Messages.get(this, "curse_discover"));
			}
		}
		cursedKnown = true;
		if (parent != null) parent.cursedKnown = true;

		if (heroAttacker != null && heroAttacker.pointsInTalent(Talent.SURVIVALISTS_INTUITION) == 2){
			usesLeftToID = Math.min(usesLeftToID, 0);
			availableUsesToID = Math.max(usesLeftToID, 0);
		}

		int result = super.proc(attacker, defender, damage);

		if (parent != null && parent.usesLeftToID > usesLeftToID){
			float diff = parent.usesLeftToID - usesLeftToID;
			parent.usesLeftToID -= diff;
			parent.availableUsesToID -= diff;
			if (usesLeftToID <= 0) {
				if (ShardOfOblivion.passiveIDDisabled(curUser)){
					parent.setIDReady();
				} else {
					parent.identify();
				}
			}
		}

		if (!isIdentified() && ShardOfOblivion.passiveIDDisabled(curUser) && heroAttacker != null){
			Buff.prolong(heroAttacker, ShardOfOblivion.ThrownUseTracker.class, 50f, this);
		}

		return result;
	}

	@Override
	public Item virtual() {
		Item item = super.virtual();
		((MissileWeapon)item).setID = setID;
		return item;
	}

	public int defaultQuantity(){
		return 3;
	}

	public boolean extraThrownLeft = false;

	@Override
	public Item random() {
		int n = 0;
		if (Random.Int(4) == 0) {
			n++;
			if (Random.Int(5) == 0) {
				n++;
			}
		}
		level(n);

		Random.pushGenerator(Random.Long());
			float effectRoll = Random.Float();
			if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier(null)) {
				enchant(Enchantment.randomCurse());
				cursed = true;
			} else if (effectRoll >= 1f - (0.1f * ParchmentScrap.enchantChanceMultiplier(null))){
				enchant();
			}
		Random.popGenerator();

		return this;
	}

	public String status() {
		return Integer.toString( quantity );
	}
	
	@Override
	public float castDelay(Char user, int cell) {
		if (Actor.findChar(cell) != null && Actor.findChar(cell) != user){
			return delayFactor( user );
		} else {
			return super.castDelay(user, cell);
		}
	}
	
	protected void rangedHit( Char enemy, int cell ){
		decrementDurability();
		if (durability > 0 && !spawnedForEffect){
			if (sticky && enemy != null && enemy.isActive() && enemy.alignment != Char.Alignment.ALLY){
				PinCushion p = Buff.affect(enemy, PinCushion.class, this);
				if (p.target == enemy){
					p.stick(this);
					return;
				}
			}
			Dungeon.level.drop( this, cell ).sprite.drop();
		}
	}
	
	protected void rangedMiss( int cell ) {
		parent = null;
		if (!spawnedForEffect) super.onThrow(cell);
	}

	public float durabilityLeft(){
		return durability;
	}

	public void repair( float amount ){
		durability += amount;
		durability = Math.min(durability, MAX_DURABILITY);
	}

	public void damage( float amount ){
		durability -= amount;
		durability = Math.max(durability, 1);
	}

	public final float durabilityPerUse(){
		return durabilityPerUse(level(), curUser);
	}

	public float durabilityPerUse(int level, Hero hero){
		float usages = baseUses * (float)(Math.pow(1.5f, level));

		if (hero != null && hero.hasTalent(Talent.DURABLE_PROJECTILES)){
			usages *= 1.25f + (0.25f*hero.pointsInTalent(Talent.DURABLE_PROJECTILES));
		}
		if (holster) {
			usages *= MagicalHolster.HOLSTER_DURABILITY_FACTOR;
		}

		usages /= augment.delayFactor(1f);

		if (hero != null) usages *= RingOfSharpshooting.durabilityMultiplier(hero);

		if (usages >= 100f) return 0;

		if (useRoundingInDurabilityCalc){
			usages = Math.round(usages);
			return (MAX_DURABILITY/usages) + 0.001f;
		} else {
			return MAX_DURABILITY/usages;
		}
	}

	protected boolean useRoundingInDurabilityCalc = true;

	protected void decrementDurability(){
		if (parent != null){
			if (parent.durability <= parent.durabilityPerUse()){
				durability = 0;
				parent.durability = MAX_DURABILITY;
				parent.extraThrownLeft = false;
				if (parent.durabilityPerUse() < 100f) {
					GLog.n(Messages.get(this, "has_broken"));
				}
			} else {
				parent.durability -= parent.durabilityPerUse();
				if (parent.durability > 0 && parent.durability <= parent.durabilityPerUse()){
					GLog.w(Messages.get(this, "about_to_break"));
				}
			}
			parent = null;
		} else {
			durability -= durabilityPerUse();
			if (durability > 0 && durability <= durabilityPerUse()){
				GLog.w(Messages.get(this, "about_to_break"));
			} else if (durabilityPerUse() < 100f && durability <= 0){
				GLog.n(Messages.get(this, "has_broken"));
			}
		}
	}
	
	@Override
	public int damageRoll(Char owner) {
		int damage = augment.damageFactor(super.damageRoll( owner ));
		
		if (owner instanceof Hero) {
			Hero hero = (Hero) owner;
			int exStr = hero.STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange( 0, exStr, hero);
			}
			if (owner.buff(Momentum.class) != null && owner.buff(Momentum.class).freerunning()) {
				damage = Math.round(damage * (1f + 0.15f * hero.pointsInTalent(Talent.PROJECTILE_MOMENTUM)));
			}
			// Используем новые методы min/max с owner
			int baseMin = min(owner);
			int baseMax = max(owner);
			// но damage уже посчитан через super.damageRoll, который использует min()/max() без owner.
			// Поэтому мы должны пересчитать урон, чтобы учесть кольцо. Можно сделать так:
			damage = augment.damageFactor(Random.NormalIntRange(baseMin, baseMax));
		}
		
		return damage;
	}
	
	@Override
	public void reset() {
		super.reset();
		durability = MAX_DURABILITY;
	}
	
	@Override
	public Item merge(Item other) {
		super.merge(other);
		if (isSimilar(other)) {
			extraThrownLeft = false;

			durability += ((MissileWeapon)other).durability;
			durability -= MAX_DURABILITY;
			while (durability <= 0){
				quantity -= 1;
				durability += MAX_DURABILITY;
			}

			if (quantity > defaultQuantity() && setID != 0 && setID != getClass().getSimpleName().hashCode()){
				quantity = defaultQuantity();
				durability = MAX_DURABILITY;
			}

			levelKnown = levelKnown || other.levelKnown;
			cursedKnown = cursedKnown || other.cursedKnown;
			if (((Weapon)other).readyToIdentify()){
				setIDReady();
			}

			masteryPotionBonus = masteryPotionBonus || ((MissileWeapon) other).masteryPotionBonus;
			enchantHardened = enchantHardened || ((MissileWeapon) other).enchantHardened;

			if (!curseInfusionBonus && ((MissileWeapon) other).curseInfusionBonus && ((MissileWeapon) other).hasCurseEnchant()){
				enchantment = ((MissileWeapon) other).enchantment;
				curseInfusionBonus = true;
				cursed = cursed || other.cursed;
			} else if (!curseInfusionBonus && !hasGoodEnchant() && ((MissileWeapon) other).hasGoodEnchant()){
				enchantment = ((MissileWeapon) other).enchantment;
				cursed = other.cursed;
			} else if (!curseInfusionBonus && hasCurseEnchant() && !((MissileWeapon) other).hasCurseEnchant()){
				enchantment = ((MissileWeapon) other).enchantment;
				cursed = other.cursed;
			}

			if (((MissileWeapon) other).enchantment instanceof Explosive
				&& enchantment instanceof Explosive){
				((Explosive) enchantment).merge((Explosive) ((MissileWeapon) other).enchantment);
			}
		}
		return this;
	}
	
	@Override
	public Item split(int amount) {
		bundleRestoring = true;
		Item split = super.split(amount);
		bundleRestoring = false;

		if (split != null){
			MissileWeapon m = (MissileWeapon)split;
			m.durability = MAX_DURABILITY;
			m.parent = this;
			extraThrownLeft = m.extraThrownLeft = true;

			if (m.enchantment instanceof Explosive){
				((Explosive) m.enchantment).clear();
			}
		}
		
		return split;
	}
	
	@Override
	public boolean doPickUp(Hero hero, int pos) {
		parent = null;
		if (!UpgradedSetTracker.pickupValid(hero, this)){
			Sample.INSTANCE.play( Assets.Sounds.ITEM );
			hero.spendAndNext( pickupDelay() );
			GLog.w(Messages.get(this, "dust"));
			quantity(0);
			return true;
		} else {
			extraThrownLeft = false;
			return super.doPickUp(hero, pos);
		}
	}
	
	@Override
	public boolean isIdentified() {
		return levelKnown && cursedKnown;
	}
	
	@Override
	public String info() {
		String info = super.info();

		if (levelKnown) {
			info += "\n\n" + Messages.get(MissileWeapon.class, "stats_known", tier, augment.damageFactor(min()), augment.damageFactor(max()), STRReq());
			if (curUser != null) {
				if (STRReq() > curUser.STR()) {
					info += " " + Messages.get(Weapon.class, "too_heavy");
				} else if (curUser.STR() > STRReq()) {
					info += " " + Messages.get(Weapon.class, "excess_str", curUser.STR() - STRReq());
				}
			}
		} else {
			info += "\n\n" + Messages.get(MissileWeapon.class, "stats_unknown", tier, min(0), max(0), STRReq(0));
			if (curUser != null && STRReq(0) > curUser.STR()) {
				info += " " + Messages.get(MissileWeapon.class, "probably_too_heavy");
			}
		}

		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
			if (enchantHardened) info += " " + Messages.get(Weapon.class, "enchant_hardened");
			info += " " + enchantment.desc();
		} else if (enchantHardened){
			info += "\n\n" + Messages.get(Weapon.class, "hardened_no_enchant");
		}

		if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		} else if (!isIdentified() && cursedKnown){
			info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
		}

		info += "\n\n";
		String statsInfo = statsInfo();
		if (!statsInfo.equals("")) info += statsInfo + " ";
		info += Messages.get(MissileWeapon.class, "distance");

		switch (augment) {
			case SPEED:
				info += " " + Messages.get(Weapon.class, "faster");
				break;
			case DAMAGE:
				info += " " + Messages.get(Weapon.class, "stronger");
				break;
			case NONE:
		}

		if (levelKnown) {
			float usesLeft = durability / durabilityPerUse();
			float totalUses = MAX_DURABILITY / durabilityPerUse();
			if (durabilityPerUse() > 0) {
				info += "\n\n" + Messages.get(this, "uses_left",
						(int) Math.ceil(usesLeft),
						(int) Math.ceil(totalUses));
			} else {
				info += "\n\n" + Messages.get(this, "unlimited_uses");
			}
		}  else {
			float totalUses = MAX_DURABILITY / durabilityPerUse(0, null);
			if (durabilityPerUse(0, null) > 0) {
				info += "\n\n" + Messages.get(this, "unknown_uses", (int) Math.ceil(totalUses));
			} else {
				info += "\n\n" + Messages.get(this, "unlimited_uses");
			}
		}
		
		return info;
	}

	public String statsInfo(){
		return Messages.get(this, "stats_desc");
	}
	
	@Override
	public int value() {
		int price = 5 * tier * quantity;
		if (hasGoodEnchant()) {
			price *= 1.5;
		}
		if (cursedKnown && (cursed || hasCurseEnchant())) {
			price /= 2;
		}
		if (levelKnown && level() > 0) {
			price *= (level() + 1);
		}
		if (price < 1) {
			price = 1;
		}
		return price;
	}

	private static final String SET_ID = "set_id";
	private static final String SPAWNED = "spawned";
	private static final String DURABILITY = "durability";
	private static final String EXTRA_LEFT = "extra_left";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SET_ID, setID);
		bundle.put(SPAWNED, spawnedForEffect);
		bundle.put(DURABILITY, durability);
		bundle.put(EXTRA_LEFT, extraThrownLeft);
	}
	
	private static boolean bundleRestoring = false;
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		bundleRestoring = true;
		super.restoreFromBundle(bundle);
		bundleRestoring = false;

		if (bundle.contains(SET_ID)){
			setID = bundle.getLong(SET_ID);
		} else {
			if (level() > 0){
				quantity = defaultQuantity();
			} else if (!(this instanceof Dart)){
				levelKnown = cursedKnown = true;
				setID = getClass().getSimpleName().hashCode();
			}
		}

		spawnedForEffect = bundle.getBoolean(SPAWNED);
		durability = bundle.getFloat(DURABILITY);
		extraThrownLeft = bundle.getBoolean(EXTRA_LEFT);
	}

	public static class PlaceHolder extends MissileWeapon {
		{
			setImage(ItemSpriteSheet.MISSILE_HOLDER);
		}
		@Override
		public boolean isSimilar(Item item) {
			return item instanceof MissileWeapon && !(item instanceof Dart);
		}
		@Override
		public String status() {
			return null;
		}
		@Override
		public String info() {
			return "";
		}
	}

	public static class UpgradedSetTracker extends Buff {
		{
			revivePersists = true;
		}
		public HashMap<Long, Integer> levelThresholds = new HashMap<>();

		public static boolean pickupValid(Hero h, MissileWeapon w){
			if (h.buff(UpgradedSetTracker.class) != null){
				HashMap<Long, Integer> levelThresholds = h.buff(UpgradedSetTracker.class).levelThresholds;
				if (levelThresholds.containsKey(w.setID)){
					return w.trueLevel() >= levelThresholds.get(w.setID);
				}
				return true;
			}
			return true;
		}

		public static final String SET_IDS = "set_ids";
		public static final String SET_LEVELS = "set_levels";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			long[] IDs = new long[levelThresholds.size()];
			int[] levels = new int[levelThresholds.size()];
			int i = 0;
			for (Long ID : levelThresholds.keySet()){
				IDs[i] = ID;
				levels[i] = levelThresholds.get(ID);
				i++;
			}
			bundle.put(SET_IDS, IDs);
			bundle.put(SET_LEVELS, levels);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			long[] IDs = bundle.getLongArray(SET_IDS);
			int[] levels = bundle.getIntArray(SET_LEVELS);
			levelThresholds.clear();
			for (int i = 0; i <IDs.length; i++){
				levelThresholds.put(IDs[i], levels[i]);
			}
		}
	}
}