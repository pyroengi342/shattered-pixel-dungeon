// ScrollOfMetamorphosis.java (исправленный)
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Transmuting;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Random;

import network.AudioWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

public class ScrollOfMetamorphosis extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_METAMORPH);
		talentFactor = 2f;
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
		GameScene.show(new WndMetamorphChoose(this, hero));
	}

	public void onMetamorph(Hero hero, Talent oldTalent, Talent newTalent) {
		readAnimation(hero);
		AudioWrapper.play(Assets.Sounds.READ, hero.pos);
		hero.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10);
		Transmuting.show(hero, oldTalent, newTalent);

		if (hero.hasTalent(newTalent)) {
			Talent.onTalentUpgraded(hero, newTalent);
		}
	}

	private void confirmCancelation(Window chooseWindow, Hero hero, boolean byID) {
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				byID ? Messages.get(InventoryScroll.class, "warning") : Messages.get(ScrollOfMetamorphosis.class, "cancel_warn"),
				Messages.get(InventoryScroll.class, "yes"),
				Messages.get(InventoryScroll.class, "no")) {
			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0:
						hero.spendAndNext(TIME_TO_READ);
						identifiedByUse = false;
						chooseWindow.hide();
						break;
					case 1:
						// do nothing
						break;
				}
			}
			@Override
			public void onBackPressed() {}
		});
	}

	public static class WndMetamorphChoose extends Window {
		private final ScrollOfMetamorphosis scroll;
		private final Hero hero;
		private TalentsPane pane;

		public WndMetamorphChoose(ScrollOfMetamorphosis scroll, Hero hero) {
			this.scroll = scroll;
			this.hero = hero;

			float top = 0;

			IconTitle title = new IconTitle(scroll);
			title.color(TITLE_COLOR);
			title.setRect(0, 0, 120, 0);
			add(title);

			top = title.bottom() + 2;

			RenderedTextBlock text = PixelScene.renderTextBlock(Messages.get(ScrollOfMetamorphosis.class, "choose_desc"), 6);
			text.maxWidth(120);
			text.setPos(0, top);
			add(text);

			top = text.bottom() + 2;

			ArrayList<LinkedHashMap<Talent, Integer>> talents = new ArrayList<>();
			Talent.initClassTalents(hero.heroClass, talents, hero.metamorphedTalents);

			for (LinkedHashMap<Talent, Integer> tier : talents) {
				for (Talent talent : tier.keySet()) {
					tier.put(talent, hero.pointsInTalent(talent));
				}
			}

			pane = new TalentsPane(TalentButton.Mode.METAMORPH_CHOOSE, talents);
			add(pane);
			pane.setPos(0, top);
			pane.setSize(120, pane.content().height());
			resize((int) pane.width(), (int) pane.bottom());
			pane.setPos(0, top);
		}

		@Override
		public void hide() {
			super.hide();
		}

		@Override
		public void onBackPressed() {
			if (scroll != null && scroll.identifiedByUse) {
				scroll.confirmCancelation(this, hero, true);
			} else {
				super.onBackPressed();
			}
		}

		@Override
		public void offset(int xOffset, int yOffset) {
			super.offset(xOffset, yOffset);
			pane.setPos(pane.left(), pane.top());
		}
	}

	public static  class WndMetamorphReplace extends Window {
		private final ScrollOfMetamorphosis scroll;
		private final Hero hero;
		private final Talent replacing;
		private final int tier;
		private final LinkedHashMap<Talent, Integer> replaceOptions;

		public WndMetamorphReplace(ScrollOfMetamorphosis scroll, Hero hero, Talent replacing, int tier) {
			this.scroll = scroll;
			this.hero = hero;
			this.replacing = replacing;
			this.tier = tier;

			if (!scroll.identifiedByUse) {
				scroll.detach( hero.belongings.backpack);
			}
			scroll.identifiedByUse = false;

			LinkedHashMap<Talent, Integer> options = new LinkedHashMap<>();
			Set<Talent> curTalentsAtTier = hero.talents.get(tier - 1).keySet();

			for (HeroClass cls : HeroClass.values()) {
				ArrayList<LinkedHashMap<Talent, Integer>> clsTalents = new ArrayList<>();
				Talent.initClassTalents(cls, clsTalents);

				Set<Talent> clsTalentsAtTier = clsTalents.get(tier - 1).keySet();
				boolean replacingIsInSet = false;
				for (Talent talent : clsTalentsAtTier.toArray(new Talent[0])) {
					if (talent == replacing) {
						replacingIsInSet = true;
						break;
					} else {
						if (curTalentsAtTier.contains(talent)) {
							clsTalentsAtTier.remove(talent);
						}
					}
				}
				if (!replacingIsInSet && !clsTalentsAtTier.isEmpty()) {
					options.put(Random.element(clsTalentsAtTier), hero.pointsInTalent(replacing));
				}
			}

			replaceOptions = options;
			setup();
		}

		private void setup() {
			float top = 0;

			IconTitle title = new IconTitle(scroll);
			title.color(TITLE_COLOR);
			title.setRect(0, 0, 120, 0);
			add(title);

			top = title.bottom() + 2;

			RenderedTextBlock text = PixelScene.renderTextBlock(Messages.get(ScrollOfMetamorphosis.class, "replace_desc"), 6);
			text.maxWidth(120);
			text.setPos(0, top);
			add(text);

			top = text.bottom() + 2;

			TalentsPane.TalentTierPane optionsPane = new TalentsPane.TalentTierPane(replaceOptions, tier, TalentButton.Mode.METAMORPH_REPLACE);
			add(optionsPane);
			optionsPane.title.text(" ");
			optionsPane.setPos(0, top);
			optionsPane.setSize(120, optionsPane.height());
			resize(120, (int) optionsPane.bottom());
		}

		@Override
		public void hide() {
			super.hide();
		}

		@Override
		public void onBackPressed() {
			scroll.confirmCancelation(this, hero, false);
		}
	}
}