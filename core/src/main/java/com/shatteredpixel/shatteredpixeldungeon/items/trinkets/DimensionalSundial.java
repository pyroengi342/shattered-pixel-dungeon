// DimensionalSundial.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.Calendar;
import java.util.GregorianCalendar;

import network.Multiplayer;

public class DimensionalSundial extends Trinket {

    {
        setImage(ItemSpriteSheet.SUNDIAL);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
//        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this,
                "stats_desc",
                (int) (100 * (1f - enemySpawnMultiplierDaytime(level))),
                (int) (100 * (enemySpawnMultiplierNighttime(level) - 1f)));
    }

    // Для предупреждения о времени суток (локальное для клиента)
    public static boolean sundialWarned = false;

    public static float spawnMultiplierAtCurrentTime() {
        int lvl = -1;
        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
            lvl = trinketLevel(DimensionalSundial.class, player.hero);
            if (lvl != -1) break;
        }
        if (lvl != -1) {
            Calendar cal = GregorianCalendar.getInstance();
            if (cal.get(Calendar.HOUR_OF_DAY) >= 20 || cal.get(Calendar.HOUR_OF_DAY) <= 7) {
                if (!sundialWarned) {
                    GLog.w(Messages.get(DimensionalSundial.class, "warning"));
                    sundialWarned = true;
                }
                return enemySpawnMultiplierNighttime(lvl);
            } else {
                return enemySpawnMultiplierDaytime(lvl);
            }
        } else {
            return 1f;
        }
    }
    public static float enemySpawnMultiplierDaytime(int level) {
        if (level == -1) return 1f;
        return 0.95f - 0.05f * level;
    }

    public static float enemySpawnMultiplierNighttime(int level) {
//        int lvl = -1;
//        for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
//            lvl = trinketLevel(DimensionalSundial.class, player.hero);
//            if (lvl != -1) break;
//        }
        if (level == -1) return 1f;
        return 1.25f + 0.25f * level;
    }
}