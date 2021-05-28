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
            LOG.log(Level.FINE, "Couldn't verify token: " + connection, e);
            return false;
        }

        if (authContext == null) {
            LOG.warning("Failed to get auth context for connection: " + connection);
            return false;
        }

        Boolean result = brokerService.customHandlerAuthorises(authContext, connection, topic, isWrite);
        if (result != null) {
            return result;
        }

        List<String> topicTokens = topic.getTokens().stream().map(Token::toString).collect(Collectors.toList());

        if (topicTokens.size() < 2) {
            LOG.fine("Topic must contain at least two tokens '" + topic + "': " + connection);
            return false;
        }

        boolean isAttributeTopic = MqttBrokerService.isAttributeTopic(topicTokens);
        boolean isAssetTopic = MqttBrokerService.isAssetTopic(topicTokens);

        if (!isAssetTopic && !isAttributeTopic) {
            LOG.fine("Topic starts with invalid value '" + topic  + "': " + connection);
            return false;
        }

        String assetId = topicTokens.get(1);

        if (!SINGLE_LEVEL_WILDCARD.equals(assetId) && !MULTI_LEVEL_WILDCARD.equals(assetId)) {
            Asset<?> asset = assetStorageService.find(assetId);

            if (asset == null) {
                LOG.fine("Asset not found for topic '" + topic + "': " + connection);
                return false;
            }

            if (isAttributeTopic && topicTokens.size() > 2
                && !(SINGLE_LEVEL_WILDCARD.equals(topicTokens.get(2)) || MULTI_LEVEL_WILDCARD.equals(topicTokens.get(2)))) {
                String attributeName = topicTokens.get(2);

                if (!asset.hasAttribute(attributeName)) {
                    LOG.fine("Asset attribute not found for topic '" + topic + "': " + connection);
                    return false;
                }
            }
        }

        if (isWrite) {

            if (topicTokens.contains(SINGLE_LEVEL_WILDCARD) || topicTokens.contains(MULTI_LEVEL_WILDCARD)) {
                LOG.fine("Write does not support wildcards '" + topic + "': " + connection);
                return false;
            }

            if (isAssetTopic) {
                LOG.fine("Cannot write assets '" + topic + "': " + connection);
                return false;
            }


            if (topicTokens.get(0).equals(ATTRIBUTE_VALUE_TOPIC) && topicTokens.size() != 3) {
                LOG.info("Write request to attribute value topic should contain asset ID and attribute name only, topic '" + topic + "': " + connection);
                return false;
            }

            if (topicTokens.get(0).equals(ATTRIBUTE_TOPIC) && topicTokens.size() != 1) {
                LOG.info("Write request to attribute topic should not contain any other tokens, topic '" + topic + "': " + connection);
                return false;
            }

            // Check we have the correct write role
            if (!authContext.hasResourceRole(ClientRole.WRITE_ATTRIBUTES.getValue(), username)) {
                LOG.info("Write request to attribute topic but user doesn't have required role, topic '" + topic + "': " + connection);
                return false;
            }

            // Security of attribute write will be handled by the asset processing service so don't need to do anything here
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

