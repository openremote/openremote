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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class WeatherAsset extends Asset<WeatherAsset> {

    public static final AttributeDescriptor<Double> TEMPERATURE = new AttributeDescriptor<>("temperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> UV_INDEX = new AttributeDescriptor<>("uVIndex", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.LABEL, "UV index"),
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> SUN_IRRADIANCE = new AttributeDescriptor<>("sunIrradiance", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> SUN_AZIMUTH = new AttributeDescriptor<>("sunAzimuth", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> SUN_ZENITH = new AttributeDescriptor<>("sunZenith", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> SUN_ALTITUDE = new AttributeDescriptor<>("sunAltitude", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> WIND_SPEED = new AttributeDescriptor<>("windSpeed", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_METRE, UNITS_PER, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> WIND_DIRECTION = new AttributeDescriptor<>("windDirection", ValueType.DIRECTION,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> RAINFALL = new AttributeDescriptor<>("rainfall", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_MILLI, UNITS_METRE);
    public static final AttributeDescriptor<Integer> HUMIDITY = new AttributeDescriptor<>("humidity", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));

    public static final AssetDescriptor<WeatherAsset> DESCRIPTOR = new AssetDescriptor<>("weather-partly-cloudy", "49B0D8", WeatherAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WeatherAsset() {
    }

    public WeatherAsset(String name) {
        super(name);
    }

    public Optional<Double> getTemperature() {
        return getAttributes().getValue(TEMPERATURE);
    }

    public Optional<Double> getUVIndex() {
        return getAttributes().getValue(UV_INDEX);
    }

    public Optional<Double> getSunIrradiance() {
        return getAttributes().getValue(SUN_IRRADIANCE);
    }

    public Optional<Double> getSunAzimuth() {
        return getAttributes().getValue(SUN_AZIMUTH);
    }

    public Optional<Double> getSunZenith() {
        return getAttributes().getValue(SUN_ZENITH);
    }

    public Optional<Double> getSunAltitude() {
        return getAttributes().getValue(SUN_ALTITUDE);
    }

    public Optional<Double> getRainfall() {
        return getAttributes().getValue(RAINFALL);
    }

    public Optional<Integer> getHumidity() {
        return getAttributes().getValue(HUMIDITY);
    }
}
