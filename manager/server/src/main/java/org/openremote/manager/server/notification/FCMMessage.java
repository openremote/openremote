package org.openremote.manager.server.notification;


public class FCMMessage {

    private String to;

    public FCMMessage(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
