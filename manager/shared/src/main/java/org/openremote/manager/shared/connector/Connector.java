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
package org.openremote.manager.shared.connector;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.shared.ngsi.Attribute;
import org.openremote.manager.shared.ngsi.AttributeType;
import org.openremote.manager.shared.ngsi.Entity;

/**
 * Interface for agent connectors that is used to connect and communicate with agents
 * in an agnostic way. (That is, a Camel component with the required endpoints.)
 */
public class Connector extends Entity {

    // Camel components which are OR connectors must have this in component.properties
    public static final String PROPERTY_TYPE = "openremote-connector-type";

    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_SYNTAX = "syntax";

    public Connector() {
    }

    public Connector(JsonObject jsonObject) {
        super(jsonObject);
    }

    public Connector(String type) {
        super();
        setType(type);
    }

    public String getName() {
        return getAttributeValueAsString(ATTRIBUTE_NAME);
    }

    public void setName(String name) {
        Attribute attr = new Attribute(ATTRIBUTE_NAME, AttributeType.STRING, Json.create(name));
        super.addAttribute(attr);
    }

    public String getSyntax() {
        return getAttributeValueAsString(ATTRIBUTE_SYNTAX);
    }

    public void setSyntax(String syntax) {
        Attribute attr = new Attribute(ATTRIBUTE_SYNTAX, AttributeType.STRING, Json.create(syntax));
        super.addAttribute(attr);
    }

}
