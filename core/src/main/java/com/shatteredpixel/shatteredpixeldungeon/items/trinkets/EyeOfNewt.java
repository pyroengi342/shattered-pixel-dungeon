// EyeOfNewt.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class EyeOfNewt extends Trinket {

    {
        setImage(ItemSpriteSheet.EYE_OF_NEWT);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc",
                Messages.decimalFormat("#.##", 100 * (1f - visionRangeMultiplier( viewer))),
                mindVisionRange( viewer));
    }

    public static float visionRangeMultiplier(Hero hero) {
        int lvl = trinketLevel(EyeOfNewt.class, hero);
        if (lvl < 0) return 1;
        return 0.875f - 0.125f * lvl;
    }

    public static int mindVisionRange(Hero hero) {
        int lvl = trinketLevel(EyeOfNewt.class, hero);
        if (lvl < 0) return 0;
        return 2 + lvl;
    }
}