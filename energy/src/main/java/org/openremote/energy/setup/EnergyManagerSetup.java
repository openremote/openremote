/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.energy.setup;

import org.openremote.energy.asset.ElectricityBatteryAsset;
import org.openremote.energy.asset.ElectricityChargerAsset;
import org.openremote.energy.asset.ElectricityConsumerAsset;
import org.openremote.energy.asset.ElectricityProducerSolarAsset;
import org.openremote.energy.asset.ElectricityStorageAsset;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.geo.GeoJSONPoint;

public class EnergyManagerSetup extends ManagerSetup {

    public EnergyManagerSetup(Container container) {
        super(container);
    }

    protected ElectricityStorageAsset createDemoElectricityStorageAsset(String name, Asset<?> area,
                                                                        GeoJSONPoint location) {
        ElectricityStorageAsset electricityStorageAsset = new ElectricityBatteryAsset(name);
        electricityStorageAsset.setParent(area);
        electricityStorageAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityStorageAsset;
    }

    protected ElectricityProducerSolarAsset createDemoElectricitySolarProducerAsset(String name, Asset<?> area,
                                                                                    GeoJSONPoint location) {
        ElectricityProducerSolarAsset electricityProducerAsset = new ElectricityProducerSolarAsset(name);
        electricityProducerAsset.setParent(area);
        electricityProducerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityProducerAsset;
    }

    protected ElectricityConsumerAsset createDemoElectricityConsumerAsset(String name, Asset<?> area,
                                                                          GeoJSONPoint location) {
        ElectricityConsumerAsset electricityConsumerAsset = new ElectricityConsumerAsset(name);
        electricityConsumerAsset.setParent(area);
        electricityConsumerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityConsumerAsset;
    }

    protected ElectricityChargerAsset createDemoElectricityChargerAsset(String name, Asset<?> area,
                                                                        GeoJSONPoint location) {
        ElectricityChargerAsset electricityChargerAsset = new ElectricityChargerAsset(name);
        electricityChargerAsset.setParent(area);
        electricityChargerAsset.getAttributes().addOrReplace(new Attribute<>(Asset.LOCATION, location));

        return electricityChargerAsset;
    }
}
