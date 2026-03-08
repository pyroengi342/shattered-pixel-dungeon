// ScrollOfSirensSong.java
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfSirensSong extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_SIREN);
	}

	protected boolean identifiedByUse = false;

	@Override
	public void doRead(Hero hero) {
		if (!isKnown()) {
			identify(true);
			Item detached = detach( hero.belongings.backpack);
			if (detached == null) return;
			identifiedByUse = true;
		} else {
			identifiedByUse = false;
		}
		GameScene.selectCell(new SirenTargeter(this, hero));
	}

	private class SirenTargeter extends CellSelector.Listener {
		private final ScrollOfSirensSong scroll;
		private final Hero hero;

		SirenTargeter(ScrollOfSirensSong scroll, Hero hero) {
			this.scroll = scroll;
			this.hero = hero;
		}

		@Override
		public void onSelect(Integer cell) {
			if (cell == null && isKnown()) {
				return;
			}

			Mob target = null;
			if (cell != null) {
				Char ch = Actor.findChar(cell);
				if (ch != null && ch.alignment != Char.Alignment.ALLY && ch instanceof Mob) {
					target = (Mob) ch;
				}
			}

			if (target == null && !anonymous && !scroll.identifiedByUse) {
				if (hero == Multiplayer.localHero()) {
					GLog.w(Messages.get(ScrollOfSirensSong.class, "cancel"));
				}
			} else {

				hero.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
				AudioWrapper.play(Assets.Sounds.CHARMS, hero.pos);
				Sample.INSTANCE.playDelayed(Assets.Sounds.LULLABY, 0.1f);

				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
					if (hero.fieldOfView[mob.pos] && mob != target && mob.alignment != Char.Alignment.ALLY) {
						Buff.affect(mob, Charm.class, Charm.DURATION, scroll).object = hero.id();
						if (hero == Multiplayer.localHero()) {
							mob.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
						}
					}
				}

				if (target != null) {
					if (!target.isImmune(Enthralled.class)) {
						AllyBuff.affectAndLoot(target, hero, Enthralled.class);
					} else {
						Buff.affect(target, Charm.class, Charm.DURATION, scroll).object = hero.id();
					}
					if (hero == Multiplayer.localHero()) {
						target.sprite.centerEmitter().burst(Speck.factory(Speck.HEART), 10);
					}
				} else {
					if (hero == Multiplayer.localHero()) {
						GLog.w(Messages.get(ScrollOfSirensSong.class, "no_target"));
					}
				}

				if (!scroll.identifiedByUse) {
					scroll.detach( hero.belongings.backpack);
				}
				scroll.identifiedByUse = false;

				readAnimation(hero);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(ScrollOfSirensSong.class, "prompt");
		}
	}

	public static class Enthralled extends AllyBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.HEARTS);
			else target.sprite.remove(CharSprite.State.HEARTS);
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}
	}
}