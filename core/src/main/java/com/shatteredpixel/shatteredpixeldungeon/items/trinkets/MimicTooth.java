// MimicTooth.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class MimicTooth extends Trinket {

    {
        image = ItemSpriteSheet.MIMIC_TOOTH;
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
                Messages.decimalFormat("#.##", mimicChanceMultiplier(level, viewer)),
                Messages.decimalFormat("#.##", 100 * ebonyMimicChance(level, viewer)));
    }

    // Новые методы с героем
    public static float mimicChanceMultiplier(int level, Hero hero) {
        int lvl = trinketLevel(MimicTooth.class, hero);
        if (lvl == -1) return 1f;
        return 1.5f + 0.5f * lvl;
    }

    public static boolean stealthyMimics(Hero hero) {
        return trinketLevel(MimicTooth.class, hero) >= 0;
    }

    public static float ebonyMimicChance(int level, Hero hero) {
        int lvl = trinketLevel(MimicTooth.class, hero);
        if (lvl >= 0) return 0.125f + 0.125f * lvl;
        else return 0;
    }
}