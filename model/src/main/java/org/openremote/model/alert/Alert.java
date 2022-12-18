package org.openremote.model.alert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.units.qual.A;

public class Alert {

    public enum Severity{
        LOW,
        MEDIUM,
        HIGH
    }

    public enum Status{
        OPEN,
        CLOSED
    }

    public enum Trigger {
        INTERNAL,
        CLIENT,
        GLOBAL_RULESET,
        REALM_RULESET,
        ASSET_RULESET
    }

    public static final String HEADER_TRIGGER = Alert.class.getName() + ".TRIGGER";
    public static final String HEADER_TRIGGER_ID = Alert.class.getName() + ".TRIGGERID";

    protected String title;
    protected String content;
    protected Severity severity;
    protected Status status;

    @JsonCreator
    public Alert(@JsonProperty("title") String title,
                 @JsonProperty("content") String content,
                 @JsonProperty("severity") Severity severity) {
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.status = Status.OPEN;
    }

    public Alert() {

    }

    public String getTitle() { return title; }

    public Alert setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getContent() { return content; }

    public Alert setContent(String content) {
        this.content = content;
        return this;
    }

    public Severity getSeverity() { return severity; }

    public Alert setSeverity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public Status getStatus() { return status; }

    public Alert setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+ "{" +
                "title=" + title + '\'' +
                ", context=" + content +
                ", severity=" + severity +
                ", status=" + status +
                '}';
    }
}
