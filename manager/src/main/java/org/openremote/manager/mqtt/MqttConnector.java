package org.openremote.manager.mqtt;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.openremote.manager.mqtt.KeycloakAuthenticator.MQTT_CLIENT_ID_SEPARATOR;

public class MqttConnector {

    static class MqttConnection {
        protected final String realm;
        protected final String clientId;
        protected final String username;
        protected final byte[] password;
        protected final Set<String> assets;
        protected String accessToken;

        private MqttConnection(String clientId, String username, byte[] password, Set<String> assets) {
            int indexSplit = clientId.indexOf(MQTT_CLIENT_ID_SEPARATOR);
            if (indexSplit > 0) {
                realm = clientId.substring(0, indexSplit);
            } else {
                realm = clientId;
            }
            this.clientId = clientId;
            this.username = username;
            this.password = password;
            this.assets = assets;
        }
    }

    private static final Logger LOG = Logger.getLogger(MqttConnector.class.getName());

    protected final Map<String, MqttConnection> connectionMap;

    public MqttConnector() {
        connectionMap = new HashMap<>();
    }

    protected MqttConnection createConnection(String realm, String clientId, byte[] password) {
        if (connectionMap.containsKey(clientId)) {
            LOG.info("Connection already present. Not adding connection");
            return null;
        }
        MqttConnection connection = new MqttConnection(realm, clientId, password, new LinkedHashSet<>());
        connectionMap.put(clientId, connection);
        return connection;
    }

    protected MqttConnection getConnection(String clientId) {
        return connectionMap.get(clientId);
    }

    protected MqttConnection removeConnection(String clientId) {
        return connectionMap.remove(clientId);
    }
}
