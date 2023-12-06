package org.openremote.model.alarm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

public class AlarmConfig implements Serializable {



    protected Alarm.Severity severity;
    protected String assigneeId;
    protected String content;

    @JsonCreator
    public AlarmConfig(@JsonProperty("severity") Alarm.Severity severity,
                       @JsonProperty("content") String content,
                       @JsonProperty("assigneeId") String assigneeId) {
        this.severity = severity;
        this.assigneeId = assigneeId;
        this.content = content;
    }

    public String getAssigneeId() { return this.assigneeId; }
    public String getContent() { return this.content; }

    public Alarm.Severity getSeverity() { return this.severity; }

    public AlarmConfig setSeverity(Alarm.Severity severity) {
        this.severity = severity;
        return this;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName()+ "{" +
                "severity=" + this.severity +
                ", assigneeId=" + this.assigneeId +
                ", content=" + this.content +
                '}';
    }
}
