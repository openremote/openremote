package org.openremote.model.alert;

import org.openremote.model.notification.SentNotification;

import javax.persistence.*;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "ALERT")
public class SentAlert {

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Column(name = "TITLE", length = 1024)
    public String title;

    @Column(name = "CONTENT", length = 131072)
    public String content;

    @Column(name = "ALERT_TRIGGER")
    public String alert_trigger;

    @Column(name = "SEVERITY", length = 50)
    @Enumerated(EnumType.STRING)
    public Alert.Severity severity;

    @Column(name = "STATUS", length = 10)
    @Enumerated(EnumType.STRING)
    public Alert.Status status;

    @Column(name = "STATUS_CHANGED_BY", length = 43)
    public String status_changed_by;

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


}
