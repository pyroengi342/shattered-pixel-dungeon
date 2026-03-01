// PetrifiedSeed.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class PetrifiedSeed extends Trinket {

    {
        setImage(ItemSpriteSheet.PETRIFIED_SEED);
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
                Messages.decimalFormat("#.##", 100 * stoneInsteadOfSeedChance( viewer)),
                Messages.decimalFormat("#.##", 100 * (grassLootMultiplier( viewer) - 1f)));
    }

    public static float grassLootMultiplier(Hero hero) {
        int lvl = trinketLevel(PetrifiedSeed.class, hero);
        if (lvl <= 0) return 1f;
        return 1f + .25f * lvl / 3f;
    }

    public static float stoneInsteadOfSeedChance(Hero hero) {
        int lvl = trinketLevel(PetrifiedSeed.class, hero);
        switch (lvl) {
            default: return 0;
            case 0: return 0.25f;
            case 1: return 0.46f;
            case 2: return 0.65f;
            case 3: return 0.8f;
        }
    }
}