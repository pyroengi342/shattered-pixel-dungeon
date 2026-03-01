package network.handlers.window;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

public class SubclassHandler implements MessageHandler {
    @Override
    public String getType() {
        return "SUBCLASS_CHOOSE";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        Game.runOnRenderThread(() -> {
            // Игнорируем своё сообщение
            if (senderId == NetworkManager.getLocalPlayerId()) return;

            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            String subClassName = bundle.getString("subClass");
            HeroSubClass subClass = HeroSubClass.valueOf(subClassName);
            hero.subClass = subClass; // Устанавливаем подкласс для героя-отправителя
            // При необходимости можно обновить связанные с подклассом данные
        });
    }

    public static void send(HeroSubClass subClass) {
        Bundle bundle = new Bundle();
        bundle.put("subClass", subClass.name());
        NetworkManager.sendMessage("SUBCLASS_CHOOSE", bundle);
    }
}