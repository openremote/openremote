/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.value.impl;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.util.TextUtil;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;

@JsonDeserialize(converter = ColourRGB.HexStringColourRGBConverter.class)
public class ColourRGB implements Serializable {

    public static class HexStringColourRGBConverter extends StdConverter<JsonNode, ColourRGB> {

        @Override
        public ColourRGB convert(JsonNode value) {
            if (value.isTextual()) {
                return ColourRGB.fromHexString(value.asText());
            }
            if (value.isObject()) {
                return new ColourRGB(
                    value.get("r").asInt(),
                    value.get("g").asInt(),
                    value.get("b").asInt()
                );
            }
            if (value.isArray() && value.size() == 3) {
                return new ColourRGB(
                    value.get(0).asInt(),
                    value.get(1).asInt(),
                    value.get(2).asInt()
                );
            }
            return null;
        }
    }

    protected int r;
    protected int g;
    protected int b;

    public ColourRGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColourRGB other = (ColourRGB) o;

        if (r != other.r) return false;
        if (g != other.g) return false;
        return b == other.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b);
    }

    @JsonValue
    @Override
    public String toString() {
        return "#" + ProtocolUtil.bytesToHexString(new byte[] {(byte)r, (byte)g, (byte)b});
    }

    public static ColourRGB fromHexString(String hexString) {
        if (TextUtil.isNullOrEmpty(hexString)) {
            return null;
        }
        if (hexString.charAt(0) == '#') {
            hexString = hexString.substring(1);
        }
        byte[] values = ProtocolUtil.bytesFromHexString(hexString);
        if (values.length == 3) {
            return new ColourRGB(
                Byte.toUnsignedInt(values[0]),
                Byte.toUnsignedInt(values[1]),
                Byte.toUnsignedInt(values[2])
            );
        }

        return null;
    }

    public static ColourRGB fromHS(int hue, int saturation) {
        float hueNormalised = hue/65535f;
        float saturationNormalised = saturation/65535f;
        int rgb = Color.HSBtoRGB(hueNormalised, saturationNormalised, 1);
        Color colour = new Color(rgb);
        return new ColourRGB(colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
