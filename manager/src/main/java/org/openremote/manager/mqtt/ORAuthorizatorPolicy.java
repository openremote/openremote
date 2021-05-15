package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Token;
import io.moquette.broker.subscriptions.Topic;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.manager.mqtt.MqttBrokerService.*;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class ORAuthorizatorPolicy implements IAuthorizatorPolicy {

    private static final Logger LOG = SyslogCategory.getLogger(API, ORAuthorizatorPolicy.class);

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final ClientEventService clientEventService;
    protected final MqttBrokerService brokerService;

    public ORAuthorizatorPolicy(ManagerKeycloakIdentityProvider identityProvider,
                                MqttBrokerService brokerService,
                                AssetStorageService assetStorageService,
                                ClientEventService clientEventService) {
        this.identityProvider = identityProvider;
        this.brokerService = brokerService;
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
    }

    @Override
    public boolean canWrite(Topic topic, String username, String clientId) {
        String[] realmAndUsername = username.split(":");
        String realm = realmAndUsername[0];
        username = realmAndUsername[1];
        return verifyRights(topic, clientId, realm, username, true);
    }

    @Override
    public boolean canRead(Topic topic, String username, String clientId) {
        String[] realmAndUsername = username.split(":");
        String realm = realmAndUsername[0];
        username = realmAndUsername[1];
        return verifyRights(topic, clientId, realm, username, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean verifyRights(Topic topic, String sessionId, String realm, String username, boolean isWrite) {
        MqttConnection connection = brokerService.sessionIdConnectionMap.get(sessionId);
        AuthContext authContext;

        if (connection == null) {
            LOG.warning("No connection found: sessionId=" + sessionId);
            return false;
        }

        try {
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.getAccessToken(), identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
            authContext = accessToken != null ? new AccessTokenAuthContext(connection.realm, accessToken) : null;
        } catch (VerificationException e) {
            LOG.log(Level.INFO, "Couldn't verify token: " + connection, e);
            return false;
        }

        if (authContext == null) {
            LOG.info("Failed to get auth context for connection: " + connection);
            return false;
        }

        Boolean result = brokerService.customHandlerAuthorises(authContext, connection, topic, isWrite);
        if (result != null) {
            return result;
        }

        List<String> topicTokens = topic.getTokens().stream().map(Token::toString).collect(Collectors.toList());

        if (topicTokens.size() < 2) {
            LOG.info("Topic may not be empty and should have the following format: //asset/... or //attribute/...");
            return false;
        }

        boolean isAttributeTopic = MqttBrokerService.isAttributeTopic(topicTokens);
        boolean isAssetTopic = MqttBrokerService.isAssetTopic(topicTokens);

        if (!isAssetTopic && !isAttributeTopic) {
            LOG.info("Topic should start with 'asset' or 'attribute': " + connection);
            return false;
        }

        String assetId = topicTokens.get(1);

        if (!SINGLE_LEVEL_WILDCARD.equals(assetId) && !MULTI_LEVEL_WILDCARD.equals(assetId)) {
            Asset<?> asset = assetStorageService.find(assetId);

            if (asset == null) {
                LOG.info("Asset not found for topic '" + topic + "': " + connection);
                return false;
            }

            if (isAttributeTopic && topicTokens.size() > 2
                && !(SINGLE_LEVEL_WILDCARD.equals(topicTokens.get(2)) || MULTI_LEVEL_WILDCARD.equals(topicTokens.get(2)))) {
                String attributeName = topicTokens.get(2);

                if (!asset.hasAttribute(attributeName)) {
                    LOG.info("Asset attribute not found for topic '" + topic + "': " + connection);
                    return false;
                }
            }
        }

        if (isWrite) {

            if (topicTokens.contains(SINGLE_LEVEL_WILDCARD) || topicTokens.contains(MULTI_LEVEL_WILDCARD)) {
                LOG.info("Write does not support wildcards in topic: " + connection);
                return false;
            }

            // Check we have the correct write role
            if (isAssetTopic && !authContext.hasResourceRole(ClientRole.WRITE_ASSETS.getValue(), username)) {
                LOG.info("Write request to asset topic but user doesn't have required role: " + connection);
                return false;
            } else if (isAttributeTopic && !authContext.hasResourceRole(ClientRole.WRITE_ATTRIBUTES.getValue(), username)) {
                LOG.info("Write request to attribute topic but user doesn't have required role: " + connection);
                return false;
            }

            // Check if this is a restricted user
            boolean isRestrictedUser = identityProvider.isRestrictedUser(authContext.getUserId());

            if (isAssetTopic) {
                if (topicTokens.size() > 1) {
                    LOG.info("Write request to asset topic shouldn't contain any topic parts: " + connection);
                    return false;
                }

                if (isRestrictedUser) {
                    LOG.info("Restricted users cannot write assets: " + connection);
                    return false;
                }
            } else {
                // Must be of form //attribute/assetId/attributeName or //attribute/assetId/attributeName/value
                if (topicTokens.size() == 3 || (topicTokens.size() == 4 && ATTRIBUTE_VALUE_TOPIC.equals(topicTokens.get(3)))) {
                    LOG.info("Write request to attribute topic should contain asset ID and attribute name only: " + connection);
                    return false;
                }

                // Security of attribute write will be handled by the asset processing service so don't need to do anything here
            }
        } else { // read

            // Build filter for the topic and verify that the filter is OK for given auth context
            AssetFilter filter = buildAssetFilter(connection, topicTokens);
            Class subscriptionClass = isAssetTopic ? AssetEvent.class : AttributeEvent.class;

            if (filter == null) {
                LOG.info("Invalid event filter generated for topic '" + topic + "': " + connection);
                return false;
            }
            EventSubscription subscription = new EventSubscription(
                subscriptionClass,
                filter
            );

            if (!clientEventService.authorizeEventSubscription(authContext, subscription)) {
                LOG.info("Generated event filter was not authorised for topic '" + topic + "': " + connection);
                return false;
            }
        }

        return true;
    }
}

