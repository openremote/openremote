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
package org.openremote.agent.protocol;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ThreadPerChannelEventLoopGroup;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.rxtx.RxtxChannel;
import io.netty.channel.rxtx.RxtxDeviceAddress;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link MessageProcessor} implementation for serial ports.
 */
public abstract class AbstractSerialMessageProcessor<T> extends AbstractNettyMessageProcessor<T> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractSerialMessageProcessor.class);
    protected String port;
    protected int baudRate;

    public AbstractSerialMessageProcessor(String port, Integer baudRate, ProtocolExecutorService executorService) {
        super(executorService);
        TextUtil.requireNonNullAndNonEmpty(port);
        Objects.requireNonNull(baudRate);
        this.port = port;
        this.baudRate = baudRate;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NrJavaSerialChannel.class;
    }

    @Override
    protected SocketAddress getSocketAddress() {
        return new NrJavaSerialAddress(port, 38400);
    }

    @Override
    protected String getSocketAddressString() {
        return port;
    }

    @Override
    protected EventLoopGroup getWorkerGroup() {
        return new OioEventLoopGroup(1);
    }
}
