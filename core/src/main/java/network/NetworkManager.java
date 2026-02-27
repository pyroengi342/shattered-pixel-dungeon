// NetworkManager.java
package network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {
    public enum Mode { NONE, CLIENT, SERVER }
    private static NetworkManager instance;
    private final MessageDispatcher messageDispatcher = new MessageDispatcher(this);
    private static Mode mode = Mode.NONE;

    // Netty
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private static Channel clientChannel;

    // Переменные для отслеживания состояния соединения
    private volatile boolean isDisconnecting = false;
    // Флаг для предотвращения двойного запуска
    private volatile boolean isStarting = false;
    private String lastError = null;

    // Kryo initialize
    private final Kryo kryo = new Kryo();

    // Client info
    private static final Map<Integer, ChannelHandlerContext> connectedClients = new ConcurrentHashMap<>();

    private static int localPlayerId = -1; // -1 reserved for HOST message
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

    private NetworkManager() {
        setupKryo();
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean isConnected() {
        if (mode == Mode.CLIENT && clientChannel != null) {
            return clientChannel.isActive();
        } else if (mode == Mode.SERVER && serverChannel != null) {
            return serverChannel.isActive();
        }
        return false;
    }

    public static Mode getMode() {
        return mode;
    }
    public void disconnect() {
        isDisconnecting = true;

        new Thread(() -> {
            if (mode == Mode.CLIENT) {
                stopClient();
            } else if (mode == Mode.SERVER) {
                Multiplayer.Players.remove(0);
                stopServer();
            }

            Game.runOnRenderThread(() -> {
                mode = Mode.NONE;
                localPlayerId = -1;
                if (!Multiplayer.isHost) {
                    Multiplayer.Players.clear();
                }
                connectedClients.clear();

                if (isDisconnecting) {
                    showMessage("Disconnected successfully");
                }

                isDisconnecting = false;
                network.Multiplayer.isMultiplayer = false;
                network.Multiplayer.isHost = false;
            });
        }, "Network-Disconnect-Thread").start();
    }

    // Server Handler
    private class ServerHandler extends SimpleChannelInboundHandler<BundleMessage> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            Game.runOnRenderThread(() -> {
                int playerId = ctx.channel().hashCode();
                connectedClients.put(playerId, ctx);
                {
                    BundleMessage assignIdMessage = new BundleMessage("PLAYER_ASSIGN", playerId);
                    Bundle assignBundle = new Bundle();
                    assignBundle.put("assignedId", playerId);
                    assignBundle.put("name", "Player " + playerId);

                    assignIdMessage.bundleData = assignBundle.toString();
                    ctx.writeAndFlush(assignIdMessage);
                }
                {
                    // Adding player
                    Multiplayer.PlayerInfo newPlayer = new Multiplayer.PlayerInfo(playerId, "Player " + playerId);
                    Multiplayer.Players.add(newPlayer);

                    // 2. Tell everybody about new player
                    BundleMessage joinMessage = new BundleMessage("PLAYER_JOIN", playerId);
                    Bundle bundle = new Bundle();
                    bundle.put("name", newPlayer.name);
                    joinMessage.bundleData = bundle.toString();
                    broadcastMessageServer(joinMessage, ctx);

                    // 3. Tell the about others for the player
                    sendExistingPlayersToNewClient(ctx);
                }


                sendCurrGameStateToPlayer(ctx);
            });
        }
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, BundleMessage msg) throws Exception {
            Game.runOnRenderThread(() -> {
                messageDispatcher.dispatch(msg);
                if (msg.type.equals("PLAYER_INPUT") || msg.type.equals("LEVEL_UPDATE")) {
                    broadcastMessageServer(msg, ctx);
                }
            });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);

            Game.runOnRenderThread(() -> {
                int playerId = ctx.channel().hashCode();
                connectedClients.remove(playerId);
                Multiplayer.Players.remove(playerId);

                showMessage("Player " + playerId + " disconnected (" + connectedClients.size() + " remaining)");

                BundleMessage leaveMessage = new BundleMessage("PLAYER_LEAVE", playerId);
                broadcastMessageServer(leaveMessage);
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            String errorMsg = getDetailedErrorMessage(cause);
            System.err.println("Server client handler error (" + ctx.channel().hashCode() + "): " + errorMsg);
            cause.printStackTrace();

            Game.runOnRenderThread(() -> {
                showMessage("Client error: " + getDetailedErrorMessage(cause));
            });

            ctx.close();
        }
    }
    public void startServer() {
        if (isStarting) return;
        isStarting = true;
        new Thread(() -> {
            try {
                mode = Mode.SERVER;

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

                int port = MPSettings.multiplayerPort();
                ChannelFuture f = b.bind(port);

                f.addListener((ChannelFutureListener) future -> {
                    Game.runOnRenderThread(() -> {
                        if (future.isSuccess()) {
                            serverChannel = future.channel();
                            MPSettings.multiplayerHost(true);
                            showMessage("Server started on port " + port);

                            network.Multiplayer.isMultiplayer = true;
                            network.Multiplayer.isHost = true;

                            // Создаем игрока для администратора
                            Multiplayer.PlayerInfo hostPlayer = new Multiplayer.PlayerInfo(0, "Host");
                            hostPlayer.isLocal = true; // Отмечаем как локального игрока
                            Multiplayer.Players.add(hostPlayer);
                            connectToServer("127.0.0.1");
                        } else {
                            //showError("Failed to start server: " + future.cause().getMessage());
                            mode = Mode.NONE;
                            stopServer();
                        }
                        isStarting = false;
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                Game.runOnRenderThread(() -> {
                    showMessage("Failed to start server: " + e.getMessage());
                    mode = Mode.NONE;
                    isStarting = false;
                });
            }
        }, "Network-Server-Thread").start();
    }
    private void stopServer() {
        // Сначала уведомляем всех клиентов
        Game.runOnRenderThread(() -> {
            BundleMessage shutdownMsg = new BundleMessage("SERVER_SHUTDOWN", -1);
            broadcastMessageServer(shutdownMsg);
        });

        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close().sync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        connectedClients.clear();
    }

    // Sending message for all clients
    public static void broadcastMessageServer(BundleMessage message) {
        if (mode != Mode.SERVER) return;

        for (ChannelHandlerContext clientCtx : connectedClients.values()) {
            if (clientCtx.channel().isActive()) {
                clientCtx.writeAndFlush(message);
            }
        }
    }
    public static void broadcastMessageServer(BundleMessage message, ChannelHandlerContext ctxToIgnore) {
        if (mode != Mode.SERVER) return;

        for (ChannelHandlerContext clientCtx : connectedClients.values()) {
            if (clientCtx != ctxToIgnore && clientCtx.channel().isActive()) {
                clientCtx.writeAndFlush(message);
            }
        }
    }

    // Kryo Codec setup
    private void setupKryo() {
        kryo.register(BundleMessage.class);
        kryo.register(String.class);
        kryo.register(int.class);
        kryo.register(Multiplayer.PlayerInfo.class);
        kryo.register(HashMap.class);
        kryo.register(java.util.ArrayList.class);
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

    // Client Handler
    private class ClientHandler extends SimpleChannelInboundHandler<BundleMessage> {
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
                Game.runOnRenderThread(() -> {
                    localPlayerId = ctx.channel().hashCode();
                    lastError = null; // Сбрасываем ошибку при подключении
                });
            }
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, BundleMessage msg) throws Exception {
                Game.runOnRenderThread(() -> {
                    messageDispatcher.dispatch(msg);
                    //handleNetworkMessage(msg);
                });
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);
                Game.runOnRenderThread(() -> {
                    mode = Mode.NONE;
                    localPlayerId = -1;
                    Multiplayer.Players.clear();

                    String message;
                    if (isDisconnecting) {
                        message = "Disconnected from server (user request)";
                        isDisconnecting = false;
                    } else if (lastError != null) {
                        message = "Connection lost: " + lastError;
                        lastError = null;
                    } else {
                        message = "Connection lost: Server might be offline or network problem";
                    }

                    showMessage(message);
                    network.Multiplayer.isMultiplayer = false;
                    network.Multiplayer.isHost = false;
                });
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                String errorMsg = getDetailedErrorMessage(cause);
                lastError = errorMsg;

                System.err.println("Client network error: " + errorMsg);
                cause.printStackTrace();
                ctx.close();
            }
        }
    public void connectToServer(String host) {
        if (isStarting) return;
        isStarting = true;

        // Если мы администратор и уже создали себя как игрока
        if (mode == Mode.SERVER && host.equals("127.0.0.1")) {
            // Пропускаем обычное подключение для администратора
            Game.runOnRenderThread(() -> {
                showMessage("Host connected as player");
                isStarting = false;
            });
            return;
        }

        new Thread(() -> {
            try {
                mode = Mode.CLIENT;

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

                int port = MPSettings.multiplayerPort();
                ChannelFuture f = b.connect(host, port);

                f.addListener((ChannelFutureListener) future -> {
                    Game.runOnRenderThread(() -> {
                        if (future.isSuccess()) {
                            clientChannel = future.channel();
                            MPSettings.multiplayerIP(host);
                            MPSettings.multiplayerHost(false);

                            network.Multiplayer.isMultiplayer = true;
                            network.Multiplayer.isHost = false;
                        } else {
                            showMessage("Connection failed: " + future.cause().getMessage());
                            mode = Mode.NONE;
                            stopClient();
                        }
                        isStarting = false;
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                Game.runOnRenderThread(() -> {
                    showMessage("Connection failed: " + e.getMessage());
                    mode = Mode.NONE;
                    isStarting = false;
                });
            }
        }, "Network-Client-Thread").start();
    }
    private void stopClient() {
        if (clientChannel != null && clientChannel.isOpen()) {
            try {
                clientChannel.close().sync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    public static void setLocalPlayerId(int id) {
        localPlayerId = id;
    }
    // Info
    private String getDetailedErrorMessage(Throwable cause) {
        if (cause == null) return "Unknown error";

        String simpleName = cause.getClass().getSimpleName();
        String message = cause.getMessage();

        // Анализируем тип ошибки
        if (cause instanceof io.netty.channel.ConnectTimeoutException) {
            return "Connection timeout - server might be offline";
        } else if (cause instanceof java.net.ConnectException) {
            return "Cannot connect to server - check address and port";
        } else if (cause instanceof java.net.UnknownHostException) {
            return "Unknown host - check server address";
        } else if (cause instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return "Server timeout - no response received";
        } else if (cause instanceof java.io.IOException) {
            return "Network error: " + (message != null ? message : "I/O problem");
        } else if (cause instanceof com.esotericsoftware.kryo.KryoException) {
            return "Data serialization error";
        } else if (cause instanceof io.netty.handler.codec.DecoderException) {
            return "Protocol error - invalid data received";
        }

        // Общий формат
        return simpleName + (message != null ? ": " + message : "");
    }
    public void showMessage(String message) {
        Game.runOnRenderThread(() -> {
            if (ShatteredPixelDungeon.scene() != null) {
                ShatteredPixelDungeon.scene().addToFront(new WndMessage(message));
            }
        });
    }

    // Game Start
    public static void broadcastSeed(long seed, String customSeedText) {
        if (mode != Mode.SERVER) return;

        BundleMessage message = new BundleMessage();
        message.type = "SEED_INIT";
        message.playerId = -1; // HOST message

        Bundle bundle = new Bundle();
        bundle.put("seed", seed);
        bundle.put("customSeedText", customSeedText != null ? customSeedText : "");
        message.bundleData = bundle.toString();

        broadcastMessageServer(message);
    }
    public static void sendHeroClass(HeroClass heroClassName) {
        if (mode == Mode.NONE) return;

        if (mode == Mode.CLIENT && clientChannel != null) {
            BundleMessage message = new BundleMessage();
            message.type = "HERO_CLASS";
            message.playerId = getLocalPlayerId();

            Bundle bundle = new Bundle();
            bundle.put("HERO_CLASS", heroClassName);

            message.bundleData = bundle.toString();

            clientChannel.writeAndFlush(message);
        }
    }

    // Player methods
    private void sendCurrGameStateToPlayer(ChannelHandlerContext ctx) {
        Bundle gameState = new Bundle();
        BundleMessage message = new BundleMessage("GAME_STATE", -1);
        message.bundleData = gameState.toString();

        // Хорошо бы здесь отправлять сид, информацию конкретно по heroes, mobs, items
        // Если игра началась
        ctx.writeAndFlush(message);
    }
    private void sendExistingPlayersToNewClient(ChannelHandlerContext newClientCtx) {
        for (ChannelHandlerContext clientCtx : connectedClients.values()) {
            if (clientCtx != newClientCtx) {
                Multiplayer.PlayerInfo existingPlayer = Multiplayer.Players.get(clientCtx.channel().hashCode());
                if (existingPlayer != null) {
                    BundleMessage existingMessage = new BundleMessage("PLAYER_JOIN", existingPlayer.connectionID);
                    Bundle existingBundle = new Bundle();
                    existingBundle.put("name", existingPlayer.name);
                    existingMessage.bundleData = existingBundle.toString();

                    newClientCtx.writeAndFlush(existingMessage);
                }

            }
        }
    }
    public static int getLocalPlayerId() {
        return localPlayerId;
    }
}