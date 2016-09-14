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
package org.openremote.manager.shared.device;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.shared.attribute.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Device asset attribute handling for representing devices that come from agents.
 * <p>
 * A device asset requires a key (typically unique within the scope of the
 * {@link org.openremote.manager.shared.connector.ConnectorComponent}) and
 * zero or many device resources (which also have a key, typically unique
 * within the scope of the device). Both keys are used when an agent builds
 * messaging routes which integrate the connector component. Semantics and scope
 * of the keys are defined by each connector component and can be internal to
 * the connector component.
 * <p>
 * The resources define the functionality of the device and provide access
 * for each item of functionality.
 * <p>
 * TODO: The capabilities allow a device to expose defined device and device resource
 * metadata. A capability such as <em>LightControl</em> would be 'well-known'
 * by software that wants to control lighting. Capabilities provide contextual
 * information for UI and mapping purposes.
 * <p>
 * TODO: Implement resource refs
 */
@JsonIgnoreType // TODO This must be enabled (and fixed before it's usable) if any of serialized classes uses this type
public class DeviceAttributes extends Attributes {

    protected static final String KEY = "key";

    public static boolean isReadOnly(Attribute attribute) {
        return attribute.getName().equals(KEY);
    }

    public DeviceAttributes() {
        super();
    }

    public DeviceAttributes(JsonObject jsonObject) {
        super(jsonObject);
    }

    public String getKey() {
        return hasAttribute(KEY) ? get(KEY).getValueAsString() : null;
    }

    public void setKey(String key) {
        put(new Attribute(KEY, AttributeType.STRING, Json.create(key)));
    }

    public DeviceResource[] getDeviceResources() {
        Attribute[] attributes = super.get();
        List<DeviceResource> list = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (DeviceResource.isDeviceResource(attribute)) {
                list.add(new DeviceResource(attribute));
            }
        }
        return list.toArray(new DeviceResource[list.size()]);
    }

    public DeviceResource getDeviceResource(String name) {
        Attribute attribute = super.get(name);
        if (attribute == null || !DeviceResource.isDeviceResource(attribute))
            return null;
        return new DeviceResource(attribute);
    }


    /* TODO Implement capabilities, distinguish from device resources with attribute metadata
    public CapabilityRef[] getCapabilities() {
        if (!hasAttribute(RESOURCES))
            return null;

        JsonArray capabilitiesArr = get(CAPABILITIES).getValueAsArray();
        CapabilityRef[] capabilities = new CapabilityRef[capabilitiesArr.length()];

        for (int i = 0; i < capabilitiesArr.length(); i++) {
            JsonObject capabilityObj = capabilitiesArr.getObject(i);
            capabilities[i] = new CapabilityRef(capabilityObj);
        }

        return capabilities;
    }

    public void setCapabilities(CapabilityRef... capabilities) {
        if (capabilities == null || capabilities.length == 0) {
            remove(CAPABILITIES);
            return;
        }

        JsonArray capabilitiesArr = Json.createArray();

        for (int i = 0; i < capabilities.length; i++) {
            capabilitiesArr.set(i, Json.parse(capabilities[i].toString()));
        }

        put(new Attribute(CAPABILITIES, AttributeType.OBJECT_ARRAY, capabilitiesArr));
    }
    */
}