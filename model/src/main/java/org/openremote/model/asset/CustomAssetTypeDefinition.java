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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "CUSTOM_ASSET_TYPE")
public class CustomAssetTypeDefinition {

    @Id
    @NotBlank
    @Pattern(regexp = "^\\w+$")
    @Size(max = 255)
    @Column(name = "NAME", nullable = false, length = 255)
    protected String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "DISPLAY_NAME", nullable = false, length = 255)
    protected String displayName;

    @Size(max = 255)
    @Column(name = "ICON", length = 255)
    protected String icon;

    @Size(max = 255)
    @Column(name = "COLOUR", length = 255)
    protected String colour;

    @Column(name = "DESCRIPTION")
    protected String description;

    @Column(name = "ENABLED", nullable = false)
    protected boolean enabled = true;

    @NotNull
    @Valid
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ATTRIBUTES", nullable = false)
    protected List<CustomAssetTypeAttributeDefinition> attributes = new ArrayList<>();

    @Version
    @Column(name = "VERSION", nullable = false)
    protected long version;

    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Instant createdOn;

    @Column(name = "CREATED_BY", updatable = false, length = 255)
    protected String createdBy;

    @Column(name = "UPDATED_ON", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.UpdateTimestamp
    protected Instant updatedOn;

    @Column(name = "UPDATED_BY", length = 255)
    protected String updatedBy;

    protected CustomAssetTypeDefinition() {
    }

    @JsonCreator
    public CustomAssetTypeDefinition(
        String name,
        String displayName,
        String icon,
        String colour,
        String description,
        boolean enabled,
        List<CustomAssetTypeAttributeDefinition> attributes
    ) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.colour = colour;
        this.description = description;
        this.enabled = enabled;
        setAttributes(attributes);
    }

    public String getName() {
        return name;
    }

    public CustomAssetTypeDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CustomAssetTypeDefinition setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public CustomAssetTypeDefinition setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getColour() {
        return colour;
    }

    public CustomAssetTypeDefinition setColour(String colour) {
        this.colour = colour;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public CustomAssetTypeDefinition setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CustomAssetTypeDefinition setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public List<CustomAssetTypeAttributeDefinition> getAttributes() {
        return attributes;
    }

    public CustomAssetTypeDefinition setAttributes(List<CustomAssetTypeAttributeDefinition> attributes) {
        this.attributes = attributes != null ? new ArrayList<>(attributes) : new ArrayList<>();
        return this;
    }

    public long getVersion() {
        return version;
    }

    public CustomAssetTypeDefinition setVersion(long version) {
        this.version = version;
        return this;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public CustomAssetTypeDefinition setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public CustomAssetTypeDefinition setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public Instant getUpdatedOn() {
        return updatedOn;
    }

    public CustomAssetTypeDefinition setUpdatedOn(Instant updatedOn) {
        this.updatedOn = updatedOn;
        return this;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public CustomAssetTypeDefinition setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomAssetTypeDefinition that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
