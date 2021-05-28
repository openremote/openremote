/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.model.syslog.SyslogCategory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link IOClient} implementation for UDP.
 * <p>
 * Users of this {@link IOClient} are responsible for adding encoders for converting messages of type &lt;T&gt; to
 * {@link io.netty.buffer.ByteBuf} (see {@link MessageToByteEncoder}) and adding decoders to convert from
 * {@link io.netty.buffer.ByteBuf} to messages of type &lt;T&gt; and ensuring these decoded messages are passed back
 * to this client via {@link AbstractNettyIOClient#onMessageReceived} (see {@link ByteToMessageDecoder and
 * {@link MessageToMessageDecoder}).
 */
public class UDPIOClient<T> extends AbstractNettyIOClient<T, InetSocketAddress> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, UDPIOClient.class);
    protected String host;
    protected int port;
    protected int bindPort;

    public UDPIOClient(String host, Integer port, Integer bindPort) {
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
        return bootstrap.bind("0.0.0.0", bindPort);
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
        bootstrap.option(ChannelOption.SO_BROADCAST, true);
    }
}
