// CrackedSpyglass.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class CrackedSpyglass extends Trinket {

    {
        image = ItemSpriteSheet.SPYGLASS;
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        if (isIdentified() && buffedLvl() >= 2) {
            return Messages.get(this, "stats_desc_upgraded",
                    Messages.decimalFormat("#.##", 100 * (extraLootChance(level, viewer) - 1f)));
        } else {
            return Messages.get(this, "stats_desc",
                    Messages.decimalFormat("#.##", 100 * extraLootChance(level, viewer)));
        }
    }

    // Версия с героем
    public static float extraLootChance(int level, Hero hero) {
        int lvl = trinketLevel(CrackedSpyglass.class, hero);
        if (lvl == -1) return 0;
        return 0.375f * (lvl + 1);
    }
}