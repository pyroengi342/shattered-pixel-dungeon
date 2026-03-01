package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class AbilityHandler implements MessageHandler {
    @Override
    public String getType() {
        return "ABILITY_CHOOSE";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            int abilityIndex = bundle.getInt("abilityIndex");
            ArmorAbility[] abilities = hero.heroClass.armorAbilities();
            if (abilityIndex < 0 || abilityIndex >= abilities.length) return;

            ArmorAbility chosen = abilities[abilityIndex];
            Armor armor = hero.belongings.armor();

            // Используем KingsCrown.upgradeArmor() для корректной установки способности
            new KingsCrown().upgradeArmor(hero, armor, chosen);
        });
    }

    public static void send(int abilityIndex) {
        Bundle bundle = new Bundle();
        bundle.put("abilityIndex", abilityIndex);
        NetworkManager.sendMessage("ABILITY_CHOOSE", bundle);
    }
}