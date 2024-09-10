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

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

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
        RESOLVED,
        CLOSED
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public static final String HEADER_SOURCE = Alarm.class.getName() + ".SOURCE";
    public static final String HEADER_SOURCE_ID = Alarm.class.getName() + ".SOURCEID";

    @NotNull
    protected String title;

    protected String content;

    @NotNull
    protected Severity severity;

    @NotNull
    protected Status status;

    protected String assigneeId;

    @NotNull
    protected String realm;

    @NotNull
    protected String sourceId;

    @NotNull
    protected Source source;

    @JsonCreator
    public Alarm(String title, String content, Severity severity, String assigneeId, String realm) {
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.status = Status.OPEN;
        this.assigneeId = assigneeId;
        this.realm = realm;
    }

    @JsonCreator
    public Alarm() {

    }

    public String getTitle() {
        return this.title;
    }

    public Alarm setTitle(@NotNull String title) {
        this.title = title;
        return this;
    }

    public String getContent() {
        return this.content;
    }

    public Alarm setContent(String content) {
        this.content = content;
        return this;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public Alarm setSeverity(@NotNull Severity severity) {
        this.severity = severity;
        return this;
    }

    public Status getStatus() {
        return this.status;
    }

    public Alarm setStatus(@NotNull Status status) {
        this.status = status;
        return this;
    }

    public String getAssigneeId() {
        return this.assigneeId;
    }

    public Alarm setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public String getRealm() {
        return this.realm;
    }

    public Alarm setRealm(@NotNull String realm) {
        this.realm = realm;
        return this;
    }

    public Source getSource() {
        return source;
    }

    public Alarm setSource(@NotNull Source source) {
        this.source = source;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Alarm setSourceId(@NotNull String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", severity=" + severity +
                ", status=" + status +
                ", assigneeId='" + assigneeId + '\'' +
                ", realm='" + realm + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", source=" + source +
                '}';
    }

}
