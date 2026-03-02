package network.handlers;

import com.watabou.utils.Bundle;

import network.ClientStateMachine;

public class HeroCreatedHandler implements MessageHandler {
    @Override
    public String getType() { return "HERO_CREATED"; }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        int playerId = bundle.getInt("playerId");
        ClientStateMachine.getInstance().onHeroCreated(playerId);
    }
}

