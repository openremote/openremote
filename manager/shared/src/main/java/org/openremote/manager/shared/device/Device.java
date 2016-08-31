package org.openremote.manager.shared.device;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetType;

/**
 * Device asset for representing devices that come from Agents.
 * A Device requires a URI (unique within the scope of the connector) and
 * one or more DeviceResource objects.
 *
 * The resources define the functionality of the device and provide the URIs
 * for each item of functionality.
 *
 * The capabilities allow a device to expose defined 'capabilities' such as
 * LightControl and these capabilities provide contextual information for UI
 * and mapping purposes.
 *
 */
public class Device extends Asset {
    protected static final String URI_KEY = "uri";
    protected static final String RESOURCES_KEY = "resources";
    protected static final String CAPABILITIES_KEY = "capabilities";
    
    public Device() {
        setType(AssetType.DEVICE);
    }

    @Override
    public void setType(String type) {
    }

    public String getUri() {
        if (attributes == null) {
            return null;
        }

        return attributes.getString(URI_KEY);
    }

    public void setUri(String uri) {
        if (attributes == null) {
            attributes = Json.createObject();
        }

        attributes.put(URI_KEY, uri);
    }

    public DeviceResource[] getResources()
    {
        if (attributes == null || !attributes.hasKey(RESOURCES_KEY)) {
            return null;
        }

        JsonArray resourcesArr = attributes.getArray(RESOURCES_KEY);
        DeviceResource[] resources = new DeviceResource[resourcesArr.length()];

        for (int i = 0; i < resourcesArr.length(); i++) {
            JsonObject resourceObj = resourcesArr.getObject(i);
            resources[i] = new DeviceResource(resourceObj);
        }

        return resources;
    }

    public void setResources(DeviceResource[] resources){
        if (attributes == null) {
            attributes = Json.createObject();
        }

        if (resources == null || resources.length == 0) {
            attributes.remove(RESOURCES_KEY);
            return;
        }

        JsonArray resourcesArr = Json.createArray();

        for (int i = 0; i < resources.length; i++) {
            resourcesArr.set(i, Json.parse(resources[i].toString()));
        }

        attributes.put(RESOURCES_KEY, resourcesArr);
    }

    public CapabilityRef[] getCapabilities() {
        if (attributes == null || !attributes.hasKey(CAPABILITIES_KEY)) {
            return null;
        }

        JsonArray capabilitiesArr = attributes.getArray(CAPABILITIES_KEY);
        CapabilityRef[] capabilities = new CapabilityRef[capabilitiesArr.length()];

        for (int i = 0; i < capabilitiesArr.length(); i++) {
            JsonObject capabilityObj = capabilitiesArr.getObject(i);
            capabilities[i] = new CapabilityRef(capabilityObj);
        }

        return capabilities;
    }

    public void setCapabilities(CapabilityRef[] capabilities){
        if (attributes == null) {
            attributes = Json.createObject();
        }

        if (capabilities == null || capabilities.length == 0) {
            attributes.remove(CAPABILITIES_KEY);
            return;
        }

        JsonArray capabilitiesArr = Json.createArray();

        for (int i = 0; i < capabilities.length; i++) {
            capabilitiesArr.set(i, Json.parse(capabilities[i].toString()));
        }

        attributes.put(CAPABILITIES_KEY, capabilitiesArr);
    }
}
