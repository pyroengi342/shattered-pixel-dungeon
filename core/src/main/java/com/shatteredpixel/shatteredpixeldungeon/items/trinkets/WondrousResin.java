package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class WondrousResin extends Trinket {

	{
		setImage(ItemSpriteSheet.WONDROUS_RESIN);
	}

	@Override
	protected int upgradeEnergyCost() {
		//6 -> 10(16) -> 15(31) -> 20(51)
		return 10+5*level();
	}

	@Override
	public String statsDesc() {
		Hero viewer = Multiplayer.localHero();
		int level = isIdentified() ? buffedLvl() : 0;
		if (isIdentified()){
			return Messages.get(this, "stats_desc",
					Messages.decimalFormat("#.##", 100 * positiveCurseEffectChance(level, viewer)),
					Messages.decimalFormat("#.##", 100 * extraCurseEffectChance(level, viewer)));
		} else {
			return Messages.get(this, "typical_stats_desc",
					Messages.decimalFormat("#.##", 100 * positiveCurseEffectChance(0, viewer)),
					Messages.decimalFormat("#.##", 100 * extraCurseEffectChance(0, viewer)));
		}
	}

	//used when bonus curse effects are being created
	public static boolean forcePositive = false;

    public static float positiveCurseEffectChance(Hero hero) {
        return positiveCurseEffectChance(trinketLevel(WondrousResin.class, hero), hero);
    }

    public static float positiveCurseEffectChance(int level, Hero hero) {
        // если нужно учитывать ещё что-то связанное с героем
        if (level >= 0) {
            return 0.25f + 0.25f * level;
        } else {
            return 0;
        }
    }

    public static float extraCurseEffectChance(Hero hero) {
        return extraCurseEffectChance(trinketLevel(WondrousResin.class, hero), hero);
    }

    public static float extraCurseEffectChance(int level, Hero hero) {
        if (level >= 0) {
            return 0.125f + 0.125f * level;
        } else {
            return 0;
        }
    }

}
