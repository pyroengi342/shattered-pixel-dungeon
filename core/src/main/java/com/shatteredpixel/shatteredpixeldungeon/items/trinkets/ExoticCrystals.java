// ExoticCrystals.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class ExoticCrystals extends Trinket {

    {
        setImage(ItemSpriteSheet.EXOTIC_CRYSTALS);
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
                Messages.decimalFormat("#.##", 100 * consumableExoticChance()));
    }

    public static float consumableExoticChance() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(ExoticCrystals.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl == -1) return 0f;
        return 0.125f + 0.125f * lvl;
    }
}