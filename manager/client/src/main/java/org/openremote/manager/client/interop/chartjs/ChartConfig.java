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
package org.openremote.manager.client.interop.chartjs;

import com.google.gwt.core.client.JavaScriptObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class ChartConfig {

    @JsProperty
    String type;

    @JsProperty
    JavaScriptObject data;

    @JsProperty
    JavaScriptObject options;

    public ChartConfig(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JavaScriptObject getData() {
        return data;
    }

    public void setData(JavaScriptObject data) {
        this.data = data;
    }

    public JavaScriptObject getOptions() {
        return options;
    }

    public void setOptions(JavaScriptObject options) {
        this.options = options;
    }
}
