package org.openremote.manager.shared.device;

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
        return jsonObject.getString("description");
    }

    public DeviceResource setDescription(String description) {
        jsonObject.put("description", description);
        return this;
    }

    public Access getAccess() {
        String accessStr = jsonObject.get("access");
        return accessStr != null ? Access.valueOf(accessStr) : null;
    }

    public DeviceResource setAccess(Access access) {
        jsonObject.put("access", access.toString());
        return this;
    }

    public String getUnits() {
        return jsonObject.get("units");
    }

    public DeviceResource setUnits(String units) {
        jsonObject.put("units", units);
        return this;
    }

    public String getUri() {
        return jsonObject.get("uri");
    }

    public DeviceResource setUri(String uri) {
        jsonObject.put("uri", uri);
        return this;
    }

    public boolean isPassive() {
        return jsonObject.getBoolean("passive");
    }

    public DeviceResource setPassive(boolean passive) {
        jsonObject.put("passive", passive);
        return this;
    }

    public boolean isConstant() {
        return jsonObject.getBoolean("constant");
    }

    public DeviceResource setConstant(boolean constant) {
        jsonObject.put("constant", constant);
        return this;
    }
}
