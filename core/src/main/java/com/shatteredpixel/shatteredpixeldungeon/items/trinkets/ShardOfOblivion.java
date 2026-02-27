// ShardOfOblivion.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Identification;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.hornRecharge;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

import network.Multiplayer;

import java.util.ArrayList;

public class ShardOfOblivion extends Trinket {

    {
        image = ItemSpriteSheet.OBLIVION_SHARD;
    }

    public static final String AC_IDENTIFY = "IDENTIFY";

    @Override
    protected int upgradeEnergyCost() {
        return 6 + 2 * level();
    }
	
	public static boolean passiveIDDisabled(Hero hero) {
        return trinketLevel(ShardOfOblivion.class, hero) >= 0;
    }

    @Override
    public String statsDesc() {
        if (isIdentified()) {
            return Messages.get(this, "stats_desc", buffedLvl() + 1);
        } else {
            return Messages.get(this, "stats_desc", 1);
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_IDENTIFY);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_IDENTIFY)) {
            curUser = hero;
            curItem = this;
            GameScene.selectItem(new IdentifySelector(hero, this));
        } else {
            super.execute(hero, action);
        }
    }

    // Внутренний класс для выбора предмета, захватывает героя
    private class IdentifySelector extends WndBag.ItemSelector {
        private Hero hero;
        private ShardOfOblivion shard;

        IdentifySelector(Hero hero, ShardOfOblivion shard) {
            this.hero = hero;
            this.shard = shard;
        }

        @Override
        public String textPrompt() {
            return Messages.get(ShardOfOblivion.class, "identify_prompt");
        }

        @Override
        public boolean itemSelectable(Item item) {
            return !item.isIdentified() && item.isUpgradable();
        }

        @Override
        public void onSelect(Item item) {
            if (item == null) return;

            boolean ready = false;
            if (item instanceof Weapon) {
                ready = ((Weapon) item).readyToIdentify();
                if (item.isEquipped(hero) && hero.pointsInTalent(Talent.ADVENTURERS_INTUITION) == 2) {
                    ready = true;
                }
            } else if (item instanceof Armor) {
                ready = ((Armor) item).readyToIdentify();
                if (item.isEquipped(hero) && hero.pointsInTalent(Talent.VETERANS_INTUITION) == 2) {
                    ready = true;
                }
            } else if (item instanceof Ring) {
                ready = ((Ring) item).readyToIdentify();
                if (item.isEquipped(hero) && hero.pointsInTalent(Talent.THIEFS_INTUITION) == 2) {
                    ready = true;
                }
            } else if (item instanceof Wand) {
                ready = ((Wand) item).readyToIdentify();
            }

            if (ready) {
                item.identify();
                Badges.validateItemLevelAquired(item);
                hero.sprite.operate(hero.pos);
                Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
                hero.sprite.parent.add(new Identification(hero.sprite.center().offset(0, -16)));
                GLog.p(Messages.get(ShardOfOblivion.class, "identify"));
            } else {
                GLog.w(Messages.get(ShardOfOblivion.class, "identify_not_yet"));
            }
        }
    }

    public static class WandUseTracker extends FlavourBuff {
        {
            type = buffType.POSITIVE;
        }
        public static float DURATION = 50f;

        @Override
        public int icon() {
            return BuffIndicator.WAND;
        }
        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0, 0.6f, 1);
        }
        @Override
        public float iconFadePercent() {
            return Math.max(0, (DURATION - visualcooldown()) / DURATION);
        }
    }

    public static class ThrownUseTracker extends FlavourBuff {
        {
            type = buffType.POSITIVE;
        }
        public static float DURATION = 50f;

        @Override
        public int icon() {
            return BuffIndicator.THROWN_WEP;
        }
        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0, 0.6f, 1);
        }
        @Override
        public float iconFadePercent() {
            return Math.max(0, (DURATION - visualcooldown()) / DURATION);
        }
    }

    // Версия с героем
    public static float lootChanceMultiplier(Hero hero) {
        return lootChanceMultiplier(trinketLevel(ShardOfOblivion.class, hero), hero);
    }

    public static float lootChanceMultiplier(int level, Hero hero) {
        if (level < 0) return 1f;

        int wornUnIDed = 0;
        if (hero.belongings.weapon() != null && !hero.belongings.weapon().isIdentified()) {
            wornUnIDed++;
        }
        if (hero.belongings.armor() != null && !hero.belongings.armor().isIdentified()) {
            wornUnIDed++;
        }
        if (hero.belongings.ring() != null && !hero.belongings.ring().isIdentified()) {
            wornUnIDed++;
        }
        if (hero.belongings.misc() != null && !hero.belongings.misc().isIdentified()) {
            wornUnIDed++;
        }
        if (hero.buff(WandUseTracker.class) != null) {
            wornUnIDed++;
        }
        if (hero.buff(ThrownUseTracker.class) != null) {
            wornUnIDed++;
        }

        wornUnIDed = Math.min(wornUnIDed, level + 1);
        return 1f + 0.2f * wornUnIDed;
    }
}