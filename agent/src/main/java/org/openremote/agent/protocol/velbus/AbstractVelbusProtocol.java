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

import org.openremote.agent.protocol.*;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.velbus.device.DevicePropertyValue;
import org.openremote.agent.protocol.velbus.device.FeatureProcessor;
import org.openremote.agent.protocol.velbus.device.VelbusDeviceType;
import org.openremote.container.util.CodecUtil;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.file.FileInfo;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.velbus.VelbusConfiguration.*;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public abstract class AbstractVelbusProtocol extends AbstractProtocol implements
    ProtocolLinkedAttributeDiscovery,
    ProtocolLinkedAttributeImport {

    public static final String PROTOCOL_BASE_NAME = PROTOCOL_NAMESPACE + ":velbus";
    public static final String META_VELBUS_DEVICE_ADDRESS = PROTOCOL_BASE_NAME + ":deviceAddress";
    public static final String META_VELBUS_DEVICE_VALUE_LINK = PROTOCOL_BASE_NAME + ":deviceValueLink";
    public static final String META_VELBUS_TIME_INJECTION_INTERVAL_SECONDS = PROTOCOL_BASE_NAME + ":timeInjectionInterval";
    public static final int DEFAULT_TIME_INJECTION_INTERVAL_SECONDS = 3600 * 6; //
    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractVelbusProtocol.class);
    protected final Map<String, VelbusNetwork> networkMap = new HashMap<>();
    protected final Map<AttributeRef, Pair<VelbusNetwork, Consumer<ConnectionStatus>>> networkConfigurationMap = new HashMap<>();
    protected final Map<AttributeRef, Consumer<DevicePropertyValue>> attributePropertyValueConsumers = new HashMap<>();
    protected static final String VERSION = "1.0";
    protected static final List<MetaItemDescriptorImpl> META_ITEM_DESCRIPTORS = Collections.singletonList(
        new MetaItemDescriptorImpl(
            META_VELBUS_TIME_INJECTION_INTERVAL_SECONDS,
            ValueType.NUMBER,
            false,
            REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO,
            MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            1,
            Values.create(DEFAULT_TIME_INJECTION_INTERVAL_SECONDS),
            false, null, null, null)
    );


    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl(
            META_VELBUS_DEVICE_ADDRESS,
            ValueType.NUMBER,
            true,
            "^([1-9]\\d{0,1}|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$", //1-255
            "1-255",
            1,
            null,
            false,
                null, null, null),
        new MetaItemDescriptorImpl(
            META_VELBUS_DEVICE_VALUE_LINK,
            ValueType.STRING,
            true,
            null,
            null,
            1,
            null,
            false,
                null, null, null)
    );

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return new ArrayList<>(META_ITEM_DESCRIPTORS);
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return new ArrayList<>(ATTRIBUTE_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        // Look for existing network
        String networkIdentifier = getUniqueNetworkIdentifier(protocolConfiguration);
        VelbusNetwork velbusNetwork = null;
        Pair<VelbusNetwork, Consumer<ConnectionStatus>> velbusNetworkConsumerPair = null;

        if (networkMap.containsKey(networkIdentifier)) {
            velbusNetwork = networkMap.get(networkIdentifier);
        } else {
            try {
                IoClient<VelbusPacket> messageProcessor = createIoClient(protocolConfiguration);
                int timeInjectionSeconds = getTimeInjectionIntervalSeconds(protocolConfiguration);
                LOG.fine("Creating new VELBUS network instance for protocolConfiguration: " + protocolRef);
                velbusNetwork = new VelbusNetwork(messageProcessor, executorService, timeInjectionSeconds);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to create message processor for protocol Configuration: " + protocolConfiguration, e);
                updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR);
            } finally {
                networkMap.put(networkIdentifier, velbusNetwork);
            }
        }

        if (velbusNetwork != null) {
            if (velbusNetwork.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
                velbusNetwork.connect();
            }
            Consumer<ConnectionStatus> statusConsumer = status -> updateStatus(protocolRef, status);
            velbusNetwork.addConnectionStatusConsumer(statusConsumer);
            velbusNetworkConsumerPair = new Pair<>(velbusNetwork, statusConsumer);
        }
        networkConfigurationMap.put(protocolRef, velbusNetworkConsumerPair);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        Pair<VelbusNetwork, Consumer<ConnectionStatus>> velbusNetworkAndConsumer = networkConfigurationMap.remove(protocolRef);
        updateStatus(protocolRef, ConnectionStatus.DISCONNECTED);

        if (velbusNetworkAndConsumer != null) {
            velbusNetworkAndConsumer.key.removeConnectionStatusConsumer(velbusNetworkAndConsumer.value);

            // Check if network is no longer used
            if (networkConfigurationMap.isEmpty() || networkConfigurationMap.values()
                .stream()
                .noneMatch(networkStatusConsumer -> velbusNetworkAndConsumer.key.equals(networkStatusConsumer.key))) {
                    networkMap.remove(getUniqueNetworkIdentifier(protocolConfiguration));
                    velbusNetworkAndConsumer.key.disconnect();
                    velbusNetworkAndConsumer.key.close();
                }
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        Pair<VelbusNetwork, Consumer<ConnectionStatus>> velbusNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (velbusNetworkConsumerPair == null) {
            LOG.info("Protocol Configuration doesn't have a valid VelbusNetwork so cannot link");
            return;
        }

        VelbusNetwork velbusNetwork = velbusNetworkConsumerPair.key;

        // Get the device that this attribute is linked to
        int deviceAddress = getVelbusDeviceAddress(attribute);

        // Get the property that this attribute is linked to
        String property = getVelbusDevicePropertyLink(attribute);

        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        ValueType valueType = attribute.getType().orElse(AttributeValueType.STRING).getValueType();
        LOG.fine("Linking attribute to device '" +deviceAddress + "' and property '" + property + "': " + attributeRef);

        Consumer<DevicePropertyValue> propertyValueConsumer = propertyValue ->
            updateLinkedAttribute(new AttributeState(attributeRef, propertyValue != null ? propertyValue.toValue(valueType) : null));

        attributePropertyValueConsumers.put(attributeRef, propertyValueConsumer);
        velbusNetwork.addPropertyValueConsumer(deviceAddress, property, propertyValueConsumer);
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        Pair<VelbusNetwork, Consumer<ConnectionStatus>> velbusNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (velbusNetworkConsumerPair == null) {
            return;
        }

        VelbusNetwork velbusNetwork = velbusNetworkConsumerPair.key;

        // Get the device that this attribute is linked to
        int deviceAddress = getVelbusDeviceAddress(attribute);

        // Get the property that this attribute is linked to
        String property = getVelbusDevicePropertyLink(attribute);

        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        Consumer<DevicePropertyValue> propertyValueConsumer = attributePropertyValueConsumers.remove(attributeRef);
        velbusNetwork.removePropertyValueConsumer(deviceAddress, property, propertyValueConsumer);
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        Pair<VelbusNetwork, Consumer<ConnectionStatus>> velbusNetworkConsumerPair = networkConfigurationMap.get(protocolConfiguration.getReferenceOrThrow());

        if (velbusNetworkConsumerPair == null) {
            return;
        }

        VelbusNetwork velbusNetwork = velbusNetworkConsumerPair.key;
        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        if (attribute == null) {
            return;
        }

        // Get the device that this attribute is linked to
        int deviceAddress = getVelbusDeviceAddress(attribute);

        // Get the property that this attribute is linked to
        String property = getVelbusDevicePropertyLink(attribute);

        velbusNetwork.writeProperty(deviceAddress, property, event.getValue().orElse(null));
    }

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration) {
        // TODO: Implement asset attribute discovery using the bus
        return new AssetTreeNode[0];
    }

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {
        Document xmlDoc;
        try {
            String xmlStr = fileInfo.isBinary() ? new String(CodecUtil.decodeBase64(fileInfo.getContents()), "UTF8") : fileInfo.getContents();
            LOG.info("Parsing VELBUS project file: " + fileInfo.getName());

            xmlDoc = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlStr)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert file into XML", e);
        }

        xmlDoc.getDocumentElement().normalize();

        NodeList modules = xmlDoc.getElementsByTagName("Module");
        LOG.info("Found " + modules.getLength() + " module(s)");

        List<Asset> devices = new ArrayList<>(modules.getLength());
        MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());

        for (int i=0; i<modules.getLength(); i++) {
            Element module = (Element)modules.item(i);

            // TODO: Process memory map and add
            Optional<VelbusDeviceType> deviceType = EnumUtil.enumFromString(VelbusDeviceType.class, module.getAttribute("type").replaceAll("-", ""));

            if (!deviceType.isPresent()) {
                LOG.info("Module device type '" + module.getAttribute("type") + "' is not supported so ignoring");
                continue;
            }

            String[] addresses = module.getAttribute("address").split(",");
            Integer baseAddress = Integer.parseInt(addresses[0], 16);
            String build = module.getAttribute("build");
            String serial = module.getAttribute("serial");
            String name = module.getElementsByTagName("Caption").item(0).getTextContent();
            name = isNullOrEmpty(name) ? deviceType.toString() : name;
            Asset device = new Asset(name, AssetType.THING);
            device.setAttributes(
                new AssetAttribute("build", AttributeValueType.STRING, Values.create(build))
                    .setMeta(
                        new MetaItem(MetaItemType.LABEL, Values.create("Build")),
                        new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
                    ),
                new AssetAttribute("serialNumber", AttributeValueType.STRING, Values.create(serial))
                    .setMeta(
                        new MetaItem(MetaItemType.LABEL, Values.create("Serial No")),
                        new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
                    )
            );

            getLinkedAttributeDescriptors(deviceType.get(), baseAddress)
                .forEach(descriptor -> {
                    AssetAttribute attribute = new AssetAttribute(descriptor.getName(), descriptor.getAttributeValueDescriptor())
                        .setMeta(
                            agentLink,
                            new MetaItem(MetaItemType.LABEL, Values.create(descriptor.getDisplayName()))
                        )
                        .addMeta(descriptor.getMetaItems());

                    if (descriptor.isReadOnly()) {
                        attribute.addMeta(new MetaItem(MetaItemType.READ_ONLY, Values.create(true)));
                    } else if(descriptor.isExecutable()) {
                        attribute.addMeta(new MetaItem(MetaItemType.EXECUTABLE, Values.create(true)));
                    }

                    device.addAttributes(attribute);
                });

            devices.add(device);
        }

        return devices.stream().map(AssetTreeNode::new).toArray(AssetTreeNode[]::new);
    }

    /**
     * Should return an instance of {@link IoClient} for the supplied protocolConfiguration
     */
    protected abstract IoClient<VelbusPacket> createIoClient(AssetAttribute protocolConfiguration) throws RuntimeException;


    /**
     * Should return a String key that uniquely identifies the VelbusNetwork that corresponds with the supplied
     * {@link org.openremote.model.asset.agent.ProtocolConfiguration}
     */
    protected abstract String getUniqueNetworkIdentifier(AssetAttribute protocolConfiguration);

    public static List<LinkedAttributeDescriptor> getLinkedAttributeDescriptors(VelbusDeviceType deviceType, int deviceAddress) {
        List<LinkedAttributeDescriptor> descriptors = new ArrayList<>();

        FeatureProcessor[] processors = deviceType.getFeatureProcessors();
        if (processors != null) {
            Arrays.stream(processors)
                .forEach(processor ->
                    processor.getPropertyDescriptors(deviceType)
                        .forEach(propertyDescriptor ->
                            descriptors.add(
                                new LinkedAttributeDescriptor(
                                    propertyDescriptor.getName(),
                                    propertyDescriptor.getDisplayName(),
                                    propertyDescriptor.getAttributeValueDescriptor(),
                                    propertyDescriptor.isReadOnly(),
                                    false,
                                    createLinkedAttributeMetaItems(
                                        deviceAddress,
                                        propertyDescriptor.getLinkName()
                                    )
                                )
                            )
                        )
                );
        }

        return descriptors;
    }

    public static MetaItem[] createLinkedAttributeMetaItems(int address, String deviceLink) {
        return new MetaItem[] {
            new MetaItem(META_VELBUS_DEVICE_ADDRESS, Values.create(address)),
            new MetaItem(META_VELBUS_DEVICE_VALUE_LINK, Values.create(deviceLink))
        };
    }
}
