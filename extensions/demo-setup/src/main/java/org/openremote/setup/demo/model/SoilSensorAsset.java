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
package org.openremote.setup.demo.model;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

import static org.openremote.model.Constants.*;

@Entity
public class SoilSensorAsset extends Asset<SoilSensorAsset> {

    public static final AttributeDescriptor<Integer> SOIL_TENSION_MEASURED = new AttributeDescriptor<>("soilTensionMeasured",
            ValueType.POSITIVE_INTEGER)
            .withUnits(UNITS_KILO, UNITS_PASCAL);
    public static final AttributeDescriptor<Integer> SOIL_TENSION_MIN = new AttributeDescriptor<>("soilTensionMin",
            ValueType.POSITIVE_INTEGER)
            .withUnits(UNITS_KILO, UNITS_PASCAL)
            .withConstraints(new ValueConstraint.Min(0),new ValueConstraint.Max(80));
    public static final AttributeDescriptor<Integer> SOIL_TENSION_MAX = new AttributeDescriptor<>("soilTensionMax",
            ValueType.POSITIVE_INTEGER)
            .withUnits(UNITS_KILO, UNITS_PASCAL)
            .withConstraints(new ValueConstraint.Min(0),new ValueConstraint.Max(80));
    public static final AttributeDescriptor<Double> TEMPERATURE = new AttributeDescriptor<>("temperature",
            ValueType.NUMBER)
            .withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> SALINITY = new AttributeDescriptor<>("salinity",
            ValueType.NUMBER);

    public static final AssetDescriptor<SoilSensorAsset> DESCRIPTOR = new AssetDescriptor<>("water-percent", "993333", SoilSensorAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected SoilSensorAsset() {
    }

    public SoilSensorAsset(String name) {
        super(name);
    }

}
