// CrackedSpyglass.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class CrackedSpyglass extends Trinket {

    {
        setImage(ItemSpriteSheet.SPYGLASS);
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
                    Messages.decimalFormat("#.##", 100 * (extraLootChance() - 1f)));
        } else {
            return Messages.get(this, "stats_desc",
                    Messages.decimalFormat("#.##", 100 * extraLootChance()));
        }
    }

    // Версия с героем
    public static float extraLootChance() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(CrackedSpyglass.class, player.hero);
            if (lvl != -1) break;
        }

        if (lvl == -1) return 0;
        return 0.375f * (lvl + 1);
    }
}