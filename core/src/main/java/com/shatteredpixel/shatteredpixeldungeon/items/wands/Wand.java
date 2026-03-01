package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

import network.Multiplayer;

public abstract class Wand extends Item {

	public static final String AC_ZAP	= "ZAP";

	private static final float TIME_TO_ZAP	= 1f;
	
	public int maxCharges = initialCharges();
	public int curCharges = maxCharges;
	public float partialCharge = 0f;
	
	protected Charger charger;
	
	public boolean curChargeKnown = false;
	
	public boolean curseInfusionBonus = false;
	public int resinBonus = 0;

	private static final int USES_TO_ID = 10;
	private float usesLeftToID = USES_TO_ID;
	private float availableUsesToID = USES_TO_ID/2f;

	protected int collisionProperties = Ballistica.MAGIC_BOLT;
	
	{
		defaultAction = AC_ZAP;
		usesTargeting = true;
		bones = true;
	}

	public void staffFx(MagesStaff.StaffParticle particle) {
			particle.color(0xFFFFFF);
			particle.am = 0.3f;
			particle.setLifespan(1f);
			particle.speed.polar(Random.Float(PointF.PI2), 2f);
			particle.setSize(1f, 2f);
			particle.radiateXY(0.5f);
	}


	public int initialCharges() {
        return 2; // значение по умолчанию
    }

    protected int chargesPerCast() {
        return 1; // значение по умолчанию
    }

    public void updateLevel() {
        maxCharges = Math.min(initialCharges() + level(), 10);
        curCharges = Math.min(curCharges, maxCharges);
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (curCharges > 0 || !curChargeKnown) {
            actions.add(AC_ZAP);
        }
        return actions;
    }
	
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_ZAP)) {
            curUser = hero;          // устанавливаем текущего пользователя
            curItem = this;
            // создаём экземплярный слушатель, захватывающий this
            GameScene.selectCell(new WandZapListener(this, hero));
        }
    }

	@Override
	public int targetingPos(Hero user, int dst) {
		if (cursed && cursedKnown){
			return new Ballistica(user.pos, dst, Ballistica.MAGIC_BOLT).collisionPos;
		} else {
			return new Ballistica(user.pos, dst, collisionProperties).collisionPos;
		}
	}

	public abstract void onZap(Ballistica attack);

	public abstract void onHit( MagesStaff staff, Char attacker, Char defender, int damage);

	//not affected by enchantment proc chance changers
	public static float procChanceMultiplier( Char attacker ){
		if (attacker.buff(Talent.EmpoweredStrikeTracker.class) != null){
			return 1f + ((Hero)attacker).pointsInTalent(Talent.EMPOWERED_STRIKE)/2f;
		}
		return 1f;
	}

	public boolean tryToZap( Hero owner, int target ){

		if (owner.buff(WildMagic.WildMagicTracker.class) == null && owner.buff(MagicImmune.class) != null){
			GLog.w( Messages.get(this, "no_magic") );
			return false;
		}

		//if we're using wild magic, then assume we have charges
		if ( owner.buff(WildMagic.WildMagicTracker.class) != null || curCharges >= chargesPerCast()){
			return true;
		} else {
			GLog.w(Messages.get(this, "fizzles"));
			return false;
		}
	}

	@Override
	public boolean collect( Bag container ) {
		if (super.collect( container )) {
			if (container.owner != null) {
				if (container instanceof MagicalHolster)
					charge( container.owner, MagicalHolster.HOLSTER_SCALE_FACTOR);
				else
					charge( container.owner );
			}
			return true;
		} else {
			return false;
		}
	}

	public void gainCharge( float amt ){
		gainCharge( amt, false );
	}

	public void gainCharge( float amt, boolean overcharge ){
		partialCharge += amt;
		while (partialCharge >= 1) {
			if (overcharge) curCharges = Math.min(maxCharges+(int)amt, curCharges+1);
			else curCharges = Math.min(maxCharges, curCharges+1);
			partialCharge--;
			updateQuickslot();
		}
	}
	
	public void charge( Char owner ) {
		if (charger == null) charger = new Charger();
		charger.attachTo( owner );
	}

	public void charge( Char owner, float chargeScaleFactor ){
		charge( owner );
		charger.setScaleFactor( chargeScaleFactor );
	}

	protected void wandProc(Char target, int chargesUsed) {
        wandProc(target, buffedLvl(), chargesUsed);
    }

    protected void wandProc(Char target, int wandLevel, int chargesUsed) {
        if (curUser.hasTalent(Talent.ARCANE_VISION)) {
            int dur = 5 + 5 * curUser.pointsInTalent(Talent.ARCANE_VISION);
            Buff.append(curUser, TalismanOfForesight.CharAwareness.class, dur).charID = target.id();
    	}

        if (target != curUser &&
                curUser.subClass == HeroSubClass.WARLOCK &&
                Random.Float() > (Math.pow(0.92f, (wandLevel * chargesUsed) + 1) - 0.07f)) {
            SoulMark.prolong(target, SoulMark.class, SoulMark.DURATION + wandLevel, curUser);
        }

        if (curUser.subClass == HeroSubClass.PRIEST && target.buff(GuidingLight.Illuminated.class) != null) {
            target.buff(GuidingLight.Illuminated.class).detach();
            target.damage(curUser.lvl + 5, GuidingLight.INSTANCE);
        }

        if (target.alignment != Char.Alignment.ALLY
                && curUser.heroClass != HeroClass.CLERIC
                && curUser.hasTalent(Talent.SEARING_LIGHT)
                && curUser.buff(Talent.SearingLightCooldown.class) == null) {
            Buff.affect(target, GuidingLight.Illuminated.class, this);
            Buff.affect(curUser, Talent.SearingLightCooldown.class, 20f, this);
        }

        if (target.alignment != Char.Alignment.ALLY
                && curUser.heroClass != HeroClass.CLERIC
                && curUser.hasTalent(Talent.SUNRAY)) {
            if (Random.Int(20) < 1 + 2 * curUser.pointsInTalent(Talent.SUNRAY)) {
                Buff.prolong(target, Blindness.class, 4f, this);
            }
        }
    }

	public static void wandProc(Hero owner, Char target, int wandLevel, int chargesUsed) {
		if (owner == null) return;

		if (owner.hasTalent(Talent.ARCANE_VISION)) {
			int dur = 5 + 5 * owner.pointsInTalent(Talent.ARCANE_VISION);
			Buff.append(owner, TalismanOfForesight.CharAwareness.class, dur).charID = target.id();
		}

		if (target != owner &&
				owner.subClass == HeroSubClass.WARLOCK &&
				Random.Float() > (Math.pow(0.92f, (wandLevel * chargesUsed) + 1) - 0.07f)) {
			SoulMark.prolong(target, SoulMark.class, SoulMark.DURATION + wandLevel, owner);
		}

		if (owner.subClass == HeroSubClass.PRIEST && target.buff(GuidingLight.Illuminated.class) != null) {
			target.buff(GuidingLight.Illuminated.class).detach();
			target.damage(owner.lvl + 5, GuidingLight.INSTANCE);
		}

		if (target.alignment != Char.Alignment.ALLY
				&& owner.heroClass != HeroClass.CLERIC
				&& owner.hasTalent(Talent.SEARING_LIGHT)
				&& owner.buff(Talent.SearingLightCooldown.class) == null) {
			Buff.affect(target, GuidingLight.Illuminated.class, owner);
			Buff.affect(owner, Talent.SearingLightCooldown.class, 20f, owner);
		}

		if (target.alignment != Char.Alignment.ALLY
				&& owner.heroClass != HeroClass.CLERIC
				&& owner.hasTalent(Talent.SUNRAY)) {
			if (Random.Int(20) < 1 + 2 * owner.pointsInTalent(Talent.SUNRAY)) {
				Buff.prolong(target, Blindness.class, 4f, owner);
			}
		}
	}

	@Override
	public void onDetach( ) {
		stopCharging();
	}

	public void stopCharging() {
		if (charger != null) {
			charger.detach();
			charger = null;
		}
	}
	
	public void level( int value) {
		super.level( value );
		updateLevel();
	}
	
	@Override
	public Item identify( boolean byHero ) {
		
		curChargeKnown = true;
		super.identify(byHero);
		
		updateQuickslot();
		
		return this;
	}

	public void setIDReady(){
		usesLeftToID = -1;
	}

	public boolean readyToIdentify(){
		return !isIdentified() && usesLeftToID <= 0;
	}
	
	public void onHeroGainExp( float levelPercent, Hero hero ){
		levelPercent *= Talent.itemIDSpeedFactor(hero, this);
		if (!isIdentified() && availableUsesToID <= USES_TO_ID/2f) {
			//gains enough uses to ID over 1 level
			availableUsesToID = Math.min(USES_TO_ID/2f, availableUsesToID + levelPercent * USES_TO_ID/2f);
		}
	}

    @Override
    public String info() {
        // Используем локального героя для отображения информации
        Hero viewer = Multiplayer.localHero();
        String desc = super.info();

        desc += "\n\n" + statsDesc();

        if (resinBonus == 1) {
            desc += "\n\n" + Messages.get(Wand.class, "resin_one");
        } else if (resinBonus > 1) {
            desc += "\n\n" + Messages.get(Wand.class, "resin_many", resinBonus);
        }

        if (cursed && cursedKnown) {
            desc += "\n\n" + Messages.get(Wand.class, "cursed");
        } else if (!isIdentified() && cursedKnown) {
            desc += "\n\n" + Messages.get(Wand.class, "not_cursed");
        }

        if (viewer != null && viewer.subClass == HeroSubClass.BATTLEMAGE) {
            desc += "\n\n" + Messages.get(this, "bmage_desc");
        }

        return desc;
    }

	public String statsDesc(){
		return Messages.get(this, "stats_desc");
	}

	public String upgradeStat1(int level){
		return null;
	}

	public String upgradeStat2(int level){
		return null;
	}

	public String upgradeStat3(int level){
		return null;
	}
	
	@Override
	public boolean isIdentified() {
		return super.isIdentified() && curChargeKnown;
	}
	
    @Override
    public String status() {
        if (levelKnown) {
            return (curChargeKnown ? curCharges : "?") + "/" + maxCharges;
        } else {
            return null;
        }
    }
	
	@Override
	public int level() {
		if (!cursed && curseInfusionBonus){
			curseInfusionBonus = false;
			updateLevel();
		}
		int level = super.level();
		if (curseInfusionBonus) level += 1 + level/6;
		level += resinBonus;
		return level;
	}
	
	@Override
	public Item upgrade() {

		super.upgrade();

		if (Random.Int(3) == 0) {
			cursed = false;
		}

		if (resinBonus > 0){
			resinBonus--;
		}

		updateLevel();
		curCharges = Math.min( curCharges + 1, maxCharges );
		updateQuickslot();
		
		return this;
	}
	
	@Override
	public Item degrade() {
		super.degrade();
		
		updateLevel();
		updateQuickslot();
		
		return this;
	}

    @Override
    public int buffedLvl() {
        int lvl = super.buffedLvl();

        if (charger != null && charger.target != null) {
            // Если жезл в руках героя, charger.target — этот герой
            Char owner = charger.target;

            // inside staff, apply degradation
            if (owner == curUser && !curUser.belongings.contains(this) && owner.buff(Degrade.class) != null) {
                lvl = Degrade.reduceLevel(lvl);
            }

            if (owner.buff(ScrollEmpower.class) != null) {
                lvl += 2;
            }

            if (curCharges == 1 && owner instanceof Hero && ((Hero) owner).hasTalent(Talent.DESPERATE_POWER)) {
                lvl += ((Hero) owner).pointsInTalent(Talent.DESPERATE_POWER);
            }

            if (owner.buff(WildMagic.WildMagicTracker.class) != null) {
                int bonus = 4 + ((Hero) owner).pointsInTalent(Talent.WILD_POWER);
                if (Random.Int(2) == 0) bonus++;
                bonus /= 2;

                int maxBonusLevel = 3 + ((Hero) owner).pointsInTalent(Talent.WILD_POWER);
                if (lvl < maxBonusLevel) {
                    lvl = Math.min(lvl + bonus, maxBonusLevel);
                }
            }

            WandOfMagicMissile.MagicCharge buff = owner.buff(WandOfMagicMissile.MagicCharge.class);
            if (buff != null && buff.level() > lvl) {
                return buff.level();
            }
        }
        return lvl;
    }

    public void fx(Ballistica bolt, Callback callback) {
        if (curUser != null) { // проверка на всякий случай
            MagicMissile.boltFromChar(curUser.sprite.parent,
                    MagicMissile.MAGIC_MISSILE,
                    curUser.sprite,
                    bolt.collisionPos,
                    callback);
            Sample.INSTANCE.play(Assets.Sounds.ZAP);
        }
    }

    public void wandUsed() {
        // вызывается после zap, curUser должен быть установлен
        if (!isIdentified()) {
            float uses = Math.min(availableUsesToID, Talent.itemIDSpeedFactor(curUser, this));
            availableUsesToID -= uses;
            usesLeftToID -= uses;
            if (usesLeftToID <= 0 || curUser.pointsInTalent(Talent.SCHOLARS_INTUITION) == 2) {
                if (ShardOfOblivion.passiveIDDisabled(curUser)) {
                    if (usesLeftToID > -1) {
                        GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), name());
                    }
                    setIDReady();
                } else {
                    identify();
                    GLog.p(Messages.get(Wand.class, "identify"));
                    Badges.validateItemLevelAquired(this);
                }
            }
            if (ShardOfOblivion.passiveIDDisabled(curUser)) {
                Buff.prolong(curUser, ShardOfOblivion.WandUseTracker.class, 50f, this);
            }
        }

        // inside staff
        if (charger != null && charger.target == curUser && !curUser.belongings.contains(this)) {
            if (curUser.hasTalent(Talent.EXCESS_CHARGE) && curCharges >= maxCharges) {
                int shieldToGive = Math.round(buffedLvl() * 0.67f * curUser.pointsInTalent(Talent.EXCESS_CHARGE));
                Buff.affect(curUser, Barrier.class, this).setShield(shieldToGive);
                curUser.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
            }
        }

        curCharges -= cursed ? 1 : chargesPerCast();

        // remove magic charge
        WandOfMagicMissile.MagicCharge buff = curUser.buff(WandOfMagicMissile.MagicCharge.class);
        if (buff != null
                && buff.wandJustApplied() != this
                && buff.level() == buffedLvl()
                && buffedLvl() > super.buffedLvl()) {
            buff.detach();
        } else {
            ScrollEmpower empower = curUser.buff(ScrollEmpower.class);
            if (empower != null) {
                empower.use();
            }
        }

        // ... остальная логика с талантами (использует curUser) без изменений

        Invisibility.dispel(curUser);
        updateQuickslot();

        curUser.spendAndNext(TIME_TO_ZAP);
    }

    // random, glowing, value, store/restore без изменений

    public int collisionProperties(int target) {
        if (cursed) return Ballistica.MAGIC_BOLT;
        else return collisionProperties;
    }

    public static class PlaceHolder extends Wand {
		{
			setImage(ItemSpriteSheet.WAND_HOLDER);
		}

		@Override
		public boolean isSimilar(Item item) {
			return item instanceof Wand;
		}

		@Override
		public void onZap(Ballistica attack) {
			// ничего не делает
		}

		@Override
		public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
			// ничего не делает
		}

		@Override
		public String info() {
			return "";
		}
    }

    // Внутренний класс-слушатель для выбора цели
    private static class WandZapListener extends CellSelector.Listener {
        private final Wand wand;
        private final Hero hero;

        WandZapListener(Wand wand, Hero hero) {
            this.wand = wand;
            this.hero = hero;
        }

        @Override
        public void onSelect(Integer target) {
            if (target == null) return;

            final Ballistica shot = new Ballistica(hero.pos, target, wand.collisionProperties(target));
            int cell = shot.collisionPos;

            if (target == hero.pos || cell == hero.pos) {
                if (target == hero.pos && hero.hasTalent(Talent.SHIELD_BATTERY)) {
                    if (hero.buff(MagicImmune.class) != null) {
                        GLog.w(Messages.get(Wand.class, "no_magic"));
                        return;
                    }
                    if (wand.curCharges == 0) {
                        GLog.w(Messages.get(Wand.class, "fizzles"));
                        return;
                    }
                    float shield = hero.HT * (0.04f * wand.curCharges);
                    if (hero.pointsInTalent(Talent.SHIELD_BATTERY) == 2) shield *= 1.5f;
                    Buff.affect(hero, Barrier.class, this).setShield(Math.round(shield));
                    hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(Math.round(shield)), FloatingText.SHIELDING);
                    wand.curCharges = 0;
                    hero.sprite.operate(hero.pos);
                    Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
                    ScrollOfRecharging.charge(hero);
                    updateQuickslot();
                    hero.spendAndNext(Actor.TICK);
                    return;
                }
                GLog.i(Messages.get(Wand.class, "self_target"));
                return;
            }

            hero.sprite.zap(cell);

            if (Actor.findChar(target) != null)
                QuickSlotButton.target(Actor.findChar(target));
            else
                QuickSlotButton.target(Actor.findChar(cell));

            if (wand.tryToZap(hero, target)) {
                hero.busy();

                // backup barrier logic
                if (hero.hasTalent(Talent.BACKUP_BARRIER)
                        && wand.curCharges == wand.chargesPerCast()
                        && wand.charger != null && wand.charger.target == hero) {
                    if (hero.heroClass == HeroClass.MAGE && !hero.belongings.contains(wand)) {
                        int shieldToGive = 1 + 2 * hero.pointsInTalent(Talent.BACKUP_BARRIER);
                        Buff.affect(hero, Barrier.class, this).setShield(shieldToGive);
                        hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
                    } else if (hero.heroClass != HeroClass.MAGE) {
                        boolean highest = true;
                        for (Item i : hero.belongings.getAllItems(Wand.class)) {
                            if (i.level() > wand.level()) {
                                highest = false;
                                break;
                            }
                        }
                        if (highest) {
                            int shieldToGive = 1 + 2 * hero.pointsInTalent(Talent.BACKUP_BARRIER);
                            Buff.affect(hero, Barrier.class, this).setShield(shieldToGive);
                            hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
                        }
                    }
                }

                if (wand.cursed) {
                    if (!wand.cursedKnown) {
                        GLog.n(Messages.get(Wand.class, "curse_discover", wand.name()));
                    }
                    CursedWand.cursedZap(wand,
                            hero,
                            new Ballistica(hero.pos, target, Ballistica.MAGIC_BOLT),
                            new Callback() {
                                @Override
                                public void call() {
                                    wand.wandUsed();
                                }
                            });
                } else {
                    wand.fx(shot, new Callback() {
                        public void call() {
                            wand.onZap(shot);
                            if (Random.Float() < WondrousResin.extraCurseEffectChance(hero)) {
                                WondrousResin.forcePositive = true;
                                CursedWand.cursedZap(wand,
                                        hero,
                                        new Ballistica(hero.pos, target, Ballistica.MAGIC_BOLT),
                                        new Callback() {
                                            @Override
                                            public void call() {
                                                WondrousResin.forcePositive = false;
                                                wand.wandUsed();
                                            }
                                        });
                            } else {
                                wand.wandUsed();
                            }
                        }
                    });
                }
                wand.cursedKnown = true;
            }
        }

        @Override
        public String prompt() {
            return Messages.get(Wand.class, "prompt");
        }

		
    }
	
	public class Charger extends Buff {

        private static final float BASE_CHARGE_DELAY = 10f;
        private static final float SCALING_CHARGE_ADDITION = 40f;
        private static final float NORMAL_SCALE_FACTOR = 0.875f;
        private static final float CHARGE_BUFF_BONUS = 0.25f;

        float scalingFactor = NORMAL_SCALE_FACTOR;

        @Override
        public boolean attachTo(Char target) {
            if (super.attachTo(target)) {
                if (target instanceof Hero && curUser == null && cooldown() == 0 && target.cooldown() > 0) {
                    spend(TICK);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean act() {
            if (curCharges < maxCharges && target.buff(MagicImmune.class) == null)
                recharge();

            while (partialCharge >= 1 && curCharges < maxCharges) {
                partialCharge--;
                curCharges++;
                updateQuickslot();
            }

            if (curCharges == maxCharges) {
                partialCharge = 0;
            }

            spend(TICK);
            return true;
        }
		
		private void recharge() {
			int missingCharges = maxCharges - curCharges;
			missingCharges = Math.max(0, missingCharges);

			float turnsToCharge = (float) (BASE_CHARGE_DELAY
					+ (SCALING_CHARGE_ADDITION * Math.pow(scalingFactor, missingCharges)));

			if (target instanceof Hero && Regeneration.regenOn((Hero) target)) {
				partialCharge += (1f / turnsToCharge) * RingOfEnergy.wandChargeMultiplier(target);
			}

			for (Recharging bonus : target.buffs(Recharging.class)) {
				if (bonus != null && bonus.remainder() > 0f) {
					partialCharge += CHARGE_BUFF_BONUS * bonus.remainder();
				}
			}
		}

        public Wand wand() {
            return Wand.this;
        }

        public void gainCharge(float charge) {
            if (curCharges < maxCharges) {
                partialCharge += charge;
                while (partialCharge >= 1f) {
                    curCharges++;
                    partialCharge--;
                }
                if (curCharges >= maxCharges) {
                    partialCharge = 0;
                    curCharges = maxCharges;
                }
                updateQuickslot();
            }
        }

        private void setScaleFactor(float value) {
            this.scalingFactor = value;
        }
    }
}
