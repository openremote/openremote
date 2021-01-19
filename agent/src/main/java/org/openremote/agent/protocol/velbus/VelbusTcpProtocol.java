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
package org.openremote.agent.protocol.velbus;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.tcp.TcpIoClient;
import org.openremote.model.util.TextUtil;

import java.util.Objects;

public class VelbusTcpProtocol extends AbstractVelbusProtocol<VelbusTcpProtocol, VelbusTcpAgent> {

    public static final String PROTOCOL_DISPLAY_NAME = "VELBUS TCP";

    public VelbusTcpProtocol(VelbusTcpAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    protected IoClient<VelbusPacket> createIoClient(VelbusTcpAgent agent) throws RuntimeException {

        // Extract IP and Host
        String host = agent.getHost().orElse(null);
        Integer port = agent.getPort().orElse(null);

        TextUtil.requireNonNullAndNonEmpty(host, "Host cannot be null or empty");
        Objects.requireNonNull(port, "Port cannot be null");
        TcpIoClient<VelbusPacket> client = new TcpIoClient<>(host, port);
        client.setEncoderDecoderProvider(
            () -> new ChannelHandler[] {
                new VelbusPacketEncoder(),
                new VelbusPacketDecoder(),
                new AbstractNettyIoClient.MessageToMessageDecoder<>(VelbusPacket.class, client)
            }
        );
        return client;
    }
}
