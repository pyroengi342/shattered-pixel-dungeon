// ScrollOfAntiMagic.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.AudioWrapper;
import network.Multiplayer;

public class ScrollOfAntiMagic extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_ANTIMAGIC);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		Buff.affect(hero, MagicImmune.class, MagicImmune.DURATION);
		if (hero == Multiplayer.localHero()) {
			new Flare(5, 32).color(0x00FF00, true).show(hero.sprite, 2f);
			AudioWrapper.play(Assets.Sounds.READ, hero.pos);
		}
		identify(true);
		readAnimation(hero);
	}
}