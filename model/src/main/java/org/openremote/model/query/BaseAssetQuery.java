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
package org.openremote.model.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.CalendarEventConfiguration;
import org.openremote.model.query.filter.*;
import org.openremote.model.value.*;

import java.util.*;

/**
 * Encapsulate asset query restriction, projection, and ordering of results.
 */
// TODO: Add AssetQuery support for arbitrary attribute property path amd value (e.g. attributes.consoleProviders.geofence.version == "ORConsole")
@SuppressWarnings("unchecked")
public class BaseAssetQuery<CHILD extends BaseAssetQuery<CHILD>> {

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

    /**
     * String matching options
     */
    public enum Match {
        EXACT,
        NOT_EXACT,
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
        NOT_EQUALS,
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

        public Select recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public ObjectValue toModelValue() {
            ObjectValue objectValue = Values.createObject();
            objectValue.put("include", Values.create(include.toString()));
            objectValue.put("recursive", Values.create(recursive));
            objectValue.put("access", Values.create(access.toString()));
            objectValue.put("attributeNames",
                            Values.createArray().addAll(Arrays.stream(attributeNames).map(Values::create).toArray(Value[]::new)));
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
    public List<String> ids;
    public StringPredicate name;
    public ParentPredicate parent;
    public PathPredicate path;
    public TenantPredicate tenant;
    public String userId;
    public StringPredicate type;
    public AttributePredicate[] attribute;
    public AttributeMetaPredicate[] attributeMeta;
    public CalendarEventActivePredicate calendarEventActive;
    // Ordering
    public OrderBy orderBy;
    public int limit;

    protected BaseAssetQuery() {
    }

    // TODO: Remove this
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

    // TODO: Remove this
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
                                                                                  .orElseThrow(() -> new IllegalArgumentException(
                                                                                      "StringPredicate missing for name in AttributePredicate"))
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

    public CHILD select(Select select) {
        if (select == null) {
            select = new Select();
        }

        this.select = select;
        return (CHILD) this;
    }

    @JsonProperty("id")
    public CHILD id(String id) {
        this.ids = Collections.singletonList(id);
        return (CHILD) this;
    }

    public CHILD ids(List<String> ids) {
        this.ids = ids;
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
     * Will filter out assets that have a {@link CalendarEventConfiguration} attribute that results in the event not
     * being 'active/in progress' at the specified time. Assets without a {@link CalendarEventConfiguration} attribute
     * are assumed to always be in progress (i.e. will always pass this check).
     *
     * <b>
     * NOTE: This predicate is applied in memory and the results should be limited as much as possible by applying other
     * predicates to the query to avoid performance issues.
     * </b>
     */
    public CHILD calendarEventActive(long timestampSeconds) {
        calendarEventActive = new CalendarEventActivePredicate(timestampSeconds);
        return (CHILD) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "select=" + select +
            ", id='" + ids + '\'' +
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
