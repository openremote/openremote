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
package org.openremote.model.asset.impl;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class VentilationAsset extends Asset<VentilationAsset> {

    public static final AttributeDescriptor<Integer> FAN_SPEED = new AttributeDescriptor<>("fanSpeed", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Double> FLOW = new AttributeDescriptor<>("flow", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY))
        .withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_HOUR);

    public static final AssetDescriptor<VentilationAsset> DESCRIPTOR = new AssetDescriptor<>("fan", "0b99c3", VentilationAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected VentilationAsset() {
    }

    public VentilationAsset(String name) {
        super(name);
    }

    public Optional<Double> getFlow() {
        return getAttributes().getValue(FLOW);
    }

    public Optional<Integer> getFanSpeed() {
        return getAttributes().getValue(FAN_SPEED);
    }
}
