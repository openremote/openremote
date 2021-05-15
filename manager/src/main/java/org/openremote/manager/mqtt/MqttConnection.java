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
    protected final Map<String, Consumer<SharedEvent>> subscriptionHandlerMap = new HashMap<>();
    protected final String sessionId;
    protected Supplier<String> tokenSupplier;

    public MqttConnection(ManagerKeycloakIdentityProvider identityProvider, String sessionId, String realm, String username, String password) {
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
        String tokenEndpointUri = identityProvider.getTokenUri(realm).toString();

        if (!TextUtil.isNullOrEmpty(realm)
            && !TextUtil.isNullOrEmpty(username)
            && !TextUtil.isNullOrEmpty(password)) {

            OAuthGrant grant = new OAuthClientCredentialsGrant(tokenEndpointUri, username, password, null);
            tokenSupplier = identityProvider.getAccessTokenSupplier(grant);
        } else {
            LOG.info("Invalid credentials provided, MQTT connection is not valid: " + this);
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

    /**
     * Doesn't mean that credentials are valid just that correct info is set
     */
    public boolean isValid() {
        return tokenSupplier != null;
    }

    public String getAccessToken() {
        if (!isValid()) {
            return null;
        }

        return tokenSupplier.get();
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            ", username='" + username + '\'' +
            ", sessionId='" + sessionId + '\'' +
            '}';
    }
}
