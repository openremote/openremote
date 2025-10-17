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
package org.openremote.setup.clustering;

import org.openremote.model.asset.Asset;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.asset.impl.*;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.geo.GeoJSONPoint;
import java.util.Random;

public class ManagerClusteringSetup extends ManagerSetup {

    public ManagerClusteringSetup(Container container) {
        super(container);
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

        for (var i = 0; i < 12_000; i++) {
            ThingAsset thing = new ThingAsset("Thing" + i);
            thing.setRealm("master");
            thing.getAttributes().addOrReplace(
                    // Rotterdam bb coords
                    // 51.89 // South
                    // 51.99 // North
                    // 4.24  // West
                    // 4.51  // East
                    new Attribute<>(Asset.LOCATION, new GeoJSONPoint(getRandomNumberInRange(4.24f, 4.51f), getRandomNumberInRange(51.89f, 51.99f)))
            );
            thing.setId(UniqueIdentifierGenerator.generateId(thing.getName()));
            assetStorageService.merge(thing);
        }
    }
}
