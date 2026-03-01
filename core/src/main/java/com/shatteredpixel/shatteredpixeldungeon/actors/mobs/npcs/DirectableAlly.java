package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Bundle;

import network.Multiplayer;

public class DirectableAlly extends NPC {

	{
		alignment = Char.Alignment.ALLY;
		intelligentAlly = true;
		WANDERING = new Wandering();
		HUNTING = new Hunting();
		state = WANDERING;
		actPriority = MOB_PRIO + 1;
	}

	protected boolean attacksAutomatically = true;

	protected int defendingPos = -1;
	protected boolean movingToDefendPos = false;

	private int ownerID = -1;

	public void setOwner(Hero owner) {
		this.ownerID = owner.id();
	}

	public Hero getOwner() {
		return (Hero) Actor.findById(ownerID);
	}

	public void defendPos(int cell) {
		defendingPos = cell;
		movingToDefendPos = true;
		aggro(null);
		state = WANDERING;
	}

	public void clearDefensingPos() {
		defendingPos = -1;
		movingToDefendPos = false;
	}

	public void followHero() {
		defendingPos = -1;
		movingToDefendPos = false;
		aggro(null);
		state = WANDERING;
	}

	public void targetChar(Char ch) {
		defendingPos = -1;
		movingToDefendPos = false;
		aggro(ch);
		target = ch.pos;
	}

	@Override
	public void aggro(Char ch) {
		enemy = ch;
		if (!movingToDefendPos && state != PASSIVE) {
			state = HUNTING;
		}
	}

	public void directTocell(int cell) {
		Hero owner = getOwner();
		if (owner == null || owner != Multiplayer.localHero()) return; // только владелец

		if (!owner.fieldOfView[cell]) {
			defendPos(cell);
			return;
		}

		Char ch = Actor.findChar(cell);
		if (ch == null) {
			defendPos(cell);
			return;
		}

		if (ch == owner) {
			followHero();
		} else if (ch.alignment == Char.Alignment.ENEMY) {
			targetChar(ch);
		} else {
			defendPos(cell);
		}
	}

	private class Wandering extends Mob.Wandering {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			Hero owner = getOwner();
			if (owner == null) {
				// Если нет владельца, просто стоим
				spend(TICK);
				return true;
			}

			if (enemyInFOV && attacksAutomatically && !movingToDefendPos
					&& (defendingPos == -1 || !owner.fieldOfView[defendingPos] || canAttack(enemy))) {
				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;
			} else {
				enemySeen = false;
				int oldPos = pos;
				target = defendingPos != -1 ? defendingPos : owner.pos;
				if (getCloser(target)) {
					spend(1 / speed());
					if (pos == defendingPos) movingToDefendPos = false;
					return moveSprite(oldPos, pos);
				} else {
					if (movingToDefendPos) {
						defendingPos = pos;
						movingToDefendPos = false;
					}
					spend(TICK);
				}
			}
			return true;
		}
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			Hero owner = getOwner();
			if (owner != null && enemyInFOV && defendingPos != -1
					&& owner.fieldOfView[defendingPos] && !canAttack(enemy)) {
				target = defendingPos;
				state = WANDERING;
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private static final String OWNER_ID = "owner_id";
	private static final String DEFEND_POS = "defend_pos";
	private static final String MOVING_TO_DEFEND = "moving_to_defend";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(OWNER_ID, ownerID);
		bundle.put(DEFEND_POS, defendingPos);
		bundle.put(MOVING_TO_DEFEND, movingToDefendPos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		ownerID = bundle.getInt(OWNER_ID);
		if (bundle.contains(DEFEND_POS)) defendingPos = bundle.getInt(DEFEND_POS);
		movingToDefendPos = bundle.getBoolean(MOVING_TO_DEFEND);
	}
}