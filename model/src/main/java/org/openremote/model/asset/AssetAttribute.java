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
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.*;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.attribute.MetaItemType.*;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.attribute.MetaItem.replaceMetaByName;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

public class AssetAttribute extends Attribute {

    protected String assetId;

    public AssetAttribute() {
        super(Values.createObject());
    }

    protected AssetAttribute(ObjectValue objectValue) {
        super(objectValue);
    }

    public AssetAttribute(String name) {
        super(name);
    }

    public AssetAttribute(String name, AttributeValueDescriptor type) {
        super(name, type);
    }

    public AssetAttribute(AttributeDescriptor attributeDescriptor) {
        this(attributeDescriptor.getAttributeName(), attributeDescriptor);
    }

    public AssetAttribute(String name, AttributeDescriptor attributeDescriptor) {
        this(name, attributeDescriptor, attributeDescriptor.getInitialValue());
    }

    public AssetAttribute(AttributeDescriptor attributeDescriptor, Value value) {
        this(attributeDescriptor.getAttributeName(), attributeDescriptor, value);
    }

    public AssetAttribute(String name, AttributeDescriptor attributeDescriptor, Value value) {
        this(name, attributeDescriptor, value, 0L);
    }

    public AssetAttribute(AttributeDescriptor attributeDescriptor, Value value, long timestamp) {
        this(attributeDescriptor.getAttributeName(), attributeDescriptor, value, timestamp);
    }

    public AssetAttribute(String name, AttributeDescriptor attributeDescriptor, Value value, long timestamp) {
        super(name, attributeDescriptor.getValueDescriptor(), value);
        if (value != null) {
            if (value.getType() != attributeDescriptor.getValueDescriptor().getValueType()) {
                throw new IllegalArgumentException("Provided value type is not compatible with this attribute type");
            }
        }
        setValue(value, timestamp);
        if (attributeDescriptor.getMetaItemDescriptors() != null) {
            addMeta(Arrays.stream(attributeDescriptor.getMetaItemDescriptors()).map(MetaItem::new).toArray(MetaItem[]::new));
        }
    }

    public AssetAttribute(String name, AttributeValueType type, Value value, long timestamp) {
        super(name, type, value, timestamp);
    }

    public AssetAttribute(String name, AttributeValueType type, Value value) {
        super(name, type, value);
    }

    public AssetAttribute(String assetId, String name) {
        super(name);
        setAssetId(assetId);
    }

    public AssetAttribute(String assetId, String name, AttributeValueType type) {
        super(name, type);
        setAssetId(assetId);
    }

    public AssetAttribute(String assetId, String name, AttributeValueType type, Value value) {
        super(name, type, value);
        setAssetId(assetId);
    }

    public AssetAttribute(String assetId, String name, AttributeValueType type, Value value, long timestamp) {
        super(name, type, value, timestamp);
        setAssetId(assetId);
    }

    @JsonCreator
    private AssetAttribute(@JsonProperty("assetId") String assetId,
                          @JsonProperty("name") String name,
                          @JsonProperty("type") AttributeValueDescriptor type,
                          @JsonProperty("meta") List<MetaItem> metaItems,
                          @JsonProperty("value") Value value,
                          @JsonProperty("timestamp") Long timestamp) {
        super(name, type, value);
        setValueTimestamp(timestamp);
        if (assetId != null) {
            setAssetId(assetId);
        }
        setMeta(metaItems);
    }

    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    protected void setAssetId(String assetId) {
        requireNonNullAndNonEmpty(assetId);
        this.assetId = assetId;
    }

    public Optional<AttributeRef> getReference() {
        return Optional.ofNullable(
            getAssetId().isPresent() && getName().isPresent()
                ? new AttributeRef(getAssetId().get(), getName().get())
                : null
        );
    }

    /**
     * The below is used in a sufficient number of places to provide it here as a utility method
     * with a standardised exception message.
     */
    public AttributeRef getReferenceOrThrow() {
        return getReference().orElseThrow(() -> new IllegalStateException("Attribute doesn't have an attribute ref"));
    }

    @JsonIgnore
    public Optional<AttributeState> getState() {
        return getReference().map(ref -> new AttributeState(ref, getValue().orElse(null)));
    }

    @Override
    public List<ValidationFailure> getMetaItemValidationFailures(MetaItem item, Optional<MetaItemDescriptor> metaItemDescriptor) {
        return super.getMetaItemValidationFailures(item, metaItemDescriptor);
    }

    @JsonIgnore
    @Override
    public AssetAttribute setMeta(Meta meta) {
        super.setMeta(meta);
        return this;
    }

    @JsonIgnore
    @Override
    public AssetAttribute setMeta(MetaItem... meta) {
        super.setMeta(meta);
        return this;
    }

    @JsonProperty("meta")
    @Override
    public void setMeta(List<MetaItem> metaItems) {
        super.setMeta(metaItems);
    }

    public AssetAttribute addMeta(MetaItem... meta) {
        if (meta != null) {
            getMeta().addAll(Arrays.asList(meta));
        }
        return this;
    }

    public AssetAttribute addMeta(MetaItemDescriptor... metaItemDescriptors) {
        if (metaItemDescriptors != null) {
            getMeta().addAll(Arrays.stream(metaItemDescriptors).map(MetaItem::new).collect(Collectors.toList()));
        }
        return this;
    }

    /**
     * @return The current value and its timestamp represented as an attribute event.
     */
    public Optional<AttributeEvent> getStateEvent() {
        return getState().flatMap(state -> getValueTimestamp().map(ts -> new AttributeEvent(state, ts)));
    }

    public boolean hasLabel() {
        return getMetaStream().anyMatch(isMetaNameEqualTo(LABEL));
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(getMetaStream()
            .filter(isMetaNameEqualTo(LABEL))
            .findFirst()
            .flatMap(AbstractValueHolder::getValueAsString)
            .orElseGet(() -> getName().orElse(null)));
    }

    public Optional<String> getLabelOrName() {
        return getLabel().map(Optional::of).orElseGet(this::getName);
    }

    public void setLabel(String label) {
        if (!isNullOrEmpty(label)) {
            replaceMetaByName(getMeta(), LABEL, Values.create(label));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(LABEL));
        }
    }

    @JsonIgnore
    public boolean isExecutable() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(EXECUTABLE))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setExecutable(boolean executable) {
        if (executable) {
            replaceMetaByName(getMeta(), EXECUTABLE, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(EXECUTABLE));
        }
    }

    @JsonIgnore
    public boolean hasAgentLink() {
        return getMetaStream().anyMatch(isMetaNameEqualTo(AGENT_LINK));
    }

    @JsonIgnore
    public boolean isProtocolConfiguration() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(PROTOCOL_CONFIGURATION))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    @JsonIgnore
    public boolean isShowOnDashboard() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(SHOW_ON_DASHBOARD))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setShowOnDashboard(boolean show) {
        if (show) {
            replaceMetaByName(getMeta(), SHOW_ON_DASHBOARD, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(SHOW_ON_DASHBOARD));
        }
    }

    @JsonIgnore
    public boolean hasFormat() {
        return getMetaStream().anyMatch(isMetaNameEqualTo(FORMAT));
    }

    public Optional<String> getFormat() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(FORMAT))
            .findFirst()
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    public void setFormat(String format) {
        if (!isNullOrEmpty(format)) {
            replaceMetaByName(getMeta(), FORMAT, Values.create(format));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(FORMAT));
        }
    }

    public boolean hasDescription() {
        return getMetaStream().anyMatch(isMetaNameEqualTo(DESCRIPTION));
    }

    public Optional<String> getDescription() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(DESCRIPTION))
            .findFirst()
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    public void setDescription(String description) {
        if (!isNullOrEmpty(description)) {
            replaceMetaByName(getMeta(), DESCRIPTION, Values.create(description));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(DESCRIPTION));
        }
    }

    /**
     * Defaults to <code>true</code> if there is no {@link MetaItemType#DISABLED} item.
     */
    public boolean isEnabled() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(DISABLED))
            .findFirst()
            .map(metaItem -> !metaItem.getValueAsBoolean().orElse(false))
            .orElse(true);
    }

    public void setDisabled(boolean disabled) {
        if (disabled) {
            replaceMetaByName(getMeta(), DISABLED, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(DISABLED));
        }
    }

    public boolean isAccessRestrictedRead() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(MetaItemType.ACCESS_RESTRICTED_READ))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public boolean isAccessRestrictedWrite() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(MetaItemType.ACCESS_RESTRICTED_WRITE))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public boolean isAccessPublicRead() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(MetaItemType.ACCESS_PUBLIC_READ))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public boolean isReadOnly() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(READ_ONLY))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            replaceMetaByName(getMeta(), READ_ONLY, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(READ_ONLY));
        }
    }

    public boolean isStoreDatapoints() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(STORE_DATA_POINTS))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setStoreDatapoints(boolean storeDatapoints) {
        if (storeDatapoints) {
            replaceMetaByName(getMeta(), STORE_DATA_POINTS, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(STORE_DATA_POINTS));
        }
    }

    public boolean isRuleState() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(RULE_STATE))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setRuleState(boolean ruleState) {
        if (ruleState) {
            replaceMetaByName(getMeta(), RULE_STATE, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(RULE_STATE));
        }
    }

    public boolean isRuleEvent() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(RULE_EVENT))
            .findFirst()
            .map(metaItem -> metaItem.getValueAsBoolean().orElse(false))
            .orElse(false);
    }

    public void setRuleEvent(boolean ruleEvent) {
        if (ruleEvent) {
            replaceMetaByName(getMeta(), RULE_EVENT, Values.create(true));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(RULE_EVENT));
        }
    }

    public Optional<String> getRuleEventExpires() {
        return getMetaStream()
            .filter(isMetaNameEqualTo(RULE_EVENT_EXPIRES))
            .findFirst()
            .flatMap(AbstractValueHolder::getValueAsString);
    }

    public void setRuleEventExpires(String expiry) {
        if (!isNullOrEmpty(expiry)) {
            replaceMetaByName(getMeta(), RULE_EVENT_EXPIRES, Values.create(expiry));
        } else {
            getMeta().removeIf(isMetaNameEqualTo(RULE_EVENT_EXPIRES));
        }
    }

    public AssetAttribute deepCopy() {
        AssetAttribute copy = new AssetAttribute(getObjectValue().deepCopy());
        copy.name = name;
        copy.assetId = assetId;
        return copy;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            ", name='" + name + '\'' +
            "} " + objectValue.toJson();
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectValue, name, assetId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof AssetAttribute))
            return false;
        AssetAttribute that = (AssetAttribute) obj;

        return Objects.equals(assetId, that.assetId)
            && Objects.equals(name, that.name)
            && Objects.equals(objectValue, that.objectValue);
    }

    //    ---------------------------------------------------
    //    FUNCTIONAL METHODS BELOW
    //    ---------------------------------------------------

    public static Optional<AssetAttribute> attributeFromJson(ObjectValue objectValue, String assetId, String name) {
        if (objectValue == null) {
            return Optional.of(new AssetAttribute(name));
        }

        AssetAttribute attribute = new AssetAttribute(objectValue);
        if (!isNullOrEmpty(assetId)) {
            attribute.setAssetId(assetId);
        }
        if (!isNullOrEmpty(name)) {
            attribute.setName(name);
        }
        return Optional.of(attribute);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<AssetAttribute> getAddedOrModifiedAttributes(List<AssetAttribute> oldAttributes,
                                                                      List<AssetAttribute> newAttributes) {
        return getAddedOrModifiedAttributes(oldAttributes, newAttributes, key -> false);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<AssetAttribute> getAddedOrModifiedAttributes(List<AssetAttribute> oldAttributes,
                                                                      List<AssetAttribute> newAttributes,
                                                                      Predicate<String> ignoredAttributeKeys) {
        return getAddedOrModifiedAttributes(oldAttributes, newAttributes, name -> false, name -> false, ignoredAttributeKeys);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<AssetAttribute> getAddedOrModifiedAttributes(List<AssetAttribute> oldAttributes,
                                                                      List<AssetAttribute> newAttributes,
                                                                      Predicate<String> ignoredAttributeNames,
                                                                      Predicate<String> ignoredAttributeKeys) {
        return getAddedOrModifiedAttributes(
            oldAttributes,
            newAttributes,
            null,
            ignoredAttributeNames,
            ignoredAttributeKeys);
    }

    /**
     * @return All attributes that exist only in the new list or are different than any attribute in the old list.
     */
    public static Stream<AssetAttribute> getAddedOrModifiedAttributes(List<AssetAttribute> oldAttributes,
                                                                      List<AssetAttribute> newAttributes,
                                                                      Predicate<String> limitToAttributeNames,
                                                                      Predicate<String> ignoredAttributeNames,
                                                                      Predicate<String> ignoredAttributeKeys) {
        return newAttributes.stream().filter(newAttribute -> oldAttributes.stream().noneMatch(
            oldAttribute -> newAttribute.getObjectValue().equalsIgnoreKeys(oldAttribute.getObjectValue(), ignoredAttributeKeys))
        ).filter(addedOrModifiedAttribute ->
            !addedOrModifiedAttribute.getName().isPresent() ||
                (limitToAttributeNames == null && ignoredAttributeNames == null) ||
                (limitToAttributeNames != null && limitToAttributeNames.test(addedOrModifiedAttribute.getName().get())) ||
                (ignoredAttributeNames != null && !ignoredAttributeNames.test(addedOrModifiedAttribute.getName().get()))
        );
    }

    /**
     * @return All attributes that exist only in the new list (based on name).
     */
    public static Stream<AssetAttribute> getAddedAttributes(List<AssetAttribute> oldAttributes,
                                                            List<AssetAttribute> newAttributes) {
        return newAttributes.stream().filter(newAttribute -> oldAttributes.stream().noneMatch(
                oldAttribute -> newAttribute.getNameOrThrow().equals(newAttribute.getNameOrThrow())
        ));
    }

    public static Stream<AssetAttribute> attributesFromJson(ObjectValue objectValue, String assetId) {
        if (objectValue == null || objectValue.keys().length == 0) {
            return Stream.empty();
        }
        //noinspection ConstantConditions
        return Arrays
            .stream(objectValue.keys())
            .map(key -> new Pair<>(key, objectValue.getObject(key).orElse(null)))
            .map(pair -> attributeFromJson(pair.value, assetId, pair.key))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    /**
     * Maps the attribute stream to a {@link ObjectValue}, duplicate attribute names are not
     * allowed (internally this is a hash map, thus not allowing duplicate JSON object property
     * names).
     */
    public static <A extends Attribute> Optional<ObjectValue> attributesToJson(Collection<A> attributes) {
        if (attributes.size() == 0)
            return Optional.empty();
        ObjectValue objectValue = Values.createObject();

        for (A attribute : attributes) {
            if (attribute.getName().isPresent())
                objectValue.put(attribute.getName().get(), attribute.getObjectValue());
        }
        return Optional.of(objectValue);
    }
}
