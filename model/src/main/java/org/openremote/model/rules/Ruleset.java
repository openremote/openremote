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
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

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
    @JsonSubTypes.Type(value = TenantRuleset.class, name = TenantRuleset.TYPE),
    @JsonSubTypes.Type(value = GlobalRuleset.class, name = GlobalRuleset.TYPE)
})
public abstract class Ruleset {

    public static final MetaItemDescriptor<Boolean> SHOW_ON_LIST = new MetaItemDescriptor<>("showOnList", ValueType.BOOLEAN);

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
    protected MetaMap meta;

    @Transient
    protected RulesetStatus status;

    @Transient
    protected String error;

    public static final MetaItemDescriptor<Boolean> CONTINUE_ON_ERROR = new MetaItemDescriptor<>("continueOnError", ValueType.BOOLEAN);
    public static final MetaItemDescriptor<CalendarEvent> VALIDITY = new MetaItemDescriptor<>("validity", ValueType.CALENDAR_EVENT);
    public static final MetaItemDescriptor<Boolean> TRIGGER_ON_PREDICTED_DATA = new MetaItemDescriptor<>("triggerOnPredictedData", ValueType.BOOLEAN);

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

    public MetaMap getMeta() {
        if (meta == null) {
            meta = new MetaMap();
        }
        return meta;
    }

    public Ruleset setMeta(MetaMap meta) {
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
        return getMeta().get(CONTINUE_ON_ERROR).flatMap(MetaItem::getValue).orElse(false);
    }

    public Ruleset setContinueOnError(boolean continueOnError) {
        getMeta().set(CONTINUE_ON_ERROR, continueOnError);
        return this;
    }

    @JsonIgnore
    public CalendarEvent getValidity() {
        return getMeta().get(VALIDITY).flatMap(MetaItem::getValue).orElse(null);
    }

    @JsonIgnore
    public Ruleset setValidity(CalendarEvent calendarEvent) {
        getMeta().set(VALIDITY, calendarEvent);
        return this;
    }

    public boolean isTriggerOnPredictedData() {
        return getMeta().get(TRIGGER_ON_PREDICTED_DATA).flatMap(MetaItem::getValue).orElse(false);
    }

    public Ruleset setTriggerOnPredictedData(boolean triggerOnPredictedData) {
        getMeta().set(TRIGGER_ON_PREDICTED_DATA, triggerOnPredictedData);
        return this;
    }
}
