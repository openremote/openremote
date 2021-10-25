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
package org.openremote.manager.provisioning;

import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import org.openremote.manager.mqtt.MQTTHandler;
import org.openremote.manager.mqtt.MqttConnection;
import org.openremote.model.syslog.SyslogCategory;

import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * This {@link MQTTHandler} is responsible for provisioning service users and assets and authenticating the client
 * against the configured {@link org.openremote.model.provisioning.ProvisioningConfig}s.
 */
public class UserAssetProvisioningMQTTHandler extends MQTTHandler {

    protected static final Logger LOG = SyslogCategory.getLogger(API, UserAssetProvisioningMQTTHandler.class);
    public static final String TOP_LEVEL_TOPIC = "provisioning";
    protected ProvisioningService provisioningService;

    @Override
    public boolean topicMatches(Topic topic) {
        return false;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean canSubscribe(MqttConnection connection, Topic topic) {
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
    public void doSubscribe(MqttConnection connection, Topic topic, InterceptSubscribeMessage msg) {

    }

    @Override
    public void doUnsubscribe(MqttConnection connection, Topic topic, InterceptUnsubscribeMessage msg) {

    }

    @Override
    public boolean canPublish(MqttConnection connection, Topic topic) {
        return false;
    }

    @Override
    public void doPublish(MqttConnection connection, Topic topic, InterceptPublishMessage msg) {

    }
}
