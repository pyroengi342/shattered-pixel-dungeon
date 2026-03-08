// ScrollOfMysticalEnergy.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.utils.AudioWrapper;

public class ScrollOfMysticalEnergy extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_MYSTENRG);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		Buff.affect(hero, ArtifactRecharge.class, this).set(30).ignoreHornOfPlenty = false;

		AudioWrapper.play(Assets.Sounds.READ, hero.pos);
		AudioWrapper.play(Assets.Sounds.CHARGEUP, hero.pos);

		SpellSprite.show(hero, SpellSprite.CHARGE, 0, 1, 1);
		identify(true);
		ScrollOfRecharging.charge(hero);

		readAnimation(hero);
	}
}