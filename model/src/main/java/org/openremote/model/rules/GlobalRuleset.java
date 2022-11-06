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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.calendar.CalendarEvent;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that apply to all realms and assets, for the whole system.
 */
@Entity
@Table(name = "GLOBAL_RULESET")
public class GlobalRuleset extends Ruleset {

    public static final String TYPE = "global";

    public GlobalRuleset() {
    }

    public GlobalRuleset(String name, Lang language, String rules) {
        super(name, language, rules);
    }

    @Override
    public GlobalRuleset setId(Long id) {
        super.setId(id);
        return this;
    }

    @Override
    public GlobalRuleset setVersion(long version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public GlobalRuleset setCreatedOn(Date createdOn) {
        super.setCreatedOn(createdOn);
        return this;
    }

    @Override
    public GlobalRuleset setLastModified(Date lastModified) {
        super.setLastModified(lastModified);
        return this;
    }

    @Override
    public GlobalRuleset setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public GlobalRuleset setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        return this;
    }

    @Override
    public GlobalRuleset setRules(String rules) {
        super.setRules(rules);
        return this;
    }

    @Override
    public GlobalRuleset setLang(Lang lang) {
        super.setLang(lang);
        return this;
    }

    @Override
    public GlobalRuleset setMeta(ObjectNode meta) {
        super.setMeta(meta);
        return this;
    }

    @Override
    public GlobalRuleset setStatus(RulesetStatus status) {
        super.setStatus(status);
        return this;
    }

    @Override
    public GlobalRuleset setError(String error) {
        super.setError(error);
        return this;
    }

    @Override
    public GlobalRuleset setContinueOnError(boolean continueOnError) {
        super.setContinueOnError(continueOnError);
        return this;
    }

    @Override
    public GlobalRuleset setValidity(CalendarEvent calendarEvent) {
        super.setValidity(calendarEvent);
        return this;
    }

    @Override
    public GlobalRuleset setTriggerOnPredictedData(boolean triggerOnPredictedData) {
        super.setTriggerOnPredictedData(triggerOnPredictedData);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", lang='" + lang + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", lastModified='" + lastModified + '\'' +
            ", enabled='" + enabled + '\'' +
            ", meta='" + meta + '\'' +
            '}';
    }
}
