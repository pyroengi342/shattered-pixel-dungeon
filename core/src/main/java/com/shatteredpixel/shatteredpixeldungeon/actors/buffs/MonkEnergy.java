/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMonkAbilities;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

public class MonkEnergy extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public float energy = 0;
	public int cooldown; //currently unused, abilities had cooldowns prior to v2.5

	private static final float MAX_COOLDOWN = 5;

	@Override
	public int icon() {
		return BuffIndicator.MONK_ENERGY;
	}

	@Override
	public void tintIcon(Image icon) {
		if (cooldown > 0){
			icon.hardlight(0.33f, 0.33f, 1f);
		} else {
			icon.resetColor();
		}
	}

	@Override
	public float iconFadePercent() {
		return GameMath.gate(0, cooldown/MAX_COOLDOWN, 1);
	}

	@Override
	public String iconTextDisplay() {
		if (cooldown > 0){
			return Integer.toString(cooldown);
		} else {
			return "";
		}
	}

	@Override
	public boolean act() {
		if (cooldown > 0){
			cooldown--;
			if (cooldown == 0 && energy >= 1){
				ActionIndicator.setAction(this);
			}
			BuffIndicator.refreshHero();
		}

		spend(TICK);
		return true;
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc", (int)energy, energyCap());
		if (cooldown > 0){
			desc += "\n\n" + Messages.get(this, "desc_cooldown", cooldown);
		}
		return desc;
	}

	public static String ENERGY = "energy";
	public static String COOLDOWN = "cooldown";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ENERGY, energy);
		bundle.put(COOLDOWN, cooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		energy = bundle.getFloat(ENERGY);
		cooldown = bundle.getInt(COOLDOWN);

		if (energy >= 1 && cooldown == 0){
			ActionIndicator.setAction(this);
		}
	}

	public void gainEnergy(Mob enemy ){
		if (target == null) return;

		if (!Regeneration.regenOn(((Hero) target))){
			return; //to prevent farming boss minions
		}

		float energyGain;

		//bosses and minibosses give extra energy, certain enemies give half, otherwise give 1
		if (Char.hasProp(enemy, Char.Property.BOSS))            energyGain = 5;
		else if (Char.hasProp(enemy, Char.Property.MINIBOSS))   energyGain = 3;
		else if (enemy instanceof Ghoul)                        energyGain = 0.5f;
		else if (enemy instanceof RipperDemon)                  energyGain = 0.5f;
		else if (enemy instanceof YogDzewa.Larva)               energyGain = 0.5f;
		else if (enemy instanceof Wraith)                       energyGain = 0.5f;
		else                                                    energyGain = 1;

		float enGainMulti = 1f;
		if (target instanceof Hero) {
			Hero hero = (Hero) target;
			if (hero.hasTalent(Talent.UNENCUMBERED_SPIRIT)) {
				int points = hero.pointsInTalent(Talent.UNENCUMBERED_SPIRIT);

				if (hero.belongings.armor() != null){
					if (hero.belongings.armor().tier <= 1 && points >= 3){
						enGainMulti += 1.00f;
					} else if (hero.belongings.armor().tier <= 2 && points >= 2){
						enGainMulti += 0.75f;
					} else if (hero.belongings.armor().tier <= 3 && points >= 1){
						enGainMulti += 0.50f;
					}
				}

				if (hero.belongings.weapon() instanceof MeleeWeapon
						&& (hero.buff(RingOfForce.BrawlersStance.class) == null
						|| !hero.buff(RingOfForce.BrawlersStance.class).active)){
					if (((MeleeWeapon) hero.belongings.weapon()).tier <= 1 && points >= 3){
						enGainMulti += 1.00f;
					} else if (((MeleeWeapon) hero.belongings.weapon()).tier <= 2 && points >= 2){
						enGainMulti += 0.75f;
					} else if (((MeleeWeapon) hero.belongings.weapon()).tier <= 3 && points >= 1){
						enGainMulti += 0.50f;
					}
				}

			}
		}
		energyGain *= enGainMulti;

		energy += energyGain;
		//if we kill while using an ability, don't apply the cap yet, will be enforced after spending
		if (target.buff(MonkAbility.UnarmedAbilityTracker.class) == null){
			energy = Math.min(energy, energyCap());
		}

		if (energy >= 1 && cooldown == 0){
			ActionIndicator.setAction(this);
		}
		BuffIndicator.refreshHero();
	}

	//10 at base, 20 at level 30
	public int energyCap(){
		return Math.max(10, 5 + ((Hero) target).lvl/2);
	}

	public void abilityUsed( MonkAbility abil ){
		energy -= abil.energyCost();
		energy = Math.min(energy, energyCap());

		if (target instanceof Hero && ((Hero) target).hasTalent(Talent.COMBINED_ENERGY)
				&& abil.energyCost() >= 5-((Hero) target).pointsInTalent(Talent.COMBINED_ENERGY)) {
			Talent.CombinedEnergyAbilityTracker tracker = target.buff(Talent.CombinedEnergyAbilityTracker.class);
			if (tracker == null || !tracker.wepAbilUsed){
				Buff.prolong(target, Talent.CombinedEnergyAbilityTracker.class, 5f, this).monkAbilused = true;
			} else {
				tracker.monkAbilused = true;
				processCombinedEnergy(tracker);
			}
		}

		if (cooldown > 0 || energy < 1){
			ActionIndicator.clearAction(this);
		} else {
			ActionIndicator.refresh();
		}
		BuffIndicator.refreshHero();
	}

	public boolean abilitiesEmpowered( Hero hero ){
		//100%/80%/60% energy at +1/+2/+3
		return energy/energyCap() >= 1.2f - 0.2f*hero.pointsInTalent(Talent.MONASTIC_VIGOR);
	}

	public void processCombinedEnergy(Talent.CombinedEnergyAbilityTracker tracker){
		energy = Math.min(energy+1, energyCap());
		tracker.detach();
		if (energy >= 1){
			ActionIndicator.setAction(this);
		}
		BuffIndicator.refreshHero();
	}

	@Override
	public String actionName() {
		return Messages.get(this, "action");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.MONK_ABILITIES;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text( Integer.toString((int)energy) );
		txt.hardlight(CharSprite.POSITIVE);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		if (abilitiesEmpowered((Hero) target)){
			return 0xAAEE22;
		} else {
			return 0xA08840;
		}
	}

	@Override
	public void doAction() {
		GameScene.show(new WndMonkAbilities(this));
	}

    public static abstract class MonkAbility {

        public static MonkAbility[] abilities = new MonkAbility[]{
                new Flurry(),
                new Focus(),
                new Dash(),
                new DragonKick(),
                new Meditate()
        };

        public String name(){
            return Messages.get(this, "name");
        }

        // Новый метод с параметром MonkEnergy
        public abstract String desc(MonkEnergy energy);

        public abstract int energyCost();

        public boolean usable(MonkEnergy buff){
            return buff.energy >= energyCost();
        }

        public String targetingPrompt(){
            return null; //return a string if uses targeting
        }

        public abstract void doAbility(Hero hero, Integer target );

        public static class UnarmedAbilityTracker extends FlavourBuff{};

        public static class FlurryEmpowerTracker extends FlavourBuff{};

        public static class FlurryCooldownTracker extends FlavourBuff{};

        public static class Flurry extends MonkAbility {

            @Override
            public int energyCost() {
                return 1;
            }

            @Override
            public boolean usable(MonkEnergy buff) {
                return super.usable(buff) && buff.target.buff(FlurryCooldownTracker.class) == null;
            }

            @Override
            public String desc(MonkEnergy energy) {
                Hero hero = (Hero) energy.target;
                if (energy.abilitiesEmpowered(hero)){
                    //1.5x hero unarmed damage (rounds the result)
                    return Messages.get(this, "empower_desc", 2, Math.round(1.5f * (hero.STR() - 8)));
                } else {
                    return Messages.get(this, "desc", 2, Math.round(1.5f * (hero.STR() - 8)));
                }
            }

            @Override
            public String targetingPrompt() {
                return Messages.get(MeleeWeapon.class, "prompt");
            }

            @Override
            public void doAbility(Hero hero, Integer target) {
                // без изменений
            }
        }

        public static class Focus extends MonkAbility {

            @Override
            public int energyCost() {
                return 2;
            }

            @Override
            public boolean usable(MonkEnergy buff) {
                return super.usable(buff) && buff.target.buff(FocusBuff.class) == null;
            }

            @Override
            public String desc(MonkEnergy energy) {
                // Focus не имеет цифровых параметров в описании, можно просто вернуть базовое
                // Но если хотите, можно добавить проверку на empower
                if (energy.abilitiesEmpowered((Hero) energy.target)){
                    return Messages.get(this, "empower_desc");
                } else {
                    return Messages.get(this, "desc");
                }
            }

            @Override
            public void doAbility(Hero hero, Integer target) {
                // без изменений
            }

            public static class FocusBuff extends Buff { /* ... */ }
        }

        public static class Dash extends MonkAbility {

            @Override
            public int energyCost() {
                return 3;
            }

            @Override
            public String targetingPrompt() {
                return Messages.get(this, "prompt");
            }

            @Override
            public String desc(MonkEnergy energy) {
                // Dash, вероятно, не имеет цифр в описании, но если есть – используем energy
                if (energy.abilitiesEmpowered((Hero) energy.target)){
                    return Messages.get(this, "empower_desc");
                } else {
                    return Messages.get(this, "desc");
                }
            }

            @Override
            public void doAbility(Hero hero, Integer target) {
                // без изменений
            }
        }

        public static class DragonKick extends MonkAbility {

            @Override
            public int energyCost() {
                return 4;
            }

            @Override
            public String desc(MonkEnergy energy) {
                Hero hero = (Hero) energy.target;
                if (energy.abilitiesEmpowered(hero)){
                    return Messages.get(this, "empower_desc", 9, 9 * (hero.STR() - 8));
                } else {
                    return Messages.get(this, "desc", 6, 6 * (hero.STR() - 8));
                }
            }

            @Override
            public String targetingPrompt() {
                return Messages.get(MeleeWeapon.class, "prompt");
            }

            @Override
            public void doAbility(Hero hero, Integer target) {
                // без изменений
            }
        }

        public static class Meditate extends MonkAbility {

            @Override
            public int energyCost() {
                return 5;
            }

            @Override
            public String desc(MonkEnergy energy) {
                if (energy.abilitiesEmpowered((Hero) energy.target)){
                    return Messages.get(this, "empower_desc");
                } else {
                    return Messages.get(this, "desc");
                }
            }

            @Override
            public void doAbility(Hero hero, Integer target) {
                // без изменений
            }

            public static class MeditateResistance extends FlavourBuff{ /* ... */ }
        }
    }
}
