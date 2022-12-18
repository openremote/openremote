package org.openremote.model.alert;

import org.openremote.model.notification.SentNotification;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "ALERT")
public class SentAlert {

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Column(name = "TITLE")
    protected String title;

    @Column(name = "CONTENT", length = 4096)
    protected String content;

    @NotNull
    @Column(name = "TRIGGER", length = 50)
    @Enumerated(EnumType.STRING)
    protected Alert.Trigger trigger;

    @NotNull
    @Column(name = "TRIGGER_ID", length = 50)
    protected String triggerId;

    @NotNull
    @Column(name = "SEVERITY", length = 50)
    @Enumerated(EnumType.STRING)
    protected Alert.Severity severity;

    @NotNull
    @Column(name = "STATUS", length = 10)
    @Enumerated(EnumType.STRING)
    protected Alert.Status status;

//    @NotNull
//    @Column(name = "STATUS_CHANGED_BY", length = 43)
//    public String status_changed_by;

    public Long getId() {
        return id;
    }

    public SentAlert setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() { return title; }

    public SentAlert setTitle(String title){
        this.title = title;
        return this;
    }

    public String getContent() { return content; }

    public SentAlert setContent(String content){
        this.content = content;
        return this;
    }

    public Alert.Trigger getTrigger() { return trigger; }

    public SentAlert setTrigger(Alert.Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    public String getTriggerId() { return triggerId; }

    public SentAlert setTriggerId(String triggerId) {
        this.triggerId = triggerId;
        return this;
    }

    public Alert.Severity getSeverity() { return severity; }

    public SentAlert setSeverity(Alert.Severity severity) {
        this.severity = severity;
        return this;
    }

    public Alert.Status getStatus() {
        return status;
    }

    public SentAlert setStatus(Alert.Status status) {
        this.status = status;
        return this;
    }


}
