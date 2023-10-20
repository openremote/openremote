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
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.*;

import jakarta.persistence.Entity;

@Entity
public class HarvestRobotAsset extends Asset<HarvestRobotAsset> {

        public enum OperationMode {
                MOVING,
                SCANNING,
                CUTTING,
                UNLOADING,
                CHARGING
        }
        public static final ValueDescriptor<OperationMode> OPERATION_MODE_VALUE = new ValueDescriptor<>("operationMode",
                        OperationMode.class);
        public static final AttributeDescriptor<OperationMode> OPERATION_MODE = new AttributeDescriptor<>(
                        "operationMode", OPERATION_MODE_VALUE);

        public enum VegetableType {
                TOMATO,
                CUCUMBER,
                BELL_PEPPER
        }
        public static final ValueDescriptor<VegetableType> VEGETABLE_TYPE_VALUE = new ValueDescriptor<>("vegetableType",
                        VegetableType.class);
        public static final AttributeDescriptor<VegetableType> VEGETABLE_TYPE = new AttributeDescriptor<>(
                        "vegetableType", VEGETABLE_TYPE_VALUE);

        public static final AttributeDescriptor<Integer> DIRECTION = new AttributeDescriptor<>("direction",
                ValueType.DIRECTION);
        public static final AttributeDescriptor<Double> SPEED = new AttributeDescriptor<>("speed",
                ValueType.POSITIVE_NUMBER)
                .withUnits(UNITS_KILO, UNITS_METRE, UNITS_PER, UNITS_HOUR);

        public static final AttributeDescriptor<Integer> HARVESTED_SESSION = new AttributeDescriptor<>("harvestedSession",
                ValueType.POSITIVE_INTEGER);
        public static final AttributeDescriptor<Integer> HARVESTED_TOTAL = new AttributeDescriptor<>("harvestedTotal",
                ValueType.POSITIVE_INTEGER);

    public static final AssetDescriptor<HarvestRobotAsset> DESCRIPTOR = new AssetDescriptor<>("robot-industrial", "38761d", HarvestRobotAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected HarvestRobotAsset() {
    }

    public HarvestRobotAsset(String name) {
        super(name);
    }

}
