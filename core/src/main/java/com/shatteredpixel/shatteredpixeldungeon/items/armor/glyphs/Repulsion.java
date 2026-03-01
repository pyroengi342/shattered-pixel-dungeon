// Repulsion.java
package com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class Repulsion extends Armor.Glyph {

	private static final ItemSprite.Glowing WHITE = new ItemSprite.Glowing(0xFFFFFF);

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		int level = Math.max(0, armor.buffedLvl());

		float procChance = (level + 1f) / (level + 5f) *
				genericProcChanceMultiplier(defender, (defender instanceof Hero) ? (Hero) defender : null);

		if (Dungeon.level.adjacent(attacker.pos, defender.pos) && Random.Float() < procChance) {
			float powerMulti = Math.max(1f, procChance);
			int opposite = attacker.pos + (attacker.pos - defender.pos);
			Ballistica trajectory = new Ballistica(attacker.pos, opposite, Ballistica.MAGIC_BOLT);
			WandOfBlastWave.throwChar(attacker, trajectory, Math.round(2 * powerMulti),
					true, true, this,
					(defender instanceof Hero) ? (Hero) defender : null);
		}
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return WHITE;
	}
}