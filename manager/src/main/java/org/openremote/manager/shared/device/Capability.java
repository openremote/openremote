package org.openremote.manager.shared.device;

import java.util.Map;

/**
 * Analogous to an Object in LWM2M; note the mandatory field
 * has been removed to make the device object model flexible.
 *
 * Idea is that we have a repository of capabilites that can
 * be implemented by devices.
 *
 * Example capabilities are: -
 *
 * 3 = DeviceInfo
 * 3311 = Light Control
 */
public class Capability implements CapabilityDefinition {
    protected int id;
    protected String name;
    protected String description;
    protected Map<Integer, Resource> resources;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<Integer, Resource> getResources() {
        return resources;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setResources(Map<Integer, Resource> resources) {
        this.resources = resources;
    }

    public void addResource(int id, Resource resource) {
        resources.put(id, resource);
    }

    public void removeResource(int id) {
        resources.remove(id);
    }

    public void removeResource(Resource resource) {
        resources.entrySet().stream().filter(entry -> entry.getValue().equals(resource)).map(Map.Entry::getKey).forEach(resources::remove);
    }
}
