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

public class EventType {
    private EventType(){}

    public static final String CLICK = "click";
    public static final String DOUBLE_CLICK = "dblclick";
    public static final String DRAG = "drag";
    public static final String DRAG_START = "dragstart";
    public static final String DRAG_END = "dragend";
    public static final String MOUSE_DOWN = "mousedown";
    public static final String MOUSE_MOVE = "mousemove";
    public static final String MOUSE_UP = "mouseup";
    public static final String MOVE = "move";
    public static final String MOVE_END = "moveend";
    public static final String MOUSE_START = "movestart";
    public static final String ROTATE = "rotate";
    public static final String ROTATE_END = "rotateend";
    public static final String ROTATE_START = "rotatestart";
    public static final String ZOOM = "zoom";
    public static final String ZOOM_END = "zoomend";
    public static final String ZOOM_START = "zoomstart";
}
