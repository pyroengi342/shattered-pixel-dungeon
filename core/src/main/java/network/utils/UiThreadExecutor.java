package network.utils;

import com.watabou.noosa.Game;

public class UiThreadExecutor {
    private UiThreadExecutor() {
        /* This utility class should not be instantiated */
    }

    public static void run(Runnable action) {
        Game.runOnRenderThread(action::run);
    }
}