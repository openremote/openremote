package org.openremote.manager.server.notification;


public class FCMBaseMessage {

    private String to;

    public FCMBaseMessage(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
