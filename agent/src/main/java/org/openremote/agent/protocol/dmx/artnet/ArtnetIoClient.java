package org.openremote.agent.protocol.dmx.artnet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.udp.UdpIoClient;
import org.openremote.model.syslog.SyslogCategory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link IoClient} implementation for UDP.
 * <p>
 * Users of this {@link IoClient} are responsible for adding encoders for converting messages of type &lt;T&gt; to
 * {@link io.netty.buffer.ByteBuf} (see {@link MessageToByteEncoder}) and adding decoders to convert from
 * {@link io.netty.buffer.ByteBuf} to messages of type &lt;T&gt; and ensuring these decoded messages are passed back
 * to this client via {@link AbstractNettyIoClient#onMessageReceived} (see {@link ByteToMessageDecoder and
 * {@link MessageToMessageDecoder}).
 */
public class ArtnetIoClient<T> extends AbstractNettyIoClient<T, InetSocketAddress> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, UdpIoClient.class);
    protected String host;
    protected int port;
    protected int bindPort;

    public ArtnetIoClient(String host, Integer port, Integer bindPort, ProtocolExecutorService executorService) {
        super(executorService);

        if (port == null) {
            port = 0;
        } else if (port < 1 || port > 65536) {
            throw new IllegalArgumentException("Port must be between 1 and 65536");
        }

        if (bindPort == null) {
            bindPort = 0;
        } else if (bindPort < 1 || bindPort > 65536) {
            throw new IllegalArgumentException("Bind port must be between 1 and 65536");
        }

        this.host = host;
        this.port = port;
        this.bindPort = bindPort;
    }

    @Override
    protected void addEncodersDecoders(Channel channel) {
        channel.pipeline().addLast(new MessageToMessageEncoder<ByteBuf>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
                out.add(new DatagramPacket(msg.retain(), host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(port)));
            }
        });

        super.addEncodersDecoders(channel);

        channel.pipeline().addFirst(new io.netty.handler.codec.MessageToMessageDecoder<DatagramPacket>() {
            @Override
            protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
                out.add(msg.content().retain());
            }
        });
    }

    @Override
    protected ChannelFuture startChannel() {
        return bootstrap.bind(bindPort);
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NioDatagramChannel.class;
    }

    @Override
    public String getClientUri() {
        return "udp://" + (host != null ? host : "0.0.0.0") + ":" + port + " (bindPort: " + bindPort + ")";
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
        return new NioEventLoopGroup(1);
    }

    @Override
    protected void configureChannel() {
        super.configureChannel();
        //bootstrap.option(ChannelOption.SO_BROADCAST, true);
    }

    @Override
    protected synchronized void scheduleReconnect() {
        // No connection to reconnect
    }
}