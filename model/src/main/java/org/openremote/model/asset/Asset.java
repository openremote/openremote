/*
 * Copyright 2016, OpenRemote Inc.
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

import elemental.json.JsonObject;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;

import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;

/**
 * The main model class of this software.
 * <p>
 * An asset is an identifiable item in a composite relationship with other assets. This tree
 * of assets can be managed through a <code>null</code> {@link #parentId} property for root
 * items, and a valid parent identifier for sub-items.
 * <p>
 * Each asset has dynamically typed optional attributes with an underlying
 * {@link elemental.json.Json} object model. Use the {@link org.openremote.model.Attributes}
 * class to work with this API.
 * <p>
 * The location of an asset is stored as a pair of LNG/LAT coordinates.
 */
@MappedSuperclass
@Table(name = "ASSET")
public class Asset {

    @Id
    @Column(name = "ID", length = 27)
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
    protected long version;

    @NotNull
    @Column(name = "TENANT_REALM", nullable = false)
    protected String realm;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false)
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn = new Date();

    @NotNull
    @Column(name = "NAME", nullable = false)
    protected String name;

    @NotNull
    @Column(name = "ASSET_TYPE", nullable = false)
    protected String type;

    @Column(name = "PARENT_ID")
    protected String parentId;

    @Column(name = "ATTRIBUTES", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    protected JsonObject attributes;

    @Transient
    protected String[] path;

    @Transient
    protected double[] coordinates;

    public Asset() {
    }

    public Asset(String name, String type) {
        this(null, name, type);
    }

    public Asset(String realm, String name, String type) {
        this.realm = realm;
        this.name = name;
        this.type = type;
    }

    public Asset(String name, AssetType type) {
        this(null, name, type.getValue());
    }

    public Asset(String realm, String name, AssetType type) {
        this(realm, name, type.getValue());
    }

    public Asset(Asset parent) {
        this.realm = parent.getRealm();
        this.parentId = parent.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public AssetType getWellKnownType() {
        return AssetType.getByValue(getType());
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setType(AssetType type) {
        setType(type != null ? type.getValue() : null);
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = attributes;
    }

    public String[] getPath() {
        return path;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double... coordinates) {
        this.coordinates = coordinates;
    }

    public boolean hasCoordinates() {
        return getCoordinates() != null && getCoordinates().length > 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", realm='" + realm + '\'' +
            ", name='" + name + '\'' +
            ", createOn='" + createdOn + '\'' +
            ", type ='" + type + '\'' +
            ", parent ='" + parentId + '\'' +
            ", path ='" + Arrays.toString(path) + '\'' +
            ", coordinates ='" + Arrays.toString(coordinates) + '\'' +
            '}';
    }
}
