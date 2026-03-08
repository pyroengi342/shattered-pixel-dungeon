// ScrollOfLullaby.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.utils.AudioWrapper;

public class ScrollOfLullaby extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_LULLABY);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);

		hero.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
		AudioWrapper.play(Assets.Sounds.LULLABY, hero.pos);

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (hero.fieldOfView[mob.pos]) {
				Buff.affect(mob, Drowsy.class, Drowsy.DURATION);
				mob.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
			}
		}

		Buff.affect(hero, Drowsy.class, Drowsy.DURATION);

		GLog.i(Messages.get(this, "sooth"));

		identify(true);
		readAnimation(hero);
	}

	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}
}