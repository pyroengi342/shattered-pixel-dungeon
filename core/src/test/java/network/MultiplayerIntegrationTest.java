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

import network.stub.TestNetworkManager;
import network.handlers.server.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests using stub NetworkManager.
 * Tests the full message flow without requiring real networking or LWJGL.
 */
public class MultiplayerIntegrationTest {

    private TestNetworkManager serverManager;
    private TestNetworkManager clientManager;

    @BeforeEach
    public void setup() {
        serverManager = new TestNetworkManager();
        clientManager = new TestNetworkManager();
        
        serverManager.reset();
        clientManager.reset();
        
        Multiplayer.Players.clear();
        Multiplayer.isMultiplayer = false;
        Multiplayer.isHost = false;
    }

    @AfterEach
    public void teardown() {
        Multiplayer.Players.clear();
    }

    @Test
    public void testServerStartup() {
        serverManager.startServer();
        
        assertEquals(TestNetworkManager.Mode.SERVER, serverManager.getMode());
        assertTrue(Multiplayer.isHost);
        assertTrue(Multiplayer.isMultiplayer);
        
        serverManager.disconnect();
        assertEquals(TestNetworkManager.Mode.NONE, serverManager.getMode());
    }

    @Test
    public void testClientConnection() {
        serverManager.startServer();
        clientManager.connectToServer("localhost");
        
        assertEquals(TestNetworkManager.Mode.SERVER, serverManager.getMode());
        assertEquals(TestNetworkManager.Mode.CLIENT, clientManager.getMode());
        assertTrue(Multiplayer.isMultiplayer);
    }

    @Test
    public void testPlayerReadyMessageFlow() {
        PlayerReadyHandler handler = new PlayerReadyHandler();
        assertEquals("PLAYER_READY", handler.getType());
    }

    @Test
    public void testPlayerReadyStateLogic() {
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(1, "TestPlayer");
        Multiplayer.Players.add(player);
        
        // With 1 player ready, allReady is true (the only player is ready)
        Multiplayer.Players.setReady(1, true);
        assertTrue(Multiplayer.Players.getReady(1));
        assertTrue(Multiplayer.Players.allReady());
        
        // Add second player (not ready)
        Multiplayer.PlayerInfo player2 = new Multiplayer.PlayerInfo(2, "TestPlayer2");
        Multiplayer.Players.add(player2);
        
        // Now allReady should be false (player2 not ready)
        assertFalse(Multiplayer.Players.allReady());
        
        // Ready player2
        Multiplayer.Players.setReady(2, true);
        assertTrue(Multiplayer.Players.allReady());
    }

    @Test
    public void testHeroClassMessageFlow() {
        HeroClassHandler handler = new HeroClassHandler();
        assertEquals("HERO_CLASS", handler.getType());
    }

    @Test
    public void testClientServerDisconnectFlow() {
        serverManager.startServer();
        clientManager.connectToServer("localhost");
        
        assertEquals(TestNetworkManager.Mode.SERVER, serverManager.getMode());
        
        clientManager.disconnect();
        assertEquals(TestNetworkManager.Mode.NONE, clientManager.getMode());
        assertEquals(TestNetworkManager.Mode.SERVER, serverManager.getMode());
        
        serverManager.disconnect();
        assertEquals(TestNetworkManager.Mode.NONE, serverManager.getMode());
    }

    @Test
    public void testMultiplePlayersJoin() {
        serverManager.startServer();
        
        for (int i = 1; i <= 3; i++) {
            Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(i, "Player" + i);
            Multiplayer.Players.add(player);
        }
        
        assertEquals(3, Multiplayer.Players.getPlayerCount());
    }

    @Test
    public void testAllReadyWithMultiplePlayers() {
        for (int i = 1; i <= 4; i++) {
            Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(i, "Player" + i);
            Multiplayer.Players.add(player);
        }
        
        // Initially no one is ready
        assertFalse(Multiplayer.Players.allReady());
        
        // 3 out of 4 ready - not all ready
        for (int i = 1; i <= 3; i++) {
            Multiplayer.Players.setReady(i, true);
        }
        assertFalse(Multiplayer.Players.allReady());
        
        // All 4 ready
        Multiplayer.Players.setReady(4, true);
        assertTrue(Multiplayer.Players.allReady());
    }

    @Test
    public void testMessageSimulation() {
        serverManager.registerTestHandler(new PlayerReadyHandler());
        
        com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
        bundle.put("player_ready", true);
        
        serverManager.simulateMessage("PLAYER_READY", 1, bundle);
        
        assertEquals(1, serverManager.getReceivedMessages().size());
        assertEquals("PLAYER_READY", serverManager.getReceivedMessages().get(0).type);
    }

    @Test
    public void testKickHandlerExists() {
        PlayerKickHandler handler = new PlayerKickHandler();
        assertEquals("PLAYER_KICK", handler.getType());
    }

    @Test
    public void testPlayerGetById() {
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(42, "TestPlayer");
        Multiplayer.Players.add(player);
        
        Multiplayer.PlayerInfo found = Multiplayer.Players.get(42);
        assertNotNull(found);
        assertEquals("TestPlayer", found.name);
        
        Multiplayer.PlayerInfo notFound = Multiplayer.Players.get(999);
        assertNull(notFound);
    }
}
