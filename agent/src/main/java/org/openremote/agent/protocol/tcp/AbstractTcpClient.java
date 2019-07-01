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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link IoClient} implementation for TCP.
 */
public abstract class AbstractTcpClient<T> extends AbstractNettyIoClient<T, InetSocketAddress> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractTcpClient.class);
    protected String host;
    protected int port;

    public AbstractTcpClient(String host, int port, ProtocolExecutorService executorService) {
        super(executorService);
        TextUtil.requireNonNullAndNonEmpty(host);
        this.host = host;
        this.port = port;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    protected String getSocketAddressString() {
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
