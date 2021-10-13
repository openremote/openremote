/*
 * Copyright 2021, OpenRemote Inc.
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
import io.moquette.interception.messages.*;
import org.openremote.container.security.AuthContext;
import org.openremote.model.Container;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

public class ClientProvisioningCustomHandler implements MQTTCustomHandler {

    private static final Logger LOG = SyslogCategory.getLogger(API, ClientProvisioningCustomHandler.class);
    public static final String TOP_LEVEL_TOPIC = "client";

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void onConnect(MqttConnection connection, InterceptConnectMessage msg) {

    }

    @Override
    public void onDisconnect(MqttConnection connection, InterceptDisconnectMessage msg) {

    }

    @Override
    public void onConnectionLost(MqttConnection connection, InterceptConnectionLostMessage msg) {

    }

    @Override
    public boolean canSubscribe(AuthContext authContext, MqttConnection connection, Topic topic) {
        if (!TOP_LEVEL_TOPIC.equals(topic.headToken().toString())) {
            return false;
        }

        // client/UNIQUE_ID/connect/response

//        topic.getTokens().size() ==
//        if (Arrays.stream(this.realms).noneMatch(realm -> realm.equals(connection.getRealm()))) {
//            LOG.info("realm mismatch");
//            return false;
//        }
//        return true;

        return false;
    }

    @Override
    public boolean canPublish(AuthContext authContext, MqttConnection connection, Topic topic) {
        return false;
    }

    @Override
    public boolean onSubscribe(MqttConnection connection, Topic topic, InterceptSubscribeMessage msg) {
        return false;
    }

    @Override
    public void onUnsubscribe(MqttConnection connection, Topic topic, InterceptUnsubscribeMessage msg) {

    }

    @Override
    public boolean onPublish(MqttConnection connection, Topic topic, InterceptPublishMessage msg) {
        return false;
    }
}
