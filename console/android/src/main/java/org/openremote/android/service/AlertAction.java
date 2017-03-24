package org.openremote.android.service;


public class AlertAction {
    private AlertNotification.ActionType type;
    private String name;

    public AlertAction() {
    }

    public AlertNotification.ActionType getType() {
        return type;
    }

    public void setType(AlertNotification.ActionType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AlertAction{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}