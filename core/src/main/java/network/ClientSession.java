package network;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import io.netty.channel.ChannelHandlerContext;
import network.PlayerStateMachine;

public class ClientSession {
    public final int playerId;
    public final ChannelHandlerContext ctx;
    public final PlayerStateMachine stateMachine;
    public String name;
    public HeroClass heroClass;
    public Hero hero; // создаётся после генерации мира

    public ClientSession(int playerId, ChannelHandlerContext ctx, String name) {
        this.playerId = playerId;
        this.ctx = ctx;
        this.name = name;
        this.stateMachine = new PlayerStateMachine();
        // Подписываемся на изменения состояния для глобальной логики
        this.stateMachine.addListener(this::onStateChanged);
    }

    private void onStateChanged(PlayerStateMachine.State newState) {
        // Серверная логика: например, проверка готовности всех игроков
        // ServerGameLogic.getInstance().checkAllPlayersReady();
    }
}