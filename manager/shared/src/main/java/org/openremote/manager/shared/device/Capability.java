package org.openremote.manager.shared.device;

import org.openremote.model.Attribute;

import java.util.List;

/**
 * Analogous to an Object in LWM2M; note the mandatory field
 * has been removed to make the device object model flexible.
 *
 * Idea is that we have a repository of capabilities that can
 * be implemented by devices.
 *
 * Example capabilities are: -
 *
 * 3 = DeviceInfo
 * 3311 = Light Control
 */
// TODO Not used
public class Capability implements CapabilityDefinition {
    protected String type;
    protected String name;
    protected String description;
    protected List<Attribute> resources;

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Attribute> getResources() {
        return resources;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setResources(List<Attribute> resources) {
        this.resources = resources;
    }
}
