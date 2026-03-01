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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Bundle;

public class LifeLinkSpell extends ClericSpell {

	public static LifeLinkSpell INSTANCE = new LifeLinkSpell();

	@Override
	public int icon() {
		return HeroIcon.LIFE_LINK;
	}

	@Override
public String desc(Hero hero){
		return Messages.get(this, "desc", 4 + 2*hero.pointsInTalent(Talent.LIFE_LINK), 30 + 5*hero.pointsInTalent(Talent.LIFE_LINK)) + "\n\n" + Messages.get(this, "charge_cost", (int)chargeUse(hero));
	}

	@Override
	public boolean canCast(Hero hero) {
		return super.canCast(hero)
				&& hero.hasTalent(Talent.LIFE_LINK)
				&& (PowerOfMany.getPoweredAlly() != null || Stasis.getStasisAlly(hero) != null);
	}

	@Override
	public float chargeUse(Hero hero) {
		return 2;
	}

	@Override
	public void onCast(HolyTome tome, Hero hero) {
		int duration = Math.round(6.67f + 3.33f * hero.pointsInTalent(Talent.LIFE_LINK));

		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null) {
			hero.sprite.zap(ally.pos);
			hero.sprite.parent.add(new Beam.HealthRay(hero.sprite.center(), ally.sprite.center()));
			Buff.prolong(hero, LifeLink.class, duration, hero).object = ally.id();
		} else {
			ally = Stasis.getStasisAlly(hero);
			hero.sprite.operate(hero.pos);
			hero.sprite.parent.add(new Beam.HealthRay(DungeonTilemap.tileCenterToWorld(hero.pos), hero.sprite.center()));
		}

		// Создаём бафф на союзнике
		LifeLinkSpellBuff buff = Buff.prolong(ally, LifeLinkSpellBuff.class, duration, hero);
		buff.setOwner(hero);
		buff.setTotalDuration(duration);   // <-- сохраняем полную длительность

		if (ally == Stasis.getStasisAlly(hero)) {
			ally.buff(LifeLink.class).clearTime();
			buff.clearTime();
		}

		hero.spendAndNext(Actor.TICK);
		onSpellCast(tome, hero);
	}

	public static class LifeLinkSpellBuff extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}
		private int ownerID;
		private float totalDuration;   // <-- новое поле

		public void setOwner(Hero owner) { this.ownerID = owner.id(); }
		public Hero getOwner() { return (Hero) Actor.findById(ownerID); }
		public void setTotalDuration(float d) { this.totalDuration = d; }

		@Override
		public int icon() {
			return BuffIndicator.HOLY_ARMOR;
		}

		@Override
		public float iconFadePercent() {
			if (totalDuration == 0) return 0;
			return Math.max(0, (totalDuration - visualcooldown()) / totalDuration);
		}

		// при сохранении/загрузке также сохраняем totalDuration
		private static final String TOTAL_DURATION = "total_duration";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TOTAL_DURATION, totalDuration);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			totalDuration = bundle.getFloat(TOTAL_DURATION);
		}
	}
}
