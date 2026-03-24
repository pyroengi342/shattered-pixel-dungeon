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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import network.states.TurnManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TurnManager and turn-based gameplay logic.
 */
public class TurnManagerTest {

    @BeforeEach
    public void setup() {
        // Reset state
        Multiplayer.Players.clear();
        TurnManager.getInstance().reset();
    }

    @AfterEach
    public void teardown() {
        TurnManager.getInstance().reset();
        Multiplayer.Players.clear();
    }

    @Test
    public void testTurnManagerSingleton() {
        TurnManager tm1 = TurnManager.getInstance();
        TurnManager tm2 = TurnManager.getInstance();
        assertSame(tm1, tm2);
    }

    @Test
    public void testInitialState() {
        TurnManager tm = TurnManager.getInstance();
        assertEquals(0, tm.getPlayerCount());
        assertEquals(-1, tm.getCurrentPlayerId());
        assertEquals(1, tm.getTurnNumber());
        assertFalse(tm.isTurnInProgress());
    }

    @Test
    public void testInitTurnsWithNoPlayers() {
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        assertEquals(0, tm.getPlayerCount());
    }

    @Test
    public void testInitTurnsWithPlayers() {
        // Add alive players with heroes
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        p1.hero.heroClass = HeroClass.WARRIOR;
        
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        p2.hero = new Hero();
        p2.hero.heroClass = HeroClass.MAGE;
        
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        assertEquals(2, tm.getPlayerCount());
        assertTrue(tm.getCurrentPlayerId() == 1 || tm.getCurrentPlayerId() == 2);
    }

    @Test
    public void testIsPlayerTurn() {
        // Add a single player
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        assertTrue(tm.isPlayerTurn(1));
        assertFalse(tm.isPlayerTurn(2));
    }

    @Test
    public void testCanPlayerAct() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        
        // Should be able to act - it's this player's turn and no action in progress
        assertTrue(tm.canPlayerAct(1));
        
        // Not this player's turn
        assertFalse(tm.canPlayerAct(2));
    }

    @Test
    public void testPlayerStartedAction() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        
        // Start an action
        boolean started = tm.playerStartedAction(1);
        assertTrue(started);
        assertTrue(tm.isTurnInProgress());
        
        // Cannot start another action while one is in progress
        started = tm.playerStartedAction(1);
        assertFalse(started); // Should fail because turn already in progress
    }

    @Test
    public void testEndTurn() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        p2.hero = new Hero();
        
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        int firstPlayer = tm.getCurrentPlayerId();
        
        // Start and end turn
        tm.playerStartedAction(firstPlayer);
        tm.endTurn();
        
        // Should be next player's turn
        assertFalse(tm.isTurnInProgress());
        assertNotEquals(firstPlayer, tm.getCurrentPlayerId());
    }

    @Test
    public void testTurnNumberIncrements() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        assertEquals(1, tm.getTurnNumber());
        
        // With only 1 player, wrapping around increments turn
        tm.playerStartedAction(1);
        tm.endTurn();
        
        assertEquals(2, tm.getTurnNumber());
    }

    @Test
    public void testCannotActWhenNotYourTurn() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        p2.hero = new Hero();
        
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        int currentPlayer = tm.getCurrentPlayerId();
        int otherPlayer = (currentPlayer == 1) ? 2 : 1;
        
        // Cannot act when it's not your turn
        assertFalse(tm.canPlayerAct(otherPlayer));
    }

    @Test
    public void testResetClearsState() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        assertTrue(tm.getPlayerCount() > 0);
        
        tm.reset();
        
        assertEquals(0, tm.getPlayerCount());
        assertEquals(1, tm.getTurnNumber());
        assertFalse(tm.isTurnInProgress());
    }

    @Test
    public void testTimerUpdate() {
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.Players.add(p1);
        
        TurnManager.getInstance().initTurns();
        
        TurnManager tm = TurnManager.getInstance();
        
        // Start a turn to enable timer
        tm.playerStartedAction(1);
        
        float initialTime = tm.getTimeRemaining();
        
        // Advance time
        tm.update(1.0f);
        
        // Timer should decrease when turn is in progress
        assertTrue(tm.getTimeRemaining() < initialTime);
    }
}
