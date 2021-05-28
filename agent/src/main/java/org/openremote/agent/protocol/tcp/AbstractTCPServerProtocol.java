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
import org.openremote.model.Container;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This is an abstract protocol for creating TCP Server protocols. This allows TCP clients to connect and exchange data.
 * It is up to the concrete implementations whether or not linked {@link Attribute}s can be used with this protocol and
 * to define the semantics of these linked attributes.
 */
public abstract class AbstractTCPServerProtocol<R, S extends AbstractTCPServer<R>, T extends AbstractTCPServerProtocol<R, S, T, U, V>, U extends AbstractTCPServerAgent<U, T, V>, V extends AgentLink<?>> extends AbstractProtocol<U, V> {

    private static final Logger LOG = Logger.getLogger(AbstractTCPServerProtocol.class.getName());

    protected S tcpServer;

    public AbstractTCPServerProtocol(U agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) throws Exception {

        int port = getAgent().getBindPort().orElseThrow(() ->
             new IllegalArgumentException("Missing or invalid attribute: " + AbstractTCPServerAgent.BIND_PORT));

        String bindAddress = getAgent().getBindHost().orElse(null);

        LOG.info("Creating TCP server instance");
        tcpServer = createTcpServer(port, bindAddress, agent);
        Consumer<ConnectionStatus> connectionStatusConsumer = this::onServerConnectionStatusChanged;
        tcpServer.addConnectionStatusConsumer(connectionStatusConsumer);
        startTcpServer();
    }

    protected void onServerConnectionStatusChanged(ConnectionStatus connectionStatus) {
        setConnectionStatus(connectionStatus);
    }

    @Override
    protected void doStop(Container container) throws Exception {

        if (tcpServer == null) {
            return;
        }

        LOG.info("Removing TCP server instance");
        tcpServer.removeAllConnectionStatusConsumers();
        stopTcpServer();
    }

    protected abstract S createTcpServer(int port, String bindAddress, U agent);

    protected void startTcpServer() {
        LOG.info("Starting TCP server instance");
        tcpServer.start();
    }

    protected void stopTcpServer() {
        LOG.info("Stopping TCP server instance");
        tcpServer.stop();
    }
}
