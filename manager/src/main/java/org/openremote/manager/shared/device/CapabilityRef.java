package org.openremote.manager.shared.device;

import java.util.Map;

public class CapabilityRef {
    protected int typeId;
    protected String name;
    protected Map<Integer, ResourceRef> resources;

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, ResourceRef> getResources() {
        return resources;
    }

    public void setResources(Map<Integer, ResourceRef> resources) {
        this.resources = resources;
    }

    public void addResource(int id, ResourceRef resource) {
        resources.put(id, resource);
    }

    public void removeResource(int id) {
        resources.remove(id);
    }

    public void removeResource(ResourceRef resource) {
        resources.entrySet().stream().filter(entry -> entry.getValue().equals(resource)).map(Map.Entry::getKey).forEach(resources::remove);
    }
}
