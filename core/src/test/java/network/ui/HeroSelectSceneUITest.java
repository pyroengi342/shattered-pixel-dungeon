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

package network.ui;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import network.Multiplayer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for HeroSelectScene multiplayer functionality.
 * Tests the logic methods without requiring full libGDX rendering.
 */
public class HeroSelectSceneUITest {

    @BeforeEach
    public void setup() {
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

    // ==================== Hero Class Availability Tests ====================

    @Test
    public void testClassAvailableWhenNoMultiplayer() {
        // When not in multiplayer, all classes should be available
        Multiplayer.Players.clear();
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.WARRIOR));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.MAGE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.ROGUE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.HUNTRESS));
    }

    @Test
    public void testClassUnavailableWhenTakenByOtherPlayer() {
        // Set up multiplayer with one player having selected a class
        Multiplayer.isMultiplayer = true;
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(1, "Player1");
        player.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player.hero.heroClass = HeroClass.WARRIOR;
        Multiplayer.Players.add(player);

        // Warrior should be unavailable
        assertFalse(HeroSelectScene.isClassAvailableLogic(HeroClass.WARRIOR));
        // Other classes should be available
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.MAGE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.ROGUE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.HUNTRESS));
    }

    @Test
    public void testClassAvailableWhenPlayerHasNoHero() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.PlayerInfo player = new Multiplayer.PlayerInfo(1, "Player1");
        // player.hero is null - no class selected yet
        Multiplayer.Players.add(player);

        // All classes should be available
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.WARRIOR));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.MAGE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.ROGUE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.HUNTRESS));
    }

    @Test
    public void testMultiplePlayersDifferentClasses() {
        Multiplayer.isMultiplayer = true;

        Multiplayer.PlayerInfo player1 = new Multiplayer.PlayerInfo(1, "Player1");
        player1.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player1.hero.heroClass = HeroClass.WARRIOR;
        Multiplayer.Players.add(player1);

        Multiplayer.PlayerInfo player2 = new Multiplayer.PlayerInfo(2, "Player2");
        player2.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player2.hero.heroClass = HeroClass.MAGE;
        Multiplayer.Players.add(player2);

        // Warrior and Mage should be unavailable
        assertFalse(HeroSelectScene.isClassAvailableLogic(HeroClass.WARRIOR));
        assertFalse(HeroSelectScene.isClassAvailableLogic(HeroClass.MAGE));
        // Rogue and Huntress should be available
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.ROGUE));
        assertTrue(HeroSelectScene.isClassAvailableLogic(HeroClass.HUNTRESS));
    }

    // ==================== Ready Button Visibility Tests ====================

    @Test
    public void testReadyButtonShownForClient() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = false;

        assertTrue(HeroSelectScene.shouldShowReadyButton());
        assertFalse(HeroSelectScene.shouldShowStartButton());
    }

    @Test
    public void testReadyButtonHiddenForHost() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = true;

        assertFalse(HeroSelectScene.shouldShowReadyButton());
        assertTrue(HeroSelectScene.shouldShowStartButton());
    }

    @Test
    public void testReadyButtonHiddenWhenNotMultiplayer() {
        Multiplayer.isMultiplayer = false;
        Multiplayer.isHost = false;

        // Ready button should be hidden (only shown for clients in multiplayer)
        assertFalse(HeroSelectScene.shouldShowReadyButton());
        // Start button should be shown (for single-player game start)
        assertTrue(HeroSelectScene.shouldShowStartButton());
    }

    // ==================== Start Button State Tests ====================

    @Test
    public void testStartButtonEnabledWhenAllPlayersReady() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = true;

        Multiplayer.PlayerInfo player1 = new Multiplayer.PlayerInfo(1, "Player1");
        player1.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player1.hero.heroClass = HeroClass.WARRIOR;
        player1.isReady = true;
        Multiplayer.Players.add(player1);

        Multiplayer.PlayerInfo player2 = new Multiplayer.PlayerInfo(2, "Player2");
        player2.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player2.hero.heroClass = HeroClass.MAGE;
        player2.isReady = true;
        Multiplayer.Players.add(player2);

        assertTrue(HeroSelectScene.shouldEnableStartButton());
    }

    @Test
    public void testStartButtonDisabledWhenNotAllPlayersReady() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = true;

        Multiplayer.PlayerInfo player1 = new Multiplayer.PlayerInfo(1, "Player1");
        player1.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player1.hero.heroClass = HeroClass.WARRIOR;
        player1.isReady = true;
        Multiplayer.Players.add(player1);

        Multiplayer.PlayerInfo player2 = new Multiplayer.PlayerInfo(2, "Player2");
        player2.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
        player2.hero.heroClass = HeroClass.MAGE;
        player2.isReady = false; // Not ready
        Multiplayer.Players.add(player2);

        assertFalse(HeroSelectScene.shouldEnableStartButton());
    }

    @Test
    public void testStartButtonDisabledForClient() {
        Multiplayer.isMultiplayer = true;
        Multiplayer.isHost = false;

        // Client should not be able to start the game
        assertFalse(HeroSelectScene.shouldEnableStartButton());
    }

    // ==================== Player List Visibility Tests ====================

    @Test
    public void testPlayerListShownInMultiplayer() {
        Multiplayer.isMultiplayer = true;

        assertTrue(HeroSelectScene.shouldShowPlayerList());
    }

    @Test
    public void testPlayerListHiddenWhenNotMultiplayer() {
        Multiplayer.isMultiplayer = false;

        assertFalse(HeroSelectScene.shouldShowPlayerList());
    }

    // ==================== Ready Button Text Tests ====================

    @Test
    public void testReadyButtonTextReady() {
        // When player is ready
        assertEquals("not_ready", HeroSelectScene.getReadyButtonText(true));
    }

    @Test
    public void testReadyButtonTextNotReady() {
        // When player is not ready
        assertEquals("ready", HeroSelectScene.getReadyButtonText(false));
    }
}
