/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.setup.clustering;

import org.openremote.model.asset.Asset;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.geo.GeoJSONPoint;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.model.syslog.SyslogCategory.DATA;
import static org.openremote.model.util.MapAccess.getInteger;
import static org.openremote.model.util.MapAccess.getString;

public class ManagerSetup extends org.openremote.manager.setup.ManagerSetup {
    public static final String OR_SETUP_ASSET_TYPES = "OR_SETUP_ASSET_TYPES";
    public static final String OR_SETUP_ASSETS = "OR_SETUP_ASSETS";

    private static final Logger LOG = SyslogCategory.getLogger(DATA, org.openremote.manager.setup.ManagerSetup.class);

    protected Container container;
    protected Executor executor;

    private final AtomicInteger createdAssets = new AtomicInteger(0);

    public ManagerSetup(Container container, Executor executor) {
        super(container);
        this.container = container;
        this.executor = executor;
    }

    private static float getRandomNumberInRange(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextFloat(max - min) + min;
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        String defaultTypes = "ThingAsset,LightAsset,RoomAsset,ThermostatAsset,ThermostatAsset";
        String types = getString(container.getConfig(), OR_SETUP_ASSET_TYPES, defaultTypes);
        int assetsPerType = getInteger(container.getConfig(), OR_SETUP_ASSETS, 10000);

        Reflections reflections = new Reflections("org.openremote.model.asset.impl");
        Set<Class<? extends Asset>> allAvailableClasses = reflections.getSubTypesOf(Asset.class);

        Map<String, Class<? extends Asset>> assetRegistry = allAvailableClasses.stream()
            .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
            .collect(Collectors.toMap(Class::getSimpleName, clazz -> clazz));

        String[] typesToLoad = types.split(",");

        for (String typeName : typesToLoad) {
            String cleanName = typeName.trim();
            Class<? extends Asset> clazz = assetRegistry.get(cleanName);

            if (clazz == null) {
                LOG.severe("Skipping unknown asset type: " + cleanName);
                continue;
            }

            // Skip asset types which either can't directly be initialized or are slow to init
            if (clazz.equals(UnknownAsset.class) || clazz.equals(GroupAsset.class) || clazz.equals(GatewayAsset.class)) continue;

            IntStream.rangeClosed(1, assetsPerType).forEach(i -> {
                executor.execute(() -> {
                    try {
                        createAsset(clazz, i);
                        createdAssets.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        }

        int expectedAssetCount = typesToLoad.length * assetsPerType;
        if (expectedAssetCount > 1) waitForAssetsToBeCreated(expectedAssetCount);
    }

    private void createAsset(Class<? extends Asset> clazz, int i) throws Exception {
        String name = clazz.getSimpleName() + i;
        Asset<?> asset = clazz.getConstructor(String.class).newInstance(name);

        asset.setRealm("master");
        asset.getAttributes().addOrReplace(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(
                getRandomNumberInRange(4.24f, 4.51f), // West to East
                getRandomNumberInRange(51.89f, 51.99f) // South to North
            ))
        );

        asset.setId(UniqueIdentifierGenerator.generateId(name));
        assetStorageService.merge(asset);
    }

    private void waitForAssetsToBeCreated(int expectedAssetCount) throws Exception {
        int minAssetsPerSecond = 150;
        int waitCounter = 0;
        int waitDuration = 10;
        int waitLimit = Math.max(waitDuration, expectedAssetCount / minAssetsPerSecond);
        while (createdAssets.get() < expectedAssetCount) {
            if (waitCounter * waitDuration > waitLimit) {
                throw new IllegalStateException("Failed to provision all requested assets in the specified time (" + waitLimit + " seconds).");
            }
            waitCounter++;
            Thread.sleep(waitDuration * 1000);
        }
    }
}
