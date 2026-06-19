/*
 * Copyright 2026 OpenRemote Inc.
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueFormat;

public class CustomAssetTypeAttributeDefinition {

    @NotBlank
    @Pattern(regexp = "^\\w+$")
    protected String name;

    /**
     * Existing {@link org.openremote.model.value.ValueDescriptor} name, not the descriptor instance.
     */
    @NotBlank
    @Pattern(regexp = "^\\w+(\\[\\])*$")
    protected String type;

    protected Boolean optional;
    protected Object defaultValue;
    protected String[] units;
    @Valid
    protected ValueFormat format;
    @Valid
    protected ValueConstraint[] constraints;
    @Valid
    protected MetaMap meta;
    protected Integer position;

    protected CustomAssetTypeAttributeDefinition() {
    }

    @JsonCreator
    public CustomAssetTypeAttributeDefinition(
        String name,
        String type,
        Boolean optional,
        Object defaultValue,
        String[] units,
        ValueFormat format,
        ValueConstraint[] constraints,
        MetaMap meta,
        Integer position
    ) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.units = units;
        this.format = format;
        this.constraints = constraints;
        this.meta = meta;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public CustomAssetTypeAttributeDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public CustomAssetTypeAttributeDefinition setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isOptional() {
        return optional != null ? optional : false;
    }

    public Boolean getOptional() {
        return optional;
    }

    public CustomAssetTypeAttributeDefinition setOptional(Boolean optional) {
        this.optional = optional;
        return this;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public CustomAssetTypeAttributeDefinition setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String[] getUnits() {
        return units;
    }

    public CustomAssetTypeAttributeDefinition setUnits(String[] units) {
        this.units = units;
        return this;
    }

    public ValueFormat getFormat() {
        return format;
    }

    public CustomAssetTypeAttributeDefinition setFormat(ValueFormat format) {
        this.format = format;
        return this;
    }

    public ValueConstraint[] getConstraints() {
        return constraints;
    }

    public CustomAssetTypeAttributeDefinition setConstraints(ValueConstraint[] constraints) {
        this.constraints = constraints;
        return this;
    }

    public MetaMap getMeta() {
        return meta;
    }

    public CustomAssetTypeAttributeDefinition setMeta(MetaMap meta) {
        this.meta = meta;
        return this;
    }

    public Integer getPosition() {
        return position;
    }

    public CustomAssetTypeAttributeDefinition setPosition(Integer position) {
        this.position = position;
        return this;
    }
}
