/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class WebsocketAgent extends IOAgent<WebsocketAgent, WebsocketAgentProtocol, WebsocketAgentLink> {

    public static final ValueDescriptor<WebsocketSubscription> WEBSOCKET_SUBSCRIPTION_VALUE_DESCRIPTOR = new ValueDescriptor<>("websocketSubscription", WebsocketSubscription.class);

    /**
     * Websocket connect endpoint URI
     */
    public static final AttributeDescriptor<String> CONNECT_URL = new AttributeDescriptor<>("connectURL", ValueType.WS_URL);

    /**
     * Headers for websocket connect call
     */
    public static final AttributeDescriptor<ValueType.MultivaluedStringMap> CONNECT_HEADERS = new AttributeDescriptor<>("connectHeaders", ValueType.MULTIVALUED_TEXT_MAP);

    /**
     * Array of {@link WebsocketSubscription}s that should be executed once the websocket connection is established; the
     * subscriptions are executed in the order specified in the array.
     */
    public static final AttributeDescriptor<WebsocketSubscription[]> CONNECT_SUBSCRIPTIONS = new AttributeDescriptor<>("connectSubscriptions", WEBSOCKET_SUBSCRIPTION_VALUE_DESCRIPTOR.asArray());

    public static final AgentDescriptor<WebsocketAgent, WebsocketAgentProtocol, WebsocketAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        WebsocketAgent.class, WebsocketAgentProtocol.class, WebsocketAgentLink.class, null
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WebsocketAgent() {
    }

    public WebsocketAgent(String name) {
        super(name);
    }

    public Optional<String> getConnectUri() {
        return getAttributes().getValue(CONNECT_URL);
    }

    public WebsocketAgent setConnectURI(String value) {
        getAttributes().getOrCreate(CONNECT_URL).setValue(value);
        return this;
    }

    public Optional<ValueType.MultivaluedStringMap> getConnectHeaders() {
        return getAttributes().getValue(CONNECT_HEADERS);
    }

    public WebsocketAgent setConnectHeaders(ValueType.MultivaluedStringMap value) {
        getAttributes().getOrCreate(CONNECT_HEADERS).setValue(value);
        return this;
    }

    public Optional<WebsocketSubscription[]> getConnectSubscriptions() {
        return getAttributes().getValue(CONNECT_SUBSCRIPTIONS);
    }

    public WebsocketAgent setConnectSubscriptions(WebsocketSubscription[] value) {
        getAttributes().getOrCreate(CONNECT_SUBSCRIPTIONS).setValue(value);
        return this;
    }

    @Override
    public WebsocketAgentProtocol getProtocolInstance() {
        return new WebsocketAgentProtocol(this);
    }
}
