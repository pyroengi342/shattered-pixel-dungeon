// Stone.java
package com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.GameMath;

public class Stone extends Armor.Glyph {

	private static final ItemSprite.Glowing GREY = new ItemSprite.Glowing(0x222222);

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		testing = true;
		float accuracy = attacker.attackSkill(defender);
		float evasion = defender.defenseSkill(attacker);
		testing = false;

		// ... (копипаста из оригинального кода для accuracy и evasion)

		// Добавляем множитель глифа
		evasion *= genericProcChanceMultiplier(defender, (defender instanceof Hero) ? (Hero) defender : null);

		float hitChance;
		if (evasion >= accuracy) {
			hitChance = (accuracy / evasion) / 2f;
		} else {
			hitChance = 1f - (evasion / accuracy) / 2f;
		}

		hitChance = GameMath.gate(0.25f, (1f + 3f * hitChance) / 4f, 1f);
		damage = (int) Math.ceil(damage * hitChance);

		return damage;
	}

	private static boolean testing = false;

	public static boolean testingEvasion() {
		return testing;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GREY;
	}
}