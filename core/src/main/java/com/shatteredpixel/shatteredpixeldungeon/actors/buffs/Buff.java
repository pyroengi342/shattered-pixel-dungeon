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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.HashSet;

public class Buff extends Actor {

    // НОВОЕ: поле для хранения источника баффа
    protected Object source;

    // Методы для работы с источником
    public Buff setSource(Object source) {
        this.source = source;
        return this;
    }

    public Object getSource() {
        return source;
    }

    public Char getSourceChar() {
        if (source instanceof Char) {
            return (Char) source;
        }
        // Если source - это ID, попробуем найти персонажа
        if (source instanceof Integer) {
            return (Char) Actor.findById((Integer) source);
        }
        return null;
    }
    private static final String SOURCE = "source";
	
	public Char target;

	//whether this buff was already extended by the mnemonic prayer spell
	public boolean mnemonicExtended = false;

	{
		actPriority = BUFF_PRIO; //low priority, towards the end of a turn
	}

	//determines how the buff is announced when it is shown.
	public enum buffType {POSITIVE, NEGATIVE, NEUTRAL}
	public buffType type = buffType.NEUTRAL;
	
	//whether or not the buff announces its name
	public boolean announced = false;

	//whether a buff should persist through revive effects or similar (e.g. transmogrify)
	public boolean revivePersists = false;
	
	protected HashSet<Class> resistances = new HashSet<>();
	
	public HashSet<Class> resistances() {
		return new HashSet<>(resistances);
	}
	
	protected HashSet<Class> immunities = new HashSet<>();
	
	public HashSet<Class> immunities() {
		return new HashSet<>(immunities);
	}

    public boolean attachTo(Char target) {
        return attachTo(target, null);
    }
    public boolean attachTo(Char target, Object source) {
        // Устанавливаем источник ПЕРЕД прикреплением
        this.source = source;

        if (target.isImmune(getClass())) {
            this.source = null;
            return false;
        }

        this.target = target;

        if (target.add(this)) {
            if (target.sprite != null) fx(true);
            return true;
        } else {
            this.target = null;
            this.source = null;
            return false;
        }
    }
	
	public void detach() {
        // Очищаем ссылки перед отсоединением
        if (target != null) {
            if (target.remove(this) && target.sprite != null) {
                fx(false);
            }
            target = null;
        }
        clearReferences();
	}
	
	@Override
	public boolean act() {
		diactivate();
		return true;
	}
	
	public int icon() {
		return BuffIndicator.NONE;
	}

	//some buffs may want to tint the base texture color of their icon
	public void tintIcon( Image icon ){
		//do nothing by default
	}

	//percent (0-1) to fade out out the buff icon, usually if buff is expiring
	public float iconFadePercent(){
		return 0;
	}

	//text to display on large buff icons in the desktop UI
	public String iconTextDisplay(){
		return "";
	}

	//visual effect usually attached to the sprite of the character the buff is attacked to
	public void fx(boolean on) {
		//do nothing by default
	}

	public String heroMessage(){
		String msg = Messages.get(this, "heromsg");
		if (msg.isEmpty()) {
			return null;
		} else {
			return msg;
		}
	}

	public String name() {
		return Messages.get(this, "name");
	}

	public String desc(){
		return Messages.get(this, "desc");
	}

	//to handle the common case of showing how many turns are remaining in a buff description.
	protected String dispTurns(float input){
		return Messages.decimalFormat("#.##", input);
	}

	//buffs act after the hero, so it is often useful to use cooldown+1 when display buff time remaining
	public float visualcooldown(){
		return cooldown()+1f;
	}

	private static final String MNEMONIC_EXTENDED    = "mnemonic_extended";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (source instanceof Class) {
			bundle.put(SOURCE, (Class) source);
		} else if (source instanceof Bundlable) {
			bundle.put(SOURCE, (Bundlable) source);
		} else if (source instanceof Integer) {
			bundle.put(SOURCE, (int) source);
		} else if (source instanceof String) {
			bundle.put(SOURCE, (String) source);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(SOURCE)) {
			source = bundle.get(SOURCE); // Bundle сам вернёт объект соответствующего типа (Class, Integer, String, Bundlable)
		}
	}

	//creates a fresh instance of the buff and attaches that, this allows duplication.
	public static<T extends Buff> T append( Char target, Class<T> buffClass, Object source ) {
		T buff = Reflection.newInstance(buffClass);
		buff.attachTo( target, source );
		return buff;
	}

	public static<T extends FlavourBuff> T append( Char target, Class<T> buffClass, float duration, Object source ) {
		T buff = append( target, buffClass, source );
		buff.spend( duration * target.resist(buffClass) );
		return buff;
	}

	//same as append, but prevents duplication.
	public static<T extends Buff> T affect( Char target, Class<T> buffClass, Object source ) {
		T buff = target.buff( buffClass );
		if (buff != null) {
			return buff;
		} else {
			return append( target, buffClass, source );
		}
	}
	
	public static<T extends FlavourBuff> T affect( Char target, Class<T> buffClass, float duration, Object source ) {
		T buff = affect( target, buffClass, source );
		buff.spend( duration * target.resist(buffClass) );
		return buff;
	}

	//postpones an already active buff, or creates & attaches a new buff and delays that.
	public static<T extends FlavourBuff> T prolong( Char target, Class<T> buffClass, float duration, Object source ) {
		T buff = affect( target, buffClass, source );
		buff.postpone( duration * target.resist(buffClass) );
		return buff;
	}

	public static<T extends CounterBuff> T count( Char target, Class<T> buffclass, float count, Object source ) {
		T buff = affect( target, buffclass, source );
		buff.countUp( count );
		return buff;
	}
	
	public static void detach( Char target, Class<? extends Buff> cl ) {
		for ( Buff b : target.buffs( cl )){
			b.detach();
		}
	}

    // Метод для очистки всех ссылок
    private void clearReferences() {
        source = null;
        target = null;
    }
    @Override
    protected void onRemove() {
        // Очищаем ссылки при полном удалении баффа
        clearReferences();
        super.onRemove();
    }
}
