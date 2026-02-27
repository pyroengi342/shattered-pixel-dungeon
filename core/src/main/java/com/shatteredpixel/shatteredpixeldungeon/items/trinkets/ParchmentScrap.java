// ParchmentScrap.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class ParchmentScrap extends Trinket {

    {
        image = ItemSpriteSheet.PARCHMENT_SCRAP;
    }

    @Override
    protected int upgradeEnergyCost() {
        return 10 + 5 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc",
                (int) enchantChanceMultiplier(level, viewer),
                Messages.decimalFormat("#.##", curseChanceMultiplier(level, viewer)));
    }

    public static float enchantChanceMultiplier(int level, Hero hero) {
        int lvl = trinketLevel(ParchmentScrap.class, hero);
        switch (lvl) {
            default: return 1;
            case 0: return 2;
            case 1: return 4;
            case 2: return 7;
            case 3: return 10;
        }
    }

    public static float curseChanceMultiplier(int level, Hero hero) {
        int lvl = trinketLevel(ParchmentScrap.class, hero);
        switch (lvl) {
            default: return 1;
            case 0: return 1.5f;
            case 1: return 2f;
            case 2: return 1f;
            case 3: return 0f;
        }
    }
}