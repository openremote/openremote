/*
 * Copyright 2017, OpenRemote Inc.
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
import org.hibernate.annotations.Formula;
import org.openremote.model.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.openremote.model.Constants.*;

// @formatter:off
/**
 * The main model class of this software.
 * <p>
 * An asset is an identifiable item in a composite relationship with other assets. This tree
 * of assets can be managed through a <code>null</code> {@link #parentId} property for root
 * assets, and a valid parent identifier for sub-assets.
 * <p>
 * The properties {@link #parentName} and {@link #parentType} are transient, not required
 * for storing assets, and only resolved and usable when the asset is loaded from storage.
 * <p>
 * An asset is stored in and therefore access-controlled through a {@link #realmId}. The
 * transient properties {@link #tenantRealm} (the unique realm name of the realm
 * ID) and {@link #tenantDisplayName} are only resolved and usable when the asset is loaded
 * from storage and not relevant for storing assets.
 * <p>
 * The {@link #createdOn} value is milliseconds since the Unix epoch.
 * <p>
 * The {@link #type} of the asset is an arbitrary string, it should be a URI, thus avoiding
 * collisions and representing "ownership" of asset type. Well-known asset types handled by
 * the core platform are defined in {@link AssetType}, third-party extensions can defined
 * their own asset types.
 * <p>
 * The {@link #path} is a list of parent asset identifiers, starting with the identifier of
 * this asset, followed by parent asset identifiers, and ending with the identifier of the
 * root asset in the tree. This is a transient property and only resolved and usable when
 * the asset is loaded from storage and as calculating it is costly, might be empty when
 * certain optimized loading operations are used.
 * <p>
 * The {@link #coordinates} are a pair of LNG/LAT values with the location of the asset.
 * <p>
 * An asset may have dynamically-typed {@link #attributes} with an underlying
 * {@link elemental.json.JsonObject} model. Use the {@link org.openremote.model.Attributes}
 * class to work with this API. This property can be empty when certain optimized loading
 * operations are used.
 * <p>
 * Constructors can filter attributes of an asset as to only contain protected attributes,
 * and their protected metadata (see {@link AssetMeta#PROTECTED}, {@link AssetMeta.Access}).
 * <p>
 * Note that third-party metadata items (not in the
 * {@link org.openremote.model.Constants#NAMESPACE}) are never included on
 * a protected attribute!
 * <p>
 * Example JSON representation of an asset tree:
 * <blockquote><pre>{@code
{
  "id": "0oI7Gf_kTh6WyRJFUTr8Lg",
  "version": 0,
  "createdOn": 1489042784142,
  "name": "Smart Home",
  "type": "urn:openremote:asset:building",
  "realmId": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
  "tenantRealm": "customerA",
  "tenantDisplayName": "Customer A",
  "path": [
    "0oI7Gf_kTh6WyRJFUTr8Lg"
  ],
  "coordinates": [
    5.469751699216005,
    51.44760787406028
  ]
}
 * }</pre></blockquote>
 * <blockquote><pre>{@code
{
  "id": "B0x8ZOqZQHGjq_l0RxAJBA",
  "version": 0,
  "createdOn": 1489042784148,
  "name": "Apartment 1",
  "type": "urn:openremote:asset:residence",
  "parentId": "0oI7Gf_kTh6WyRJFUTr8Lg",
  "parentName": "Smart Home",
  "parentType": "urn:openremote:asset:building",
  "realmId": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
  "tenantRealm": "customerA",
  "tenantDisplayName": "Customer A",
  "path": [
    "B0x8ZOqZQHGjq_l0RxAJBA",
    "0oI7Gf_kTh6WyRJFUTr8Lg"
  ],
  "coordinates": [
    5.469751699216005,
    51.44760787406028
  ]
}
 * }</pre></blockquote>
 * <blockquote><pre>{@code
{
  "id": "bzlRiJmSSMCl8HIUt9-lMg",
  "version": 0,
  "createdOn": 1489042784157,
  "name": "Livingroom",
  "type": "urn:openremote:asset:room",
  "parentId": "B0x8ZOqZQHGjq_l0RxAJBA",
  "parentName": "Apartment 1",
  "parentType": "urn:openremote:asset:residence",
  "realmId": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
  "tenantRealm": "customerA",
  "tenantDisplayName": "Customer A",
  "path": [
    "bzlRiJmSSMCl8HIUt9-lMg",
    "B0x8ZOqZQHGjq_l0RxAJBA",
    "0oI7Gf_kTh6WyRJFUTr8Lg"
  ],
  "coordinates": [
    5.469751699216005,
    51.44760787406028
  ]
}
 * }</pre></blockquote>
 * <blockquote><pre>{@code
{
  "id": "W7GV_lFeQVyHLlgHgE3dEQ",
  "version": 0,
  "createdOn": 1489042784164,
  "name": "Livingroom Thermostat",
  "type": "urn:openremote:asset:thing",
  "parentId": "bzlRiJmSSMCl8HIUt9-lMg",
  "parentName": "Livingroom",
  "parentType": "urn:openremote:asset:room",
  "realmId": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
  "tenantRealm": "customerA",
  "tenantDisplayName": "Customer A",
  "path": [
    "W7GV_lFeQVyHLlgHgE3dEQ",
    "bzlRiJmSSMCl8HIUt9-lMg",
    "B0x8ZOqZQHGjq_l0RxAJBA",
    "0oI7Gf_kTh6WyRJFUTr8Lg"
  ],
  "coordinates": [
    5.460315214821094,
    51.44541688237109
  ],
  "attributes": {
    "currentTemperature": {
      "meta": [
        {
          "name": "urn:openremote:asset:meta:label",
          "value": "Current Temp"
        },
        {
          "name": "urn:openremote:asset:meta:protected",
          "value": true
        },
        {
          "name": "urn:openremote:asset:meta:readOnly",
          "value": true
        },
        {
          "name": "urn:openremote:foo:bar",
          "value": "FOO"
        },
        {
          "name": "urn:thirdparty:bar",
          "value": "BAR"
        }
      ],
      "type": "Decimal",
      "value": 19.2,
      "valueTimestamp": 1.489670166115E12
    },
    "somethingPrivate": {
      "type": "String",
      "value": "Foobar",
      "valueTimestamp": 1.489670156115E12
    }
  }
}
 * }</pre></blockquote>
 */
// @formatter:on
@MappedSuperclass
public class Asset implements IdentifiableEntity {

    static JsonObject filterProtectedAttributes(JsonObject originalAttributes) {
        if (originalAttributes == null)
            return null;
        Attributes filteredAttributes = new Attributes();
        for (Attribute originalAttribute : new Attributes(originalAttributes).get()) {

            // An attribute must be protected to be included
            if (!originalAttribute.isProtected()) {
                continue;
            }

            Attribute protectedAttribute = originalAttribute.copy();
            filteredAttributes.put(protectedAttribute);

            if (!protectedAttribute.hasMeta())
                continue;

            // Any meta item of the attribute, if it's in our namespace, must be protected READ to be included
            Meta protectedMeta = new Meta();
            for (MetaItem metaItem : protectedAttribute.getMeta().all()) {
                if (!metaItem.getName().startsWith(NAMESPACE))
                    continue;

                AssetMeta wellKnownMeta = AssetMeta.byName(metaItem.getName());
                if (wellKnownMeta != null && wellKnownMeta.getAccess().protectedRead) {
                    protectedMeta.add(
                        new MetaItem(metaItem.getName(), metaItem.getValue())
                    );
                }
            }
            if (protectedMeta.size() > 0)
                protectedAttribute.setMeta(protectedMeta);

        }
        return filteredAttributes.getJsonObject();
    }

    @Id
    @Column(name = "ID", length = 27)
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false)
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn = new Date();

    @NotNull(message = "{Asset.name.NotNull}")
    @Size(min = 3, max = 1023, message = "{Asset.name.Size}")
    @Column(name = "NAME", nullable = false, length = 1023)
    protected String name;

    @NotNull(message = "{Asset.type.NotNull}")
    @Size(min = 3, max = 255, message = "{Asset.type.Size}")
    @Column(name = "ASSET_TYPE", nullable = false, updatable = false)
    protected String type;

    @Column(name = "PARENT_ID", length = 27)
    protected String parentId;

    @Transient
    protected String parentName;

    @Transient
    protected String parentType;

    @Column(name = "REALM_ID", nullable = false)
    protected String realmId;

    @Transient
    protected String tenantRealm;

    @Transient
    protected String tenantDisplayName;

    @Transient
    protected double[] coordinates;

    // The following are expensive to query, so if they are null, they might not have been loaded

    @Formula("get_asset_tree_path(ID)")
    @org.hibernate.annotations.Type(type = "org.openremote.container.persistence.ArrayUserType")
    protected String[] path;

    @Column(name = "ATTRIBUTES", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    protected JsonObject attributes;

    public Asset() {
    }

    public Asset(String name, AssetType type) {
        this(null, name, type.getValue());
    }

    public Asset(String realm, String name, AssetType type) {
        this(realm, name, type.getValue());
    }

    public Asset(String realmId, String name, String type) {
        this.realmId = realmId;
        this.name = name;
        this.type = type;
    }

    public Asset(boolean filterProtectedAttributes,
                 String id, long version, Date createdOn, String name, String type,
                 String parentId, String parentName, String parentType,
                 String realmId, String tenantRealm, String tenantDisplayName,
                 String[] path, JsonObject attributes) {
        this.id = id;
        this.version = version;
        this.createdOn = createdOn;
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentType = parentType;
        this.realmId = realmId;
        this.tenantRealm = tenantRealm;
        this.tenantDisplayName = tenantDisplayName;
        this.path = path;
        this.attributes = filterProtectedAttributes ? filterProtectedAttributes(attributes) : attributes;
    }

    public Asset(Asset parent) {
        this.parentId = parent.getId();
        this.parentName = parent.getName();
        this.parentType = parent.getType();
        this.realmId = parent.getRealmId();
        this.tenantRealm = parent.getTenantRealm();
        this.tenantDisplayName = parent.getTenantDisplayName();
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

    public boolean isWellKnownType(AssetType assetType) {
        return assetType.equals(getWellKnownType());
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

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     */
    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     */
    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     */
    public String getTenantRealm() {
        return tenantRealm;
    }

    public void setTenantRealm(String tenantRealm) {
        this.tenantRealm = tenantRealm;
    }

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     */
    public String getTenantDisplayName() {
        return tenantDisplayName;
    }

    public void setTenantDisplayName(String tenantDisplayName) {
        this.tenantDisplayName = tenantDisplayName;
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

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     * <p>
     * The identifiers of all parents representing the path in the tree. The first element
     * is the identifier of this instance, the last is the root asset without a parent.
     */
    public String[] getPath() {
        return path;
    }

    /**
     * NOTE: This is a transient and optional property, set only in database query results.
     * <p>
     * The identifiers of all parents representing the path in the tree. The first element
     * is the root asset without a parent, the last is the identifier of this instance.
     */
    public String[] getReversePath() {
        if (path == null)
            return null;

        String[] newArray = new String[path.length];
        int j = 0;
        for (int i=path.length; i>0; i--) {
            newArray[j] = path[i-1];
            j++;
        }
        return newArray;
    }

    public boolean pathContains(String assetId) {
        return path != null && Arrays.asList(getPath()).contains(assetId);
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type ='" + type + '\'' +
            '}';
    }

    public String toStringAll() {
        return "Asset{" +
            "id='" + id + '\'' +
            ", version=" + version +
            ", createdOn=" + createdOn +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", parentId='" + parentId + '\'' +
            ", parentName='" + parentName + '\'' +
            ", parentType='" + parentType + '\'' +
            ", realmId='" + realmId + '\'' +
            ", tenantRealm='" + tenantRealm + '\'' +
            ", tenantDisplayName='" + tenantDisplayName + '\'' +
            ", coordinates=" + Arrays.toString(coordinates) +
            ", path=" + Arrays.toString(path) +
            ", attributes=" + attributes +
            '}';
    }
}
