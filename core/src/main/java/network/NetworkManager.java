// NetworkManager.java
package network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import network.handlers.HeroClassHandler;
import network.handlers.HeroCreatedHandler;
import network.handlers.PlayerAssignHandler;
import network.handlers.PlayerJoinHandler;
import network.handlers.PlayerLeaveHandler;
import network.handlers.SeedInitHandler;
import network.handlers.ServerShutdownHandler;
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

public class NetworkManager {
    private static NetworkManager instance;

    public enum Mode { NONE, CLIENT, SERVER, LOCALHOST }

    private final Kryo kryo;
    private final MessageDispatcher messageDispatcher;
    private boolean isDisconnecting = false;
    
    private ServerCore server;
    private ClientCore client;

    private NetworkManager() {
        kryo = new Kryo();
        setupKryo();
        messageDispatcher = new MessageDispatcher();
        initHandlers();
    }

    private static class KryoEncoder extends MessageToByteEncoder<Object> {
        private final Kryo kryo;

        public KryoEncoder(Kryo kryo) {
            this.kryo = kryo;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            try (ByteBufOutputStream bbos = new ByteBufOutputStream(out);
                 Output output = new Output(bbos)) {
                kryo.writeObject(output, msg);
                output.flush();
            }
        }
    }
    private static class KryoDecoder extends ByteToMessageDecoder {
        private final Kryo kryo;

        public KryoDecoder(Kryo kryo) {
            this.kryo = kryo;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) throws Exception {
            try (ByteBufInputStream bbis = new ByteBufInputStream(in);
                 Input input = new Input(bbis)) {
                Object obj = kryo.readObject(input, BundleMessage.class);
                out.add(obj);
            }
        }
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    private ClientCallflow clientCallflow;

    public ClientCallflow getClientCallflow() {
        if (clientCallflow == null) {
            clientCallflow = new ClientCallflow(ClientStateMachine.getInstance(), this);
        }
        return clientCallflow;
    }
    private void initHandlers() {
        messageDispatcher.registerHandler(new PlayerAssignHandler());
        messageDispatcher.registerHandler(new PlayerJoinHandler());
        messageDispatcher.registerHandler(new PlayerLeaveHandler());
        messageDispatcher.registerHandler(new ServerShutdownHandler(this));
        messageDispatcher.registerHandler(new SeedInitHandler());
        messageDispatcher.registerHandler(new HeroClassHandler());
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

    private void setupKryo() {
        kryo.register(BundleMessage.class);
        kryo.register(String.class);
        kryo.register(int.class);
        kryo.register(Multiplayer.PlayerInfo.class);
        kryo.register(HashMap.class);
        kryo.register(java.util.ArrayList.class);
    }

    // ----- Управление сервером и клиентом -----

    public void startServer() {
        if (server != null) return;
        server = new ServerCore();
        server.start(MPSettings.multiplayerPort());
    }

    public void connectToServer(String host) {
        if (client != null && client.isConnected()) return;
        client = new ClientCore(ClientStateMachine.getInstance(),
                getClientCallflow());
        client.connect(host, MPSettings.multiplayerPort());
    }

    public void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        Game.runOnRenderThread(() -> {
            Multiplayer.isMultiplayer = false;
            Multiplayer.isHost = false;
            Multiplayer.Players.clear();
        });
    }

    // ----- Состояние -----

    public Mode getModeImpl() {
        if (server != null && client != null && client.isConnected()) {
            return Mode.LOCALHOST;
        } else if (server != null) {
            return Mode.SERVER;
        } else if (client != null && client.isConnected()) {
            return Mode.CLIENT;
        } else {
            return Mode.NONE;
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public int getLocalPlayerIdImpl() {
        return client != null ? client.getLocalPlayerId() : -1;
    }

    public void setLocalPlayerIdImpl(int id) {
        if (client != null) client.setLocalPlayerId(id);
    }

    // ----- Отправка сообщений -----

    public void sendMessageImpl(String type, Bundle bundle) {
        BundleMessage msg = new BundleMessage(type, getLocalPlayerId());
        msg.bundleData = bundle.toString();
        if (client != null && client.isConnected()) {
            client.sendMessage(msg);
        } else if (server != null) {
            server.broadcastMessage(msg, null);
        }
    }

    public void sendToServerImpl(BundleMessage msg) {
        if (client != null && client.isConnected()) {
            client.sendMessage(msg);
        }
    }

    public void broadcastMessageServerImpl(BundleMessage msg) {
        if (server != null) {
            server.broadcastMessage(msg, null);
        }
    }

    public void broadcastMessageServerImpl(BundleMessage msg, ChannelHandlerContext ctxToIgnore) {
        if (server != null) {
            server.broadcastMessage(msg, ctxToIgnore);
        }
    }

    // ----- Вспомогательные методы -----

    public void showMessage(String text) {
        Game.runOnRenderThread(() -> {
            if (ShatteredPixelDungeon.scene() != null) {
                ShatteredPixelDungeon.scene().addToFront(new WndMessage(text));
            }
        });
    }

    // ======================== Внутренние классы ========================

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

    // ----- Серверное ядро -----
    private class ServerCore {
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private Channel serverChannel;
        Map<Integer, ClientSessionState> connectedClients = new ConcurrentHashMap<>();
        private final ServerStateMachine stateMachine = ServerStateMachine.getInstance();
        private final ServerCallflow callflow;
        public ServerCore() {
            this.callflow = new ServerCallflow(ServerStateMachine.getInstance(), connectedClients);
            // Подписываем callflow на изменения глобального состояния сервера
            ServerStateMachine.getInstance().addListener(callflow::onServerStateChanged);
        }
        public ClientSessionState getSession(int playerId) {
            return connectedClients.get(playerId);
        }
        public void start(int port) {
            Game.runOnRenderThread(() -> stateMachine.onServerStarting());
            new Thread(() -> {
                try {
                    bossGroup = new NioEventLoopGroup(1);
                    workerGroup = new NioEventLoopGroup();
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ChannelPipeline p = ch.pipeline();
                                    p.addLast(new LengthFieldPrepender(4));
                                    p.addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                                    p.addLast(new KryoDecoder(kryo));
                                    p.addLast(new KryoEncoder(kryo));
                                    p.addLast(new ServerHandler());
                                }
                            })
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .childOption(ChannelOption.TCP_NODELAY, true);

                    ChannelFuture f = b.bind(port).sync();
                    serverChannel = f.channel();
                    Game.runOnRenderThread(() -> {
                        stateMachine.onServerStarted();
                        Dungeon.seed = 1234;
                        showMessage("Server started on port " + port);
                        Multiplayer.isMultiplayer = true;
                        Multiplayer.isHost = true;
                    });
                } catch (Exception e) {
                    // e.printStackTrace();
                    Game.runOnRenderThread(() -> stateMachine.onError("Failed to start server: " + e.getMessage()));
                }
            }, "Server-Thread").start();
        }

        public void stop() {
            if (serverChannel != null) serverChannel.close();
            if (bossGroup != null) bossGroup.shutdownGracefully();
            if (workerGroup != null) workerGroup.shutdownGracefully();

            Game.runOnRenderThread(() -> stateMachine.reset());
            connectedClients.clear();
        }

        public void broadcastMessage(BundleMessage msg, ChannelHandlerContext ignore) {
            for (ClientSessionState session : connectedClients.values()) {
                ChannelHandlerContext ctx = session.ctx;
                if (ctx != ignore && ctx.channel().isActive()) {
                    ctx.writeAndFlush(msg);
                }
            }
        }

        private class ServerHandler extends SimpleChannelInboundHandler<BundleMessage> {
            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                if (stateMachine.getCurrentState() != ServerStateMachine.State.OPERATIONAL) {
                     ctx.close();
                     return;
                 }
                if (connectedClients.size() >= MPSettings.maxPlayers()) {
                    ctx.close();
                    return;
                }

                Game.runOnRenderThread(() -> {
                    int playerId = ctx.channel().hashCode();
                    String name = "Player " + playerId;
                    ClientSessionState session = new ClientSessionState(playerId, ctx, name, callflow);
                    connectedClients.put(playerId, session);

                    Game.runOnRenderThread(() -> {
                        callflow.onClientConnected(session);
                    });
                });
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, BundleMessage msg) {
                Game.runOnRenderThread(() -> {
                    messageDispatcher.dispatch(msg);
                    broadcastMessage(msg, ctx);
                });
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
                Game.runOnRenderThread(() -> {
                    int playerId = ctx.channel().hashCode();
                    connectedClients.remove(playerId);
                    Multiplayer.Players.remove(playerId);

                    BundleMessage leaveMsg = new BundleMessage("PLAYER_LEAVE", playerId);
                    broadcastMessage(leaveMsg, ctx);
                });
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                String err = getDetailedErrorMessage(cause);
                Game.runOnRenderThread(() -> showMessage("Client error: " + err));
                ctx.close();
            }
        }
    }

    // ----- Клиентское ядро -----
    private class ClientCore {
        private EventLoopGroup workerGroup;
        private Channel clientChannel;
        private int localPlayerId = -1;
        private String lastError;
        private final ClientStateMachine clientStateMachine;
        private final ClientCallflow clientCallflow;
        public ClientCore(ClientStateMachine clientStateMachine, ClientCallflow clientCallflow) {
            this.clientStateMachine = clientStateMachine;
            this.clientCallflow = clientCallflow;
        }
        public void connect(String host, int port) {
            new Thread(() -> {
                try {
                    workerGroup = new NioEventLoopGroup();
                    Bootstrap b = new Bootstrap();
                    b.group(workerGroup)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ChannelPipeline p = ch.pipeline();
                                    p.addLast(new LengthFieldPrepender(4));
                                    p.addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                                    p.addLast(new KryoDecoder(kryo));
                                    p.addLast(new KryoEncoder(kryo));
                                    p.addLast(new ClientHandler());
                                }
                            })
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

                    ChannelFuture f = b.connect(host, port).sync();
                    clientChannel = f.channel();
                    Game.runOnRenderThread(() -> {
                        Multiplayer.isMultiplayer = true;
                        Multiplayer.isHost = false;
                    });
                }  catch (Exception e) {
                    e.printStackTrace();
                    Game.runOnRenderThread(() -> {
                        ClientStateMachine.getInstance().onError("Connection failed");
                        showMessage("Connection failed");
                    });
                }
            }, "Client-Thread").start();
        }

        public void disconnect() {
            if (clientChannel != null) clientChannel.close();
            if (workerGroup != null) workerGroup.shutdownGracefully();
            localPlayerId = -1;
        }

        public boolean isConnected() {
            return clientChannel != null && clientChannel.isActive();
        }

        public int getLocalPlayerId() { return localPlayerId; }
        public void setLocalPlayerId(int id) { this.localPlayerId = id; }

        public void sendMessage(BundleMessage msg) {
            if (isConnected()) {
                clientChannel.writeAndFlush(msg);
            }
        }

        private class ClientHandler extends SimpleChannelInboundHandler<BundleMessage> {
            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                Game.runOnRenderThread(() -> {
                    localPlayerId = ctx.channel().hashCode();
                    lastError = null;
//                    clientCallflow.onConnected();
                });
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, BundleMessage msg) {
                Game.runOnRenderThread(() -> messageDispatcher.dispatch(msg));
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
                Game.runOnRenderThread(() -> {
                    if (!isDisconnecting) { // нужно добавить флаг в NetworkManager
                        String message = lastError != null ? "Connection lost: " + lastError
                                : "Connection lost: Server might be offline";
                        showMessage(message);
                        clientStateMachine.onError(message);
                    }
                    Multiplayer.isMultiplayer = false;
                    Multiplayer.isHost = false;
                    Multiplayer.Players.clear();
                });
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                lastError = getDetailedErrorMessage(cause);
                clientStateMachine.onError(lastError);
                ctx.close();
            }
        }
    }

    // ----- Вспомогательные методы (ошибки) -----
    private String getDetailedErrorMessage(Throwable cause) {
        if (cause == null) return "Unknown error";
        if (cause instanceof io.netty.channel.ConnectTimeoutException) {
            return "Connection timeout - server might be offline";
        } else if (cause instanceof java.net.ConnectException) {
            return "Cannot connect to server - check address and port";
        } else if (cause instanceof java.net.UnknownHostException) {
            return "Unknown host - check server address";
        } else if (cause instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return "Server timeout - no response received";
        } else if (cause instanceof java.io.IOException) {
            return "Network error: " + cause.getMessage();
        } else if (cause instanceof com.esotericsoftware.kryo.KryoException) {
            return "Data serialization error";
        } else if (cause instanceof io.netty.handler.codec.DecoderException) {
            return "Protocol error - invalid data received";
        }
        return cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
    }

    // ----- Статические прокси для обратной совместимости -----
    public static int getLocalPlayerId() {
        return getInstance().getLocalPlayerIdImpl();
    }

    public static void sendMessage(String type, Bundle bundle) {
        getInstance().sendMessageImpl(type, bundle);
    }

    public static void sendToServer(BundleMessage msg) {
        getInstance().sendToServerImpl(msg);
    }

    public static void broadcastMessageServer(BundleMessage msg) {
        getInstance().broadcastMessageServerImpl(msg);
    }

    public static void broadcastMessageServer(BundleMessage msg, ChannelHandlerContext ctxToIgnore) {
        getInstance().broadcastMessageServerImpl(msg, ctxToIgnore);
    }

    public static Mode getMode() {
        return getInstance().getModeImpl();
    }

    public static void setLocalPlayerId(int id) {
        if (getInstance() != null) getInstance().setLocalPlayerIdImpl(id);
    }

    public static ClientSessionState getSession(int playerId) {
        NetworkManager nm = getInstance();
        if (nm.server != null) {
            return nm.server.getSession(playerId);
        }
        return null;
    }
}