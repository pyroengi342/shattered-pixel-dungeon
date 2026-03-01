package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Identification;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.Multiplayer;

public class ScrollOfIdentify extends InventoryScroll {

	{
		setIcon(ItemSpriteSheet.Icons.SCROLL_IDENTIFY);
		bones = true;
	}

	@Override
	protected boolean usableOnItem(Item item, Hero hero) {
		return !item.isIdentified();
	}

	@Override
	protected void onItemSelected(Item item, Hero hero) {
		if (hero == Multiplayer.localHero()) {
			hero.sprite.parent.add(new Identification(hero.sprite.center().offset(0, -16)));
		}
		IDItem(item, hero);
	}
	public static void IDItem(Item item) {
		IDItem(item, null);
	}
	public static void IDItem(Item item, Hero hero) {
		if (ShardOfOblivion.passiveIDDisabled(hero)) {
			if (item instanceof Weapon) {
				((Weapon) item).setIDReady();
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				}
				return;
			} else if (item instanceof Armor) {
				((Armor) item).setIDReady();
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				}
				return;
			} else if (item instanceof Ring) {
				((Ring) item).setIDReady();
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				}
				return;
			} else if (item instanceof Wand) {
				((Wand) item).setIDReady();
				if (hero == Multiplayer.localHero()) {
					GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				}
				return;
			} else {
				item.identify();
				if (hero != null && hero == Multiplayer.localHero()) {
					GLog.i(Messages.get(ScrollOfIdentify.class, "it_is", item.title()));
					Badges.validateItemLevelAquired(item);
				}
			}
		}

		item.identify();
		if (hero == Multiplayer.localHero()) {
			GLog.i(Messages.get(ScrollOfIdentify.class, "it_is", item.title()));
			Badges.validateItemLevelAquired(item);
		}
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}