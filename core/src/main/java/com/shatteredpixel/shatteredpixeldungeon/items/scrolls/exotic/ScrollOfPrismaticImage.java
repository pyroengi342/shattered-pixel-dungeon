// ScrollOfPrismaticImage.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrismaticGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrismaticImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.AudioWrapper;
import network.Multiplayer;

public class ScrollOfPrismaticImage extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_PRISIMG);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		boolean found = false;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m instanceof PrismaticImage) {
				found = true;
				m.HP = m.HT;
				if (hero == Multiplayer.localHero()) {
					m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(m.HT), FloatingText.HEALING);
				}
			}
		}

		if (!found) {
			Char ally = Stasis.getStasisAlly(hero);
			if (ally instanceof PrismaticImage) {
				found = true;
				ally.HP = ally.HT;
			}
		}

		if (!found) {
			Buff.affect(hero, PrismaticGuard.class, this).set(PrismaticGuard.maxHP(hero));
		}

		identify(true);
		AudioWrapper.play(Assets.Sounds.READ, hero.pos);
		readAnimation(hero);
	}
}