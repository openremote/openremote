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
package org.openremote.model.attribute;

import com.google.gwt.regexp.shared.RegExp;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.ValidationFailure;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static org.openremote.model.attribute.MetaItem.MetaItemFailureReason.*;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * A named arbitrary {@link Value}. A meta item must have a value to be stored.
 * <p>
 * Name should be a URI, thus avoiding collisions and representing "ownership" of the meta item.
 */
public class MetaItem extends AbstractValueHolder {

    public enum MetaItemFailureReason implements ValidationFailure.Reason {
        META_ITEM_NAME_IS_REQUIRED,
        META_ITEM_VALUE_IS_REQUIRED,
        META_ITEM_MISSING,
        META_ITEM_DUPLICATION,
        META_ITEM_VALUE_MISMATCH
    }

    public MetaItem() {
        this(Values.createObject());
    }

    public MetaItem(ObjectValue objectValue) {
        super(objectValue);
    }

    public MetaItem(String name, Value value) {
        super(Values.createObject());
        setName(name);
        setValue(value);
    }

    public MetaItem(MetaItemDescriptor metaItemDescriptor) {
        super(Values.createObject());
        setName(metaItemDescriptor.getUrn());
        setValue(metaItemDescriptor.getInitialValue());
    }

    public MetaItem(HasUniqueResourceName hasUniqueResourceName, Value value) {
        this(hasUniqueResourceName.getUrn(), value);
    }

    public Optional<String> getName() {
        return getObjectValue().getString("name");
    }

    public void setName(String name) {
        getObjectValue().put("name", TextUtil.requireNonNullAndNonEmpty(name));
    }

    public void clearName() {
        getObjectValue().remove("name");
    }

    @Override
    public List<ValidationFailure> getValidationFailures() {
        return getValidationFailures(Optional.empty());
    }

    public List<ValidationFailure> getValidationFailures(Optional<MetaItemDescriptor> metaItemDescriptor) {

        List<ValidationFailure> failures = super.getValidationFailures();

        // Check name
        if (!getName().isPresent())
            failures.add(new ValidationFailure(META_ITEM_NAME_IS_REQUIRED));

        // MetaItemDescriptor validation takes priority
        metaItemDescriptor
            .ifPresent(descriptor ->
                descriptor.getValidator()
                    .map(validator ->
                        validator.apply(getValue().orElse(null))
                            .map(failures::add)
                            .orElse(true)
                    )
                    .orElseGet(
                        () -> {
                            if (!getValue().isPresent()) {
                                failures.add(new ValidationFailure(META_ITEM_VALUE_IS_REQUIRED, descriptor.getValueType().name())
                                );
                            }

                            if (getValue().map(Value::getType).map(type -> descriptor.getValueType() != type).orElse(true)) {
                                failures.add(new ValidationFailure(META_ITEM_VALUE_MISMATCH, descriptor.getValueType().name()));
                                return true;
                            }

                            if (getValue().isPresent() && !isNullOrEmpty(descriptor.getPattern())) {
                                String valueStr = getValue().get().toString();

                                if (isNullOrEmpty(valueStr)) {
                                    failures.add(new ValidationFailure(MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED, descriptor.getValueType().name()));
                                    return true;
                                }

                                // Do case insensitive regex (can't include this flag in the pattern like in normal java)
                                if (!RegExp.compile(descriptor.getPattern(), "i").test(valueStr)) {
                                    failures.add(new ValidationFailure(MetaItemFailureReason.META_ITEM_VALUE_MISMATCH, descriptor.getPatternFailureMessage()));
                                    return true;
                                }
                            }

                            // Here because Optional doesn't support ifAbsent
                            return true;
                        }
                    ));

        if (!metaItemDescriptor.isPresent() && !getValue().isPresent()) {
            failures.add(new ValidationFailure(META_ITEM_VALUE_IS_REQUIRED));
        }

        return failures;
    }

    public MetaItem copy() {
        return new MetaItem(getObjectValue().deepCopy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MetaItem)) {
            return false;
        }

        MetaItem metaItem = (MetaItem) o;
        return Objects.equals(getName(), metaItem.getName())
            && getValue().equals(metaItem.getValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            getObjectValue().toJson() +
            '}';
    }

    //    ---------------------------------------------------
    //    FUNCTIONAL METHODS BELOW
    //    ---------------------------------------------------

    public static boolean isMetaNameEqualTo(MetaItem item, String name) {
        if (item == null)
            return false;

        Optional<String> itemName = item.getName();
        return (!itemName.isPresent() && name == null) || (itemName.isPresent() && itemName.get().equals(name));
    }

    public static Predicate<MetaItem> isMetaNameEqualTo(String name) {
        return metaItem -> isMetaNameEqualTo(metaItem, name);
    }

    public static boolean isMetaNameEqualTo(MetaItem item, HasUniqueResourceName hasUniqueResourceName) {
        return hasUniqueResourceName != null && isMetaNameEqualTo(item, hasUniqueResourceName.getUrn());
    }

    public static Predicate<MetaItem> isMetaNameEqualTo(HasUniqueResourceName hasUniqueResourceName) {
        return metaItem -> isMetaNameEqualTo(metaItem, hasUniqueResourceName);
    }

    // STREAM AND COLLECTION METHODS BELOW

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, String name, MetaItem newMetaItem) {
        metaItems.removeIf(isMetaNameEqualTo(name));
        metaItems.add(newMetaItem);
    }

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, String name, Value newValue) {
        metaItems.removeIf(isMetaNameEqualTo(name));
        metaItems.add(new MetaItem(name, newValue));
    }

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, String name, T newMetaItems) {
        metaItems.removeIf(isMetaNameEqualTo(name));
        metaItems.addAll(newMetaItems);
    }

    public static <T extends Collection<MetaItem>, V extends Collection<Value>> void replaceMetaByNameWithValues(T metaItems, String name, V newValues) {
        metaItems.removeIf(isMetaNameEqualTo(name));
        newValues
            .stream()
            .map(value -> new MetaItem(name, value))
            .forEach(metaItems::add);
    }

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, HasUniqueResourceName hasUniqueResourceName, MetaItem newMetaItem) {
        replaceMetaByName(metaItems, hasUniqueResourceName.getUrn(), newMetaItem);
    }

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, HasUniqueResourceName hasUniqueResourceName, Value newValue) {
        replaceMetaByName(metaItems, hasUniqueResourceName.getUrn(), newValue);
    }

    public static <T extends Collection<MetaItem>> void replaceMetaByName(T metaItems, HasUniqueResourceName hasUniqueResourceName, T newMetaItems) {
        replaceMetaByName(metaItems, hasUniqueResourceName.getUrn(), newMetaItems);
    }

    public static <T extends Collection<MetaItem>, V extends Collection<Value>> void replaceMetaByNameWithValues(T metaItems, HasUniqueResourceName hasUniqueResourceName, V newValues) {
        replaceMetaByNameWithValues(metaItems, hasUniqueResourceName.getUrn(), newValues);
    }
}