// NetworkManager.java
package network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.utils.Bundle;

import io.netty.channel.ChannelHandlerContext;
import network.SMTH.MultiplayerClient;
import network.SMTH.MultiplayerServer;
import network.handlers.server.HeroClassHandler;
import network.handlers.server.PlayerKickHandler;
import network.handlers.client.HeroCreatedHandler;
import network.handlers.client.ClientPlayerReadyHandler;
import network.handlers.client.KickNotifyHandler;
import network.handlers.client.PlayerAssignHandler;
import network.handlers.client.PlayerJoinHandler;
import network.handlers.PlayerLeaveHandler;
import network.handlers.client.SeedInitHandler;
import network.handlers.client.ServerShutdownHandler;
import network.handlers.window.AbilityHandler;
import network.handlers.window.BlacksmithHandler;
import network.handlers.window.ComboHandler;
import network.handlers.window.EnergizeHandler;
import network.handlers.window.GhostRewardHandler;
import network.handlers.window.ItemUseHandler;
import network.handlers.window.MonkAbilityHandler;
import network.handlers.window.SubclassHandler;
import network.handlers.window.UpgradeHandler;
import network.states.ClientSessionState;
import network.states.ClientStateMachine;
import network.states.ServerStateMachine;
import network.utils.UiThreadExecutor;

// network/NetworkManager.java
public class NetworkManager {
    public static class BundleMessage {
        public String bundleData;
        public int playerId;
        public String type;
        public BundleMessage() {}
        public BundleMessage(String type, int playerId) {
            this.type = type;
            this.playerId = playerId;
        }
    }
    private static NetworkManager instance;

    public enum Mode { NONE, CLIENT, SERVER, LOCALHOST }
    private final Map<Integer, ClientSessionState> connectedClients = new ConcurrentHashMap<>();
    private final Kryo kryo;
    private final MessageDispatcher messageDispatcher;
    private boolean isDisconnecting = false;

    private MultiplayerServer server;
    private MultiplayerClient client;
    private ClientAgent clientAgent;

    private NetworkManager() {
        kryo = new Kryo();
        setupKryo();
        messageDispatcher = new MessageDispatcher();
        initHandlers();
    }

    public static NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    private void setupKryo() {
        kryo.register(BundleMessage.class);
        kryo.register(String.class);
        kryo.register(int.class);
        kryo.register(Multiplayer.PlayerInfo.class);
        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
    }

    private void initHandlers() {
        messageDispatcher.registerHandler(new PlayerAssignHandler());
        messageDispatcher.registerHandler(new PlayerJoinHandler());
        messageDispatcher.registerHandler(new PlayerLeaveHandler());
        messageDispatcher.registerHandler(new ServerShutdownHandler(this));
        messageDispatcher.registerHandler(new SeedInitHandler());
        messageDispatcher.registerHandler(new HeroClassHandler());
        messageDispatcher.registerHandler(new ClientPlayerReadyHandler());
        messageDispatcher.registerHandler(new PlayerKickHandler());
        messageDispatcher.registerHandler(new KickNotifyHandler());
        // Window handlers
        messageDispatcher.registerHandler(new UpgradeHandler());
        messageDispatcher.registerHandler(new ItemUseHandler());
        messageDispatcher.registerHandler(new GhostRewardHandler());
        messageDispatcher.registerHandler(new MonkAbilityHandler());
        messageDispatcher.registerHandler(new EnergizeHandler());
        messageDispatcher.registerHandler(new ComboHandler());
        messageDispatcher.registerHandler(new SubclassHandler());
        messageDispatcher.registerHandler(new AbilityHandler());
        messageDispatcher.registerHandler(new BlacksmithHandler());
        messageDispatcher.registerHandler(new HeroCreatedHandler());
    }

    public ClientAgent getClientCallflow() {
        if (clientAgent == null) {
            clientAgent = new ClientAgent(ClientStateMachine.getInstance());
        }
        return clientAgent;
    }

    // --- Управление ---
    public void startServer() {
        if (server != null) return;
        ServerAgent callflow = new ServerAgent(ServerStateMachine.getInstance(), connectedClients);
        server = new MultiplayerServer(kryo, messageDispatcher, connectedClients, callflow);
        server.start(MPSettings.multiplayerPort());
    }
    public void connectToServer(String host) {
        if (client != null && client.isConnected()) return;
        client = new MultiplayerClient(kryo, messageDispatcher, ClientStateMachine.getInstance(), getClientCallflow());
        client.connect(host, MPSettings.multiplayerPort());
    }

    public void disconnect() {
        isDisconnecting = true;
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        UiThreadExecutor.run(() -> {
            Multiplayer.isMultiplayer = false;
            Multiplayer.isHost = false;
            Multiplayer.Players.clear();
        });
        isDisconnecting = false;
    }

    public boolean isDisconnecting() { return isDisconnecting; }

    // --- Состояние ---
    public Mode getModeImpl() {
        if (server != null && client != null && client.isConnected()) return Mode.LOCALHOST;
        else if (server != null) return Mode.SERVER;
        else if (client != null && client.isConnected()) return Mode.CLIENT;
        else return Mode.NONE;
    }

    public boolean isConnected() { return server != null || (client != null && client.isConnected()); }
    public int getLocalPlayerIdImpl() { return client != null ? client.getLocalPlayerId() : -1; }
    public void setLocalPlayerIdImpl(int id) { if (client != null) client.setLocalPlayerId(id); }

    // --- Отправка сообщений ---
    public void sendMessageImpl(String type, Bundle bundle) {
        BundleMessage msg = new BundleMessage(type, getLocalPlayerId());
        msg.bundleData = bundle.toString();
        if (client != null && client.isConnected()) {
            client.send(msg);
        } else if (server != null) {
            server.broadcast(msg, null);
        }
    }

    public void sendToServerImpl(BundleMessage msg) {
        if (client != null && client.isConnected()) client.send(msg);
    }

    public void broadcastMessageServerImpl(BundleMessage msg) {
        if (server != null) server.broadcast(msg, null);
    }

    public void broadcastMessageServerImpl(BundleMessage msg, ChannelHandlerContext ctxToIgnore) {
        if (server != null) server.broadcast(msg, ctxToIgnore);
    }

    // --- Вспомогательные методы ---
    public void showMessage(String text) {
        UiThreadExecutor.run(() -> {
            if (ShatteredPixelDungeon.scene() != null) {
                ShatteredPixelDungeon.scene().addToFront(new WndMessage(text));
            }
        });
    }

    public static ClientSessionState getSession(int playerId) {
        NetworkManager nm = getInstance();
        return nm.server != null ? nm.server.getSession(playerId) : null;
    }

    // --- Статические прокси (оставить для совместимости) ---
    public static int getLocalPlayerId() { return getInstance().getLocalPlayerIdImpl(); }
    public static void sendMessage(String type, Bundle bundle) { getInstance().sendMessageImpl(type, bundle); }
    public static void sendToServer(BundleMessage msg) { getInstance().sendToServerImpl(msg); }
    public static void broadcastMessageServer(BundleMessage msg) { getInstance().broadcastMessageServerImpl(msg); }
    public static void broadcastMessageServer(BundleMessage msg, ChannelHandlerContext ctxToIgnore) {
        getInstance().broadcastMessageServerImpl(msg, ctxToIgnore);
    }
    public static Mode getMode() { return getInstance().getModeImpl(); }
    public static void setLocalPlayerId(int id) { getInstance().setLocalPlayerIdImpl(id); }
}