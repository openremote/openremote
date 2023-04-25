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

import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.AssetInfo;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TsIgnoreTypeParams;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.MetaItemType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Merge this with AssetQuery and use AssetQueryPredicate to resolve
@TsIgnoreTypeParams
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

    @SuppressWarnings("unchecked")
    @Override
    public T apply(T event) {

        MetaItemDescriptor<?> filterAttributesBy = null;

        if (restrictedEvents) {
            if (event instanceof AttributeEvent attributeEvent) {
                if (!attributeEvent.isRestrictedRead()) {
                    return null;
                }
            } else {
                filterAttributesBy = MetaItemType.ACCESS_RESTRICTED_READ;
            }
        }

        if (publicEvents) {
            if (event instanceof AttributeEvent attributeEvent) {
                if (!attributeEvent.isPublicRead()) {
                    return null;
                }
            } else if (event instanceof AssetEvent assetEvent) {
                if (!assetEvent.isAccessPublicRead()) {
                    return null;
                } else {
                    filterAttributesBy = MetaItemType.ACCESS_PUBLIC_READ;
                }
            }
        }

        if (assetIds != null && assetIds.length > 0) {
            if (!Arrays.asList(assetIds).contains(event.getAssetId())) {
                return null;
            }
        }

        if (parentIds != null && parentIds.length > 0) {
            if (!Arrays.asList(parentIds).contains(event.getParentId())) {
                return null;
            }
        }

        if(path != null && path.length > 0) {
            List<String> pathList = Arrays.asList(event.getPath());
            if (Arrays.stream(path).noneMatch(pathList::contains)) {
                return null;
            }
        }

        if (!TextUtil.isNullOrEmpty(realm)) {
            if (!realm.equals(event.getRealm())) {
                return null;
            }
        }

        if (filterAttributesBy != null) {
            // Filter attributes before doing name match
            AssetEvent assetEvent = (AssetEvent) event;
            if (assetEvent.getAsset() != null) {
                Asset<?> asset = ValueUtil.clone(assetEvent.getAsset());
                MetaItemDescriptor<?> finalFilterAttributesBy = filterAttributesBy;
                asset.setAttributes(asset.getAttributes().values().stream().filter(attribute -> attribute.hasMeta(finalFilterAttributesBy)).collect(Collectors.toList()));
                event = (T) new AssetEvent(assetEvent.getCause(), asset, assetEvent.getUpdatedProperties());
            }
        }

        if (attributeNames != null && attributeNames.length > 0) {
            List<String> eventAttributeNames = Arrays.asList(event.getAttributeNames());
            if (Arrays.stream(attributeNames).noneMatch(eventAttributeNames::contains)) {
                return null;
            }
        }

        return event;
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
