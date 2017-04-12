package org.openremote.android.service;


import java.io.Serializable;

public class AlertAction implements Serializable {
    private AlertNotification.ActionType type;
    private String title;
    private String assetId;
    private String attributeName;
    private String rawJson;

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public AlertAction() {
    }

    public AlertNotification.ActionType getType() {
        return type;
    }

    public void setType(AlertNotification.ActionType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "AlertAction{" +
                "type=" + type +
                ", title='" + title + '\'' +
                '}';
    }
}