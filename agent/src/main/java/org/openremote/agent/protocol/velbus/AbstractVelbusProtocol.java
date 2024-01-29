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

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.agent.protocol.velbus.device.VelbusDeviceType;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.*;
import org.openremote.model.protocol.ProtocolAssetImport;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;

public abstract class AbstractVelbusProtocol<S extends AbstractVelbusProtocol<S,T>, T extends VelbusAgent<T, S>> extends AbstractProtocol<T, VelbusAgentLink> implements
    ProtocolAssetImport {

    public static final int DEFAULT_TIME_INJECTION_INTERVAL_SECONDS = 3600 * 6;
    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractVelbusProtocol.class);
    protected VelbusNetwork network;
    protected final Map<AttributeRef, Consumer<Object>> attributePropertyValueConsumers = new HashMap<>();

    protected AbstractVelbusProtocol(T agent) {
        super(agent);
    }

    @Override
    public String getProtocolInstanceUri() {
        return network != null && network.client != null ? network.client.getClientUri() : "";
    }

    @Override
    protected void doStart(Container container) throws Exception {

        try {

            IOClient<VelbusPacket> messageProcessor = createIoClient(agent);
            int timeInjectionSeconds = agent.getTimeInjectionInterval().orElse(DEFAULT_TIME_INJECTION_INTERVAL_SECONDS);

            LOG.fine("Creating new VELBUS network instance for protocol instance: " + agent);
            network = new VelbusNetwork(messageProcessor, executorService, timeInjectionSeconds);
            network.connect();
            network.addConnectionStatusConsumer(this::setConnectionStatus);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create IO client for protocol instance: " + agent, e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (network != null) {
            network.removeConnectionStatusConsumer(this::setConnectionStatus);
            network.disconnect();
            network.close();
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, VelbusAgentLink agentLink) {

        // Get the device that this attribute is linked to
        int deviceAddress = getOrThrowAgentLinkProperty(agentLink.getDeviceAddress(), "device address");

        // Get the property that this attribute is linked to
        String property = getOrThrowAgentLinkProperty(agentLink.getDeviceValueLink(), "device value");

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        LOG.fine("Linking attribute to device '" + deviceAddress + "' and property '" + property + "': " + attributeRef);

        Consumer<Object> propertyValueConsumer = propertyValue ->
            updateLinkedAttribute(new AttributeState(attributeRef, propertyValue));

        attributePropertyValueConsumers.put(attributeRef, propertyValueConsumer);
        network.addPropertyValueConsumer(deviceAddress, property, propertyValueConsumer);
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, VelbusAgentLink agentLink) {

        // Get the device that this attribute is linked to
        int deviceAddress = getOrThrowAgentLinkProperty(agentLink.getDeviceAddress(), "device address");

        // Get the property that this attribute is linked to
        String property = getOrThrowAgentLinkProperty(agentLink.getDeviceValueLink(), "device value");

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Consumer<Object> propertyValueConsumer = attributePropertyValueConsumers.remove(attributeRef);
        network.removePropertyValueConsumer(deviceAddress, property, propertyValueConsumer);
    }

    @Override
    protected void doLinkedAttributeWrite(VelbusAgentLink agentLink, AttributeEvent event, Object processedValue) {

        // Get the device that this attribute is linked to
        int deviceAddress = getOrThrowAgentLinkProperty(agentLink.getDeviceAddress(), "device address");

        // Get the property that this attribute is linked to
        String property = getOrThrowAgentLinkProperty(agentLink.getDeviceValueLink(), "device value");

        network.writeProperty(deviceAddress, property, event.getValue().orElse(null));
    }

    /**
     * Should return an instance of {@link IOClient} for the supplied agent
     */
    protected abstract IOClient<VelbusPacket> createIoClient(T agent) throws RuntimeException;

    /* ProtocolAssetImport */

    @Override
    public Future<Void> startAssetImport(byte[] fileData, Consumer<AssetTreeNode[]> assetConsumer) {

        return executorService.submit(() -> {
            Document xmlDoc;
            try {
                String xmlStr = new String(fileData, StandardCharsets.UTF_8);
                LOG.info("Parsing VELBUS project file");

                xmlDoc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlStr)));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to convert VELBUS project file into XML", e);
                return;
            }

            xmlDoc.getDocumentElement().normalize();

            NodeList modules = xmlDoc.getElementsByTagName("Module");
            LOG.info("Found " + modules.getLength() + " module(s)");

            List<Asset<?>> devices = new ArrayList<>(modules.getLength());

            for (int i = 0; i < modules.getLength(); i++) {
                Element module = (Element) modules.item(i);

                // TODO: Process memory map and add
                Optional<VelbusDeviceType> deviceType = EnumUtil.enumFromString(VelbusDeviceType.class, module.getAttribute("type").replaceAll("-", ""));

                if (!deviceType.isPresent()) {
                    LOG.info("Module device type '" + module.getAttribute("type") + "' is not supported so ignoring");
                    continue;
                }

                String[] addresses = module.getAttribute("address").split(",");
                int baseAddress = Integer.parseInt(addresses[0], 16);
                String build = module.getAttribute("build");
                String serial = module.getAttribute("serial");
                String name = module.getElementsByTagName("Caption").item(0).getTextContent();
                name = isNullOrEmpty(name) ? deviceType.toString() : name;

                // TODO: Use device specific asset types
                Asset<?> device = new ThingAsset(name);

                device.addAttributes(
                    new Attribute<>("build", ValueType.TEXT, build)
                        .addMeta(
                            new MetaItem<>(MetaItemType.LABEL, "Build"),
                            new MetaItem<>(MetaItemType.READ_ONLY, true)
                        ),
                    new Attribute<>("serialNumber", ValueType.TEXT, serial)
                        .addMeta(
                            new MetaItem<>(MetaItemType.LABEL, "Serial No"),
                            new MetaItem<>(MetaItemType.READ_ONLY, true)
                        )
                );

                device.addAttributes(
                    deviceType.flatMap(type -> Optional.ofNullable(type.getFeatureProcessors())
                        .map(processors ->
                            Arrays.stream(processors).flatMap(processor ->
                                processor.getPropertyDescriptors(type).stream().map(descriptor -> {

                                    VelbusAgentLink agentLink = new VelbusAgentLink(agent.getId(), baseAddress, descriptor.getLinkName());

                                    Attribute<?> attribute = new Attribute<>(descriptor.getName(), descriptor.getAttributeValueDescriptor())
                                        .addMeta(
                                            new MetaItem<>(AGENT_LINK, agentLink),
                                            new MetaItem<>(MetaItemType.LABEL, descriptor.getDisplayName())
                                        );

                                    if (descriptor.isReadOnly()) {
                                        attribute.addMeta(new MetaItem<>(MetaItemType.READ_ONLY, true));
                                    }
                                    return attribute;
                                })
                            ).toArray(Attribute<?>[]::new)
                        ))
                        .orElse(new Attribute<?>[0])
                );

                devices.add(device);
            }

            assetConsumer.accept(devices.stream().map(AssetTreeNode::new).toArray(AssetTreeNode[]::new));
        }, null);
    }
}
