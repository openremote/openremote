package org.openremote.model.rules.flow;

public class NodeConnection {
    private String from;
    private String to;

    public NodeConnection(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public NodeConnection() {
        from = null;
        to = null;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
