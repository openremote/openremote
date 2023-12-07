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
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a generic Serial client protocol for communicating with Serial ports; it uses the {@link SerialIOClient} to
 * handle the communication and all messages are processed as strings; if you require custom message type handling
 * then please sub class the {@link AbstractSerialProtocol}).
 */
public class SerialProtocol extends AbstractSerialProtocol<SerialProtocol, SerialAgent, DefaultAgentLink, String, SerialIOClient<String>> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SerialProtocol.class);
    public static final String PROTOCOL_DISPLAY_NAME = "Serial";

    protected final List<Pair<AttributeRef, Consumer<String>>> protocolMessageConsumers = new ArrayList<>();

    public SerialProtocol(SerialAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {

        Consumer<String> messageConsumer = ProtocolUtil.createGenericAttributeMessageConsumer(assetId, attribute, agentLink, timerService::getCurrentTimeMillis, this::updateLinkedAttribute);

        if (messageConsumer != null) {
            protocolMessageConsumers.add(new Pair<>(
                new AttributeRef(assetId, attribute.getName()),
                messageConsumer
            ));
        }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        protocolMessageConsumers.removeIf(attRefConsumerPair -> attRefConsumerPair.key.equals(attributeRef));
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return getGenericStringEncodersAndDecoders(client, agent);
    }

    @Override
    protected void onMessageReceived(String message) {
        protocolMessageConsumers.forEach(c -> {
            if (c.value != null) {
                c.value.accept(message);
            }
        });
    }

    @Override
    protected String createWriteMessage(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {
        return ValueUtil.convert(processedValue, String.class);
    }
}
