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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.geo.GeoJSONPoint;

import java.util.ArrayList;
import java.util.List;
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
        int devicesAmount = getInteger(container.getConfig(), OR_SETUP_DEVICES, 10);
        int parentsAmount = getInteger(container.getConfig(), OR_SETUP_GROUPS, 2);
        LOG.info("Starting load2 Manager setup with " + devicesAmount + " devices, spread over " + parentsAmount + " parents.");

        // Create parents
        List<Asset<?>> parentAssets = buildParentAssets(parentsAmount, keycloakSetup.realmOne.getName());

        // Create devices
        if(!parentAssets.isEmpty()) {
            buildDeviceAssets(devicesAmount, keycloakSetup.realmOne.getName(), parentAssets);
        } else {
            buildDeviceAssets(devicesAmount, keycloakSetup.realmOne.getName());
        }
        LOG.info("Finished with the load2 Manager setup.");
    }

    /**
     * Creates an X amount of {@link CityAsset}, and inserts them into the root level.
     * @param amount Number of parent {@link CityAsset} to generate
     * @return List of generated {@link CityAsset}
     */
    protected List<Asset<?>> buildParentAssets(int amount, String realm) {
        List<Asset<?>> parentAssets = new ArrayList<>();
        for(int i = 0; i < amount; i++) {
            CityAsset cityAsset = new CityAsset("City #" + i);
            cityAsset.setRealm(realm);
            cityAsset = assetStorageService.merge(cityAsset);
            parentAssets.add(cityAsset);
        }
        return parentAssets;
    }

    /**
     * Creates an X amount of {@link ElectricityChargerAsset}, and inserts them as a child under a parent asset.
     * It will equally spread the number of assets across the given parent assets.
     * (100 assets with 5 parents will generate 20 assets for each parent)
     *
     * @param amount Number of {@link ElectricityChargerAsset} to generate
     * @param realm Realm to insert the generated assets into
     * @param parents Parent assets to insert the generated assets as children
     */
    protected void buildDeviceAssets(int amount, String realm, List<Asset<?>> parents) {
        int childrenAmount = amount / parents.size();
        for (Asset<?> parent : parents) {
            buildDeviceAssets(childrenAmount, realm, parent);
        }
    }

    /**
     * Creates an X amount of {@link ElectricityChargerAsset}, and inserts them as a child under the given parent asset.
     * @param amount Number of {@link ElectricityChargerAsset} to generate
     * @param realm Realm to insert the generated assets into
     * @param parentAsset Parent asset to insert the generated assets as children
     */
    protected void buildDeviceAssets(int amount, String realm, Asset<?> parentAsset) {
        for(int i = 0; i < amount; i++) {
            ElectricityChargerAsset chargerAsset = new ElectricityChargerAsset("Charger #" + i);
            chargerAsset.setRealm(realm);
            chargerAsset.setParent(parentAsset);
            double x = 4.242 + Math.random() * (4.505 - 4.242);
            double y = 51.890 + Math.random() * (51.988 - 51.890);
            chargerAsset.getAttributes().addOrReplace(
                    new Attribute<>(ElectricityChargerAsset.ENERGY_LEVEL_PERCENTAGE, 70),
                    new Attribute<>(ElectricityChargerAsset.LOCATION, new GeoJSONPoint(x, y))
            );
            assetStorageService.merge(chargerAsset);
        }
    }

    /**
     * Creates an X amount of {@link ElectricityChargerAsset}, and inserts them in the database on root level.
     * @param amount Number of {@link ElectricityChargerAsset} to generate
     * @param realm Realm to insert the generated assets into
     */
    protected void buildDeviceAssets(int amount, String realm) {
        buildDeviceAssets(amount, realm, (Asset<?>) null);
    }
}
