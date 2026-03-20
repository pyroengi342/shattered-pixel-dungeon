# -*- encoding=utf8 -*-
"""
HeroSelectScene UI Tests
Tests for multiplayer lobby interface in Shattered Pixel Dungeon
"""

__author__ = "SPD Test Team"
__version__ = "1.0.0"

from airtest.core.api import *
from airtest.core.cv import Template
import sys
import os

# Add helpers to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

try:
    from game_helper import (
        HeroSelectAssets,
        select_hero,
        wait_for_hero_select,
        navigate_to_hero_select,
        click_ready_button,
        take_screenshot,
        log_state,
    )
except ImportError:
    log_state("Warning: game_helper not loaded, using defaults")


def test_hero_select_scene_loads():
    """
    Test that hero select scene loads correctly.
    """
    log_state("Test: Hero select scene loads")

    # Navigate to hero select
    assert navigate_to_hero_select(), "Failed to navigate to hero select"

    # Take screenshot
    take_screenshot("hero_select_loaded")

    return True


def test_hero_selection():
    """
    Test that all hero classes can be selected.
    """
    log_state("Test: Hero selection")

    # Test each hero class
    heroes = ["warrior", "mage", "rogue", "huntress"]

    for hero in heroes:
        log_state(f"Selecting hero: {hero}")
        assert select_hero(hero), f"Failed to select {hero}"
        take_screenshot(f"hero_selected_{hero}")

    return True


def test_multiplayer_ready_button():
    """
    Test ready button behavior in multiplayer.
    Note: Requires multiplayer session to be active.
    """
    log_state("Test: Ready button")

    # This test would require a running multiplayer session
    # Placeholder for actual implementation

    return True


def test_start_button_state():
    """
    Test that start button state changes based on player readiness.
    """
    log_state("Test: Start button state")

    # Select a hero first
    select_hero("warrior")

    # Start button should be visible
    start_btn = Template("start_button.png", threshold=0.8)
    assert exists(start_btn), "Start button not visible after hero selection"

    take_screenshot("start_button_visible")

    return True


# =============================================================================
# Test Suite Runner
# =============================================================================


def run_all_tests():
    """Run all test cases."""
    tests = [
        test_hero_select_scene_loads,
        test_hero_selection,
        test_start_button_state,
        test_multiplayer_ready_button,
    ]

    passed = 0
    failed = 0

    for test in tests:
        try:
            log_state(f"Running: {test.__name__}")
            if test():
                passed += 1
                log_state(f"PASSED: {test.__name__}")
            else:
                failed += 1
                log_state(f"FAILED: {test.__name__}")
        except Exception as e:
            failed += 1
            log_state(f"ERROR in {test.__name__}: {e}")
            take_screenshot(f"error_{test.__name__}")

    log_state(f"\n{'=' * 50}")
    log_state(f"Results: {passed} passed, {failed} failed")
    log_state(f"{'=' * 50}")

    return failed == 0


if __name__ == "__main__":
    auto_setup(__file__, devices=["Windows:///"], logdir=True)
    run_all_tests()
