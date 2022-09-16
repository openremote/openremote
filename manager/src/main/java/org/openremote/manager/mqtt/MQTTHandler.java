/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This allows custom handlers to be discovered by the {@link MQTTBrokerService} during system startup using the
 * {@link java.util.ServiceLoader} mechanism. This allows topic(s) can be handled in a custom way. Any instances must
 * have a no-arg constructor.
 */
public abstract class MQTTHandler {

    public static final String TOKEN_MULTI_LEVEL_WILDCARD = "#";
    public static final String TOKEN_SINGLE_LEVEL_WILDCARD = "+";
    protected ClientEventService clientEventService;
    protected MQTTBrokerService mqttBrokerService;
    protected MessageBrokerService messageBrokerService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected boolean isKeycloak;

    /**
     * Gets the priority of this handler which is used to determine the call order; handlers with a lower priority are
     * initialised and started first.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Provides a name to identify this custom handler for logging purposes etc.
     */
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Called when the system starts to allow for initialisation.
     */
    public void start(Container container) throws Exception {
        mqttBrokerService = container.getService(MQTTBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);

        if (!identityService.isKeycloakEnabled()) {
            getLogger().warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }
    }

    /**
     * Called when the system stops to allow for any cleanup.
     */
    public void stop() throws Exception {

    }

    /**
     * Will be called when any client connects; if returns false then subsequent handlers will not be called
     */
    public boolean onConnect(RemotingConnection connection) {
        return true;
    }

    /**
     * Will be called when any client disconnects
     */
    public void onDisconnect(RemotingConnection connection) {

    }

    /**
     * Will be called when any client loses connection
     */
    public void onConnectionLost(RemotingConnection connection) {

    }

    /**
     * Checks that topic is valid and has at least 3 tokens (generally first two tokens should be {realm}/{clientId} but
     * it is up to the {@link #canSubscribe} and/or {@link #canPublish} methods to validate this).
     */
    public boolean handlesTopic(Topic topic) {
        if (!topicTokenCountGreaterThan(topic, 2)) {
            getLogger().finer("Topic must contain more than 2 tokens");
            return false;
        }
        if (!topicMatches(topic)) {
            getLogger().finer("Topic failed to match this handler");
            return false;
        }
        return true;
    }

    /**
     * Checks that authenticated session and topic realm matches the authenticated user and also that topic client ID
     * matches the connection client ID.
     */
    public boolean checkCanSubscribe(KeycloakSecurityContext securityContext, Topic topic) {
        if (securityContext == null) {
            getLogger().fine("Anonymous connection subscriptions not supported by this handler, topic=" + topic);
            return false;
        }
        if (!topicRealmMatches(securityContext, topic) || !topicClientIdMatches(securityContext, topic)) {
            getLogger().fine("Topic realm and client ID tokens must match the connection, topic=" + topic + ", user=" + securityContext);
            return false;
        }
        if (!canSubscribe(connection, topic)) {
            getLogger().fine("Cannot subscribe to this topic, topic=" + topic + ", connection" + connection);
            return false;
        }
        return true;
    }

    /**
     * Checks that authenticated sessions and topic realm matches the authenticated user and also that topic client ID
     * matches the connection client ID.
     */
    public boolean checkCanPublish(KeycloakSecurityContext securityContext, Topic topic) {
        if (securityContext == null) {
            getLogger().fine("Anonymous connection publishes not supported by this handler, topic=" + topic + ", connection" + connection);
            return false;
        }
        if (!topicRealmMatches(connection, topic) || !topicClientIdMatches(connection, topic)) {
            getLogger().fine("Topic realm and client ID tokens must match the connection, topic=" + topic + ", connection" + connection);
            return false;
        }
        if (!canPublish(connection, topic)) {
            getLogger().fine("Cannot publish to this topic, topic=" + topic + ", connection" + connection);
            return false;
        }
        return true;
    }

    /**
     * Indicates if this handler will handle the specified topic; independent of whether it is a publish or subscribe.
     * Should generally check the third token onwards unless {@link #handlesTopic} has been overridden.
     */
    protected abstract boolean topicMatches(Topic topic);

    protected abstract Logger getLogger();

    /**
     * Called to authorise a subscription if {@link #handlesTopic} returned true; should return true if the subscription
     * is allowed otherwise return false.
     */
    public abstract boolean canSubscribe(KeycloakSecurityContext securityContext, Topic topic);

    /**
     * Called to authorise a publish if {@link #handlesTopic} returned true; should return true if the publish is
     * allowed otherwise return false.
     */
    public abstract boolean canPublish(KeycloakSecurityContext securityContext, Topic topic);

    /**
     * Called to handle subscribe if {@link #canSubscribe} returned true.
     */
    public abstract void doSubscribe(KeycloakSecurityContext securityContext, Topic topic, InterceptSubscribeMessage msg);

    /**
     * Called to handle unsubscribe if {@link #handlesTopic} returned true.
     */
    public abstract void doUnsubscribe(KeycloakSecurityContext securityContext, Topic topic, InterceptUnsubscribeMessage msg);

    /**
     * Called to handle publish if {@link #canPublish} returned true.
     */
    public abstract void doPublish(KeycloakSecurityContext securityContext, Topic topic, InterceptPublishMessage msg);

    public static boolean topicRealmMatches(KeycloakSecurityContext securityContext, Topic topic) {
        return securityContext.getRealm().equals(topicTokenIndexToString(topic, 0));
    }

    public static boolean topicClientIdMatches(KeycloakSecurityContext securityContext, Topic topic) {
        return connection.clientId.equals(topicTokenIndexToString(topic, 1));
    }

    public static boolean topicTokenCountGreaterThan(Topic topic, int size) {
        return topic.getTokens() != null && topic.getTokens().size() > size;
    }

    public static String topicTokenIndexToString(Topic topic, int tokenNumber) {
        return topicTokenCountGreaterThan(topic, tokenNumber) ? topic.getTokens().get(tokenNumber) : null;
    }

    public static Map<String, Object> prepareHeaders(MqttConnection connection) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ConnectionConstants.SESSION_KEY, connection.getClientId());
        headers.put(ClientEventService.HEADER_CONNECTION_TYPE, ClientEventService.HEADER_CONNECTION_TYPE_MQTT);
        headers.put(Constants.AUTH_CONTEXT, connection.getAuthContext());
        headers.put(Constants.REALM_PARAM_NAME, connection.getRealm());
        return headers;
    }
}
