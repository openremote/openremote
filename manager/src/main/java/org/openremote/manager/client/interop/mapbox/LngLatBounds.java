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
package org.openremote.manager.client.interop.mapbox;

import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "LngLatBounds", namespace = "mapboxgl")
public class LngLatBounds {
    public LngLatBounds(LngLat sw, LngLat ne) {
    }

    public native LngLat getNorthEast();

    public native LngLat getSouthWest();

    public native LngLat getSouthEast();

    public native LngLat getNorthWest();

    public native LngLat getCenter();

    public native double getNorth();

    public native double getEast();

    public native double getSouth();

    public native double getWest();

    public native LngLatBounds extend(LngLat includeLngLat);

    public native LngLatBounds extend(LngLatBounds includeBounds);
}
