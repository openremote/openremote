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
package org.openremote.manager.provisioning;

import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.mqtt.MQTTHandler;
import org.openremote.manager.mqtt.MqttBrokerService;
import org.openremote.manager.mqtt.MqttConnection;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.provisioning.*;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Role;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.isPersistenceEventForEntityType;
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
                .routeId("ProvisioningConfigPersistenceChanges")
                .filter(isPersistenceEventForEntityType(ProvisioningConfig.class))
                .process(exchange -> {
                    PersistenceEvent<ProvisioningConfig<?,?>> persistenceEvent = (PersistenceEvent<ProvisioningConfig<?,?>>)exchange.getIn().getBody(PersistenceEvent.class);

                    boolean forceDisconnect = persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE;

                    if (persistenceEvent.getCause() == PersistenceEvent.Cause.UPDATE) {
                        // Force disconnect if the certain properties have changed
                        forceDisconnect = Arrays.stream(persistenceEvent.getPropertyNames()).anyMatch((propertyName) ->
                            propertyName.equals(ProvisioningConfig.DISABLED_PROPERTY_NAME)
                                || propertyName.equals(ProvisioningConfig.DATA_PROPERTY_NAME));
                    }

                    if (forceDisconnect) {
                        LOG.info("Provisioning config modified or deleted so forcing connected clients to disconnect: " + persistenceEvent.getEntity());
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
    protected MqttBrokerService brokerService;
    protected AssetStorageService assetStorageService;
    protected ManagerKeycloakIdentityProvider identityProvider;
    protected boolean isKeycloak;
    protected final Map<Long, Set<MqttConnection>> provisioningConfigAuthenticatedConnectionMap = new HashMap<>();

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        provisioningService = container.getService(ProvisioningService.class);
        timerService = container.getService(TimerService.class);
        brokerService = container.getService(MqttBrokerService.class);
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

    @Override
    public boolean handlesTopic(Topic topic) {
        // Skip standard checks
        return topicMatches(topic);
    }

    @Override
    public boolean checkCanSubscribe(MqttConnection connection, Topic topic) {
        // Skip standard checks
        return canSubscribe(connection, topic);
    }

    @Override
    public boolean checkCanPublish(MqttConnection connection, Topic topic) {
        // Skip standard checks
        return canPublish(connection, topic);
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
    public boolean canSubscribe(MqttConnection connection, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        return isResponseTopic(topic)
            && !TOKEN_MULTI_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1))
            && !TOKEN_SINGLE_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1));
    }

    @Override
    public void doSubscribe(MqttConnection connection, Topic topic, InterceptSubscribeMessage msg) {
        // Nothing to do here as we'll publish to this topic in response to client messages
    }

    @Override
    public void doUnsubscribe(MqttConnection connection, Topic topic, InterceptUnsubscribeMessage msg) {

    }

    @Override
    public boolean canPublish(MqttConnection connection, Topic topic) {
        if (!isKeycloak) {
            LOG.fine("Identity provider is not keycloak");
            return false;
        }

        return isRequestTopic(topic)
            && !TOKEN_MULTI_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1))
            && !TOKEN_SINGLE_LEVEL_WILDCARD.equals(topicTokenIndexToString(topic, 1));
    }

    @Override
    public void doPublish(MqttConnection connection, Topic topic, InterceptPublishMessage msg) {
        String payloadContent = msg.getPayload().toString(StandardCharsets.UTF_8);

        ProvisioningMessage provisioningMessage = ValueUtil.parse(payloadContent, ProvisioningMessage.class)
            .orElseGet(() -> {
                LOG.fine("Failed to parse message from client: topic=" + topic + ", connection=" + connection);
                brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.MESSAGE_INVALID), MqttQoS.AT_MOST_ONCE);
                return null;
            });

        if (provisioningMessage == null) {
            return;
        }

        if (provisioningMessage instanceof X509ProvisioningMessage) {
            processX509ProvisioningMessage(connection, topic, (X509ProvisioningMessage)provisioningMessage);
        }
    }

    @Override
    public void onConnectionLost(MqttConnection connection, InterceptConnectionLostMessage msg) {
        synchronized (provisioningConfigAuthenticatedConnectionMap) {
            provisioningConfigAuthenticatedConnectionMap.values().forEach(connections -> connections.remove(connection));
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

    protected void processX509ProvisioningMessage(MqttConnection connection, Topic topic, X509ProvisioningMessage provisioningMessage) {

        if (TextUtil.isNullOrEmpty(provisioningMessage.getCert())) {
            LOG.fine("Certificate is missing from X509 provisioning message: topic=" + topic + ", connection=" + connection);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CERTIFICATE_INVALID), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Parse client cert
        X509Certificate clientCertificate;
        try {
            clientCertificate = ProvisioningUtil.getX509Certificate(provisioningMessage.getCert());
        } catch (CertificateException e) {
            LOG.log(Level.INFO, "Failed to parse client X.509 certificate: topic=" + topic + ", connection=" + connection, e);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CERTIFICATE_INVALID), MqttQoS.AT_MOST_ONCE);
            return;
        }

        X509ProvisioningConfig matchingConfig = getMatchingX509ProvisioningConfig(connection, clientCertificate);

        if (matchingConfig == null) {
            LOG.fine("No matching provisioning config found for client certificate: topic=" + topic + ", connection=" + connection);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNAUTHORIZED), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Check if config is disabled
        if (matchingConfig.isDisabled()) {
            LOG.fine("Matching provisioning config is disabled for client certificate: topic=" + topic + ", connection=" + connection);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.CONFIG_DISABLED), MqttQoS.AT_MOST_ONCE);
            return;
        }

        // Validate unique ID
        String certUniqueId = ProvisioningUtil.getSubjectCN(clientCertificate.getSubjectX500Principal());
        String uniqueId = topicTokenIndexToString(topic, 1);

        if (TextUtil.isNullOrEmpty(certUniqueId)) {
            LOG.info("Client X.509 certificate missing unique ID in subject CN: topic=" + topic + ", connection=" + connection);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNIQUE_ID_MISMATCH), MqttQoS.AT_MOST_ONCE);
            return;
        }

        if (TextUtil.isNullOrEmpty(uniqueId) || !certUniqueId.equals(uniqueId)) {
            LOG.info("Client X.509 certificate unique ID doesn't match topic unique ID: topic=" + topic + ", connection=" + connection);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.UNIQUE_ID_MISMATCH), MqttQoS.AT_MOST_ONCE);
            return;
        }

        String realm = matchingConfig.getRealm();

        // Get/create service user
        String serviceUsername = (PROVISIONING_USER_PREFIX + uniqueId).toLowerCase(); // Keycloak clients are case sensitive but pretends not to be so always force lowercase
        if (serviceUsername.length() > 255) {
            // Keycloak has a 255 character limit on clientId
            serviceUsername = serviceUsername.substring(0, 254);
        }
        User serviceUser;

        try {
            serviceUser = identityProvider.getUserByUsername(realm, User.SERVICE_ACCOUNT_PREFIX + serviceUsername);

            if (serviceUser != null) {
                if (!serviceUser.getEnabled()) {
                    LOG.info("Client service user has been disabled: topic=" + topic + ", connection=" + connection);
                    brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.USER_DISABLED), MqttQoS.AT_MOST_ONCE);
                    return;
                }
            } else {
                serviceUser = createClientServiceUser(realm, serviceUsername, matchingConfig);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve/create service user: topic=" + topic + ", connection=" + connection, e);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.SERVER_ERROR), MqttQoS.AT_MOST_ONCE);
            return;
        }

        Asset<?> asset;

        // Prepend realm name to unique ID to generate asset ID to further improve uniqueness
        String assetId = UniqueIdentifierGenerator.generateId(matchingConfig.getRealm() + uniqueId);
        LOG.fine("Client unique ID '" + uniqueId + "' mapped to asset ID '" + assetId + "': topic=" + topic + ", connection=" + connection);

        try {
            // Look for existing asset
            asset = assetStorageService.find(assetId);

            if (asset != null) {
                LOG.finer("Client asset found: topic=" + topic + ", connection=" + connection + ", assetId=" + assetId);

                if (!matchingConfig.getRealm().equals(asset.getRealm())) {
                    LOG.info("Client asset realm mismatch : topic=" + topic + ", connection=" + connection + ", assetId=" + assetId);
                    brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.ASSET_ERROR), MqttQoS.AT_MOST_ONCE);
                    return;
                }
            } else {
                asset = createClientAsset(realm, assetId, uniqueId, serviceUser, matchingConfig);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve/create asset: topic=" + topic + ", connection=" + connection + ", config=" + matchingConfig, e);
            brokerService.publishMessage(getResponseTopic(topic), new ErrorResponseMessage(ErrorResponseMessage.Error.SERVER_ERROR), MqttQoS.AT_MOST_ONCE);
            return;
        }

        LOG.fine("Client successfully initialised: topic=" + topic + ", connection=" + connection + ", config=" + matchingConfig);

        // Update connection with service user credentials
        connection.setCredentials(realm, serviceUser.getUsername(), serviceUser.getSecret());

        synchronized (provisioningConfigAuthenticatedConnectionMap) {
            provisioningConfigAuthenticatedConnectionMap.compute(matchingConfig.getId(), (id, connections) -> {
                if (connections == null) {
                    connections = new HashSet<>();
                }
                connections.remove(connection);
                connections.add(connection);
                return connections;
            });
            brokerService.publishMessage(getResponseTopic(topic), new SuccessResponseMessage(realm, asset), MqttQoS.AT_MOST_ONCE);
        }
    }

    protected X509ProvisioningConfig getMatchingX509ProvisioningConfig(MqttConnection connection, X509Certificate clientCertificate) {
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
                            LOG.fine("Client certificate issuer matches provisioning config CA certificate subject: connection=" + connection + ", config=" + config);
                            Date now = Date.from(timerService.getNow());

                            try {
                                clientCertificate.verify(caCertificate.getPublicKey());
                                LOG.fine("Client certificate verified against CA certificate: connection=" + connection + ", config=" + config);

                                if (!config.getData().isIgnoreExpiryDate()) {
                                    LOG.fine("Validating client certificate validity: connection=" + connection + ", timestamp=" + now);
                                    clientCertificate.checkValidity(now);
                                }

                                return true;
                            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                                LOG.log(Level.INFO, "Client certificate failed validity check: connection=" + connection + ", timestamp=" + now, e);
                            } catch (Exception e) {
                                LOG.log(Level.INFO, "Client certificate failed verification against CA certificate: connection=" + connection + ", config=" + config, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to extract certificate from provisioning config: config=" + config, e);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }

    protected User createClientServiceUser(String realm, String username, ProvisioningConfig<?, ?> provisioningConfig) {
        LOG.fine("Creating client service user: realm=" + realm + ", username=" + username);

        User serviceUser = new User()
            .setServiceAccount(true)
            .setEnabled(true)
            .setUsername(username);

        String secret = UniqueIdentifierGenerator.generateId();
        serviceUser = identityProvider.createUpdateUser(realm, serviceUser, secret);

        if (provisioningConfig.getUserRoles() != null && provisioningConfig.getUserRoles().length > 0) {
            LOG.finer("Setting user roles: realm=" + realm + ", username=" + username + ", roles=" + Arrays.toString(provisioningConfig.getUserRoles()));
            identityProvider.updateUserRoles(
                realm,
                serviceUser.getId(),
                username,
                Arrays.stream(provisioningConfig.getUserRoles()).map(ClientRole::getValue).toArray(String[]::new)
            );
        } else {
            LOG.finer("No user roles defined: realm=" + realm + ", username=" + username);
        }

        if (provisioningConfig.isRestrictedUser()) {
            LOG.finer("User will be made restricted: realm=" + realm + ", username=" + username);
            identityProvider.updateUserRealmRoles(realm, serviceUser.getId(), identityProvider.addRealmRoles(realm, serviceUser.getId(),RESTRICTED_USER_REALM_ROLE));
        }

        // Inject secret
        serviceUser.setSecret(secret);
        return serviceUser;
    }

    protected Asset<?> createClientAsset(String realm, String assetId, String uniqueId, User serviceUser, ProvisioningConfig<?, ?> provisioningConfig) throws RuntimeException {
        LOG.fine("Creating client asset: realm=" + realm + ", username=" + serviceUser.getUsername());

        if (TextUtil.isNullOrEmpty(provisioningConfig.getAssetTemplate())) {
            return null;
        }

        // Replace any placeholders in the template
        String assetTemplate = provisioningConfig.getAssetTemplate();
        assetTemplate = assetTemplate.replaceAll(UNIQUE_ID_PLACEHOLDER, uniqueId);

        // Try and parse provisioning config asset template
        Asset<?> asset = ValueUtil.parse(assetTemplate, Asset.class).orElseThrow(() ->
            new RuntimeException("Failed to de-serialise asset template into an asset instance: template=" + provisioningConfig.getAssetTemplate())
        );

        // Set ID and realm
        asset.setId(assetId);
        asset.setRealm(realm);

        assetStorageService.merge(asset);

        if (provisioningConfig.isRestrictedUser()) {
            assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(realm, serviceUser.getId(), assetId)));
        }

        return asset;
    }

    protected void forceClientDisconnects(long provisioningConfigId) {
        synchronized (provisioningConfigAuthenticatedConnectionMap) {
            provisioningConfigAuthenticatedConnectionMap.computeIfPresent(provisioningConfigId, (id, connections) -> {
                // Force disconnect of each connection and the disconnect handler will remove the connection from the map
                connections.forEach(connection -> brokerService.forceDisconnect(connection.getClientId()));
                return connections;
            });
        }
    }
}
