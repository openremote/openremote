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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.attribute.AttributeRef;

import java.util.Locale;

/**
 * Encapsulate asset query restriction, projection, and ordering of results.
 */

public class AbstractAssetQuery<CHILD extends AbstractAssetQuery<CHILD>> {

    public enum Include {
        ALL_EXCEPT_PATH_AND_ATTRIBUTES,
        ALL_EXCEPT_PATH,
        ONLY_ID_AND_NAME,
        ONLY_ID_AND_NAME_AND_ATTRIBUTES,
        ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES,
        ALL
    }

    public static class Select {
        public Include include = Include.ALL_EXCEPT_PATH_AND_ATTRIBUTES;
        public boolean filterProtected;
        public boolean recursive;
        public String[] attributeNames;

        public Select() {
        }

        public Select(Include include) {
            this(include, false);
        }

        public Select(Include include, boolean filterProtected) {
            this(include, filterProtected, false);
        }

        public Select(Include include, boolean filterProtected, boolean recursive) {
            this(include, filterProtected, recursive, (String[]) null);
        }

        public Select(Include include, boolean filterProtected, boolean recursive, String... attributeNames) {
            if (include == null) {
                include = Include.ALL_EXCEPT_PATH_AND_ATTRIBUTES;
            }
            this.include = include;
            this.filterProtected = filterProtected;
            this.recursive = recursive;
            this.attributeNames = attributeNames;
        }

        public Select include(Include include) {
            this.include = include;
            return this;
        }

        public Select filterProtected(boolean filterProtected) {
            this.filterProtected = filterProtected;
            return this;
        }

        public Select filterAttributes(String... attributeNames) {
            this.attributeNames = attributeNames;
            return this;
        }
    }

    public enum Match {
        EXACT,
        BEGIN,
        END,
        CONTAINS;

        public String prepare(String string) {
            if (string == null)
                return null;
            switch (this) {
                case BEGIN:
                    return string + "%";
                case CONTAINS:
                    return "%" + string + "%";
                case END:
                    return "%" + string;
            }
            return string;
        }
    }

    public enum OperatorMatch {
        EXACT,
        GREATER_THEN,
        GREATER_EQUALS,
        LESS_THEN,
        LESS_EQUALS,
        BETWEEN
    }

    public enum NumberType {
        DOUBLE,
        INTEGER
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = StringPredicate.class, name = "string"),
        @JsonSubTypes.Type(value = BooleanPredicate.class, name = "boolean"),
        @JsonSubTypes.Type(value = StringArrayPredicate.class, name = "string-array"),
        @JsonSubTypes.Type(value = DateTimePredicate.class, name = "datetime"),
        @JsonSubTypes.Type(value = NumberPredicate.class, name = "number")
    })
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "predicateType"
    )
    public interface ValuePredicate {
    }

    public static class StringPredicate implements ValuePredicate {
        public Match match = Match.EXACT;
        public boolean caseSensitive = true;
        public String value;

        public StringPredicate() {
        }

        public StringPredicate(String value) {
            this.value = value;
        }

        public StringPredicate(Match match, String value) {
            this.match = match;
            this.value = value;
        }

        public StringPredicate(Match match, boolean caseSensitive, String value) {
            this.match = match;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        public StringPredicate match(Match match) {
            this.match = match;
            return this;
        }

        public StringPredicate caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public StringPredicate value(String value) {
            this.value = value;
            return this;
        }

        public String prepareValue() {
            String s = match.prepare(this.value);
            if (!caseSensitive)
                s = s.toUpperCase(Locale.ROOT);
            return s;
        }
    }

    public static class BooleanPredicate implements ValuePredicate {
        public boolean predicate;

        public BooleanPredicate() {
        }

        public BooleanPredicate(boolean predicate) {
            this.predicate = predicate;
        }

        public BooleanPredicate predicate(boolean predicate) {
            this.predicate = predicate;
            return this;
        }
    }

    public static class StringArrayPredicate implements ValuePredicate {
        public StringPredicate[] predicates = new StringPredicate[0];

        public StringArrayPredicate() {
        }

        public StringArrayPredicate(StringPredicate... predicates) {
            this.predicates = predicates;
        }

        public StringArrayPredicate predicates(StringPredicate... predicates) {
            this.predicates = predicates;
            return this;
        }
    }

    public static class DateTimePredicate implements ValuePredicate {
        public OperatorMatch operatorMatch = OperatorMatch.EXACT;
        public String dateFormat = "YYYY-MM-DD HH24:MI:SS";//postgres dateformat
        public String value;
        public String rangeValue;//used when operatorMatch is Between as end date

        public DateTimePredicate() {
        }

        public DateTimePredicate(OperatorMatch operatorMatch, String value) {
            this.operatorMatch = operatorMatch;
            this.value = value;
        }

        public DateTimePredicate(String afterValue, String beforeValue) {
            this.operatorMatch = OperatorMatch.BETWEEN;
            this.value = afterValue;
            this.rangeValue = beforeValue;
        }

        public DateTimePredicate dateMatch(OperatorMatch dateMatch) {
            this.operatorMatch = dateMatch;
            return this;
        }

        public DateTimePredicate dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public DateTimePredicate value(String value) {
            this.value = value;
            return this;
        }

        public DateTimePredicate rangeValue(String beforeValue) {
            this.operatorMatch = OperatorMatch.BETWEEN;
            this.rangeValue = beforeValue;
            return this;
        }
    }

    public static class NumberPredicate implements ValuePredicate {
        public double value;
        public double rangeValue; //used when operatorMatch is Between as second value
        public OperatorMatch operatorMatch = OperatorMatch.EXACT;
        public NumberType numberType = NumberType.DOUBLE;

        public NumberPredicate() {
        }

        public NumberPredicate(double value) {
            this.value = value;
        }

        public NumberPredicate(double value, OperatorMatch operatorMatch) {
            this.value = value;
            this.operatorMatch = operatorMatch;
        }

        public NumberPredicate(double value, NumberType numberType) {
            this.value = value;
            this.numberType = numberType;
        }

        public NumberPredicate(double value, OperatorMatch operatorMatch, NumberType numberType) {
            this.value = value;
            this.operatorMatch = operatorMatch;
            this.numberType = numberType;
        }

        public NumberPredicate predicate(double predicate) {
            this.value = predicate;
            return this;
        }

        public NumberPredicate numberMatch(OperatorMatch operatorMatch) {
            this.operatorMatch = operatorMatch;
            return this;
        }

        public NumberPredicate numberType(NumberType numberType) {
            this.numberType = numberType;
            return this;
        }

        public NumberPredicate rangeValue(double rangeValue) {
            this.operatorMatch = OperatorMatch.BETWEEN;
            this.rangeValue = rangeValue;
            return this;
        }
    }

    public static class ParentPredicate {
        public String id;
        public String type;
        public boolean noParent;

        public ParentPredicate() {
        }

        public ParentPredicate(String id) {
            this.id = id;
        }

        public ParentPredicate(boolean noParent) {
            this.noParent = noParent;
        }

        public ParentPredicate id(String id) {
            this.id = id;
            return this;
        }

        public ParentPredicate type(String type) {
            this.type = type;
            return this;
        }

        public ParentPredicate type(AssetType type) {
            return type(type.getValue());
        }

        public ParentPredicate noParent(boolean noParent) {
            this.noParent = noParent;
            return this;
        }
    }

    public static class PathPredicate {
        public String[] path;

        public PathPredicate() {
        }

        public PathPredicate(String[] path) {
            this.path = path;
        }

        public PathPredicate path(String[] path) {
            this.path = path;
            return this;
        }

        public boolean hasPath() {
            return path != null && path.length > 0;
        }
    }

    public static class TenantPredicate {
        public String realmId;
        public String realm;

        public TenantPredicate() {
        }

        public TenantPredicate(String realmId) {
            this.realmId = realmId;
        }

        public TenantPredicate realmId(String id) {
            this.realmId = id;
            return this;
        }

        public TenantPredicate realm(String name) {
            this.realm = name;
            return this;
        }
    }

    public static class AttributeMetaPredicate {
        public StringPredicate itemNamePredicate;
        public ValuePredicate itemValuePredicate;

        public AttributeMetaPredicate() {
        }

        public AttributeMetaPredicate(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
        }

        public AttributeMetaPredicate(AssetMeta assetMeta) {
            this.itemNamePredicate = new StringPredicate(assetMeta.getUrn());
        }

        public AttributeMetaPredicate(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(AssetMeta assetMeta, ValuePredicate itemValuePredicate) {
            this(new StringPredicate(assetMeta.getUrn()), itemValuePredicate);
        }

        public AttributeMetaPredicate itemName(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            return this;
        }

        public AttributeMetaPredicate itemName(AssetMeta assetMeta) {
            return itemName(new StringPredicate(assetMeta.getUrn()));
        }

        public AttributeMetaPredicate itemValue(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
            return this;
        }
    }

    public static class AttributeRefPredicate extends AttributeMetaPredicate {

        public AttributeRefPredicate() {
        }

        public AttributeRefPredicate(AttributeRef attributeRef) {
            this(attributeRef.getEntityId(), attributeRef.getAttributeName());
        }

        public AttributeRefPredicate(String entityId, String attributeName) {
            super(
                new StringArrayPredicate(
                    new StringPredicate(entityId),
                    new StringPredicate(attributeName)
                ));
        }

        public AttributeRefPredicate(String name, String entityId, String attributeName) {
            super(
                new StringPredicate(name),
                new StringArrayPredicate(
                    new StringPredicate(entityId),
                    new StringPredicate(attributeName)
                ));
        }

        public AttributeRefPredicate(AssetMeta assetMeta, String entityId, String attributeName) {
            this(assetMeta.getUrn(), entityId, attributeName);
        }

        public AttributeRefPredicate(StringPredicate name, AttributeRef attributeRef) {
            super(
                name,
                new StringArrayPredicate(
                    new StringPredicate(attributeRef.getEntityId()),
                    new StringPredicate(attributeRef.getAttributeName())
                ));
        }

        public AttributeRefPredicate(AssetMeta assetMeta, AttributeRef attributeRef) {
            this(new StringPredicate(assetMeta.getUrn()), attributeRef);
        }
    }

    public static class AttributePredicate {
        public StringPredicate itemNamePredicate;
        public ValuePredicate itemValuePredicate;

        public AttributePredicate() {
        }

        public AttributePredicate(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
        }

        public AttributePredicate(AssetMeta assetMeta) {
            this.itemNamePredicate = new StringPredicate(assetMeta.getUrn());
        }

        public AttributePredicate(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributePredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributePredicate(AssetMeta assetMeta, ValuePredicate itemValuePredicate) {
            this(new StringPredicate(assetMeta.getUrn()), itemValuePredicate);
        }

        public AttributePredicate itemName(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            return this;
        }

        public AttributePredicate itemName(AssetMeta assetMeta) {
            return itemName(new StringPredicate(assetMeta.getUrn()));
        }

        public AttributePredicate itemValue(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
            return this;
        }
    }

    public static class AttributePredicateArray {
        public AttributePredicate[] predicates = new AttributePredicate[0];

        public AttributePredicateArray() {
        }

        public AttributePredicateArray(AttributePredicate... predicates) {
            this.predicates = predicates;
        }

        public AttributePredicateArray predicates(AttributePredicate... predicates) {
            this.predicates = predicates;
            return this;
        }
    }

    public static class OrderBy {

        public enum Property {
            CREATED_ON,
            NAME,
            ASSET_TYPE,
            PARENT_ID,
            REALM_ID
        }

        public Property property;
        public boolean descending;

        public OrderBy() {
        }

        public OrderBy(Property property) {
            this.property = property;
        }

        public OrderBy(Property property, boolean descending) {
            this.property = property;
            this.descending = descending;
        }

        public OrderBy property(Property property) {
            this.property = property;
            return this;
        }

        public OrderBy descending(boolean descending) {
            this.descending = descending;
            return this;
        }
    }

    // Projection
    public Select select = new Select();

    // Restriction predicates
    public String id;
    public StringPredicate namePredicate;
    public ParentPredicate parentPredicate;
    public PathPredicate pathPredicate;
    public TenantPredicate tenantPredicate;
    public String userId;
    public StringPredicate type;
    public AttributeMetaPredicate attributeMetaPredicate;
    public AttributePredicateArray attributePredicateArray;

    // Ordering
    public OrderBy orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

    protected AbstractAssetQuery() {
    }

    public CHILD select(Select select) {
        if (select == null) {
            select = new Select();
        }

        this.select = select;
        return (CHILD) this;
    }

    public CHILD id(String id) {
        this.id = id;
        return (CHILD) this;
    }

    public CHILD name(String name) {
        return name(new StringPredicate(name));
    }

    public CHILD name(StringPredicate name) {
        this.namePredicate = name;
        return (CHILD) this;
    }

    public CHILD parent(String id) {
        return parent(new ParentPredicate(id));
    }

    public CHILD parent(AssetType assetType) {
        return parent(new ParentPredicate().type(assetType));
    }

    public CHILD parent(ParentPredicate parentPredicate) {
        this.parentPredicate = parentPredicate;
        return (CHILD) this;
    }

    public CHILD path(PathPredicate pathPredicate) {
        this.pathPredicate = pathPredicate;
        return (CHILD) this;
    }

    public CHILD tenant(TenantPredicate tenantPredicate) {
        this.tenantPredicate = tenantPredicate;
        return (CHILD) this;
    }

    public CHILD userId(String userId) {
        this.userId = userId;
        return (CHILD) this;
    }

    public CHILD type(StringPredicate type) {
        this.type = type;
        return (CHILD) this;
    }

    public CHILD type(AssetType assetType) {
        return type(new StringPredicate(assetType.getValue()));
    }

    public CHILD attributeMeta(AttributeMetaPredicate attributeMetaPredicate) {
        this.attributeMetaPredicate = attributeMetaPredicate;
        return (CHILD) this;
    }

    public CHILD attributes(AttributePredicateArray attributePredicateArray) {
        this.attributePredicateArray = attributePredicateArray;
        return (CHILD) this;
    }

    public CHILD orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return (CHILD) this;
    }
}
