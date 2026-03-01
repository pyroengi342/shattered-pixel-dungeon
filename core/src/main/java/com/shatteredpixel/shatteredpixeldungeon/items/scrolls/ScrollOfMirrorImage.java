// ScrollOfMirrorImage.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import network.AudioWrapper;
import network.Multiplayer;

import java.util.ArrayList;

public class ScrollOfMirrorImage extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_MIRRORIMG);
	}

	private static final int NIMAGES = 2;

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		if (spawnImages(hero, NIMAGES) > 0) {
			if (hero == Multiplayer.localHero()) {
				GLog.i(Messages.get(this, "copies"));
			}
		} else {
			if (hero == Multiplayer.localHero()) {
				GLog.i(Messages.get(this, "no_copies"));
			}
		}
		identify(true);

		AudioWrapper.playGlobal(Assets.Sounds.READ);

		readAnimation(hero);
	}

	public static int spawnImages(Hero hero, int nImages) {
		return spawnImages(hero, hero.pos, nImages);
	}

	public static int spawnImages(Hero hero, int pos, int nImages) {
		ArrayList<Integer> respawnPoints = new ArrayList<>();

		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS9[i];
			if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
				respawnPoints.add(p);
			}
		}

		int spawned = 0;
		while (nImages > 0 && !respawnPoints.isEmpty()) {
			int index = Random.index(respawnPoints);

			MirrorImage mob = new MirrorImage();
			mob.duplicate(hero);
			GameScene.add(mob);
			ScrollOfTeleportation.appear(mob, respawnPoints.get(index));

			respawnPoints.remove(index);
			nImages--;
			spawned++;
		}

		return spawned;
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}