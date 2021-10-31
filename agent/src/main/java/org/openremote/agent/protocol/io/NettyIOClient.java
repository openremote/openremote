/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.io;

import io.netty.channel.ChannelHandler;

import java.util.function.Supplier;

/**
 * This is for IO clients that use Netty and are based on  {@link io.netty.channel.ChannelHandler}s for
 * encoding/decoding messages of type &lt;T&gt.
 *
 * @param <T> Defines the message type that the instance will encode/decode
 */
public interface NettyIOClient<T> extends IOClient<T> {

    /**
     * Allows appropriate encoders and decoders to be added to the message pipeline; if an {@link IOClient} doesn't
     * support this then an {@link UnsupportedOperationException} will be thrown, consult the {@link IOClient}'s documentation.
     */
    void setEncoderDecoderProvider(Supplier<ChannelHandler[]> encoderDecoderProvider) throws UnsupportedOperationException;
}
