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
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.model.util.TextUtil;


import static org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit.NONE;
import static org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits.STOPBITS_1;
import static org.openremote.agent.protocol.serial.JSerialCommChannelOption.*;

/**
 * This is a {@link IOClient} implementation for serial ports.
 * <p>
 * Users of this {@link IOClient} are responsible for adding encoders for converting messages of type &lt;T&gt; to
 * {@link io.netty.buffer.ByteBuf} (see {@link MessageToByteEncoder}) and adding decoders to convert from
 * {@link io.netty.buffer.ByteBuf} to messages of type &lt;T&gt; and ensuring these decoded messages are passed back
 * to this client via {@link AbstractNettyIOClient#onMessageReceived} (see {@link ByteToMessageDecoder and
 * {@link MessageToMessageDecoder}).
 */
public class SerialIOClient<T> extends AbstractNettyIOClient<T, JSerialCommDeviceAddress> {

    protected String port;
    protected int baudRate;
    public static int DEFAULT_BAUD_RATE = 38400;

    public SerialIOClient(String port, Integer baudRate) {
        TextUtil.requireNonNullAndNonEmpty(port);
        this.port = port;
        this.baudRate = baudRate == null ? DEFAULT_BAUD_RATE : baudRate;
    }

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return JSerialCommChannel.class;
    }

    @Override
    protected ChannelFuture startChannel() {
        return bootstrap.connect(new JSerialCommDeviceAddress(port));
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

    @Override
    protected void configureChannel() {
        super.configureChannel();
        bootstrap.option(BAUD_RATE, baudRate);
        bootstrap.option(DATA_BITS, 8);
        bootstrap.option(STOP_BITS, STOPBITS_1);
        bootstrap.option(PARITY_BIT, NONE);
    }
}
