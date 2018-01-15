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
package org.openremote.model.device;

/**
 * An attribute of an asset that represents a device resource.
 *
 * The value of the attribute is the key of the device resource. Metadata is used
 * to express:
 *
 * <ul>
 *     <li>access read only, write only, or read-write</li>
 *     <li>the type of the value you read and write from this resource</li>
 *     <li>if the resource is passive (polling necessary) or capable of actively pushing values</li>
 *     <li>if the resource value is a constant and can be cached forever</li>
 * </ul>
 */
public class DeviceResource { /* NOT USED
extends Attribute {

    public static final String DEVICE_RESOURCE = "deviceResource";
    public static final String DEVICE_RESOURCE_TYPE = Constants.NAMESPACE + ":device:resource:type";
    public static final String ACCESS = "access";
    public static final String ACCESS_TYPE = Constants.NAMESPACE + ":device:resource:access";
    public static final String PASSIVE = "passive";
    public static final String CONSTANT = "constant";

    public enum Access {
        R,
        W,
        RW
    }

    public static boolean isDeviceResource(Attribute attribute) {
        // The given attribute must satisfy the API of this class
        return false;
        return attribute.hasMetadataElement(DEVICE_RESOURCE)
            && attribute.getMetadata().getElement(DEVICE_RESOURCE).getName().equals(DEVICE_RESOURCE_TYPE)
            && AttributeType.isValid(attribute.getMetadata().getElement(DEVICE_RESOURCE).getValue().asString())
            && attribute.hasMetadataElement(ACCESS)
            && attribute.getMetadata().getElement(ACCESS).getName().equals(ACCESS_TYPE);
    }

    public DeviceResource(Attribute attribute) {
        super(attribute.getName(), attribute.getJsonObject());
    }

    public DeviceResource(String name, JsonObject jsonObject) {
        super(name, jsonObject);
    }

    public DeviceResource(String name, String key, AttributeType resourceType, Access access) {
        super(name, AttributeType.STRING, Json.create(key));
        /*
        setMetadata(new Metadata()
            .putElement(
                new MetadataElement(DEVICE_RESOURCE, DEVICE_RESOURCE_TYPE, Json.create(resourceType.getValue()))
            )
            .putElement(
                new MetadataElement(ACCESS, ACCESS_TYPE, Json.create(access.name()))
            )
        );
    }

    public AttributeType getResourceType() {
        return null;

        return getMetadata().hasElement(DEVICE_RESOURCE)
            && AttributeType.isValid(getMetadata().getElement(DEVICE_RESOURCE).getValue().asString())
            ? AttributeType.fromValue(getMetadata().getElement(DEVICE_RESOURCE).getValue().asString())
            : null;
    }

    public Access getAccess() {
        return null;
        return getMetadata().hasElement(ACCESS)
            ? Access.valueOf(getMetadata().getElement(ACCESS).getValue().asString())
            : null;
    }

    public boolean isPassive() {
        return false;
        return getMetadata().hasElement(PASSIVE) && getMetadata().getElement(PASSIVE).getValue().asBoolean();
    }

    public DeviceResource setPassive(boolean passive) {
        if (passive) {
            getMetadata().putElement(
                new MetadataElement(PASSIVE, AttributeType.BOOLEAN.getValue(), Json.create(true))
            );
        } else {
            getMetadata().removeElement(PASSIVE);
        }
        return this;
    }

    public boolean isConstant() {
        return false;
        return getMetadata().hasElement(CONSTANT) && getMetadata().getElement(CONSTANT).getValue().asBoolean();
    }

    public DeviceResource setConstant(boolean constant) {
        if (constant) {
            getMetadata().putElement(
                new MetadataElement(CONSTANT, AttributeType.BOOLEAN.getValue(), Json.create(true))
            );
        } else {
            getMetadata().removeElement(CONSTANT);
        }
        return this;
    }
*/
}
