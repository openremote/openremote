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
import org.openremote.model.AbstractTypeHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.openremote.model.geo.GeoJSONFeature.TYPE;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

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

    @Override
    public ObjectValue toValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("type", type);
        ObjectValue props = null;
        ObjectValue geom = null;

        if (properties != null && !properties.isEmpty()) {
            props = Values.createObject();
            ObjectValue finalProps = props;
            properties.forEach((k, v ) -> finalProps.put(k, Values.create(v)));
        }

        objectValue.put("properties", props);

        if (geometry != null) {
            geom = geometry.toValue();
        }

        objectValue.put("geometry", geom);

        return objectValue;
    }

    public static Optional<GeoJSONFeature> fromValue(Value value) {
        return Values.getObject(value)
            .map(obj -> {
                String type = obj.getString("type").orElse(null);
                GeoJSONGeometry geometry = null;
                Map<String, String> properties = null;
                ObjectValue geom = obj.getObject("geometry").orElse(null);
                ObjectValue props = obj.getObject("properties").orElse(null);

                if (isNullOrEmpty(type)) {
                    return null;
                }

                if (geom != null) {
                    String geomType = geom.getString("type").orElse("");
                    switch (geomType) {
                        case GeoJSONPoint.TYPE:
                            geometry = GeoJSONPoint.fromValue(geom).orElse(null);
                            break;
                    }
                }

                if (props != null) {
                    properties = new HashMap<>();
                    Map<String, String> finalProperties = properties;
                    props.stream().forEach(kvp -> Values.getString(kvp.value).ifPresent(v -> finalProperties.put(kvp.key, v)));
                }

                return new GeoJSONFeature(geometry, properties);
            });
    }
}
