package network.SMTH;

import com.esotericsoftware.kryo.Kryo;

import io.netty.bootstrap.Bootstrap;
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
import io.netty.channel.socket.nio.NioSocketChannel;
import network.ClientAgent;
import network.MessageDispatcher;
import network.Multiplayer;
import network.NetworkManager;
import network.codec.ChannelPipelineFactory;
import network.states.ClientStateMachine;
import network.utils.ErrorMessageUtil;
import network.utils.UiThreadExecutor;

// network/client/MultiplayerClient.java
public class MultiplayerClient {
    private final Kryo kryo;
    private final MessageDispatcher messageDispatcher;
    private final ClientStateMachine clientStateMachine;
    private final ClientAgent clientAgent;

    private EventLoopGroup workerGroup;
    private Channel clientChannel;
    private int localPlayerId = -1;
    private String lastError;

    public MultiplayerClient(Kryo kryo, MessageDispatcher messageDispatcher,
                             ClientStateMachine clientStateMachine, ClientAgent clientAgent) {
        this.kryo = kryo;
        this.messageDispatcher = messageDispatcher;
        this.clientStateMachine = clientStateMachine;
        this.clientAgent = clientAgent;
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
                                ChannelPipelineFactory.addCodecs(p, kryo);
                                p.addLast(new ClientHandler());
                            }
                        })
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

                ChannelFuture f = b.connect(host, port).sync();
                clientChannel = f.channel();
                UiThreadExecutor.run(() -> {
                    Multiplayer.isMultiplayer = true;
                    Multiplayer.isHost = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                UiThreadExecutor.run(() -> {
                    clientStateMachine.onError("Connection failed");
                    NetworkManager.getInstance().showMessage("Connection failed");
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

    public void send(NetworkManager.BundleMessage msg) {
        if (isConnected()) {
            clientChannel.writeAndFlush(msg);
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<NetworkManager.BundleMessage> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            UiThreadExecutor.run(() -> {
                localPlayerId = ctx.channel().hashCode();
                lastError = null;
                // clientAgent.onConnected(); // если нужно
            });
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, NetworkManager.BundleMessage msg) {
            UiThreadExecutor.run(() -> messageDispatcher.dispatch(msg));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            UiThreadExecutor.run(() -> {
                if (!NetworkManager.getInstance().isDisconnecting()) {
                    String message = lastError != null ? "Connection lost: " + lastError
                            : "Connection lost: Server might be offline";
                    NetworkManager.getInstance().showMessage(message);
                    clientStateMachine.onError(message);
                }
                Multiplayer.isMultiplayer = false;
                Multiplayer.isHost = false;
                Multiplayer.Players.clear();
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            lastError = ErrorMessageUtil.getDetailedErrorMessage(cause);
            clientStateMachine.onError(lastError);
            ctx.close();
        }
    }
}