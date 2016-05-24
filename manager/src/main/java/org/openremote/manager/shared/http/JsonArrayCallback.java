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
package org.openremote.manager.shared.http;

import elemental.json.JsonArray;
import org.openremote.manager.shared.Consumer;

public class JsonArrayCallback extends AbstractCallback<JsonArray> {

    public JsonArrayCallback(Consumer<JsonArray> onSuccess, Consumer<Exception> onFailure) {
        super(onSuccess, onFailure);
    }

    public JsonArrayCallback(int expectedStatusCode, Consumer<JsonArray> onSuccess, Consumer<Exception> onFailure) {
        super(expectedStatusCode, onSuccess, onFailure);
    }

    @Override
    protected JsonArray readMessageBody(Object entity) {
        return (JsonArray)entity;
        /* TODO The simple cast works, or we'd need something like this:
        if (entity instanceof JreJsonObject) {
            return (JreJsonObject) entity;
        }
        JavaScriptObject jso = (JavaScriptObject) entity;
        return jso.<JsJsonObject>cast();
        */
    }
}