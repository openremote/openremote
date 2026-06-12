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
import org.openremote.model.attribute.AttributeMap;

import java.util.List;

public class AssetAttributeConfigurationImportPreview {

    protected final AssetAttributeConfigurationAssetTypeMismatch assetTypeMismatch;
    protected final List<AssetAttributeConfigurationAttribute> importableAttributes;
    protected final List<AssetAttributeConfigurationAttribute> missingAttributes;
    protected final List<AssetAttributeConfigurationTypeMismatch> typeMismatches;
    protected final AttributeMap patchedAttributes;

    @JsonCreator
    public AssetAttributeConfigurationImportPreview(@JsonProperty("assetTypeMismatch") AssetAttributeConfigurationAssetTypeMismatch assetTypeMismatch,
                                                    @JsonProperty("importableAttributes") List<AssetAttributeConfigurationAttribute> importableAttributes,
                                                    @JsonProperty("missingAttributes") List<AssetAttributeConfigurationAttribute> missingAttributes,
                                                    @JsonProperty("typeMismatches") List<AssetAttributeConfigurationTypeMismatch> typeMismatches,
                                                    @JsonProperty("patchedAttributes") AttributeMap patchedAttributes) {
        this.assetTypeMismatch = assetTypeMismatch;
        this.importableAttributes = importableAttributes;
        this.missingAttributes = missingAttributes;
        this.typeMismatches = typeMismatches;
        this.patchedAttributes = patchedAttributes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public AssetAttributeConfigurationAssetTypeMismatch getAssetTypeMismatch() {
        return assetTypeMismatch;
    }

    public List<AssetAttributeConfigurationAttribute> getImportableAttributes() {
        return importableAttributes;
    }

    public List<AssetAttributeConfigurationAttribute> getMissingAttributes() {
        return missingAttributes;
    }

    public List<AssetAttributeConfigurationTypeMismatch> getTypeMismatches() {
        return typeMismatches;
    }

    public AttributeMap getPatchedAttributes() {
        return patchedAttributes;
    }
}
