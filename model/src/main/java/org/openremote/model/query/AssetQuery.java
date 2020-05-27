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

import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.query.filter.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Encapsulate asset query restriction, projection, and ordering of results.
 */
// TODO: Add AssetQuery support for arbitrary attribute property path amd value (e.g. attributes.consoleProviders.geofence.version == "ORConsole")
public class AssetQuery {

    public static class Select {

        public String[] attributes;
        public String[] meta;
        public boolean excludePath;
        public boolean excludeAttributeMeta;
        public boolean excludeAttributes;
        public boolean excludeAttributeValue;
        public boolean excludeAttributeTimestamp;
        public boolean excludeAttributeType;
        public boolean excludeParentInfo;

        public static Select selectExcludePathAndParentInfo() {
            return new Select()
                    .excludeAttributes(false)
                    .excludeAttributeMeta(false)
                    .excludeAttributeType(false)
                    .excludeAttributeTimestamp(false)
                    .excludeAttributeValue(false)
                    .excludePath(true)
                    .excludeParentInfo(true);
        }

        public static Select selectExcludePathAndAttributes() {
            return new Select()
                    .excludePath(true)
                    .excludeAttributes(true);
        }

        public static Select selectExcludeAll() {
            return new Select()
                    .excludeAttributes(true)
                    .excludeAttributeMeta(true)
                    .excludeAttributeType(true)
                    .excludeAttributeValue(true)
                    .excludeAttributeTimestamp(true)
                    .excludePath(true)
                    .excludeParentInfo(true);
        }

        public Select attributes(String... attributeNames) {
            this.attributes = attributeNames;
            return this;
        }

        public Select meta(String... metaUrns) {
            this.meta = metaUrns;
            return this;
        }

        public Select meta(MetaItemDescriptor... meta) {
            if (meta == null) {
                this.meta = null;
                return this;
            }

            return meta(Arrays.stream(meta).map(MetaItemDescriptor::getUrn).toArray(String[]::new));
        }

        public Select excludeAttributes(boolean exclude) {
            this.excludeAttributes = exclude;
            return this;
        }

        public Select excludeAttributeMeta(boolean exclude) {
            this.excludeAttributeMeta = exclude;
            return this;
        }

        public Select excludeAttributeType(boolean exclude) {
            this.excludeAttributeType = exclude;
            return this;
        }

        public Select excludeAttributeValue(boolean exclude) {
            this.excludeAttributeValue = exclude;
            return this;
        }

        public Select excludeAttributeTimestamp(boolean exclude) {
            this.excludeAttributeTimestamp = exclude;
            return this;
        }

        public Select excludePath(boolean exclude) {
            this.excludePath = exclude;
            return this;
        }

        public Select excludeParentInfo(boolean exclude) {
            this.excludeParentInfo = exclude;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "excludeAttributes=" + excludeAttributes +
                    ", excludeAttributeMeta=" + excludeAttributeMeta +
                    ", excludeAttributeValue=" + excludeAttributeValue +
                    ", excludeAttributeTimestamp=" + excludeAttributeTimestamp +
                    ", excludeAttributeType=" + excludeAttributeType +
                    ", excludePath=" + excludePath +
                    ", excludeParentInfo=" + excludeParentInfo +
                    ", attributeNames=" + Arrays.toString(attributes) +
                    '}';
        }
    }

    public static class OrderBy {

        public enum Property {
            CREATED_ON,
            NAME,
            ASSET_TYPE,
            PARENT_ID,
            REALM
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

    public enum Access {
        PRIVATE,
        PROTECTED,
        PUBLIC
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

    public boolean recursive;
    // Projection
    public Select select;
    // Restriction predicates
    public Access access = Access.PRIVATE;
    public String[] ids;
    public StringPredicate[] names;
    public ParentPredicate[] parents;
    public PathPredicate[] paths;
    public TenantPredicate tenant;
    public String[] userIds;
    public StringPredicate[] types;
    public LogicGroup<AttributePredicate> attributes;
    public MetaPredicate[] attributeMeta;
    // Ordering
    public OrderBy orderBy;
    public int limit;

    public AssetQuery() {
    }

    public AssetQuery recursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public AssetQuery access(Access access) {
        this.access = access;
        return this;
    }

    public AssetQuery select(Select select) {
        if (select == null) {
            select = new Select();
        }

        this.select = select;
        return this;
    }

    public AssetQuery ids(String... ids) {
        this.ids = ids;
        return this;
    }

    public AssetQuery names(String... names) {
        if (names == null || names.length == 0) {
            this.names = null;
            return this;
        }
        this.names = Arrays.stream(names).map(StringPredicate::new).toArray(StringPredicate[]::new);
        return this;
    }

    public AssetQuery names(StringPredicate... names) {
        this.names = names;
        return this;
    }

    public AssetQuery parents(String... ids) {
        if (ids == null || ids.length == 0) {
            this.names = null;
            return this;
        }
        this.parents = Arrays.stream(ids).map(ParentPredicate::new).toArray(ParentPredicate[]::new);
        return this;
    }

    public AssetQuery parents(AssetType... assetTypes) {
        if (assetTypes == null || assetTypes.length == 0) {
            this.names = null;
            return this;
        }
        this.parents = Arrays.stream(assetTypes).map(assetType -> new ParentPredicate().type(assetType)).toArray(ParentPredicate[]::new);
        return this;
    }

    public AssetQuery parents(ParentPredicate... parentPredicates) {
        this.parents = parentPredicates;
        return this;
    }

    public AssetQuery paths(PathPredicate... pathPredicates) {
        this.paths = pathPredicates;
        return this;
    }

    public AssetQuery tenant(TenantPredicate tenantPredicate) {
        this.tenant = tenantPredicate;
        return this;
    }

    public AssetQuery userIds(String... userIds) {
        this.userIds = userIds;
        return this;
    }

    public AssetQuery types(StringPredicate... typePredicates) {
        this.types = typePredicates;
        return this;
    }

    public AssetQuery types(String... types) {
        if (types == null || types.length == 0) {
            this.types = null;
            return this;
        }

        this.types = Arrays.stream(types).map(StringPredicate::new).toArray(StringPredicate[]::new);
        return this;
    }

    public AssetQuery types(AssetDescriptor... types) {
        if (types == null || types.length == 0) {
            this.types = null;
            return this;
        }

        this.types = Arrays.stream(types).map(at -> new StringPredicate(at.getType())).toArray(StringPredicate[]::new);
        return this;
    }

    public AssetQuery attributes(AttributePredicate... attributePredicates) {
        if (attributePredicates == null || attributePredicates.length == 0) {
            this.attributes = null;
            return this;
        }

        this.attributes = new LogicGroup<>(Arrays.asList(attributePredicates));
        return this;
    }

    public AssetQuery attributes(LogicGroup<AttributePredicate> attributePredicateGroup) {
        this.attributes = attributePredicateGroup;
        return this;
    }

    public AssetQuery attributeNames(String... attributeNames) {
        LogicGroup<AttributePredicate> predicateLogicGroup = new LogicGroup<>(Arrays.stream(attributeNames).map(AttributePredicate::new).collect(Collectors.toList()));
        predicateLogicGroup.operator = LogicGroup.Operator.OR;
        return attributes(predicateLogicGroup);
    }

    public AssetQuery attributeName(String attributeName) {
        return attributes(new AttributePredicate(attributeName));
    }

    public AssetQuery attributeValue(String name, ValuePredicate valuePredicate) {
        return attributes(new AttributePredicate(new StringPredicate(name), valuePredicate));
    }

    public AssetQuery attributeValue(String name) {
        return attributeValue(name, new ValueNotEmptyPredicate());
    }

    public AssetQuery attributeValue(String name, boolean b) {
        return attributeValue(name, new BooleanPredicate(b));
    }

    public AssetQuery attributeValue(String name, String s) {
        return attributeValue(name, new StringPredicate(s));
    }

    public AssetQuery attributeValue(String name, Match match, String s) {
        return attributeValue(name, new StringPredicate(match, s));
    }

    public AssetQuery attributeValue(String name, Match match, boolean caseSensitive, String s) {
        return attributeValue(name, new StringPredicate(match, caseSensitive, s));
    }

    public AssetQuery attributeValue(String name, double d) {
        return attributeValue(name, new NumberPredicate(d));
    }

    public AssetQuery attributeValue(String name, Operator operator, double d) {
        return attributeValue(name, new NumberPredicate(d, operator));
    }

    public AssetQuery attributeMeta(MetaPredicate... attributeMetaPredicates) {
        this.attributeMeta = attributeMetaPredicates;
        return this;
    }

    public AssetQuery orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "select=" + select +
                ", ids=" + (ids != null ? Arrays.toString(ids) : "null") +
                ", name=" + names +
                ", parent=" + parents +
                ", path=" + paths +
                ", tenant=" + tenant +
                ", userId='" + userIds + '\'' +
                ", type=" + types +
                ", attribute=" + (attributes != null ? attributes.toString() : "null") +
                ", attributeMeta=" + Arrays.toString(attributeMeta) +
                ", orderBy=" + orderBy +
                ", recursive=" + recursive +
                '}';
    }
}
