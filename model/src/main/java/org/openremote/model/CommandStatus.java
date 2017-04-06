/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public enum CommandStatus {
    /**
     * Command has completed
     */
    COMPLETED,
    /**
     * Command is currently executing
     */
    ACTIVE,
    /**
     * Command has been cancelled
     */
    CANCELLED,

    /**
     * Command produced an error
     */
    ERROR;

    public JsonValue asJsonValue() {
        // Use JsonObject so it can be pushed into executable attribute
        JsonObject jsonObject = Json.createObject();
        jsonObject.put("status", Json.create(this.toString()));
        return jsonObject;
    }
}
