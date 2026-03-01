package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollTricksterSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import network.Multiplayer;

public class GnollTrickster extends Gnoll {

	{
		spriteClass = GnollTricksterSprite.class;

		HP = HT = 20;
		defenseSkill = 5;

		EXP = 5;

		WANDERING = new Wandering();
		state = WANDERING;

		loot = Generator.Category.MISSILE;
		lootChance = 1f;

		properties.add(Property.MINIBOSS);
	}

	private int combo = 0;

	@Override
	public int attackSkill( Char target ) {
		return 16;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !Dungeon.level.adjacent( pos, enemy.pos )
				&& (super.canAttack(enemy) || new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (combo >= 1){
			Statistics.questScores[0] -= 50;
		}

		combo++;
		int effect = Random.Int(4)+combo;

		if (effect > 2) {
			if (effect >=6 && enemy.buff(Burning.class) == null){
				if (Dungeon.level.flamable[enemy.pos]) {
					GameScene.add(Blob.seed(enemy.pos, 4, Fire.class));
				}
				Buff.affect(enemy, Burning.class, this).reignite( enemy );
			} else {
				Buff.affect(enemy, Poison.class, this).set((effect - 2));
			}
		}
		return damage;
	}

	@Override
	protected boolean getCloser( int target ) {
		combo = 0;
		if (state == HUNTING) {
			return enemySeen && getFurther( target );
		} else {
			return super.getCloser( target );
		}
	}

	@Override
	public void aggro(Char ch) {
		if (ch == null || fieldOfView == null
				|| fieldOfView.length != Dungeon.level.length() || fieldOfView[ch.pos]) {
			super.aggro(ch);
		}
	}

	@Override
	public Item createLoot() {
		MissileWeapon drop = (MissileWeapon)super.createLoot();
		drop.level(0);
		if (drop.hasCurseEnchant()){
			drop.enchant(null);
		}
		drop.cursed = false;
		drop.identify(false);
		drop.quantity((drop.quantity()+1)/2);
		return drop;
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		Ghost.Quest.process();
	}

	protected class Wandering extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			int pos1 = super.randomDestination();
			int pos2 = super.randomDestination();

			Hero nearest = Multiplayer.findNearestHero(pos);
			if (nearest == null) return pos1;

			int heroPos = nearest.pos;
			PathFinder.buildDistanceMap(heroPos, Dungeon.level.passable);
			if (PathFinder.distance[pos2] < PathFinder.distance[pos1]) {
				return pos2;
			} else {
				return pos1;
			}
		}
	}

	private static final String COMBO = "combo";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COMBO, combo);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		combo = bundle.getInt( COMBO );
	}
}