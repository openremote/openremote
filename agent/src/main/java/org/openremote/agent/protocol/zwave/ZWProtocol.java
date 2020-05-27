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
import org.openremote.agent.protocol.ProtocolConfigurationDiscovery;
import org.openremote.agent.protocol.ProtocolLinkedAttributeDiscovery;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.AttributeValueType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.attribute.MetaItemDescriptorImpl;
import org.openremote.model.file.FileInfo;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import org.openremote.protocol.zwave.port.ZWavePortConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.zwave.ZWConfiguration.getEndpointIdAsString;
import static org.openremote.agent.protocol.zwave.ZWConfiguration.getZWEndpoint;
import static org.openremote.agent.protocol.zwave.ZWConfiguration.getZWLinkName;
import static org.openremote.agent.protocol.zwave.ZWConfiguration.getZWNodeId;
import static org.openremote.agent.protocol.zwave.ZWConfiguration.initProtocolConfiguration;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE;

public class ZWProtocol extends AbstractProtocol implements ProtocolLinkedAttributeDiscovery,
    ProtocolConfigurationDiscovery, ProtocolLinkedAttributeImport {

    // Constants ------------------------------------------------------------------------------------

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":zwave";
    public static final String PROTOCOL_DISPLAY_NAME = "Z-Wave";
    public static final String VERSION = "1.0";
    public static final String META_ZWAVE_SERIAL_PORT = PROTOCOL_NAME + ":port";
    public static final String META_ZWAVE_DEVICE_NODE_ID = PROTOCOL_NAME + ":deviceNodeId";
    public static final String META_ZWAVE_DEVICE_ENDPOINT = PROTOCOL_NAME + ":deviceEndpoint";
    public static final String META_ZWAVE_DEVICE_VALUE_LINK = PROTOCOL_NAME + ":deviceValueLink";
    public static final int DEFAULT_ENDPOINT = 0;


    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Collections.singletonList(
        new MetaItemDescriptorImpl(
            META_ZWAVE_SERIAL_PORT,
            ValueType.STRING,
            true,
            null,
            null,
            1,
            null,
            false,
            null, null, null
        )
    );

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl(
            META_ZWAVE_DEVICE_NODE_ID,
            ValueType.NUMBER,
            true,
            "^([1-9]\\d{0,1}|1[0-9][0-9]|2[0-2][0-9]|23[0-2])$", //1-232
            "1-232",
            1,
            null,
            false,
            null, null, null
        ),
        new MetaItemDescriptorImpl(
            META_ZWAVE_DEVICE_ENDPOINT,
            ValueType.NUMBER,
            true,
            REGEXP_PATTERN_INTEGER_POSITIVE,
            MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            1,
            Values.create(DEFAULT_ENDPOINT),
            false,
            null, null, null
        ),
        new MetaItemDescriptorImpl(
            META_ZWAVE_DEVICE_VALUE_LINK,
            ValueType.STRING,
            true,
            null,
            null,
            1,
            null,
            false,
            null, null, null
        )
    );



    // Class Members --------------------------------------------------------------------------------

    public static final Logger LOG = Logger.getLogger(ZWProtocol.class.getName());


    // Protected Instance Fields --------------------------------------------------------------------

    protected final Map<String, ZWNetwork> networkMap = new HashMap<>();
    protected final Map<AttributeRef,  Pair<ZWNetwork, Consumer<ConnectionStatus>>> networkConfigurationMap = new HashMap<>();
    protected final Map<AttributeRef, Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value>> sensorValueConsumerMap = new HashMap<>();


    // Implements Protocol --------------------------------------------------------------------------

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }


    // Implements AbstractProtocol ------------------------------------------------------------------

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = new ArrayList<>(PROTOCOL_META_ITEM_DESCRIPTORS.size());
        descriptors.addAll(PROTOCOL_META_ITEM_DESCRIPTORS);
        return descriptors;
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            ZWConfiguration.validateSerialConfiguration(protocolConfiguration, result);
        }
        return result;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(new MetaItem(META_ZWAVE_SERIAL_PORT, null));
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = new ArrayList<>(ATTRIBUTE_META_ITEM_DESCRIPTORS.size());
        descriptors.addAll(ATTRIBUTE_META_ITEM_DESCRIPTORS);
        return descriptors;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        Optional<String> networkId = getUniqueNetworkIdentifier(protocolConfiguration);

        if (!networkId.isPresent()) {
            LOG.severe("No serial port provided for Z-Wave protocol configuration: " + protocolConfiguration);
            updateStatus(protocolRef, ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        Consumer<ConnectionStatus> statusConsumer = status -> executorService.execute(
            () -> updateStatus(protocolRef, status)
         );

        ZWNetwork zwNetwork = null;

        synchronized (this) {
            zwNetwork = networkMap.computeIfAbsent(
                networkId.get(),
                port -> {
                    ZWControllerFactory factory = createControllerFactory(port);
                    ZWNetwork network = new ZWNetwork(factory);
                    network.addConnectionStatusConsumer(statusConsumer);
                    return network;
                }
            );
            networkConfigurationMap.put(protocolRef, new Pair<>(zwNetwork, statusConsumer));
        }

        if (protocolConfiguration.isEnabled() && zwNetwork.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            zwNetwork.connect();
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        String networkId = getUniqueNetworkIdentifier(protocolConfiguration).orElse("");

        // updateStatus(protocolRef, ConnectionStatus.DISCONNECTED);

        ZWNetwork zwNetworkToDisconnect  = null;

        synchronized (this) {
            Pair<ZWNetwork, Consumer<ConnectionStatus>> zwNetworkAndConsumer= networkConfigurationMap.remove(protocolRef);

            if (zwNetworkAndConsumer != null) {
                zwNetworkAndConsumer.key.removeConnectionStatusConsumer(zwNetworkAndConsumer.value);

                // Check if network is no longer used
                if (networkConfigurationMap.isEmpty() ||
                    networkConfigurationMap.values()
                        .stream()
                        .noneMatch(networkStatusConsumer -> zwNetworkAndConsumer.key.equals(networkStatusConsumer.key)))
                {
                    networkMap.remove(networkId);
                    zwNetworkToDisconnect = zwNetworkAndConsumer.key;
                }
            }
        }

        if (zwNetworkToDisconnect != null) {
            zwNetworkToDisconnect.disconnect();
        }
    }

    @Override
    protected synchronized void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        Pair<ZWNetwork, Consumer<ConnectionStatus>> zwNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (zwNetworkConsumerPair == null) {
            LOG.info("Protocol Configuration doesn't have a valid ZWNetwork so cannot link");
            return;
        }

        ZWNetwork zwNetwork = zwNetworkConsumerPair.key;

        int nodeId = getZWNodeId(attribute);
        int endpoint = getZWEndpoint(attribute);
        String linkName = getZWLinkName(attribute);

        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        ValueType valueType = attribute.getType().orElse(AttributeValueType.STRING).getValueType();
        LOG.fine("Linking attribute to device endpoint '" + getEndpointIdAsString(attribute) + "' and channel '" + linkName + "': " + attributeRef);

        Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value> sensorValueConsumer = value ->
            updateLinkedAttribute(new AttributeState(attributeRef, toAttributeValue(value, valueType)));

        sensorValueConsumerMap.put(attributeRef, sensorValueConsumer);
        zwNetwork.addSensorValueConsumer(nodeId, endpoint, linkName, sensorValueConsumer);
    }

    @Override
    protected synchronized void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        Pair<ZWNetwork, Consumer<ConnectionStatus>> zwNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (zwNetworkConsumerPair == null) {
            return;
        }

        ZWNetwork zwNetwork = zwNetworkConsumerPair.key;
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value> sensorValueConsumer = sensorValueConsumerMap.remove(attributeRef);
        zwNetwork.removeSensorValueConsumer(sensorValueConsumer);
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        Pair<ZWNetwork, Consumer<ConnectionStatus>> zwNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (zwNetworkConsumerPair == null) {
            return;
        }

        ZWNetwork zwNetwork = zwNetworkConsumerPair.key;
        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        if (attribute == null) {
            return;
        }

        int nodeId = getZWNodeId(attribute);
        int endpoint = getZWEndpoint(attribute);
        String linkName = getZWLinkName(attribute);

        zwNetwork.writeChannel(nodeId, endpoint, linkName, processedValue);
    }


    // Implements ProtocolConfigurationDiscovery --------------------------------------------------

    @Override
    public AssetAttribute[] discoverProtocolConfigurations() {
        return new AssetAttribute[] {
            initProtocolConfiguration(new AssetAttribute(), PROTOCOL_NAME)
        };
    }


    // Implements ProtocolLinkedAttributeDiscovery ------------------------------------------------

    @Override
    public synchronized AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration) {
        Pair<ZWNetwork, Consumer<ConnectionStatus>> zwNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (zwNetworkConsumerPair == null) {
            return new AssetTreeNode[0];
        }

        ZWNetwork zwNetwork = zwNetworkConsumerPair.key;
        try {
            return zwNetwork.discoverDevices(protocolConfiguration);
        } catch(Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            LOG.severe(errors.toString());
            throw e;
        }
    }


    // Implements ProtocolLinkedAttributeImport ---------------------------------------------------

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {
        // TODO : remove the ProtocolLinkedAttributeImport interface implementation. It has only been added because
        //        the manager GUI doesn't (currently) work at all without it.
        return new AssetTreeNode[0];
    }


    // Protected Instance Methods -----------------------------------------------------------------

    protected ZWControllerFactory createControllerFactory(String serialPort) {
        ZWavePortConfiguration configuration = new ZWavePortConfiguration();
        configuration.setCommLayer(ZWavePortConfiguration.CommLayer.NETTY);
        configuration.setComPort(serialPort);
        ZWControllerFactory factory = new NettyZWControllerFactory(configuration, executorService);
        return factory;
    }


    // Private Instance Methods -------------------------------------------------------------------

    private Optional<String> getUniqueNetworkIdentifier(AssetAttribute protocolConfiguration) {

        // TODO : Z-Wave Home ID

        return protocolConfiguration
            .getMetaItem(META_ZWAVE_SERIAL_PORT)
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    private org.openremote.model.value.Value toAttributeValue(org.openremote.protocol.zwave.model.commandclasses.channel.value.Value zwValue, ValueType type) {
        Value retValue = null;

        switch(type) {
            case OBJECT:
                break;
            case ARRAY:
                retValue = toAttributeArray(zwValue);
                break;
            case STRING:
                String strVal = zwValue.getString();
                if (strVal != null) {
                    retValue = Values.create(strVal);
                }
                break;
            case NUMBER:
                Double d = zwValue.getNumber();
                if (d != null) {
                    retValue = Values.create(d);
                }
                break;
            case BOOLEAN:
                Boolean b = zwValue.getBoolean();
                if (b != null) {
                    retValue = Values.create(b);
                }
                break;
        }
        return retValue;
    }

    private org.openremote.model.value.ArrayValue toAttributeArray(org.openremote.protocol.zwave.model.commandclasses.channel.value.Value value) {
        if (value == null) {
            return null;
        }
        ArrayValue retArray = null;
        if (value instanceof org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue) {
            org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue zwArray = (org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue)value;
            retArray = Values.createArray();
            for (int i = 0; i < zwArray.length(); i++) {
                retArray.add(toAttributeArrayItem(zwArray.get(i)));
            }
            if (retArray.stream().anyMatch(val -> val == null)) {
                return null;
            }
        }
        return retArray;
    }

    private org.openremote.model.value.Value toAttributeArrayItem(org.openremote.protocol.zwave.model.commandclasses.channel.value.Value zwValue) {
        if (zwValue == null) {
            return null;
        }
        org.openremote.model.value.Value retValue = null;
        switch(zwValue.getType()) {
            case NUMBER:
                Double d = zwValue.getNumber();
                if (d != null) {
                    retValue = Values.create(d);
                }
                break;
            case INTEGER:
                Integer i = zwValue.getInteger();
                if (i != null) {
                    retValue = Values.create(Double.valueOf(i));
                }
                break;
            case BOOLEAN:
                Boolean b = zwValue.getBoolean();
                if (b != null) {
                    retValue = Values.create(b);
                }
                break;
            case STRING:
                String s = zwValue.getString();
                if (s != null) {
                    retValue = Values.create(s);
                }
                break;
        }
        return retValue;
    }
}
