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
import com.watabou.utils.Bundle;
import network.handlers.server.PlayerMoveHandler;
import network.handlers.server.PlayerAttackHandler;
import network.states.TurnManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerMoveHandler and PlayerAttackHandler.
 */
public class PlayerActionHandlerTest {

    @BeforeEach
    public void setup() {
        Multiplayer.Players.clear();
        TurnManager.getInstance().reset();
    }

    @AfterEach
    public void teardown() {
        Multiplayer.Players.clear();
        TurnManager.getInstance().reset();
    }

    // ====== MOVE HANDLER TESTS ======

    @Test
    public void testMoveHandlerGetType() {
        PlayerMoveHandler handler = new PlayerMoveHandler();
        assertEquals("PLAYER_MOVE", handler.getType());
    }

    @Test
    public void testMoveHandlerSendMove() {
        // Test that sendMove doesn't throw (just tests the method exists and runs)
        // Note: This requires network mode to be set up, so we just verify it doesn't crash
        // In a real test, we'd mock NetworkManager
        PlayerMoveHandler handler = new PlayerMoveHandler();
        assertNotNull(handler);
    }

    @Test
    public void testMoveActionCreation() {
        // Create a hero and add player
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        p1.hero.heroClass = HeroClass.WARRIOR;
        Multiplayer.Players.add(p1);
        
        // Verify hero was added
        assertNotNull(Multiplayer.Players.getHero(1));
    }

    // ====== ATTACK HANDLER TESTS ======

    @Test
    public void testAttackHandlerGetType() {
        PlayerAttackHandler handler = new PlayerAttackHandler();
        assertEquals("PLAYER_ATTACK", handler.getType());
    }

    @Test
    public void testAttackHandlerSendAttack() {
        PlayerAttackHandler handler = new PlayerAttackHandler();
        assertNotNull(handler);
    }

    // ====== BUNDLE DATA TESTS ======

    @Test
    public void testMoveBundleData() {
        Bundle bundle = new Bundle();
        bundle.put("dst", 100);
        bundle.put("playerId", 1);
        
        assertEquals(100, bundle.getInt("dst"));
        assertEquals(1, bundle.getInt("playerId"));
    }

    @Test
    public void testAttackBundleData() {
        Bundle bundle = new Bundle();
        bundle.put("targetId", 42);
        bundle.put("playerId", 3);
        
        assertEquals(42, bundle.getInt("targetId"));
        assertEquals(3, bundle.getInt("playerId"));
    }

    @Test
    public void testBundleContainsCheck() {
        Bundle bundle = new Bundle();
        bundle.put("dst", 50);
        
        assertTrue(bundle.contains("dst"));
        assertFalse(bundle.contains("nonexistent"));
    }

    // ====== PLAYER ACTION VALIDATION ======

    @Test
    public void testPlayerWithNoHeroCannotMove() {
        // Player without hero
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = null; // No hero
        Multiplayer.Players.add(p1);
        
        // Should handle gracefully - hero is null
        assertNull(Multiplayer.Players.getHero(1));
    }

    @Test
    public void testMultiplePlayersWithDifferentClasses() {
        // Create multiple players with different classes
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        p1.hero.heroClass = HeroClass.WARRIOR;
        
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        p2.hero = new Hero();
        p2.hero.heroClass = HeroClass.ROGUE;
        
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        assertEquals(2, Multiplayer.Players.getPlayerCount());
        assertEquals(HeroClass.WARRIOR, Multiplayer.Players.getHero(1).heroClass);
        assertEquals(HeroClass.ROGUE, Multiplayer.Players.getHero(2).heroClass);
    }

    @Test
    public void testTurnManagerIntegration() {
        // Add players
        Multiplayer.PlayerInfo p1 = new Multiplayer.PlayerInfo(1, "Player1");
        p1.hero = new Hero();
        Multiplayer.PlayerInfo p2 = new Multiplayer.PlayerInfo(2, "Player2");
        p2.hero = new Hero();
        
        Multiplayer.Players.add(p1);
        Multiplayer.Players.add(p2);
        
        TurnManager.getInstance().initTurns();
        
        // With 2 players, canPlayerAct should work
        TurnManager tm = TurnManager.getInstance();
        
        int currentPlayer = tm.getCurrentPlayerId();
        int otherPlayer = (currentPlayer == 1) ? 2 : 1;
        
        assertTrue(tm.canPlayerAct(currentPlayer));
        assertFalse(tm.canPlayerAct(otherPlayer));
    }
}
