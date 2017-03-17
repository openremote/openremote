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

import java.util.Arrays;
import java.util.Date;

/**
 * A DTO for performance-critical operations with assets, such as tree loading/rendering.
 */
public class AssetInfo {

    protected String id;

    protected long version;

    protected String name;

    protected Date createdOn;

    protected String realmId;

    protected String type;

    protected String parentId;

    protected double[] coordinates;

    public AssetInfo(String id) {
        this.id = id;
    }

    public AssetInfo() {
    }

    public AssetInfo(Asset asset) {
        this(
            asset.getId(),
            asset.getVersion(),
            asset.getName(),
            asset.getCreatedOn(),
            asset.getRealmId(),
            asset.getType(),
            asset.getParentId(),
            asset.getCoordinates()
        );
    }

    public AssetInfo(String id,
                     long version,
                     String name,
                     Date createdOn,
                     String realmId,
                     String type,
                     String parentId,
                     double[] coordinates) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.createdOn = createdOn;
        this.realmId = realmId;
        this.type = type;
        this.parentId = parentId;
        this.coordinates = coordinates;
    }

    public AssetInfo(String name, String type) {
        this.name = name;
        this.type = type;
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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getType() {
        return type;
    }

    public AssetType getWellKnownType() {
        return getType() != null ? AssetType.getByValue(getType()) : null;
    }

    public boolean isWellKnownType(AssetType assetType) {
        return assetType.equals(getWellKnownType());
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", realmId='" + realmId + '\'' +
            ", name='" + name + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", type='" + type + '\'' +
            ", parentId='" + parentId + '\'' +
            ", coordinates='" + Arrays.toString(coordinates) + '\'' +
            '}';
    }
}
