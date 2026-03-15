# AGENTS.md - Shattered Pixel Dungeon Developer Guide

This file provides guidelines and instructions for AI agents working on this codebase.

## Project Overview

Shattered Pixel Dungeon is an open-source roguelike game built with Java and libGDX. It supports Android, iOS, and Desktop platforms.

## Build Commands

### Desktop Build
```bash
./gradlew desktop:debug       # Run game in debug mode
./gradlew desktop:release     # Build release JAR to desktop/build/libs
./gradlew desktop:jpackageimage  # Generate OS-specific executable
```

### Android Build
```bash
./gradlew android:assembleDebug    # Debug APK
./gradlew android:assembleRelease  # Release APK
```

### General
- Java 17+ required (21 recommended per README)
- Gradle wrapper: `./gradlew`
- No test framework configured in this project

## Code Style Guidelines

### File Organization
- Source files in `src/main/java/` following standard package structure
- Main packages: `com.shatteredpixel.shatteredpixeldungeon.*` (game), `com.watabou.*` (framework)
- Assets in `src/main/assets/`
- Network code in `network.*` package within core

### Formatting
- Use **tabs for indentation** (not spaces)
- BSD/Allman brace style: opening brace on new line
- Max line length ~120 characters (soft guideline)
- License header required on all new Java files:
```java
/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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
```

### Naming Conventions
- Classes: `PascalCase` (e.g., `NetworkManager`, `Item`)
- Methods/Variables: `camelCase` (e.g., `getItemID()`, `isConnected`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_KEY`, `TIME_TO_THROW`)
- Interfaces: Often end with `Handler`, `Listener`, or `Agent` (e.g., `MessageHandler`)

### Types
- Use Java primitives where possible (`int`, `float`, `boolean`)
- Use `long` for IDs that need large range
- Collections: `ArrayList`, `HashMap`, `ConcurrentHashMap` for thread safety
- Prefer interfaces for parameters (e.g., `List`, `Map`) when possible

### Imports
- Group imports by package: standard library first, then third-party, then project-specific
- No wildcard imports (`import foo.*`)
- Sort alphabetically within groups

### Error Handling
- Use `Game.reportException(e)` to log errors (not `e.printStackTrace()`)
- For recoverable errors, return default/fallback values rather than throwing
- Bundle serialization uses try-catch with `Game.reportException(e)`

### Key Framework Patterns

#### Bundlable Interface (Serialization)
```java
public class MyClass implements Bundlable {
    private static final String KEY_FIELD = "field";
    
    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(KEY_FIELD, field);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        field = bundle.get(KEY_FIELD);
    }
}
```

#### Signal/Callback (Event System)
- Use `Signal` from `com.watabou.utils.Signal` for event listeners
- Use `Callback` for async operations

#### Networking
- Uses Netty for network transport
- Uses Kryo for serialization
- Message handlers implement `MessageHandler` interface
- All network communication goes through `NetworkManager`

### Android/iOS Compatibility
- **Critical**: Some `org.json` methods don't exist on Android/iOS
- Use `JSONObject.optBoolean()` instead of `getBoolean()` with defaults
- Use `JSONTokener(String)` constructor only (not stream-based on mobile)
- Always use `BufferedReader` with `InputStreamReader` for Bundle I/O
- Check `Bundle.java` comments for specific Android/iOS limitations

### Performance Considerations
- Use object pooling where applicable
- Avoid allocations in game loops
- Use `SparseArray` for int-keyed maps

## Project Structure

```
shattered-pixel-dungeon/
├── core/           # Main game code
├── SPD-classes/    # Framework utilities (watabou)
├── android/        # Android launcher
├── desktop/        # Desktop launcher
├── ios/            # iOS launcher
├── services/       # Update/news services
└── docs/           # Build documentation
```

## Important Files

- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java` - Main game state
- `core/src/main/java/network/NetworkManager.java` - Multiplayer networking
- `SPD-classes/src/main/java/com/watabou/utils/Bundle.java` - Serialization (read for Android/iOS caveats)
- `build.gradle` - Build configuration (Java 11+, Netty, Kryo, libGDX dependencies)

## Multiplayer (Network Code)

### Dependencies (from build.gradle)
- **Netty**: 4.1.119.Final - TCP networking
- **Kryo**: 5.6.2 - Serialization
- **libGDX**: 1.14.0 - Game framework

### Key Network Files
| File | Purpose |
|------|---------|
| `network/NetworkManager.java` | Central network manager |
| `network/Multiplayer.java` | Player state, game helpers |
| `network/MessageDispatcher.java` | Message routing by type |
| `network/SMTH/MultiplayerServer.java` | Server implementation |
| `network/SMTH/MultiplayerClient.java` | Client implementation |
| `network/states/*.java` | State machines |
| `network/handlers/**/*.java` | Message handlers |

### Documentation
- `docs/NETCODE.md` - Network stack documentation
- `docs/NETWORK_ARCH.md` - Architecture analysis
- `docs/ROADMAP.md` - Project roadmap

### Pattern: Message Handler
```java
public class MyHandler implements MessageHandler {
    @Override
    public String getType() { return "MY_MESSAGE"; }
    
    @Override
    public void msgHandle(int senderId, Bundle data) {
        UiThreadExecutor.run(() -> { /* code */ });
    }
}
```

### Pattern: Send Message
```java
Bundle bundle = new Bundle();
bundle.put("key", value);
NetworkManager.sendMessage("TYPE", bundle);
```

## Compilation Check (IMPORTANT)

**ALWAYS run compilation check before pushing to remote:**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew compileJava --console=plain
```

If compilation fails - fix errors before pushing. Only push when `BUILD SUCCESSFUL`.
