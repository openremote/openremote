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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.Date;
import java.util.Optional;

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
            "                new AssetQuery().type(AssetType.THING).attributeValue(\"someAttribute\", \"foo\")\n" +
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
                "                    new AssetQuery().type(AssetType.THING).attributeValue(\"someAttribute\", \"foo\")\n" +
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
                );

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

    @Transient
    protected RulesetStatus status;

    @Transient
    protected String error;

    protected Ruleset() {
    }

    public Ruleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, Lang lang) {
        this(id, version, createdOn, lastModified, name, enabled, null, lang);
    }

    public Ruleset(long id, long version, Date createdOn, Date lastModified, String name, boolean enabled, String rules, Lang lang) {
        this.id = id;
        this.version = version;
        this.createdOn = createdOn;
        this.lastModified = lastModified;
        this.name = name;
        this.enabled = enabled;
        this.rules = rules;
        this.lang = lang;
    }

    public Ruleset(String name, String rules, Lang lang) {
        this.name = name;
        this.rules = rules;
        this.lang = lang;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    @PreUpdate
    @PrePersist
    public void updateLastModified() { // Don't call this setLastModified(), it confuses gwt-jackson
        setLastModified(new Date());
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lang getLang() {
        return lang;
    }

    public void setLang(Lang lang) {
        this.lang = lang;
    }

    public RulesetStatus getStatus() {
        return status;
    }

    public void setStatus(RulesetStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
