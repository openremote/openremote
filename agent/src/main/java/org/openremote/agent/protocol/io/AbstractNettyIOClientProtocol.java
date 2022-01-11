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
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.Protocol;

import java.util.function.Supplier;

/**
 * This is an abstract {@link Protocol} for protocols that require an {@link NettyIOClient}.
 */
public abstract class AbstractNettyIOClientProtocol<T extends AbstractIOClientProtocol<T, U, V, W, X>, U extends IOAgent<U, T, X>, V, W extends NettyIOClient<V>, X extends AgentLink<?>> extends AbstractIOClientProtocol<T, U, V, W, X> {
    protected AbstractNettyIOClientProtocol(U agent) {
        super(agent);
    }

    @Override
    protected W createIoClient() throws Exception {
        W client = super.createIoClient();
        Supplier<ChannelHandler[]> encoderDecoderProvider = getEncoderDecoderProvider();
        client.setEncoderDecoderProvider(encoderDecoderProvider);
        return client;
    }

    /**
     * Gets the Netty {@link ChannelHandler}s for the IO client
     */
    protected abstract Supplier<ChannelHandler[]> getEncoderDecoderProvider();
}
