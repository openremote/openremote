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
public class ParkingAsset extends Asset<ParkingAsset> {

    public static final AttributeDescriptor<Integer> SPACES_TOTAL = new AttributeDescriptor<>("spacesTotal", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> SPACES_OCCUPIED = new AttributeDescriptor<>("spacesOccupied", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> SPACES_OPEN = new AttributeDescriptor<>("spacesOpen", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> SPACES_BUFFER = new AttributeDescriptor<>("spacesBuffer", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Double> PRICE_HOURLY = new AttributeDescriptor<>("priceHourly", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR");
    public static final AttributeDescriptor<Double> PRICE_DAILY = new AttributeDescriptor<>("priceDaily", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR");

    public static final AssetDescriptor<ParkingAsset> DESCRIPTOR = new AssetDescriptor<>("parking", "0260ae", ParkingAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ParkingAsset() {
    }

    public ParkingAsset(String name) {
        super(name);
    }

    public Optional<Integer> getSpacesTotal() {
        return getAttributes().getValue(SPACES_TOTAL);
    }

    public ParkingAsset setSpacesTotal(Integer value) {
        getAttributes().getOrCreate(SPACES_TOTAL).setValue(value);
        return this;
    }

    public Optional<Integer> getSpacesOccupied() {
        return getAttributes().getValue(SPACES_OCCUPIED);
    }

    public ParkingAsset setSpacesOccupied(Integer value) {
        getAttributes().getOrCreate(SPACES_OCCUPIED).setValue(value);
        return this;
    }

    public Optional<Integer> getSpacesOpen() {
        return getAttributes().getValue(SPACES_OPEN);
    }

    public ParkingAsset setSpacesOpen(Integer value) {
        getAttributes().getOrCreate(SPACES_OPEN).setValue(value);
        return this;
    }

    public Optional<Integer> getSpacesBuffer() {
        return getAttributes().getValue(SPACES_BUFFER);
    }

    public ParkingAsset setSpacesBuffer(Integer value) {
        getAttributes().getOrCreate(SPACES_BUFFER).setValue(value);
        return this;
    }

    public Optional<Double> getPriceHourly() {
        return getAttributes().getValue(PRICE_HOURLY);
    }

    public ParkingAsset setPriceHourly(Double value) {
        getAttributes().getOrCreate(PRICE_HOURLY).setValue(value);
        return this;
    }

    public Optional<Double> getPriceDaily() {
        return getAttributes().getValue(PRICE_DAILY);
    }

    public ParkingAsset setPriceDaily(Double value) {
        getAttributes().getOrCreate(PRICE_DAILY).setValue(value);
        return this;
    }
}
