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
package org.openremote.model.asset;

import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEventWithAssetId;

import java.util.Arrays;

/**
 * This event is used when an {@link Asset} is created, read, updated or deleted (updates are only fired when one or more top
 * level {@link Asset} properties are changed (excluding {@link Asset#getAttributes}. Attribute changes are handled via
 * the {@link org.openremote.model.attribute.AttributeEvent}. When the cause is {@link Cause#READ} then the asset's
 * {@link org.openremote.model.attribute.Attribute}s will be included in the asset otherwise they are not.
 */
public class AssetEvent extends SharedEventWithAssetId {

    public static class AssetIdFilter extends EventFilter<SharedEventWithAssetId> {

        public static final String FILTER_TYPE = "asset-id";

        protected String[] assetIds = new String[0];

        protected AssetIdFilter() {
        }

        public AssetIdFilter(String... assetIds) {
            this.assetIds = assetIds;
        }

        public String[] getAssetIds() {
            return assetIds;
        }

        @Override
        public String getFilterType() {
            return FILTER_TYPE;
        }

        @Override
        public boolean apply(SharedEventWithAssetId event) {
            return Arrays.asList(assetIds).contains(event.getEntityId());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "assetIds='" + Arrays.toString(assetIds) + '\'' +
                '}';
        }
    }

    public enum Cause {
        CREATE,
        READ,
        UPDATE,
        DELETE
    }

    protected Cause cause;
    protected Asset asset;
    protected String[] updatedProperties;


    public AssetEvent(Cause cause, Asset asset, String[] updatedProperties) {
        this.cause = cause;
        this.asset = asset;
        this.updatedProperties = updatedProperties;
    }

    public String getEntityId() {
        return asset.id;
    }

    public Cause getCause() {
        return cause;
    }

    public Asset getAsset() {
        return asset;
    }

    public String[] getUpdatedProperties() {
        return updatedProperties;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "cause=" + cause +
                ", asset=" + asset +
                ", updatedProperties=" + Arrays.toString(updatedProperties) +
                ", timestamp=" + timestamp +
                '}';
    }
}
