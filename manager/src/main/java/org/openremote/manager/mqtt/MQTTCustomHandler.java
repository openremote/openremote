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

/**
 * This allows custom handlers to be discovered by the {@link MqttBrokerService} during system startup using the
 * {@link java.util.ServiceLoader} mechanism. This allows topic(s) can be handled in a custom way. Any instances must
 * have a no-arg constructor.
 */
public interface MQTTCustomHandler {

    /**
     * Gets the priority of this handler which is used to determine the call order; handlers with a lower priority are
     * initialised and started first.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Provides a name to identify this custom handler for logging purposes etc.
     */
    String getName();

    /**
     * Called when the system starts to allow for initialisation.
     */
    void start(Container container) throws Exception;

    /**
     * Called when the system stops to allow for any cleanup.
     */
    void stop() throws Exception;

    /**
     * Should this handler be called to handle this topic; general security checks are performed before this handler
     * is called so this handler can focus on the required custom business logic. This handler should return null
     * if it doesn't want to allow/prevent the pub/sub.
     */
    boolean canSubscribe(AuthContext authContext, MqttConnection connection, Topic topic);

    boolean canPublish(AuthContext authContext, MqttConnection connection, Topic topic);

    /**
     * Will be called when any client connects
     */
    void onConnect(MqttConnection connection, InterceptConnectMessage msg);

    /**
     * Will be called when any client disconnects
     */
    void onDisconnect(MqttConnection connection, InterceptDisconnectMessage msg);

    /**
     * Will be called when any client loses connection
     */
    void onConnectionLost(MqttConnection connection, InterceptConnectionLostMessage msg);

    /**
     * Should return true if this handler handles the subscribe.
     */
    boolean onSubscribe(MqttConnection connection, Topic topic, InterceptSubscribeMessage msg);

    void onUnsubscribe(MqttConnection connection, Topic topic, InterceptUnsubscribeMessage msg);

    /**
     * Should return true if this handler handles the publish.
     */
    boolean onPublish(MqttConnection connection, Topic topic, InterceptPublishMessage msg);
}
