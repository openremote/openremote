/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.asset;

import org.openremote.model.event.shared.AssetInfo;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.TextUtil;

import java.util.Arrays;
import java.util.List;

// TODO: Merge this with AssetQuery and use AssetQueryPredicate to resolve
public class AssetFilter<T extends SharedEvent & AssetInfo> extends EventFilter<T> {

    public static final String FILTER_TYPE = "asset";

    protected String[] assetIds;
    protected String realm;
    protected String[] parentIds;
    protected String[] path;
    protected String[] attributeNames;
    protected boolean publicEvents;
    protected boolean restrictedEvents;

    public AssetFilter() {
    }

    public AssetFilter(String... assetIds) {
        this.assetIds = assetIds;
    }

    public String[] getAssetIds() {
        return assetIds;
    }

    public AssetFilter<T> setAssetIds(String... assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public AssetFilter<T> setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public String[] getParentIds() {
        return parentIds;
    }

    public AssetFilter<T> setParentIds(String... parentIds) {
        this.parentIds = parentIds;
        return this;
    }

    public String[] getPath() {
        return path;
    }

    public AssetFilter<T> setPath(String[] path) {
        this.path = path;
        return this;
    }

    public String[] getAttributeNames() {
        return attributeNames;
    }

    public AssetFilter<T> setAttributeNames(String... attributeNames) {
        this.attributeNames = attributeNames;
        return this;
    }

    public boolean isPublicEvents() {
        return publicEvents;
    }

    public AssetFilter<T> setPublicEvents(boolean publicEvents) {
        this.publicEvents = publicEvents;
        return this;
    }

    public boolean isRestrictedEvents() {
        return restrictedEvents;
    }

    public AssetFilter<T> setRestrictedEvents(boolean restrictedEvents) {
        this.restrictedEvents = restrictedEvents;
        return this;
    }

    @Override
    public String getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public boolean apply(T event) {

        if (restrictedEvents && !event.canAccessRestrictedRead()) {
            return false;
        }

        if (publicEvents && !event.canAccessPublicRead()) {
            return false;
        }

        if (assetIds != null && assetIds.length > 0) {
            if (!Arrays.asList(assetIds).contains(event.getAssetId())) {
                return false;
            }
        }

        if (parentIds != null && parentIds.length > 0) {
            if (!Arrays.asList(parentIds).contains(event.getParentId())) {
                return false;
            }
        }

        if(path != null && path.length > 0) {
            List<String> pathList = Arrays.asList(event.getPath());
            if (Arrays.stream(path).noneMatch(pathList::contains)) {
                return false;
            }
        }

        if (!TextUtil.isNullOrEmpty(realm)) {
            if (!realm.equals(event.getRealm())) {
                return false;
            }
        }

        if (attributeNames != null && attributeNames.length > 0) {
            List<String> eventAttributeNames = Arrays.asList(event.getAttributeNames());
            return Arrays.stream(attributeNames).anyMatch(eventAttributeNames::contains);
        }

        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetIds='" + (assetIds != null ? Arrays.toString(assetIds) : "") + '\'' +
            ", parentIds='" + (parentIds != null ? Arrays.toString(parentIds) : "") + '\'' +
            ", realm='" + realm + '\'' +
            ", attributeNames='" + (attributeNames != null ? Arrays.toString(attributeNames) : "") + '\'' +
            '}';
    }
}
