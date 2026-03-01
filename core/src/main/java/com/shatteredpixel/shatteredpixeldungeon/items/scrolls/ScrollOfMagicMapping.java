// ScrollOfMagicMapping.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.AudioWrapper;
import network.Multiplayer;

public class ScrollOfMagicMapping extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_MAGICMAP);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;

		boolean globalNoticed = false;
		boolean localNoticed = false;

		Hero local = Multiplayer.localHero();

		for (int i = 0; i < length; i++) {
			int terr = map[i];
			if (discoverable[i]) {
				mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover(i);
					globalNoticed = true;
					if (local != null && local.fieldOfView != null && local.fieldOfView[i]) {
						GameScene.discoverTile(i, terr);
						localNoticed = true;
					}
				}
			}
		}
		GameScene.updateFog();

		if (local == hero) {
			GLog.i(Messages.get(this, "layout"));
		}

		if (localNoticed) {
			AudioWrapper.play(Assets.Sounds.SECRET, hero.pos); // звук секрета – локальный
		}

		if (local != null && (local == hero || local.fieldOfView[hero.pos])) {
			SpellSprite.show(hero, SpellSprite.MAP);
		}

		AudioWrapper.playGlobal(Assets.Sounds.READ);

		identify(true);
		readAnimation(hero);
	}

	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}

	public static void discover(int cell) {
		CellEmitter.get(cell).start(Speck.factory(Speck.DISCOVER), 0.1f, 4);
	}
}