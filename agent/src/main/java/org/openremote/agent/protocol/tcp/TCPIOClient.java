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
package org.openremote.agent.protocol.tcp;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.model.util.TextUtil;

import java.net.InetSocketAddress;

/**
 * This is a {@link IOClient} implementation for TCP.
 * <p>
 * Users of this {@link IOClient} are responsible for adding encoders for converting messages of type &lt;T&gt; to
 * {@link io.netty.buffer.ByteBuf} (see {@link MessageToByteEncoder}) and adding decoders to convert from
 * {@link io.netty.buffer.ByteBuf} to messages of type &lt;T&gt; and ensuring these decoded messages are passed back
 * to this client via {@link AbstractNettyIOClient#onMessageReceived} (see {@link ByteToMessageDecoder and
 * {@link MessageToMessageDecoder}).
 */
public class TCPIOClient<T> extends AbstractNettyIOClient<T, InetSocketAddress> {

    protected String host;
    protected int port;

    public TCPIOClient(String host, int port) {
        TextUtil.requireNonNullAndNonEmpty(host);
        this.host = host;
        this.port = port;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    public String getClientUri() {
        return "tcp://" + host + ":" + port;
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
        return new NioEventLoopGroup(1);
    }

    @Override
    protected ChannelFuture startChannel() {
        return bootstrap.connect(new InetSocketAddress(host, port));
    }

    @Override
    protected void configureChannel() {
        super.configureChannel();
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }
}
