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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import jakarta.persistence.Entity;
import org.openremote.model.value.ValueType;

import java.util.Date;
import java.util.Optional;

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
    public static final AssetDescriptor<LidarAsset> lidarAssetAssetDescriptor = new AssetDescriptor<>("Preview", "00aaaa", LidarAsset.class);

    public static final AttributeDescriptor<String> vendor      = new AttributeDescriptor<>("Vendor",           ValueType.TEXT);
    public static final AttributeDescriptor<String> type        = new AttributeDescriptor<>("Type",             ValueType.TEXT);
    public static final AttributeDescriptor<String> sensor      = new AttributeDescriptor<>("Sensor",           ValueType.TEXT);
    public static final AttributeDescriptor<Date>   dateTime    = new AttributeDescriptor<>("Dato",             ValueType.DATE_AND_TIME);
    public static final AttributeDescriptor<Double> objectID    = new AttributeDescriptor<>("Objekt-ID",        ValueType.NUMBER);
    public static final AttributeDescriptor<String> ObjectClass = new AttributeDescriptor<>("Object Klasse",    ValueType.TEXT);
    public static final AttributeDescriptor<Double> latitude    = new AttributeDescriptor<>("Breddegrad",       ValueType.NUMBER);
    public static final AttributeDescriptor<Double> longitude   = new AttributeDescriptor<>("Lengdegrad",       ValueType.NUMBER);
    public static final AttributeDescriptor<Double> velocity    = new AttributeDescriptor<>("Hastighet",        ValueType.NUMBER);
    public static final AttributeDescriptor<String> unit        = new AttributeDescriptor<>("Måleenhet",        ValueType.TEXT);
    public static final AttributeDescriptor<Double> heading     = new AttributeDescriptor<>("Retning",          ValueType.NUMBER);
    public static final AttributeDescriptor<Double> DENM        = new AttributeDescriptor<>("DENM Kode",        ValueType.NUMBER);
    public static final AttributeDescriptor<String> message     = new AttributeDescriptor<>("Melding",          ValueType.TEXT);

    public Optional<String> getvendor() {return this.getAttributes().getValue(vendor);}
    public Optional<String> gettype() {return this.getAttributes().getValue(type);}
    public Optional<String> getsensor() {return this.getAttributes().getValue(sensor);}
    public Optional<Date>   getdateTime() {return this.getAttributes().getValue(dateTime);}
    public Optional<Double> getobjectID() {return this.getAttributes().getValue(objectID);}
    public Optional<String> getObjectClass() {return this.getAttributes().getValue(ObjectClass);}
    public Optional<Double> getlatitude() {return this.getAttributes().getValue(latitude);}
    public Optional<Double> getlongitude() {return this.getAttributes().getValue(longitude);}
    public Optional<Double> getvelocity() {return this.getAttributes().getValue(velocity);}
    public Optional<String> getunit() {return this.getAttributes().getValue(unit);}
    public Optional<Double> getheading() {return this.getAttributes().getValue(heading);}
    public Optional<Double> getDENM() {return this.getAttributes().getValue(DENM);}
    public Optional<String> getmessage() {return this.getAttributes().getValue(message);}

    protected LidarAsset() {
    }

    public LidarAsset(String name) {
        super(name);
    }

}
