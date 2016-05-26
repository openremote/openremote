package org.openremote.manager.shared.device;

import java.util.List;

/**
 * Device model for representing devices that come from IOT connectors.
 * A Device requires a URI (unique within the scope of the connector) and
 * one or more DeviceResource objects.
 *
 * The resources define the functionality of the device and provide the URIs
 * for each item of functionality.
 *
 * The capabilities allow a device to expose defined 'capabilities' such as
 * LightControl and these capabilities provide contextual information for UI
 * purposes and also NGSI entity mapping.
 *
 */
public class Device {
    protected String uri;
    protected String name;
    protected String description;
    protected String type;
    protected List<DeviceResource> resources;
    protected List<CapabilityRef> capabilities;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<DeviceResource> getResources() {
        return resources;
    }

    public void setResources(List<DeviceResource> resources) {
        this.resources = resources;
    }

    public List<CapabilityRef> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<CapabilityRef> capabilities) {
        this.capabilities = capabilities;
    }

    public void addCapability(CapabilityRef capability) {
        capabilities.add(capability);
    }

    public void removeCapability(CapabilityRef capabilityRef) {
        capabilities.remove(capabilityRef);
    }

    public void removeCapability(int index) {
        if(index < capabilities.size()) {
            capabilities.remove(index);
        }
    }
}
