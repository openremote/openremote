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
package org.openremote.agent.protocol.udp;

import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract UDP client protocol for communicating with UDP servers; concrete implementations must implement
 * {@link #getEncoderDecoderProvider} to provide encoders/decoders for messages of type &lt;T&gt;.
 */
public abstract class AbstractUdpClientProtocol<T> extends AbstractIoClientProtocol<T, UdpIoClient<T>> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractUdpClientProtocol.class);

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":abstractUdpClient";

    /**
     * Optionally sets the port that this UDP client will bind to (if not set then a random ephemeral port will be used)
     */
    public static final MetaItemDescriptor META_PROTOCOL_BIND_PORT = metaItemInteger(
            PROTOCOL_NAME + ":bindPort",
            ACCESS_PRIVATE,
            true,
            1,
            65536);

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_HOST,
        META_PROTOCOL_PORT,
        META_PROTOCOL_BIND_PORT
    );

    @Override
    protected UdpIoClient<T> createIoClient(AssetAttribute protocolConfiguration) throws Exception {
        String host = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_HOST,
            false,
            false
        ).flatMap(Values::getString).orElse(null);

        Integer port = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PORT,
            false,
            false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        Integer bindPort = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_BIND_PORT,
            false,
            false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        if (port != null && (port < 1 || port > 65536)) {
            throw new IllegalArgumentException("Port must be in the range 1-65536");
        }

        if (bindPort != null && (bindPort < 1 || bindPort > 65536)) {
            throw new IllegalArgumentException("Bind port must be in the range 1-65536 if specified");
        }

        return new UdpIoClient<>(host, port, bindPort, executorService);
    }
}
