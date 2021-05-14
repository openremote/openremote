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
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.MqttBrokerService.*;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.syslog.SyslogCategory.API;

public class KeycloakAuthorizatorPolicy implements IAuthorizatorPolicy {

    private static final Logger LOG = SyslogCategory.getLogger(API, KeycloakAuthorizatorPolicy.class);

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final ClientEventService clientEventService;
    protected final Map<String, MqttConnection> mqttConnectionMap;

    public KeycloakAuthorizatorPolicy(ManagerKeycloakIdentityProvider identityProvider,
                                      AssetStorageService assetStorageService, ClientEventService clientEventService,
                                      Map<String, MqttConnection> mqttConnectionMap) {
        this.identityProvider = identityProvider;
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
        this.mqttConnectionMap = mqttConnectionMap;
    }

    @Override
    public boolean canWrite(Topic topic, String username, String clientId) {
        return verifyRights(topic, username, clientId, ClientRole.WRITE_ASSETS);
    }

    @Override
    public boolean canRead(Topic topic, String username, String clientId) {
        return verifyRights(topic, username, clientId, ClientRole.READ_ASSETS);
    }

    protected boolean verifyRights(Topic topic, String username, String clientId, ClientRole... roles) {
        MqttConnection connection = mqttConnectionMap.get(clientId);
        if (connection == null) {
            LOG.warning("No connection found for clientId: " + clientId);
            return false;
        }

        if (!connection.username.equals(username)) {
            LOG.warning("Username mismatch");
            return false;
        }

        if (topic.isEmpty() || topic.getTokens().size() < 2) {
            LOG.warning("Topic may not be empty and should have the following format: asset/... or attribute/...");
            return false;
        }

        boolean isAttributeTopic = topic.headToken().toString().equals(ATTRIBUTE_TOPIC);

        if (!topic.headToken().toString().equals(ASSET_TOPIC) && !isAttributeTopic) {
            LOG.warning("Topic should start with 'asset' or 'attribute'");
            return false;
        }

        Token token = topic.getTokens().get(1);
        Asset<?> asset = null;
        if (!token.toString().equals(SINGLE_LEVEL_WILDCARD) && !token.toString().equals(MULTI_LEVEL_WILDCARD)) {
            asset = assetStorageService.find(token.toString());
            if (asset == null) {
                LOG.warning("Asset not found");
                return false;
            }
        }

        if (isAttributeTopic && topic.getTokens().size() > 2) {
            token = topic.getTokens().get(2);
            if (!token.toString().equals(SINGLE_LEVEL_WILDCARD) && !token.toString().equals(MULTI_LEVEL_WILDCARD)) {
                if (asset == null || !asset.getAttribute(token.toString()).isPresent()) {
                    LOG.warning("Attribute not found on asset");
                    return false;
                }
            }
        }

        AccessToken accessToken;
        try {
            accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
        } catch (VerificationException e) {
            String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
            connection.accessToken = identityProvider.getAccessToken(connection.realm, connection.username, suppliedClientSecret);
            try {
                accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
            } catch (VerificationException verificationException) {
                LOG.log(Level.WARNING, "Couldn't verify token", verificationException);
                return false;
            }
        }

        AuthContext authContext = new AccessTokenAuthContext(connection.realm, accessToken);
        if (Arrays.asList(roles).contains(ClientRole.WRITE_ASSETS)) { //write
            return identityProvider.canSubscribeWith(authContext, new TenantFilter(connection.realm), roles);
        } else { // read
            String[] topicParts = topic.getTokens().stream().map(Token::toString).toArray(String[]::new);

            if (isAttributeTopic) {
                AssetFilter<AttributeEvent> attributeAssetFilter = new AssetFilter<AttributeEvent>().setRealm(connection.realm);

                int singleLevelIndex = Arrays.asList(topicParts).indexOf(SINGLE_LEVEL_WILDCARD);

                if (asset != null) {
                    int multiLevelIndex = Arrays.asList(topicParts).indexOf(MULTI_LEVEL_WILDCARD);
                    if (multiLevelIndex == -1) {
                        if (singleLevelIndex == -1) { //attribute/assetId/
                            attributeAssetFilter.setAssetIds(asset.getId());
                        } else {
                            attributeAssetFilter.setParentIds(asset.getId());
                        }
                        if (topicParts.length > 2) { //attribute/assetId/attributeName
                            if (singleLevelIndex == -1) { //attribute/assetId/attributeName
                                attributeAssetFilter.setAttributeNames(topicParts[2]);
                            } else if (singleLevelIndex == 2 && topicParts.length > 3 && !topicParts[3].equals(SINGLE_LEVEL_WILDCARD)) { // else attribute/assetId/+/attributeName
                                attributeAssetFilter.setAttributeNames(topicParts[3]);
                            } // else attribute/assetId/+ which should return all attributes
                        }
                    } else if (multiLevelIndex == 2) { //attribute/assetId/#
                        attributeAssetFilter.setParentIds(asset.getParentId());
                    }
                } else {
                    if (singleLevelIndex == 1) { //attribute/+
                        if (topicParts.length > 2) { //attribute/+/attributeName
                            attributeAssetFilter.setAttributeNames(topicParts[2]);
                        } else {
                            LOG.warning("Using single level must be followed with attribute name when not using assetId");
                            return false;
                        }
                    }
                }

                EventSubscription<AttributeEvent> subscription = new EventSubscription<>(
                        AttributeEvent.class,
                        attributeAssetFilter
                );
                return clientEventService.authorizeEventSubscription(authContext, subscription);
            } else {
                AssetFilter<AssetEvent> assetFilter = new AssetFilter<AssetEvent>().setRealm(connection.realm);
                if (asset != null) {
                    int multiLevelIndex = Arrays.asList(topicParts).indexOf(MULTI_LEVEL_WILDCARD);
                    int singleLevelIndex = Arrays.asList(topicParts).indexOf(SINGLE_LEVEL_WILDCARD);

                    if (multiLevelIndex == -1) {
                        assetFilter.setAssetIds(asset.getId());
                        if (singleLevelIndex == 2) { //asset/assetId/+
                            assetFilter.setParentIds(asset.getId());
                        } else if (singleLevelIndex == -1) { //asset/assetId
                            assetFilter.setAssetIds(asset.getId());
                        } else {
                            LOG.warning("Using single level can be only used after assetId for asset events");
                            return false;
                        }
                    } else if (multiLevelIndex == 2) { //asset/assetId/#
                        assetFilter.setParentIds(asset.getParentId());
                    }
                }

                EventSubscription<AssetEvent> subscription = new EventSubscription<>(
                        AssetEvent.class,
                        assetFilter
                );
                return clientEventService.authorizeEventSubscription(authContext, subscription);
            }
        }
    }
}

