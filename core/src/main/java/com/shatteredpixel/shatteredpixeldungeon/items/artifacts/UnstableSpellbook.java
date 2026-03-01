package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import network.Multiplayer;

import java.util.ArrayList;

public class UnstableSpellbook extends Artifact {

	{
		setImage(ItemSpriteSheet.ARTIFACT_SPELLBOOK);
		levelCap = 10;
		charge = (int)(level()*0.6f)+2;
		partialCharge = 0;
		chargeCap = (int)(level()*0.6f)+2;
		defaultAction = AC_READ;
	}

	public static final String AC_READ = "READ";
	public static final String AC_ADD = "ADD";

	private final ArrayList<Class> scrolls = new ArrayList<>();

	public UnstableSpellbook() {
		super();
		setupScrolls();
	}

	private void setupScrolls(){
		scrolls.clear();

		Class<?>[] scrollClasses = Generator.Category.SCROLL.classes;
		float[] probs = Generator.Category.SCROLL.defaultProbsTotal.clone();
		int i = Random.chances(probs);

		while (i != -1){
			scrolls.add(scrollClasses[i]);
			probs[i] = 0;
			i = Random.chances(probs);
		}
		scrolls.remove(ScrollOfTransmutation.class);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && charge > 0 && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_READ);
		}
		if (isEquipped(hero) && level() < levelCap && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_ADD);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals(AC_READ)) {
			if (hero.buff(Blindness.class) != null) {
				if (hero == Multiplayer.localHero()) GLog.w(Messages.get(this, "blinded"));
			} else if (!isEquipped(hero)) {
				if (hero == Multiplayer.localHero()) GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			} else if (charge <= 0) {
				if (hero == Multiplayer.localHero()) GLog.i(Messages.get(this, "no_charge"));
			} else if (cursed) {
				if (hero == Multiplayer.localHero()) GLog.i(Messages.get(this, "cursed"));
			} else {
				doReadEffect(hero);
			}
		} else if (action.equals(AC_ADD)) {
			GameScene.selectItem(new SpellbookAddSelector(this, hero));
		}
	}

	public void performReadEffect(Hero hero) {
		doReadEffect(hero);
	}
	private void doReadEffect(Hero hero) {
		charge--;

		Scroll scroll;
		do {
			scroll = (Scroll) Generator.randomUsingDefaults(Generator.Category.SCROLL);
		} while (scroll == null
				|| ((scroll instanceof ScrollOfIdentify ||
				scroll instanceof ScrollOfRemoveCurse ||
				scroll instanceof ScrollOfMagicMapping) && Random.Int(2) == 0)
				|| (scroll instanceof ScrollOfTransmutation));

		scroll.anonymize();
		scroll.talentChance = 0;

		if (charge > 0 && !scrolls.contains(scroll.getClass())) {
			final Scroll fScroll = scroll;

			final ExploitHandler handler = Buff.affect(hero, ExploitHandler.class, this);
			handler.setup(hero, scroll);

			GameScene.show(new WndOptions(new ItemSprite(this),
					Messages.get(this, "prompt"),
					Messages.get(this, "read_empowered"),
					scroll.trueName(),
					Messages.get(ExoticScroll.regToExo.get(scroll.getClass()), "name")){
				@Override
				protected void onSelect(int index) {
					handler.detach();
					if (index == 1){
						Scroll exotic = Reflection.newInstance(ExoticScroll.regToExo.get(fScroll.getClass()));
						exotic.anonymize();
						exotic.talentChance = 0;
						checkForArtifactProc(hero, exotic);
						exotic.doRead(hero);
						charge--;
						Talent.onArtifactUsed(hero);
					} else {
						checkForArtifactProc(hero, fScroll);
						fScroll.doRead(hero);
						Talent.onArtifactUsed(hero);
					}
					updateQuickslot();
				}

				@Override
				public void onBackPressed() {
					// do nothing
				}
			});
		} else {
			checkForArtifactProc(hero, scroll);
			scroll.doRead(hero);
			Talent.onArtifactUsed(hero);
		}

		updateQuickslot();
	}

	private void checkForArtifactProc(Hero hero, Scroll scroll) {
		if (scroll instanceof ScrollOfLullaby
				|| scroll instanceof ScrollOfRemoveCurse
				|| scroll instanceof ScrollOfTerror) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (hero.fieldOfView[mob.pos]) {
					artifactProc(hero, mob, visiblyUpgraded(), 1);
				}
			}
		} else if (scroll instanceof ScrollOfRage) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				artifactProc(hero, mob, visiblyUpgraded(), 1);
			}
		}
	}

	public static class ExploitHandler extends Buff {
		{
			actPriority = VFX_PRIO;
		}

		private int ownerID;
		private Scroll scroll;

		public void setup(Hero owner, Scroll scroll) {
			this.ownerID = owner.id();
			this.scroll = scroll;
		}

		@Override
		public boolean act() {
			Hero owner = (Hero) Actor.findById(ownerID);
			if (owner != null && scroll != null) {
				scroll.anonymize();
				scroll.talentChance = 0;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						scroll.doRead(owner);
						Item.updateQuickslot();
					}
				});
			}
			detach();
			return true;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("ownerID", ownerID);
			bundle.put("scroll", scroll);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			ownerID = bundle.getInt("ownerID");
			scroll = (Scroll) bundle.get("scroll");
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new bookRecharge();
	}

	@Override
	public void charge(Hero target, float amount) {
		if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null) {
			partialCharge += 0.1f * amount;
			while (partialCharge >= 1) {
				partialCharge--;
				charge++;
			}
			if (charge >= chargeCap) partialCharge = 0;
			updateQuickslot();
		}
	}

	@Override
	public Item upgrade() {
		chargeCap = (int)((level()+1)*0.6f)+2;
		while (!scrolls.isEmpty() && scrolls.size() > (levelCap-1-level())) {
			scrolls.remove(0);
		}
		return super.upgrade();
	}

	@Override
	public void resetForTrinity(int visibleLevel) {
		super.resetForTrinity(visibleLevel);
		setupScrolls();
		while (!scrolls.isEmpty() && scrolls.size() > (levelCap-1-level())) {
			scrolls.remove(0);
		}
	}

	@Override
	public String desc() {
		String desc = super.desc();
		Hero viewer = Multiplayer.localHero();

		if (viewer != null && isEquipped(viewer)) {
			if (cursed) {
				desc += "\n\n" + Messages.get(this, "desc_cursed");
			}
			if (level() < levelCap && scrolls.size() > 0) {
				desc += "\n\n" + Messages.get(this, "desc_index");
				desc += "\n" + "_" + Messages.get(scrolls.get(0), "name") + "_";
				if (scrolls.size() > 1)
					desc += "\n" + "_" + Messages.get(scrolls.get(1), "name") + "_";
			}
		}

		if (level() > 0) {
			desc += "\n\n" + Messages.get(this, "desc_empowered");
		}
		return desc;
	}

	private static final String SCROLLS = "scrolls";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SCROLLS, scrolls.toArray(new Class[scrolls.size()]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		scrolls.clear();
		if (bundle.contains(SCROLLS) && bundle.getClassArray(SCROLLS) != null) {
			for (Class<?> scroll : bundle.getClassArray(SCROLLS)) {
				if (scroll != null) scrolls.add(scroll);
			}
		}
	}

	public class bookRecharge extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap
					&& !cursed
					&& target.buff(MagicImmune.class) == null
					&& Regeneration.regenOn((Hero) target)) {
				float chargeGain = 1 / (120f - (chargeCap - charge) * 5f);
				chargeGain *= RingOfEnergy.artifactChargeMultiplier(target);
				partialCharge += chargeGain;
				while (partialCharge >= 1) {
					partialCharge--;
					charge++;
					if (charge == chargeCap) partialCharge = 0;
				}
			}
			updateQuickslot();
			spend(TICK);
			return true;
		}
	}

	private class SpellbookAddSelector extends WndBag.ItemSelector {
		private final UnstableSpellbook spellbook;
		private final Hero hero;

		SpellbookAddSelector(UnstableSpellbook spellbook, Hero hero) {
			this.spellbook = spellbook;
			this.hero = hero;
		}

		@Override
		public String textPrompt() {
			return Messages.get(UnstableSpellbook.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return ScrollHolder.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Scroll && item.isIdentified() && spellbook.scrolls.contains(item.getClass());
		}

		@Override
		public void onSelect(Item item) {
			if (item != null && item instanceof Scroll && item.isIdentified()) {
				for (int i = 0; i < Math.min(2, spellbook.scrolls.size()); i++) {
					if (spellbook.scrolls.get(i).equals(item.getClass())) {
						hero.sprite.operate(hero.pos);
						hero.busy();
						hero.spend(2f);
						Sample.INSTANCE.play(Assets.Sounds.BURNING);
						hero.sprite.emitter().burst(ElmoParticle.FACTORY, 12);

						spellbook.scrolls.remove(i);
						item.detach(hero.belongings.backpack);

						spellbook.upgrade();
						if (hero == Multiplayer.localHero()) {
							Catalog.countUse(UnstableSpellbook.class);
						}
						GLog.i(Messages.get(UnstableSpellbook.class, "infuse_scroll"));
						return;
					}
				}
				GLog.w(Messages.get(UnstableSpellbook.class, "unable_scroll"));
			} else if (item instanceof Scroll && !item.isIdentified()) {
				GLog.w(Messages.get(UnstableSpellbook.class, "unknown_scroll"));
			}
		}
	}
}