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

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.camel.builder.RouteBuilder;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.provisioning.ProvisioningService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.provisioning.*;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE;
import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * This {@link MQTTHandler} is responsible for provisioning service users and assets and authenticating the client
 * against the configured {@link org.openremote.model.provisioning.ProvisioningConfig}s.
 */
public class UserAssetProvisioningMQTTHandler extends MQTTHandler {

    protected static class ProvisioningPersistenceRouteBuilder extends RouteBuilder {

        UserAssetProvisioningMQTTHandler mqttHandler;

        public ProvisioningPersistenceRouteBuilder(UserAssetProvisioningMQTTHandler mqttHandler) {
            this.mqttHandler = mqttHandler;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void configure() throws Exception {

            from(PERSISTENCE_TOPIC)
                .routeId("Persistence-ProvisioningConfig")
                .filter(isPersistenceEventForEntityType(ProvisioningConfig.class))
                .process(exchange -> {
                    PersistenceEvent<ProvisioningConfig<?,?>> persistenceEvent = (PersistenceEvent<ProvisioningConfig<?,?>>)exchange.getIn().getBody(PersistenceEvent.class);

                    boolean forceDisconnect = persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE;

                    if (persistenceEvent.getCause() == PersistenceEvent.Cause.UPDATE) {
                        // Force disconnect if the certain properties have changed
                        forceDisconnect = persistenceEvent.hasPropertyChanged(ProvisioningConfig.DISABLED_PROPERTY_NAME)
                            || persistenceEvent.hasPropertyChanged(ProvisioningConfig.DATA_PROPERTY_NAME);
                    }

                    if (forceDisconnect) {
                        LOG.fine("Provisioning config modified or deleted so forcing connected clients to disconnect: " + persistenceEvent.getEntity());
                        mqttHandler.forceClientDisconnects(persistenceEvent.getEntity().getId());
                    }
                });
        }
    }

    protected static final Logger LOG = SyslogCategory.getLogger(API, UserAssetProvisioningMQTTHandler.class);
    public static final String PROVISIONING_TOKEN = "provisioning";
    public static final String REQUEST_TOKEN = "request";
    public static final String RESPONSE_TOKEN = "response";
    public static final String UNIQUE_ID_PLACEHOLDER = "%UNIQUE_ID%";
    public static final String PROVISIONING_USER_PREFIX = "ps-";
    protected ProvisioningService provisioningService;
    protected TimerService timerService;
    protected AssetStorageService assetStorageService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected boolean isKeycloak;
    protected final ConcurrentMap<Long, Set<RemotingConnection>> provisioningConfigAuthenticatedConnectionMap = new ConcurrentHashMap<>();
    protected Timer provisioningTimer;
    protected final Map<String, RemotingConnection> responseSubscribedConnections = new ConcurrentHashMap<>();

    @Override
    public void init(Container container, Configuration serverConfiguration) throws Exception {
        super.init(container, serverConfiguration);

        if (container.getMeterRegistry() != null) {
            provisioningTimer = container.getMeterRegistry().timer("or.provisioning", Tags.empty());
        }
    }

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        provisioningService = container.getService(ProvisioningService.class);
        timerService = container.getService(TimerService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
            container.getService(MessageBrokerService.class).getContext().addRoutes(new ProvisioningPersistenceRouteBuilder(this));
        }
    }

    /**
     * Limit the number of messages allowed in the address queue and fail when over; this will disconnect the client.
     * Approx 20ms per request with 20s timeout on auto provisioning request = 1000
     */
    @Override
    protected AddressSettings getPublishTopicAddressSettings(Container container, String publishTopic) {
        AddressSettings addressSettings = super.getPublishTopicAddressSettings(container, publishTopic);
        if (addressSettings != null) {
            addressSettings
                .setMaxSizeMessages(1000L);
        }
        return addressSettings;
    }

    @Override
    public boolean handlesTopic(Topic topic) {
        // Skip standard checks
        return topicMatches(topic);
    }

    @Override
    public boolean checkCanSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        // Skip standard checks
        if (!canSubscribe(connection, securityContext, topic)) {
            getLogger().fine("Cannot subscribe to this topic, topic=" + topic + ", " + MQTTBrokerService.connectionToString(connection));
            return false;
        }
        return true;
    }

    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        // Skip standard checks
        if (!canPublish(connection, securityContext, topic)) {
            getLogger().fine("Cannot publish to this topic, topic=" + topic + ", " + MQTTBrokerService.connectionToString(connection));
            return false;
        }
        return true;
    }

    @Override
    public boolean topicMatches(Topic topic) {
        return isProvisioningTopic(topic)
            && topic.getTokens().size() == 3
            && (isRequestTopic(topic) || isResponseTopic(topic));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        boolean allowed = isResponseTopic(topic)
            && !TOKEN_MULTI_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1))
            && !TOKEN_SINGLE_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1));

        if (allowed) {
            // Only allow if there is no existing subscription for this topic
            RemotingConnection existingConnection = responseSubscribedConnections.get(topic.getString());
            if (existingConnection != null) {
                LOG.warning("Subscription already exists possible eavesdropping");
                allowed = false;
            }
        }

        return allowed;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        responseSubscribedConnections.put(topic.getString(), connection);
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        responseSubscribedConnections.remove(topic.getString());
    }

    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
            PROVISIONING_TOKEN + "/" + Topic.SINGLE_LEVEL_TOKEN + "/" + REQUEST_TOKEN
        );
    }

    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        return isRequestTopic(topic)
            && !TOKEN_MULTI_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1))
            && !TOKEN_SINGLE_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1));
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        // Offload messages to the thread pool to improve processing rate
        if (!connection.getTransportConnection().isOpen()) {
            // Drop provisioning requests for closed connections
            LOG.finest(() -> "Skipping provisioning request as connection is now closed: " + MQTTBrokerService.connectionToString(connection));
            return;
        }
        // When no more threads available in the executorService the calling ActiveMQ client thread will execute which
        // will cause provisioning messages to wait in the address queue eventually hitting the message limit and
        // additional requests will be failed until the queue drains, this provides natural rate limiting.
        executorService.submit(() -> {
            if (provisioningTimer != null) {
                provisioningTimer.record(() -> processProvisioningRequest(connection, topic, body));
            } else {
                processProvisioningRequest(connection, topic, body);
            }
        });
    }

    @Override
    public void onConnectionLost(RemotingConnection connection) {
        provisioningConfigAuthenticatedConnectionMap.values().forEach(connections -> {
            if (connections.remove(connection)) {
                // Remove sessions as durable sessions will never reconnect if client ID already subscribed to attribute topics
                mqttBrokerService.doForceDisconnect(connection);
            }
        });
    }

    @Override
    public void onDisconnect(RemotingConnection connection) {
        provisioningConfigAuthenticatedConnectionMap.values().forEach(connections -> {
            if (connections.remove(connection)) {
                // Remove sessions as durable sessions will never reconnect if client ID already subscribed to attribute topics
                mqttBrokerService.doForceDisconnect(connection);
            }
        });
    }

    protected void processProvisioningRequest(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);

        ProvisioningMessage provisioningMessage = ValueUtil.parse(payloadContent, ProvisioningMessage.class)
            .orElseGet(() -> {
                LOG.info("Failed to parse provisioning request message from client: " + MQTTBrokerService.connectionToString(connection));
                publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.MESSAGE_INVALID), MqttQoS.AT_MOST_ONCE);
                return null;
            });

        if (provisioningMessage == null) {
            return;
        }

        if (provisioningMessage instanceof X509ProvisioningMessage) {
            processX509ProvisioningMessage(connection, topic, (X509ProvisioningMessage)provisioningMessage);
        }
    }

    protected static boolean isProvisioningTopic(Topic topic) {
        return PROVISIONING_TOKEN.equals(topicTokenIndexToString(topic, 0));
    }

    protected static boolean isRequestTopic(Topic topic) {
        return REQUEST_TOKEN.equals(topicTokenIndexToString(topic, 2));
    }

    protected static boolean isResponseTopic(Topic topic) {
        return RESPONSE_TOKEN.equals(topicTokenIndexToString(topic, 2));
    }

    protected String getResponseTopic(Topic topic) {
        return PROVISIONING_TOKEN + "/" + topicTokenIndexToString(topic, 1) + "/" + RESPONSE_TOKEN;
    }

    protected void processX509ProvisioningMessage(RemotingConnection connection, Topic topic, X509ProvisioningMessage provisioningMessage) {

        LOG.fine(() -> "Processing X.509 provisioning message: " + MQTTBrokerService.connectionToString(connection));

        if (TextUtil.isNullOrEmpty(provisioningMessage.getCert())) {
            LOG.info("Certificate is missing from X509 provisioning message" + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CERTIFICATE_INVALID), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Parse client cert
        X509Certificate clientCertificate;
        try {
            clientCertificate = ProvisioningUtil.getX509Certificate(provisioningMessage.getCert());
        } catch (CertificateException e) {
            LOG.log(Level.INFO, "Failed to parse X.509 certificate: " + MQTTBrokerService.connectionToString(connection), e);
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CERTIFICATE_INVALID), MqttQoS.AT_MOST_ONCE);
            return;
        }

        X509ProvisioningConfig matchingConfig = getMatchingX509ProvisioningConfig(connection, clientCertificate);

        if (matchingConfig == null) {
            LOG.fine("No matching provisioning config found for X.509 certificate: " + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNAUTHORIZED), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Check if config is disabled
        if (matchingConfig.isDisabled()) {
            LOG.fine("Matching provisioning config is disabled for X.509 certificate: " + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CONFIG_DISABLED), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Validate unique ID
        String certUniqueId = ProvisioningUtil.getSubjectCN(clientCertificate.getSubjectX500Principal());
        String uniqueId = topicTokenIndexToString(topic, 1);

        if (TextUtil.isNullOrEmpty(certUniqueId)) {
            LOG.info(() -> "X.509 certificate missing unique ID in subject CN: " + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNIQUE_ID_MISMATCH), MqttQoS.AT_MOST_ONCE);
            return;
        }

        if (TextUtil.isNullOrEmpty(uniqueId) || !certUniqueId.equals(uniqueId)) {
            LOG.info(() -> "X.509 certificate unique ID doesn't match topic unique ID: " + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNIQUE_ID_MISMATCH), MqttQoS.AT_MOST_ONCE);
            return;
        }

        String realm = matchingConfig.getRealm();

        // Get/create service user
        User serviceUser;

        try {
            LOG.finest("Checking service user: " + uniqueId);
            serviceUser = getCreateClientServiceUser(realm, identityProvider, uniqueId, matchingConfig);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve/create service user: " + MQTTBrokerService.connectionToString(connection), e);
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.SERVER_ERROR), MqttQoS.AT_MOST_ONCE);
            return;
        }

        if (!serviceUser.getEnabled()) {
            LOG.info(() -> "Service user exists and has been disabled so cannot continue:  " + MQTTBrokerService.connectionToString(connection));
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.USER_DISABLED), MqttQoS.AT_MOST_ONCE);
            return;
        }
        LOG.finest("Service user exists and is enabled");

        Asset<?> asset;

        try {
            LOG.finest(() -> "Checking provisioned asset: " + uniqueId);
            asset = getCreateClientAsset(assetStorageService, realm, uniqueId, serviceUser, matchingConfig);

            if (asset != null) {
                if (!matchingConfig.getRealm().equals(asset.getRealm())) {
                    LOG.warning("Client asset realm mismatch");
                    publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.ASSET_ERROR), MqttQoS.AT_MOST_ONCE);
                    return;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve/create asset: " + MQTTBrokerService.connectionToString(connection) + ", config=" + matchingConfig, e);
            publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.SERVER_ERROR), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Authenticate the connection using this service user's credentials - this will also update the connection's subject
        mqttBrokerService.authenticateConnection(connection, realm, serviceUser.getUsername(), serviceUser.getSecret());

        provisioningConfigAuthenticatedConnectionMap.compute(matchingConfig.getId(), (id, connections) -> {
            if (connections == null) {
                connections = ConcurrentHashMap.newKeySet();
            }
            connections.add(connection);
            return connections;
        });

        LOG.fine("Client successfully provisioned: " + uniqueId);
        publishMessage(getResponseTopic(topic), new SuccessResponseMessage(realm, asset), MqttQoS.AT_MOST_ONCE);
    }

    protected X509ProvisioningConfig getMatchingX509ProvisioningConfig(RemotingConnection connection, X509Certificate clientCertificate) {
        return provisioningService
            .getProvisioningConfigs()
            .stream()
            .filter(config -> config instanceof X509ProvisioningConfig)
            .map(config -> (X509ProvisioningConfig)config)
            .filter(config -> {
                try {
                    X509Certificate caCertificate = config.getCertificate();
                    if (caCertificate != null) {
                        if (caCertificate.getSubjectX500Principal().getName().equals(clientCertificate.getIssuerX500Principal().getName())) {
                            LOG.finest(() -> "Client certificate issuer matches provisioning config CA certificate subject: " + MQTTBrokerService.connectionToString(connection) + ", config=" + config);
                            Date now = Date.from(timerService.getNow());

                            try {
                                clientCertificate.verify(caCertificate.getPublicKey());
                                LOG.finest(() -> "Client certificate verified against CA certificate: " + MQTTBrokerService.connectionToString(connection) + ", config=" + config);

                                if (!config.getData().isIgnoreExpiryDate()) {
                                    LOG.finest(() -> "Validating client certificate validity: " + MQTTBrokerService.connectionToString(connection) + ", timestamp=" + now);
                                    clientCertificate.checkValidity(now);
                                }

                                return true;
                            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                                LOG.log(Level.INFO, "Client certificate failed validity check: " + MQTTBrokerService.connectionToString(connection) + ", timestamp=" + now, e);
                            } catch (Exception e) {
                                LOG.log(Level.INFO, "Client certificate failed verification against CA certificate: " + MQTTBrokerService.connectionToString(connection) + ", config=" + config, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract certificate from provisioning config: " + MQTTBrokerService.connectionToString(connection) + ", config=" + config, e);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }

    public static User getCreateClientServiceUser(String realm, ManagerKeycloakIdentityProvider identityProvider, String uniqueId, ProvisioningConfig<?, ?> provisioningConfig) throws RuntimeException {
        String username = (PROVISIONING_USER_PREFIX + uniqueId);
        User serviceUser = identityProvider.getUserByUsername(realm, User.SERVICE_ACCOUNT_PREFIX + username);

        if (serviceUser != null) {
            LOG.fine("Service user found: realm=" + realm + ", username=" + username);
            return serviceUser;
        }

        LOG.finest("Creating service user: realm=" + realm + ", username=" + username);

        serviceUser = new User()
            .setServiceAccount(true)
            .setEnabled(true)
            .setUsername(username);

        String secret = UniqueIdentifierGenerator.generateId();
        serviceUser = identityProvider.createUpdateUser(realm, serviceUser, secret, true);

        if (provisioningConfig.getUserRoles() != null && provisioningConfig.getUserRoles().length > 0) {
            LOG.finest("Setting user roles: realm=" + realm + ", username=" + username + ", roles=" + Arrays.toString(provisioningConfig.getUserRoles()));
            identityProvider.updateUserClientRoles(
                realm,
                serviceUser.getId(),
                Constants.KEYCLOAK_CLIENT_ID,
                Arrays.stream(provisioningConfig.getUserRoles()).map(ClientRole::getValue).toArray(String[]::new)
            );
        } else {
            LOG.finest("No user roles defined: realm=" + realm + ", username=" + username);
        }

        if (provisioningConfig.isRestrictedUser()) {
            LOG.finest("User will be made restricted: realm=" + realm + ", username=" + username);
            identityProvider.updateUserRealmRoles(realm, serviceUser.getId(), identityProvider.addUserRealmRoles(realm, serviceUser.getId(),RESTRICTED_USER_REALM_ROLE));
        }

        // Inject secret
        serviceUser.setSecret(secret);
        return serviceUser;
    }

    public static Asset<?> getCreateClientAsset(AssetStorageService assetStorageService, String realm, String uniqueId, User serviceUser, ProvisioningConfig<?, ?> provisioningConfig) throws RuntimeException {
        // Prepend realm name to unique ID to generate asset ID to further improve uniqueness
        String assetId = UniqueIdentifierGenerator.generateId(realm + uniqueId);
        Asset<?> asset = assetStorageService.find(assetId);

        if (asset != null) {
            LOG.finest("Asset exists");
            return asset;
        }

        LOG.finest("Creating client asset: realm=" + realm + ", username=" + serviceUser.getUsername());

        if (TextUtil.isNullOrEmpty(provisioningConfig.getAssetTemplate())) {
            LOG.finest("Provisioning config doesn't contain an asset template: " + provisioningConfig);
            return null;
        }

        // Replace any placeholders in the template
        String assetTemplate = provisioningConfig.getAssetTemplate();
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, uniqueId);

        // Try and parse provisioning config asset template
        asset = ValueUtil.parse(assetTemplate, Asset.class).orElseThrow(() ->
            new RuntimeException("Failed to de-serialise asset template into an asset instance: " + provisioningConfig)
        );

        // Set ID and realm
        asset.setId(assetId);
        asset.setRealm(realm);

        asset = assetStorageService.merge(asset);

        if (provisioningConfig.isRestrictedUser()) {
            assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(realm, serviceUser.getId(), assetId)));
        }

        return asset;
    }

    protected void forceClientDisconnects(long provisioningConfigId) {
        provisioningConfigAuthenticatedConnectionMap.computeIfPresent(provisioningConfigId, (id, connections) -> {
            // Force disconnect of each connection and the disconnect handler will remove the connection from the map
            connections.forEach(connection -> {
                try {
                    LOG.fine("Force disconnecting client that is using provisioning config ID '" + provisioningConfigId + "': " + MQTTBrokerService.connectionToString(connection));
                    connection.disconnect(false);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to disconnect client: " + MQTTBrokerService.connectionToString(connection), e);
                }
            });
            connections.clear();
            return connections;
        });
    }
}
