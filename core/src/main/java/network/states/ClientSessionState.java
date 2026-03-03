package network.states;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import io.netty.channel.ChannelHandlerContext;
import network.ServerCallflow;

/**
 * Сессия клиента на сервере.
 * Содержит канал и машину состояний для данного клиента.
 */
public class ClientSessionState {
    public final int playerId;
    public final ChannelHandlerContext ctx;
    public final PlayerStateMachine stateMachine;
    public String name;

    public ClientSessionState(int playerId, ChannelHandlerContext ctx, String name, ServerCallflow callflow) {
        this.playerId = playerId;
        this.ctx = ctx;
        this.name = name;
        this.stateMachine = new PlayerStateMachine(playerId, name);
        // Подписываемся на изменения состояния
        this.stateMachine.addListener(newState -> callflow.onClientStateChanged(this));
    }

    // Удобные методы-обёртки
    public void setHero(Hero hero) {
        stateMachine.setHero(hero);
    }

    public void setSeed(long seed) {
        stateMachine.setSeed(seed);
    }

    public void setReady(boolean ready) {
        stateMachine.setReady(ready);
    }
}