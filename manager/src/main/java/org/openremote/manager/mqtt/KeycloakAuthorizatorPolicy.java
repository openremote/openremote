package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.AccessToken;
import org.openremote.container.security.ClientCredentialsAuthFrom;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.security.ClientRole;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.KeycloakAuthenticator.MQTT_CLIENT_ID_SEPARATOR;
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
    public boolean canWrite(Topic topic, String clientId, String realm) {
        return verifyRights(topic, clientId, realm, ClientRole.WRITE_ASSETS);
    }

    @Override
    public boolean canRead(Topic topic, String clientId, String realm) {
        return verifyRights(topic, clientId, realm, ClientRole.READ_ASSETS);
    }

    private boolean verifyRights(Topic topic, String clientId, String realm, ClientRole ...roles) {
        int indexSplit = realm.indexOf(MQTT_CLIENT_ID_SEPARATOR);
        if (indexSplit > 0) {
            realm = realm.substring(0, indexSplit);
        }

        MqttConnector.MqttConnection connection = mqttConnector.getConnection(clientId);
        if (connection == null) {
            LOG.info("No connection found for clientId: " + clientId);
            return false;
        }

        if (topic.isEmpty() || topic.getTokens().size() > 2) {
            LOG.info("Topic may not be empty and should have the following format: asset/{assetId}");
            return false;
        }

        if (!topic.headToken().toString().equals("asset")) {
            LOG.info("Topic should have the following format: asset/{assetId}");
            return false;
        }

        String assetId = topic.getTokens().get(1).toString();
        Asset asset = assetStorageService.find(assetId);
        if (asset == null) {
            LOG.info("Asset not found for assetId: " + assetId);
            return false;
        }

        if (!asset.getRealm().equals(realm)) {
            LOG.info("Asset not in same realm");
            return false;
        }
        try {
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID));
            return identityProvider.canSubscribeWith(new AccessTokenAuthContext(realm, accessToken), new TenantFilter(realm), roles);
        } catch (VerificationException e) {
            if (e instanceof TokenNotActiveException) {
                String suppliedClientSecret = new String(connection.password, StandardCharsets.UTF_8);
                connection.accessToken = identityProvider.getKeycloak().getAccessToken(connection.realm, new ClientCredentialsAuthFrom(MqttBrokerService.MQTT_KEYCLOAK_CLIENT_ID, suppliedClientSecret)).getToken();
                try {
                    AccessToken accessToken = AdapterTokenVerifier.verifyToken(connection.accessToken, identityProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID));
                    return identityProvider.canSubscribeWith(new AccessTokenAuthContext(realm, accessToken), new TenantFilter(realm), roles);
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
