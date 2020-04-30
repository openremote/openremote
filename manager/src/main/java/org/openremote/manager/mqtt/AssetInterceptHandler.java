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

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssetInterceptHandler implements InterceptHandler {

    protected final ManagerIdentityService identityService;
    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;

    protected final Map<String, Set<String>> connectionAssets;

    AssetInterceptHandler(AssetStorageService assetStorageService,
                          AssetProcessingService assetProcessingService,
                          ManagerIdentityService managerIdentityService,
                          ManagerKeycloakIdentityProvider managerKeycloakIdentityProvider) {
        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        this.identityService = managerIdentityService;
        this.identityProvider = managerKeycloakIdentityProvider;
        connectionAssets = new HashMap<>();
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage interceptConnectMessage) {
        connectionAssets.put(interceptConnectMessage.getClientID(), new HashSet<>());

    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
        connectionAssets.remove(interceptDisconnectMessage.getClientID());
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {

    }

    @Override
    public void onPublish(InterceptPublishMessage message) {
        System.out.println("moquette mqtt broker message intercepted, topic: " + message.getTopicName()
            + ", content: " + new String(message.getPayload().array()));
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
        Set<String> attributeIds = connectionAssets.get(interceptSubscribeMessage.getClientID());
        if (attributeIds != null) {
            attributeIds.add(interceptSubscribeMessage.getTopicFilter());
        } else {
            throw new IllegalStateException("Connection with clientId " + interceptSubscribeMessage.getClientID() + " not found.");
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
        connectionAssets.remove(interceptUnsubscribeMessage.getClientID());
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {
    }

    public Map<String, Set<String>> getConnectionAssets() {
        return connectionAssets;
    }
}
