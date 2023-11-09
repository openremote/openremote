/*
 * Copyright 2021, OpenRemote Inc.
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetTypeInfo {
    protected AssetDescriptor<?> assetDescriptor;
    @JsonIgnore
    protected Map<String, AttributeDescriptor<?>> attributeDescriptors;
    @JsonSerialize(contentConverter = NameHolder.NameHolderToStringConverter.class)
    @JsonDeserialize(contentConverter = MetaItemDescriptor.StringMetaItemDescriptorConverter.class)
    protected MetaItemDescriptor<?>[] metaItemDescriptors;
    @JsonSerialize(contentConverter = ValueDescriptor.NameHolderToStringConverter.class)
    protected ValueDescriptor<?>[] valueDescriptors;

    @JsonCreator
    public AssetTypeInfo(AssetDescriptor<?> assetDescriptor, @JsonProperty("attributeDescriptors") AttributeDescriptor<?>[] attributeDescriptors, MetaItemDescriptor<?>[] metaItemDescriptors, ValueDescriptor<?>[] valueDescriptors) {
        this.assetDescriptor = assetDescriptor;
        this.attributeDescriptors = Arrays.stream(attributeDescriptors).collect(Collectors.toMap(AbstractNameValueDescriptorHolder::getName, ad -> ad));
        this.metaItemDescriptors = metaItemDescriptors;
        this.valueDescriptors = valueDescriptors;
    }

    public AssetDescriptor<?> getAssetDescriptor() {
        return assetDescriptor;
    }

    @JsonProperty("attributeDescriptors")
    protected Collection<AttributeDescriptor<?>> getAttributeDescriptorsInternal() {
        return attributeDescriptors.values();
    }

    public Map<String, AttributeDescriptor<?>> getAttributeDescriptors() {
        return attributeDescriptors;
    }

    public MetaItemDescriptor<?>[] getMetaItemDescriptors() {
        return metaItemDescriptors;
    }

    public ValueDescriptor<?>[] getValueDescriptors() {
        return valueDescriptors;
    }

    public boolean isAgent() {
        return assetDescriptor instanceof AgentDescriptor;
    }
}
