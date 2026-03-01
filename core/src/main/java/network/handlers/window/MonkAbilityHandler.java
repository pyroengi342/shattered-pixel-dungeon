package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class MonkAbilityHandler implements MessageHandler {
    @Override
    public String getType() {
        return "MONK_ABILITY";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // Игнорируем собственное сообщение (действие уже выполнено локально)
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            int abilityIndex = bundle.getInt("abilityIndex");
            Integer cell = bundle.contains("cell") ? bundle.getInt("cell") : null;

            MonkEnergy.MonkAbility[] abilities = MonkEnergy.MonkAbility.abilities;
            if (abilityIndex < 0 || abilityIndex >= abilities.length) return;

            MonkEnergy.MonkAbility ability = abilities[abilityIndex];
            MonkEnergy energyBuff = hero.buff(MonkEnergy.class);
            if (energyBuff == null) return;

            // Выполняем способность
            ability.doAbility(hero, cell);
        });
    }

    // Статический метод отправки сообщения
    public static void send(int abilityIndex, Integer cell) {
        Bundle bundle = new Bundle();
        bundle.put("abilityIndex", abilityIndex);
        if (cell != null) {
            bundle.put("cell", cell);
        }
        NetworkManager.sendMessage("MONK_ABILITY", bundle);
    }
}