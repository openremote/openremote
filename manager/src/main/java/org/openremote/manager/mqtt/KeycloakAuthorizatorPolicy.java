package org.openremote.manager.mqtt;

import com.google.inject.internal.cglib.core.$AbstractClassGenerator;
import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.security.ClientCredentialsAuthFrom;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.security.ClientRole;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.MqttBrokerService.ASSETS_TOPIC;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

public class KeycloakAuthorizatorPolicy implements IAuthorizatorPolicy {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthorizatorPolicy.class.getName());

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final MqttConnector mqttConnector;

    public KeycloakAuthorizatorPolicy(ManagerKeycloakIdentityProvider identityProvider,
                                      AssetStorageService assetStorageService,
                                      MqttConnector mqttConnector) {
        this.identityProvider = identityProvider;
        this.assetStorageService = assetStorageService;
        this.mqttConnector = mqttConnector;
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
        MqttConnector.MqttConnection connection = mqttConnector.getConnection(clientId);
        if (connection == null) {
            LOG.info("No connection found for clientId: " + clientId);
            return false;
        }

        if (!connection.username.equals(username)) {
            LOG.info("Username mismatch");
            return false;
        }

        if (topic.isEmpty() || topic.getTokens().size() != 3) {
            LOG.info("Topic may not be empty and should have the following format: assets/{assetId}/{attributeName}");
            return false;
        }

        if (!topic.headToken().toString().equals(ASSETS_TOPIC)) {
            LOG.info("Topic should have the following format: assets/{assetId}/{attributeName}");
            return false;
        }

        String assetId = topic.getTokens().get(1).toString();
        Asset asset = assetStorageService.find(assetId);
        if (asset == null) {
            LOG.info("Asset not found for assetId: " + assetId);
            return false;
        }

        if (!asset.getRealm().equals(connection.realm)) {
            LOG.info("Asset not in same clientId");
            return false;
        }

        String attributeName = topic.getTokens().get(2).toString();
        if(!asset.getAttribute(attributeName).isPresent()) {
            LOG.info("Attribute not found: " + attributeName);
            return false;
        }

        try {
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
            return identityProvider.canSubscribeWith(new AccessTokenAuthContext(connection.realm, accessToken), new TenantFilter(connection.realm), roles);
        } catch (VerificationException e) {
            if (e instanceof TokenNotActiveException) {
                String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
                connection.accessToken = identityProvider.getKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthFrom(connection.username, suppliedClientSecret)).getToken();
                try {
                    AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(connection.realm, KEYCLOAK_CLIENT_ID));
                    return identityProvider.canSubscribeWith(new AccessTokenAuthContext(connection.realm, accessToken), new TenantFilter(connection.realm), roles);
                } catch (VerificationException verificationException) {
                    LOG.log(Level.INFO, "Couldn't verify token", verificationException);
                    return false;
                }
            } else {
                LOG.log(Level.INFO, "Couldn't verify token", e);
                return false;
            }
        }
    }
}
