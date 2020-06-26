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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Formula;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.IdentifiableEntity;
import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.geo.GeoJSONFeature;
import org.openremote.model.geo.GeoJSONFeatureCollection;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.ObservableList;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
import static org.openremote.model.attribute.AttributeType.LOCATION;

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
 * An asset is stored in and therefore access-controlled through a {@link #realm}.
 * <p>
 * The {@link #createdOn} value is milliseconds since the Unix epoch.
 * <p>
 * The {@link #type} of the asset is an arbitrary string, it should be a URI, thus avoiding
 * collisions and representing "ownership" of asset type. Well-known asset types handled by
 * the core platform are defined in {@link AssetType}, third-party extensions can define
 * their own asset types in their own namespace, e.g. <code>urn:mynamespace:myassettype</code>
 * <p>
 * The {@link #path} is a list of parent asset identifiers, starting with the identifier of
 * this asset, followed by parent asset identifiers, and ending with the identifier of the
 * root asset in the tree. This is a transient property and only resolved and usable when
 * the asset is loaded from storage and as calculating it is costly, might be empty when
 * certain optimized loading operations are used.
 * An asset may have dynamically-typed {@link #attributes} with an underlying
 * {@link ObjectValue} model. Use the {@link Attribute} etc. class to work with this API.
 * This property can be empty when certain optimized loading operations are used.
 * <p>
 * For more details on restricted access of user-assigned assets, see {@link UserAsset}.
 * </p>
 * <p>
 * Example JSON representation of an asset tree:
 * <blockquote><pre>{@code
 * {
 * "id": "0oI7Gf_kTh6WyRJFUTr8Lg",
 * "version": 0,
 * "createdOn": 1489042784142,
 * "name": "Smart Building",
 * "type": "urn:openremote:asset:building",
 * "accessPublicRead": false,
 * "realm": "building",
 * "realmDisplayName": "Building",
 * "path": [
 * "0oI7Gf_kTh6WyRJFUTr8Lg"
 * ],
 * "coordinates": [
 * 5.469751699216005,
 * 51.44760787406028
 * ]
 * }
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * {
 * "id": "B0x8ZOqZQHGjq_l0RxAJBA",
 * "version": 0,
 * "createdOn": 1489042784148,
 * "name": "Apartment 1",
 * "type": "urn:openremote:asset:residence",
 * "accessPublicRead": false,
 * "parentId": "0oI7Gf_kTh6WyRJFUTr8Lg",
 * "parentName": "Smart Building",
 * "parentType": "urn:openremote:asset:building",
 * "realm": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
 * "tenantRealm": "building",
 * "tenantDisplayName": "Building",
 * "path": [
 * "B0x8ZOqZQHGjq_l0RxAJBA",
 * "0oI7Gf_kTh6WyRJFUTr8Lg"
 * ],
 * "coordinates": [
 * 5.469751699216005,
 * 51.44760787406028
 * ]
 * }
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * {
 * "id": "bzlRiJmSSMCl8HIUt9-lMg",
 * "version": 0,
 * "createdOn": 1489042784157,
 * "name": "Living Room",
 * "type": "urn:openremote:asset:room",
 * "accessPublicRead": false,
 * "parentId": "B0x8ZOqZQHGjq_l0RxAJBA",
 * "parentName": "Apartment 1",
 * "parentType": "urn:openremote:asset:residence",
 * "realm": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
 * "tenantRealm": "building",
 * "tenantDisplayName": "Building",
 * "path": [
 * "bzlRiJmSSMCl8HIUt9-lMg",
 * "B0x8ZOqZQHGjq_l0RxAJBA",
 * "0oI7Gf_kTh6WyRJFUTr8Lg"
 * ],
 * "coordinates": [
 * 5.469751699216005,
 * 51.44760787406028
 * ]
 * }
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * {
 * "id": "W7GV_lFeQVyHLlgHgE3dEQ",
 * "version": 0,
 * "createdOn": 1489042784164,
 * "name": "Living Room Thermostat",
 * "type": "urn:openremote:asset:thing",
 * "accessPublicRead": false,
 * "parentId": "bzlRiJmSSMCl8HIUt9-lMg",
 * "parentName": "Living Room",
 * "parentType": "urn:openremote:asset:room",
 * "realm": "c38a3fdf-9d74-4dac-940c-50d3dce1d248",
 * "tenantRealm": "building",
 * "tenantDisplayName": "Building",
 * "path": [
 * "W7GV_lFeQVyHLlgHgE3dEQ",
 * "bzlRiJmSSMCl8HIUt9-lMg",
 * "B0x8ZOqZQHGjq_l0RxAJBA",
 * "0oI7Gf_kTh6WyRJFUTr8Lg"
 * ],
 * "coordinates": [
 * 5.460315214821094,
 * 51.44541688237109
 * ],
 * "attributes": {
 * "currentTemperature": {
 * "meta": [
 * {
 * "name": "urn:openremote:asset:meta:label",
 * "value": "Current Temp"
 * },
 * {
 * "name": "urn:openremote:asset:meta:accessRestrictedRead",
 * "value": true
 * },
 * {
 * "name": "urn:openremote:asset:meta:readOnly",
 * "value": true
 * },
 * {
 * "name": "urn:openremote:foo:bar",
 * "value": "FOO"
 * },
 * {
 * "name": "urn:thirdparty:bar",
 * "value": "BAR"
 * }
 * ],
 * "type": "Decimal",
 * "value": 19.2,
 * "valueTimestamp": 1.489670166115E12
 * },
 * "somethingPrivate": {
 * "type": "String",
 * "value": "Foobar",
 * "valueTimestamp": 1.489670156115E12
 * }
 * }
 * }
 * }</pre></blockquote>
 */
// @formatter:on
@Entity
@Table(name = "ASSET")
@Check(constraints = "ID != PARENT_ID")
public class Asset implements IdentifiableEntity {

    public enum AssetTypeFailureReason implements ValidationFailure.Reason {
        ASSET_TYPE_MISMATCH,
        ASSET_TYPE_NOT_SUPPORTED
    }

    @Id
    @Column(name = "ID", length = 22, columnDefinition = "char(22)")
    @GeneratedValue(generator = PERSISTENCE_UNIQUE_ID_GENERATOR)
    protected String id;

    @Version
    @Column(name = "OBJ_VERSION", nullable = false)
    protected long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @org.hibernate.annotations.CreationTimestamp
    protected Date createdOn;

    @NotNull(message = "{Asset.name.NotNull}")
    @Size(min = 1, max = 1023, message = "{Asset.name.Size}")
    @Column(name = "NAME", nullable = false, length = 1023)
    protected String name;

    @NotNull(message = "{Asset.type.NotNull}")
    @Size(min = 3, max = 255, message = "{Asset.type.Size}")
    @Column(name = "ASSET_TYPE", nullable = false, updatable = false)
    protected String type;

    @Column(name = "ACCESS_PUBLIC_READ", nullable = false)
    protected boolean accessPublicRead;

    @Column(name = "PARENT_ID", length = 36)
    protected String parentId;

    @Transient
    protected String parentName;

    @Transient
    protected String parentType;

    @Column(name = "REALM", nullable = false)
    protected String realm;

    @Transient
    @JsonIgnore
    protected ObservableList<AssetAttribute> attributeList;

    // The following are expensive to query, so if they are null, they might not have been loaded

    @Formula("get_asset_tree_path(ID)")
    @org.hibernate.annotations.Type(type = "org.openremote.container.persistence.ArrayUserType")
    protected String[] path;

    @Column(name = "ATTRIBUTES", columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    public ObjectValue attributes;

    public Asset() {
    }

    public Asset(String name, AssetDescriptor type) {
        this(name, type, null, null);
    }

    public Asset(String name, String type) {
        this(name, type, false, null, null);
    }

    public Asset(@NotNull String name, @NotNull AssetDescriptor type, Asset parent) {
        this(name, type, parent, null);
    }

    public Asset(@NotNull String name, @NotNull String type, Asset parent) {
        this(name, type, false, parent, null);
    }

    public Asset(@NotNull String name, @NotNull AssetDescriptor type, Asset parent, String realm) {
        this(name, type.getType(), type.getAccessPublicRead(), parent, realm);
        if (type.getAttributeDescriptors() != null) {
            addAttributes(Arrays.stream(type.getAttributeDescriptors()).map(AssetAttribute::new).toArray(AssetAttribute[]::new));
        }
    }

    public Asset(@NotNull String name, @NotNull String type, boolean accessPublicRead, Asset parent, String realm) {
        setRealm(realm);
        setName(name);
        setType(type);
        setParent(parent);
        setAccessPublicRead(accessPublicRead);

        // Initialise realm from parent
        // TODO: Need to look at this - can child have a different realm to the parent?
        if (parent != null) {
            this.realm = parent.getRealm();
        }
    }

    @JsonCreator
    public Asset(@JsonProperty("id") String id,
                 @JsonProperty("version") long version,
                 @JsonProperty("createdOn") Date createdOn,
                 @JsonProperty("name") String name,
                 @JsonProperty("type") String type,
                 @JsonProperty("accessPublicRead") boolean accessPublicRead,
                 @JsonProperty("parentId") String parentId,
                 @JsonProperty("parentName") String parentName,
                 @JsonProperty("parentType") String parentType,
                 @JsonProperty("realm") String realm,
                 @JsonProperty("path") String[] path,
                 @JsonProperty("attributes") ObjectValue attributes) {
        this(name, type, accessPublicRead, null, realm);
        this.id = id;
        this.version = version;
        this.createdOn = createdOn;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentType = parentType;
        this.path = path;
        this.attributes = attributes;
    }

    public Asset addAttributes(AssetAttribute... attributes) throws IllegalArgumentException {
        Arrays.asList(attributes).forEach(
            attribute -> {
                if (getAttributesStream().anyMatch(attr -> isAttributeNameEqualTo(attr, attribute.getName().orElse(null)))) {
                    throw new IllegalArgumentException("Attribute by this name already exists: " + attribute.getName().orElse(""));
                }

                replaceAttribute(attribute);
            }
        );
        return this;
    }

    /**
     * Replaces existing or adds the attribute if it does not exist.
     */
    public Asset replaceAttribute(AssetAttribute attribute) throws IllegalArgumentException {
        if (attribute == null || !attribute.getName().isPresent() || !attribute.getType().isPresent())
            throw new IllegalArgumentException("Attribute cannot be null and must have a name and type");

        attribute.assetId = getId();
        List<AssetAttribute> attributeList = getAttributesList();
        attributeList.removeIf(attr -> attr.getName().orElse("").equals(attribute.getName().orElse("")));
        attributeList.add(attribute);

        return this;
    }

    public Asset removeAttribute(String name) {
        List<AssetAttribute> attributeList = getAttributesList();
        attributeList.removeIf(attr -> attr.getName().orElse("").equals(name));
        return this;
    }

    public Asset removeAttribute(AttributeDescriptor attributeDescriptor) {
        return removeAttribute(attributeDescriptor.getAttributeName());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        // Must clear the cached list of attributes they can be recreated with the new identifier
        this.attributeList = null;
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

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
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
        //TODO replace with AssetModel getValues, through a http request
        return AssetType.getByValue(getType()).isPresent();
    }

    public AssetType getWellKnownType() {
        //TODO replace with AssetModel getValues, through a http request
        return AssetType.getByValue(getType()).orElse(null);
    }

    public void setType(String type) throws IllegalArgumentException {
        this.type = type;
    }

    public void setType(AssetDescriptor type) {
        setType(type != null ? type.getType() : null);
    }

    public boolean isAccessPublicRead() {
        return accessPublicRead;
    }

    public void setAccessPublicRead(boolean accessPublicRead) {
        this.accessPublicRead = accessPublicRead;
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

    public AssetType getParentWellKnownType() {
        //TODO replace with AssetModel getValues, through a http request
        return AssetType.getByValue(getParentType()).orElse(null);
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public void setPath(String[] path) {
        this.path = path;
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
        return getAttributesList().stream();
    }

    public List<AssetAttribute> getAttributesList() {
        if (attributeList == null) {
            attributeList = new ObservableList<>(attributesFromJson(attributes, id).collect(Collectors.toList()),
                    () -> this.attributes = attributesToJson(attributeList).orElse(Values.createObject()));
        }
        return attributeList;
    }

    public boolean hasAttribute(String name) {
        return attributes != null && attributes.hasKey(name);
    }

    public Optional<AssetAttribute> getAttribute(AttributeDescriptor descriptor) {
        return getAttribute(descriptor.getAttributeName());
    }

    public Optional<AssetAttribute> getAttribute(String name) {
        return attributes == null ? Optional.empty() : attributes.getObject(name)
            .flatMap(objectValue -> AssetAttribute.attributeFromJson(objectValue, id, name));
    }

    public Asset setAttributes(ObjectValue attributes) {
        setAttributes(attributesFromJson(attributes, id).collect(Collectors.toList()));
        return this;
    }

    public Asset setAttributes(List<AssetAttribute> attributes) {
        ((ObservableList) getAttributesList()).clear(false);
        getAttributesList().addAll(attributes);
        return this;
    }

    public Asset setAttributes(AssetAttribute... attributes) {
        return setAttributes(Arrays.asList(attributes));
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type ='" + type + '\'' +
            ", parentId='" + parentId + '\'' +
            '}';
    }

    public String toStringAll() {
        return "Asset{" +
            "id='" + id + '\'' +
            ", version=" + version +
            ", createdOn=" + createdOn +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", accessPublicRead='" + accessPublicRead + '\'' +
            ", parentId='" + parentId + '\'' +
            ", parentName='" + parentName + '\'' +
            ", parentType='" + parentType + '\'' +
            ", realm='" + realm + '\'' +
            ", path=" + Arrays.toString(path) +
            ", attributes=" + attributes +
            '}';
    }

//    ---------------------------------------------------
//    FUNCTIONAL METHODS BELOW
//    ---------------------------------------------------

    /**
     * Complies to the GeoJSON specification RFC 7946
     */
    public GeoJSONPoint getCoordinates() {
        return getAttributesStream()
            .filter(attribute -> attribute.getNameOrThrow().equals(LOCATION.getAttributeName()))
            .findFirst()
            .flatMap(AbstractValueHolder::getValue)
            .flatMap(GeoJSONPoint::fromValue)
            .orElse(null);
    }

    /**
     * Complies to the GeoJSON specification RFC 7946
     */
    public void setCoordinates(GeoJSONPoint coordinates) {
        AssetAttribute locationAttribute = getAttributesStream()
            .filter(attribute -> attribute.getNameOrThrow().equals(LOCATION.getAttributeName()))
            .findFirst().orElse(new AssetAttribute(LOCATION));

        locationAttribute.setValue(coordinates == null ? null : coordinates.toValue());
        replaceAttribute(locationAttribute);
    }



    public boolean hasGeoFeature() {
        return getCoordinates() != null;
    }

    public GeoJSON getGeoFeature(int maxNameLength) {
        if (!hasGeoFeature())
            return GeoJSONFeatureCollection.EMPTY;

        return new GeoJSONFeatureCollection(
            new GeoJSONFeature(getCoordinates())
                .setProperty("id", getId())
                .setProperty("title", TextUtil.ellipsize(getName(), maxNameLength))
        );
    }

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

        asset.getAttributesList().removeIf(filter);
    }

    public static Asset map(Asset assetToMap, Asset asset) {
        return map(assetToMap, asset, null, null, null, null, null, null);
    }

    public static Asset map(Asset assetToMap, Asset asset,
                            String overrideName,
                            String overrideRealm,
                            String overrideParentId,
                            String overrideType,
                            Boolean overrideAccessPublicRead,
                            ObjectValue overrideAttributes) {
        asset.setVersion(assetToMap.getVersion());
        asset.setName(overrideName != null ? overrideName : assetToMap.getName());
        if (overrideType != null) {
            asset.setType(overrideType);
        } else {
            asset.setType(assetToMap.getType());
        }

        asset.setAccessPublicRead(overrideAccessPublicRead != null ? overrideAccessPublicRead : assetToMap.isAccessPublicRead());

        asset.setParentId(overrideParentId != null ? overrideParentId : assetToMap.getParentId());
        asset.setParentName(null);
        asset.setParentType(null);

        asset.setRealm(overrideRealm != null ? overrideRealm : assetToMap.getRealm());

        asset.setAttributes(overrideAttributes != null ? overrideAttributes : assetToMap.getAttributes());

        return asset;
    }
}