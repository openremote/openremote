package org.openremote.android.service;


public class AlertAction {
    private AlertNotification.ActionType type;
    private String title;

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