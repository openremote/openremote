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
package org.openremote.model.attribute;

import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

import static org.openremote.model.asset.MetaItemType.*;
import static org.openremote.model.attribute.AttributeValueType.*;

public enum AttributeType implements AttributeDescriptor {

    CONSOLE_NAME("consoleName", STRING),

    CONSOLE_VERSION("consoleVersion", STRING),

    CONSOLE_PLATFORM("consolePlatform", STRING),

    CONSOLE_PROVIDERS("consoleProviders", OBJECT),

    EMAIL("email", AttributeValueType.EMAIL, LABEL.withInitialValue(Values.create("Email"))),

    GEO_CITY("city", STRING,
            LABEL.withInitialValue(Values.create("City")),
            ABOUT.withInitialValue(Values.create("http://project-haystack.org/tag/geoCity"))),

    GEO_COUNTRY("country", STRING,
            LABEL.withInitialValue(Values.create("Country")),
            ABOUT.withInitialValue(Values.create("http://project-haystack.org/tag/geoCountry"))),

    GEO_POSTAL_CODE("postalCode", NUMBER,
            LABEL.withInitialValue(Values.create("Postal Code")),
            ABOUT.withInitialValue(Values.create("http://project-haystack.org/tag/geoPostalCode"))),

    GEO_STREET("street", STRING,
            LABEL.withInitialValue(Values.create("Street")),
            ABOUT.withInitialValue(Values.create("http://project-haystack.org/tag/geoStreet"))),

    LOCATION("location", AttributeValueType.GEO_JSON_POINT, LABEL.withInitialValue(Values.create("Location"))),

    SURFACE_AREA(
            "surfaceArea",
            NUMBER,
            LABEL.withInitialValue(Values.create("Surface Area")),
            DESCRIPTION.withInitialValue(Values.create("Floor area of building measured in m²")),
            ABOUT.withInitialValue(Values.create("http://project-haystack.org/tag/area")));

    final protected String name;
    final protected AttributeValueDescriptor attributeValueDescriptor;
    final protected Value initialValue;
    final protected MetaItemDescriptor[] metaItemDescriptors;

    AttributeType(String name, AttributeValueDescriptor attributeValueDescriptor, MetaItemDescriptor... metaItemDescriptors) {
        this(name, attributeValueDescriptor, null, metaItemDescriptors);
    }

    AttributeType(String name, AttributeValueDescriptor attributeValueDescriptor, Value initialValue, MetaItemDescriptor... metaItemDescriptors) {
        this.name = name;
        this.attributeValueDescriptor = attributeValueDescriptor;
        this.metaItemDescriptors = metaItemDescriptors;
        this.initialValue = initialValue;
    }

    public static Optional<AttributeDescriptor> getByValue(String name) {
        if (name == null)
            return Optional.empty();

        for (AttributeDescriptor descriptor : values()) {
            if (name.equals(descriptor.getName()))
                return Optional.of(descriptor);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeValueDescriptor getValueDescriptor() {
        return attributeValueDescriptor;
    }

    @Override
    public Optional<MetaItemDescriptor[]> getMetaItemDescriptors() {
        return Optional.ofNullable(metaItemDescriptors);
    }

    @Override
    public Value getInitialValue() {
        return initialValue;
    }
}
