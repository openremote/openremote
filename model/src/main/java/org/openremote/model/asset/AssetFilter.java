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

    protected List<String> assetIds;
    protected List<String> assetTypes;
    protected List<Class<? extends Asset>> assetClasses;
    protected String realm;
    protected List<String> parentIds;
    protected List<String> path;
    protected List<String> attributeNames;
    protected boolean publicEvents;
    protected boolean restrictedEvents;
    protected boolean internal;

    public AssetFilter() {
    }

    public AssetFilter(String... assetIds) {
        this.assetIds = Arrays.asList(assetIds);
    }

    public AssetFilter(List<String> assetIds) {
        this.assetIds = assetIds;
    }

    public String[] getAssetIds() {
        return assetIds != null ? assetIds.toArray(new String[0]) : null;
    }

    public AssetFilter<T> setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public AssetFilter<T> setAssetIds(String... assetIds) {
        this.assetIds = Arrays.asList(assetIds);
        return this;
    }

    public String[] getAssetTypes() {
        return assetTypes != null ? assetTypes.toArray(new String[0]) : null;
    }

    public AssetFilter<T> setAssetTypes(String... assetTypes) {
        this.assetTypes = Arrays.asList(assetTypes);
        return this;
    }

    public AssetFilter<T> setAssetTypes(List<String> assetTypes) {
        this.assetTypes = assetTypes;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Asset>[] getAssetClasses() {
        return assetClasses != null ? assetClasses.toArray(new Class[0]) : null;
    }

    @SafeVarargs
    public final AssetFilter<T> setAssetClasses(Class<? extends Asset>... assetClasses) {
        this.assetClasses = Arrays.asList(assetClasses);
        return this;
    }

    public AssetFilter<T> setAssetClasses(List<Class<? extends Asset>> assetClasses) {
        this.assetClasses = assetClasses;
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
        return parentIds != null ? parentIds.toArray(new String[0]) : null;
    }

    public AssetFilter<T> setParentIds(String... parentIds) {
        this.parentIds = Arrays.asList(parentIds);
        return this;
    }

    public String[] getPath() {
        return path != null ? path.toArray(new String[0]) : null;
    }

    public AssetFilter<T> setPath(String[] path) {
        this.path = Arrays.asList(path);
        return this;
    }

    public String[] getAttributeNames() {
        return attributeNames != null ? attributeNames.toArray(new String[0]) : null;
    }

    public AssetFilter<T> setAttributeNames(String... attributeNames) {
        this.attributeNames = Arrays.asList(attributeNames);
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

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @Override
    public String getFilterType() {
        return FILTER_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(T event) {

        MetaItemDescriptor<?> filterAttributesBy = null;

        // Non internal subscribers of attribute events only get value updates so make sure the value has changed
        if (!internal && event instanceof AttributeEvent attributeEvent) {
            if (!attributeEvent.valueChanged()) {
                return null;
            }
        }

        if (restrictedEvents) {
            if (event instanceof AttributeEvent attributeEvent) {
                if (!attributeEvent.getMetaValue(MetaItemType.ACCESS_RESTRICTED_READ).orElse(false)) {
                    return null;
                }
            } else {
                filterAttributesBy = MetaItemType.ACCESS_RESTRICTED_READ;
            }
        }

        if (publicEvents) {
            if (event instanceof AttributeEvent attributeEvent) {
                if (!attributeEvent.getMetaValue(MetaItemType.ACCESS_PUBLIC_READ).orElse(false)) {
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

        if (assetIds != null && !assetIds.isEmpty()) {
            if (!assetIds.contains(event.getId())) {
                return null;
            }
        }

        if (assetTypes != null && !assetTypes.isEmpty()) {
            if (!assetTypes.contains(event.getAssetType())) {
                return null;
            }
        }

        if (assetClasses != null && !assetClasses.isEmpty()) {
            T finalEvent = event;
            if (assetClasses.stream().noneMatch(ac -> ac.isAssignableFrom(finalEvent.getAssetClass()))) {
                return null;
            }
        }

        if (parentIds != null && !parentIds.isEmpty()) {
            if (!parentIds.contains(event.getParentId())) {
                return null;
            }
        }

        if(path != null && !path.isEmpty()) {
            List<String> pathList = Arrays.asList(event.getPath());
            if (path.stream().noneMatch(pathList::contains)) {
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

        if (attributeNames != null && !attributeNames.isEmpty()) {
            List<String> eventAttributeNames = Arrays.asList(event.getAttributeNames());
            if (attributeNames.stream().noneMatch(eventAttributeNames::contains)) {
                return null;
            }
        }

        return event;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetIds='" + (assetIds != null ? String.join(",", assetIds) : "") + '\'' +
            "assetTypes='" + (assetTypes != null ? String.join(",", assetTypes) : "") + '\'' +
            ", parentIds='" + (parentIds != null ? String.join(",", parentIds) : "") + '\'' +
            ", realm='" + realm + '\'' +
            ", attributeNames='" + (attributeNames != null ? String.join(",", attributeNames) : "") + '\'' +
            '}';
    }
}
