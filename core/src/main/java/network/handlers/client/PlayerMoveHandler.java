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

package network.handlers.client;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import network.Multiplayer;
import network.NetworkManager;
import network.handlers.MessageHandler;

/**
 * Client-side handler for player movement broadcasts.
 * Receives MOVE broadcasts from server and applies to local hero representation.
 */
public class PlayerMoveHandler implements MessageHandler {

    @Override
    public String getType() {
        return "PLAYER_MOVE";
    }

    @Override
    public void msgHandle(int senderId, Bundle bundle) {
        // Run on render thread for thread safety
        Game.runOnRenderThread(() -> {
            // Don't process our own movements (we handle them locally)
            if (senderId == NetworkManager.getLocalPlayerId()) {
                return;
            }

            if (!bundle.contains("dst")) return;
            int dst = bundle.getInt("dst");
            if (dst < 0) return;

            // Get the hero for this player
            Hero hero = Multiplayer.Players.getHero(senderId);
            if (hero == null) return;

            // Create and set the move action - game loop will process it
            hero.curAction = new HeroAction.Move(dst);
        });
    }
}
