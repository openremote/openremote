package org.openremote.manager.shared.device;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.AttributeType;

public class DeviceResource extends Attribute {

    public enum Access {
        R,
        W,
        RW
    }

    public DeviceResource(String name) {
        super(name);
    }

    public DeviceResource(JsonObject jsonObject) {
        super(jsonObject.get("name"), jsonObject);
    }

    public DeviceResource(String name, String description, AttributeType type, Access access, String units) {
        super(name, type);
        jsonObject.put("name", name);
        setAccess(access);
        setUnits(units);
        setDescription(description);
    }

    public String getDescription() {
        return jsonObject.get("description").asString();
    }

    public DeviceResource setDescription(String description) {
        jsonObject.put("description", Json.create(description));
        return this;
    }

    public Access getAccess() {
        return jsonObject.hasKey("access") ? Access.valueOf(jsonObject.get("access").asString()) : null;
    }

    public DeviceResource setAccess(Access access) {
        jsonObject.put("access", Json.create(access.toString()));
        return this;
    }

    public String getUnits() {
        return jsonObject.get("units").asString();
    }

    public DeviceResource setUnits(String units) {
        jsonObject.put("units", Json.create(units));
        return this;
    }

    public String getUri() {
        return jsonObject.get("uri").asString();
    }

    public DeviceResource setUri(String uri) {
        jsonObject.put("uri", Json.create(uri));
        return this;
    }

    public boolean isPassive() {
        return jsonObject.getBoolean("passive");
    }

    public DeviceResource setPassive(boolean passive) {
        jsonObject.put("passive", Json.create(passive));
        return this;
    }

    public boolean isConstant() {
        return jsonObject.get("constant").asBoolean();
    }

    public DeviceResource setConstant(boolean constant) {
        jsonObject.put("constant", Json.create(constant));
        return this;
    }
}
