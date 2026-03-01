// ScrollOfDread.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.AudioWrapper;
import network.Multiplayer;

public class ScrollOfDread extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_DREAD);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		if (hero == Multiplayer.localHero()) {
			new Flare(5, 32).color(0xFF0000, true).show(hero.sprite, 2f);
			AudioWrapper.play(Assets.Sounds.READ, hero.pos);
		}
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.alignment != Char.Alignment.ALLY && hero.fieldOfView[mob.pos]) {
				if (!mob.isImmune(Dread.class)) {
					Buff.affect(mob, Dread.class, this).object = hero.id();
				} else {
					Buff.affect(mob, Terror.class, Terror.DURATION, this).object = hero.id();
				}
			}
		}
		identify(true);
		readAnimation(hero);
	}
}