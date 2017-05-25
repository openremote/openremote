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

import org.hibernate.annotations.Formula;
import org.openremote.model.IdentifiableEntity;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.geo.GeoJSONFeature;
import org.openremote.model.geo.GeoJSONGeometry;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;
import static org.openremote.model.Constants.PERSISTENCE_UNIQUE_ID_GENERATOR;
import static org.openremote.model.asset.AssetAttribute.*;

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
 * {@link ObjectValue} model. Use the {@link Attribute} etc. class to work with this API.
 * This property can be empty when certain optimized loading operations are used.
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
  "name": "Living Room",
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
  "name": "Living Room Thermostat",
  "type": "urn:openremote:asset:thing",
  "parentId": "bzlRiJmSSMCl8HIUt9-lMg",
  "parentName": "Living Room",
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

    @Id
    @Column(name = "ID", length = 27)
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
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
    public ObjectValue attributes;

    public Asset() {
    }

    public Asset(String name, AssetType type) {
        this(name, type, null, null);
    }

    public Asset(String name, String type) {
        this(name, type, null, null);
    }

    public Asset(@NotNull String name, @NotNull AssetType type, Asset parent) {
        this(name, type.getValue(), parent);
    }

    public Asset(@NotNull String name, @NotNull String type, Asset parent) {
        this(name, type, parent, null);
    }

    public Asset(@NotNull String name, @NotNull AssetType type, Asset parent, @NotNull String realmId) {
        this(name, type.getValue(), parent, realmId);
    }

    public Asset(@NotNull String name, @NotNull String type, Asset parent, @NotNull String realmId) {
        setRealmId(realmId);
        setName(name);
        setType(type);
        setParent(parent);

        // Initialise realm from parent
        // TODO: Need to look at this - can child have a different realm to the parent?
        if (parent != null) {
            this.realmId = parent.getRealmId();
            this.tenantRealm = parent.getTenantRealm();
            this.tenantDisplayName = parent.getTenantDisplayName();
        }
    }

    public Asset(boolean filterProtectedAttributes,
                 String id, long version, Date createdOn, String name, String type,
                 String parentId, String parentName, String parentType,
                 String realmId, String tenantRealm, String tenantDisplayName,
                 String[] path, ObjectValue attributes) {
        this(name, type, null, realmId);
        this.id = id;
        this.version = version;
        this.createdOn = createdOn;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentType = parentType;
        this.tenantRealm = tenantRealm;
        this.tenantDisplayName = tenantDisplayName;
        this.path = path;

        if (filterProtectedAttributes && attributes != null) {
            this.attributes = attributesToJson(
                filterProtectedAttributes(attributesFromJson(attributes, id))
                    .collect(Collectors.toList())
            ).orElse(Values.createObject());
        } else {
            this.attributes = attributes;
        }
    }


    public void addAttributes(AssetAttribute... attributes) throws IllegalArgumentException {
        if (this.attributes == null) {
            this.attributes = Values.createObject();
        }

        Arrays.asList(attributes).forEach(
            attribute -> {
                if (getAttributesStream().anyMatch(attr -> isAttributeNameEqualTo(attr, attribute.getName().orElse(null)))) {
                    throw new IllegalArgumentException("Attribute by this name already exists");
                }

                replaceAttribute(attribute);
            }
        );
    }

    /**
     * Replaces existing or adds the attribute if it does not exist.
     */
    public void replaceAttribute(AssetAttribute attribute) throws IllegalArgumentException {
        if (attribute == null || !attribute.getName().isPresent() || !attribute.getType().isPresent())
            throw new IllegalArgumentException("Attribute cannot be null and must have a name and type");

        attribute.assetId = getId();
        if (attributes == null) {
            attributes = Values.createObject();
        }
        attributes.put(attribute.getName().get(), attribute.getObjectValue());
    }

    public void removeAttribute(String name) {
        if (attributes == null) {
            return;
        }

        attributes.remove(name);
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

    public void setName(String name) throws IllegalArgumentException {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public boolean hasTypeWellKnown() {
        return AssetType.getByValue(getType()).isPresent();
    }

    public AssetType getWellKnownType() {
        return AssetType.getByValue(getType()).orElse(AssetType.CUSTOM);
    }

    public void setType(String type) throws IllegalArgumentException {
        this.type = type;
    }

    public void setType(AssetType type) {
        setType(type != null ? type.getValue() : null);
    }

    public void setParent(Asset parent) {
        if (parent == null) {
            parentId = null;
            parentName = null;
            parentType = null;
        } else {
            parentId = parent.id;
            parentName = parent.name;
            parentType = parent.type;
        }
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

    public boolean hasCoordinates() {
        return getCoordinates() != null && getCoordinates().length > 0;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double... coordinates) {
        this.coordinates = coordinates;
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
        for (int i = path.length; i > 0; i--) {
            newArray[j] = path[i - 1];
            j++;
        }
        return newArray;
    }

    public boolean pathContains(String assetId) {
        return path != null && Arrays.asList(getPath()).contains(assetId);
    }

    public ObjectValue getAttributes() {
        return attributes;
    }

    public Stream<AssetAttribute> getAttributesStream() {
        return attributesFromJson(attributes, id);
    }

    public List<AssetAttribute> getAttributesList() {
        return getAttributesStream().collect(Collectors.toList());
    }

    public boolean hasAttribute(String name) {
        return attributes != null && attributes.hasKey(name);
    }

    public Optional<AssetAttribute> getAttribute(String name) {
        return attributes == null ? Optional.empty() : attributes.getObject(name)
            .flatMap(objectValue -> AssetAttribute.attributeFromJson(objectValue, id, name));
    }

    protected void setAttributes(ObjectValue attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(List<AssetAttribute> attributes) {
        this.attributes = attributesToJson(attributes).orElse(Values.createObject());
    }

    public void setAttributes(AssetAttribute... attributes) {
        setAttributes(Arrays.asList(attributes));
    }

    /**
     * This assumes {@link #getCoordinates} array index 0 is Lng and index 1 is Lat,
     * which is true for PostGIS backend. Because Lat/Lng is the 'right way', we flip
     * it here for display. Rounding to 5 decimal places gives us precision of about 1 meter.
     */
    public String getCoordinatesLabel() {
        return
            new BigDecimal(getCoordinates()[1]).setScale(5, RoundingMode.HALF_UP) + " " +
                new BigDecimal(getCoordinates()[0]).setScale(5, RoundingMode.HALF_UP) + " Lat|Lng";
    }

    public boolean hasGeoFeature() {
        return getCoordinates() != null && getCoordinates().length == 2;
    }

    public GeoJSON getGeoFeature(int maxNameLength) {
        if (!hasGeoFeature())
            return GeoJSON.EMPTY_FEATURE_COLLECTION;
        return new GeoJSON("FeatureCollection").setFeatures(
            new GeoJSONFeature("Feature")
                .setProperty("id", getId())
                .setProperty("title", TextUtil.ellipsize(getName(), maxNameLength))
                .setGeometry(new GeoJSONGeometry(getCoordinates()))
        );
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

//    ---------------------------------------------------
//    FUNCTIONAL METHODS BELOW
//    ---------------------------------------------------

    public static boolean isAssetNameEqualTo(Asset asset, String name) {
        return asset != null && asset.getName().equals(name);
    }

    public static boolean isAssetTypeEqualTo(Asset asset, String assetType) {
        return asset != null
            && asset.getType() != null
            && asset.getType().equals(assetType);
    }

    public static boolean isAssetTypeEqualTo(Asset asset, AssetType assetType) {
        return asset != null && asset.getWellKnownType() == assetType;
    }

    public static void removeAttributes(Asset asset, Predicate<AssetAttribute> filter) {
        if (asset == null)
            return;

        List<AssetAttribute> attributes = asset.getAttributesList();
        attributes.removeIf(filter);
        asset.setAttributes(attributes);
    }
}