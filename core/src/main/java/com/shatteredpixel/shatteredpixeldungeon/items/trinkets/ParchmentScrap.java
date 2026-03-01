// ParchmentScrap.java
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import network.Multiplayer;

public class ParchmentScrap extends Trinket {
    // TODO this needs to be remade completely
    {
        setImage(ItemSpriteSheet.PARCHMENT_SCRAP);
    }

    @Override
    protected int upgradeEnergyCost() {
        return 10 + 5 * level();
    }

    @Override
    public String statsDesc() {
        Hero viewer = Multiplayer.localHero();
//        int level = isIdentified() ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc",
                (int) enchantChanceMultiplier(viewer),
                Messages.decimalFormat("#.##", curseChanceMultiplier(viewer)));
    }

    public static float enchantChanceMultiplier(Hero hero) {
        int lvl = trinketLevel(ParchmentScrap.class, hero);
        switch (lvl) {
            case 0: return 2;
            case 1: return 4;
            case 2: return 7;
            case 3: return 10;
            default: return 1;
        }
    }

    public static float curseChanceMultiplier(Hero hero) {
        int lvl = trinketLevel(ParchmentScrap.class, hero);
        switch (lvl) {
            case 0: return 1.5f;
            case 1: return 2f;
            case 2: return 1f;
            case 3: return 0f;
            default: return 1;
        }
    }

//    private void extracted() {
//        float effectRoll = Random.Float();
//        //30% chance to be cursed
//        //10% chance to be enchanted
//        if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier(curUser)) {
//            enchant(Weapon.Enchantment.randomCurse());
//            cursed = true;
//        } else if (effectRoll >= 1f - (0.1f * ParchmentScrap.enchantChanceMultiplier(curUser))){
//            enchant();
//        }
//    }
}