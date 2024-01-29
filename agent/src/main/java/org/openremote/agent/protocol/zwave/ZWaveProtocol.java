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
import org.openremote.model.value.impl.ColourRGB;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.Value;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class ZWaveProtocol extends AbstractProtocol<ZWaveAgent, ZWaveAgentLink> implements ProtocolAssetDiscovery {

    // Constants ------------------------------------------------------------------------------------

    public static final String PROTOCOL_DISPLAY_NAME = "Z-Wave";

    // Class Members --------------------------------------------------------------------------------

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ZWaveProtocol.class.getName());

    // Protected Instance Fields --------------------------------------------------------------------

    protected ZWaveNetwork network;
    protected Map<AttributeRef, Consumer<Value>> sensorValueConsumerMap = new HashMap<>();

    public ZWaveProtocol(ZWaveAgent agent) {
        super(agent);
    }

    // Implements Protocol --------------------------------------------------------------------------

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public synchronized String getProtocolInstanceUri() {
        return network != null && network.ioClient != null ? network.ioClient.getClientUri() : "";
    }

    // Implements AbstractProtocol ------------------------------------------------------------------

    @Override
    protected synchronized void doStart(Container container) throws Exception {
        String serialPort = agent.getSerialPort().orElseThrow(() -> new IllegalStateException("Invalid serial port property"));
        network = new ZWaveNetwork(serialPort, executorService);
        network.addConnectionStatusConsumer(this::setConnectionStatus);
        network.connect();
    }

    @Override
    protected synchronized void doStop(Container container) throws Exception {
        if (network != null) {
            network.removeConnectionStatusConsumer(this::setConnectionStatus);
            network.disconnect();
            network = null;
        }
    }

    @Override
    protected synchronized void doLinkAttribute(String assetId, Attribute<?> attribute, ZWaveAgentLink agentLink) {
        if (network == null) {
            return;
        }
        int nodeId = agentLink.getDeviceNodeId().orElse(0);
        int endpoint = agentLink.getDeviceEndpoint().orElse(0);
        String linkName = agentLink.getDeviceValue().orElse("");
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        Class<?> clazz = attribute.getTypeClass();
        Consumer<Value> sensorValueConsumer = value ->
            updateLinkedAttribute(new AttributeState(attributeRef, toAttributeValue(value, clazz)));

        sensorValueConsumerMap.put(attributeRef, sensorValueConsumer);
        network.addSensorValueConsumer(nodeId, endpoint, linkName, sensorValueConsumer);
    }

    @Override
    protected synchronized void doUnlinkAttribute(String assetId, Attribute<?> attribute, ZWaveAgentLink agentLink) {
        if (network == null) {
            return;
        }
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Consumer<Value> sensorValueConsumer = sensorValueConsumerMap.remove(attributeRef);
        network.removeSensorValueConsumer(sensorValueConsumer);
    }

    @Override
    protected synchronized void doLinkedAttributeWrite(ZWaveAgentLink agentLink, AttributeEvent event, Object processedValue) {
        if (network == null) {
            return;
        }
        int nodeId = getOrThrowAgentLinkProperty(agentLink.getDeviceNodeId(), "device node ID");
        int endpoint = getOrThrowAgentLinkProperty(agentLink.getDeviceEndpoint(), "device endpoint");
        String linkName = getOrThrowAgentLinkProperty(agentLink.getDeviceValue(), "device property");
        network.writeChannel(nodeId, endpoint, linkName, processedValue);
    }


    // Implements ProtocolAssetDiscovery ------------------------------------------------

    @Override
    public synchronized Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer) {
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


    // Private Instance Methods -------------------------------------------------------------------

    private Object toAttributeValue(org.openremote.protocol.zwave.model.commandclasses.channel.value.Value value, Class<?> clazz) {
        if (value == null || clazz == null) {
            return null;
        }
        Object retValue = null;
        if (clazz == String.class) {
            retValue = value.getString();
        } else if (clazz == Boolean.class) {
            retValue = value.getBoolean();
        } else if (clazz == Double.class) {
            retValue = value.getNumber();
        } else if (clazz == Integer.class) {
            retValue = value.getInteger();
        } else if (clazz == ColourRGB.class && value instanceof org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue zwArray) {
            if (zwArray.length() >= 3) {
                List<Object> values = new ArrayList<>(zwArray.length());
                for (int i = 0; i < zwArray.length(); i++) {
                    values.add(toAttributeValue(zwArray.get(i), Integer.class));
                }
                if (values.stream().anyMatch(Objects::isNull)) {
                    return null;
                }
                int offset = (zwArray.length() == 3 ? 0 : 1); // RGB : ARGB
                retValue = new ColourRGB(
                    (Integer) values.get(offset), (Integer) values.get(1 + offset), (Integer) values.get(2 + offset)
                );
            }
        } else {
            LOG.warning("Couldn't update Z-Wave sensor value because of unexpected attribute type: " + clazz);
        }
        return retValue;
    }
}
