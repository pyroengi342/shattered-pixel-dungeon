// SaltCube.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class SaltCube extends Trinket {

    {
        setImage(ItemSpriteSheet.SALT_CUBE);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
//        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this,
                "stats_desc",
                Messages.decimalFormat("#.##", 100 * ((1f / hungerGainMultiplier( viewer)) - 1f)),
                Messages.decimalFormat("#.##", 100 * (1f - healthRegenMultiplier( viewer))));
    }

    public static float hungerGainMultiplier(Hero hero) {
        int lvl = trinketLevel(SaltCube.class, hero);
        if (lvl == -1) return 1f;
        return 1f / (1f + 0.25f * (lvl + 1));
    }

    public static float healthRegenMultiplier( Hero hero) {
        int lvl = trinketLevel(SaltCube.class, hero);
        switch (lvl) {
            case -1: default:
                return 1f;
            case 0:
                return 0.84f;
            case 1:
                return 0.73f;
            case 2:
                return 0.66f;
            case 3:
                return 0.6f;
        }
    }

}