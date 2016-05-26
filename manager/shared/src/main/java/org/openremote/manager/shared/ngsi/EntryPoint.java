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
package org.openremote.manager.shared.ngsi;

import elemental.json.JsonObject;

public class EntryPoint {

    final protected JsonObject jsonObject;

    public EntryPoint(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getEntitiesLocation() {
        return jsonObject.hasKey("entities_url") ? jsonObject.getString("entities_url") : null;
    }

    public void setEntitiesLocation(String entitiesLocation) {
        jsonObject.put("entities_url", entitiesLocation);
    }

    public String getTypesLocation() {
        return jsonObject.hasKey("types_url") ? jsonObject.getString("types_url") : null;
    }

    public void setTypesLocation(String typesLocation) {
        jsonObject.put("types_url", typesLocation);
    }

    public String getSubscriptionsLocation() {
        return jsonObject.hasKey("subscriptions_url") ? jsonObject.getString("subscriptions_url") : null;
    }

    public void setSubscriptionsLocation(String subscriptionsLocation) {
        jsonObject.put("subscriptions_url", subscriptionsLocation);
    }

    public String getRegistrationsLocation() {
        return jsonObject.hasKey("registrations_url") ? jsonObject.getString("registrations_url") : null;
    }

    public void setRegistrationsLocation(String registrationsLocation) {
        jsonObject.put("registrations_url", registrationsLocation);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + jsonObject.toJson();
    }
}
