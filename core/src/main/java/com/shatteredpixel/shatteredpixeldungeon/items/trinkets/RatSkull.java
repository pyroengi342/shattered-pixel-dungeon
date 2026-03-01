// RatSkull.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class RatSkull extends Trinket {

    {
        setImage(ItemSpriteSheet.RAT_SKULL);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        //     Hero viewer = Multiplayer.localHero();
        //int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc", (int)(exoticChanceMultiplier()));
    }

    public static float exoticChanceMultiplier() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(RatSkull.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl == -1) return 1f;
        return 2f + 1f * lvl;
    }
}