/*
 * Copyright 2019, OpenRemote Inc.
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

import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that can only be triggered by asset modifications in a particular
 * realm, and can only modify asset data in a particular realm.
 */
@Entity
@Table(name = "TENANT_RULESET")
public class TenantRuleset extends Ruleset {

    public static final String TYPE = "tenant";

    @Column(name = "REALM", nullable = false)
    protected String realm;

    @Column(name = "ACCESS_PUBLIC_READ", nullable = false)
    protected boolean accessPublicRead;

    public TenantRuleset() {}

    public TenantRuleset(String realm, String name, Lang language, String rules) {
        super(name, language, rules);
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public TenantRuleset setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public boolean isAccessPublicRead() {
        return accessPublicRead;
    }

    public TenantRuleset setAccessPublicRead(boolean accessPublicRead) {
        this.accessPublicRead = accessPublicRead;
        return this;
    }

    @Override
    public TenantRuleset setId(Long id) {
        super.setId(id);
        return this;
    }

    @Override
    public TenantRuleset setVersion(long version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public TenantRuleset setCreatedOn(Date createdOn) {
        super.setCreatedOn(createdOn);
        return this;
    }

    @Override
    public TenantRuleset setLastModified(Date lastModified) {
        super.setLastModified(lastModified);
        return this;
    }

    @Override
    public TenantRuleset setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TenantRuleset setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        return this;
    }

    @Override
    public TenantRuleset setRules(String rules) {
        super.setRules(rules);
        return this;
    }

    @Override
    public TenantRuleset setLang(Lang lang) {
        super.setLang(lang);
        return this;
    }

    @Override
    public TenantRuleset setMeta(ObjectValue meta) {
        super.setMeta(meta);
        return this;
    }

    @Override
    public TenantRuleset addMeta(String key, Value value) {
        super.addMeta(key, value);
        return this;
    }

    @Override
    public TenantRuleset removeMeta(String key) {
        super.removeMeta(key);
        return this;
    }

    @Override
    public TenantRuleset setStatus(RulesetStatus status) {
        super.setStatus(status);
        return this;
    }

    @Override
    public TenantRuleset setError(String error) {
        super.setError(error);
        return this;
    }

    @Override
    public TenantRuleset setContinueOnError(boolean continueOnError) {
        super.setContinueOnError(continueOnError);
        return this;
    }

    @Override
    public TenantRuleset setValidity(CalendarEvent calendarEvent) {
        super.setValidity(calendarEvent);
        return this;
    }

    @Override
    public TenantRuleset setTriggerOnPredictedData(boolean triggerOnPredictedData) {
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
            ", realm='" + realm + '\'' +
            ", accessPublicRead='" + accessPublicRead + '\'' +
            '}';
    }
}
