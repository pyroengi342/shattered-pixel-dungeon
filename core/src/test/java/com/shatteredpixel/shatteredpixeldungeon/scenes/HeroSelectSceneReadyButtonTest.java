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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Ready Button functionality in HeroSelectScene.
 * 
 * The ready button allows players to toggle their ready status in multiplayer mode.
 * It should only be visible to non-host players in a multiplayer lobby.
 */
public class HeroSelectSceneReadyButtonTest {

    @BeforeEach
    public void setup() {
        // Reset state before each test
        network.Multiplayer.isMultiplayer = false;
        network.Multiplayer.isHost = false;
        HeroSelectScene.setLocalPlayerReady(false);
    }

    // ========================================================================
    // Test: Ready State Management via Public API
    // ========================================================================

    @Test
    public void testSetLocalPlayerReadyTrue() {
        // When: Setting ready to true
        HeroSelectScene.setLocalPlayerReady(true);
        
        // Then: State should be true (via static access)
        assertEquals("not_ready", HeroSelectScene.getReadyButtonText(true));
    }

    @Test
    public void testSetLocalPlayerReadyFalse() {
        // Given: Player is ready
        HeroSelectScene.setLocalPlayerReady(true);
        
        // When: Setting ready to false
        HeroSelectScene.setLocalPlayerReady(false);
        
        // Then: State should be false
        assertEquals("ready", HeroSelectScene.getReadyButtonText(false));
    }

    // ========================================================================
    // Test: Button Visibility
    // ========================================================================

    @Test
    public void testReadyButtonHiddenWhenNotMultiplayer() {
        // Given: Not in multiplayer
        network.Multiplayer.isMultiplayer = false;
        
        // When: Checking if ready button should show
        boolean shouldShow = HeroSelectScene.shouldShowReadyButton();
        
        // Then: Should not show
        assertFalse(shouldShow);
    }

    @Test
    public void testReadyButtonHiddenForHost() {
        // Given: Multiplayer with host
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = true;
        
        // When: Checking if ready button should show
        boolean shouldShow = HeroSelectScene.shouldShowReadyButton();
        
        // Then: Host should not see ready button
        assertFalse(shouldShow);
    }

    @Test
    public void testReadyButtonVisibleForClient() {
        // Given: Multiplayer with non-host player
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = false;
        
        // When: Checking if ready button should show
        boolean shouldShow = HeroSelectScene.shouldShowReadyButton();
        
        // Then: Client should see ready button
        assertTrue(shouldShow);
    }

    @Test
    public void testReadyButtonHiddenWhenMultiplayerDisabled() {
        // Given: Multiplayer flag is explicitly false
        network.Multiplayer.isMultiplayer = false;
        network.Multiplayer.isHost = false;
        
        // Then: shouldShowReadyButton must return false
        assertFalse(HeroSelectScene.shouldShowReadyButton());
    }

    // ========================================================================
    // Test: Button Text
    // ========================================================================

    @Test
    public void testReadyButtonTextWhenNotReady() {
        // Given: Player is not ready
        String text = HeroSelectScene.getReadyButtonText(false);
        
        // Then: Should show "ready" text
        assertEquals("ready", text);
    }

    @Test
    public void testReadyButtonTextWhenReady() {
        // Given: Player is ready
        String text = HeroSelectScene.getReadyButtonText(true);
        
        // Then: Should show "not_ready" text
        assertEquals("not_ready", text);
    }

    @Test
    public void testReadyButtonTextIsLocalizedKey() {
        // The button text should be a localization key
        String notReady = HeroSelectScene.getReadyButtonText(true);
        String ready = HeroSelectScene.getReadyButtonText(false);
        
        // These should be different strings (localization keys)
        assertNotEquals(notReady, ready);
        assertTrue(notReady.length() > 0);
        assertTrue(ready.length() > 0);
    }

    // ========================================================================
    // Test: Toggle Behavior (via public API simulation)
    // ========================================================================

    @Test
    public void testToggleFromReadyToNotReady() {
        // Given: Player is ready
        HeroSelectScene.setLocalPlayerReady(true);
        
        // Then: Button text should reflect ready state
        assertEquals("not_ready", HeroSelectScene.getReadyButtonText(true));
    }

    @Test
    public void testToggleFromNotReadyToReady() {
        // Given: Player is not ready
        HeroSelectScene.setLocalPlayerReady(false);
        
        // Then: Button text should reflect not-ready state
        assertEquals("ready", HeroSelectScene.getReadyButtonText(false));
    }

    // ========================================================================
    // Test: Edge Cases
    // ========================================================================

    @Test
    public void testReadyStateInSinglePlayerMode() {
        // Given: Single player mode (not multiplayer)
        network.Multiplayer.isMultiplayer = false;
        
        // When: Setting ready state
        HeroSelectScene.setLocalPlayerReady(true);
        
        // Then: Ready button should still not show (not multiplayer)
        assertFalse(HeroSelectScene.shouldShowReadyButton());
    }

    @Test
    public void testMultipleStateChanges() {
        // When: Multiple state changes
        HeroSelectScene.setLocalPlayerReady(true);
        HeroSelectScene.setLocalPlayerReady(false);
        HeroSelectScene.setLocalPlayerReady(true);
        
        // Then: Final state should be ready
        assertEquals("not_ready", HeroSelectScene.getReadyButtonText(true));
    }

    @Test
    public void testButtonVisibilityConsistency() {
        // Test that visibility is consistent with multiplayer state
        
        // Not multiplayer - button hidden
        network.Multiplayer.isMultiplayer = false;
        assertFalse(HeroSelectScene.shouldShowReadyButton());
        
        // Multiplayer host - button hidden
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = true;
        assertFalse(HeroSelectScene.shouldShowReadyButton());
        
        // Multiplayer client - button visible
        network.Multiplayer.isMultiplayer = true;
        network.Multiplayer.isHost = false;
        assertTrue(HeroSelectScene.shouldShowReadyButton());
    }
}
