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
package org.openremote.model.asset.impl;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AbstractNameValueHolder;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Collection;
import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.value.ValueType.BOOLEAN;

@Entity
public class ElectricityProducerWindAsset extends ElectricityProducerAsset {

    public static final AttributeDescriptor<Double> WIND_SPEED_REFERENCE = new AttributeDescriptor<>("windSpeedReference", ValueType.POSITIVE_NUMBER
    ).withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND);
    public static final AttributeDescriptor<Double> WIND_SPEED_MIN = new AttributeDescriptor<>("windSpeedMin", ValueType.POSITIVE_NUMBER
    ).withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND);
    public static final AttributeDescriptor<Double> WIND_SPEED_MAX = new AttributeDescriptor<>("windSpeedMax", ValueType.POSITIVE_NUMBER
    ).withUnits(UNITS_METRE, UNITS_PER, UNITS_SECOND);

    public static final AttributeDescriptor<Boolean> INCLUDE_FORECAST_WIND_SERVICE = new AttributeDescriptor<>("includeForecastWindService", BOOLEAN);

    public static final AttributeDescriptor<Boolean> SET_ACTUAL_WIND_VALUE_WITH_FORECAST = new AttributeDescriptor<>("setWindActualValueWithForecast", BOOLEAN);

    public static final AssetDescriptor<ElectricityProducerWindAsset> DESCRIPTOR = new AssetDescriptor<>("wind-turbine", "4B87EA", ElectricityProducerWindAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityProducerWindAsset() {
    }

    public ElectricityProducerWindAsset(String name) {
        super(name);
    }

    public Optional<Double> getWindSpeedReference() {
        return getAttributes().getValue(WIND_SPEED_REFERENCE);
    }

    public Optional<Double> getWindSpeedMin() {
        return getAttributes().getValue(WIND_SPEED_MIN);
    }

    public Optional<Double> getWindSpeedMax() {
        return getAttributes().getValue(WIND_SPEED_MAX);
    }

    public Optional<Boolean> isIncludeForecastWindService() {
        return getAttribute(INCLUDE_FORECAST_WIND_SERVICE).flatMap(AbstractNameValueHolder::getValue);
    }

    public Optional<Boolean> isSetActualWindValueWithForecast() {
        return getAttribute(SET_ACTUAL_WIND_VALUE_WITH_FORECAST).flatMap(AbstractNameValueHolder::getValue);
    }

    @Override
    public ElectricityProducerWindAsset setPower(Double value) {
        super.setPower(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setPowerSetpoint(Double value) {
        super.setPowerSetpoint(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setPowerImportMin(Double value) {
        super.setPowerImportMin(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setPowerImportMax(Double value) {
        super.setPowerImportMax(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setPowerExportMin(Double value) {
        super.setPowerExportMin(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setPowerExportMax(Double value) {
        super.setPowerExportMax(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEnergyImportTotal(Double value) {
        super.setEnergyImportTotal(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEnergyExportTotal(Double value) {
        super.setEnergyExportTotal(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEfficiencyImport(Integer value) {
        super.setEfficiencyImport(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEfficiencyExport(Integer value) {
        super.setEfficiencyExport(value);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setModel(String model) {
        super.setModel(model);
        return this;
    }

    public ElectricityProducerWindAsset setWindSpeedMin(double value) {
        getAttributes().getOrCreate(WIND_SPEED_MIN).setValue(value);
        return this;
    }

    public ElectricityProducerWindAsset setWindSpeedMax(double value) {
        getAttributes().getOrCreate(WIND_SPEED_MAX).setValue(value);
        return this;
    }

    public ElectricityProducerWindAsset setWindSpeedReference(double value) {
        getAttributes().getOrCreate(WIND_SPEED_REFERENCE).setValue(value);
        return this;
    }

    public ElectricityProducerWindAsset setIncludeForecastWindService(boolean value) {
        getAttributes().getOrCreate(INCLUDE_FORECAST_WIND_SERVICE).setValue(value);
        return this;
    }

    public ElectricityProducerWindAsset setSetActualWindValueWithForecast(boolean value) {
        getAttributes().getOrCreate(SET_ACTUAL_WIND_VALUE_WITH_FORECAST).setValue(value);
        return this;
    }
}
