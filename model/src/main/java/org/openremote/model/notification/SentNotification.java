/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.notification;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "NOTIFICATION")
public class SentNotification {

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Column(name = "NAME")
    protected String name;

    @NotNull
    @Column(name = "TYPE", nullable = false, length = 50)
    protected String type;

    @NotNull
    @Column(name = "TARGET", length = 50)
    @Enumerated(EnumType.STRING)
    protected Notification.TargetType target;

    @NotNull
    @Column(name = "TARGET_ID")
    protected String targetId;

    @NotNull()
    @Column(name = "SOURCE", length = 50)
    @Enumerated(EnumType.STRING)
    protected Notification.Source source;

    @Column(name = "SOURCE_ID", length = 43)
    protected String sourceId;

    @Column(name = "MESSAGE", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected AbstractNotificationMessage message;

    @Column(name = "ERROR", length = 4096)
    protected String error;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SENT_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Date sentOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DELIVERED_ON", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Date deliveredOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ACKNOWLEDGED_ON", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Date acknowledgedOn;

    @Column(name = "ACKNOWLEDGEMENT")
    protected String acknowledgement;

    public Long getId() {
        return id;
    }

    public SentNotification setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public SentNotification setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public SentNotification setType(String type) {
        this.type = type;
        return this;
    }

    public Date getSentOn() {
        return sentOn;
    }

    public SentNotification setSentOn(Date sentOn) {
        this.sentOn = sentOn;
        return this;
    }

    public Notification.Source getSource() {
        return source;
    }

    public SentNotification setSource(Notification.Source source) {
        this.source = source;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public SentNotification setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Notification.TargetType getTarget() {
        return target;
    }

    public SentNotification setTarget(Notification.TargetType target) {
        this.target = target;
        return this;
    }

    public String getTargetId() {
        return targetId;
    }

    public SentNotification setTargetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    public AbstractNotificationMessage getMessage() {
        return message;
    }

    public SentNotification setMessage(AbstractNotificationMessage message) {
        this.message = message;
        return this;
    }

    public Date getDeliveredOn() {
        return deliveredOn;
    }

    public SentNotification setDeliveredOn(Date deliveredOn) {
        this.deliveredOn = deliveredOn;
        return this;
    }

    public Date getAcknowledgedOn() {
        return acknowledgedOn;
    }

    public SentNotification setAcknowledgedOn(Date acknowledgedOn) {
        this.acknowledgedOn = acknowledgedOn;
        return this;
    }

    public String getAcknowledgement() {
        return acknowledgement;
    }

    public SentNotification setAcknowledgement(String acknowledgement) {
        this.acknowledgement = acknowledgement;
        return this;
    }

    public String getError() {
        return error;
    }

    public SentNotification setError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", target=" + target +
            ", targetId='" + targetId + '\'' +
            ", source=" + source +
            ", sourceId='" + sourceId + '\'' +
            ", message=" + message +
            ", sentOn=" + sentOn +
            ", deliveredOn=" + deliveredOn +
            ", acknowledgedOn=" + acknowledgedOn +
            ", acknowledgement='" + acknowledgement + '\'' +
            '}';
    }
}
