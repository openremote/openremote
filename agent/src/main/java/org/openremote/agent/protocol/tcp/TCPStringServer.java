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

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

/**
 * This is an {@link AbstractTCPServer} implementation that handles {@link String} messages.
 * <p>
 * Uses {@link DelimiterBasedFrameDecoder} and {@link StringDecoder}.
 */
public class TCPStringServer extends AbstractTCPServer<String> {

    protected String delimiter;
    protected int maxFrameLength;
    protected boolean stripDelimiter;

    public TCPStringServer(InetSocketAddress localAddress, String delimiter, int maxFrameLength, boolean stripDelimiter) {
        super(localAddress);
        this.delimiter = delimiter;
        this.maxFrameLength = maxFrameLength;
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    protected void addDecoders(SocketChannel channel) {
        // Add delimiter and string decoders to do the work
        addDecoder(channel, new DelimiterBasedFrameDecoder(maxFrameLength, stripDelimiter, Unpooled.wrappedBuffer(delimiter.getBytes())));
        addDecoder(channel, new StringDecoder());
    }

    @Override
    protected void addEncoders(SocketChannel channel) {
        addEncoder(channel, new StringEncoder());
    }
}
