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

public class TrapMechanism extends Trinket {

	{
		setImage(ItemSpriteSheet.TRAP_MECHANISM);
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
				(int)(100 * overrideNormalLevelChance( )),
				(int)(100 * revealHiddenTrapChance( )));
	}

	// Версии с героем для использования в логике
	public static float overrideNormalLevelChance() {
		int lvl = -1;
		for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
			lvl = trinketLevel(TrapMechanism.class, player.hero);
			if (lvl != -1) break;
		}
		if (lvl == -1) return 0f;
		return 0.25f + 0.25f * lvl;
	}

	public static float revealHiddenTrapChance() {
		int lvl = -1;
		for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
			lvl = trinketLevel(TrapMechanism.class, player.hero);
			if (lvl != -1) break;
		}
		if (lvl == -1) return 0f;
		return 0.1f + 0.1f * lvl;
	}

	// Метод для получения ощущения уровня (используется при генерации)
	public static Level.Feeling getNextFeeling() {
		TrapMechanism mech = null;
		for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
			mech = player.hero.belongings.getItem(TrapMechanism.class);
			if (mech != null) break;
		}

		if (mech == null) return Level.Feeling.NONE;
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
	private final ArrayList<Boolean> levelFeels = new ArrayList<>();
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