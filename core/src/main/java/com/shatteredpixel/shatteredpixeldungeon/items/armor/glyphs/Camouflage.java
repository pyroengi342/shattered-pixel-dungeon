package com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.audio.Sample;

import network.AudioWrapper;
import network.Multiplayer;

public class Camouflage extends Armor.Glyph {

	private static final ItemSprite.Glowing GREEN = new ItemSprite.Glowing(0x448822);

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		// no proc effect, triggers in HighGrass.trample
		return damage;
	}

	public static void activate(Char ch, int level) {
		if (level == -1) return;
		Hero owner = (ch instanceof Hero) ? (Hero) ch : null;
		float multiplier = genericProcChanceMultiplier(ch, owner);
		Buff.prolong(ch, Invisibility.class, Math.round((3 + level / 2f) * multiplier), null);
		AudioWrapper.play(Assets.Sounds.MELD, ch.pos);
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GREEN;
	}
}