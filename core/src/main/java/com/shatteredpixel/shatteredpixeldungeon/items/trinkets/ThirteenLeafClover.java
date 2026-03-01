// ThirteenLeafClover.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

import network.Multiplayer;

public class ThirteenLeafClover extends Trinket {

    {
        setImage(ItemSpriteSheet.CLOVER);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
//        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc",
                Math.round(MAX_CHANCE * 100 * alterHeroDamageChance( viewer)),
                Math.round((1f - MAX_CHANCE) * 100 * alterHeroDamageChance( viewer)));
    }

    public static float alterHeroDamageChance( Hero hero) {
        int lvl = trinketLevel(ThirteenLeafClover.class, hero);
        if (lvl == -1) return 0;
        return 0.25f + 0.25f * lvl;
    }

    private static final float MAX_CHANCE = 0.6f;

    public static int alterDamageRoll(int min, int max) {
        if (Random.Float() < MAX_CHANCE) {
            return max;
        } else {
            return min;
        }
    }
}