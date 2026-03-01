package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;

public abstract class InventoryScroll extends Scroll {

	protected boolean identifiedByUse = false;

	@Override
	public void doRead(Hero hero) {
		if (!isKnown()) {
			identify(true);
			Item detached = detach(hero.belongings.backpack);
			if (detached == null) return;
			identifiedByUse = true;
		} else {
			identifiedByUse = false;
		}

		GameScene.selectItem(new InventoryItemSelector(this, hero));
	}

	protected class InventoryItemSelector extends WndBag.ItemSelector {
		private final InventoryScroll scroll;
		private final Hero hero;

		InventoryItemSelector(InventoryScroll scroll, Hero hero) {
			this.scroll = scroll;
			this.hero = hero;
		}

		@Override
		public String textPrompt() {
			return scroll.inventoryTitle();
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return scroll.preferredBag;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return scroll.usableOnItem(item, hero);
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				boolean isUpgrade = scroll instanceof ScrollOfUpgrade;
				if (!isUpgrade && !scroll.identifiedByUse) {
					Item detached = scroll.detach(hero.belongings.backpack);
					if (detached == null) return;
				}
				scroll.onItemSelected(item, hero);

				if (!isUpgrade) {
					scroll.readAnimation(hero);
					Sample.INSTANCE.play(Assets.Sounds.READ);
				}
			} else if (scroll.identifiedByUse && !scroll.anonymous) {
				scroll.confirmCancelation(hero);
			} else if (scroll.anonymous) {
				hero.spendAndNext(TIME_TO_READ);
			}
		}
	}

	private void confirmCancelation(Hero hero) {
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				Messages.get(this, "warning"),
				Messages.get(this, "yes"),
				Messages.get(this, "no")) {
			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0:
						hero.spendAndNext(TIME_TO_READ);
						identifiedByUse = false;
						break;
					case 1:
						GameScene.selectItem(new InventoryItemSelector(InventoryScroll.this, hero));
						break;
				}
			}
			@Override
			public void onBackPressed() {}
		});
	}

	protected String inventoryTitle() {
		return Messages.get(this, "inv_title");
	}

	protected Class<? extends Bag> preferredBag = null;

	protected boolean usableOnItem(Item item, Hero hero) {
		return true;
	}

	protected abstract void onItemSelected(Item item, Hero hero);
}