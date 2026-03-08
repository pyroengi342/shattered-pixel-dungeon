package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfRetribution extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_RETRIB);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);

		// визуальные и звуковые эффекты только для локального героя
		if (hero == Multiplayer.localHero()) {
			GameScene.flash(0x80FFFFFF);
			GLog.i(Messages.get(this, "blast"));
		}

		// масштабирование от здоровья
		float hpPercent = (hero.HT - hero.HP) / (float)(hero.HT);
		float power = Math.min(4f, 4.45f * hpPercent);

		// звук взрыва (глобальный или привязанный к позиции героя? пусть будет глобальный, чтобы все слышали)
		AudioWrapper.playGlobal(Assets.Sounds.BLAST);

		ArrayList<Mob> targets = new ArrayList<>();

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (hero.fieldOfView[mob.pos]) {
				targets.add(mob);
			}
		}

		for (Mob mob : targets){
			mob.damage(Math.round(mob.HT / 10f + (mob.HP * power * 0.225f)), this);
			if (mob.isAlive()) {
				Buff.prolong(mob, Blindness.class, Blindness.DURATION, this);
			}
		}

		Buff.prolong(hero, Weakness.class, Weakness.DURATION, this);
		Buff.prolong(hero, Blindness.class, Blindness.DURATION, this);
		Dungeon.observe(hero);

		identify(true);
		readAnimation(hero);
	}

	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}
}