/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.zwave;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.protocol.zwave.model.Controller;
import org.openremote.protocol.zwave.port.ZWavePortConfiguration;

public class NettyZWControllerFactory implements ZWControllerFactory {

    private final ProtocolExecutorService executorService;
    private final ZWavePortConfiguration configuration;

    public NettyZWControllerFactory(ZWavePortConfiguration configuration, ProtocolExecutorService executorService) {
        this.configuration = configuration;
        this.executorService = executorService;
    }

    @Override
    public Controller createController(IoClient<SerialDataPacket> messageProcessor) {
        return new Controller(NettyConnectionManager.create(configuration, messageProcessor));
    }

    @Override
    public IoClient<SerialDataPacket> createMessageProcessor() {
        ZWSerialClient client = new ZWSerialClient(configuration.getComPort(), 115200, executorService);
        client.setEncoderDecoderProvider(
            () -> new ChannelHandler[] {
                new ZWPacketEncoder(),
                new ZWPacketDecoder(),
                new AbstractNettyIoClient.MessageToMessageDecoder<>(SerialDataPacket.class, client)
            }
        );
        return client;
    }
}
