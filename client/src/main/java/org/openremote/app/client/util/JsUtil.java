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
package org.openremote.app.client.util;

import jsinterop.annotations.JsMethod;

import java.util.List;

@SuppressWarnings("EqualityComparisonWithCoercionJS")
public class JsUtil {

    public native static void log(Object o) /*-{
        console.dir(o);
    }-*/;

    public native static void printType(Object o) /*-{
        console.log(({}).toString.call(o).match(/\s([a-zA-Z]+)/)[1].toLowerCase());
    }-*/;

    public native static String typeOf(Object o) /*-{
        var type = typeof o;
        if (type == 'object') {
            return typeof (o.valueOf());
        } else {
            return type;
        }
    }-*/;


    @SuppressWarnings("rawtypes")
    @JsMethod(namespace = "JsUtil")
    public static Object toJsArray(Object obj) {
        if (obj instanceof List) {
            return ((List)obj).toArray(new Object[0]);
        }
        return obj;
    }
}
