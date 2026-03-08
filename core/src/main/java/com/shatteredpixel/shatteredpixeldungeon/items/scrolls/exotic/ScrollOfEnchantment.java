// ScrollOfEnchantment.java (исправленный)
package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;

import network.utils.AudioWrapper;
import network.Multiplayer;

public class ScrollOfEnchantment extends ExoticScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_ENCHANT);
		unique = true;
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
		GameScene.selectItem(new EnchantmentItemSelector(this, hero));
	}

	public static boolean enchantable(Item item) {
		return (item instanceof Weapon || item instanceof Armor)
				&& (item.isUpgradable() || item instanceof SpiritBow);
	}

	private void confirmCancelation(Hero hero) {
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				Messages.get(InventoryScroll.class, "warning"),
				Messages.get(InventoryScroll.class, "yes"),
				Messages.get(InventoryScroll.class, "no")) {
			@Override
			protected void onSelect(int index) {
				switch (index) {
					case 0:
						hero.spendAndNext(TIME_TO_READ);
						identifiedByUse = false;
						break;
					case 1:
						GameScene.selectItem(new EnchantmentItemSelector(ScrollOfEnchantment.this, hero));
						break;
				}
			}
			@Override
			public void onBackPressed() {}
		});
	}

	private class EnchantmentItemSelector extends WndBag.ItemSelector {
		private final ScrollOfEnchantment scroll;
		private final Hero hero;

		EnchantmentItemSelector(ScrollOfEnchantment scroll, Hero hero) {
			this.scroll = scroll;
			this.hero = hero;
		}

		@Override
		public String textPrompt() {
			return Messages.get(ScrollOfEnchantment.class, "inv_title");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return enchantable(item);
		}

		@Override
		public void onSelect(final Item item) {
			if (item instanceof Weapon) {
				if (!scroll.identifiedByUse) {
					scroll.detach( hero.belongings.backpack);
				}
				scroll.identifiedByUse = false;

				final Weapon.Enchantment[] enchants = new Weapon.Enchantment[3];
				Class<? extends Weapon.Enchantment> existing = ((Weapon) item).enchantment != null ? ((Weapon) item).enchantment.getClass() : null;
				enchants[0] = Weapon.Enchantment.randomCommon(existing);
				enchants[1] = Weapon.Enchantment.randomUncommon(existing);
				enchants[2] = Weapon.Enchantment.random(existing, enchants[0].getClass(), enchants[1].getClass());

				GameScene.show(new WndEnchantSelect(scroll, hero, (Weapon) item, enchants[0], enchants[1], enchants[2]));

			} else if (item instanceof Armor) {
				if (!scroll.identifiedByUse) {
					scroll.detach( hero.belongings.backpack);
				}
				scroll.identifiedByUse = false;

				final Armor.Glyph[] glyphs = new Armor.Glyph[3];
				Class<? extends Armor.Glyph> existing = ((Armor) item).glyph != null ? ((Armor) item).glyph.getClass() : null;
				glyphs[0] = Armor.Glyph.randomCommon(existing);
				glyphs[1] = Armor.Glyph.randomUncommon(existing);
				glyphs[2] = Armor.Glyph.random(existing, glyphs[0].getClass(), glyphs[1].getClass());

				GameScene.show(new WndGlyphSelect(scroll, hero, (Armor) item, glyphs[0], glyphs[1], glyphs[2]));
			} else if (scroll.identifiedByUse) {
				scroll.confirmCancelation(hero);
			}
		}
	}

	public class WndEnchantSelect extends WndOptions {
		private final ScrollOfEnchantment scroll;
		private final Hero hero;
		private final Weapon wep;
		private final Weapon.Enchantment[] enchantments;

		public WndEnchantSelect(ScrollOfEnchantment scroll, Hero hero, Weapon wep,
								Weapon.Enchantment ench1,
								Weapon.Enchantment ench2,
								Weapon.Enchantment ench3) {
			super(new ItemSprite(scroll),
					Messages.titleCase(scroll.name()),
					Messages.get(ScrollOfEnchantment.class, "weapon"),
					ench1.name(),
					ench2.name(),
					ench3.name(),
					Messages.get(ScrollOfEnchantment.class, "cancel"));
			this.scroll = scroll;
			this.hero = hero;
			this.wep = wep;
			enchantments = new Weapon.Enchantment[3];
			enchantments[0] = ench1;
			enchantments[1] = ench2;
			enchantments[2] = ench3;
		}

		@Override
		protected void onSelect(int index) {
			if (index < 3) {
				wep.enchant(enchantments[index]);
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(StoneOfEnchantment.class, "weapon"));
				}
				scroll.readAnimation(hero);
				AudioWrapper.play(Assets.Sounds.READ, hero.pos);
				Enchanting.show(hero, wep);
			} else {
				GameScene.show(new WndConfirmCancel(scroll, hero, this));
			}
		}

		@Override
		protected boolean hasInfo(int index) {
			return index < 3;
		}

		@Override
		protected void onInfo(int index) {
			GameScene.show(new WndTitledMessage(
					Icons.get(Icons.INFO),
					Messages.titleCase(enchantments[index].name()),
					enchantments[index].desc()));
		}

		@Override
		public void onBackPressed() {
			// do nothing, reader has to cancel
		}
	}

	public class WndGlyphSelect extends WndOptions {
		private final ScrollOfEnchantment scroll;
		private final Hero hero;
		private final Armor arm;
		private final Armor.Glyph[] glyphs;

		public WndGlyphSelect(ScrollOfEnchantment scroll, Hero hero, Armor arm,
							  Armor.Glyph glyph1,
							  Armor.Glyph glyph2,
							  Armor.Glyph glyph3) {
			super(new ItemSprite(scroll),
					Messages.titleCase(scroll.name()),
					Messages.get(ScrollOfEnchantment.class, "armor"),
					glyph1.name(),
					glyph2.name(),
					glyph3.name(),
					Messages.get(ScrollOfEnchantment.class, "cancel"));
			this.scroll = scroll;
			this.hero = hero;
			this.arm = arm;
			glyphs = new Armor.Glyph[3];
			glyphs[0] = glyph1;
			glyphs[1] = glyph2;
			glyphs[2] = glyph3;
		}

		@Override
		protected void onSelect(int index) {
			if (index < 3) {
				arm.inscribe(glyphs[index]);
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(StoneOfEnchantment.class, "armor"));
				}
				scroll.readAnimation(hero);
				AudioWrapper.play(Assets.Sounds.READ, hero.pos);
				Enchanting.show(hero, arm);
			} else {
				GameScene.show(new WndConfirmCancel(scroll, hero, this));
			}
		}

		@Override
		protected boolean hasInfo(int index) {
			return index < 3;
		}

		@Override
		protected void onInfo(int index) {
			GameScene.show(new WndTitledMessage(
					Icons.get(Icons.INFO),
					Messages.titleCase(glyphs[index].name()),
					glyphs[index].desc()));
		}

		@Override
		public void onBackPressed() {
			// do nothing
		}
	}

	public class WndConfirmCancel extends WndOptions {
		private final ScrollOfEnchantment scroll;
		private final Hero hero;
		private final Window previousWindow;

		public WndConfirmCancel(ScrollOfEnchantment scroll, Hero hero, Window previousWindow) {
			super(new ItemSprite(scroll),
					Messages.titleCase(scroll.name()),
					Messages.get(ScrollOfEnchantment.class, "cancel_warn"),
					Messages.get(ScrollOfEnchantment.class, "cancel_warn_yes"),
					Messages.get(ScrollOfEnchantment.class, "cancel_warn_no"));
			this.scroll = scroll;
			this.hero = hero;
			this.previousWindow = previousWindow;
		}

		@Override
		protected void onSelect(int index) {
			if (index == 1) {
				GameScene.show(previousWindow);
			} else {
				// cancel, do nothing else
			}
		}

		@Override
		public void onBackPressed() {
			// do nothing
		}
	}
}