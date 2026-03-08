package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Arrays;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class BrokenSeal extends Item {

    public static final String AC_AFFIX = "AFFIX";
    public static final String AC_INFO = "INFO_WINDOW";

    {
        setImage(ItemSpriteSheet.SEAL);
        cursedKnown = levelKnown = true;
        unique = true;
        bones = false;
        defaultAction = AC_INFO;
    }

    private Armor.Glyph glyph;

    // Методы, требующие героя, теперь принимают его параметром
    public boolean canTransferGlyph(Hero hero) {
        if (glyph == null) {
            return false;
        }
        if (hero.pointsInTalent(Talent.RUNIC_TRANSFERENCE) == 2) {
            return true;
        } else return hero.pointsInTalent(Talent.RUNIC_TRANSFERENCE) == 1
                && (Arrays.asList(Armor.Glyph.common).contains(glyph.getClass())
                || Arrays.asList(Armor.Glyph.uncommon).contains(glyph.getClass()));
    }

    public Armor.Glyph getGlyph() {
        return glyph;
    }

    public void setGlyph(Armor.Glyph glyph) {
        this.glyph = glyph;
    }

    public int maxShield(int armTier, int armLvl, Hero hero) {
        return 3 + 2 * armTier + hero.pointsInTalent(Talent.IRON_WILL);
    }

    @Override
    public ItemSprite.Glowing glowing() {
        return glyph != null ? glyph.glowing() : null;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_AFFIX);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_AFFIX)) {
            // Сохраняем героя для использования в селекторе
            final Hero currentHero = hero;
            GameScene.selectItem(new ArmorSelector(currentHero, this));
        } else if (action.equals(AC_INFO)) {
            GameScene.show(new WndUseItem(null, this));
        }
    }

    // outgoing может быть самой печатью или бронёй
    public void affixToArmor(Armor armor, Item outgoing, Hero hero) {
        if (armor != null) {
            if (!armor.cursedKnown) {
                if (hero == Multiplayer.localHero()) {
                    GLog.w(Messages.get(BrokenSeal.class, "unknown_armor"));
                }
            } else if (armor.cursed && (getGlyph() == null || !getGlyph().curse())) {
                if (hero == Multiplayer.localHero()) {
                    GLog.w(Messages.get(BrokenSeal.class, "cursed_armor"));
                }
            } else if (armor.glyph != null && getGlyph() != null
                    && canTransferGlyph(hero)
                    && armor.glyph.getClass() != getGlyph().getClass()) {

                // Показываем окно только локальному герою
                if (hero == Multiplayer.localHero()) {
                    GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.SEAL),
                            Messages.get(BrokenSeal.class, "choose_title"),
                            Messages.get(BrokenSeal.class, "choose_desc", armor.glyph.name(), getGlyph().name()),
                            armor.glyph.name(),
                            getGlyph().name()) {
                        @Override
                        protected void onSelect(int index) {
                            if (index == -1) return;

                            if (outgoing == BrokenSeal.this) {
                                // detach у печени требует героя
                                detach(hero.belongings.backpack);
                            } else if (outgoing instanceof Armor) {
                                ((Armor) outgoing).detachSeal();
                            }

                            if (index == 0) setGlyph(null);

                            if (hero == Multiplayer.localHero()) {
                                GLog.p(Messages.get(BrokenSeal.class, "affix"));
                                hero.sprite.operate(hero.pos);
                                AudioWrapper.playGlobal(Assets.Sounds.UNLOCK);
                            }
                            armor.affixSeal(BrokenSeal.this);
                        }

                        @Override
                        public void hide() {
                            super.hide();
                            hero.next();
                        }
                    });
                }
            } else {
                if (outgoing == this) {
                    detach(hero.belongings.backpack);
                } else if (outgoing instanceof Armor) {
                    ((Armor) outgoing).detachSeal();
                }

                if (hero == Multiplayer.localHero()) {
                    GLog.p(Messages.get(BrokenSeal.class, "affix"));
                    hero.sprite.operate(hero.pos);
                    AudioWrapper.playGlobal(Assets.Sounds.UNLOCK);
                }
                armor.affixSeal(this);
                hero.next();
            }
        }
    }

    @Override
    public String name() {
        return glyph != null ? glyph.name(super.name()) : super.name();
    }

    @Override
    public String info() {
        String info = super.info();
        if (glyph != null) {
            info += "\n\n" + Messages.get(this, "inscribed", glyph.name());
            info += " " + glyph.desc();
        }
        return info;
    }

    @Override
    public boolean isUpgradable() {
        return level() == 0;
    }

    // Внутренний класс для выбора брони, хранит героя
    private static class ArmorSelector extends WndBag.ItemSelector {
        private final Hero hero;
        private final BrokenSeal seal;

        ArmorSelector(Hero hero, BrokenSeal seal) {
            this.hero = hero;
            this.seal = seal;
        }

        @Override
        public String textPrompt() {
            return Messages.get(BrokenSeal.class, "prompt");
        }

        @Override
        public Class<? extends Bag> preferredBag() {
            return Belongings.Backpack.class;
        }

        @Override
        public boolean itemSelectable(Item item) {
            return item instanceof Armor;
        }

        @Override
        public void onSelect(Item item) {
            if (item instanceof Armor) {
                seal.affixToArmor((Armor) item, seal, hero);
            }
        }
    }

    private static final String GLYPH = "glyph";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(GLYPH, glyph);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        glyph = (Armor.Glyph) bundle.get(GLYPH);
    }

    public static class WarriorShield extends ShieldBuff {

        {
            type = buffType.POSITIVE;
            detachesAtZero = false;
            shieldUsePriority = 2;
        }

        private Armor armor;
        private int cooldown = 0;
        private float turnsSinceEnemies = 0;
        private static final int COOLDOWN_START = 150;

        @Override
        public int icon() {
            if (coolingDown() || shielding() > 0 || cooldown < 0) {
                return BuffIndicator.SEAL_SHIELD;
            } else {
                return BuffIndicator.NONE;
            }
        }

        @Override
        public void tintIcon(Image icon) {
            icon.resetColor();
            if (coolingDown() && shielding() == 0) {
                icon.brightness(0.3f);
            } else if (cooldown < 0) {
                icon.invert();
            }
        }

        @Override
        public float iconFadePercent() {
            if (shielding() > 0) {
                return GameMath.gate(0, 1f - shielding() / (float) maxShield(), 1);
            } else if (coolingDown()) {
                return GameMath.gate(0, cooldown / (float) COOLDOWN_START, 1);
            } else if (cooldown < 0) {
                return GameMath.gate(0, (COOLDOWN_START + cooldown) / (float) COOLDOWN_START, 1);
            } else {
                return 0;
            }
        }

        @Override
        public String iconTextDisplay() {
            if (shielding() > 0) {
                return Integer.toString(shielding());
            } else if (coolingDown() || cooldown < 0) {
                return Integer.toString(cooldown);
            } else {
                return "";
            }
        }

        @Override
        public String desc() {
            if (shielding() > 0) {
                return Messages.get(this, "desc_active", shielding(), cooldown);
            } else if (cooldown < 0) {
                return Messages.get(this, "desc_negative_cooldown", cooldown);
            } else {
                return Messages.get(this, "desc_cooldown", cooldown);
            }
        }

        @Override
        public synchronized boolean act() {
            if (cooldown > 0 && Regeneration.regenOn(((Hero) target))) {
                cooldown--;
            }

            if (shielding() > 0) {
                if (((Hero) target).visibleEnemies() == 0 && target.buff(Combo.class) == null) {
                    turnsSinceEnemies += HoldFast.buffDecayFactor(target);
                    if (turnsSinceEnemies >= 5) {
                        if (cooldown > 0) {
                            float percentLeft = shielding() / (float) maxShield();
                            cooldown = Math.max(0, (int) (cooldown - COOLDOWN_START * (percentLeft / 2f)));
                        }
                        decShield(shielding());
                    }
                } else {
                    turnsSinceEnemies = 0;
                }
            }

            if (shielding() <= 0 && maxShield() <= 0 && cooldown == 0) {
                detach();
            }

            spend(TICK);
            return true;
        }

        public synchronized void activate() {
            incShield(maxShield());
            cooldown = Math.max(0, cooldown + COOLDOWN_START);
            turnsSinceEnemies = 0;
        }

        public boolean coolingDown() {
            return cooldown > 0;
        }

        public void reduceCooldown(float percentage) {
            cooldown -= Math.round(COOLDOWN_START * percentage);
            cooldown = Math.max(cooldown, -COOLDOWN_START);
        }

        public synchronized void setArmor(Armor arm) {
            armor = arm;
        }

        public synchronized int maxShield() {
            // target уже является владельцем баффа
            Hero owner = (Hero) target;
            if (owner.heroClass != HeroClass.WARRIOR && owner.hasTalent(Talent.IRON_WILL)) {
                return owner.pointsInTalent(Talent.IRON_WILL);
            }
            if (armor != null && armor.isEquipped(owner) && armor.checkSeal() != null) {
                return armor.checkSeal().maxShield(armor.tier, armor.level(), owner);
            } else {
                return 0;
            }
        }

        public static final String COOLDOWN = "cooldown";
        public static final String TURNS_SINCE_ENEMIES = "turns_since_enemies";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(COOLDOWN, cooldown);
            bundle.put(TURNS_SINCE_ENEMIES, turnsSinceEnemies);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (bundle.contains(COOLDOWN)) {
                cooldown = bundle.getInt(COOLDOWN);
                turnsSinceEnemies = bundle.getFloat(TURNS_SINCE_ENEMIES);
            } else if (shielding() > 0) {
                turnsSinceEnemies = -100;
            }
        }
    }
}