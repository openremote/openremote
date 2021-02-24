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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class PeopleCounterAsset extends Asset<PeopleCounterAsset> {

    public static final AttributeDescriptor<Integer> COUNT_IN = new AttributeDescriptor<>("countIn", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> COUNT_OUT = new AttributeDescriptor<>("countOut", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> COUNT_TOTAL = new AttributeDescriptor<>("countTotal", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> COUNT_IN_PER_MINUTE = new AttributeDescriptor<>("countInMinute", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> COUNT_OUT_PER_MINUTE = new AttributeDescriptor<>("countOutMinute", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Double> COUNT_GROWTH_PER_MINUTE = new AttributeDescriptor<>("countGrowthMinute", ValueType.NUMBER);

    public static final AssetDescriptor<PeopleCounterAsset> DESCRIPTOR = new AssetDescriptor<>("account-multiple", "4b5966", PeopleCounterAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected PeopleCounterAsset() {
    }

    public PeopleCounterAsset(String name) {
        super(name);
    }

    public Optional<Integer> getCountIn() {
        return getAttributes().getValue(COUNT_IN);
    }

    public Optional<Integer> getCountOut() {
        return getAttributes().getValue(COUNT_OUT);
    }

    public Optional<Integer> getCountInMinute() {
        return getAttributes().getValue(COUNT_IN_PER_MINUTE);
    }

    public Optional<Integer> getCountOutMinute() {
        return getAttributes().getValue(COUNT_OUT_PER_MINUTE);
    }

    public Optional<Integer> getCountTotal() {
        return getAttributes().getValue(COUNT_TOTAL);
    }

    public Optional<Double> getCountGrowthMinute() {
        return getAttributes().getValue(COUNT_GROWTH_PER_MINUTE);
    }
}
