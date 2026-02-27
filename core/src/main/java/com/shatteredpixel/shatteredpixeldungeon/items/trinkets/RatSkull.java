// RatSkull.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class RatSkull extends Trinket {

    {
        image = ItemSpriteSheet.RAT_SKULL;
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc", (int)(exoticChanceMultiplier(level, viewer)));
    }

    public static float exoticChanceMultiplier(int level, Hero hero) {
        int lvl = trinketLevel(RatSkull.class, hero);
        if (lvl == -1) return 1f;
        return 2f + 1f * lvl;
    }
}