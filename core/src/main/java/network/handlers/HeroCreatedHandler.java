package network.handlers;

import com.watabou.utils.Bundle;

import network.LogicStateMachine;

public class HeroCreatedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CREATED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        int playerId = bundle.getInt("playerId");
        LogicStateMachine.getInstance().onHeroCreated(playerId);
    }
}

