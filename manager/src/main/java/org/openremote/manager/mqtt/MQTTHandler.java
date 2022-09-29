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

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
import org.apache.activemq.artemis.reader.MessageUtil;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.util.Pair;

import javax.security.auth.Subject;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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

        Set<String> publishListenerTopics = getPublishListenerTopics();
        if (publishListenerTopics != null) {
            publishListenerTopics.forEach(this::addPublishConsumer);
        }
    }

    protected void addPublishConsumer(String topic) {
        try {
            getLogger().info("Adding publish consumer for topic '" + topic + "': handler=" + getName());
            String coreTopic = MQTTUtil.convertMqttTopicFilterToCoreAddress(topic, mqttBrokerService.wildcardConfiguration);
            mqttBrokerService.internalSession.createQueue(new QueueConfiguration(coreTopic).setRoutingType(RoutingType.ANYCAST).setAutoCreateAddress(true).setAutoCreated(true));
            ClientConsumer consumer = mqttBrokerService.internalSession.createConsumer(coreTopic);
            consumer.setMessageHandler(message -> {
                Topic publishTopic = Topic.parse(MQTTUtil.convertCoreAddressToMqttTopicFilter(message.getAddress(), mqttBrokerService.wildcardConfiguration));
                String clientID = message.getStringProperty(MessageUtil.CONNECTION_ID_PROPERTY_NAME);
                RemotingConnection connection = mqttBrokerService.getConnectionFromClientID(clientID);
                onPublish(connection, publishTopic, message.getReadOnlyBodyBuffer().byteBuf());
            });
        } catch (ActiveMQException e) {
            getLogger().log(Level.WARNING, "Failed to create handler consumer for topic '" + topic + "': handler=" + getName(), e);
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
    public boolean checkCanSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (securityContext == null) {
            getLogger().fine("Anonymous connection subscriptions not supported by this handler, topic=" + topic);
            return false;
        }
        if (!topicRealmAllowed(securityContext, topic) || !topicClientIdMatches(connection, topic)) {
            getLogger().fine("Topic realm and client ID tokens must match the connection, topic=" + topic + ", user=" + securityContext);
            return false;
        }
        return canSubscribe(connection, securityContext, topic);
    }

    /**
     * Checks that authenticated sessions and topic realm matches the authenticated user and also that topic client ID
     * matches the connection client ID.
     */
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (securityContext == null) {
            getLogger().fine("Anonymous connection publishes not supported by this handler, topic=" + topic);
            return false;
        }
        if (!topicRealmAllowed(securityContext, topic) || !topicClientIdMatches(connection, topic)) {
            getLogger().fine("Topic realm and client ID tokens must match the connection, topic=" + topic + ", user=" + securityContext);
            return false;
        }
        return canPublish(connection, securityContext, topic);
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
    public abstract boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic);

    /**
     * Called to authorise a publish if {@link #handlesTopic} returned true; should return true if the publish is
     * allowed otherwise return false.
     */
    public abstract boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic);

    /**
     * Called to handle subscribe if {@link #canSubscribe} returned true.
     */
    public abstract void onSubscribe(RemotingConnection connection, Topic topic);

    /**
     * Called to handle unsubscribe if {@link #handlesTopic} returned true.
     */
    public abstract void onUnsubscribe(RemotingConnection connection, Topic topic);

    /**
     * Get the set of topics this handler wants to subscribe to for incoming publish messages; messages that match
     * these topics will be passed to {@link #onPublish}.
     */
    public abstract Set<String> getPublishListenerTopics();

    /**
     * Called to handle publish if {@link #canPublish} returned true.
     */
    public abstract void onPublish(RemotingConnection connection, Topic topic, ByteBuf body);

    public static String topicRealm(Topic topic) {
        return topicTokenIndexToString(topic, 0);
    }

    public static boolean topicRealmAllowed(KeycloakSecurityContext securityContext, Topic topic) {
        return securityContext.getRealm().equals(topicRealm(topic)) || KeycloakIdentityProvider.isSuperUser(securityContext);
    }

    public static boolean topicClientIdMatches(RemotingConnection connection, Topic topic) {
        return Objects.equals(connection.getClientID(), topicTokenIndexToString(topic, 1));
    }

    public static boolean topicTokenCountGreaterThan(Topic topic, int size) {
        return topic.getTokens() != null && topic.getTokens().size() > size;
    }

    public static String topicTokenIndexToString(Topic topic, int tokenNumber) {
        return topicTokenCountGreaterThan(topic, tokenNumber) ? topic.getTokens().get(tokenNumber) : null;
    }

    protected static Subject getSubjectFromConnection(RemotingConnection connection) {
        return connection != null ? connection.getSubject() : null;
    }

    protected static RemotingConnection getConnectionFromSubject(Subject subject) {
        return subject.getPrincipals()
            .stream()
            .filter(p -> p instanceof MQTTConnectionPrincipal)
            .findFirst()
            .map(p -> ((MQTTConnectionPrincipal)p).getConnection())
            .orElse(null);
    }

    protected static KeycloakSecurityContext getSecurityContextFromSubject(Subject subject) {
        return KeycloakIdentityProvider.getSecurityContext(subject);
    }

    protected static Optional<AuthContext> getAuthContextFromConnection(RemotingConnection connection) {
        return Optional.ofNullable(getSubjectFromConnection(connection))
            .map(MQTTHandler::getSecurityContextFromSubject)
            .map(DefaultMQTTHandler::getAuthContextFromSecurityContext);
    }

    protected static AuthContext getAuthContextFromSecurityContext(KeycloakSecurityContext securityContext) {
        return securityContext == null ? null : new AccessTokenAuthContext(securityContext.getRealm(), securityContext.getToken());
    }
}
