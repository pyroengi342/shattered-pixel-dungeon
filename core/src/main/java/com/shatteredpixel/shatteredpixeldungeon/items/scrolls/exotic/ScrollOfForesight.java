// ScrollOfForesight.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.utils.AudioWrapper;

public class ScrollOfForesight extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_FORESIGHT);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		AudioWrapper.play(Assets.Sounds.READ, hero.pos);

		Buff.affect(hero, Foresight.class, Foresight.DURATION);

		identify(true);
		readAnimation(hero);
	}
}