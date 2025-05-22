/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.custom;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.ElectricVehicleAsset;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueDescriptor;

import jakarta.persistence.Entity;
import org.openremote.model.value.ValueType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.Constants.UNITS_HOUR;

/**
 * This is an example of a custom {@link Asset} type; this must be registered via an
 * {@link org.openremote.model.AssetModelProvider} and must conform to the following requirements:
 *
 * <ul>
 * <li>Must have {@link Entity} annotation
 * <li>Optionally add {@link ValueDescriptor}s
 * <li>Optionally add {@link org.openremote.model.value.MetaItemDescriptor}s
 * <li>Optionally add {@link AttributeDescriptor}s
 * <li>Must have a public static final {@link AssetDescriptor}
 * <li>Must have a protected no args constructor (for hydrators i.e. JPA/Jackson)
 * <li>For a given {@link Asset} type only one {@link AssetDescriptor} can exist
 * <li>{@link AttributeDescriptor}s that override a super class descriptor cannot change the
 * value type; just the formatting etc.
 * <li>{@link org.openremote.model.value.MetaItemDescriptor}s names must be unique
 * <li>{@link ValueDescriptor}s names must be unique
 * </ul>
 */
@Entity
public class LidarAsset extends Asset<LidarAsset> {
    public static final AssetDescriptor<LidarAsset> lidarAssetAssetDescriptor = new AssetDescriptor<>("eye-circle", "00aaaa", LidarAsset.class);



    public enum VehicleType {
        VEHICLE,
        LARGE_VEHICLE
    }

    public static final ValueDescriptor<VehicleType> VEHICLE_TYPE_VALUE = new ValueDescriptor<>("vehicleType", VehicleType.class);

    public static final AttributeDescriptor<VehicleType> VEHICLE_TYPE = new AttributeDescriptor<>("vehicleType",   VEHICLE_TYPE_VALUE);

    public static final AttributeDescriptor<String> vendor      = new AttributeDescriptor<>("Vendor",           ValueType.TEXT);
    public static final AttributeDescriptor<String> thing        = new AttributeDescriptor<>("Type",             ValueType.TEXT);
    public static final AttributeDescriptor<String> sensor      = new AttributeDescriptor<>("Sensor",           ValueType.TEXT);
    public static final AttributeDescriptor<Date>   dateTime    = new AttributeDescriptor<>("Dato",             ValueType.DATE_AND_TIME);
    public static final AttributeDescriptor<Integer> HEADING = new AttributeDescriptor<>("Retning", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_DEGREE);
    public static final AttributeDescriptor<Integer> LONGITUDE = new AttributeDescriptor<>("Breddegrad", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_DEGREE);
    public static final AttributeDescriptor<Integer> LATITUDE = new AttributeDescriptor<>("Lengdegrad", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_DEGREE);
    public static final AttributeDescriptor<Integer> VELOCITY = new AttributeDescriptor<>("Hastighet", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_KILO, UNITS_METRE,UNITS_HOUR);
    public static final AttributeDescriptor<Integer> DENM = new AttributeDescriptor<>("DENM", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.READ_ONLY));
    //public static final AttributeDescriptor<String> unit        = new AttributeDescriptor<>("Måleenhet",        ValueType.TEXT);
    //public static final AttributeDescriptor<String> extra       = new AttributeDescriptor<>("Melding",          ValueType.TEXT);

    // TODO:
    //


    public Optional<VehicleType> getVehicleType() {
        return getAttributes().getValue(VEHICLE_TYPE);
    }

    //public Optional<String> getVendor() {return this.getAttributes().getValue(vendor);}
    //public Optional<String> getThing() {return this.getAttributes().getValue(thing);}
    //public Optional<String> getSensor() {return this.getAttributes().getValue(sensor);}
    //public Optional<Date>   getDateTime() {return this.getAttributes().getValue(dateTime);}
    //public Optional<String> getObjectClass() {return this.getAttributes().getValue(vehicleType);}
    //public Optional<Integer> getObjectID() {return this.getAttributes().getValue(vehicleID);}
    //public Optional<String> getLatitude() {return this.getAttributes().getValue(latitude);}
    //public Optional<String> getLongitude() {return this.getAttributes().getValue(longitude);}
    //public Optional<String> getVelocity() {return this.getAttributes().getValue(velocity);}
    //public Optional<String> getHeading() {return this.getAttributes().getValue(heading);}
    //public Optional<String> getDENM() {return this.getAttributes().getValue(DENM);}
    //public Optional<String> getUnit() {return this.getAttributes().getValue(unit);}
    //public Optional<String> getMessage() {return this.getAttributes().getValue(extra);}

    protected LidarAsset() {
    }

    public LidarAsset(String name) {
        super(name);
    }

}
