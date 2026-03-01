// MimicTooth.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class MimicTooth extends Trinket {

    {
        setImage(ItemSpriteSheet.MIMIC_TOOTH);
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
                Messages.decimalFormat("#.##", mimicChanceMultiplier()),
                Messages.decimalFormat("#.##", 100 * ebonyMimicChance()));
    }

    // Новые методы с героем
    public static float mimicChanceMultiplier() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(MimicTooth.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl == -1) return 1f;
        return 1.5f + 0.5f * lvl;
    }

    public static boolean stealthyMimics() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(MimicTooth.class, player.hero);
            if (lvl != -1) break;
        }
        return lvl >= 0;
    }

    public static float ebonyMimicChance() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(MimicTooth.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl >= 0) return 0.125f + 0.125f * lvl;
        else return 0;
    }
}