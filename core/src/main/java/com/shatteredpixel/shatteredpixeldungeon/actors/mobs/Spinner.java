package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SpinnerSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import network.Multiplayer;

public class Spinner extends Mob {

	{
		spriteClass = SpinnerSprite.class;

		HP = HT = 50;
		defenseSkill = 17;

		EXP = 9;
		maxLvl = 17;

		loot = MysteryMeat.class;
		lootChance = 0.125f;

		HUNTING = new Hunting();
		FLEEING = new Fleeing();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 20);
	}

	@Override
	public int attackSkill(Char target) {
		return 22;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 6);
	}

	private int webCoolDown = 0;
	private int lastEnemyPos = -1;

	private static final String WEB_COOLDOWN = "web_cooldown";
	private static final String LAST_ENEMY_POS = "last_enemy_pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(WEB_COOLDOWN, webCoolDown);
		bundle.put(LAST_ENEMY_POS, lastEnemyPos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		webCoolDown = bundle.getInt(WEB_COOLDOWN);
		lastEnemyPos = bundle.getInt(LAST_ENEMY_POS);
	}

	@Override
	protected boolean act() {
		if (state == HUNTING || state == FLEEING){
			webCoolDown--;
		}

		AiState lastState = state;
		boolean result = super.act();

		if (!(lastState == WANDERING && state == HUNTING)) {
			if (!shotWebVisually){
				if (enemy != null && enemySeen) {
					lastEnemyPos = enemy.pos;
				}
				// если враг не виден или его нет, оставляем lastEnemyPos без изменений
			}
			shotWebVisually = false;
		}

		return result;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (Random.Int(2) == 0) {
			int duration = Random.IntRange(7, 8);
			duration = Math.round(duration * (AscensionChallenge.statModifier(this) / 2f + 0.5f));
			Buff.affect(enemy, Poison.class, this).set(duration);
			webCoolDown = 0;
			state = FLEEING;
		}
		return damage;
	}

	private boolean shotWebVisually = false;

	public int webPos() {
		Char enemy = this.enemy;
		if (enemy == null) return -1;

		if (state != FLEEING && enemy.pos == lastEnemyPos && canAttack(enemy)) {
			return -1;
		}

		Ballistica b;
		if (lastEnemyPos == enemy.pos) {
			b = new Ballistica(enemy.pos, pos, Ballistica.WONT_STOP);
		} else {
			b = new Ballistica(lastEnemyPos, enemy.pos, Ballistica.WONT_STOP);
		}

		int collisionIndex = 0;
		for (int i = 0; i < b.path.size(); i++) {
			if (b.path.get(i) == enemy.pos) {
				collisionIndex = i;
				break;
			}
		}

		if (b.path.size() <= collisionIndex + 1) {
			return -1;
		}

		int webPos = b.path.get(collisionIndex + 1);

		int projectilePos = new Ballistica(pos, webPos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos;

		if (webPos != enemy.pos && projectilePos == webPos && Dungeon.level.passable[webPos]) {
			return webPos;
		} else {
			return -1;
		}
	}

	public void shootWeb() {
		int webPos = webPos();
		if (webPos != -1) {
			int i;
			for (i = 0; i < PathFinder.CIRCLE8.length; i++) {
				if ((enemy.pos + PathFinder.CIRCLE8[i]) == webPos) {
					break;
				}
			}

			int leftPos = enemy.pos + PathFinder.CIRCLE8[left(i)];
			int rightPos = enemy.pos + PathFinder.CIRCLE8[right(i)];

			if (Dungeon.level.passable[leftPos]) applyWebToCell(leftPos);
			if (Dungeon.level.passable[webPos])  applyWebToCell(webPos);
			if (Dungeon.level.passable[rightPos]) applyWebToCell(rightPos);

			webCoolDown = 10;
			Multiplayer.interruptAll();
		}
		next();
	}

	protected void applyWebToCell(int cell) {
		GameScene.add(Blob.seed(cell, 20, Web.class));
	}

	private int left(int direction) {
		return direction == 0 ? 7 : direction - 1;
	}

	private int right(int direction) {
		return direction == 7 ? 0 : direction + 1;
	}

	{
		resistances.add(Poison.class);
	}

	{
		immunities.add(Web.class);
	}

	private class Hunting extends Mob.Hunting {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (enemyInFOV && webCoolDown <= 0 && lastEnemyPos != -1) {
				if (webPos() != -1) {
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap(webPos());
						shotWebVisually = true;
						return false;
					} else {
						shootWeb();
						return true;
					}
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private class Fleeing extends Mob.Fleeing {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (buff(Terror.class) == null && buff(Dread.class) == null &&
					enemyInFOV && enemy.buff(Poison.class) == null) {
				state = HUNTING;
				return true;
			}

			if (enemyInFOV && webCoolDown <= 0 && lastEnemyPos != -1) {
				if (webPos() != -1) {
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap(webPos());
						shotWebVisually = true;
						return false;
					} else {
						shootWeb();
						return true;
					}
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}
}