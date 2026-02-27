package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

public class HeavyBoomerang extends MissileWeapon {

    {
        image = ItemSpriteSheet.BOOMERANG;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1f;

        tier = 4;
        sticky = false;
        baseUses = 5;
    }

    @Override
    public int max(int lvl) {
        return 4 * tier + (tier - 1) * lvl;
    }

    boolean circlingBack = false;

    @Override
    protected float adjacentAccFactor(Char owner, Char target) {
        if (circlingBack) {
            return 1.5f;
        }
        return super.adjacentAccFactor(owner, target);
    }

    @Override
    public float pickupDelay() {
        return circlingBack ? 0f : super.pickupDelay();
    }

    @Override
    protected void rangedHit(Char enemy, int cell) {
        decrementDurability();
        if (durability > 0) {
            // curUser установлен перед броском (владелец бумеранга)
            Buff.append(curUser, CircleBack.class, this).setup(this, cell, curUser.pos, Dungeon.depth, Dungeon.branch);
        }
    }

    @Override
    protected void rangedMiss(int cell) {
        parent = null;
        Buff.append(curUser, CircleBack.class, this).setup(this, cell, curUser.pos, Dungeon.depth, Dungeon.branch);
    }

    public static class CircleBack extends Buff {

        {
            revivePersists = true;
        }

        private HeavyBoomerang boomerang;
        private int thrownPos;
        private int returnPos;
        private int returnDepth;
        private int returnBranch;

        private int left;

        public void setup(HeavyBoomerang boomerang, int thrownPos, int returnPos, int returnDepth, int returnBranch) {
            this.boomerang = boomerang;
            this.thrownPos = thrownPos;
            this.returnPos = returnPos;
            this.returnDepth = returnDepth;
            this.returnBranch = returnBranch;
            left = 5;
        }

        public int returnPos() {
            return returnPos;
        }

        public MissileWeapon cancel() {
            detach();
            return boomerang;
        }

        public int activeDepth() {
            return returnDepth;
        }

        @Override
        public boolean act() {
            if (returnDepth == Dungeon.depth && returnBranch == Dungeon.branch) {
                left--;
                if (left <= 0) {
                    final Char returnTarget = Actor.findChar(returnPos);
                    final Char owner = this.target; // владелец баффа — герой, бросивший бумеранг

                    MissileSprite visual = ((MissileSprite) owner.sprite.parent.recycle(MissileSprite.class));
                    visual.reset(thrownPos,
                            returnPos,
                            boomerang,
                            new Callback() {
                                @Override
                                public void call() {
                                    detach();
                                    boomerang.circlingBack = true;
                                    if (returnTarget == owner) {
                                        if (!boomerang.spawnedForEffect) {
                                            if (owner instanceof Hero && !boomerang.doPickUp((Hero) owner)) {
                                                Dungeon.level.drop(boomerang, returnPos).sprite.drop();
                                            }
                                        }
                                    } else if (returnTarget != null) {
                                        if (owner instanceof Hero && ((Hero) owner).shoot(returnTarget, boomerang)) {
                                            boomerang.decrementDurability();
                                        }
                                        if (!boomerang.spawnedForEffect && boomerang.durability > 0) {
                                            Dungeon.level.drop(boomerang, returnPos).sprite.drop();
                                        }
                                    } else if (!boomerang.spawnedForEffect) {
                                        Dungeon.level.drop(boomerang, returnPos).sprite.drop();
                                    }
                                    boomerang.circlingBack = false;
                                    CircleBack.this.next();
                                }
                            });
                    visual.alpha(0f);
                    float duration = Dungeon.level.trueDistance(thrownPos, returnPos) / 20f;
                    owner.sprite.parent.add(new AlphaTweener(visual, 1f, duration));
                    return false;
                }
            }
            spend(TICK);
            return true;
        }

        private static final String BOOMERANG = "boomerang";
        private static final String THROWN_POS = "thrown_pos";
        private static final String RETURN_POS = "return_pos";
        private static final String RETURN_DEPTH = "return_depth";
        private static final String RETURN_BRANCH = "return_branch";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(BOOMERANG, boomerang);
            bundle.put(THROWN_POS, thrownPos);
            bundle.put(RETURN_POS, returnPos);
            bundle.put(RETURN_DEPTH, returnDepth);
            bundle.put(RETURN_BRANCH, returnBranch);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            boomerang = (HeavyBoomerang) bundle.get(BOOMERANG);
            thrownPos = bundle.getInt(THROWN_POS);
            returnPos = bundle.getInt(RETURN_POS);
            returnDepth = bundle.getInt(RETURN_DEPTH);
            returnBranch = bundle.contains(RETURN_BRANCH) ? bundle.getInt(RETURN_BRANCH) : 0;
        }
    }
}