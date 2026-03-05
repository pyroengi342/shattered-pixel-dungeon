package network;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import network.handlers.HeroClassHandler;
import network.states.ClientSessionState;
import network.states.ClientStateMachine;
import network.states.PlayerStateMachine;

public class ClientCallflow {
    private final ClientStateMachine clientState;
    private final NetworkManager networkManager;
    private HeroClass pendingHeroClass; // выбранный класс, ожидающий отправки

    public ClientCallflow(ClientStateMachine clientState, NetworkManager networkManager) {
        this.clientState = clientState;
        this.networkManager = networkManager;
        // Подписываемся на изменения состояния клиента
        clientState.addListener(this::onClientStateChanged);
        // Немедленно проверяем текущее состояние (оно уже может быть HANDSHAKE)
//        onClientStateChanged(clientState.getCurrentState());
    }
    private void onClientStateChanged(PlayerStateMachine.State newState) {
        switch (newState) {
            case HANDSHAKE:
                sendPendingHeroClassIfAny();
            case WAITING_FOR_HERO:
                // Достигнуто состояние, когда можно отправлять класс (seed получен)
                sendPendingHeroClassIfAny();
                break;
            case WAITING_FOR_SEED:
                sendPendingHeroClassIfAny();
                break;
            case GAME_READY:
                break;
            // Другие состояния можно обрабатывать при необходимости
            default:
                break;
        }
    }

    /**
     * Вызывается из UI, когда игрок выбрал класс и нажал "Start".
     */
    public void onHeroClassSelected(HeroClass heroClass) {
        this.pendingHeroClass = heroClass;
        sendPendingHeroClassIfAny();
    }

    private void sendPendingHeroClassIfAny() {
        if (pendingHeroClass == null || GamesInProgress.selectedClass != pendingHeroClass)
        {
            pendingHeroClass = GamesInProgress.selectedClass;
            Multiplayer.Players.get(NetworkManager.getLocalPlayerId()).hero = new Hero();
            if (Multiplayer.localHero() != null){
                int a = 0;
            };
            clientState.onHeroCreated(Multiplayer.localHero());
            Multiplayer.Players.setHeroClass(NetworkManager.getLocalPlayerId(), GamesInProgress.selectedClass);

            HeroClassHandler.sendHeroClass(GamesInProgress.selectedClass);

        }
    }

    public void reset() {
        pendingHeroClass = null;
    }
}