package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import network.Multiplayer;

public abstract class InventorySpell extends Spell {

	@Override
	protected void onCast(Hero hero) {
		GameScene.selectItem(new InventorySpellSelector(this, hero));
	}

	protected String inventoryTitle() {
		return Messages.get(this, "inv_title");
	}

	protected Class<? extends Bag> preferredBag() {
		return null;
	}

	protected boolean usableOnItem(Item item) {
		return true;
	}

	protected abstract void onItemSelected(Item item, Hero hero);

	protected class InventorySpellSelector extends WndBag.ItemSelector {
		private final InventorySpell spell;
		private final Hero hero;

		InventorySpellSelector(InventorySpell spell, Hero hero) {
			this.spell = spell;
			this.hero = hero;
		}

		@Override
		public String textPrompt() {
			return spell.inventoryTitle();
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return spell.preferredBag();
		}

		@Override
		public boolean itemSelectable(Item item) {
			return spell.usableOnItem(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;

			boolean isInfusion = spell instanceof MagicalInfusion;
			Item usedItem = spell;

			if (!isInfusion) {
				usedItem = spell.detach(hero.belongings.backpack);
				if (usedItem == null) return;
			}

			spell.onItemSelected(item, hero);

			if (!isInfusion) {
				hero.spend(1f);
				hero.busy();
				hero.sprite.operate(hero.pos);

				Sample.INSTANCE.play(Assets.Sounds.READ);
				Invisibility.dispel(hero);

				if (hero == Multiplayer.localHero()) {
					Catalog.countUse(spell.getClass());
					if (Random.Float() < spell.talentChance) {
						Talent.onScrollUsed(hero, hero.pos, spell.talentFactor, spell.getClass());
					}
				}
			}
		}
	}
}