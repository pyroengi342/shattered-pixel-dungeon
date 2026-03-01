/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FetidRatSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollTricksterSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GreatCrabSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import network.Multiplayer;
import network.handlers.window.GhostRewardHandler;

public class WndSadGhost extends Window {
	public static void processReward(Item reward, Hero hero) {
		if (reward == null) return;

		// Применяем зачарование/глиф, если они есть
		if (reward instanceof Weapon && Ghost.Quest.enchant != null) {
			((Weapon) reward).enchant(Ghost.Quest.enchant);
		} else if (reward instanceof Armor && Ghost.Quest.glyph != null) {
			((Armor) reward).inscribe(Ghost.Quest.glyph);
		}

		reward.identify(false);
		// Пытаемся добавить предмет герою
		if (reward.doPickUp(hero)) {
			if (hero == Multiplayer.localHero()) {
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(
						com.shatteredpixel.shatteredpixeldungeon.messages.Messages.capitalize(
								com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(hero, "you_now_have", reward.name())
						)
				);
			}
		} else {
			// Если не влезает, бросаем на позицию призрака
			Ghost ghost = findGhost();
			if (ghost != null) {
				Dungeon.level.drop(reward, ghost.pos).sprite.drop();
			}
		}

		// Призрак прощается и исчезает
		Ghost ghost = findGhost();
		if (ghost != null) {
			ghost.yell(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(WndSadGhost.class, "farewell"));
			ghost.die(null);
		}

		// Завершаем квест
		Ghost.Quest.complete();
	}

	// Вспомогательный метод для поиска призрака на уровне
	private static Ghost findGhost() {
		for (com.shatteredpixel.shatteredpixeldungeon.actors.Char ch : com.shatteredpixel.shatteredpixeldungeon.actors.Actor.chars()) {
			if (ch instanceof Ghost) {
				return (Ghost) ch;
			}
		}
		return null;
	}
	private static final int WIDTH		= 120;
	private static final int BTN_SIZE	= 32;
	private static final int BTN_GAP	= 5;
	private static final int GAP		= 2;

	Ghost ghost;
	
	public WndSadGhost( final Ghost ghost, final int type ) {
		
		super();

		this.ghost = ghost;
		
		IconTitle titlebar = new IconTitle();
		RenderedTextBlock message;
		switch (type){
			case 1:default:
				titlebar.icon( new FetidRatSprite() );
				titlebar.label( Messages.get(this, "rat_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "rat")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;
			case 2:
				titlebar.icon( new GnollTricksterSprite() );
				titlebar.label( Messages.get(this, "gnoll_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "gnoll")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;
			case 3:
				titlebar.icon( new GreatCrabSprite());
				titlebar.label( Messages.get(this, "crab_title") );
				message = PixelScene.renderTextBlock( Messages.get(this, "crab")+"\n\n"+Messages.get(this, "give_item"), 6 );
				break;

		}

		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		message.maxWidth(WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );

		ItemButton btnWeapon = new ItemButton(){
			@Override
			protected void onClick() {
				GameScene.show(new RewardWindow(item()));
			}
		};
		btnWeapon.item( Ghost.Quest.weapon );
		btnWeapon.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
		add( btnWeapon );

		ItemButton btnArmor = new ItemButton(){
			@Override
			protected void onClick() {
				GameScene.show(new RewardWindow(item()));
			}
		};
		btnArmor.item( Ghost.Quest.armor );
		btnArmor.setRect( btnWeapon.right() + BTN_GAP, btnWeapon.top(), BTN_SIZE, BTN_SIZE );
		add(btnArmor);

		resize(WIDTH, (int) btnArmor.bottom() + BTN_GAP);
	}
	
	private void selectReward( Item reward ) {
		
		hide();
		
		if (reward == null) return;

		if (reward instanceof Weapon && Ghost.Quest.enchant != null){
			((Weapon) reward).enchant(Ghost.Quest.enchant);
		} else if (reward instanceof Armor && Ghost.Quest.glyph != null){
			((Armor) reward).inscribe(Ghost.Quest.glyph);
		}
		
		reward.identify(false);
		if (reward.doPickUp( Multiplayer.localHero() )) {
			GLog.i( Messages.capitalize(Messages.get(Multiplayer.localHero(), "you_now_have", reward.name())) );
		} else {
			Dungeon.level.drop( reward, ghost.pos ).sprite.drop();
		}
		
		ghost.yell( Messages.get(this, "farewell") );
		ghost.die( null );
		
		Ghost.Quest.complete();
	}

	private class RewardWindow extends WndInfoItem {

		public RewardWindow( Item item ) {
			super(item);

			RedButton btnConfirm = new RedButton(Messages.get(WndSadGhost.class, "confirm")){
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
					WndSadGhost.this.selectReward(item); // локальное выполнение
					if (network.Multiplayer.isMultiplayer) {
						int choice = (item == Ghost.Quest.weapon) ? 0 : 1;
						GhostRewardHandler.send(choice);
					}
				}
			};
			btnConfirm.setRect(0, height+2, width/2-1, 16);
			add(btnConfirm);

			RedButton btnCancel = new RedButton(Messages.get(WndSadGhost.class, "cancel")){
				@Override
				protected void onClick() {
					RewardWindow.this.hide();
				}
			};
			btnCancel.setRect(btnConfirm.right()+2, height+2, btnConfirm.width(), 16);
			add(btnCancel);

			resize(width, (int)btnCancel.bottom());
		}
	}
}