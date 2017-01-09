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
package org.openremote.model.asset;

import elemental.json.Json;
import elemental.json.JsonObject;

public class Color {

    final protected int red;
    final protected int green;
    final protected int blue;

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Color(JsonObject jsonObject) {
        this(
            (int)jsonObject.get("red").asNumber(),
            (int)jsonObject.get("green").asNumber(),
            (int)jsonObject.get("blue").asNumber()
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

    public JsonObject asJsonValue() {
        JsonObject object = Json.createObject();
        object.put("red", Json.create(getRed()));
        object.put("green", Json.create(getGreen()));
        object.put("blue", Json.create(getBlue()));
        return object;
    }

    public Color red(int red) {
        return new Color(red, getGreen(), getBlue());
    }

    public Color green(int green) {
        return new Color(getRed(), green, getBlue());
    }

    public Color blue(int blue) {
        return new Color(getRed(), getGreen(), blue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Color color = (Color) o;

        if (red != color.red) return false;
        if (green != color.green) return false;
        return blue == color.blue;
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
