/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan;

import jakarta.persistence.Entity;
import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public abstract class LoRaWANAgent<T extends LoRaWANAgent<T, U>, U extends AbstractLoRaWANProtocol<U, T>> extends Agent<T, U, MQTTAgentLink>{

    public static final AttributeDescriptor<String> MQTT_HOST = new AttributeDescriptor<>("MQTTHost", ValueType.HOSTNAME_OR_IP_ADDRESS).withOptional(false);
    public static final AttributeDescriptor<Integer> MQTT_PORT = new AttributeDescriptor<>("MQTTPort", ValueType.PORT).withOptional(false);
    public static final AttributeDescriptor<String> CLIENT_ID = MQTTAgent.CLIENT_ID;
    public static final AttributeDescriptor<Boolean> SECURE_MODE = MQTTAgent.SECURE_MODE;
    public static final AttributeDescriptor<Integer> PUBLISH_QOS = MQTTAgent.PUBLISH_QOS;
    public static final AttributeDescriptor<Integer> SUBSCRIBE_QOS = MQTTAgent.SUBSCRIBE_QOS;
    public static final AttributeDescriptor<String> CLIENT_CERTIFICATE_ALIAS = MQTTAgent.CLIENT_CERTIFICATE_ALIAS;
    public static final AttributeDescriptor<Boolean> RESUME_SESSION = MQTTAgent.RESUME_SESSION;
    public static final AttributeDescriptor<Boolean> WEBSOCKET_MODE = MQTTAgent.WEBSOCKET_MODE;
    public static final AttributeDescriptor<String> WEBSOCKET_PATH = MQTTAgent.WEBSOCKET_PATH;
    public static final AttributeDescriptor<String> WEBSOCKET_QUERY = MQTTAgent.WEBSOCKET_QUERY;
    public static final AttributeDescriptor<String> LAST_WILL_TOPIC = MQTTAgent.LAST_WILL_TOPIC;
    public static final AttributeDescriptor<String> LAST_WILL_PAYLOAD = MQTTAgent.LAST_WILL_PAYLOAD;
    public static final AttributeDescriptor<Boolean> LAST_WILL_RETAIN = MQTTAgent.LAST_WILL_RETAIN;

    public static final AttributeDescriptor<String> APPLICATION_ID = new AttributeDescriptor<>("applicationId", ValueType.TEXT);
    public static final AttributeDescriptor<String> API_KEY = new AttributeDescriptor<>("apiKey", ValueType.TEXT);


    // For Hydrators
    protected LoRaWANAgent() {}

    protected LoRaWANAgent(String name) {
        super(name);
    }

    public Optional<String> getMqttHost() {
        return getAttributes().getValue(MQTT_HOST);
    }

    public LoRaWANAgent<T, U> setMqttHost(String host) {
        getAttributes().getOrCreate(MQTT_HOST).setValue(host);
        return this;
    }

    public Optional<Integer> getMqttPort() {
        return getAttributes().getValue(MQTT_PORT);
    }

    public LoRaWANAgent<T, U> setMqttPort(Integer port) {
        getAttributes().getOrCreate(MQTT_PORT).setValue(port);
        return this;
    }

    public Optional<String> getClientId() {
        return getAttributes().getValue(CLIENT_ID);
    }

    public LoRaWANAgent<T, U> setClientId(String clientId) {
        getAttributes().getOrCreate(CLIENT_ID).setValue(clientId);
        return this;
    }

    public Optional<Boolean> isSecureMode() {
        return getAttributes().getValue(SECURE_MODE);
    }

    public LoRaWANAgent<T, U> setSecureMode(boolean secureMode) {
        getAttributes().getOrCreate(SECURE_MODE).setValue(secureMode);
        return this;
    }

    public Optional<String> getCertificateAlias() {
        return getAttributes().getValue(CLIENT_CERTIFICATE_ALIAS);
    }

    public LoRaWANAgent<T, U> setCertificateAlias(String certificateAlias) {
        getAttributes().getOrCreate(CLIENT_CERTIFICATE_ALIAS).setValue(certificateAlias);
        return this;
    }

    public Optional<Boolean> isWebsocketMode() {
        return getAttributes().getValue(WEBSOCKET_MODE);
    }

    public LoRaWANAgent<T, U> setWebsocketMode(boolean websocketMode) {
        getAttributes().getOrCreate(WEBSOCKET_MODE).setValue(websocketMode);
        return this;
    }

    public Optional<Boolean> isResumeSession() {
        return getAttributes().getValue(RESUME_SESSION);
    }

    public LoRaWANAgent<T, U> setResumeSession(boolean resumeSession) {
        getAttributes().getOrCreate(RESUME_SESSION).setValue(resumeSession);
        return this;
    }

    public Optional<String> getWebsocketPath() {
        return getAttributes().getValue(WEBSOCKET_PATH);
    }

    public LoRaWANAgent<T, U> setWebsocketPath(String websocketPath) {
        getAttributes().getOrCreate(WEBSOCKET_PATH).setValue(websocketPath);
        return this;
    }

    public Optional<String> getWebsocketQuery() {
        return getAttributes().getValue(WEBSOCKET_QUERY);
    }

    public LoRaWANAgent<T, U> setWebsocketQuery(String websocketQuery) {
        getAttributes().getOrCreate(WEBSOCKET_QUERY).setValue(websocketQuery);
        return this;
    }

    public Optional<String> getLastWillTopic() {
        return getAttributes().getValue(LAST_WILL_TOPIC);
    }

    public LoRaWANAgent<T, U> setLastWillTopic(String lastWillTopic) {
        getAttributes().getOrCreate(LAST_WILL_TOPIC).setValue(lastWillTopic);
        return this;
    }

    public Optional<String> getLastWillPayload() {
        return getAttributes().getValue(LAST_WILL_PAYLOAD);
    }

    public LoRaWANAgent<T, U> setLastWillPayload(String lastWillPayload) {
        getAttributes().getOrCreate(LAST_WILL_PAYLOAD).setValue(lastWillPayload);
        return this;
    }

    public Optional<Boolean> isLastWillRetain() {
        return getAttributes().getValue(LAST_WILL_RETAIN);
    }

    public LoRaWANAgent<T, U> setLastWillRetain(boolean lastWillRetain) {
        getAttributes().getOrCreate(LAST_WILL_RETAIN).setValue(lastWillRetain);
        return this;
    }

    public Optional<Integer> getPublishQoS() {
        return getAttributes().getValue(PUBLISH_QOS);
    }

    public LoRaWANAgent<T, U> setPublishQos(int publishQos) {
        getAttributes().getOrCreate(PUBLISH_QOS).setValue(publishQos);
        return this;
    }

    public Optional<Integer> getSubscribeQoS() {
        return getAttributes().getValue(SUBSCRIBE_QOS);
    }

    public LoRaWANAgent<T, U> setSubscribeQos(int subscribeQos) {
        getAttributes().getOrCreate(SUBSCRIBE_QOS).setValue(subscribeQos);
        return this;
    }

    public Optional<String> getApplicationId() {
        return getAttributes().getValue(APPLICATION_ID);
    }

    public LoRaWANAgent<T, U> setApplicationId(String applicationId) {
        getAttributes().getOrCreate(APPLICATION_ID).setValue(applicationId);
        return this;
    }

    public Optional<String> getApiKey() {
        return getAttributes().getValue(API_KEY);
    }

    public LoRaWANAgent<T, U> setApiKey(String apiKey) {
        getAttributes().getOrCreate(API_KEY).setValue(apiKey);
        return this;
    }
}
