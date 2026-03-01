// MossyClump.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

import network.Multiplayer;

public class MossyClump extends Trinket {

    {
        setImage(ItemSpriteSheet.MOSSY_CLUMP);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 10 + 5 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc", (int) (100 * overrideNormalLevelChance()));
    }

    public static float overrideNormalLevelChance() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(MossyClump.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl == -1) return 0f;
        return 0.25f + 0.25f * lvl;
    }

    // true for grass, false for water
    private final ArrayList<Boolean> levelFeels = new ArrayList<>();
    private int shuffles = 0;

    public static Level.Feeling getNextFeeling() {
        MossyClump clump = null;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            clump = player.hero.belongings.getItem(MossyClump.class);
            if (clump != null) break;
        }

        if (clump == null)
            return Level.Feeling.NONE;

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