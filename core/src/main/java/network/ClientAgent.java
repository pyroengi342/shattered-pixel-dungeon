package network;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import network.handlers.HeroClassHandler;
import network.states.ClientStateMachine;
import network.states.PlayerStateMachine;

import java.util.HashMap;
import java.util.Map;

public class ClientAgent {
    private final ClientStateMachine clientState;
    private HeroClass pendingHeroClass;

    // Перечисление данных, которые клиент должен отправить на сервер
    private enum RequiredData {
        CONNECTION_ID,
        SEED,
        HERO_CLASS,
    }

    // Собственный функциональный интерфейс для обратной совместимости
    private interface RequestHandler { void handle(); }

    // Флаги отправки каждого типа данных
    private final Map<RequiredData, Boolean> sentFlags = new HashMap<>();

    // Обработчики отправки данных
    private final Map<RequiredData, RequestHandler> requestHandlers = new HashMap<>();

    public ClientAgent(ClientStateMachine clientState) {
        this.clientState = clientState;
        clientState.addListener(newState -> onClientStateChanged());

        // Инициализация обработчиков
        requestHandlers.put(RequiredData.HERO_CLASS, () -> {
            // Определяем выбранный класс
            HeroClass heroClass = pendingHeroClass != null ? pendingHeroClass : GamesInProgress.selectedClass;
            if (heroClass == null) return;

            // Создаём локального героя, если его ещё нет
            int localId = NetworkManager.getLocalPlayerId();
            if (Multiplayer.Players.get(localId).hero == null) {
                Multiplayer.Players.get(localId).hero = new Hero();
                clientState.onHeroCreated(Multiplayer.localHero());
            }

            // Устанавливаем класс и отправляем на сервер
            Multiplayer.Players.setHeroClass(localId, heroClass);
            HeroClassHandler.sendHeroClass(heroClass);

            markSent(RequiredData.HERO_CLASS);
        });

        // Можно добавить обработчики для других RequiredData
    }

    // Вызывается при любом изменении состояния клиента (получен seed, создан герой и т.д.)
    private void onClientStateChanged() { checkClientProgress(); }

    // Централизованная проверка: что ещё не отправлено и можно ли отправить
    private void checkClientProgress() {
        for (RequiredData data : RequiredData.values()) {
            if (!isSent(data)) {
                RequestHandler handler = requestHandlers.get(data);
                if (handler != null) {
                    handler.handle();
                } else {
                    System.err.println("No handler for RequiredData: " + data);
                }
            }
        }
    }

    private boolean isSent(RequiredData data) {
        Boolean flag = sentFlags.get(data);
        return flag != null ? flag : false;
    }
    private void markSent(RequiredData data) { sentFlags.put(data, true); }
    // Вызывается из UI, когда игрок выбрал класс
    public void onHeroClassSelected(HeroClass heroClass) {
        this.pendingHeroClass = heroClass;
        // Сбрасываем флаг, чтобы при следующей проверке класс отправился заново
        sentFlags.put(RequiredData.HERO_CLASS, false);
        checkClientProgress();
    }

    // Сброс при выходе из лобби или переподключении
    public void reset() {
        pendingHeroClass = null;
        sentFlags.clear();
    }
}