# Design: Headless Testing Infrastructure для Shattered Pixel Dungeon

**Дата:** 2026-03-16  
**Статус:** Approved

## Цель

Добавить headless режим для тестирования network/lobby логики и настроить Airtest для UI тестирования desktop приложения.

## Background

- Shattered Pixel Dungeon — libGDX игра с network multiplayer
- Текущее состояние: нет тестов, игра требует OpenGL
- Нужна инфраструктура для автоматического тестирования:
  - Headless mode — для network/lobby тестов
  - Airtest — для UI тестирования desktop приложения

## Решение

### 1. Headless Mode для Network/Lobby

**Зависимости:**
```gradle
// core/build.gradle
testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
```

**Использование:**
```java
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

// Важно: загрузить нативы для headless режима
try {
    System.loadLibrary("gdx");
} catch (Throwable ignored) {}

// Создание headless приложения
HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
config.updateInterval = 30; // 30 FPS fixed update
HeadlessApplication app = new HeadlessApplication(new MyGameListener(), config);
```

**Что можно тестировать:**
- NetworkManager — подключение, отключение, reconnection
- MultiplayerClient / MultiplayerServer — состояния
- Message handlers — обработка сообщений
- Game state — ready, waiting, playing

### 2. Airtest для Desktop UI

**Установка:**
```bash
pip install -U airtest
```

**Подключение к Desktop:**
```python
from airtest.core.api import *

# Windows
init_device("Windows", uuid="title_re=Shattered Pixel Dungeon")

# Linux (через ADB или winlib)
# Или через screenshot + image recognition
```

**Основные API:**
```python
# Клики по изображению
touch(Template("start_button.png"))

# Свайпы
swipe((400, 800), (400, 200))

# Assertions
assert_exists(Template("main_menu.png"))
assert_not_exists(Template("error_dialog.png"))

# Текстовый ввод
text("player_name")
```

### 3. Skill для тестирования

**Создать skill:** `game-tester`
- Использует headless для network тестов
- Использует Airtest для UI тестов
- Может запускать тесты локально

### 4. Документация

**Создать:** `docs/TESTING.md`
- Как запускать headless тесты
- Как запускать Airtest
- Примеры тестов

## Реализация

### Task 1: Добавить headless зависимости и JUnit

**Files:**
- Modify: `core/build.gradle`

```gradle
// В начале файла добавить:
apply plugin: 'java-library'

// В dependencies добавить:
dependencies {
    // ... existing deps
    
    // JUnit для тестов
    testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
    
    // Headless для тестов
    testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
testImplementation "com.badlogicgames.gdx:gdx:$gdxVersion:natives-desktop"
}

// Добавить после dependencies:
test {
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
    }
}
```

### Task 2: Создать LobbyTest класс

**Files:**
- Create: `core/src/test/java/network/LobbyTest.java`

```java
public class LobbyTest {
    
    @Test
    public void testConnection() {
        // Test connect to localhost server
    }
    
    @Test
    public void testDisconnection() {
        // Test clean disconnect
    }
    
    @Test
    public void testReconnection() {
        // Test reconnect after disconnect
    }
    
    @Test
    public void testGameReadyState() {
        // Test game ready signal
    }
}
```

### Task 3: Настроить Airtest

**Files:**
- Create: `tests/airtest/`

```
tests/airtest/
├── connect_desktop.air/
│   └── connect_desktop.py
├── lobby_test.air/
│   └── lobby_test.py
└── README.md
```

### Task 4: Создать game-tester skill

**Files:**
- Create: `.config/opencode/skills/game-tester/SKILL.md`

### Task 5: Написать документацию

**Files:**
- Create: `docs/TESTING.md`

## Acceptance Criteria

1. `./gradlew test` запускает headless тесты
2. Airtest может подключиться к desktop приложению
3. Skill game-tester доступен для использования
4. Документация описывает как запускать тесты
