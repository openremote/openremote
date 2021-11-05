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
package org.openremote.agent.protocol.mqtt;

import org.openremote.agent.protocol.io.IOAgent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.util.ModelIgnore;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class MQTTAgent extends IOAgent<MQTTAgent, MQTTProtocol, MQTTAgentLink> {

    public static final AttributeDescriptor<String> MQTT_HOST = HOST.withOptional(false);
    public static final AttributeDescriptor<Integer> MQTT_PORT = PORT.withOptional(false);
    public static final AttributeDescriptor<String> CLIENT_ID = new AttributeDescriptor<>("clientId", ValueType.TEXT);
    public static final AttributeDescriptor<Boolean> SECURE_MODE = new AttributeDescriptor<>("secureMode", ValueType.BOOLEAN);
    public static final AttributeDescriptor<Boolean> RESUME_SESSION = new AttributeDescriptor<>("resumeSession", ValueType.BOOLEAN);
    public static final AttributeDescriptor<Boolean> WEBSOCKET_MODE = new AttributeDescriptor<>("websocketMode", ValueType.BOOLEAN);
    public static final AttributeDescriptor<String> WEBSOCKET_PATH = new AttributeDescriptor<>("websocketPath", ValueType.TEXT);
    public static final AttributeDescriptor<String> WEBSOCKET_QUERY = new AttributeDescriptor<>("websocketQuery", ValueType.TEXT);

    public static final AgentDescriptor<MQTTAgent, MQTTProtocol, MQTTAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        MQTTAgent.class, MQTTProtocol.class, MQTTAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected MQTTAgent() {
    }

    public MQTTAgent(String name) {
        super(name);
    }

    @Override
    public MQTTProtocol getProtocolInstance() {
        return new MQTTProtocol(this);
    }

    public Optional<String> getClientId() {
        return getAttributes().getValue(CLIENT_ID);
    }

    public MQTTAgent setClientId(String clientId) {
        getAttributes().getOrCreate(CLIENT_ID).setValue(clientId);
        return this;
    }

    public Optional<Boolean> isSecureMode() {
        return getAttributes().getValue(SECURE_MODE);
    }

    public MQTTAgent setSecureMode(boolean secureMode) {
        getAttributes().getOrCreate(SECURE_MODE).setValue(secureMode);
        return this;
    }

    public Optional<Boolean> isWebsocketMode() {
        return getAttributes().getValue(WEBSOCKET_MODE);
    }

    public MQTTAgent setWebsocketMode(boolean websocketMode) {
        getAttributes().getOrCreate(WEBSOCKET_MODE).setValue(websocketMode);
        return this;
    }

    public Optional<Boolean> isResumeSession() {
        return getAttributes().getValue(RESUME_SESSION);
    }

    public MQTTAgent setResumeSession(boolean resumeSession) {
        getAttributes().getOrCreate(RESUME_SESSION).setValue(resumeSession);
        return this;
    }

    public Optional<String> getWebsocketPath() {
        return getAttributes().getValue(WEBSOCKET_PATH);
    }

    public MQTTAgent setWebsocketPath(String websocketPath) {
        getAttributes().getOrCreate(WEBSOCKET_PATH).setValue(websocketPath);
        return this;
    }

    public Optional<String> getWebsocketQuery() {
        return getAttributes().getValue(WEBSOCKET_QUERY);
    }

    public MQTTAgent setWebsocketQuery(String websocketQuery) {
        getAttributes().getOrCreate(WEBSOCKET_QUERY).setValue(websocketQuery);
        return this;
    }
}
