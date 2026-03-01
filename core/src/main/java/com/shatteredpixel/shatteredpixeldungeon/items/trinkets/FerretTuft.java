// FerretTuft.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class FerretTuft extends Trinket {

    {
        setImage(ItemSpriteSheet.FERRET_TUFT);
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
                Messages.decimalFormat("#.##", 100 * (evasionMultiplier() - 1f)));
    }

    public static float evasionMultiplier() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(FerretTuft.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl <= -1) return 1;
        return 1 + 0.125f * (lvl + 1);
    }
}