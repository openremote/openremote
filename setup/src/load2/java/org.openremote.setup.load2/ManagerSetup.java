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
package org.openremote.setup.load2;


import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.CityAsset;
import org.openremote.model.asset.impl.ElectricityChargerAsset;
import org.openremote.model.asset.impl.LightAsset;
import org.openremote.model.attribute.Attribute;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;

public class ManagerSetup extends org.openremote.manager.setup.ManagerSetup {
    public static final String OR_SETUP_DEVICES = "OR_SETUP_DEVICES";
    public static final String OR_SETUP_GROUPS = "OR_SETUP_GROUPS";
    final protected AssetStorageService assetStorageService;
    protected Container container;
    protected Executor executor;

    private static final Logger LOG = Logger.getLogger(ManagerSetup.class.getName());

    public ManagerSetup(Container container, Executor executor) {
        super(container);
        this.container = container;
        this.executor = executor;
        this.assetStorageService = container.getService(AssetStorageService.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onStart() throws Exception {
        KeycloakSetup keycloakSetup = setupService.getTaskOfType(KeycloakSetup.class);
        int devices = getInteger(container.getConfig(), OR_SETUP_DEVICES, 10);
        int groups = getInteger(container.getConfig(), OR_SETUP_GROUPS, 2);
        int devicesPerGroup = devices / groups;
        LOG.info("Starting load2 Manager setup with " + devices + " devices, spread over " + groups + " groups.");

        // Create groups
        ArrayList<Asset<?>> groupAssets = this.buildGroupAssets(groups, keycloakSetup.realmOne.getName());

        // Create devices
        if(!groupAssets.isEmpty()) {
            this.buildDeviceAssets(devicesPerGroup, keycloakSetup.realmOne.getName(), groupAssets);
        } else {
            this.buildDeviceAssets(devices, keycloakSetup.realmOne.getName());
        }
        LOG.info("Finished with the load2 Manager setup.");
    }

    /**
     * TODO
     *
     * @param amount
     * @return
     * @throws Exception
     */
    protected ArrayList<Asset<?>> buildGroupAssets(int amount, String realm) throws Exception {
        ArrayList<Asset<?>> groupAssets = new ArrayList<>();
        for(int i = 0; i < amount; i++) {
            CityAsset groupAsset = new CityAsset("City #" + i);
            groupAsset.setRealm(realm);
            groupAsset = assetStorageService.merge(groupAsset);
            groupAssets.add(groupAsset);
        }
        /*AtomicInteger createdAssets = new AtomicInteger(0);
        ArrayList<Asset<?>> groupAssets = new ArrayList<>();
        if (amount > 0) {
            IntStream.rangeClosed(0, amount).forEach(i -> {
                executor.execute(() -> {
                    // Build group asset
                    CityAsset groupAsset = new CityAsset("City #" + i);
                    groupAsset.setRealm(realm);
                    groupAsset = assetStorageService.merge(groupAsset);
                    groupAssets.add(groupAsset);
                    createdAssets.incrementAndGet();
                });
            });
        }
        // Wait until all assets created
        int waitCounter = 0;
        while (createdAssets.get() < amount) {
            if (waitCounter > 200) {
                throw new IllegalStateException("Failed to create all requested group assets in the specified time");
            }
            waitCounter++;
            Thread.sleep(10000);
        }*/
        return groupAssets;
    }

    /**
     * TODO
     *
     * @param amountPerGroup
     * @param realm
     * @param parents
     */
    protected void buildDeviceAssets(int amountPerGroup, String realm, ArrayList<Asset<?>> parents) throws Exception {
        for(int i = 0; i < parents.size(); i++) {
            buildDeviceAssets(amountPerGroup, realm, parents.get(i));
        }
        /*IntStream.rangeClosed(1, parents.size()).forEach(i -> {
            try {
                buildDeviceAssets(amountPerGroup, realm, parents.get(i));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });*/
    }

    /**
     * TODO
     *
     * @param amount
     * @param realm
     * @param parentAsset
     */
    protected void buildDeviceAssets(int amount, String realm, Asset<?> parentAsset) throws Exception {
        for(int i = 0; i < amount; i++) {
            ElectricityChargerAsset chargerAsset = new ElectricityChargerAsset("Charger #" + i);
            chargerAsset.setRealm(realm);
            chargerAsset.setParent(parentAsset);
            chargerAsset.getAttributes().addOrReplace(
                    new Attribute<>(ElectricityChargerAsset.ENERGY_LEVEL_PERCENTAGE, 70)
            );
            assetStorageService.merge(chargerAsset);
        }
        /*AtomicInteger createdAssets = new AtomicInteger(0);
        IntStream.rangeClosed(1, amount).forEach(i -> {
            executor.execute(() -> {
                // Build light asset
                LightAsset lightAsset = new LightAsset("Light #" + i);
                lightAsset.setRealm(realm);
                lightAsset.setParent(parentAsset);
                lightAsset.getAttributes().addOrReplace(
                        new Attribute<>(LightAsset.BRIGHTNESS, 30)
                );
                assetStorageService.merge(lightAsset);
                createdAssets.incrementAndGet();
            });
        });
        // Wait until all assets created
        int waitCounter = 0;
        while (createdAssets.get() < amount) {
            if (waitCounter > 200) {
                throw new IllegalStateException("Failed to create all requested assets in the specified time");
            }
            waitCounter++;
            Thread.sleep(10000);
        }*/
    }

    /**
     * TODO
     *
     * @param amount
     * @param realm
     */
    protected void buildDeviceAssets(int amount, String realm) {
        for(int i = 0; i < amount; i++) {
            LightAsset lightAsset = new LightAsset("Light #" + i);
            lightAsset.setRealm(realm);
            lightAsset.getAttributes().addOrReplace(
                    new Attribute<>(LightAsset.BRIGHTNESS, 30)
            );
            assetStorageService.merge(lightAsset);
        }
        /*IntStream.rangeClosed(1, amount).forEach(i -> {
            executor.execute(() -> {
                // Build light asset
                LightAsset lightAsset = new LightAsset("Light #" + i);
                lightAsset.setRealm(realm);
                lightAsset.getAttributes().addOrReplace(
                        new Attribute<>(LightAsset.BRIGHTNESS, 30)
                );
                assetStorageService.merge(lightAsset);
            });
        });*/
    }
}
