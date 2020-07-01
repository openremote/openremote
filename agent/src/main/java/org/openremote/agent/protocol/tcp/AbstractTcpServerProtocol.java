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

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Values;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

/**
 * This is an abstract protocol for creating TCP Server protocols. This allows TCP clients to connect and exchange data.
 * It is up to the concrete implementations whether or not linked {@link Attribute}s can be used with this protocol and
 * to define the semantics of these linked attributes.
 * <p>
 * <h1>Protocol Configurations</h1>
 * An instance is created by defining a {@link ProtocolConfiguration} with the following {@link MetaItem}s:
 * <ul>
 * <li>{@link #META_PROTOCOL_BIND_PORT} <b>(required)</b></li>
 * <li>{@link #META_PROTOCOL_BIND_ADDRESS}</li>
 * </ul>
 */
public abstract class AbstractTcpServerProtocol<T extends AbstractTcpServer<U>, U> extends AbstractProtocol {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":tcpServer";

    /**
     * Sets the port that this server will bind to.
     */
    public static final String META_PROTOCOL_BIND_PORT = PROTOCOL_NAME + ":bindPort";
    /**
     * Sets the network interface that this server will bind to; if not defined then the server
     * will bind to all network interfaces.
     */
    public static final String META_PROTOCOL_BIND_ADDRESS = PROTOCOL_NAME + ":bindAddress";

    private static final Logger LOG = Logger.getLogger(AbstractTcpServerProtocol.class.getName());
    protected final Map<AttributeRef, T> tcpServerMap = new HashMap<>();

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        int port = protocolConfiguration.getMetaItem(META_PROTOCOL_BIND_PORT)
            .flatMap(AbstractValueHolder::getValueAsInteger)
            .orElseThrow(() ->
                 new IllegalArgumentException("Missing or invalid require meta item: " + META_PROTOCOL_BIND_PORT));

        Optional<StringValue> bindAddress = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_BIND_ADDRESS,
            StringValue.class,
            false,
            true
        );

        LOG.info("Creating TCP server instance");
        T tcpServer = createTcpServer(port, bindAddress.map(StringValue::getString).orElse(null), protocolConfiguration);
        Consumer<ConnectionStatus> connectionStatusConsumer = connectionStatus ->
                onServerConnectionStatusChanged(protocolRef, connectionStatus);
        tcpServer.addConnectionStatusConsumer(connectionStatusConsumer);
        tcpServerMap.put(protocolRef, tcpServer);
        startTcpServer(protocolRef, tcpServer);
    }

    protected void onServerConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        T tcpServer = tcpServerMap.remove(protocolRef);
        if (tcpServer == null) {
            return;
        }

        LOG.info("Removing TCP server instance");
        tcpServer.removeAllConnectionStatusConsumers();
        stopTcpServer(protocolRef, tcpServer);
        updateStatus(protocolRef, ConnectionStatus.DISCONNECTED);
    }

    protected abstract T createTcpServer(int port, String bindAddress, AssetAttribute protocolConfiguration);

    protected void startTcpServer(AttributeRef protocolRef, T tcpServer) {
        LOG.info("Starting TCP server instance");
        tcpServer.start();
    }

    protected void stopTcpServer(AttributeRef protocolRef, T tcpServer) {
        LOG.info("Stopping TCP server instance");
        tcpServer.stop();
    }
}
