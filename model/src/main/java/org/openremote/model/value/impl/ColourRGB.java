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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;

public class ColourRGB implements Serializable {

    protected int r;
    protected int g;
    protected int b;

    @JsonCreator
    public ColourRGB(@JsonProperty("r") int r, @JsonProperty("g") int g, @JsonProperty("b") int b) {
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "r=" + r +
            ", g=" + g +
            ", b=" + b +
            '}';
    }

    public static ColourRGB fromHS(int hue, int saturation) {
        float hueNormalised = hue/65535f;
        float saturationNormalised = saturation/65535f;
        int rgb = Color.HSBtoRGB(hueNormalised, saturationNormalised, 1);
        Color colour = new Color(rgb);
        return new ColourRGB(colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
