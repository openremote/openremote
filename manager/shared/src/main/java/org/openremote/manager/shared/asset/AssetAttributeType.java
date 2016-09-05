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
package org.openremote.manager.shared.asset;

import elemental.json.Json;
import elemental.json.JsonString;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.attribute.MetadataElement;

/**
 * Asset attribute metadata type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 */
public enum AssetAttributeType {

    SENSOR("urn:openremote:asset:sensor"),
    ACTUATOR("urn:openremote:asset:actuator");

    final protected String value;

    AssetAttributeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public JsonString getJsonValue() {
        return Json.create(getValue());
    }

    public static boolean isSensor(Attribute attribute) {
        MetadataElement metaElementType;
        return attribute.hasMetadata()
            && (metaElementType = attribute.getMetadata().getElement("type")) != null
            && metaElementType.getType().equals(AttributeType.STRING.getValue())
            && metaElementType.getValue().equals(AssetAttributeType.SENSOR.getJsonValue());
    }

    public static boolean isActuator(Attribute attribute) {
        MetadataElement metaElementType;
        return attribute.hasMetadata()
            && (metaElementType = attribute.getMetadata().getElement("type")) != null
            && metaElementType.getType().equals(AttributeType.STRING.getValue())
            && metaElementType.getValue().equals(AssetAttributeType.ACTUATOR.getJsonValue());
    }

}
