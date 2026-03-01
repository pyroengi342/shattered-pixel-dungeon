package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class VialOfBlood extends Trinket {

    {
        setImage(ItemSpriteSheet.BLOOD_VIAL);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        if (isIdentified()) {
            return Messages.get(this,
                    "stats_desc",
                    Messages.decimalFormat("#.##", 100 * (totalHealMultiplier(level, viewer) - 1f)),
                    Integer.toString(maxHealPerTurn(level, viewer)));
        } else {
            return Messages.get(this,
                    "typical_stats_desc",
                    Messages.decimalFormat("#.##", 100 * (totalHealMultiplier(0, viewer) - 1f)),
                    Integer.toString(maxHealPerTurn(0, viewer)));
        }
    }

    // Версии с явным героем
    public static boolean delayBurstHealing(Hero hero) {
        return trinketLevel(VialOfBlood.class, hero) != -1;
    }

    public static float totalHealMultiplier(Hero hero) {
        return totalHealMultiplier(trinketLevel(VialOfBlood.class, hero), hero);
    }

    public static float totalHealMultiplier(int level, Hero hero) {
        if (level == -1) {
            return 1;
        } else {
            return 1f + 0.125f * (level + 1);
        }
    }

    public static int maxHealPerTurn(Hero hero) {
        return maxHealPerTurn(trinketLevel(VialOfBlood.class, hero), hero);
    }

    public static int maxHealPerTurn(int level, Hero hero) {
        int maxHP = (hero != null) ? hero.HT : 20; // если герой неизвестен, используем 20
        if (level == -1) {
            return maxHP;
        } else {
            switch (level) {
                case 0:
                default:
                    return 4 + Math.round(0.15f * maxHP);
                case 1:
                    return 3 + Math.round(0.10f * maxHP);
                case 2:
                    return 2 + Math.round(0.07f * maxHP);
                case 3:
                    return 1 + Math.round(0.05f * maxHP);
            }
        }
    }
}