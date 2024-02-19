package org.openremote.agent.protocol.homeassistant.commands;

public class EntityStateCommand {
    private String service;
    private String entityId;
    private String attributeName;
    private String attributeValue;

    public EntityStateCommand(String service, String entityId, String attributeName, String attributeValue) {
        this.service = service;
        this.entityId = entityId;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }


    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }


    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }


}
