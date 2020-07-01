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
package org.openremote.agent.protocol.serial;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.tcp.TcpIoClient;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueFilter;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.openremote.container.util.Util.joinCollections;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a generic TCP client protocol for communicating with TCP servers; it uses the {@link TcpIoClient} to
 * handle the communication and all messages are processed as strings; if you require custom message type handling or
 * more specific {@link #getProtocolConfigurationMetaItemDescriptors()} or {@link #getLinkedAttributeMetaItemDescriptors()}
 * then please sub class the {@link AbstractSerialClientProtocol}).
 * <h1>Protocol Configurations</h1>
 * <p>
 * {@link Attribute}s that are configured as {@link ProtocolConfiguration}s for this protocol support the meta
 * items defined in {@link #PROTOCOL_META_ITEM_DESCRIPTORS}.
 * <h1>Linked Attributes</h1>
 * <p>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the meta items defined in {@link #ATTRIBUTE_META_ITEM_DESCRIPTORS}.
 * <h1>Protocol -> Attribute</h1>
 * <p>
 * When a new value comes from the protocol destined for a linked {@link Attribute} the actual value written to the
 * attribute can be filtered in the standard way using {@link ValueFilter}s via the
 * {@link Protocol#META_ATTRIBUTE_VALUE_FILTERS} {@link MetaItem}.
 * <h1>Attribute -> Protocol</h1>
 * <p>
 * When a linked {@link Attribute} is written to, the actual value written to the protocol can either be the exact value
 * written to the linked {@link Attribute} or the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} can be
 * used to inject the written value into a bigger payload using the {@link Protocol#DYNAMIC_VALUE_PLACEHOLDER} and then
 * this bigger payload will be written to the protocol.
 * <h1>Executable Attributes</h1>
 * When a linked {@link Attribute} that has an {@link MetaItemType#EXECUTABLE} {@link MetaItem} is executed the
 * {@link Value} stored in the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} is actually written to the
 * protocol (note dynamic value injection doesn't work in this scenario as there is no dynamic value to inject).
 * <p>
 * <h1>Protocol Specifics</h1>
 * <p>
 * This is a generic protocol that supports:
 * {@link Protocol#META_PROTOCOL_CONVERT_HEX} or {@link Protocol#META_PROTOCOL_CONVERT_BINARY} to facilitate working
 * with UDP servers that handle binary data.
 */
public class SerialClientProtocol extends AbstractSerialClientProtocol<String> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SerialClientProtocol.class);
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":serialClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Serial Client";
    public static final String PROTOCOL_VERSION = "1.0";
    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = joinCollections(AbstractSerialClientProtocol.PROTOCOL_META_ITEM_DESCRIPTORS, AbstractIoClientProtocol.PROTOCOL_GENERIC_META_ITEM_DESCRIPTORS);

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_ATTRIBUTE_MATCH_FILTERS,
        META_ATTRIBUTE_MATCH_PREDICATE);

    protected final Map<AttributeRef, List<Pair<AttributeRef, Consumer<String>>>> protocolMessageConsumers = new HashMap<>();

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
        return PROTOCOL_VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.remove(protocolConfiguration.getReferenceOrThrow());
        }
        super.doUnlinkProtocolConfiguration(agent, protocolConfiguration);
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        Consumer<String> messageConsumer = Protocol.createGenericAttributeMessageConsumer(attribute, assetService, this::updateLinkedAttribute);

        if (messageConsumer != null) {
            synchronized (protocolMessageConsumers) {
                protocolMessageConsumers.compute(protocolRef, (ref, consumers) -> {
                    if (consumers == null) {
                        consumers = new ArrayList<>();
                    }
                    consumers.add(new Pair<>(
                        attribute.getReferenceOrThrow(),
                        messageConsumer
                    ));
                    return consumers;
                });
            }
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.compute(protocolConfiguration.getReferenceOrThrow(), (ref, consumers) -> {
                if (consumers != null) {
                    consumers.removeIf((attrRefConsumer) -> attrRefConsumer.key.equals(attributeRef));
                }
                return consumers;
            });
        }
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider(SerialIoClient<String> client, AssetAttribute protocolConfiguration) {
        return getGenericStringEncodersAndDecoders(client, protocolConfiguration);
    }

    @Override
    protected void onMessageReceived(AttributeRef protocolRef, String message) {
        List<Pair<AttributeRef, Consumer<String>>> consumers;

        synchronized (protocolMessageConsumers) {
            consumers = protocolMessageConsumers.get(protocolRef);

            if (consumers != null) {
                consumers.forEach(c -> {
                    if (c.value != null) {
                        c.value.accept(message);
                    }
                });
            }
        }
    }

    @Override
    protected String createWriteMessage(AssetAttribute protocolConfiguration, AssetAttribute attribute, AttributeEvent event, Value processedValue) {
        if (attribute.isReadOnly()) {
            LOG.fine("Attempt to write to an attribute that doesn't support writes: " + event.getAttributeRef());
            return null;
        }

        if (attribute.isExecutable()) {
            AttributeExecuteStatus status = event.getValue()
                .flatMap(Values::getString)
                .flatMap(AttributeExecuteStatus::fromString)
                .orElse(null);

            if (status != null && status != AttributeExecuteStatus.REQUEST_START) {
                LOG.fine("Unsupported execution status: " + status);
                return null;
            }
        }

        return processedValue != null ? processedValue.toString() : null;
    }
}
