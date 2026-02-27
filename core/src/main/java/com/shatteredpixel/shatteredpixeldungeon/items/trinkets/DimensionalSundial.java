// DimensionalSundial.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.Multiplayer;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DimensionalSundial extends Trinket {

    {
        image = ItemSpriteSheet.SUNDIAL;
    }

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this,
                "stats_desc",
                (int) (100 * (1f - enemySpawnMultiplierDaytime(level, viewer))),
                (int) (100 * (enemySpawnMultiplierNighttime(level, viewer) - 1f)));
    }

    // Для предупреждения о времени суток (локальное для клиента)
    private static boolean sundialWarned = false;

    public static float spawnMultiplierAtCurrentTime(Hero hero) {
        if (trinketLevel(DimensionalSundial.class, hero) != -1) {
            Calendar cal = GregorianCalendar.getInstance();
            if (cal.get(Calendar.HOUR_OF_DAY) >= 20 || cal.get(Calendar.HOUR_OF_DAY) <= 7) {
                if (!sundialWarned && hero == Multiplayer.localHero()) {
                    GLog.w(Messages.get(DimensionalSundial.class, "warning"));
                    sundialWarned = true;
                }
                return enemySpawnMultiplierNighttime(hero);
            } else {
                return enemySpawnMultiplierDaytime(hero);
            }
        } else {
            return 1f;
        }
    }

    public static float enemySpawnMultiplierDaytime(Hero hero) {
        return enemySpawnMultiplierDaytime(trinketLevel(DimensionalSundial.class, hero), hero);
    }

    public static float enemySpawnMultiplierDaytime(int level, Hero hero) {
        int lvl = trinketLevel(DimensionalSundial.class, hero);
        if (lvl == -1) return 1f;
        return 0.95f - 0.05f * lvl;
    }

    public static float enemySpawnMultiplierNighttime(Hero hero) {
        return enemySpawnMultiplierNighttime(trinketLevel(DimensionalSundial.class, hero), hero);
    }

    public static float enemySpawnMultiplierNighttime(int level, Hero hero) {
        int lvl = trinketLevel(DimensionalSundial.class, hero);
        if (lvl == -1) return 1f;
        return 1.25f + 0.25f * lvl;
    }
}