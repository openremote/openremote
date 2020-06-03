package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Token;
import io.moquette.broker.subscriptions.Topic;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.ClientCredentialsAuthForm;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.security.ClientRole;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.manager.mqtt.MqttBrokerService.ASSETS_TOPIC;
import static org.openremote.manager.mqtt.MqttBrokerService.TOPIC_SEPARATOR;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

public class KeycloakAuthorizatorPolicy implements IAuthorizatorPolicy {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthorizatorPolicy.class.getName());

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final ClientEventService clientEventService;
    protected final Map<String, MqttConnection> mqttConnectionMap;

    public KeycloakAuthorizatorPolicy(ManagerKeycloakIdentityProvider identityProvider,
                                      ClientEventService clientEventService,
                                      Map<String, MqttConnection> mqttConnectionMap) {
        this.identityProvider = identityProvider;
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

    private boolean verifyRights(Topic topic, String username, String clientId, ClientRole... roles) {
        MqttConnection connection = mqttConnectionMap.get(clientId);
        if (connection == null) {
            LOG.info("No connection found for clientId: " + clientId);
            return false;
        }

        if (!connection.username.equals(username)) {
            LOG.info("Username mismatch");
            return false;
        }

        if (topic.isEmpty() || topic.getTokens().size() < 2) {
            LOG.info("Topic may not be empty and should have the following format: assets/{assetId}(optional: /{attributeName})");
            return false;
        }

        if (!topic.headToken().toString().equals(ASSETS_TOPIC)) {
            LOG.info("Topic should have the following format: assets/{assetId}(optional: /{attributeName})");
            return false;
        }

        AccessToken accessToken = null;
        try {
            accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
        } catch (VerificationException e) {
            if (e instanceof TokenNotActiveException) {
                String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
                connection.accessToken = identityProvider.getExternalKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthForm(connection.username, suppliedClientSecret)).getToken();
                try {
                    accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
                } catch (VerificationException verificationException) {
                    LOG.log(Level.INFO, "Couldn't verify token", verificationException);
                    return false;
                }
            } else {
                LOG.log(Level.INFO, "Couldn't verify token", e);
                return false;
            }
        }

        AuthContext authContext = new AccessTokenAuthContext(connection.realm, accessToken);
        if (Arrays.asList(roles).contains(ClientRole.WRITE_ASSETS)) { //write
            return identityProvider.canSubscribeWith(authContext, new TenantFilter(connection.realm), roles);
        } else { // read
            String[] topicParts = topic.getTokens().stream().map(Token::toString).toArray(String[]::new);
            String assetId = topicParts[1];
            AssetFilter<AttributeEvent> attributeAssetFilter = new AssetFilter<AttributeEvent>().setRealm(connection.realm).setAssetIds(assetId);
            if (topicParts.length == 3) { //attribute specific
                attributeAssetFilter.setAttributeNames(topicParts[2]);
            }
            EventSubscription<AttributeEvent> subscription = new EventSubscription<>(
                    AttributeEvent.class,
                    attributeAssetFilter
            );
            return clientEventService.authorizeEventSubscription(authContext, subscription);
        }
    }
}
