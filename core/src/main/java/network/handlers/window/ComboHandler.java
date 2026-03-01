package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class ComboHandler implements MessageHandler {
    @Override
    public String getType() {
        return "COMBO_MOVE";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            int moveOrdinal = bundle.getInt("move");
            Combo combo = hero.buff(Combo.class);
            if (combo == null) return;

            Combo.ComboMove move = Combo.ComboMove.values()[moveOrdinal];
            combo.useMove(move);
        });
    }

    // Статический метод отправки
    public static void send(Combo.ComboMove move) {
        Bundle bundle = new Bundle();
        bundle.put("move", move.ordinal());
        NetworkManager.sendMessage("COMBO_MOVE", bundle);
    }
}