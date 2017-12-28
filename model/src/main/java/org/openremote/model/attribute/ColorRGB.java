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
package org.openremote.model.attribute;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Values;

public class ColorRGB {

    final protected int red;
    final protected int green;
    final protected int blue;

    public ColorRGB(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public ColorRGB(double red, double green, double blue) {
        this((int) red, (int) green, (int) blue);
    }

    public ColorRGB(ArrayValue arrayValue) {
        this(
            arrayValue.getNumber(0).orElseThrow(() -> new IllegalArgumentException("Element 0 must be a number")),
            arrayValue.getNumber(1).orElseThrow(() -> new IllegalArgumentException("Element 1 must be a number")),
            arrayValue.getNumber(2).orElseThrow(() -> new IllegalArgumentException("Element 2 must be a number"))
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

    public ArrayValue asArrayValue() {
        ArrayValue array = Values.createArray();
        array.set(0, Values.create(getRed()));
        array.set(1, Values.create(getGreen()));
        array.set(2, Values.create(getBlue()));
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
