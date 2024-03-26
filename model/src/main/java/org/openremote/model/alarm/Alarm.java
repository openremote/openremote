package org.openremote.model.alarm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Alarm {
    public enum Source {
        MANUAL,
        CLIENT,
        GLOBAL_RULESET,
        REALM_RULESET,
        ASSET_RULESET,
        AGENT
    }
    public enum Status {
        OPEN,
        ACKNOWLEDGED,
        IN_PROGRESS,
        CLOSED,
        RESOLVED
    }
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public static final String HEADER_SOURCE = Alarm.class.getName() + ".SOURCE";
    public static final String HEADER_SOURCE_ID = Alarm.class.getName() + ".SOURCEID";

    protected String title;
    protected String content;
    protected Severity severity;
    protected Status status;
    protected String assigneeId;
    protected String realm;
    protected String sourceId;
    protected Source source;

    @JsonCreator
    public Alarm(@JsonProperty("title") String title,
                 @JsonProperty("content") String content,
                 @JsonProperty("severity") Severity severity,
                 @JsonProperty("assigneeId") String assignee,
                 @JsonProperty("realm") String realm){
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.status = Status.OPEN;
        if(assignee != null){
            this.assigneeId = assignee;
        }
        this.realm = realm;
    }

    @JsonCreator
    public Alarm() {

    }

    public String getTitle() { return this.title; }

    public Alarm setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getContent() { return this.content; }

    public Alarm setContent(String content) {
        this.content = content;
        return this;
    }

    public Severity getSeverity() { return this.severity; }

    public Alarm setSeverity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public Status getStatus() { return this.status; }

    public Alarm setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getAssignee() { return this.assigneeId; }

    public  Alarm setAssignee(String assignee) {
        this.assigneeId = assignee;
        return this;
    }

    public String getRealm() { return this.realm; }

    public  Alarm setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public Source getSource() {
        return source;
    }

    public Alarm setSource(Source source) {
        this.source = source;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Alarm setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+ "{" +
                "title=" + this.title + '\'' +
                ", context=" + this.content +
                ", severity=" + this.severity +
                ", status=" + this.status +
                '}';
    }
}
