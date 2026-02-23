package network;

import com.watabou.utils.GameSettings;
public class MPSettings extends GameSettings{
    public static final String KEY_MULTIPLAYER_HOST = "multiplayer_host";
    public static final String KEY_MULTIPLAYER_IP = "multiplayer_ip";
    public static final String KEY_MULTIPLAYER_PORT = "multiplayer_port";
    public static final String KEY_MAX_PLAYERS = "max_players";

    public static void multiplayerHost(boolean value) {
        put(KEY_MULTIPLAYER_HOST, value);
    }

    public static boolean multiplayerHost() {
        return getBoolean(KEY_MULTIPLAYER_HOST, true); // По умолчанию хост
    }

    public static void multiplayerIP(String ip) {
        put(KEY_MULTIPLAYER_IP, ip);
    }

    public static String multiplayerIP() {
        return getString(KEY_MULTIPLAYER_IP, "localhost", 20);
    }

    public static void multiplayerPort(int port) {
        put(KEY_MULTIPLAYER_PORT, port);
    }

    public static int multiplayerPort() {
        return getInt(KEY_MULTIPLAYER_PORT, 54555, 1024, 65535);
    }

    public static void maxPlayers(boolean value) {
        put(KEY_MAX_PLAYERS, value);
    }
    public static int maxPlayers() {
        return getInt(KEY_MAX_PLAYERS, 4, 2, 10);
    }
}

