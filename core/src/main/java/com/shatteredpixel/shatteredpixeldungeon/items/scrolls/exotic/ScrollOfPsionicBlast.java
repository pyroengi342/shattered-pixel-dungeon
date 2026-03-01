// ScrollOfPsionicBlast.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

import network.Multiplayer;

public class ScrollOfPsionicBlast extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_PSIBLAST);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);

		if (hero == Multiplayer.localHero()) {
			GameScene.flash(0x80FFFFFF);
			GLog.i(Messages.get(ScrollOfRetribution.class, "blast"));
		}

		Sample.INSTANCE.play(Assets.Sounds.BLAST);

		ArrayList<Mob> targets = new ArrayList<>();

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (hero.fieldOfView[mob.pos]) {
				targets.add(mob);
			}
		}

		for (Mob mob : targets) {
			mob.damage(Math.round(mob.HT / 2f + mob.HP / 2f), this);
			if (mob.isAlive()) {
				Buff.prolong(mob, Blindness.class, Blindness.DURATION, null);
			}
		}

		hero.damage(Math.max(0, Math.round(hero.HT * (0.5f * (float) Math.pow(0.9, targets.size())))), this);
		if (hero.isAlive()) {
			Buff.prolong(hero, Blindness.class, Blindness.DURATION, null);
			Buff.prolong(hero, Weakness.class, Weakness.DURATION * 5f, null);
			Dungeon.observe(hero);
			readAnimation(hero);
		} else {
			if (hero == Multiplayer.localHero()) {
				Badges.validateDeathFromFriendlyMagic();
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath"));
			}
		}

		identify(true);
	}
}