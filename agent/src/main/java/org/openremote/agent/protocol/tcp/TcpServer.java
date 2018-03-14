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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.openremote.agent.protocol.io.AbstractIoServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Abstract IO Server for creating TCP socket servers that clients can connect to. Concrete implementation need
 * to configure the pipeline with the necessary encoders and decoders to ensure messages of type &lt;T&gt; are
 * generated/consumed at/from the end/start of the pipeline.
 */
public abstract class TcpServer<T> extends AbstractIoServer<T, SocketChannel> {

    protected SocketAddress localAddress;

    public TcpServer(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    protected String getInstanceIdentifier() {
        return localAddress == null ? null : "tcp://" + localAddress;
    }

    @Override
    protected Class<? extends ServerChannel> getServerChannelClass() {
        return NioServerSocketChannel.class;
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    protected SocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    protected String getClientDescriptor(SocketChannel client) {
        return client == null || client.remoteAddress() == null ? null : "tcp://" + client.remoteAddress();
    }
}
