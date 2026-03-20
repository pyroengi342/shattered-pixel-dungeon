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

package network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multiplayer lobby functionality.
 */
public class LobbyTest extends NetworkTestBase {

    @Test
    public void testNetworkManagerInitialized() {
        // Test that NetworkManager can be accessed
        assertNotNull(NetworkManager.getInstance());
    }

    @Test
    public void testModeNoneByDefault() {
        // Test initial state
        NetworkManager.Mode mode = NetworkManager.getMode();
        assertEquals(NetworkManager.Mode.NONE, mode);
    }

    @Test
    public void testMultiplayerFlagsOff() {
        // Test multiplayer flags initially false
        assertFalse(Multiplayer.isMultiplayer);
        assertFalse(Multiplayer.isHost);
    }

    @Test
    public void testPlayerCountZeroInitially() {
        // Test player count
        assertEquals(0, Multiplayer.Players.getPlayerCount());
    }
}
