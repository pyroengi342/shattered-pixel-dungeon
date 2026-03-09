package network.states;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

import network.NetworkManager;

/**
 * Собственная сессия клиента.
 * Отслеживает своё состояние.
 */
public class ClientStateMachine {
    private static ClientStateMachine instance;
    private final PlayerStateMachine stateMachine;

    // Локальный игрок ещё не подключён – создаём с временными данными,
    // playerId и name будут обновлены позже из сообщений.
    private ClientStateMachine()
    { stateMachine = new PlayerStateMachine(-1, ""); }

    public static ClientStateMachine getInstance() {
        if (instance == null) {
            instance = new ClientStateMachine();
        }
        return instance;
    }

    public void addListener(PlayerStateMachine.StateListener listener)
    { stateMachine.addListener(listener); }

    public void removeListener(PlayerStateMachine.StateListener listener)
    { stateMachine.removeListener(listener); }

    public PlayerStateMachine.State getCurrentState()
    { return stateMachine.getCurrentState(); }

    // --- Методы, вызываемые из UI ---

    // Реальное состояние изменится после получения PLAYER_ASSIGN
    public void connectToServer(String host)
    { NetworkManager.getInstance().connectToServer(host); }

    public void onHeroCreated(Hero hero) { stateMachine.setHero(hero); }

    // Устанавливаем ready = true (или сервер сам пришлёт GAME_START)
    public void setReady(boolean ready) { stateMachine.setReady(ready); }
    public boolean getReady() { return stateMachine.getReady(); }

    public void onError(String reason) { stateMachine.forceError(); }

    public int getPlayerId() {
        return stateMachine.getPlayerId();
    }

    public void reset() {}

    // ClientStateMachine.java
    public void onSeedInit(long seed) { stateMachine.setSeed(seed); }
    public void onPlayerAssign(int playerId, String name) {
        // Обновляем ID и имя в stateMachine
        stateMachine.setPlayerId(playerId);
        stateMachine.setName(name);
        // состояние пересчитается автоматически (hero=null, seed=null -> HANDSHAKE)
    }
    public boolean isHeroCreated() {
        return stateMachine.getHero() != null
                && stateMachine.getHero().heroClass != null;
    }
    public boolean isSeedReceived() {
        return stateMachine.getSeed() != null;
    }
    public boolean isLocalPlayerIdSet() {
        return stateMachine.getPlayerId() != -1;
    }

}