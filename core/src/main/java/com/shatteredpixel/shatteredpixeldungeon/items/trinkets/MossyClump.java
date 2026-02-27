// MossyClump.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import network.Multiplayer;

import java.util.ArrayList;

public class MossyClump extends Trinket {

    {
        image = ItemSpriteSheet.MOSSY_CLUMP;
    }

    @Override
    protected int upgradeEnergyCost() {
        return 10 + 5 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc", (int) (100 * overrideNormalLevelChance(level, viewer)));
    }

    public static float overrideNormalLevelChance(int level, Hero hero) {
        int lvl = trinketLevel(MossyClump.class, hero);
        if (lvl == -1) return 0f;
        return 0.25f + 0.25f * lvl;
    }

    // true for grass, false for water
    private ArrayList<Boolean> levelFeels = new ArrayList<>();
    private int shuffles = 0;

    public static Level.Feeling getNextFeeling(Hero hero) {
        if (hero == null) return Level.Feeling.NONE;
        MossyClump clump = hero.belongings.getItem(MossyClump.class);
        if (clump == null) {
            return Level.Feeling.NONE;
        }
        if (clump.levelFeels.isEmpty()) {
            Random.pushGenerator(Dungeon.seed + 1);
            clump.levelFeels.add(true);
            clump.levelFeels.add(true);
            clump.levelFeels.add(false);
            clump.levelFeels.add(false);
            clump.levelFeels.add(false);
            clump.levelFeels.add(false);
            for (int i = 0; i <= clump.shuffles; i++) {
                Random.shuffle(clump.levelFeels);
            }
            clump.shuffles++;
            Random.popGenerator();
        }

        return clump.levelFeels.remove(0) ? Level.Feeling.GRASS : Level.Feeling.WATER;
    }

    private static final String FEELS = "feels";
    private static final String SHUFFLES = "shuffles";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        if (!levelFeels.isEmpty()) {
            boolean[] storeFeels = new boolean[levelFeels.size()];
            for (int i = 0; i < storeFeels.length; i++) {
                storeFeels[i] = levelFeels.get(i);
            }
            bundle.put(FEELS, storeFeels);
        }
        bundle.put(SHUFFLES, shuffles);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        levelFeels.clear();
        if (bundle.contains(FEELS)) {
            for (boolean b : bundle.getBooleanArray(FEELS)) {
                levelFeels.add(b);
            }
        }
        shuffles = bundle.getInt(SHUFFLES);
    }
}