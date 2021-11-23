package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.syslog.SyslogCategory;

import java.util.logging.Logger;

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
        String realm = null;

        if (username != null) {
            String[] realmAndUsername = username.split(":");
            realm = realmAndUsername[0];
            username = realmAndUsername[1];
        }
        return verifyRights(topic, clientId, realm, username, true);
    }

    @Override
    public boolean canRead(Topic topic, String username, String clientId) {
        String realm = null;

        if (username != null) {
            String[] realmAndUsername = username.split(":");
            realm = realmAndUsername[0];
            username = realmAndUsername[1];
        }
        return verifyRights(topic, clientId, realm, username, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean verifyRights(Topic topic, String clientId, String realm, String username, boolean isWrite) {
        MqttConnection connection = brokerService.clientIdConnectionMap.get(clientId);
        int i=0;

        try {
            while (connection == null && i<10) {
                // The connection handler gets called after this quite often so try and wait until the connection is registered
                connection = brokerService.clientIdConnectionMap.get(clientId);
                i++;
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            LOG.finer("Interrupted whilst waiting for connection to be initialised: clientId=" + clientId);
        }

        if (connection == null) {
            LOG.warning("No connection found: clientId=" + clientId);
            return false;
        }

        // See if a custom handler wants to handle this topic pub/sub
        for (MQTTHandler handler : brokerService.getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Passing topic to handler for " + (isWrite ? "pub" : "sub") + ": handler=" + handler.getName() + ", topic=" + topic + ", connection=" + connection);
                boolean result;

                if (isWrite) {
                    result = handler.checkCanPublish(connection, topic);
                } else {
                    result = handler.checkCanSubscribe(connection, topic);
                }
                return result;
            }
        }

        LOG.fine("No handler has allowed " + (isWrite ? "pub" : "sub") + ": topic=" + topic + ", connection=" + connection);
        return false;
    }
}

