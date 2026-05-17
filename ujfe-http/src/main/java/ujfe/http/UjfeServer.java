package ujfe.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import ujfe.live.LiveSession;

import java.util.Objects;

public final class UjfeServer {
    private final UjfeServerConfig config;
    private final LiveSession liveSession;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public UjfeServer(UjfeServerConfig config, LiveSession liveSession) {
        this.config = Objects.requireNonNull(config, "config");
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(config.maxJsonPayloadBytes()))
                                .addLast(new UjfeHttpHandler(liveSession, config.maxJsonPayloadBytes()));
                    }
                });

        channel = bootstrap.bind(config.host(), config.port()).sync().channel();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (channel != null) {
            channel.closeFuture().sync();
        }
    }

    public void stop() {
        liveSession.close();
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
