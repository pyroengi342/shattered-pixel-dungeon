package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSadGhost;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class GhostRewardHandler implements MessageHandler {
    @Override
    public String getType() {
        return "GHOST_REWARD";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // Игнорируем собственное сообщение (уже выполнено локально)
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            int choice = bundle.getInt("choice"); // 0 - оружие, 1 - броня
            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            Item reward = (choice == 0) ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.weapon
                    : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.armor;
            if (reward == null) return;

            // Выполняем получение награды от имени героя-инициатора
            WndSadGhost.processReward(reward, hero);
        });
    }

    // Статический метод отправки сообщения о выборе
    public static void send(int choice) {
        Bundle bundle = new Bundle();
        bundle.put("choice", choice);
        NetworkManager.sendMessage("GHOST_REWARD", bundle);
    }
}