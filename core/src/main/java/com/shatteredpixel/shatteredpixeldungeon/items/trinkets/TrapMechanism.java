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

public class TrapMechanism extends Trinket {

	{
		image = ItemSpriteSheet.TRAP_MECHANISM;
	}

	@Override
	protected int upgradeEnergyCost() {
		return 6+2*level();
	}

	@Override
	public String statsDesc() {
		Hero viewer = Multiplayer.localHero();
		int level = isIdentified() ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc",
				(int)(100 * overrideNormalLevelChance(level, viewer)),
				(int)(100 * revealHiddenTrapChance(level, viewer)));
	}

	// Версии с героем для использования в логике
	public static float overrideNormalLevelChance(int level, Hero hero) {
		if (hero == null) return 0f;
		int lvl = trinketLevel(TrapMechanism.class, hero);
		if (lvl == -1) return 0f;
		return 0.25f + 0.25f * lvl;
	}

	public static float revealHiddenTrapChance(int level, Hero hero) {
		if (hero == null) return 0f;
		int lvl = trinketLevel(TrapMechanism.class, hero);
		if (lvl == -1) return 0f;
		return 0.1f + 0.1f * lvl;
	}

	// Метод для получения ощущения уровня (используется при генерации)
	public static Level.Feeling getNextFeeling(Hero hero) {
		if (hero == null) return Level.Feeling.NONE;
		TrapMechanism mech = hero.belongings.getItem(TrapMechanism.class);
		if (mech == null) {
			return Level.Feeling.NONE;
		}
		if (mech.levelFeels.isEmpty()){
			Random.pushGenerator(Dungeon.seed+1);
				mech.levelFeels.add(true);
				mech.levelFeels.add(true);
				mech.levelFeels.add(true);
				mech.levelFeels.add(false);
				mech.levelFeels.add(false);
				mech.levelFeels.add(false);
				for (int i = 0; i <= mech.shuffles; i++) {
					Random.shuffle(mech.levelFeels);
				}
				mech.shuffles++;
			Random.popGenerator();
		}

		return mech.levelFeels.remove(0) ? Level.Feeling.TRAPS : Level.Feeling.CHASM;
	}

	// true для traps, false для chasm
	private ArrayList<Boolean> levelFeels = new ArrayList<>();
	private int shuffles = 0;

	private static final String FEELS = "feels";
	private static final String SHUFFLES = "shuffles";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (!levelFeels.isEmpty()){
			boolean[] storeFeels = new boolean[levelFeels.size()];
			for (int i = 0; i < storeFeels.length; i++){
				storeFeels[i] = levelFeels.get(i);
			}
			bundle.put(FEELS, storeFeels);
		}
		bundle.put( SHUFFLES, shuffles );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		levelFeels.clear();
		if (bundle.contains(FEELS)){
			for (boolean b : bundle.getBooleanArray(FEELS)){
				levelFeels.add(b);
			}
		}
		shuffles = bundle.getInt( SHUFFLES );
	}

}