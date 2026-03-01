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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MimicTooth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MimicSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import network.Multiplayer;

public class GoldenMimic extends Mimic {

	{
		spriteClass = MimicSprite.Golden.class;
	}

	@Override
	public String name() {
		if (alignment == Alignment.NEUTRAL){
			return Messages.get(Heap.class, "locked_chest");
		} else {
			return super.name();
		}
	}

	@Override
	public String description() {
		if (alignment == Alignment.NEUTRAL){
			if (MimicTooth.stealthyMimics()){
				return Messages.get(Heap.class, "locked_chest_desc");
			} else {
				return Messages.get(Heap.class, "locked_chest_desc") + "\n\n" + Messages.get(this, "hidden_hint");
			}
		} else {
			return super.description();
		}
	}

	public void stopHiding(){
		state = HUNTING;
		if (sprite != null) sprite.idle();

		// Находим ближайшего героя, который видит мимика или находится рядом
		Hero nearest = Multiplayer.findNearestHero(pos);
		if (nearest != null) {
			enemy = nearest;
			target = nearest.pos;
		} else {
			// Если никого нет, сбрасываем цель
			enemy = null;
			target = -1;
		}

		// Локальные эффекты показываем только если локальный герой видит мимика
		Hero local = Multiplayer.localHero();
		if (local != null && local.fieldOfView != null && local.fieldOfView[pos]) {
			GLog.w(Messages.get(this, "reveal"));
			CellEmitter.get(pos).burst(Speck.factory(Speck.STAR), 10);
			Sample.INSTANCE.play(Assets.Sounds.MIMIC, 1, 0.85f);
		}
	}

	@Override
	public void setLevel(int level) {
		super.setLevel(Math.round(level*1.33f));
	}

	@Override
	protected void generatePrize( boolean useDecks ) {
		super.generatePrize( useDecks );
		//all existing prize items are guaranteed uncursed, and have a 50% chance to be +1 if they were +0
		for (Item i : items){
			if (i instanceof EquipableItem || i instanceof Wand){
				i.cursed = false;
				i.cursedKnown = true;
				if (i instanceof Weapon && ((Weapon) i).hasCurseEnchant()){
					((Weapon) i).enchant(null);
				}
				if (i instanceof Armor && ((Armor) i).hasCurseGlyph()){
					((Armor) i).inscribe(null);
				}
				if (!(i instanceof Artifact) && i.level() == 0 && Random.Int(2) == 0){
					i.upgrade();
				}
			}
		}
	}
}
