package network.SMTH;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

import io.netty.bootstrap.ServerBootstrap;
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
import network.MessageDispatcher;
import network.Multiplayer;
import network.NetworkManager;
import network.ServerAgent;
import network.codec.ChannelPipelineFactory;
import network.states.ClientSessionState;
import network.states.ServerStateMachine;
import network.utils.ErrorMessageUtil;
import network.utils.UiThreadExecutor;

public class MultiplayerServer {
    private final Kryo kryo;
    private final MessageDispatcher messageDispatcher;
    private final ServerAgent callflow;
    private final ServerStateMachine stateMachine = ServerStateMachine.getInstance();
    private final Map<Integer, ClientSessionState> connectedClients; // новое поле

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public MultiplayerServer(Kryo kryo,
                             MessageDispatcher messageDispatcher,
                             Map<Integer, ClientSessionState> connectedClients,
                             ServerAgent callflow) {
        this.kryo = kryo;
        this.messageDispatcher = messageDispatcher;
        this.connectedClients = connectedClients; // сохраняем ссылку
        this.callflow = callflow;
        // почему так?
        // ServerStateMachine.getInstance().addListener(callflow::onServerStateChanged);
        stateMachine.addListener(callflow::onServerStateChanged);
    }

    public void start(int port) {
        UiThreadExecutor.run(stateMachine::onServerStarting);
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
                                ChannelPipelineFactory.addCodecs(p, kryo);
                                p.addLast(new ServerHandler(messageDispatcher, callflow, connectedClients, stateMachine));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);

                ChannelFuture f = b.bind(port).sync();
                serverChannel = f.channel();
                UiThreadExecutor.run(() -> {
                    stateMachine.onServerStarted();

                    Dungeon.seed = 1234; // временно
                    NetworkManager.getInstance().showMessage("Server started on port " + port);
                    NetworkManager.getInstance().connectToServer("127.0.0.1");

                    Multiplayer.isMultiplayer = true;
                    Multiplayer.isHost = true;
                });
            } catch (Exception e) {
                UiThreadExecutor.run(() -> stateMachine.onError("Failed to start server: " + e.getMessage()));
            }
        }, "Server-Thread").start();
    }

    public void stop() {
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        UiThreadExecutor.run(() -> {
            stateMachine.reset();
            connectedClients.clear();
        });
    }

    public void broadcast(NetworkManager.BundleMessage msg, ChannelHandlerContext ignore) {
        for (ClientSessionState session : connectedClients.values()) {
            ChannelHandlerContext ctx = session.ctx;
            if (ctx != ignore && ctx.channel().isActive()) {
                ctx.writeAndFlush(msg);
            }
        }
    }

    public ClientSessionState getSession(int playerId) {
        return connectedClients.get(playerId);
    }

    // Внутренний обработчик канала
    private static class ServerHandler extends SimpleChannelInboundHandler<NetworkManager.BundleMessage> {
        private final MessageDispatcher messageDispatcher;
        private final ServerAgent agent;
        private final Map<Integer, ClientSessionState> connectedClients;
        private final ServerStateMachine stateMachine;

        ServerHandler(MessageDispatcher messageDispatcher, ServerAgent agent,
                      Map<Integer, ClientSessionState> connectedClients, ServerStateMachine stateMachine) {
            this.messageDispatcher = messageDispatcher;
            this.agent = agent;
            this.connectedClients = connectedClients;
            this.stateMachine = stateMachine;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            UiThreadExecutor.run(() -> {
                ClientSessionState session = new ClientSessionState(-1, ctx, null, agent);
                agent.onClientConnected(session);
            });
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, NetworkManager.BundleMessage msg) {
            UiThreadExecutor.run(() -> messageDispatcher.dispatch(msg));
        }
        // рассылаем всем, кроме отправителя
//                for (ClientSessionState session : connectedClients.values()) {
//                    if (session.ctx != ctx) {
//                        session.ctx.writeAndFlush(msg);
//                    }
//                }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            UiThreadExecutor.run(() -> {
                int playerId = ctx.channel().hashCode();
                connectedClients.remove(playerId);

                Multiplayer.Players.remove(playerId);

                NetworkManager.BundleMessage leaveMsg = new NetworkManager.BundleMessage("PLAYER_LEAVE", playerId);
                // рассылаем всем остальным
                for (ClientSessionState session : connectedClients.values()) {
                    session.ctx.writeAndFlush(leaveMsg);
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            String err = ErrorMessageUtil.getDetailedErrorMessage(cause);
            UiThreadExecutor.run(() -> {
                NetworkManager.getInstance().showMessage("Client error: " + err);
                // Optionally: remove the client from connectedClients
                ServerSession session = connectedClients.get(ctx.channel());
                if (session != null) {
                    connectedClients.remove(ctx.channel());
                    Multiplayer.Players.remove(session.playerId);
                }
            });
            ctx.close();
        }
    }
}