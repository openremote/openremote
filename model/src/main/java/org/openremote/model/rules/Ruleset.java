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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Optional;

import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_SEQUENCE_ID_GENERATOR;

/**
 * Rules can be defined in three scopes: global, for a realm, for an asset sub-tree.
 */
@MappedSuperclass
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssetRuleset.class, name = AssetRuleset.TYPE),
    @JsonSubTypes.Type(value = TenantRuleset.class, name = TenantRuleset.TYPE),
    @JsonSubTypes.Type(value = GlobalRuleset.class, name = GlobalRuleset.TYPE)
})
public abstract class Ruleset {

    public enum Lang {
        JAVASCRIPT(".js",
            "rules = [ // An array of rules, add more objects to add more rules\n" +
            "    {\n" +
            "        name: \"Set bar to foo on someAttribute\",\n" +
            "        description: \"An example rule that sets 'bar' on someAttribute when it is 'foo'\",\n" +
            "        when: function(facts) {\n" +
            "            return facts.matchAssetState(\n" +
            "                new AssetQuery().types(AssetType.THING).attributeValue(\"someAttribute\", \"foo\")\n" +
            "            ).map(function(thing) {\n" +
            "                facts.bind(\"assetId\", thing.id);\n" +
            "                return true;\n" +
            "            }).orElse(false);\n" +
            "        },\n" +
            "        then: function(facts) {\n" +
            "            facts.updateAssetState(\n" +
            "                facts.bound(\"assetId\"), \"someAttribute\", \"bar\"\n" +
            "            );\n" +
            "        }\n" +
            "    }\n" +
            "]"),
        GROOVY(".groovy",
            "package demo.rules\n" +
                "\n" +
                "import org.openremote.manager.rules.RulesBuilder\n" +
                "import org.openremote.model.asset.*\n" +
                "import org.openremote.model.attribute.*\n" +
                "import org.openremote.model.value.*\n" +
                "import org.openremote.model.rules.*\n" +
                "import java.util.logging.*\n" +
                "\n" +
                "Logger LOG = binding.LOG\n" +
                "RulesBuilder rules = binding.rules\n" +
                "Assets assets = binding.assets\n" +
                "\n" +
                "rules.add()\n" +
                "        .name(\"Set bar to foo on someAttribute\")\n" +
                "        .description(\"An example rule that sets 'bar' on someAttribute when it is 'foo'\")\n" +
                "        .when(\n" +
                "        { facts ->\n" +
                "            facts.matchFirstAssetState(\n" +
                "                    new AssetQuery().types(AssetType.THING).attributeValue(\"someAttribute\", \"foo\")\n" +
                "            ).map { thing ->\n" +
                "                facts.bind(\"assetId\", thing.id)\n" +
                "                true\n" +
                "            }.orElse(false)\n" +
                "        })\n" +
                "        .then(\n" +
                "        { facts ->\n" +
                "            facts.updateAssetState(\n" +
                "                    facts.bound(\"assetId\") as String, \"someAttribute\", \"bar\"\n" +
                "            )\n" +
                "        })"),
        JSON(".json",
            "locations: [],\n" +
                    "rules: [\n" +
                    "  {\n" +
                    "    name: \"Test Rule\",\n" +
                    "    when: {\n" +
                    "        operator: \"AND\",\n" +
                    "        predicates: [\n" +
                    "          {\n" +
                    "            query: {\n" +
                    "\n" +
                    "            }\n" +
                    "          }\n" +
                    "        ],\n" +
                    "        conditions: []\n" +
                    "    },\n" +
                    "    then: {\n" +
                    "            notifications: [],\n" +
                    "            attributeEvents: []\n" +
                    "    },\n" +
                    "    reset: null // Only trigger once ever\n" +
                    "  }\n" +
                    "]"
                ),
        FLOW(".json", "{\"name\":\"\", \"description\": \"\", \"nodes\":[], \"connections\":[]}");

        final String fileExtension;
        final String emptyRulesExample;

        Lang(String fileExtension, String emptyRulesExample) {
            this.fileExtension = fileExtension;
            this.emptyRulesExample = emptyRulesExample;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public String getEmptyRulesExample() {
            return emptyRulesExample;
        }

        static public Optional<Lang> valueOfFileName(String fileName) {
            for (Lang lang : values()) {
                if (fileName.endsWith(lang.getFileExtension()))
                    return Optional.of(lang);
            }
            return Optional.empty();
        }
    }

    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = PERSISTENCE_SEQUENCE_ID_GENERATOR)
    protected Long id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
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
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    protected ObjectValue meta;

    @Transient
    protected RulesetStatus status;

    @Transient
    protected String error;

    public static final String META_KEY_CONTINUE_ON_ERROR = "urn:openremote:rule:meta:continueOnError";
    public static final String META_KEY_VALIDITY = "urn:openremote:rule:meta:validity";

    protected Ruleset() {
    }

    protected Ruleset(String name, Lang language, String rules) {
        this.name = name;
        this.lang = language;
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
    public void updateLastModified() { // Don't call this setLastModified(), it confuses gwt-jackson
        setLastModified(new Date());
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

    public ObjectValue getMeta() {
        return meta;
    }

    public Optional<Value> getMeta(String key) {
        return meta != null ? meta.get(key) : Optional.empty();
    }

    public Ruleset setMeta(ObjectValue meta) {
        this.meta = meta;
        return this;
    }

    public Ruleset addMeta(String key, Value value) {
        if (meta == null) {
            meta = Values.createObject();
        }
        meta.put(key, value);
        return this;
    }

    public Ruleset removeMeta(String key) {
        if (meta == null) {
            return this;
        }
        meta.remove(key);
        return this;
    }

    public boolean hasMeta(String key) {
        return meta != null && meta.hasKey(key);
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
        return Values.getObject(meta).flatMap(objValue -> objValue.getBoolean(META_KEY_CONTINUE_ON_ERROR)).orElse(false);
    }

    public Ruleset setContinueOnError(boolean continueOnError) {
        if (meta == null) {
            meta = Values.createObject();
        }
        if (!continueOnError) {
            meta.remove(META_KEY_CONTINUE_ON_ERROR);
        } else {
            meta.put(META_KEY_CONTINUE_ON_ERROR, true);
        }
        return this;
    }

    public CalendarEvent getValidity() {
        return Values.getObject(meta).flatMap(objValue -> objValue.getObject(META_KEY_VALIDITY)).flatMap(CalendarEvent::fromValue).orElse(null);
    }

    public Ruleset setValidity(CalendarEvent calendarEvent) {
        if (meta == null) {
            meta = Values.createObject();
        }
        if (calendarEvent == null) {
            meta.remove(META_KEY_VALIDITY);
        } else {
            meta.put(META_KEY_VALIDITY, calendarEvent.toValue());
        }
        return this;
    }
}
