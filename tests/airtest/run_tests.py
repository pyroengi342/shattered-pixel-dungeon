#!/usr/bin/env python3
"""
Airtest UI Test Runner for Shattered Pixel Dungeon
Run automated UI tests on the desktop build.
"""

import os
import sys
import argparse
import subprocess
import time
import signal

# Add airtest to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from airtest.core.api import *

# Game configuration
GAME_TITLE = "Shattered Pixel Dungeon"
DEFAULT_PORT = 54556
GAME_ARGS = []


def parse_args():
    parser = argparse.ArgumentParser(description="Run Airtest UI tests for SPD")
    parser.add_argument("--platform", default="desktop", choices=["desktop", "android"])
    parser.add_argument("--device", default=None, help="Device URI (e.g., Android:///)")
    parser.add_argument(
        "--headless", action="store_true", help="Run in headless mode with Xvfb"
    )
    parser.add_argument("--game-path", default=None, help="Path to game executable")
    parser.add_argument(
        "--no-start",
        action="store_true",
        help="Don't start game, assume already running",
    )
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    return parser.parse_args()


def start_game_headless(game_path=None):
    """Start game in headless Xvfb environment."""
    print("[*] Starting game in headless mode...")

    # Find game executable if not specified
    if game_path is None:
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        game_path = os.path.join(project_root, "desktop", "build", "libs")

        # Look for JAR file
        for f in os.listdir(game_path):
            if f.endswith(".jar"):
                game_path = os.path.join(game_path, f)
                break

    if not os.path.exists(game_path):
        print(f"[!] Game not found at: {game_path}")
        return None

    # Start game with Xvfb
    cmd = ["xvfb-run", "-a", "java", "-jar", game_path]

    proc = subprocess.Popen(
        cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, preexec_fn=os.setsid
    )

    print(f"[*] Game started with PID: {proc.pid}")
    time.sleep(3)  # Wait for game to initialize

    return proc


def stop_game(process):
    """Stop the game process."""
    if process:
        print("[*] Stopping game...")
        try:
            os.killpg(os.getpgid(process.pid), signal.SIGTERM)
            process.wait(timeout=5)
        except:
            try:
                os.killpg(os.getpgid(process.pid), signal.SIGKILL)
            except:
                pass


def connect_to_device(platform, device_uri=None):
    """Connect to the target device."""
    print(f"[*] Connecting to {platform} device...")

    if device_uri:
        connect_device(device_uri)
    elif platform == "desktop":
        # Connect to Windows desktop
        connect_device("Windows:///")
    elif platform == "android":
        connect_device("Android:///")

    print("[*] Device connected.")


def run_hero_select_tests():
    """Run HeroSelectScene UI tests."""
    print("\n" + "=" * 50)
    print("Running HeroSelectScene UI Tests")
    print("=" * 50 + "\n")

    # Load the test script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    script_path = os.path.join(script_dir, "hero_select_scene.air")

    if not os.path.exists(script_path):
        print(f"[!] Test script not found: {script_path}")
        print("[*] Creating default test script...")
        create_default_tests(script_dir)

    # Run tests using Airtest runner
    from airtest.core.api import auto_setup

    auto_setup(__file__, devices=["Windows://"], project_root=script_dir, logdir=True)

    # Import and run test cases
    try:
        from airtest.core.helper import G

        # Test 1: Verify game started
        print("\n[*] Test 1: Verify game started...")
        assert exists(Template("title_scene.png", threshold=0.8)), (
            "Title scene not found"
        )
        print("[PASS] Title scene visible")

        # Test 2: Click "Play" button
        print("\n[*] Test 2: Navigate to Hero Select...")
        touch(Template("play_button.png", threshold=0.8))
        wait(Template("hero_select_title.png", threshold=0.8))
        print("[PASS] Hero Select scene loaded")

        # Test 3: Verify hero buttons visible
        print("\n[*] Test 3: Verify hero class buttons...")
        for hero in ["warrior", "mage", "rogue", "huntress"]:
            assert exists(Template(f"hero_{hero}.png", threshold=0.7)), (
                f"Hero {hero} not found"
            )
        print("[PASS] All hero buttons visible")

        # Test 4: Select a hero
        print("\n[*] Test 4: Select hero...")
        touch(Template("hero_warrior.png", threshold=0.7))
        wait(Template("start_button.png", threshold=0.8))
        print("[PASS] Hero selected, start button visible")

        print("\n" + "=" * 50)
        print("All UI Tests PASSED!")
        print("=" * 50)
        return True

    except AssertionError as e:
        print(f"\n[FAIL] {e}")
        return False
    except Exception as e:
        print(f"\n[ERROR] {e}")
        return False


def create_default_tests(script_dir):
    """Create default test structure if none exists."""
    test_script = os.path.join(script_dir, "hero_select_scene.air")
    os.makedirs(test_script, exist_ok=True)

    # Create minimal test script
    with open(os.path.join(test_script, "main.py"), "w") as f:
        f.write("""
# HeroSelectScene UI Tests
# This script tests the multiplayer lobby UI

from airtest.core.api import *

def test_hero_selection():
    '''Test hero class selection'''
    # Navigate to hero select
    touch(Template("play_button.png"))
    wait(Template("hero_select.png"))
    
    # Select warrior
    touch(Template("hero_warrior.png"))
    assert exists(Template("start_button.png"))
    
def test_multiplayer_button():
    '''Test multiplayer button in options'''
    touch(Template("options_button.png"))
    touch(Template("multiplayer_button.png"))
    assert exists(Template("multiplayer_window.png"))

if __name__ == "__main__":
    auto_setup(__file__)
    test_hero_selection()
    test_multiplayer_button()
""")


def main():
    args = parse_args()

    print("=" * 50)
    print("Shattered Pixel Dungeon - Airtest UI Runner")
    print("=" * 50)

    game_proc = None

    try:
        # Setup
        if not args.no_start and args.platform == "desktop":
            if args.headless:
                game_proc = start_game_headless(args.game_path)
            else:
                print("[*] Please start the game manually, then press Enter...")
                input()

        # Connect to device
        connect_to_device(args.platform, args.device)

        # Run tests
        success = run_hero_select_tests()

        return 0 if success else 1

    finally:
        if game_proc:
            stop_game(game_proc)


if __name__ == "__main__":
    sys.exit(main())
