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
package org.openremote.manager.client.assets.asset;

public class Asset {

    public enum Type {
        COMPOSITE,
        SENSOR
    }

    public static boolean isRoot(Asset asset) {
        return asset.getId() == null;
    }

    public static final String ROOT_ID = null;
    public static final String ROOT_TYPE = Type.COMPOSITE.name();
    public static final String ROOT_LABEL = "ROOT";
    public static final String ROOT_LOCATION = null;

    public String id;
    public String type;
    public String displayName;
    public String location;

    public Asset() {
    }

    public Asset(String id, String type, String displayName, String location) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", displayName='" + displayName + '\'' +
            ", location='" + location + '\'' +
            '}';
    }
}
