/*
 * Copyright 2026 OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AssetAttributeConfigurationDocument {

    public static final int CURRENT_VERSION = 1;

    protected final int version;
    protected final String assetType;
    protected final Map<String, AssetAttributeConfigurationEntry> attributes;
    protected final Map<String, AssetAttributeConfigurationGenericParameter> genericParameters;

    @JsonCreator
    public AssetAttributeConfigurationDocument(@JsonProperty("version") int version,
                                               @JsonProperty("assetType") String assetType,
                                               @JsonProperty("attributes") Map<String, AssetAttributeConfigurationEntry> attributes,
                                               @JsonProperty("genericParameters") Map<String, AssetAttributeConfigurationGenericParameter> genericParameters) {
        this.version = version;
        this.assetType = assetType;
        this.attributes = attributes;
        this.genericParameters = genericParameters;
    }

    public AssetAttributeConfigurationDocument(int version,
                                               String assetType,
                                               Map<String, AssetAttributeConfigurationEntry> attributes) {
        this(version, assetType, attributes, null);
    }

    public int getVersion() {
        return version;
    }

    public String getAssetType() {
        return assetType;
    }

    public Map<String, AssetAttributeConfigurationEntry> getAttributes() {
        return attributes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, AssetAttributeConfigurationGenericParameter> getGenericParameters() {
        return genericParameters;
    }
}
