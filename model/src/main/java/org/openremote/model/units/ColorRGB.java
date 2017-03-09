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
package org.openremote.model.units;

import elemental.json.Json;
import elemental.json.JsonArray;

public class ColorRGB {

    final protected int red;
    final protected int green;
    final protected int blue;

    public ColorRGB(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public ColorRGB(JsonArray jsonArray) {
        this(
            (int)jsonArray.get(0).asNumber(),
            (int)jsonArray.get(1).asNumber(),
            (int)jsonArray.get(2).asNumber()
        );
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public JsonArray asJsonValue() {
        JsonArray array = Json.createArray();
        array.set(0, Json.create(getRed()));
        array.set(1, Json.create(getGreen()));
        array.set(2, Json.create(getBlue()));
        return array;
    }

    public ColorRGB red(int red) {
        return new ColorRGB(red, getGreen(), getBlue());
    }

    public ColorRGB green(int green) {
        return new ColorRGB(getRed(), green, getBlue());
    }

    public ColorRGB blue(int blue) {
        return new ColorRGB(getRed(), getGreen(), blue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorRGB colorRGB = (ColorRGB) o;

        if (red != colorRGB.red) return false;
        if (green != colorRGB.green) return false;
        return blue == colorRGB.blue;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "red=" + red +
            ", green=" + green +
            ", blue=" + blue +
            '}';
    }
}
