// ScrollOfRecharging.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfRecharging extends Scroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_RECHARGE);
	}

	@Override
	public void doRead(Hero hero) {
		detach(hero.belongings.backpack);
		Buff.affect(hero, Recharging.class, Recharging.DURATION);
		charge(hero);

		AudioWrapper.playGlobal(Assets.Sounds.READ);
		AudioWrapper.playGlobal(Assets.Sounds.CHARGEUP);

		if (hero == Multiplayer.localHero()) {
			GLog.i(Messages.get(this, "surge"));
		}
		SpellSprite.show(hero, SpellSprite.CHARGE);
		identify(true);
		readAnimation(hero);
	}

	public static void charge(Char user) {
		if (user.sprite != null) {
			Emitter e = user.sprite.centerEmitter();
			if (e != null) e.burst(EnergyParticle.FACTORY, 15);
		}
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}