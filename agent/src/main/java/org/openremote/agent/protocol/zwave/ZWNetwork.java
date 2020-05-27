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

import org.openremote.agent.protocol.io.IoClient;
import org.openremote.controller.exception.ConfigurationException;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeValueType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;
import org.openremote.protocol.zwave.ConnectionException;
import org.openremote.protocol.zwave.model.Controller;
import org.openremote.protocol.zwave.model.ZWEndPoint;
import org.openremote.protocol.zwave.model.ZWManufacturerID;
import org.openremote.protocol.zwave.model.ZWaveNode;
import org.openremote.protocol.zwave.model.commandclasses.ZWCommandClass;
import org.openremote.protocol.zwave.model.commandclasses.ZWCommandClassID;
import org.openremote.protocol.zwave.model.commandclasses.ZWParameterByte;
import org.openremote.protocol.zwave.model.commandclasses.ZWParameterInt;
import org.openremote.protocol.zwave.model.commandclasses.ZWParameterItem;
import org.openremote.protocol.zwave.model.commandclasses.ZWParameterShort;
import org.openremote.protocol.zwave.model.commandclasses.channel.Channel;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.BooleanValue;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.NumberValue;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.StringValue;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_ENDPOINT;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_NODE_ID;
import static org.openremote.agent.protocol.zwave.ZWProtocol.META_ZWAVE_DEVICE_VALUE_LINK;
import static org.openremote.protocol.zwave.model.ZWNodeInitializerListener.NodeInitState.INITIALIZATION_FINISHED;

public class ZWNetwork {

    private final ZWControllerFactory controllerFactory;
    protected Controller controller;
    protected IoClient<SerialDataPacket> ioClient;
    private final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    private final Map<Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value>, ChannelConsumerLink> consumerLinkMap = new HashMap<>();

    public ZWNetwork(ZWControllerFactory controllerFactory) {
        if (controllerFactory == null) {
            throw new IllegalArgumentException("Missing controller factory.");
        }
        this.controllerFactory = controllerFactory;
    }

    public synchronized void connect() {
        if (controller != null) {
            return;
        }

        ioClient = controllerFactory.createMessageProcessor();
        ioClient.addConnectionStatusConsumer(this::onConnectionStatusChanged);
        controller = createController(ioClient);
        
        try {
            controller.connect();
        } catch (ConfigurationException e) {
            removeStatusChangedHandler(ioClient);
            controller = null;
            ioClient = null;
            onConnectionStatusChanged(ConnectionStatus.ERROR_CONFIGURATION);
        } catch (ConnectionException e) {
            removeStatusChangedHandler(ioClient);
            controller = null;
            ioClient = null;
            onConnectionStatusChanged(ConnectionStatus.ERROR_CONFIGURATION);
        }
    }

    public synchronized void disconnect() {
        if (controller == null) {
            return;
        }
        
        try {
            controller.disconnect();
        } catch (ConnectionException e) {

        } finally {
            removeStatusChangedHandler(ioClient);
            ioClient = null;
            controller = null;
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }
    }

    public synchronized ConnectionStatus getConnectionStatus() {
        if (ioClient != null) {
            return ioClient.getConnectionStatus();
        } else {
            return ConnectionStatus.DISCONNECTED;
        }
    }

    public synchronized void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.add(connectionStatusConsumer);
    }

    public synchronized void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    public synchronized void addSensorValueConsumer(int nodeId, int endpoint, String channelName, Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value> consumer) {
        ChannelConsumerLink link = ChannelConsumerLink.createLink(nodeId, endpoint, channelName, consumer, controller);
        consumerLinkMap.put(consumer, link);
    }

    public synchronized void removeSensorValueConsumer(Consumer<org.openremote.protocol.zwave.model.commandclasses.channel.value.Value> consumer) {
        ChannelConsumerLink link = consumerLinkMap.get(consumer);
        if (link != null) {
            consumerLinkMap.remove(consumer);
            link.unlink();
        }
    }

    public synchronized void writeChannel(int nodeId, int endpoint, String linkName, Value value) {
        Channel channel = findChannel(nodeId, endpoint, linkName);
        if (channel != null && value != null) {
            org.openremote.protocol.zwave.model.commandclasses.channel.value.Value zwValue = null;
            ValueType type = channel.getValueType();
            switch(type) {
                case STRING:
                     zwValue =Values.getString(value).map(val -> new StringValue(val)).orElse(null);
                    break;
                case NUMBER:
                    zwValue = Values.getNumber(value).map(val -> new NumberValue(val)).orElse(null);
                    break;
                case INTEGER:
                    zwValue = Values.getIntegerCoerced(value).map(val -> new NumberValue(val)).orElse(null);
                    break;
                case BOOLEAN:
                    zwValue = Values.getBoolean(value).map(val -> new BooleanValue(val)).orElse(null);
                    break;
                case ARRAY:
                    zwValue = toZWArray(Values.getArray(value).orElse(null));
            }

            if (zwValue != null) {
                channel.executeSetCommand(zwValue);
            }
        }
    }

    public synchronized AssetTreeNode[] discoverDevices(AssetAttribute protocolConfiguration) {
        if (controller == null) {
            return new AssetTreeNode[0];
        }

        MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());

        // Filter nodes
        
        List<ZWaveNode> nodes = controller.getNodes()
            .stream()
            .filter(node -> node.getState() == INITIALIZATION_FINISHED)
            .filter(node -> {
                // Do not add devices that have already been discovered
                List<ZWCommandClass> cmdClasses = new ArrayList<>();
                cmdClasses.addAll(node.getSupportedCommandClasses());
                for (ZWEndPoint curEndpoint : node.getEndPoints()) {
                    cmdClasses.addAll(curEndpoint.getCmdClasses());
                }
                return cmdClasses
                    .stream()
                    .map(ZWCommandClass::getChannels)
                    .flatMap(List<Channel>::stream)
                    .noneMatch(channel ->
                        consumerLinkMap.values().stream().anyMatch(link -> link.getChannel() == channel)
                    );
            })
            .collect(toList());

        List<AssetTreeNode> assetNodes = nodes
            .stream()
            .map(node -> {

                // Root device
                
                AssetTreeNode deviceNode = createDeviceNode(
                    agentLink,
                    node.getSupportedCommandClasses(),
                    node.getGenericDeviceClassID().getDisplayName(node.getSpecificDeviceClassID())
                );

                // Device Info

                Asset deviceInfoAsset = new Asset("Info", AssetType.THING);
                List<AssetAttribute> infoAttributes = createNodeInfoAttributes(node, agentLink);
                deviceInfoAsset.addAttributes(infoAttributes.toArray(new AssetAttribute[infoAttributes.size()]));
                deviceNode.addChild(new AssetTreeNode(deviceInfoAsset));

                // Sub device

                for (ZWEndPoint curEndpoint : node.getEndPoints()) {
                    String subDeviceName = curEndpoint.getGenericDeviceClassID().getDisplayName(curEndpoint.getSpecificDeviceClassID()) +
                                           " - " + curEndpoint.getEndPointNumber();
                    AssetTreeNode subDeviceNode = createDeviceNode(
                        agentLink,
                        curEndpoint.getCmdClasses(),
                        subDeviceName
                    );
                    deviceNode.addChild(subDeviceNode);
                }

                // Parameters

                List<ZWParameterItem> parameters = node.getParameters();
                if (parameters.size() > 0) {
                    AssetTreeNode parameterListNode = new AssetTreeNode(new Asset("Parameters", AssetType.THING));
                    deviceNode.addChild(parameterListNode);

                    List<AssetTreeNode> parameterNodes = parameters
                        .stream()
                        .filter(parameter -> parameter.getChannels().size() > 0)
                        .map(parameter -> {
                            Integer number = parameter.getNumber();
                            String parameterLabel = number.toString() + " : " + parameter.getDisplayName();
                            String description = parameter.getDescription();
                            Asset parameterAsset = new Asset(parameterLabel, AssetType.THING);
                            AssetTreeNode parameterNode = new AssetTreeNode(parameterAsset);

                            List<AssetAttribute> attributes = parameter.getChannels()
                                .stream()
                                .map(channel -> {
                                    AssetAttribute attribute = new AssetAttribute(channel.getName(), TypeMapper.toAttributeType(channel.getChannelType()))
                                        .setMeta(
                                            agentLink,
                                            new MetaItem(MetaItemType.LABEL, Values.create(channel.getDisplayName()))
                                        )
                                        .addMeta(getMetaItems(channel));
                                    addValidRangeMeta(attribute, parameter);
                                    return attribute;
                                })
                                .collect(toList());
                            if (description != null && description.length() > 0) {
                                AssetAttribute descriptionAttribute = new AssetAttribute(
                                    "description", AttributeValueType.STRING, Values.create("-")
                                ).setMeta(
                                    new MetaItem(MetaItemType.LABEL, Values.create("Description")),
                                    new MetaItem(MetaItemType.READ_ONLY, Values.create(true)),
                                    new MetaItem(MetaItemType.DESCRIPTION, Values.create(description))
                                );
                                attributes.add(descriptionAttribute);
                            }
                            parameterAsset.addAttributes(attributes.toArray(new AssetAttribute[attributes.size()]));
                            return parameterNode;
                        })
                        .collect(toList());

                    parameterNodes.forEach(parameterNode -> parameterListNode.addChild(parameterNode));
                }

                return deviceNode;
            })
            .collect(toList());

        // Z-Wave Controller

        Asset networkManagementAsset = new Asset("Z-Wave Controller", AssetType.THING);
        List<AssetAttribute> attributes = controller.getSystemCommandManager().getChannels()
            .stream()
            .filter(channel ->
                consumerLinkMap.values().stream().noneMatch(link -> link.getChannel() == channel))
            .map(channel -> {
                AssetAttribute attribute = new AssetAttribute(channel.getName(), TypeMapper.toAttributeType(channel.getChannelType()))
                    .setMeta(
                        agentLink,
                        new MetaItem(MetaItemType.LABEL, Values.create(channel.getDisplayName()))
                    )
                    .addMeta(getMetaItems(channel));
                return attribute;
            })
            .collect(toList());
        if (attributes.size() > 0) {
            networkManagementAsset.addAttributes(attributes.toArray(new AssetAttribute[attributes.size()]));
            assetNodes.add(new AssetTreeNode(networkManagementAsset));
        }

        return assetNodes.toArray(new AssetTreeNode[assetNodes.size()]);
    }

    protected Controller createController(IoClient<SerialDataPacket> ioClient) {
        return controllerFactory.createController(ioClient);
    }

    protected synchronized void onConnectionStatusChanged(ConnectionStatus status) {
        connectionStatusConsumers.forEach(consumer ->consumer.accept(status));
    }

    private MetaItem[] getMetaItems(Channel channel) {
        int nodeId = channel.getCommandClass() != null ? channel.getCommandClass().getContext().getNodeID() : 0;
        int endpoint = channel.getCommandClass() != null ? channel.getCommandClass().getContext().getDestEndPoint() : 0;
        List<MetaItem> unitMetaItems = TypeMapper.toMetaItems(channel.getChannelType());
        List<MetaItem> metaItems =  new ArrayList<>(3 + (channel.isReadOnly() ? 1 : 0) + unitMetaItems.size());
        metaItems.add(new MetaItem(META_ZWAVE_DEVICE_NODE_ID, Values.create(nodeId)));
        metaItems.add(new MetaItem(META_ZWAVE_DEVICE_ENDPOINT, Values.create(endpoint)));
        metaItems.add(new MetaItem(META_ZWAVE_DEVICE_VALUE_LINK, Values.create(channel.getLinkName())));
        if (channel.isReadOnly()) {
            metaItems.add(new MetaItem(MetaItemType.READ_ONLY, Values.create(true)));
        }
        metaItems.addAll(unitMetaItems);
        return metaItems.toArray(new MetaItem[metaItems.size()]);
    }

    private void removeStatusChangedHandler(IoClient<SerialDataPacket> ioClient) {
        if (ioClient != null) {
            ioClient.removeConnectionStatusConsumer(this::onConnectionStatusChanged);
        }
    }
    
    private Channel findChannel(int nodeId, int endpointNumber, String channelName) {
        Channel channel = null;
        if (controller != null) {
            channel = controller.findChannel(nodeId, endpointNumber, channelName);
        }
        return channel;
    }


    private org.openremote.protocol.zwave.model.commandclasses.channel.value.ArrayValue toZWArray(org.openremote.model.value.ArrayValue arrValue) {
        if (arrValue == null) {
            return null;
        }
        ArrayValue zwArray = new ArrayValue();
        org.openremote.protocol.zwave.model.commandclasses.channel.value.Value[] arr = new org.openremote.protocol.zwave.model.commandclasses.channel.value.Value[arrValue.length()];
        zwArray.addAll(
            arrValue
                .stream()
                .map(arrItem -> toZWArrayItem(arrItem))
                .collect(toList())
                .toArray(arr)
        );
        for (int i = 0; i < zwArray.length(); i++) {
            if (zwArray.get(i) == null) {
                return null;
            }
        }
        return zwArray;
    }

    private org.openremote.protocol.zwave.model.commandclasses.channel.value.Value toZWArrayItem(org.openremote.model.value.Value value) {
        if (value == null) {
            return null;
        }
        org.openremote.protocol.zwave.model.commandclasses.channel.value.Value zwValue = null;
        switch(value.getType()) {
            case NUMBER:
                zwValue = Values.getNumber(value).map(val -> new NumberValue(val)).orElse(null);
                break;
            case BOOLEAN:
                zwValue = Values.getBoolean(value).map(val -> new BooleanValue(val)).orElse(null);
                break;
            case STRING:
                zwValue = Values.getString(value).map(val -> new StringValue(val)).orElse(null);
                break;
        }
        return zwValue;
    }

    private AssetTreeNode createDeviceNode(MetaItem agentLink, List<ZWCommandClass> cmdClasses, String name) {

        Asset device = new Asset(name, AssetType.THING);

        List<AssetAttribute> attributes = cmdClasses
            .stream()
            .filter(commandClass -> commandClass.getID().toRaw() != ZWCommandClassID.COMMAND_CLASS_CONFIGURATION.toRaw() &&
                                    commandClass.getID().toRaw() != ZWCommandClassID.COMMAND_CLASS_ZWAVEPLUS_INFO.toRaw() &&
                                    commandClass.getID().toRaw() != ZWCommandClassID.COMMAND_CLASS_BASIC.toRaw())
            .map(ZWCommandClass::getChannels)
            .flatMap(List<Channel>::stream)
            .map(channel -> {
                int endpoint = channel.getCommandClass().getContext().getDestEndPoint();
                String attributeName = channel.getName() + (endpoint == 0 ? "" : "_" + endpoint);
                String displayName = channel.getDisplayName() + (endpoint == 0 ? "" : " - " + endpoint);
                AssetAttribute attribute = new AssetAttribute(attributeName, TypeMapper.toAttributeType(channel.getChannelType()))
                    .setMeta(
                        agentLink,
                        new MetaItem(MetaItemType.LABEL, Values.create(displayName))
                    )
                    .addMeta(getMetaItems(channel));
                return attribute;
            })
            .collect(toList());

        device.addAttributes(attributes.toArray(new AssetAttribute[attributes.size()]));

        return new AssetTreeNode(device);
    }
    
    private List<AssetAttribute> createNodeInfoAttributes(ZWaveNode node, MetaItem agentLink) {
        List<AssetAttribute> attributes = new ArrayList<>();

        AssetAttribute nodeIdAttrib = new AssetAttribute(
            "nodeId", AttributeValueType.NUMBER, Values.create(node.getNodeID()))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Node ID")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(nodeIdAttrib);

        AssetAttribute manufacturerIdAttrib = new AssetAttribute(
            "manufacturerId", AttributeValueType.STRING, Values.create(String.format("0x%04X",node.getManufacturerId())))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Manufacturer ID")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(manufacturerIdAttrib);

        AssetAttribute manufacturerAttrib = new AssetAttribute(
            "manufacturerName", AttributeValueType.STRING, Values.create(ZWManufacturerID.fromRaw(node.getManufacturerId()).getName()))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Manufacturer")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(manufacturerAttrib);

        AssetAttribute flirsAttrib = new AssetAttribute(
            "isFlirs", AttributeValueType.BOOLEAN, Values.create(node.getNodeInfo().isFLIRS()))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("FLIRS")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(flirsAttrib);

        AssetAttribute routingAttrib = new AssetAttribute(
            "isRouting", AttributeValueType.BOOLEAN, Values.create(node.getNodeInfo().isRouting()))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Routing")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(routingAttrib);

        AssetAttribute listeningAttrib = new AssetAttribute(
            "isListening", AttributeValueType.BOOLEAN, Values.create(node.getNodeInfo().isListening()))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Listening")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(listeningAttrib);

        AssetAttribute productTypeIdAttrib = new AssetAttribute(
            "productTypeId", AttributeValueType.STRING, Values.create(String.format("0x%04X", node.getProductTypeID())))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Product Type ID")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(productTypeIdAttrib);

        AssetAttribute productIdAttrib = new AssetAttribute(
            "productId", AttributeValueType.STRING, Values.create(String.format("0x%04X", node.getProductID())))
            .setMeta(
                new MetaItem(MetaItemType.LABEL, Values.create("Product ID")),
                new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
            );
        attributes.add(productIdAttrib);

        String modelName = node.getModelName();
        if (modelName != null) {
            AssetAttribute modelNameAttrib = new AssetAttribute(
                "modelName", AttributeValueType.STRING, Values.create(modelName))
                .setMeta(
                    new MetaItem(MetaItemType.LABEL, Values.create("Model")),
                    new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
                );
            attributes.add(modelNameAttrib);
        }

        // Z-Wave Plus Info

        List<AssetAttribute> zwPlusAttributes = node.getSupportedCommandClasses()
            .stream()
            .filter(commandClass -> commandClass.getID().toRaw() == ZWCommandClassID.COMMAND_CLASS_ZWAVEPLUS_INFO.toRaw())
            .map(ZWCommandClass::getChannels)
            .flatMap(List<Channel>::stream)
            .map(channel -> {
                String attributeName = channel.getName();
                String displayName = channel.getDisplayName();
                AssetAttribute attribute = new AssetAttribute(attributeName, TypeMapper.toAttributeType(channel.getChannelType()))
                    .setMeta(
                        agentLink,
                        new MetaItem(MetaItemType.LABEL, Values.create(displayName))
                    )
                    .addMeta(getMetaItems(channel));
                return attribute;
            })
            .collect(toList());

        attributes.addAll(zwPlusAttributes);

        return attributes;
    }

    private void addValidRangeMeta(AssetAttribute attribute, ZWParameterItem parameter) {
        Long min = null;
        Long max = null;
        if (parameter instanceof ZWParameterByte) {
            min = ((ZWParameterByte)parameter).getMinValue();
            max = ((ZWParameterByte)parameter).getMaxValue();
        } else if (parameter instanceof ZWParameterShort) {
            min = ((ZWParameterShort)parameter).getMinValue();
            max = ((ZWParameterShort)parameter).getMaxValue();
        } else if (parameter instanceof ZWParameterInt) {
            min = ((ZWParameterInt)parameter).getMinValue();
            max = ((ZWParameterInt)parameter).getMaxValue();
        }

        if (min != null && max != null) {
            String validRangString = "[" + min + " - " + max + "]";
            attribute.addMeta(new MetaItem(MetaItemType.DESCRIPTION, Values.create(validRangString)));
        }
    }
}
