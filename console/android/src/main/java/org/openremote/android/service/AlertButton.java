package org.openremote.android.service;

import java.io.Serializable;

public class AlertButton implements Serializable {
    private String title;
    private AlertAction action;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AlertAction getAction() {
        return action;
    }

    public void setAction(AlertAction action) {
        this.action = action;
    }
}
