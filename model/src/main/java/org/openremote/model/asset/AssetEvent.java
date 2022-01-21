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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.AssetInfo;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;

/**
 * This event is used when an {@link Asset} is created, read, updated or deleted (updates are only fired when one or more top
 * level {@link Asset} properties are changed (including attributes). Attribute changes are handled via
 * the {@link org.openremote.model.attribute.AttributeEvent}. When the cause is {@link Cause#READ} then the asset's
 * {@link org.openremote.model.attribute.Attribute}s will be included in the asset otherwise they are not.
 */
public class AssetEvent extends SharedEvent implements AssetInfo {

    public enum Cause {
        CREATE,
        READ,
        UPDATE,
        DELETE
    }

    protected Cause cause;
    protected Asset<?> asset;
    protected String[] updatedProperties;

    @JsonCreator
    public AssetEvent(@JsonProperty("cause") Cause cause, @JsonProperty("asset") Asset<?> asset, @JsonProperty("updatedProperties") String[] updatedProperties) {
        this.cause = cause;
        this.asset = asset;
        this.updatedProperties = updatedProperties;
    }

    public String getAssetId() {
        return asset.id;
    }

    public String getAssetName() {
        return asset.name;
    }

    @Override
    public String getRealm() {
        return asset.realm;
    }

    @Override
    public String getParentId() {
        return asset.parentId;
    }

    @Override
    public String[] getPath() {
        return asset.getPath();
    }

    @Override
    public String[] getAttributeNames() {
        return updatedProperties;
    }

    public Cause getCause() {
        return cause;
    }

    public Asset<?> getAsset() {
        return asset;
    }

    public String[] getUpdatedProperties() {
        return updatedProperties;
    }

    @Override
    public boolean canAccessPublicRead() {
        return asset.isAccessPublicRead();
    }

    @Override
    public boolean canAccessRestrictedRead() {
        return true;
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
