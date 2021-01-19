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

import java.util.Objects;

public class ColourRGBW extends ColourRGB {

    protected int w;

    @JsonCreator
    public ColourRGBW(@JsonProperty("r") int r, @JsonProperty("g") int g, @JsonProperty("b") int b, @JsonProperty("w") int w) {
        super(r, g, b);
        this.w = w;
    }

    public int getW() {
        return w;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColourRGBW other = (ColourRGBW) o;

        return super.equals(other) && w == other.w;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(w);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "r=" + r +
            ", g=" + g +
            ", b=" + b +
            ", w=" + w +
            '}';
    }
}
