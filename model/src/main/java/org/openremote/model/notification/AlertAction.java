package org.openremote.model.notification;

import elemental.json.Json;
import elemental.json.JsonObject;


public class AlertAction {

    private JsonObject jsonObject ;

    public AlertAction() {
        jsonObject = Json.createObject();

    }

    public AlertAction(String title, ActionType type,String assetId, String attributeName, String rawJson) {
        this();
        setTitle(title);
        setType(type);
        setAssetId(assetId);
        setAttributeName(attributeName);
        setRawJson(rawJson);
    }

    private void setRawJson(String rawJson) {
        if (rawJson != null) {
            jsonObject.put("rawJson", Json.create(rawJson));
        } else if (jsonObject.hasKey("rawJson")) {
            jsonObject.remove("rawJson");
        }
    }

    private void setAttributeName(String attributeName) {
        if (attributeName != null) {
            jsonObject.put("attributeName", Json.create(attributeName));
        } else if (jsonObject.hasKey("attributeName")) {
            jsonObject.remove("attributeName");
        }
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

    public void setAssetId(String assetId) {
        if (assetId != null) {
            jsonObject.put("assetId", Json.create(assetId));
        } else if (jsonObject.hasKey("assetId")) {
            jsonObject.remove("assetId");
        }
    }
}
