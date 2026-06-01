/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.alarm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Formula;
import org.openremote.model.asset.Asset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

@Entity
@Table(name = "ALARM")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class SentAlarm {
    @Id
    @JsonProperty
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    @SequenceGenerator(name = PERSISTENCE_SEQUENCE_ID_GENERATOR, initialValue = 1000, allocationSize = 1)
    protected Long id;

    @NotNull
    @JsonProperty
    @Column(name = "REALM", nullable = false, updatable = false)
    protected String realm;

    @JsonProperty
    @Column(name = "TITLE", nullable = false)
    protected String title;

    @JsonProperty
    @Column(name = "CONTENT", length = 4096)
    protected String content;

    @NotNull
    @JsonProperty
    @Column(name = "SEVERITY", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    protected Alarm.Severity severity;

    @NotNull
    @JsonProperty
    @Column(name = "STATUS", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    protected Alarm.Status status;

    @NotNull()
    @JsonProperty
    @Column(name = "SOURCE", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    protected Alarm.Source source;

    @JsonProperty
    @Column(name = "SOURCE_ID", nullable = false, length = 43)
    protected String sourceId;

    @JsonProperty
    @Formula("(select u.USERNAME from PUBLIC.USER_ENTITY u where u.ID = SOURCE_ID)")
    protected String sourceUsername;

    @JsonProperty
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Instant createdOn;

    @JsonProperty
    @Column(name = "ACKNOWLEDGED_ON", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Instant acknowledgedOn;

    @JsonProperty
    @Column(name = "LAST_MODIFIED", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected Instant lastModified;

    @JsonProperty
    @Column(name = "ASSIGNEE_ID")
    protected String assigneeId;

    @JsonProperty
    @Formula("(select u.USERNAME from PUBLIC.USER_ENTITY u where u.ID = ASSIGNEE_ID)")
    protected String assigneeUsername;

    @JsonProperty
    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ALARM_ASSET_LINK")
    protected List<Asset<?>> asset = new ArrayList<>();

    @JsonProperty
    public Long getId() {
        return id;
    }

    public SentAlarm setId(Long id) {
        this.id = id;
        return this;
    }

    @JsonProperty
    public String getRealm() {
        return realm;
    }

    public SentAlarm setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    @JsonProperty
    public String getTitle() {
        return title;
    }

    public SentAlarm setTitle(String title) {
        this.title = title;
        return this;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }

    public SentAlarm setContent(String content) {
        this.content = content;
        return this;
    }

    @JsonProperty
    public Alarm.Severity getSeverity() {
        return severity;
    }

    public SentAlarm setSeverity(Alarm.Severity severity) {
        this.severity = severity;
        return this;
    }

    @JsonProperty
    public Alarm.Status getStatus() {
        return status;
    }

    public SentAlarm setStatus(Alarm.Status status) {
        this.status = status;
        return this;
    }

    @JsonProperty
    public Alarm.Source getSource() {
        return source;
    }

    public SentAlarm setSource(Alarm.Source source) {
        this.source = source;
        return this;
    }

    @JsonProperty
    public String getSourceId() {
        return sourceId;
    }

    public SentAlarm setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    @JsonProperty
    public String getSourceUsername() {
        return sourceUsername;
    }

    @JsonProperty
    public Instant getCreatedOn() {
        return createdOn;
    }

    public SentAlarm setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @JsonProperty
    public Instant getAcknowledgedOn() {
        return acknowledgedOn;
    }

    public SentAlarm setAcknowledgedOn(Instant acknowledgedOn) {
        this.acknowledgedOn = acknowledgedOn;
        return this;
    }

    @JsonProperty
    public Instant getLastModified() {
        return lastModified;
    }

    public SentAlarm setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    @JsonProperty
    public String getAssigneeId() {
        return assigneeId;
    }

    public SentAlarm setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    @JsonProperty
    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    @JsonProperty
    @JsonIgnore
    public List<Asset<?>> getAsset() {
        return asset;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", severity=" + severity + '\'' +
                ", status='" + status + '\'' +
                ", source=" + source +
                ", sourceId='" + sourceId + '\'' +
                ", createdOn=" + createdOn + '\'' +
                ", acknowledgedOn=" + acknowledgedOn + '\'' +
                ", lastModified=" + lastModified + '\'' +
                ", assigneeId=" + assigneeId + '\'' +
                '}';
    }
}
