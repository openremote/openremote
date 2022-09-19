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
import org.hibernate.annotations.Formula;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.calendar.CalendarEvent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Rules that can only be triggered by asset modifications in a particular
 * asset subtree, and can only modify asset data in a particular asset subtree.
 */
@Entity
@Table(name = "ASSET_RULESET")
public class AssetRuleset extends Ruleset {

    public static final String TYPE = "asset";

    @Column(name = "ASSET_ID", length = 22, nullable = false, columnDefinition = "char(22)")
    protected String assetId;

    @Column(name = "ACCESS_PUBLIC_READ", nullable = false)
    protected boolean accessPublicRead;

    @Formula("(select a.REALM from Asset a where a.ID = ASSET_ID)")
    protected String realm;

    public AssetRuleset() {}

    public AssetRuleset(String assetId, String name, Lang language, String rules) {
        super(name, language, rules);
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    public AssetRuleset setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public AssetRuleset setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public boolean isAccessPublicRead() {
        return accessPublicRead;
    }

    public AssetRuleset setAccessPublicRead(boolean accessPublicRead) {
        this.accessPublicRead = accessPublicRead;
        return this;
    }

    @Override
    public AssetRuleset setId(Long id) {
        super.setId(id);
        return this;
    }

    @Override
    public AssetRuleset setVersion(long version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public AssetRuleset setCreatedOn(Date createdOn) {
        super.setCreatedOn(createdOn);
        return this;
    }

    @Override
    public AssetRuleset setLastModified(Date lastModified) {
        super.setLastModified(lastModified);
        return this;
    }

    @Override
    public AssetRuleset setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public AssetRuleset setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        return this;
    }

    @Override
    public AssetRuleset setRules(String rules) {
        super.setRules(rules);
        return this;
    }

    @Override
    public AssetRuleset setLang(Lang lang) {
        super.setLang(lang);
        return this;
    }

    @Override
    public AssetRuleset setMeta(ObjectNode meta) {
        super.setMeta(meta);
        return this;
    }

    @Override
    public AssetRuleset setStatus(RulesetStatus status) {
        super.setStatus(status);
        return this;
    }

    @Override
    public AssetRuleset setError(String error) {
        super.setError(error);
        return this;
    }

    @Override
    public AssetRuleset setContinueOnError(boolean continueOnError) {
        super.setContinueOnError(continueOnError);
        return this;
    }

    @Override
    public AssetRuleset setValidity(CalendarEvent calendarEvent) {
        super.setValidity(calendarEvent);
        return this;
    }

    @Override
    public AssetRuleset setTriggerOnPredictedData(boolean triggerOnPredictedData) {
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
                ", assetId='" + assetId + '\'' +
                ", accessPublicRead='" + accessPublicRead + '\'' +
                '}';
    }
}
