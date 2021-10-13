package org.openremote.manager.mqtt;

import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.TextUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handles access token generation and tracks all subscriptions for the connection
 */
public class MqttConnection {

    protected static final Logger LOG = Logger.getLogger(MqttConnection.class.getSimpleName());
    protected final String realm;
    protected final String username; // This is OAuth clientId
    protected final String password;
    protected final boolean credentials;
    protected final Map<String, Consumer<SharedEvent>> subscriptionHandlerMap = new HashMap<>();
    protected final String clientId;
    protected Supplier<String> tokenSupplier;

    public MqttConnection(ManagerKeycloakIdentityProvider identityProvider, String clientId, String realm, String username, String password) {
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        String tokenEndpointUri = identityProvider.getTokenUri(realm).toString();

        credentials = !TextUtil.isNullOrEmpty(realm)
            && !TextUtil.isNullOrEmpty(username)
            && !TextUtil.isNullOrEmpty(password);

        if (credentials) {
            OAuthGrant grant = new OAuthClientCredentialsGrant(tokenEndpointUri, username, password, null);
            tokenSupplier = identityProvider.getAccessTokenSupplier(grant);
        } else {
            LOG.fine("MQTT connection with no credentials so will have limited capabilities: " + this);
        }
    }

    public String getRealm() {
        return this.realm;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public Map<String, Consumer<SharedEvent>> getSubscriptionHandlerMap() {
        return this.subscriptionHandlerMap;
    }

    public String getAccessToken() {
        if (tokenSupplier == null) {
            return null;
        }

        return tokenSupplier.get();
    }

    /**
     * This is MQTT client ID not to be confused with OAuth client ID
     */
    public String getClientId() {
        return clientId;
    }

    public boolean hasCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            ", username='" + username + '\'' +
            ", sessionId='" + clientId + '\'' +
            '}';
    }
}
