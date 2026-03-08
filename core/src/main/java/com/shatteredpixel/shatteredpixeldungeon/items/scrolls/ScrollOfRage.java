// ScrollOfRage.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfRage extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_RAGE);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);

		Hero local = Multiplayer.localHero();

		// Логика: все мобы бегут к читающему и получают Amok
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			mob.beckon(hero.pos);
			if (mob.alignment != Char.Alignment.ALLY) {
				Buff.prolong(mob, Amok.class, 5f, this);
			}
		}

		if (local == hero) {
			GLog.w(Messages.get(this, "roar"));
		}

		identify(true);

		hero.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);

		AudioWrapper.playGlobal(Assets.Sounds.CHALLENGE);

		readAnimation(hero);
	}

	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}
}