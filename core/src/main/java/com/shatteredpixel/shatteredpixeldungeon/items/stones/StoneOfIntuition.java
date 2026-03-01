package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Identification;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

import network.Multiplayer;

public class StoneOfIntuition extends InventoryStone {
	
	{
		setImage(ItemSpriteSheet.STONE_INTUITION);
	}

	@Override
	protected boolean usableOnItem(Item item) {
		if (item instanceof Ring){
			return !((Ring) item).isKnown();
		} else if (item instanceof Potion){
			return !((Potion) item).isKnown();
		} else if (item instanceof Scroll){
			return !((Scroll) item).isKnown();
		}
		return false;
	}
	
	@Override
	protected void onItemSelected(Item item) {
		GameScene.show( new WndGuess(item));
	}

	@Override
	public String desc() {
		String text = super.desc();
		Hero local = Multiplayer.localHero();
		if (local != null){
			if (local.buff(IntuitionUseTracker.class) == null){
				text += "\n\n" + Messages.get(this, "break_info");
			} else {
				text += "\n\n" + Messages.get(this, "break_warn");
			}
		}
		return text;
	}

	public static class IntuitionUseTracker extends Buff {{ revivePersists = true; }}

    public class WndGuess extends Window {
		
		private static final int WIDTH = 120;
		private static final int BTN_SIZE = 20;

		private Class<?extends Item> guessedClass = null; // текущий выбор в этом окне
		
		public WndGuess(final Item item){
			
			IconTitle titlebar = new IconTitle();
			titlebar.icon( new ItemSprite(item) );
			titlebar.label( Messages.titleCase(item.name()) );
			titlebar.setRect( 0, 0, WIDTH, 0 );
			add( titlebar );
			
			RenderedTextBlock text = PixelScene.renderTextBlock(6);
			text.text( Messages.get(this, "text") );
			text.setPos(0, titlebar.bottom()+2);
			text.maxWidth( WIDTH );
			add(text);
			
			final RedButton guess = new RedButton(""){
				@Override
				protected void onClick() {
					super.onClick();
					useAnimation();
					if (guessedClass != null && item.getClass() == guessedClass){
						if (item instanceof Ring){
							((Ring) item).setKnown();
							Item.updateQuickslot();
						} else {
							item.identify();
						}
						GLog.p( Messages.get(WndGuess.class, "correct") );
						curUser.sprite.parent.add( new Identification( curUser.sprite.center().offset( 0, -16 ) ) );
					} else {
						GLog.w( Messages.get(WndGuess.class, "incorrect") );
					}
					if (!anonymous) {
						// локальные достижения и таланты
						if (curUser == Multiplayer.localHero()) {
							Catalog.countUse(StoneOfIntuition.class);
						}
						if (curUser.buff(IntuitionUseTracker.class) == null) {
							Buff.affect(curUser, IntuitionUseTracker.class, curUser);
						} else {
							curItem.detach(curUser.belongings.backpack);
							curUser.buff(IntuitionUseTracker.class).detach();
						}
						if (curUser == Multiplayer.localHero()) {
							Talent.onRunestoneUsed(curUser, curUser.pos, StoneOfIntuition.class);
						}
					}
					guessedClass = null;
					hide();
				}
			};
			guess.visible = false;
			guess.icon( new ItemSprite(item) );
			guess.enable(false);
			guess.setRect(0, 80, WIDTH, 20);
			add(guess);
			
			float left;
			float top = text.bottom() + 5;
			int rows;
			int placed = 0;
			
			final ArrayList<Class<?extends Item>> unIDed = new ArrayList<>();
			if (item.isIdentified()){
				hide();
				return;
			} else if (item instanceof Potion){
				if (item instanceof ExoticPotion) {
					for (Class<?extends Item> i : Potion.getUnknown()){
						unIDed.add(ExoticPotion.regToExo.get(i));
					}
				} else {
					unIDed.addAll(Potion.getUnknown());
				}
			} else if (item instanceof Scroll){
				if (item instanceof ExoticScroll) {
					for (Class<?extends Item> i : Scroll.getUnknown()){
						unIDed.add(ExoticScroll.regToExo.get(i));
					}
				} else {
					unIDed.addAll(Scroll.getUnknown());
				}
			} else if (item instanceof Ring) {
				unIDed.addAll(Ring.getUnknown());
			} else {
				hide();
				return;
			}
			
			if (unIDed.size() <= 5){
				rows = 1;
				top += BTN_SIZE/2f;
				left = (WIDTH - BTN_SIZE*unIDed.size())/2f;
			} else {
				rows = 2;
				left = (WIDTH - BTN_SIZE*((unIDed.size()+1)/2))/2f;
			}
			
			for (final Class<?extends Item> i : unIDed){

				IconButton btn = new IconButton(){
					@Override
					protected void onClick() {
						guessedClass = i;
						guess.visible = true;
						guess.text( Messages.titleCase(Messages.get(guessedClass, "name")) );
						guess.enable(true);
						super.onClick();
					}
				};
				Image im = new Image(Assets.Sprites.ITEM_ICONS);
				im.frame(ItemSpriteSheet.Icons.film.get(Reflection.newInstance(i).getIcon()));
				im.scale.set(2f);
				btn.icon(im);
				btn.setRect(left + placed*BTN_SIZE, top, BTN_SIZE, BTN_SIZE);
				add(btn);
				
				placed++;
				if (rows == 2 && placed == ((unIDed.size()+1)/2)){
					placed = 0;
					if (unIDed.size() % 2 == 1){
						left += BTN_SIZE/2f;
					}
					top += BTN_SIZE;
				}
			}
			
			resize(WIDTH, 100);
			
		}

	}
}