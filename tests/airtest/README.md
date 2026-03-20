# Airtest UI Tests for Shattered Pixel Dungeon

Automated UI testing for the multiplayer lobby interface using Airtest.

## Prerequisites

- Python 3.7+
- Java 17+
- Desktop build of Shattered Pixel Dungeon

### Optional (for headless testing)
```bash
sudo apt-get install xvfb  # Linux only
```

## Installation

```bash
# Install Airtest
pip install airtest

# Install additional dependencies for Unity/Android testing (optional)
pip install pocoui unity3d-player
```

## Running Tests

### Interactive Desktop Test
```bash
cd tests/airtest
python3 run_tests.py --platform desktop
```

### Headless Test (Linux with Xvfb)
```bash
xvfb-run -a python3 run_tests.py --platform desktop --headless
```

### With Custom Game Path
```bash
python3 run_tests.py --platform desktop --game-path /path/to/game.jar
```

### Skip Game Start (already running)
```bash
python3 run_tests.py --no-start
```

## Test Structure

```
tests/airtest/
├── README.md              # This file
├── run_tests.py           # Main test runner
├── game_helper.py         # Game-specific helper functions
├── settings.json          # Airtest project config
├── assets/               # UI screenshot templates
│   ├── hero_warrior.png
│   ├── hero_mage.png
│   └── ...
├── screenshots/           # Captured screenshots
├── hero_select_scene.air/ # Hero select test suite
│   └── main.py
└── logs/                  # Test execution logs
```

## Test Coverage

| Test | Description |
|------|-------------|
| `test_hero_select_scene_loads` | Verify hero select scene loads |
| `test_hero_selection` | Test selecting each hero class |
| `test_start_button_state` | Verify start button after hero select |
| `test_multiplayer_ready_button` | Test ready button in MP (requires session) |

## Adding UI Templates

UI templates (screenshots) are stored in `assets/`. To add new templates:

1. Run game and take screenshot:
   ```bash
   python3 -c "from airtest.core.api import snapshot; snapshot('assets/my_element.png')"
   ```

2. Or use Airtest IDE for visual recording

3. Reference in tests:
   ```python
   from airtest.core.cv import Template
   my_element = Template("assets/my_element.png", threshold=0.8)
   ```

## Headless CI Setup

For GitHub Actions or other CI:

```yaml
- name: Install xvfb
  run: sudo apt-get install -y xvfb

- name: Run UI Tests
  run: xvfb-run -a python3 tests/airtest/run_tests.py --headless
```

## Troubleshooting

### "Device not found"
- Ensure game is running before starting tests
- Or use `--no-start` flag if game is already running

### "Template not found"
- Add UI screenshots to `assets/` directory
- Use `take_screenshot()` to capture during test

### "Airtest import errors"
```bash
pip install --upgrade airtest
pip install opencv-contrib-python
```
