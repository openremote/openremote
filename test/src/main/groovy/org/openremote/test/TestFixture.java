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

import org.openremote.container.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.rules.Ruleset;
import org.spockframework.runtime.extension.AbstractGlobalExtension;

import java.util.List;

/**
 * Used to store state for tests to help improve test performance
 */
public class TestFixture extends AbstractGlobalExtension {

    // Store the container here to allow stopping it after all tests run
    protected static Container container;
    public static List<Ruleset> globalRulesets;
    public static List<Ruleset> tenantRulesets;
    public static List<Ruleset> assetRulesets;
    public static List<Asset> assets;
    public static List<UserAsset> userAssets;

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
