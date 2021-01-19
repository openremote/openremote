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
package org.openremote.model.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;

import static org.openremote.model.geo.GeoJSONFeature.TYPE;

@JsonTypeName(TYPE)
public class GeoJSONFeature extends GeoJSON {

    public static final String TYPE = "Feature";
    @JsonProperty
    protected GeoJSONGeometry geometry;
    @JsonProperty
    protected Map<String, String> properties;

    @JsonCreator
    public GeoJSONFeature(@JsonProperty("geometry") GeoJSONGeometry geometry, @JsonProperty("properties") Map<String, String> properties) {
        super(TYPE);
        this.geometry = geometry;
        this.properties = properties;
    }

    public GeoJSONFeature(GeoJSONGeometry geometry) {
        this(geometry, new HashMap<>());
    }

    public GeoJSONGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJSONGeometry geometry) {
        this.geometry = geometry;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public GeoJSONFeature setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public String getProperty(String name) {
        return properties == null ? null : properties.get(name);
    }

    public GeoJSONFeature setProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        properties.put(name, value);
        return this;
    }
}
