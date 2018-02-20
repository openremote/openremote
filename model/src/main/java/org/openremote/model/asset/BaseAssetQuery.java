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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.value.*;

import java.util.Arrays;
import java.util.Locale;

/**
 * Encapsulate asset query restriction, projection, and ordering of results.
 */
@SuppressWarnings("unchecked")
public class BaseAssetQuery<CHILD extends BaseAssetQuery<CHILD>> {

    public static final ArrayValue queriesAsValue(BaseAssetQuery... queries) {
        ArrayValue arrayValue = Values.createArray();

        for (BaseAssetQuery query : queries) {
            ObjectValue objectValue = Values.createObject();
            if (query.select != null) {
                objectValue.put("select", query.select.toModelValue());
            }
            if (query.type != null) {
                objectValue.put("type", query.type.toModelValue());
            }
            if (query.attribute != null && query.attribute.length > 0) {
                ArrayValue attributeArray = Values.createArray();
                for (AttributePredicate attributePredicate : query.attribute) {
                    attributeArray.add(attributePredicate.toModelValue());
                }
                objectValue.put("attribute", attributeArray);
            }
            arrayValue.add(objectValue);
        }
        return arrayValue;
    }

    public static final AssetQuery objectValueAsQuery(ObjectValue objectValue) {
        AssetQuery assetQuery = new AssetQuery();

        objectValue.getObject("select").ifPresent(selectValue -> {
            assetQuery.select = Select.fromObjectValue(selectValue);
        });

        objectValue.getObject("type").ifPresent(typeValue -> {
            assetQuery.type = StringPredicate.fromObjectValue(typeValue);
        });

        objectValue.getArray("attribute").ifPresent(attributeValue -> {
            assetQuery.attribute = attributeValue.stream()
                .map(val -> (ObjectValue) val)
                .map(attributePredicateValue -> {
                    AttributePredicate attributePredicate = new AttributePredicate();

                    attributePredicate.name = StringPredicate.fromObjectValue(attributePredicateValue
                        .getObject("name")
                        .orElseThrow(() -> new IllegalArgumentException("StringPredicate missing for name in AttributePredicate"))
                    );

                    ObjectValue predicateValue = attributePredicateValue
                        .getObject("value")
                        .orElseThrow(() -> new IllegalArgumentException("value missing in AttributePredicate"));

                    predicateValue.getString("predicateType").ifPresent(predicateType -> {
                        switch (predicateType) {
                            case "StringPredicate":
                                attributePredicate.value = StringPredicate.fromObjectValue(predicateValue);
                                break;
                            case "BooleanPredicate":
                                attributePredicate.value = BooleanPredicate.fromObjectValue(predicateValue);
                                break;
                            case "StringArrayPredicate":
                                attributePredicate.value = StringArrayPredicate.fromObjectValue(predicateValue);
                                break;
                            case "DateTimePredicate":
                                attributePredicate.value = DateTimePredicate.fromObjectValue(predicateValue);
                                break;
                            case "NumberPredicate":
                                attributePredicate.value = NumberPredicate.fromObjectValue(predicateValue);
                                break;
                        }
                    });
                    return attributePredicate;
                })
                .toArray(AttributePredicate[]::new);
        });

        return assetQuery;
    }

    public enum Include {
        ALL_EXCEPT_PATH_AND_ATTRIBUTES,
        ALL_EXCEPT_PATH,
        ONLY_ID_AND_NAME,
        ONLY_ID_AND_NAME_AND_ATTRIBUTES,
        ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES,
        ALL
    }

    public enum Access {
        PRIVATE_READ,
        RESTRICTED_READ,
        PUBLIC_READ
    }

    public static class Select {
        public Include include;
        public boolean recursive;
        public Access access;
        public String[] attributeNames;

        public Select() {
        }

        public Select(Include include) {
            this(include, Access.PRIVATE_READ);
        }

        public Select(Include include, Access access) {
            this(include, false, access);
        }

        public Select(Include include, boolean recursive, Access access) {
            this(include, recursive, access, (String[]) null);
        }

        public Select(Include include, boolean recursive, String... attributeNames) {
            this(include, recursive, Access.PRIVATE_READ, attributeNames);
        }

        public Select(Include include, boolean recursive, Access access, String... attributeNames) {
            if (include == null) {
                include = Include.ALL_EXCEPT_PATH_AND_ATTRIBUTES;
            }
            this.include = include;
            this.access = access;
            this.recursive = recursive;
            this.attributeNames = attributeNames;
        }

        public Select include(Include include) {
            this.include = include;
            return this;
        }

        public Select filterAccess(Access access) {
            this.access = access;
            return this;
        }

        public Select filterAttributes(String... attributeNames) {
            this.attributeNames = attributeNames;
            return this;
        }

        public static Select fromObjectValue(ObjectValue objectValue) {
            Select select = new Select();
            objectValue.getString("include").ifPresent(include -> {
                select.include = Include.valueOf(include);
            });
            objectValue.getString("access").ifPresent(access -> {
                select.access = Access.valueOf(access);
            });
            objectValue.getBoolean("recursive").ifPresent(recursive -> {
                select.recursive = recursive;
            });
            objectValue.getArray("attributeNames").ifPresent(attributeNames -> {
                select.attributeNames = attributeNames.stream()
                    .map(value -> (StringValue) value)
                    .map(StringValue::getString)
                    .toArray(String[]::new);
            });
            return select;
        }

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("include", Values.create(include.toString()));
            objectValue.put("recursive", Values.create(recursive));
            objectValue.put("access", Values.create(access.toString()));
            objectValue.put("attributeNames", Values.createArray().addAll(Arrays.stream(attributeNames).map(Values::create).toArray(Value[]::new)));
            return objectValue;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "include=" + include +
                ", recursive=" + recursive +
                ", access=" + access +
                ", attributeNames=" + Arrays.toString(attributeNames) +
                '}';
        }
    }

    /**
     * String matching options
     */
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

    /**
     * Binary operators
     */
    public enum Operator {
        EQUALS,
        GREATER_THAN,
        GREATER_EQUALS,
        LESS_THAN,
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
    public interface ValuePredicate<T> {
        ObjectValue toModelValue();
    }

    public static class ValueNotEmptyPredicate implements ValuePredicate<Value> {
        @Override
        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "ValueNotEmptyPredicate");
            return objectValue;
        }
    }

    public static class StringPredicate implements ValuePredicate<String> {
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

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "StringPredicate");
            objectValue.put("match", Values.create(match.toString()));
            objectValue.put("caseSensitive", Values.create(caseSensitive));
            objectValue.put("value", Values.create(value));
            return objectValue;
        }

        public static StringPredicate fromObjectValue(ObjectValue objectValue) {
            StringPredicate stringPredicate = new StringPredicate();
            objectValue.getString("match").ifPresent(match -> {
                stringPredicate.match = Match.valueOf(match);
            });
            objectValue.getBoolean("caseSensitive").ifPresent(caseSensitive -> {
                stringPredicate.caseSensitive = caseSensitive;
            });
            objectValue.getString("value").ifPresent(value -> {
                stringPredicate.value = value;
            });
            return stringPredicate;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "match=" + match +
                ", caseSensitive=" + caseSensitive +
                ", value='" + value + '\'' +
                '}';
        }
    }

    public static class BooleanPredicate implements ValuePredicate<Boolean> {
        public boolean value;

        public BooleanPredicate() {
        }

        public BooleanPredicate(boolean value) {
            this.value = value;
        }

        public BooleanPredicate value(boolean value) {
            this.value = value;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "predicate=" + value +
                '}';
        }

        @Override
        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "BooleanPredicate");
            objectValue.put("value", Values.create(value));
            return objectValue;
        }

        public static BooleanPredicate fromObjectValue(ObjectValue objectValue) {
            BooleanPredicate booleanPredicate = new BooleanPredicate();
            booleanPredicate.value = objectValue
                .getBoolean("value")
                .orElseThrow(() -> new IllegalArgumentException("value missing for BooleanPredicate"));
            return booleanPredicate;
        }
    }

    public static class StringArrayPredicate implements ValuePredicate<String[]> {
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

        @Override
        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "StringArrayPredicate");
            ArrayValue valuesArray = Values.createArray();
            for (StringPredicate stringPredicate : predicates) {
                valuesArray.add(stringPredicate.toModelValue());
            }
            objectValue.put("value", valuesArray);
            return objectValue;
        }

        public static StringArrayPredicate fromObjectValue(ObjectValue objectValue) {
            StringArrayPredicate stringArrayPredicate = new StringArrayPredicate();
            objectValue.getArray("value").ifPresent(arrayValue -> {
                ;
                stringArrayPredicate.predicates = objectValue.stream()
                    .map(val -> (ObjectValue) val)
                    .map(StringPredicate::fromObjectValue)
                    .toArray(StringPredicate[]::new);
            });
            return stringArrayPredicate;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "predicates=" + Arrays.toString(predicates) +
                '}';
        }
    }

    public static class DateTimePredicate implements ValuePredicate<Long> {
        public String value;
        public String rangeValue; // Used as upper bound when Operator.BETWEEN
        public Operator operator = Operator.EQUALS;
        public String dateFormat = "YYYY-MM-DD HH24:MI:SS";//postgres dateformat

        public DateTimePredicate() {
        }

        public DateTimePredicate(Operator operator, String value) {
            this.operator = operator;
            this.value = value;
        }

        public DateTimePredicate(String afterValue, String beforeValue) {
            this.operator = Operator.BETWEEN;
            this.value = afterValue;
            this.rangeValue = beforeValue;
        }

        public DateTimePredicate dateMatch(Operator dateMatch) {
            this.operator = dateMatch;
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
            this.operator = Operator.BETWEEN;
            this.rangeValue = beforeValue;
            return this;
        }

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "StringPredicate");
            objectValue.put("dateFormat", Values.create(dateFormat));
            objectValue.put("value", Values.create(value));
            objectValue.put("rangeValue", Values.create(rangeValue));
            objectValue.put("operator", Values.create(operator.toString()));
            return objectValue;
        }

        public static DateTimePredicate fromObjectValue(ObjectValue objectValue) {
            DateTimePredicate dateTimePredicate = new DateTimePredicate();

            objectValue.getString("dateFormat").ifPresent(format -> {
                dateTimePredicate.dateFormat = format;
            });
            objectValue.getString("value").ifPresent(value -> {
                dateTimePredicate.value = value;
            });
            objectValue.getString("rangeValue").ifPresent(rangeValue -> {
                dateTimePredicate.rangeValue = rangeValue;
            });
            objectValue.getString("operator").ifPresent(operator -> {
                dateTimePredicate.operator = Operator.valueOf(operator);
            });
            return dateTimePredicate;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "value='" + value + '\'' +
                ", rangeValue='" + rangeValue + '\'' +
                ", operator =" + operator +
                ", dateFormat='" + dateFormat + '\'' +
                '}';
        }
    }

    public static class NumberPredicate implements ValuePredicate<Double> {
        public double value;
        public double rangeValue; // Used as upper bound when Operator.BETWEEN
        public Operator operator = Operator.EQUALS;
        public NumberType numberType = NumberType.DOUBLE;

        public NumberPredicate() {
        }

        public NumberPredicate(double value) {
            this.value = value;
        }

        public NumberPredicate(double value, Operator operator) {
            this.value = value;
            this.operator = operator;
        }

        public NumberPredicate(double value, NumberType numberType) {
            this.value = value;
            this.numberType = numberType;
        }

        public NumberPredicate(double value, Operator operator, NumberType numberType) {
            this.value = value;
            this.operator = operator;
            this.numberType = numberType;
        }

        public NumberPredicate predicate(double predicate) {
            this.value = predicate;
            return this;
        }

        public NumberPredicate numberMatch(Operator operator) {
            this.operator = operator;
            return this;
        }

        public NumberPredicate numberType(NumberType numberType) {
            this.numberType = numberType;
            return this;
        }

        public NumberPredicate rangeValue(double rangeValue) {
            this.operator = Operator.BETWEEN;
            this.rangeValue = rangeValue;
            return this;
        }

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("predicateType", "StringPredicate");
            objectValue.put("value", Values.create(value));
            objectValue.put("rangeValue", Values.create(rangeValue));
            objectValue.put("operator", Values.create(operator.toString()));
            return objectValue;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "value=" + value +
                ", rangeValue=" + rangeValue +
                ", operator=" + operator +
                ", numberType=" + numberType +
                '}';
        }

        public static NumberPredicate fromObjectValue(ObjectValue objectValue) {
            NumberPredicate numberPredicate = new NumberPredicate();
            objectValue.getNumber("value").ifPresent(value -> {
                numberPredicate.value = value;
            });
            objectValue.getNumber("rangeValue").ifPresent(rangeValue -> {
                numberPredicate.rangeValue = rangeValue;
            });
            objectValue.getString("operator").ifPresent(operator -> {
                numberPredicate.operator = Operator.valueOf(operator);
            });
            return numberPredicate;
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

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", noParent=" + noParent +
                '}';
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

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "path=" + Arrays.toString(path) +
                '}';
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

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "realmId='" + realmId + '\'' +
                ", realm='" + realm + '\'' +
                '}';
        }
    }

    public static class AttributePredicate {
        public StringPredicate name;
        public ValuePredicate value;

        public AttributePredicate() {
        }

        public AttributePredicate(String name) {
            this(new StringPredicate(name));
        }

        public AttributePredicate(StringPredicate name) {
            this.name = name;
        }

        public AttributePredicate(ValuePredicate value) {
            this.value = value;
        }

        public AttributePredicate(StringPredicate name, ValuePredicate value) {
            this.name = name;
            this.value = value;
        }

        public AttributePredicate name(StringPredicate name) {
            this.name = name;
            return this;
        }

        public AttributePredicate value(ValuePredicate value) {
            this.value = value;
            return this;
        }

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("name", name.toModelValue());
            objectValue.put("value", value.toModelValue());
            return objectValue;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "name=" + name +
                ", value=" + value +
                '}';
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

        public AttributeMetaPredicate(MetaItemDescriptor metaItemDescriptor) {
            this.itemNamePredicate = new StringPredicate(metaItemDescriptor.getUrn());
        }

        public AttributeMetaPredicate(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(MetaItemDescriptor metaItemDescriptor, ValuePredicate itemValuePredicate) {
            this(new StringPredicate(metaItemDescriptor.getUrn()), itemValuePredicate);
        }

        public AttributeMetaPredicate itemName(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            return this;
        }

        public AttributeMetaPredicate itemName(MetaItemDescriptor metaItemDescriptor) {
            return itemName(new StringPredicate(metaItemDescriptor.getUrn()));
        }

        public AttributeMetaPredicate itemValue(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "itemNamePredicate=" + itemNamePredicate +
                ", itemValuePredicate=" + itemValuePredicate +
                '}';
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

        public AttributeRefPredicate(MetaItemDescriptor metaItemDescriptor, String entityId, String attributeName) {
            this(metaItemDescriptor.getUrn(), entityId, attributeName);
        }

        public AttributeRefPredicate(StringPredicate name, AttributeRef attributeRef) {
            super(
                name,
                new StringArrayPredicate(
                    new StringPredicate(attributeRef.getEntityId()),
                    new StringPredicate(attributeRef.getAttributeName())
                ));
        }

        public AttributeRefPredicate(MetaItemDescriptor metaItemDescriptor, AttributeRef attributeRef) {
            this(new StringPredicate(metaItemDescriptor.getUrn()), attributeRef);
        }
    }

    public static class CalendarEventActivePredicate {

        @JsonProperty("timestamp")
        public long timestampSeconds;

        @JsonCreator
        public CalendarEventActivePredicate(@JsonProperty("timestamp") long timestampSeconds) {
            this.timestampSeconds = timestampSeconds;
        }
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = RadialLocationPredicate.class, name = "radial"),
        @JsonSubTypes.Type(value = RectangularLocationPredicate.class, name = "rect")
    })
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "predicateType"
    )
    public static class LocationPredicate {
    }

    public static class RadialLocationPredicate extends LocationPredicate {

        public boolean negated;
        public int radius;
        public double lat;
        public double lng;

        @JsonCreator
        public RadialLocationPredicate(@JsonProperty("radius") int radius,
                                       @JsonProperty("lat") double lat,
                                       @JsonProperty("lng") double lng,
                                       @JsonProperty("negated") boolean negated) {
            this.radius = radius;
            this.lat = lat;
            this.lng = lng;
            this.negated = negated;
        }

        public RadialLocationPredicate(@JsonProperty("radius") int radius,
                                       @JsonProperty("lat") double lat,
                                       @JsonProperty("lng") double lng) {
            this(radius, lat, lng, false);
        }

        public RadialLocationPredicate negate() {
            negated = true;
            return this;
        }
    }

    public static class RectangularLocationPredicate extends LocationPredicate {

        public boolean negated;
        public double latMin;
        public double lngMin;
        public double latMax;
        public double lngMax;

        @JsonCreator
        public RectangularLocationPredicate(@JsonProperty("latMin") double latMin,
                                            @JsonProperty("lngMin") double lngMin,
                                            @JsonProperty("latMax") double latMax,
                                            @JsonProperty("lngMax") double lngMax,
                                            @JsonProperty("negated") boolean negated) {
            this.latMin = latMin;
            this.lngMin = lngMin;
            this.latMax = latMax;
            this.lngMax = lngMax;
            this.negated = negated;
        }

        public RectangularLocationPredicate(@JsonProperty("latMin") double latMin,
                                            @JsonProperty("lngMin") double lngMin,
                                            @JsonProperty("latMax") double latMax,
                                            @JsonProperty("lngMax") double lngMax) {
            this(latMin, lngMin, latMax, lngMax, false);
        }

        public RectangularLocationPredicate negate() {
            negated = true;
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

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "property=" + property +
                ", descending=" + descending +
                '}';
        }
    }

    // Projection
    public Select select;

    // Restriction predicates
    public String id;
    public StringPredicate name;
    public ParentPredicate parent;
    public PathPredicate path;
    public TenantPredicate tenant;
    public String userId;
    public StringPredicate type;
    public AttributePredicate[] attribute;
    public AttributeMetaPredicate[] attributeMeta;
    public CalendarEventActivePredicate calendarEventActive;
    public LocationPredicate location;

    // Ordering
    public OrderBy orderBy;

    protected BaseAssetQuery() {
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
        this.name = name;
        return (CHILD) this;
    }

    public CHILD parent(String id) {
        return parent(new ParentPredicate(id));
    }

    public CHILD parent(AssetType assetType) {
        return parent(new ParentPredicate().type(assetType));
    }

    public CHILD parent(ParentPredicate parentPredicate) {
        this.parent = parentPredicate;
        return (CHILD) this;
    }

    public CHILD path(PathPredicate pathPredicate) {
        this.path = pathPredicate;
        return (CHILD) this;
    }

    public CHILD tenant(TenantPredicate tenantPredicate) {
        this.tenant = tenantPredicate;
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

    public CHILD type(String type) {
        return type(new StringPredicate(type));
    }

    public CHILD type(AssetType assetType) {
        return type(new StringPredicate(assetType.getValue()));
    }

    public CHILD attributes(AttributePredicate... attributePredicates) {
        this.attribute = attributePredicates;
        return (CHILD) this;
    }

    public CHILD attributeName(String attributeName) {
        return attributes(new AttributePredicate(attributeName));
    }

    public CHILD attributeName(Match match, String attributeName) {
        return attributes(new AttributePredicate(new StringPredicate(match, attributeName)));
    }

    /**
     * Match non-empty attribute value.
     */
    public CHILD attributeValue(String name) {
        return attributes(new AttributePredicate(new StringPredicate(name), new ValueNotEmptyPredicate()));
    }

    public CHILD attributeValue(String name, ValuePredicate valuePredicate) {
        return attributes(new AttributePredicate(new StringPredicate(name), valuePredicate));
    }

    public CHILD attributeValue(String name, boolean b) {
        return attributeValue(name, new BooleanPredicate(b));
    }

    public CHILD attributeValue(String name, String s) {
        return attributeValue(name, new StringPredicate(s));
    }

    public CHILD attributeValue(String name, Match match, String s) {
        return attributeValue(name, new StringPredicate(match, s));
    }

    public CHILD attributeValue(String name, Match match, boolean caseSensitive, String s) {
        return attributeValue(name, new StringPredicate(match, caseSensitive, s));
    }

    public CHILD attributeValue(String name, double d) {
        return attributeValue(name, new NumberPredicate(d));
    }

    public CHILD attributeValue(String name, Operator operator, double d) {
        return attributeValue(name, new NumberPredicate(d, operator));
    }

    public CHILD attributeMeta(AttributeMetaPredicate... attributeMetaPredicates) {
        this.attributeMeta = attributeMetaPredicates;
        return (CHILD) this;
    }

    public CHILD orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return (CHILD) this;
    }

    /**
     * Will filter out assets that have a {@link CalendarEventConfiguration} attribute that results in the event
     * not being 'active/in progress' at the specified time. Assets without a {@link CalendarEventConfiguration}
     * attribute are assumed to always be in progress (i.e. will always pass this check).
     * <p>
     * <b>
     * NOTE: This predicate is applied in memory and the results should be limited as much as possible by applying
     * other predicates to the query to avoid performance issues.
     * </b>
     */
    public CHILD calendarEventActive(long timestampSeconds) {
        calendarEventActive = new CalendarEventActivePredicate(timestampSeconds);
        return (CHILD) this;
    }

    public CHILD location(LocationPredicate locationPredicate) {
        this.location = locationPredicate;
        return (CHILD) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "select=" + select +
            ", id='" + id + '\'' +
            ", name=" + name +
            ", parent=" + parent +
            ", path=" + path +
            ", tenant=" + tenant +
            ", userId='" + userId + '\'' +
            ", type=" + type +
            ", attribute=" + Arrays.toString(attribute) +
            ", attributeMeta=" + Arrays.toString(attributeMeta) +
            ", orderBy=" + orderBy +
            '}';
    }
}
