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
package org.openremote.model.rules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.util.ValueUtil;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Optional;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

/**
 * Rules can be defined in three scopes: global, for a realm, for an asset sub-tree.
 */
@MappedSuperclass
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssetRuleset.class, name = AssetRuleset.TYPE),
    @JsonSubTypes.Type(value = RealmRuleset.class, name = RealmRuleset.TYPE),
    @JsonSubTypes.Type(value = GlobalRuleset.class, name = GlobalRuleset.TYPE)
})
public abstract class Ruleset {

    public static final String SHOW_ON_LIST = "showOnList";
    public static final String CONTINUE_ON_ERROR = "continueOnError";
    public static final String VALIDITY = "validity";
    public static final String TRIGGER_ON_PREDICTED_DATA = "triggerOnPredictedData";

    public enum Lang {
        JAVASCRIPT,
        GROOVY,
        JSON,
        FLOW
    }

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_MODIFIED", nullable = false, columnDefinition= "TIMESTAMP WITH TIME ZONE")
    protected Date lastModified;

    @NotNull(message = "{Ruleset.name.NotNull}")
    @Column(name = "NAME", nullable = false)
    @Size(min = 3, max = 255, message = "{Ruleset.name.Size}")
    protected String name;

    @Column(name = "ENABLED", nullable = false)
    protected boolean enabled = true;

    @Column(name = "RULES", nullable = false)
    protected String rules;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "RULES_LANG", nullable = false)
    protected Lang lang = Lang.GROOVY;

    @Column(name = "META", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected ObjectNode meta;

    @Transient
    protected RulesetStatus status;

    @Transient
    protected String error;

    protected Ruleset() {
    }

    protected Ruleset(String name, Lang language, String rules) {
        this.name = name;
        if (language != null) {
            this.lang = language;
        }
        this.rules = rules;
    }

    public Long getId() {
        return id;
    }

    public Ruleset setId(Long id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public Ruleset setVersion(long version) {
        this.version = version;
        return this;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Ruleset setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @PreUpdate
    @PrePersist
    protected void updateLastModified() {
        setLastModified(new Date());
    }

    public Ruleset setLastModified() {
        return setLastModified(new Date());
    }

    public Ruleset setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public Ruleset setName(String name) {
        this.name = name;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Ruleset setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getRules() {
        return rules;
    }

    public Ruleset setRules(String rules) {
        this.rules = rules;
        return this;
    }

    public Lang getLang() {
        return lang;
    }

    public Ruleset setLang(Lang lang) {
        this.lang = lang;
        return this;
    }

    public ObjectNode getMeta() {
        if (meta == null) {
            meta = ValueUtil.createJsonObject();
        }
        return meta;
    }

    public Ruleset setMeta(ObjectNode meta) {
        this.meta = meta;
        return this;
    }

    public RulesetStatus getStatus() {
        return status;
    }

    public Ruleset setStatus(RulesetStatus status) {
        this.status = status;
        return this;
    }

    public String getError() {
        return error;
    }

    public Ruleset setError(String error) {
        this.error = error;
        return this;
    }

    public boolean isContinueOnError() {
        return Optional.ofNullable(getMeta().get(CONTINUE_ON_ERROR)).map(node -> node.asBoolean(false)).orElse(false);
    }

    public Ruleset setContinueOnError(boolean continueOnError) {
        getMeta().set(CONTINUE_ON_ERROR, BooleanNode.valueOf(continueOnError));
        return this;
    }

    @JsonIgnore
    public CalendarEvent getValidity() {
        return Optional.ofNullable(getMeta().get(VALIDITY)).map(node -> ValueUtil.convert(node, CalendarEvent.class)).orElse(null);
    }

    @JsonIgnore
    public Ruleset setValidity(CalendarEvent calendarEvent) {
        getMeta().set(VALIDITY, ValueUtil.getValue(calendarEvent, ObjectNode.class, true).orElse(null));
        return this;
    }

    public boolean isTriggerOnPredictedData() {
        return Optional.ofNullable(getMeta().get(TRIGGER_ON_PREDICTED_DATA)).map(node -> node.asBoolean(false)).orElse(false);
    }

    public Ruleset setTriggerOnPredictedData(boolean triggerOnPredictedData) {
        getMeta().set(TRIGGER_ON_PREDICTED_DATA, BooleanNode.valueOf(triggerOnPredictedData));
        return this;
    }

    public boolean isShowOnList() {
        return Optional.ofNullable(getMeta().get(SHOW_ON_LIST)).map(node -> node.asBoolean(false)).orElse(false);
    }

    public Ruleset setShowOnList(boolean showOn) {
        getMeta().set(SHOW_ON_LIST, BooleanNode.valueOf(showOn));
        return this;
    }
}
