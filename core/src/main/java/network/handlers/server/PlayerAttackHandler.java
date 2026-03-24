/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package network.handlers.server;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;
import network.states.TurnManager;

/**
 * Server-side handler for player attack actions.
 * Receives ATTACK requests from clients, validates, and broadcasts to all clients.
 */
public class PlayerAttackHandler implements MessageHandler {

    @Override
    public String getType() {
        return "PLAYER_ATTACK";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // Server handler only
        if (NetworkManager.getMode() != NetworkManager.Mode.SERVER) {
            return;
        }

        // Check turn-based rules
        if (TurnManager.getInstance().getPlayerCount() > 1) {
            // In multiplayer turn-based mode, check if it's this player's turn
            if (!TurnManager.getInstance().canPlayerAct(senderId)) {
                return; // Not this player's turn
            }
        }

        // Get target ID from bundle
        if (!bundle.contains("targetId")) return;
        int targetId = bundle.getInt("targetId");
        if (targetId < 0) return; // Invalid target

        // Find the target actor
        Char target = (Char) Actor.findById(targetId);
        if (target == null) return;

        // Validate the player exists and is in-game
        Hero hero = Multiplayer.Players.getHero(senderId);
        if (hero == null) return;

        // Validate target is in range (basic validation)
        if (!hero.fieldOfView[target.pos]) {
            // Target not in view, reject
            return;
        }

        // Create the attack action
        HeroAction.Attack action = new HeroAction.Attack(target);

        // Store action in player's curAction - the game loop will process it
        hero.curAction = action;

        // Broadcast to all clients so they can see the attack
        broadcast(senderId, targetId);
    }

    private void broadcast(int playerId, int targetId) {
        Bundle bundle = new Bundle();
        bundle.put("playerId", playerId);
        bundle.put("targetId", targetId);

        NetworkManager.BundleMessage msg = new NetworkManager.BundleMessage("PLAYER_ATTACK", playerId);
        msg.bundleData = bundle.toString();

        // Broadcast to all connected clients
        NetworkManager.broadcastMessageServer(msg, null);
    }

    /**
     * CLIENT SIDE: Send attack action to server
     */
    public static void sendAttack(int targetId) {
        if (NetworkManager.getMode() == NetworkManager.Mode.CLIENT
                || NetworkManager.getMode() == NetworkManager.Mode.LOCALHOST) {
            Bundle bundle = new Bundle();
            bundle.put("targetId", targetId);
            NetworkManager.sendMessage("PLAYER_ATTACK", bundle);
        }
    }
}
