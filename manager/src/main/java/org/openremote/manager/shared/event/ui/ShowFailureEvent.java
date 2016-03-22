/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.manager.shared.event.ui;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.event.Event;

@JsType
public class ShowFailureEvent extends Event {

    public static final int DURABLE = 9999999; // 7 days...

    public String text;
    public int durationMillis;

    @JsIgnore
    public ShowFailureEvent() {
    }

    @JsIgnore
    public ShowFailureEvent(String text) {
        this(text, DURABLE);
    }

    @JsIgnore
    public ShowFailureEvent(String text, int durationMillis) {
        this.text = text;
        this.durationMillis = durationMillis;
    }

    public String getText() {
        return text;
    }

    public int getDurationMillis() {
        return durationMillis;
    }
}
