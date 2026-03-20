#!/usr/bin/env python3
"""
Game Helper Functions for Airtest SPD Testing
Provides game-specific UI element definitions and helpers.
"""

import os
import sys
import time

# Airtest imports
from airtest.core.api import *
from airtest.core.cv import Template

# Project paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(SCRIPT_DIR, "assets")


# =============================================================================
# Template Definitions - Hero Select Scene
# =============================================================================


class HeroSelectAssets:
    """Asset paths for HeroSelectScene UI elements."""

    # Scene identifiers
    SCENE_TITLE = "title_scene"
    HERO_SELECT = "hero_select_title"

    # Hero buttons
    HERO_WARRIOR = "hero_warrior"
    HERO_MAGE = "hero_mage"
    HERO_ROGUE = "hero_rogue"
    HERO_HUNTRESS = "hero_huntress"

    # Action buttons
    PLAY_BUTTON = "play_button"
    START_BUTTON = "start_button"
    READY_BUTTON = "ready_button"
    OPTIONS_BUTTON = "options_button"

    # Multiplayer elements
    MULTIPLAYER_BUTTON = "multiplayer_button"
    MULTIPLAYER_WINDOW = "multiplayer_window"

    @classmethod
    def template(cls, name, **kwargs):
        """Get template path for an asset."""
        path = os.path.join(ASSETS_DIR, f"{name}.png")
        if os.path.exists(path):
            return Template(path, **kwargs)
        # Fallback to built-in assets
        return Template(f"{name}.png", **kwargs)


# =============================================================================
# Hero Class Helpers
# =============================================================================

HERO_CLASSES = {
    "warrior": HeroSelectAssets.HERO_WARRIOR,
    "mage": HeroSelectAssets.HERO_MAGE,
    "rogue": HeroSelectAssets.HERO_ROGUE,
    "huntress": HeroSelectAssets.HERO_HUNTRESS,
}


def select_hero(hero_class: str, timeout: float = 10) -> bool:
    """
    Select a hero class from the HeroSelectScene.

    Args:
        hero_class: One of 'warrior', 'mage', 'rogue', 'huntress'
        timeout: Maximum time to wait for elements

    Returns:
        True if hero was selected successfully
    """
    hero_key = hero_class.lower()
    if hero_key not in HERO_CLASSES:
        raise ValueError(f"Unknown hero class: {hero_class}")

    template_name = HERO_CLASSES[hero_key]
    template = HeroSelectAssets.template(template_name, threshold=0.8)

    # Click on hero
    if exists(template):
        touch(template)
        time.sleep(0.5)

        # Verify start button appears
        start_template = HeroSelectAssets.template(HeroSelectAssets.START_BUTTON)
        if wait(start_template, timeout=timeout):
            return True

    return False


def is_hero_selected(hero_class: str) -> bool:
    """Check if a specific hero is currently selected."""
    template_name = HERO_CLASSES.get(hero_class.lower())
    if template_name:
        return exists(HeroSelectAssets.template(template_name))
    return False


def wait_for_hero_select(timeout: float = 10) -> bool:
    """Wait for hero select scene to load."""
    template = HeroSelectAssets.template(HeroSelectAssets.HERO_SELECT)
    return wait(template, timeout=timeout) is not None


# =============================================================================
# Multiplayer Lobby Helpers
# =============================================================================


def open_multiplayer_menu():
    """Open the multiplayer connection menu."""
    # Click options
    options = HeroSelectAssets.template(HeroSelectAssets.OPTIONS_BUTTON)
    if exists(options):
        touch(options)
        time.sleep(0.3)

    # Click multiplayer
    mp_button = HeroSelectAssets.template(HeroSelectAssets.MULTIPLAYER_BUTTON)
    if exists(mp_button):
        touch(mp_button)
        time.sleep(0.5)

    # Verify window opened
    return exists(HeroSelectAssets.template(HeroSelectAssets.MULTIPLAYER_WINDOW))


def click_ready_button():
    """Click the Ready button (for multiplayer clients)."""
    ready = HeroSelectAssets.template(HeroSelectAssets.READY_BUTTON)
    if exists(ready):
        touch(ready)
        return True
    return False


def is_start_button_enabled() -> bool:
    """Check if the Start button is enabled (green/highlighted)."""
    start = HeroSelectAssets.template(HeroSelectAssets.START_BUTTON)
    # Check for enabled state (may need color threshold)
    return exists(start)


def wait_for_players(count: int, timeout: float = 30) -> bool:
    """
    Wait for a specific number of players to join.

    Args:
        count: Number of players to wait for
        timeout: Maximum time to wait

    Returns:
        True if correct number of players detected
    """
    start_time = time.time()

    while time.time() - start_time < timeout:
        # This would require visual detection of player count
        # Placeholder for actual implementation
        if check_player_count() >= count:
            return True
        time.sleep(1)

    return False


def check_player_count() -> int:
    """
    Check how many players are currently in the lobby.
    Returns estimated count based on visible UI elements.
    """
    # Placeholder - would need actual visual detection
    return 0


# =============================================================================
# Game Navigation Helpers
# =============================================================================


def navigate_to_hero_select():
    """Navigate from title screen to hero select."""
    # Click play
    play = HeroSelectAssets.template(HeroSelectAssets.PLAY_BUTTON)
    if exists(play):
        touch(play)

    # Wait for hero select
    return wait_for_hero_select()


def start_single_player_game():
    """Start a single player game."""
    # Ensure hero is selected
    if not exists(HeroSelectAssets.template(HeroSelectAssets.START_BUTTON)):
        return False

    # Click start
    touch(HeroSelectAssets.template(HeroSelectAssets.START_BUTTON))
    time.sleep(1)

    # Wait for loading/game start
    return True


# =============================================================================
# Utility Functions
# =============================================================================


def take_screenshot(name: str = None) -> str:
    """Take a screenshot and save it."""
    if name is None:
        name = f"screenshot_{int(time.time())}"

    path = os.path.join(SCRIPT_DIR, "screenshots", f"{name}.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)

    snapshot(filename=path)
    return path


def log_state(message: str):
    """Log current game state."""
    print(f"[SPD] {message}")


# =============================================================================
# Test Fixtures
# =============================================================================


def setup_game_for_test():
    """Setup game state for testing."""
    log_state("Setting up game for test...")

    # Navigate to hero select
    navigate_to_hero_select()
    log_state("Navigated to hero select")

    return True


def teardown_test():
    """Cleanup after test."""
    log_state("Cleaning up test...")
    # Return to title if needed
    pass
