/*
 * Copyright 2022, OpenRemote Inc.
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

import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * This handler supports last will publishes and subscriptions; the publish is verified at connect time as normal
 * {@link #canPublish} method isn't called for last will publishes. Clients can subscribe to any last will message so
 * the {@link org.openremote.model.security.ClientRole#READ_LAST_WILL} should be used sparingly.
 * Topic format is `{REALM}/lastwill/{IDENTIFIER}` the IDENTIFIER can be any string and wildcard subscriptions are allowed.
 */
// TODO: Introduce role for read/write last will
public class LastWillMQTTHandler extends MQTTHandler {

    public static final int PRIORITY = DefaultMQTTHandler.PRIORITY + 1000;
    public static final String LAST_WILL_TOKEN = "last_will";
    private static final Logger LOG = SyslogCategory.getLogger(API, LastWillMQTTHandler.class);

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        mqttBrokerService = container.getService(MqttBrokerService.class);
    }

    @Override
    public boolean handlesTopic(Topic topic) {
        // Skip standard checks
        return topicMatches(topic);
    }

    @Override
    protected boolean topicMatches(Topic topic) {
        return isLastWillTopic(topic);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean onConnect(MqttConnection connection, InterceptConnectMessage msg) {
        super.onConnect(connection, msg);

        // Force disconnect if last will topic is in a different realm
        if (msg.isWillFlag()) {
            Topic topic = TextUtil.isNullOrEmpty(msg.getWillTopic()) ? null : Topic.asTopic(msg.getWillTopic());
            if (topic == null || !isLastWillTopic(topic)) {
                getLogger().warning("Last will topic must be in the format `{REALM}/" + LAST_WILL_TOKEN + "/IDENTIFIER' client will be disconnected: " + connection);
                Runnable closeRunnable = mqttBrokerService.getForceDisconnectRunnable(connection.getClientId());
                closeRunnable.run();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkCanSubscribe(MqttConnection connection, Topic topic) {
        if (connection.getAuthContext() == null) {
            getLogger().fine("Anonymous connection subscriptions not supported by this handler, topic=" + topic + ", connection" + connection);
            return false;
        }
        if (!topicRealmMatchesConnection(connection, topic)) {
            getLogger().fine("Topic realm token must match the connection, topic=" + topic + ", connection" + connection);
            return false;
        }
        if (!canSubscribe(connection, topic)) {
            getLogger().fine("Cannot subscribe to this topic, topic=" + topic + ", connection" + connection);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkCanPublish(MqttConnection connection, Topic topic) {
        // Last will publish doesn't reach here so we verify it at the connect stage and we just disallow client side
        // publishes using this handler
        return false;
    }

    @Override
    public boolean canSubscribe(MqttConnection connection, Topic topic) {
        return isLastWillTopic(topic) && connection.getAuthContext().hasResourceRole(ClientRole.READ_LAST_WILL.getValue(), Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public boolean canPublish(MqttConnection connection, Topic topic) {
        return false;
    }

    @Override
    public void doSubscribe(MqttConnection connection, Topic topic, InterceptSubscribeMessage msg) {
        // Nothing to do here - standard broker routing between subscribers and publishers
    }

    @Override
    public void doUnsubscribe(MqttConnection connection, Topic topic, InterceptUnsubscribeMessage msg) {
        // Nothing to do here - standard broker routing between subscribers and publishers
    }

    @Override
    public void doPublish(MqttConnection connection, Topic topic, InterceptPublishMessage msg) {
        // Nothing to do here - standard broker routing between subscribers and publishers
    }

    protected static boolean isLastWillTopic(Topic topic) {
        return LAST_WILL_TOKEN.equals(topicTokenIndexToString(topic, 1))
            && topic.getTokens().size() == 3;
    }
}
