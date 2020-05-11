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
package org.openremote.agent.protocol.serial;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.model.util.TextUtil;

/**
 * This is a {@link IoClient} implementation for serial ports.
 * <p>
 * Users of this {@link IoClient} are responsible for adding encoders for converting messages of type &lt;T&gt; to
 * {@link io.netty.buffer.ByteBuf} (see {@link MessageToByteEncoder}) and adding decoders to convert from
 * {@link io.netty.buffer.ByteBuf} to messages of type &lt;T&gt; and ensuring these decoded messages are passed back
 * to this client via {@link AbstractNettyIoClient#onMessageReceived} (see {@link ByteToMessageDecoder and
 * {@link MessageToMessageDecoder}).
 */
public class SerialIoClient<T> extends AbstractNettyIoClient<T, NrJavaSerialAddress> {

    protected String port;
    protected int baudRate;
    public static int DEFAULT_BAUD_RATE = 38400;

    public SerialIoClient(String port, Integer baudRate, ProtocolExecutorService executorService) {
        super(executorService);
        TextUtil.requireNonNullAndNonEmpty(port);
        this.port = port;
        this.baudRate = baudRate == null ? DEFAULT_BAUD_RATE : baudRate;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return NrJavaSerialChannel.class;
    }

    @Override
    protected ChannelFuture startChannel() {
        return bootstrap.connect(new NrJavaSerialAddress(port, baudRate));
    }

    @Override
    public String getClientUri() {
        return "serial://" + port;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected io.netty.channel.EventLoopGroup getWorkerGroup() {
        // Note that OioEventLoopGroup has to be used because NioEventLoopGroup is *NOT* compatible
        // with io.netty.channel.rxtx.RxtxChannel and causes IllegalStateException.
        return new io.netty.channel.oio.OioEventLoopGroup(1);
    }
}
