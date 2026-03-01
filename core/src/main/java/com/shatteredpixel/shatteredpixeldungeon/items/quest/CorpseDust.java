/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class CorpseDust extends Item {
	
	{
		setImage(ItemSpriteSheet.DUST);
		
		cursed = true;
		cursedKnown = true;
		
		unique = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		return new ArrayList<>(); //yup, no dropping this one
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		if (super.doPickUp(hero, pos)){
			GLog.n( Messages.get( this, "chill") );
			Buff.affect(hero, DustGhostSpawner.class, this);
			return true;
		}
		return false;
	}

	@Override
	protected void onDetach() {

		DustGhostSpawner spawner = curUser.buff(DustGhostSpawner.class);
		if (spawner != null){
			spawner.dispel();
		}
	}

	public static class DustGhostSpawner extends Buff {

		int spawnPower = 0;

		{
			//not cleansed by reviving, but does check to ensure the dust is still present
			revivePersists = true;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero && ((Hero) target).belongings.getItem(CorpseDust.class) == null){
				spawnPower = 0;
				spend(TICK);
				return true;
			}

			spawnPower++;
			int wraiths = 1;
			for (Mob mob : Dungeon.level.mobs){
				if (mob instanceof DustWraith){
					wraiths++;
				}
			}

			int powerNeeded = Math.min(49, wraiths*wraiths);
			if (powerNeeded <= spawnPower){
				ArrayList<Integer> candidates = new ArrayList<>();
				int minDist = Math.round(target.viewDistance/3f);
				for (int i = 0; i < Dungeon.level.length(); i++){
					if (target.fieldOfView[i]
							&& !Dungeon.level.solid[i]
							&& Actor.findChar( i ) == null
							&& Dungeon.level.distance(i, target.pos) > minDist){
						candidates.add(i);
					}
				}
				if (!candidates.isEmpty()){
					Wraith.spawnAt(Random.element(candidates), DustWraith.class);
					Sample.INSTANCE.play(Assets.Sounds.CURSED);
					spawnPower -= powerNeeded;
				} else {
					spawnPower = Math.min(spawnPower, 2*wraiths);
				}
			}

			spend(TICK);
			return true;
		}

		public void dispel(){
			detach();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob instanceof DustWraith){
					mob.die(null);
				}
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(1f, new Callback() {
						@Override
						public void call() {
							if (Dungeon.level != null) {
								Dungeon.level.playLevelMusic();
							}
						}
					});
				}
			});
		}

		private static final String SPAWNPOWER = "spawnpower";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( SPAWNPOWER, spawnPower );
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			spawnPower = bundle.getInt( SPAWNPOWER );
		}
	}

	public static class DustWraith extends Wraith{

		private int atkCount = 0;

		@Override
		public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
			if (enemy instanceof Hero){
				atkCount++;
				//first attack from each wraith is free, max of -200 point penalty per wraith
				if (atkCount == 2 || atkCount == 3){
					Statistics.questScores[1] -= 100;
				}
			}
			return super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		}

		private static final String ATK_COUNT = "atk_count";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(ATK_COUNT, atkCount);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			atkCount = bundle.getInt(ATK_COUNT);
		}
	}

}
