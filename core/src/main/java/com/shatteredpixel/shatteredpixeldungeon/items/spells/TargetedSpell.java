package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import network.Multiplayer;

public abstract class TargetedSpell extends Spell {

	protected int collisionProperties = Ballistica.PROJECTILE;

	@Override
	protected void onCast(Hero hero) {
		GameScene.selectCell(new SpellTargeter(this, hero));
	}

	protected abstract void affectTarget( Ballistica bolt, Hero hero );

	protected void fx( Hero hero, Ballistica bolt, Callback callback ) {
		MagicMissile.boltFromChar( hero.sprite.parent,
				MagicMissile.MAGIC_MISSILE,
				hero.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}

	protected void onSpellUsed(Hero hero){
		detach( hero.belongings.backpack );
		Invisibility.dispel(hero);
		updateQuickslot();
		hero.spendAndNext( timeToCast() );
		if (hero == Multiplayer.localHero()) {
			Catalog.countUse(getClass());
			if (Random.Float() < talentChance)
				Talent.onScrollUsed(hero, hero.pos, talentFactor, getClass());
		}
	}

	protected float timeToCast(){
		return Actor.TICK;
	}

	private class SpellTargeter extends CellSelector.Listener {
		private final TargetedSpell spell;
		private final Hero hero;

		SpellTargeter(TargetedSpell spell, Hero hero) {
			this.spell = spell;
			this.hero = hero;
		}

		@Override
		public void onSelect(Integer target) {
			if (target == null) return;

			final Ballistica shot = new Ballistica( hero.pos, target, spell.collisionProperties);
			int cell = shot.collisionPos;

			hero.sprite.zap(cell);

			if (Actor.findChar(target) != null)
				QuickSlotButton.target(Actor.findChar(target));
			else
				QuickSlotButton.target(Actor.findChar(cell));

			hero.busy();

			spell.fx(hero, shot, new Callback() {
				public void call() {
					spell.affectTarget(shot, hero);
					spell.onSpellUsed(hero);
				}
			});
		}

		@Override
		public String prompt() {
			return Messages.get(TargetedSpell.class, "prompt");
		}
	}
}