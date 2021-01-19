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
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.impl.ColourRGB;
import org.openremote.model.value.impl.ColourRGBA;
import org.openremote.model.value.impl.ColourRGBAW;
import org.openremote.model.value.impl.ColourRGBW;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_PERCENTAGE;

@Entity
public class LightAsset extends Asset<LightAsset> {

    public static final AttributeDescriptor<Boolean> ON_OFF = new AttributeDescriptor<>("onOff", ValueType.BOOLEAN).withFormat(ValueFormat.BOOLEAN_ON_OFF());
    public static final AttributeDescriptor<Integer> BRIGHTNESS = new AttributeDescriptor<>("brightness", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<ColourRGB> COLOUR_RGB = new AttributeDescriptor<>("colourRGB", ValueType.COLOUR_RGB);
    public static final AttributeDescriptor<ColourRGBA> COLOUR_RGBA = new AttributeDescriptor<>("colourRGBA", ValueType.COLOUR_RGBA);
    public static final AttributeDescriptor<ColourRGBW> COLOUR_RGBW = new AttributeDescriptor<>("colourRGBW", ValueType.COLOUR_RGBW);
    public static final AttributeDescriptor<ColourRGBAW> COLOUR_RGBAW = new AttributeDescriptor<>("colourRGBAW", ValueType.COLOUR_RGBAW);
    public static final AttributeDescriptor<Integer> TEMPERATURE = new AttributeDescriptor<>("temperature", ValueType.POSITIVE_INTEGER);
    public static final AssetDescriptor<LightAsset> DESCRIPTOR = new AssetDescriptor<>("lightbulb", "e6688a", LightAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    LightAsset() {
        this(null);
    }

    public LightAsset(String name) {
        super(name);
    }

    public Optional<Boolean> getOnOff() {
        return getAttributes().getValue(ON_OFF);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setOnOff(Boolean value) {
        getAttributes().getOrCreate(ON_OFF).setValue(value);
        return (T)this;
    }

    public Optional<Integer> getBrightness() {
        return getAttributes().getValue(BRIGHTNESS);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setBrightness(Integer value) {
        getAttributes().getOrCreate(BRIGHTNESS).setValue(value);
        return (T)this;
    }

    public Optional<ColourRGB> getColourRGB() {
        return getAttributes().getValue(COLOUR_RGB);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setColourRGB(ColourRGB value) {
        getAttributes().getOrCreate(COLOUR_RGB).setValue(value);
        return (T)this;
    }

    public Optional<ColourRGBA> getColorRGBA() {
        return getAttributes().getValue(COLOUR_RGBA);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setColourRGBA(ColourRGBA value) {
        getAttributes().getOrCreate(COLOUR_RGBA).setValue(value);
        return (T)this;
    }

    public Optional<ColourRGBW> getColorRGBW() {
        return getAttributes().getValue(COLOUR_RGBW);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setColourRGBW(ColourRGBW value) {
        getAttributes().getOrCreate(COLOUR_RGBW).setValue(value);
        return (T)this;
    }

    public Optional<ColourRGBAW> getColorRGBAW() {
        return getAttributes().getValue(COLOUR_RGBAW);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setColourRGBAW(ColourRGBAW value) {
        getAttributes().getOrCreate(COLOUR_RGB).setValue(value);
        return (T)this;
    }

    public Optional<Integer> getTemperature() {
        return getAttributes().getValue(TEMPERATURE);
    }

    @SuppressWarnings("unchecked")
    public <T extends LightAsset> T setTemperature(Integer value) {
        getAttributes().getOrCreate(TEMPERATURE).setValue(value);
        return (T)this;
    }
}
