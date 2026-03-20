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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multiplayer networking logic.
 * These tests focus on game logic without requiring actual network connection.
 * For integration tests with real networking, see IntegrationTestManual.md
 */
public class MultiplayerUnitTest {

    @BeforeEach
    public void setup() {
        // Reset state before each test
        Multiplayer.Players.clear();
        Multiplayer.isMultiplayer = false;
        Multiplayer.isHost = false;
    }

    @AfterEach
    public void teardown() {
        Multiplayer.Players.clear();
        Multiplayer.isMultiplayer = false;
        Multiplayer.isHost = false;
    }

    @Test
    public void testPlayerInfoCreation() {
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(1, "TestPlayer");
        assertNotNull(player);
        assertEquals("TestPlayer", player.name);
        assertEquals(1, player.connectionID);
        assertFalse(player.isReady);
        assertFalse(player.isLocal);
    }

    @Test
    public void testPlayerReadyState() {
        // Create and add test players
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        // Neither ready - allReady should be false
        assertFalse(Multiplayer.Players.allReady());
        
        // First ready
        Multiplayer.Players.setReady(1, true);
        assertTrue(Multiplayer.Players.getReady(1));
        assertFalse(Multiplayer.Players.allReady());
        
        // Both ready
        Multiplayer.Players.setReady(2, true);
        assertTrue(Multiplayer.Players.allReady());
        
        // One not ready again
        Multiplayer.Players.setReady(1, false);
        assertFalse(Multiplayer.Players.allReady());
    }

    @Test
    public void testPlayerCount() {
        assertEquals(0, Multiplayer.Players.getPlayerCount());
        
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(1, "Player1"));
        assertEquals(1, Multiplayer.Players.getPlayerCount());
        
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(2, "Player2"));
        assertEquals(2, Multiplayer.Players.getPlayerCount());
        
        Multiplayer.Players.remove(1);
        assertEquals(1, Multiplayer.Players.getPlayerCount());
    }

    @Test
    public void testMultiplayerFlags() {
        assertFalse(Multiplayer.isMultiplayer);
        assertFalse(Multiplayer.isHost);
        
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = true;
        
        assertTrue(Multiplayer.isMultiplayer);
        assertTrue(Multiplayer.isHost);
    }

    @Test
    public void testGetAllPlayers() {
        assertTrue(Multiplayer.Players.getAll().isEmpty());
        
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(1, "Player1"));
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(2, "Player2"));
        
        assertEquals(2, Multiplayer.Players.getAll().size());
    }

    @Test
    public void testGetPlayerById() {
        Multiplayer.PlayerInfo player1 = new Multiplayer.PlayerInfo(100, "TestPlayer");
        Multiplayer.Players.add(player1);
        
        Multiplayer.PlayerInfo found = Multiplayer.Players.get(100);
        assertNotNull(found);
        assertEquals("TestPlayer", found.name);
        
        Multiplayer.PlayerInfo notFound = Multiplayer.Players.get(999);
        assertNull(notFound);
    }

    @Test
    public void testNetworkManagerInitialized() {
        NetworkManager manager = NetworkManager.getInstance();
        assertNotNull(manager);
    }

    @Test
    public void testModeNoneByDefault() {
        NetworkManager.Mode mode = NetworkManager.getMode();
        assertEquals(NetworkManager.Mode.NONE, mode);
    }

    @Test
    public void testPlayerNotReadyInitially() {
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(1, "Player1"));
        assertFalse(Multiplayer.Players.getReady(1));
        assertFalse(Multiplayer.Players.allReady());
    }

    @Test
    public void testMultipleReadyToggle() {
        for (int i = 1; i <= 4; i++) {
            Multiplayer.Players.add(new Multiplayer.PlayerInfo(i, "Player" + i));
        }
        assertEquals(4, Multiplayer.Players.getPlayerCount());
        
        // Set all but last ready
        for (int i = 1; i <= 3; i++) {
            Multiplayer.Players.setReady(i, true);
            assertFalse(Multiplayer.Players.allReady());
        }
        
        // Set last ready - now all ready
        Multiplayer.Players.setReady(4, true);
        assertTrue(Multiplayer.Players.allReady());
        
        // Unready first - not all ready
        Multiplayer.Players.setReady(1, false);
        assertFalse(Multiplayer.Players.allReady());
    }

    @Test
    public void testIsHostFlag() {
        assertFalse(Multiplayer.isHost);
        
        Multiplayer.isHost = true;
        assertTrue(Multiplayer.isHost);
        
        Multiplayer.isHost = false;
        assertFalse(Multiplayer.isHost);
    }

    @Test
    public void testPlayerLocalFlag() {
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(1, "LocalPlayer");
        assertFalse(player.isLocal);
        
        player.isLocal = true;
        assertTrue(player.isLocal);
    }

    @Test
    public void testClearPlayers() {
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(1, "Player1"));
        Multiplayer.Players.add(new Multiplayer.PlayerInfo(2, "Player2"));
        assertEquals(2, Multiplayer.Players.getPlayerCount());
        
        Multiplayer.Players.clear();
        assertEquals(0, Multiplayer.Players.getPlayerCount());
        assertTrue(Multiplayer.Players.getAll().isEmpty());
    }
}
