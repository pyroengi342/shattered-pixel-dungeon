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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.LarvaSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.YogSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import network.Multiplayer;

public class YogDzewa extends Mob {

	{
		spriteClass = YogSprite.class;

		HP = HT = 1000;

		EXP = 50;

		//so that allies can attack it. States are never actually used.
		state = HUNTING;

		viewDistance = 12;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.DEMONIC);
		properties.add(Property.STATIC);
	}

	private int phase = 0;

	private float abilityCooldown;
	private static final int MIN_ABILITY_CD = 10;
	private static final int MAX_ABILITY_CD = 15;

	private float summonCooldown;
	private static final int MIN_SUMMON_CD = 10;
	private static final int MAX_SUMMON_CD = 15;

	private static Class getPairedFist(Class fist){
		if (fist == YogFist.BurningFist.class) return YogFist.SoiledFist.class;
		if (fist == YogFist.SoiledFist.class) return YogFist.BurningFist.class;
		if (fist == YogFist.RottingFist.class) return YogFist.RustedFist.class;
		if (fist == YogFist.RustedFist.class) return YogFist.RottingFist.class;
		if (fist == YogFist.BrightFist.class) return YogFist.DarkFist.class;
		if (fist == YogFist.DarkFist.class) return YogFist.BrightFist.class;
		return null;
	}

	private final ArrayList<Class> fistSummons = new ArrayList<>();
	private final ArrayList<Class> challengeSummons = new ArrayList<>();
	{
		//offset seed slightly to avoid output patterns
		Random.pushGenerator(Dungeon.seedCurDepth()+1);
			fistSummons.add(Random.Int(2) == 0 ? YogFist.BurningFist.class : YogFist.SoiledFist.class);
			fistSummons.add(Random.Int(2) == 0 ? YogFist.RottingFist.class : YogFist.RustedFist.class);
			fistSummons.add(Random.Int(2) == 0 ? YogFist.BrightFist.class : YogFist.DarkFist.class);
			Random.shuffle(fistSummons);
			//randomly place challenge summons so that two fists of a pair can never spawn together
			if (Random.Int(2) == 0){
				challengeSummons.add(getPairedFist(fistSummons.get(1)));
				challengeSummons.add(getPairedFist(fistSummons.get(2)));
				challengeSummons.add(getPairedFist(fistSummons.get(0)));
			} else {
				challengeSummons.add(getPairedFist(fistSummons.get(2)));
				challengeSummons.add(getPairedFist(fistSummons.get(0)));
				challengeSummons.add(getPairedFist(fistSummons.get(1)));
			}
		Random.popGenerator();
	}

	private final ArrayList<Class> regularSummons = new ArrayList<>();
	{
		if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
			for (int i = 0; i < 6; i++){
				if (i >= 4){
					regularSummons.add(YogRipper.class);
				} else if (i >= Statistics.spawnersAlive){
					regularSummons.add(Larva.class);
				} else {
					regularSummons.add( i % 2 == 0 ? YogEye.class : YogScorpio.class);
				}
			}
		} else {
			for (int i = 0; i < 4; i++){
				if (i >= Statistics.spawnersAlive){
					regularSummons.add(Larva.class);
				} else {
					regularSummons.add(YogRipper.class);
				}
			}
		}
		Random.shuffle(regularSummons);
	}

	private final ArrayList<Integer> targetedCells = new ArrayList<>();

	@Override
	public int attackSkill(Char target) {
		return INFINITE_ACCURACY;
	}

	@Override
	protected boolean act() {
		// char logic
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);

		throwItems();

		sprite.hideAlert();
		sprite.hideLost();

		// mob logic
		enemy = chooseEnemy();
		enemySeen = enemy != null && enemy.isAlive() && fieldOfView[enemy.pos] && enemy.invisible <= 0;
		// end of char/mob logic

   		boolean anyHeroSeesBoss = Multiplayer.isVisibleToAnyHero(pos);

		// Фаза 0: просто ждём, пока герой подойдёт
		if (phase == 0) {
			// В оригинале проверялось, видит ли герой босса с учётом дистанции
			// Здесь мы можем проверять любого героя (anyHeroSeesBoss)
			if (anyHeroSeesBoss) {
				// Если босс виден, вызываем notice() (переводим в активную фазу)
				notice();
			}
			spend(TICK);
			return true;
		}

		// Фаза >0 – активная битва

		// Выбираем цель для лучей и призыва (ближайшего живого героя)
		Hero targetHero = Multiplayer.findNearestHero(pos);
		if (targetHero == null) {
			// Если никого нет, просто ждём
			spend(TICK);
			return true;
		}

		boolean terrainAffected = false;
		HashSet<Char> affected = new HashSet<>();

		// Задержка лучей, если герой укоренён – пропускаем
		if (!targetHero.rooted) {
			// Проходим по всем целевым клеткам (лучам)
			for (int i : targetedCells) {
				Ballistica b = new Ballistica(pos, i, Ballistica.WONT_STOP);
				// Рисуем луч только если локальный герой видит босса или точку попадания
				Hero local = Multiplayer.localHero();
				boolean beamVisible = local != null && local.fieldOfView != null &&
						(local.fieldOfView[pos] || local.fieldOfView[b.collisionPos]);
				if (beamVisible) {
					sprite.parent.add(new Beam.DeathRay(sprite.center(), DungeonTilemap.raisedTileCenterToWorld(b.collisionPos)));
				}

				for (int p : b.path) {
					Char ch = Actor.findChar(p);
					if (ch != null && (ch.alignment != alignment || ch instanceof Bee)) {
						affected.add(ch);
					}
					if (Dungeon.level.flamable[p]) {
						Dungeon.level.destroy(p);
						GameScene.updateMap(p);
						terrainAffected = true;
					}
				}
			}
			if (terrainAffected) {
				Dungeon.observeAll(); // этот метод уже адаптирован под мультиплеер
			}

			Invisibility.dispel(this);

			// Наносим урон всем затронутым персонажам
			for (Char ch : affected) {
				if (ch instanceof Hero) {
					// Для статистики: уменьшаем счёт босса для конкретного героя? Пока оставляем глобально, но помечаем TODO
					Statistics.bossScores[4] -= 500;
					// TODO: в мультиплеере статистика должна быть привязана к герою
				}

				if (hit(this, ch, true)) {
					int dmg;
					if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
						dmg = Random.NormalIntRange(30, 50);
					} else {
						dmg = Random.NormalIntRange(20, 30);
					}
					ch.damage(dmg, new Eye.DeathGaze());

					// Визуальные эффекты – только если локальный герой видит клетку персонажа
					Hero local = Multiplayer.localHero();
					if (local != null && local.fieldOfView != null && local.fieldOfView[ch.pos]) {
						ch.sprite.flash();
						CellEmitter.center(ch.pos).burst(PurpleParticle.BURST, Random.IntRange(1, 2));
					}

					if (!ch.isAlive() && ch instanceof Hero) {
						// Смерть героя – показываем сообщение и обрабатываем только для локального
						if (local != null && local == ch) {
							Badges.validateDeathFromEnemyMagic();
							Dungeon.fail(this);
							GLog.n(Messages.get(Char.class, "kill", name()));
						}
						// TODO: для других игроков отправить уведомление по сети
					}
				} else {
					// Промах – статус только если видно локально
					Hero local = Multiplayer.localHero();
					if (local != null && local.fieldOfView != null && local.fieldOfView[ch.pos]) {
						ch.sprite.showStatus(CharSprite.NEUTRAL, ch.defenseVerb());
					}
				}
			}
			targetedCells.clear();
		} // конец блока лучей

		// Подготовка новых лучей
		if (abilityCooldown <= 0) {
			int beams = 1 + (HT - HP) / 400;
			HashSet<Integer> affectedCells = new HashSet<>();
			for (int i = 0; i < beams; i++) {
				int targetPos = targetHero.pos; // цель – выбранный герой
				if (i != 0) {
					// Случайное смещение вокруг цели
					do {
						targetPos = targetHero.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
					} while (Dungeon.level.trueDistance(pos, targetHero.pos) > Dungeon.level.trueDistance(pos, targetPos));
				}
				targetedCells.add(targetPos);
				Ballistica b = new Ballistica(pos, targetPos, Ballistica.WONT_STOP);
				affectedCells.addAll(b.path);
			}

			// Удаляем один луч, если все клетки вокруг героя уже покрыты
			boolean allAdjTargeted = true;
			for (int i : PathFinder.NEIGHBOURS9) {
				if (!affectedCells.contains(targetHero.pos + i) && Dungeon.level.passable[targetHero.pos + i]) {
					allAdjTargeted = false;
					break;
				}
			}
			if (allAdjTargeted) {
				targetedCells.remove(targetedCells.size() - 1);
			}

			// Визуальные маркеры целей – только для локального героя, если видно
			Hero local = Multiplayer.localHero();
			for (int i : targetedCells) {
				Ballistica b = new Ballistica(pos, i, Ballistica.WONT_STOP);
				for (int p : b.path) {
					if (local != null && local.fieldOfView != null && local.fieldOfView[p]) {
						sprite.parent.add(new TargetedCell(p, 0xFF0000));
					}
					affectedCells.add(p);
				}
			}

			// Тратим время, пропорциональное кулдауну цели, и прерываем её
			spend(GameMath.gate(TICK, (int) Math.ceil(targetHero.cooldown()), 3 * TICK));
			targetHero.interrupt();

			abilityCooldown += Random.NormalFloat(MIN_ABILITY_CD, MAX_ABILITY_CD);
			abilityCooldown -= (phase - 1);
		} else {
			spend(TICK);
		}

		// Призыв мобов
		while (summonCooldown <= 0) {
			Class<? extends Mob> cls = regularSummons.remove(0);
			Mob summon = Reflection.newInstance(cls);
			regularSummons.add(cls);

			int spawnPos = -1;
			// Ищем свободную клетку рядом с боссом, ближайшую к цели (targetHero)
			for (int i : PathFinder.NEIGHBOURS8) {
				if (Actor.findChar(pos + i) == null) {
					if (spawnPos == -1 || Dungeon.level.trueDistance(targetHero.pos, spawnPos) > Dungeon.level.trueDistance(targetHero.pos, pos + i)) {
						spawnPos = pos + i;
					}
				}
			}

			// Если нет свободной, пробуем убить овцу
			if (spawnPos == -1) {
				for (int i : PathFinder.NEIGHBOURS8) {
					if (Actor.findChar(pos + i) instanceof Sheep) {
						if (spawnPos == -1 || Dungeon.level.trueDistance(targetHero.pos, spawnPos) > Dungeon.level.trueDistance(targetHero.pos, pos + i)) {
							spawnPos = pos + i;
						}
					}
				}
				if (spawnPos != -1) {
					Actor.findChar(spawnPos).die(null);
				}
			}

			if (spawnPos != -1) {
				summon.pos = spawnPos;
				GameScene.add(summon);
				Actor.add(new Pushing(summon, pos, summon.pos));
				summon.beckon(targetHero.pos); // направляем к цели
				Dungeon.level.occupyCell(summon);

				summonCooldown += Random.NormalFloat(MIN_SUMMON_CD, MAX_SUMMON_CD);
				summonCooldown -= (phase - 1);
				if (findFist() != null) {
					summonCooldown += MIN_SUMMON_CD - (phase - 1);
				}
			} else {
				break;
			}
		}

		if (summonCooldown > 0) summonCooldown--;
		if (abilityCooldown > 0) abilityCooldown--;

		// Ускорение на последней фазе
		if (phase == 5 && abilityCooldown > 2) {
			abilityCooldown = 2;
		}
		if (phase == 5 && summonCooldown > 3) {
			summonCooldown = 3;
		}

		return true;
	}

	public void processFistDeath(){
		//normally Yog has no logic when a fist dies specifically
		//but the very last fist to die does trigger the final phase
		if (phase == 4 && findFist() == null){
			yell(Messages.get(this, "hope"));
			summonCooldown = -15; //summon a burst of minions!
			phase = 5;
			BossHealthBar.bleed(true);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(0.5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.play(Assets.Music.HALLS_BOSS_FINALE, true);
						}
					});
				}
			});
		}
	}

	@Override
	public boolean isAlive() {
		return super.isAlive() || phase != 5;
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		return phase == 0 || findFist() != null || super.isInvulnerable(effect);
	}

	@Override
	public void damage( int dmg, Object src ) {

		int preHP = HP;
		super.damage( dmg, src );

		if (phase == 0 || findFist() != null) return;

		if (phase < 4) {
			HP = Math.max(HP, HT - 300 * phase);
		} else if (phase == 4) {
			HP = Math.max(HP, 100);
		}
		int dmgTaken = preHP - HP;

		if (dmgTaken > 0) {
			abilityCooldown -= dmgTaken / 10f;
			summonCooldown -= dmgTaken / 10f;
		}

		if (phase < 4 && HP <= HT - 300*phase){

			phase++;

			updateVisibility(Dungeon.level);
			GLog.n(Messages.get(this, "darkness"));
			sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "invulnerable"));

			addFist((YogFist)Reflection.newInstance(fistSummons.remove(0)));

			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
				addFist((YogFist)Reflection.newInstance(challengeSummons.remove(0)));
			}

			CellEmitter.get(Dungeon.level.exit()-1).burst(ShadowParticle.UP, 25);
			CellEmitter.get(Dungeon.level.exit()).burst(ShadowParticle.UP, 100);
			CellEmitter.get(Dungeon.level.exit()+1).burst(ShadowParticle.UP, 25);

			if (abilityCooldown < 5) abilityCooldown = 5;
			if (summonCooldown < 5) summonCooldown = 5;

		}

		for (Multiplayer.PlayerInfo player : Multiplayer.Players.getAll()) {
			LockedFloor lock = player.hero.buff(LockedFloor.class);
			if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
				if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmgTaken/3f);
				else                                                    lock.addTime(dmgTaken/2f);
			}
		}
	}

	public void addFist(YogFist fist){
		fist.pos = Dungeon.level.exit();

		CellEmitter.get(Dungeon.level.exit()-1).burst(ShadowParticle.UP, 25);
		CellEmitter.get(Dungeon.level.exit()).burst(ShadowParticle.UP, 100);
		CellEmitter.get(Dungeon.level.exit()+1).burst(ShadowParticle.UP, 25);

		if (abilityCooldown < 5) abilityCooldown = 5;
		if (summonCooldown < 5) summonCooldown = 5;

		int targetPos = Dungeon.level.exit() + Dungeon.level.width();

		if (!Dungeon.isChallenged(Challenges.STRONGER_BOSSES)
				&& (Actor.findChar(targetPos) == null || Actor.findChar(targetPos) instanceof Sheep)){
			fist.pos = targetPos;
		} else if (Actor.findChar(targetPos-1) == null || Actor.findChar(targetPos-1) instanceof Sheep){
			fist.pos = targetPos-1;
		} else if (Actor.findChar(targetPos+1) == null || Actor.findChar(targetPos+1) instanceof Sheep){
			fist.pos = targetPos+1;
		} else if (Actor.findChar(targetPos) == null || Actor.findChar(targetPos) instanceof Sheep){
			fist.pos = targetPos;
		}

		if (Actor.findChar(fist.pos) instanceof Sheep){
			Actor.findChar(fist.pos).die(null);
		}

		GameScene.add(fist, 4);
		Actor.add( new Pushing( fist, Dungeon.level.exit(), fist.pos ) );
		Dungeon.level.occupyCell(fist);
	}

	public void updateVisibility( Level level ){
		int viewDistance = 4;
		if (phase > 1 && isAlive()){
			viewDistance = Math.max(4 - (phase-1), 1);
		}
		if (Dungeon.isChallenged(Challenges.DARKNESS)) {
			viewDistance = Math.min(viewDistance, 2);
		}
		level.viewDistance = viewDistance;

		// Обновляем поле зрения всех живых героев
		Dungeon.observeAll();
	}

	private YogFist findFist(){
		for ( Char c : Actor.chars() ){
			if (c instanceof YogFist){
				return (YogFist) c;
			}
		}
		return null;
	}

	@Override
	public void beckon( int cell ) {
	}

	@Override
	public void clearEnemy() {
		//do nothing
	}

	@Override
	public void aggro(Char ch) {
		if (ch != null && ch.alignment != alignment || !(ch instanceof Larva || ch instanceof YogRipper || ch instanceof YogEye || ch instanceof YogScorpio)) {
			for (Mob mob : (Iterable<Mob>) Dungeon.level.mobs.clone()) {
				if (mob != ch && Dungeon.level.distance(pos, mob.pos) <= 4 && mob.alignment == alignment &&
						(mob instanceof Larva || mob instanceof YogRipper || mob instanceof YogEye || mob instanceof YogScorpio)) {
					mob.aggro(ch);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void die( Object cause ) {

		Bestiary.skipCountingEncounters = true;
		for (Mob mob : (Iterable<Mob>)Dungeon.level.mobs.clone()) {
			if (mob instanceof Larva || mob instanceof YogRipper || mob instanceof YogEye || mob instanceof YogScorpio) {
				mob.die( cause );
			}
		}
		Bestiary.skipCountingEncounters = false;

		updateVisibility(Dungeon.level);

		GameScene.bossSlain();

		if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && Statistics.spawnersAlive == 4){
			Badges.validateBossChallengeCompleted();
		} else {
			Statistics.qualifiedForBossChallengeBadge = false;
		}
		Statistics.bossScores[4] += 5000 + 1250*Statistics.spawnersAlive;

		Badges.validateTakingTheMick(cause);

		Dungeon.level.unseal();
		super.die( cause );

		yell( Messages.get(this, "defeated") );
	}

	@Override
	public void notice() {
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
				}
			});
			if (phase == 0) {
				phase = 1;
				summonCooldown = Random.NormalFloat(MIN_SUMMON_CD, MAX_SUMMON_CD);
				abilityCooldown = Random.NormalFloat(MIN_ABILITY_CD, MAX_ABILITY_CD);
			}
		}
	}

	@Override
	public String description() {
		String desc = super.description();

		if (Statistics.spawnersAlive > 0){
			desc += "\n\n" + Messages.get(this, "desc_spawners");
		}

		return desc;
	}

	private static final String PHASE = "phase";

	private static final String ABILITY_CD = "ability_cd";
	private static final String SUMMON_CD = "summon_cd";

	private static final String FIST_SUMMONS = "fist_summons";
	private static final String REGULAR_SUMMONS = "regular_summons";
	private static final String CHALLENGE_SUMMONS = "challenges_summons";

	private static final String TARGETED_CELLS = "targeted_cells";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PHASE, phase);

		bundle.put(ABILITY_CD, abilityCooldown);
		bundle.put(SUMMON_CD, summonCooldown);

		bundle.put(FIST_SUMMONS, fistSummons.toArray(new Class[0]));
		bundle.put(CHALLENGE_SUMMONS, challengeSummons.toArray(new Class[0]));
		bundle.put(REGULAR_SUMMONS, regularSummons.toArray(new Class[0]));

		int[] bundleArr = new int[targetedCells.size()];
		for (int i = 0; i < targetedCells.size(); i++){
			bundleArr[i] = targetedCells.get(i);
		}
		bundle.put(TARGETED_CELLS, bundleArr);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt(PHASE);
		if (phase != 0) {
			BossHealthBar.assignBoss(this);
			if (phase == 5) BossHealthBar.bleed(true);
		}

		abilityCooldown = bundle.getFloat(ABILITY_CD);
		summonCooldown = bundle.getFloat(SUMMON_CD);

		fistSummons.clear();
		Collections.addAll(fistSummons, bundle.getClassArray(FIST_SUMMONS));
		challengeSummons.clear();
		Collections.addAll(challengeSummons, bundle.getClassArray(CHALLENGE_SUMMONS));
		regularSummons.clear();
		Collections.addAll(regularSummons, bundle.getClassArray(REGULAR_SUMMONS));

		for (int i : bundle.getIntArray(TARGETED_CELLS)){
			targetedCells.add(i);
		}
	}

	public static class Larva extends Mob {

		{
			spriteClass = LarvaSprite.class;

			HP = HT = 20;
			defenseSkill = 12;
			viewDistance = Light.DISTANCE;

			EXP = 5;
			maxLvl = -2;

			properties.add(Property.DEMONIC);
			properties.add(Property.BOSS_MINION);
		}

		@Override
		public int attackSkill( Char target ) {
			return 30;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange( 15, 25 );
		}

		@Override
		public int drRoll() {
			return super.drRoll() + Random.NormalIntRange(0, 4);
		}

	}

	//used so death to yog's ripper demons have their own rankings description
	public static class YogRipper extends RipperDemon {
		{
			maxLvl = -2;
			properties.add(Property.BOSS_MINION);
		}
	}
	public static class YogEye extends Eye {
		{
			maxLvl = -2;
			properties.add(Property.BOSS_MINION);
		}
	}
	public static class YogScorpio extends Scorpio {
		{
			maxLvl = -2;
			properties.add(Property.BOSS_MINION);
		}
	}
}
