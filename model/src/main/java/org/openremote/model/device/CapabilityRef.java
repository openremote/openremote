package org.openremote.model.device;

// TODO Not used
public class CapabilityRef {/*
extends Attribute {

    protected Attributes resources;

    public CapabilityRef(JsonObject jsonObject) {
        super(jsonObject.get("name"), jsonObject);
    }

    public CapabilityRef(String name, String capabilityType, Attributes resourceRefs) {
        super(name, AttributeValueType.OBJECT);
        jsonObject.put("name", name);
        setCapabilityType(capabilityType);
        setResourceRefs(resourceRefs);
    }

    public String getCapabilityType() {
        return jsonObject.getString("capabilityType");
    }

    public void setCapabilityType(String capabilityType) {
        jsonObject.put("capabilityType", capabilityType);
    }

    public Attributes getResourceRefs() {
        JsonObject refsObj = jsonObject.getObject("resourceRefs");
        return refsObj == null ? null : new Attributes(refsObj);
    }

    public void setResourceRefs(Attributes resourceRefs) {
        jsonObject.put("resourceRefs", resourceRefs.getJsonObject());
    }
    */
}
