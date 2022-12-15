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

    protected String title;
    protected String content;
    //TODO: Add trigger type Enum/Class and field
    protected String trigger;
    protected Severity severity;
    protected Status status;

    @JsonCreator
    public Alert(@JsonProperty("title") String title,
                 @JsonProperty("content") String content,
                 @JsonProperty("trigger") String trigger,
                 @JsonProperty("severity") Severity severity,
                 @JsonProperty("status") Status status) {
        this.title = title;
        this.content = content;
        this.trigger = trigger;
        this.severity = severity;
        this.status = status;
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

    public String getTrigger() { return trigger; }

    public Alert setTrigger(String trigger) {
        this.trigger = trigger;
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
                ", trigger=" + trigger +
                ", severity=" + severity +
                ", status=" + status +
                '}';
    }
}
