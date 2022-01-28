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

import static org.openremote.model.Constants.UNITS_KNOT;
import static org.openremote.model.Constants.UNITS_METRE;

@Entity
public class ShipAsset extends Asset<ShipAsset> {

    public static final AttributeDescriptor<Integer> MSSI_NUMBER = new AttributeDescriptor<>("MSSINumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setUseGrouping(false)));
    public static final AttributeDescriptor<Integer> IMO_NUMBER = new AttributeDescriptor<>("IMONumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setUseGrouping(false)));
    public static final AttributeDescriptor<Integer> ENI_NUMBER = new AttributeDescriptor<>("ENINumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setUseGrouping(false)));
    public static final AttributeDescriptor<Integer> DIRECTION = new AttributeDescriptor<>("direction", ValueType.DIRECTION);
    public static final AttributeDescriptor<Integer> LENGTH = new AttributeDescriptor<>("length", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_METRE);
    public static final AttributeDescriptor<Double> SPEED = new AttributeDescriptor<>("speed", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KNOT);
    public static final AttributeDescriptor<String> SHIP_TYPE = new AttributeDescriptor<>("shipType", ValueType.TEXT);

    public static final AssetDescriptor<ShipAsset> DESCRIPTOR = new AssetDescriptor<>("ferry", "000080", ShipAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ShipAsset() {
    }

    public ShipAsset(String name) {
        super(name);
    }

    public Optional<Integer> getMSSINumber() {
        return getAttributes().getValue(MSSI_NUMBER);
    }

    public ShipAsset setMSSINumber(Integer value) {
        getAttributes().getOrCreate(MSSI_NUMBER).setValue(value);
        return this;
    }

    public Optional<Integer> getIMONumber() {
        return getAttributes().getValue(IMO_NUMBER);
    }

    public ShipAsset setIMONumber(Integer value) {
        getAttributes().getOrCreate(IMO_NUMBER).setValue(value);
        return this;
    }

    public Optional<Integer> getENINumber() {
        return getAttributes().getValue(ENI_NUMBER);
    }

    public ShipAsset setENINumber(Integer value) {
        getAttributes().getOrCreate(ENI_NUMBER).setValue(value);
        return this;
    }

    public Optional<Integer> getDirection() {
        return getAttributes().getValue(DIRECTION);
    }

    public ShipAsset setDirection(Integer value) {
        getAttributes().getOrCreate(DIRECTION).setValue(value);
        return this;
    }

    public Optional<Integer> getLength() {
        return getAttributes().getValue(LENGTH);
    }

    public ShipAsset setLength(Integer value) {
        getAttributes().getOrCreate(LENGTH).setValue(value);
        return this;
    }

    public Optional<Double> getSpeed() {
        return getAttributes().getValue(SPEED);
    }

    public ShipAsset setSpeed(Double value) {
        getAttributes().getOrCreate(SPEED).setValue(value);
        return this;
    }

    public Optional<String> getShipType() {
        return getAttributes().getValue(SHIP_TYPE);
    }

    public ShipAsset setShipType(String value) {
        getAttributes().getOrCreate(SHIP_TYPE).setValue(value);
        return this;
    }
}
