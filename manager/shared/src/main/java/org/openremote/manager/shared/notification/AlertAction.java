package org.openremote.manager.shared.notification;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;


public class AlertAction {

    private JsonObject jsonObject ;

    public AlertAction() {
        jsonObject = Json.createObject();

    }

    public String getTitle() {
        if (jsonObject.hasKey("title")) {
            return jsonObject.getString("title");
        } else {
            return null;
        }
    }

    public void setTitle(String title) {
        if (title != null) {
            jsonObject.put("title", Json.create(title));
        } else if (jsonObject.hasKey("title")) {
            jsonObject.remove("title");
        }
    }

    public ActionType getType() {
        if (jsonObject.hasKey("type")) {
            return ActionType.valueOf(jsonObject.getString("type"));
        } else {
            return null;
        }
    }

    public void setType(ActionType type) {
        if (type != null) {
            jsonObject.put("type", Json.create(type.name()));
        } else if (jsonObject.hasKey("type")) {
            jsonObject.remove("type");
        }
    }

    public JsonObject getValue() {
        return jsonObject;
    }
}
