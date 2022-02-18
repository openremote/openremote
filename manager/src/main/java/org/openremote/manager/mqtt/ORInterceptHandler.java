/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.mqtt;

import com.google.api.client.util.Charsets;
import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.syslog.SyslogCategory;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

public class ORInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = SyslogCategory.getLogger(API, ORInterceptHandler.class);
    protected final MqttBrokerService brokerService;
    protected final MessageBrokerService messageBrokerService;
    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final TimerService timerService;
    protected final WeakHashMap<String, MqttConnection> disconnectedConnections = new WeakHashMap<>();

    public ORInterceptHandler(MqttBrokerService brokerService,
                              ManagerKeycloakIdentityProvider identityProvider,
                              MessageBrokerService messageBrokerService,
                              TimerService timerService) {
        this.brokerService = brokerService;
        this.identityProvider = identityProvider;
        this.messageBrokerService = messageBrokerService;
        this.timerService = timerService;
    }

    @Override
    public String getID() {
        return ORInterceptHandler.class.getName();
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return new Class[]{
                InterceptConnectMessage.class,
                InterceptDisconnectMessage.class,
                InterceptConnectionLostMessage.class,
                InterceptSubscribeMessage.class,
                InterceptUnsubscribeMessage.class,
                InterceptPublishMessage.class
        };
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        String realm = null;
        String username = null;
        String password = msg.isPasswordFlag() ? new String(msg.getPassword(), Charsets.UTF_8) : null;

        if (msg.getUsername() != null) {
            String[] realmAndUsername = msg.getUsername().split(":");
            realm = realmAndUsername[0];
            username = realmAndUsername[1];
        }

        MqttConnection connection = new MqttConnection(identityProvider, msg.getClientID(), realm, username, password, msg.isCleanSession(), timerService.getCurrentTimeMillis());
        brokerService.addConnection(connection.getClientId(), connection);

        // Notify all custom handlers
        brokerService.getCustomHandlers().forEach(customHandler -> customHandler.onConnect(connection, msg));
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        MqttConnection connection;
        synchronized (disconnectedConnections) {
            connection = disconnectedConnections.remove(msg.getClientID());
        }

        if (connection != null) {
            // No topic info here so just notify all custom handlers
            MqttConnection finalConnection = connection;
            brokerService.getCustomHandlers().forEach(customHandler -> customHandler.onDisconnect(finalConnection, msg));
        } else {
            connection = brokerService.clearConnectionSession(msg.getClientID());

            // Store weak reference to connection for onConnectionLost
            synchronized (disconnectedConnections) {
                disconnectedConnections.put(msg.getClientID(), connection);
            }
        }
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        // This can fire after onConnect which causes the connection to be removed; seems that onDisconnect also gets
        // called when connection is lost and that is called first and before onConnect so we'll just use that for now
        MqttConnection connection;
        synchronized (disconnectedConnections) {
            connection = disconnectedConnections.remove(msg.getClientID());
        }

        if (connection != null) {
            // No topic info here so just notify all custom handlers
            MqttConnection finalConnection = connection;
            brokerService.getCustomHandlers().forEach(customHandler -> customHandler.onConnectionLost(finalConnection, msg));
        } else {
            connection = brokerService.clearConnectionSession(msg.getClientID());

            // Store weak reference to connection for onDisconnect
            synchronized (disconnectedConnections) {
                disconnectedConnections.put(msg.getClientID(), connection);
            }
        }
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {

        MqttConnection connection = brokerService.getConnection(msg.getClientID());

        if (connection == null) {
            LOG.info("No connection found: clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicFilter());

        for (MQTTHandler handler : brokerService.getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Handler has handled subscribe: handler=" + handler.getName() + ", topic=" + topic + ", connection=" + connection);
                handler.doSubscribe(connection, topic, msg);
                break;
            }
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {

        MqttConnection connection = brokerService.getConnection(msg.getClientID());

        if (connection == null) {
            LOG.info("No connection found: clientID=" + msg.getClientID());
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicFilter());

        for (MQTTHandler handler : brokerService.getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Handler has handled unsubscribe: handler=" + handler.getName() + ", topic=" + topic + ", connection=" + connection);
                handler.doUnsubscribe(connection, topic, msg);
                break;
            }
        }
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        String realm = null;
        String username = null;

        if (msg.getUsername() != null) {
            String[] realmAndUsername = msg.getUsername().split(":");
            realm = realmAndUsername[0];
            username = realmAndUsername[1];
        }

        MqttConnection connection = brokerService.getConnection(msg.getClientID());

        if (connection == null) {
            LOG.warning("No connection found: clientID=" + msg.getClientID() + ", realm=" + realm + ", username=" + username);
            return;
        }

        Topic topic = Topic.asTopic(msg.getTopicName());

        for (MQTTHandler handler : brokerService.getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.fine("Handler has handled publish: handler=" + handler.getName() + ", topic=" + topic + ", connection=" + connection);
                handler.doPublish(connection, topic, msg);
                break;
            }
        }
    }
}
