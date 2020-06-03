package org.openremote.manager.mqtt;

import org.openremote.model.attribute.AttributeRef;

import java.util.HashMap;
import java.util.Map;

import static org.openremote.manager.mqtt.KeycloakAuthenticator.MQTT_CLIENT_ID_SEPARATOR;

public class MqttConnection {

    protected final String realm;
    protected final String clientId;
    protected final String username;
    protected final byte[] password;
    protected final Map<String, String> assetSubscriptions;
    protected final Map<AttributeRef, String> assetAttributeSubscriptions;
    protected final Map<AttributeRef, String> assetAttributeValueSubscriptions;
    protected String accessToken;
    protected int subscriptionId;

    public MqttConnection(String clientId, String username, byte[] password) {
        int indexSplit = clientId.indexOf(MQTT_CLIENT_ID_SEPARATOR);
        if (indexSplit > 0) {
            realm = clientId.substring(0, indexSplit);
        } else {
            realm = clientId;
        }
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.assetSubscriptions = new HashMap<>();
        this.assetAttributeSubscriptions = new HashMap<>();
        assetAttributeValueSubscriptions = new HashMap<>();
        this.subscriptionId = 0;
    }

    public int getNextSubscriptionId() {
        return ++subscriptionId;
    }
}
