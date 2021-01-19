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
package org.openremote.agent.protocol.zwave;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.Value;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ZWProtocol extends AbstractProtocol<ZWAgent, ZWAgent.ZWAgentLink> implements ProtocolAssetDiscovery {

    // Constants ------------------------------------------------------------------------------------

    public static final String PROTOCOL_DISPLAY_NAME = "Z-Wave";

    // Class Members --------------------------------------------------------------------------------

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ZWProtocol.class.getName());

    // Protected Instance Fields --------------------------------------------------------------------

    protected ZWNetwork network;
    protected Map<AttributeRef, Consumer<Value>> sensorValueConsumerMap;

    public ZWProtocol(ZWAgent agent) {
        super(agent);
    }

    // Implements Protocol --------------------------------------------------------------------------

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return network != null && network.ioClient != null ? network.ioClient.getClientUri() : "";
    }

    // Implements AbstractProtocol ------------------------------------------------------------------

    @Override
    protected void doStart(Container container) throws Exception {
        String serialPort = agent.getSerialPort().orElseThrow(() -> new IllegalStateException("Invalid serial port property"));
        ZWNetwork network = new ZWNetwork(serialPort, executorService);
        network.addConnectionStatusConsumer(this::setConnectionStatus);
        network.connect();
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (network != null) {
            network.removeConnectionStatusConsumer(this::setConnectionStatus);
            network.disconnect();
        }
    }

    @Override
    protected synchronized void doLinkAttribute(String assetId, Attribute<?> attribute, ZWAgent.ZWAgentLink agentLink) {

        int nodeId = agentLink.getDeviceNodeId().orElse(0);
        int endpoint = agentLink.getDeviceEndpoint().orElse(0);
        String linkName = agentLink.getDeviceValue().orElse("");
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        // TODO: Value must be compatible with the value type of the attribute...for non primitives the object types must match
        Consumer<Value> sensorValueConsumer = value ->
            updateLinkedAttribute(new AttributeState(attributeRef, value));

        sensorValueConsumerMap.put(attributeRef, sensorValueConsumer);
        network.addSensorValueConsumer(nodeId, endpoint, linkName, sensorValueConsumer);
    }

    @Override
    protected synchronized void doUnlinkAttribute(String assetId, Attribute<?> attribute, ZWAgent.ZWAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Consumer<Value> sensorValueConsumer = sensorValueConsumerMap.remove(attributeRef);
        network.removeSensorValueConsumer(sensorValueConsumer);
    }

    @Override
    protected void doLinkedAttributeWrite(Attribute<?> attribute, ZWAgent.ZWAgentLink agentLink, AttributeEvent event, Object processedValue) {

        int nodeId = getOrThrowAgentLinkProperty(agentLink.getDeviceNodeId(), "device node ID");
        int endpoint = getOrThrowAgentLinkProperty(agentLink.getDeviceEndpoint(), "device endpoint");
        String linkName = getOrThrowAgentLinkProperty(agentLink.getDeviceValue(), "device property");

        network.writeChannel(nodeId, endpoint, linkName, processedValue);
    }


    // Implements ProtocolAssetDiscovery ------------------------------------------------

    @Override
    public Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer) {

        if (network == null || network.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.info("Network not connected so cannot perform discovery");
            return CompletableFuture.completedFuture(null);
        }

        return executorService.submit(() -> {

            try {
                AssetTreeNode[] assetTreeNodes = network.discoverDevices(agent);
                assetConsumer.accept(assetTreeNodes);
            } catch(Exception e) {
                LOG.log(Level.SEVERE, "Exception thrown during asset discovery: " + this, e);
            }
        }, null);
    }
}
