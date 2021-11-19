/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.test;

import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.container.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.security.Role;
import org.openremote.model.security.User;
import org.openremote.model.util.Pair;
import org.spockframework.runtime.extension.AbstractGlobalExtension;

import java.util.List;
import java.util.Map;

/**
 * Used to store state for tests to help improve test performance
 */
public class TestFixture extends AbstractGlobalExtension {

    // Store the container here to allow stopping it after all tests run
    protected static Container container;
    public static List<GlobalRuleset> globalRulesets;
    public static List<TenantRuleset> tenantRulesets;
    public static List<AssetRuleset> assetRulesets;
    public static List<GatewayConnection> gatewayConnections;
    public static List<Asset<?>> assets;
    public static List<UserAssetLink> userAssets;
    public static List<User> users;

    @Override
    public void start() {
        // Force RECONNECT times to be short to improve test run times
        AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS = 50;
        AbstractNettyIOClient.RECONNECT_DELAY_JITTER_MILLIS = 0;
        AbstractNettyIOClient.RECONNECT_DELAY_MAX_MILLIS = 50;
        super.start();
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
