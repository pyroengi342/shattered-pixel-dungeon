package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class MeleeWeapon extends Weapon {

    public static String AC_ABILITY = "ABILITY";

    @Override
    public void activate(Char ch) {
        super.activate(ch);
        if (ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.DUELIST) {
            Buff.affect(ch, Charger.class, ch);
        }
    }

    @Override
    public String defaultAction() {
        if (curUser != null && (curUser.heroClass == HeroClass.DUELIST
                || curUser.hasTalent(Talent.SWIFT_EQUIP))) {
            return AC_ABILITY;
        } else {
            return super.defaultAction();
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero) && hero.heroClass == HeroClass.DUELIST) {
            actions.add(AC_ABILITY);
        }
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_ABILITY)) {
            return Messages.upperCase(Messages.get(this, "ability_name"));
        } else {
            return super.actionName(action, hero);
        }
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_ABILITY)) {
            usesTargeting = false;
            if (!isEquipped(hero)) {
                if (hero.hasTalent(Talent.SWIFT_EQUIP)) {
                    if (hero.buff(Talent.SwiftEquipCooldown.class) == null
                            || hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse()) {
                        execute(hero, AC_EQUIP);
                    } else if (hero.heroClass == HeroClass.DUELIST) {
                        GLog.w(Messages.get(this, "ability_need_equip"));
                    }
                } else if (hero.heroClass == HeroClass.DUELIST) {
                    GLog.w(Messages.get(this, "ability_need_equip"));
                }
            } else if (hero.heroClass != HeroClass.DUELIST) {
                // do nothing
            } else if (STRReq() > hero.STR()) {
                GLog.w(Messages.get(this, "ability_low_str"));
            } else {
                Charger charger = Buff.affect(hero, Charger.class, hero);
                if ((charger.charges + charger.partialCharge) < abilityChargeUse(hero, null)) {
                    GLog.w(Messages.get(this, "ability_no_charge"));
                } else {
                    if (targetingPrompt() == null) {
                        duelistAbility(hero, hero.pos);
                        updateQuickslot();
                    } else {
                        usesTargeting = useTargeting();
                        GameScene.selectCell(new CellSelector.Listener() {
                            @Override
                            public void onSelect(Integer cell) {
                                if (cell != null) {
                                    duelistAbility(hero, cell);
                                    updateQuickslot();
                                }
                            }

                            @Override
                            public String prompt() {
                                return targetingPrompt();
                            }
                        });
                    }
                }
            }
        }
    }

    public String targetingPrompt() {
        return null;
    }

    public boolean useTargeting() {
        return targetingPrompt() != null;
    }

    @Override
    public int targetingPos(Hero user, int dst) {
        return dst;
    }

    protected void duelistAbility(Hero hero, Integer target) {
        // do nothing by default
    }

    protected void beforeAbilityUsed(Hero hero, Char target) {
        hero.belongings.abilityWeapon = this;
        Charger charger = Buff.affect(hero, Charger.class, hero);

        float use = abilityChargeUse(hero, target);
        charger.partialCharge -= use;
        while (charger.partialCharge < 0 && charger.charges > 0) {
            charger.charges--;
            charger.partialCharge++;
        }

        if (hero.heroClass == HeroClass.DUELIST
                && hero.hasTalent(Talent.AGGRESSIVE_BARRIER)
                && (hero.HP / (float) hero.HT) <= 0.5f) {
            int shieldAmt = 1 + 2 * hero.pointsInTalent(Talent.AGGRESSIVE_BARRIER);
            Buff.affect(hero, Barrier.class, hero).setShield(shieldAmt);
            hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldAmt), FloatingText.SHIELDING);
        }

        updateQuickslot();
    }

    protected void afterAbilityUsed(Hero hero) {
        hero.belongings.abilityWeapon = null;
        if (hero.hasTalent(Talent.PRECISE_ASSAULT)) {
            Buff.prolong(hero, Talent.PreciseAssaultTracker.class, hero.cooldown() + 4f, hero);
        }
        if (hero.hasTalent(Talent.VARIED_CHARGE)) {
            Talent.VariedChargeTracker tracker = hero.buff(Talent.VariedChargeTracker.class);
            if (tracker == null || tracker.weapon == getClass() || tracker.weapon == null) {
                Buff.affect(hero, Talent.VariedChargeTracker.class, hero).weapon = getClass();
            } else {
                tracker.detach();
                Charger charger = Buff.affect(hero, Charger.class, hero);
                charger.gainCharge(hero.pointsInTalent(Talent.VARIED_CHARGE) / 6f);
                ScrollOfRecharging.charge(hero);
            }
        }
        if (hero.hasTalent(Talent.COMBINED_LETHALITY)) {
            Talent.CombinedLethalityAbilityTracker tracker = hero.buff(Talent.CombinedLethalityAbilityTracker.class);
            if (tracker == null || tracker.weapon == this || tracker.weapon == null) {
                Buff.affect(hero, Talent.CombinedLethalityAbilityTracker.class, hero.cooldown()).weapon = this;
            } else {
                tracker.detach();
            }
        }
        if (hero.hasTalent(Talent.COMBINED_ENERGY)) {
            Talent.CombinedEnergyAbilityTracker tracker = hero.buff(Talent.CombinedEnergyAbilityTracker.class);
            if (tracker == null || !tracker.monkAbilused) {
                Buff.prolong(hero, Talent.CombinedEnergyAbilityTracker.class, 5f, hero).wepAbilUsed = true;
            } else {
                tracker.wepAbilUsed = true;
                Buff.affect(hero, MonkEnergy.class, hero).processCombinedEnergy(tracker);
            }
        }
        if (hero.buff(Talent.CounterAbilityTacker.class) != null) {
            Charger charger = Buff.affect(hero, Charger.class, hero);
            charger.gainCharge(hero.pointsInTalent(Talent.COUNTER_ABILITY) * 0.375f);
            hero.buff(Talent.CounterAbilityTacker.class).detach();
        }
    }

    public static void onAbilityKill(Hero hero, Char killed) {
        if (killed.alignment == Char.Alignment.ENEMY && hero.hasTalent(Talent.LETHAL_HASTE)) {
            Buff.affect(hero, GreaterHaste.class, hero).set(2 + 2 * hero.pointsInTalent(Talent.LETHAL_HASTE));
        }
    }

    protected int baseChargeUse(Hero hero, Char target) {
        return 1;
    }

    public final float abilityChargeUse(Hero hero, Char target) {
        return baseChargeUse(hero, target);
    }

    public int tier;

    @Override
    public int min(int lvl) {
        return tier + lvl;
    }

    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }

    public int STRReq(int lvl) {
        int req = STRReq(tier, lvl);
        if (masteryPotionBonus) {
            req -= 2;
        }
        return req;
    }

    private static boolean evaluatingTwinUpgrades = false;

    @Override
    public int buffedLvl() {
        if (!evaluatingTwinUpgrades && curUser != null && isEquipped(curUser) && curUser.hasTalent(Talent.TWIN_UPGRADES)) {
            KindOfWeapon other = null;
            if (curUser.belongings.weapon() != this) other = curUser.belongings.weapon();
            if (curUser.belongings.secondWep() != this) other = curUser.belongings.secondWep();

            if (other instanceof MeleeWeapon) {
                evaluatingTwinUpgrades = true;
                int otherLevel = other.buffedLvl();
                evaluatingTwinUpgrades = false;

                if ((tier + (3 - curUser.pointsInTalent(Talent.TWIN_UPGRADES))) <= ((MeleeWeapon) other).tier
                        && otherLevel > super.buffedLvl()) {
                    return otherLevel;
                }
            }
        }
        return super.buffedLvl();
    }

    @Override
    public int damageRoll(Char owner) {
        int damage = augment.damageFactor(super.damageRoll(owner));

        if (owner instanceof Hero) {
            Hero hero = (Hero) owner;
            int exStr = hero.STR() - STRReq();
            if (exStr > 0) {
                damage += Hero.heroDamageIntRange(0, exStr, hero);
            }
        }
        return damage;
    }

    @Override
    public String info() {
        String info = super.info();

        if (levelKnown) {
            info += "\n\n" + Messages.get(MeleeWeapon.class, "stats_known", tier, augment.damageFactor(min()), augment.damageFactor(max()), STRReq());
            if (curUser != null) {
                if (STRReq() > curUser.STR()) {
                    info += " " + Messages.get(Weapon.class, "too_heavy");
                } else if (curUser.STR() > STRReq()) {
                    info += " " + Messages.get(Weapon.class, "excess_str", curUser.STR() - STRReq());
                }
            }
        } else {
            info += "\n\n" + Messages.get(MeleeWeapon.class, "stats_unknown", tier, min(0), max(0), STRReq(0));
            if (curUser != null && STRReq(0) > curUser.STR()) {
                info += " " + Messages.get(MeleeWeapon.class, "probably_too_heavy");
            }
        }

        String statsInfo = statsInfo();
        if (!statsInfo.equals("")) info += "\n\n" + statsInfo;

        switch (augment) {
            case SPEED:
                info += " " + Messages.get(Weapon.class, "faster");
                break;
            case DAMAGE:
                info += " " + Messages.get(Weapon.class, "stronger");
                break;
            case NONE:
        }

        if (isEquipped(curUser) && !hasCurseEnchant() && curUser.buff(HolyWeapon.HolyWepBuff.class) != null
                && (curUser.subClass != HeroSubClass.PALADIN || enchantment == null)) {
            info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", Messages.get(HolyWeapon.class, "ench_name", Messages.get(Enchantment.class, "enchant"))));
            info += " " + Messages.get(HolyWeapon.class, "ench_desc");
        } else if (enchantment != null && (cursedKnown || !enchantment.curse())) {
            info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
            if (enchantHardened) info += " " + Messages.get(Weapon.class, "enchant_hardened");
            info += " " + enchantment.desc();
        } else if (enchantHardened) {
            info += "\n\n" + Messages.get(Weapon.class, "hardened_no_enchant");
        }

        if (cursed && isEquipped(curUser)) {
            info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
        } else if (cursedKnown && cursed) {
            info += "\n\n" + Messages.get(Weapon.class, "cursed");
        } else if (!isIdentified() && cursedKnown) {
            if (enchantment != null && enchantment.curse()) {
                info += "\n\n" + Messages.get(Weapon.class, "weak_cursed");
            } else {
                info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
            }
        }

        if (curUser != null && curUser.heroClass == HeroClass.DUELIST && !(this instanceof MagesStaff)) {
            info += "\n\n" + abilityInfo();
        }

        return info;
    }

    public String statsInfo() {
        return Messages.get(this, "stats_desc");
    }

    public String abilityInfo() {
        return Messages.get(this, "ability_desc");
    }

    public String upgradeAbilityStat(int level) {
        return null;
    }

    @Override
    public String status() {
        if (isEquipped(curUser) && curUser.buff(Charger.class) != null) {
            Charger buff = curUser.buff(Charger.class);
            return buff.charges + "/" + buff.chargeCap();
        } else {
            return super.status();
        }
    }

    @Override
    public int value() {
        int price = 20 * tier;
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

    public static class Charger extends Buff implements ActionIndicator.Action {

        {
            revivePersists = true;
        }

        public int charges = 2;
        public float partialCharge;

        @Override
        public boolean act() {
            if (!(target instanceof Hero)) {
                spend(TICK);
                return true;
            }
            Hero hero = (Hero) target;

            if (charges < chargeCap()) {
                if (Regeneration.regenOn(hero)) {
                    float chargeToGain = 1 / (60f - 1.5f * (chargeCap() - charges));

                    if (hero.subClass == HeroSubClass.CHAMPION) {
                        chargeToGain *= 1.5f;
                    }

                    if (hero.buff(RingOfForce.BrawlersStance.class) != null) {
                        chargeToGain *= 0.50f;
                    }

                    partialCharge += chargeToGain;
                }

                int points = hero.pointsInTalent(Talent.WEAPON_RECHARGING);
                if (points > 0 && (hero.buff(Recharging.class) != null || hero.buff(ArtifactRecharge.class) != null)) {
                    partialCharge += 1 / (20f - 5f * points);
                }

                if (partialCharge >= 1) {
                    charges++;
                    partialCharge--;
                    updateQuickslot();
                }
            } else {
                partialCharge = 0;
            }

            if (ActionIndicator.action != this && hero.subClass == HeroSubClass.CHAMPION) {
                ActionIndicator.setAction(this);
            }

            spend(TICK);
            return true;
        }

        @Override
        public void fx(boolean on) {
            if (on && target instanceof Hero && ((Hero) target).subClass == HeroSubClass.CHAMPION) {
                ActionIndicator.setAction(this);
            }
        }

        @Override
        public void detach() {
            super.detach();
            ActionIndicator.clearAction(this);
        }

        public int chargeCap() {
            if (!(target instanceof Hero)) return 0;
            Hero hero = (Hero) target;
            if (hero.subClass == HeroSubClass.CHAMPION) {
                return Math.min(10, 4 + (hero.lvl - 1) / 3);
            } else {
                return Math.min(8, 2 + (hero.lvl - 1) / 3);
            }
        }

        public void gainCharge(float charge) {
            if (!(target instanceof Hero)) return;
            Hero hero = (Hero) target;
            if (charges < chargeCap()) {
                partialCharge += charge;
                while (partialCharge >= 1f) {
                    charges++;
                    partialCharge--;
                }
                if (charges >= chargeCap()) {
                    partialCharge = 0;
                    charges = chargeCap();
                }
                updateQuickslot();
            }
        }

        public static final String CHARGES = "charges";
        private static final String PARTIALCHARGE = "partialCharge";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(CHARGES, charges);
            bundle.put(PARTIALCHARGE, partialCharge);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            charges = bundle.getInt(CHARGES);
            partialCharge = bundle.getFloat(PARTIALCHARGE);
        }

        @Override
        public String actionName() {
            return Messages.get(MeleeWeapon.class, "swap");
        }

        @Override
        public int actionIcon() {
            return HeroIcon.WEAPON_SWAP;
        }

        @Override
        public Visual primaryVisual() {
            if (!(target instanceof Hero)) return new HeroIcon(this);
            Hero hero = (Hero) target;
            Image ico;
            if (hero.belongings.weapon == null) {
                ico = new HeroIcon(this);
            } else {
                ico = new ItemSprite(hero.belongings.weapon);
            }
            ico.width += 4;
            return ico;
        }

        @Override
        public Visual secondaryVisual() {
            if (!(target instanceof Hero)) return new HeroIcon(this);
            Hero hero = (Hero) target;
            Image ico;
            if (hero.belongings.secondWep == null) {
                ico = new HeroIcon(this);
            } else {
                ico = new ItemSprite(hero.belongings.secondWep);
            }
            ico.scale.set(PixelScene.align(0.51f));
            ico.brightness(0.6f);
            return ico;
        }

        @Override
        public int indicatorColor() {
            return 0x5500BB;
        }

        @Override
        public void doAction() {
            if (!(target instanceof Hero)) return;
            Hero hero = (Hero) target;

            if (hero.subClass != HeroSubClass.CHAMPION) {
                return;
            }

            if (hero.belongings.secondWep == null && hero.belongings.backpack.items.size() >= hero.belongings.backpack.capacity()) {
                GLog.w(Messages.get(MeleeWeapon.class, "swap_full"));
                return;
            }

            KindOfWeapon temp = hero.belongings.weapon;
            hero.belongings.weapon = hero.belongings.secondWep;
            hero.belongings.secondWep = temp;

            hero.sprite.operate(hero.pos);
            Sample.INSTANCE.play(Assets.Sounds.UNLOCK);

            ActionIndicator.setAction(this);
            Item.updateQuickslot();
            AttackIndicator.updateState();
        }
    }
}