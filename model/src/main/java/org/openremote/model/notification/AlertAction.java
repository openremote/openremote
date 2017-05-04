package org.openremote.model.notification;

import org.openremote.model.AbstractTypeHolder;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;


/**
 * Wraps {@link ObjectValue}.
 */
public class AlertAction extends AbstractTypeHolder {


    public AlertAction() {
        super(Values.createObject());
    }

    public AlertAction(String title, ActionType type, String assetId, String attributeName, String rawJson) {
        this();
        setTitle(title);
        setActionType(type);
        setAssetId(assetId);
        setAttributeName(attributeName);
        setRawJson(rawJson);
    }

    private void setRawJson(String rawJson) {
        if (rawJson != null) {
            objectValue.put("rawJson", Values.create(rawJson));
        } else if (objectValue.hasKey("rawJson")) {
            objectValue.remove("rawJson");
        }
    }

    private void setAttributeName(String attributeName) {
        if (attributeName != null) {
            objectValue.put("attributeName", Values.create(attributeName));
        } else if (objectValue.hasKey("attributeName")) {
            objectValue.remove("attributeName");
        }
    }

    public String getTitle() {
        return objectValue.getString("title").orElse(null);
    }

    public void setTitle(String title) {
        if (title != null) {
            objectValue.put("title", Values.create(title));
        } else if (objectValue.hasKey("title")) {
            objectValue.remove("title");
        }
    }

    public ActionType getActionType() {
        return getType().map(ActionType::valueOf).orElse(null);
    }

    public void setActionType(ActionType type) {
        setType(type != null ? type.name() : null);
    }

    public void setAssetId(String assetId) {
        if (assetId != null) {
            objectValue.put("assetId", Values.create(assetId));
        } else if (objectValue.hasKey("assetId")) {
            objectValue.remove("assetId");
        }
    }
}
