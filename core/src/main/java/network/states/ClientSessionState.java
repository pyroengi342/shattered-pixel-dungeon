package network.states;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import io.netty.channel.ChannelHandlerContext;
import network.ServerAgent;

/**
 * Сессия клиента на сервере.
 * Содержит канал и машину состояний для данного клиента.
 */
public class ClientSessionState {
    public final ChannelHandlerContext ctx;
    public final PlayerStateMachine stateMachine;

    public ClientSessionState(int playerId, ChannelHandlerContext ctx, String name, ServerAgent callflow) {
        this.ctx = ctx;
        this.stateMachine = new PlayerStateMachine(playerId, name);
        // Подписываемся на изменения состояния
        this.stateMachine.addListener(newState -> callflow.onClientStateChanged(this));
    }
    public boolean hasData(PlayerStateMachine.RequiredData data)
    { return stateMachine.hasData(data);}
    public boolean isRequestSent(PlayerStateMachine.RequiredData data)
    { return stateMachine.isRequestSent(data);}
    public void markRequestSent(PlayerStateMachine.RequiredData data)
    { stateMachine.markRequestSent(data);}
    // Опционально: сброс флагов при переподключении
    public void resetRequests() {
        stateMachine.resetRequests();
    }

    // Удобные методы-обёртки
    public void setPlayerId(int connectionID) {
        stateMachine.setPlayerId(connectionID);
    }
    public void setHero(Hero hero) {
        stateMachine.setHero(hero);
    }
    public void setSeed(long seed) {
        stateMachine.setSeed(seed);
    }
    public void setReady(boolean ready) {
        stateMachine.setReady(ready);
    }
    public void setName(String name) {
        stateMachine.setName(name);
    }
    /// @return
    public int getPlayerId() {
        return stateMachine.getPlayerId();
    }
    public String getName()  { return stateMachine.getName(); }

}