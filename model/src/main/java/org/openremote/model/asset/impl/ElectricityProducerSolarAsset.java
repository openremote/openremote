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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AbstractNameValueHolder;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Collection;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_DEGREE;
import static org.openremote.model.value.ValueType.BOOLEAN;

@Entity
public class ElectricityProducerSolarAsset extends ElectricityProducerAsset {

    public enum PanelOrientation {
        SOUTH,
        EAST_WEST
    }

    public static final ValueDescriptor<PanelOrientation> PANEL_ORIENTATION_VALUE = new ValueDescriptor<>("panelOrientation", PanelOrientation.class);

    public static final AttributeDescriptor<PanelOrientation> PANEL_ORIENTATION = new AttributeDescriptor<>("panelOrientation", PANEL_ORIENTATION_VALUE);
    public static final AttributeDescriptor<Integer> PANEL_AZIMUTH = new AttributeDescriptor<>("panelAzimuth", ValueType.INTEGER
    ).withUnits(UNITS_DEGREE);
    public static final AttributeDescriptor<Integer> PANEL_PITCH = new AttributeDescriptor<>("panelPitch", ValueType.POSITIVE_INTEGER
    ).withUnits(UNITS_DEGREE);

    public static final AttributeDescriptor<Boolean> INCLUDE_FORECAST_SOLAR_SERVICE = new AttributeDescriptor<>("includeForecastSolarService", BOOLEAN);

    public static final AttributeDescriptor<Boolean> SET_ACTUAL_SOLAR_VALUE_WITH_FORECAST = new AttributeDescriptor<>("setActualSolarValueWithForecast", BOOLEAN);

    public static final AssetDescriptor<ElectricityProducerSolarAsset> DESCRIPTOR = new AssetDescriptor<>("white-balance-sunny", "EABB4D", ElectricityProducerSolarAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityProducerSolarAsset() {
    }

    public ElectricityProducerSolarAsset(String name) {
        super(name);
    }

    public Optional<PanelOrientation> getPanelOrientation() {
        return getAttributes().getValue(PANEL_ORIENTATION);
    }

    public Optional<Boolean> isIncludeForecastSolarService() {
        return getAttribute(INCLUDE_FORECAST_SOLAR_SERVICE).flatMap(AbstractNameValueHolder::getValue);
    }

    public Optional<Boolean> isSetActualSolarValueWithForecast() {
        return getAttribute(SET_ACTUAL_SOLAR_VALUE_WITH_FORECAST).flatMap(AbstractNameValueHolder::getValue);
    }

    public ElectricityProducerSolarAsset setPanelOrientation(PanelOrientation value) {
        getAttributes().getOrCreate(PANEL_ORIENTATION).setValue(value);
        return this;
    }

    public Optional<Integer> getPanelAzimuth() {
        return getAttributes().getValue(PANEL_AZIMUTH);
    }

    public ElectricityProducerSolarAsset setPanelAzimuth(Integer value) {
        getAttributes().getOrCreate(PANEL_AZIMUTH).setValue(value);
        return this;
    }

    public Optional<Integer> getPanelPitch() {
        return getAttributes().getValue(PANEL_PITCH);
    }

    public ElectricityProducerSolarAsset setPanelPitch(Integer value) {
        getAttributes().getOrCreate(PANEL_PITCH).setValue(value);
        return this;
    }

    public ElectricityProducerSolarAsset setIncludeForecastSolarService(boolean value) {
        getAttributes().getOrCreate(INCLUDE_FORECAST_SOLAR_SERVICE).setValue(value);
        return this;
    }

    public ElectricityProducerSolarAsset setSetActualSolarValueWithForecast(boolean value) {
        getAttributes().getOrCreate(SET_ACTUAL_SOLAR_VALUE_WITH_FORECAST).setValue(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPower(Double value) {
        super.setPower(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPowerSetpoint(Double value) {
        super.setPowerSetpoint(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPowerImportMin(Double value) {
        super.setPowerImportMin(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPowerImportMax(Double value) {
        super.setPowerImportMax(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPowerExportMin(Double value) {
        super.setPowerExportMin(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setPowerExportMax(Double value) {
        super.setPowerExportMax(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setEnergyImportTotal(Double value) {
        super.setEnergyImportTotal(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setEnergyExportTotal(Double value) {
        super.setEnergyExportTotal(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setEfficiencyImport(Integer value) {
        super.setEfficiencyImport(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setEfficiencyExport(Integer value) {
        super.setEfficiencyExport(value);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    @Override
    public ElectricityProducerSolarAsset setModel(String model) {
        super.setModel(model);
        return this;
    }
}
