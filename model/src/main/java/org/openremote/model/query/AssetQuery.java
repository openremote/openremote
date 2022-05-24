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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.query.filter.*;
import org.openremote.model.util.ValueUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Encapsulate asset query restriction, projection, and ordering of results.
 */
// TODO: Add AssetQuery support for arbitrary attribute property path and value (e.g. consoleProviders.geofence.version == "ORConsole")
public class AssetQuery implements Serializable {

    public static class Select {
        protected static final String[] EMPTY_ATTRIBUTES = new String[0];
        public String[] attributes;

        public Select attributes(String... attributeNames) {
            this.attributes = attributeNames;
            return this;
        }

        public Select excludeAttributes() {
            this.attributes = EMPTY_ATTRIBUTES;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
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
        BETWEEN;

        public <T> boolean compare(Comparator<T> comparator, T x, T y) {
            return compare(comparator, x, y, null);
        }

        public <T> boolean compare(Comparator<T> comparator, T x, T y, T z) {
            // Get null friendly comparator
            comparator = Comparator.nullsFirst(comparator);
            int comparatorResult = comparator.compare(x, y);

            switch (this) {
                case EQUALS:
                    return comparatorResult == 0;
                case GREATER_THAN:
                    return comparatorResult > 0;
                case GREATER_EQUALS:
                    return comparatorResult >= 0;
                case LESS_THAN:
                    return comparatorResult < 0;
                case LESS_EQUALS:
                    return comparatorResult <= 0;
                case BETWEEN:
                    int comparatorResultUpper = comparator.compare(x, z);
                    return comparatorResult >= 0 && comparatorResultUpper < 1;
            }
            return false;
        }
    }

    public boolean recursive;
    // Projection
    public Select select;
    // Restriction predicates
    public Access access;
    public String[] ids;
    public StringPredicate[] names;
    public ParentPredicate[] parents;
    public PathPredicate[] paths;
    public RealmPredicate realm;
    public String[] userIds;
    @JsonSerialize(contentConverter = AssetClassToStringConverter.class)
    @JsonDeserialize(contentConverter = StringToAssetClassConverter.class)
    public Class<? extends Asset<?>>[] types;
    public LogicGroup<AttributePredicate> attributes;
    // Ordering
    public OrderBy orderBy;
    public int limit;

    public static class AssetClassToStringConverter extends StdConverter<Class<? extends Asset<?>>, String> {

        @Override
        public String convert(Class<? extends Asset<?>> value) {
            return value.getSimpleName();
        }
    }

    public static class StringToAssetClassConverter extends StdConverter<String, Class<? extends Asset<?>>> {

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Asset<?>> convert(String value) {
            if (Agent.class.getSimpleName().equals(value)) {
                return (Class<? extends Asset<?>>)(Class<?>) Agent.class;
            }
            if (Asset.class.getSimpleName().equals(value)) {
                return (Class<? extends Asset<?>>)(Class<?>) Asset.class;
            }

            return ValueUtil.getAssetDescriptor(value).map(AssetDescriptor::getType).orElse(null);
        }
    }

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

    public AssetQuery parents(ParentPredicate... parentPredicates) {
        this.parents = parentPredicates;
        return this;
    }

    public AssetQuery paths(PathPredicate... pathPredicates) {
        this.paths = pathPredicates;
        return this;
    }

    public AssetQuery realm(RealmPredicate realmPredicate) {
        this.realm = realmPredicate;
        return this;
    }

    public AssetQuery userIds(String... userIds) {
        this.userIds = userIds;
        return this;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final AssetQuery types(AssetDescriptor<? extends Asset<?>>... types) {
        if (types == null || types.length == 0) {
            this.types = null;
            return this;
        }

        this.types = Arrays.stream(types).map(AssetDescriptor::getType)
            .toArray(size -> (Class<? extends Asset<?>>[])new Class<?>[size]);

        return this;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Asset<?>> AssetQuery types(Class<T> type) {
        this.types = new Class[] {type};
        return this;
    }

    @SafeVarargs
    public final AssetQuery types(Class<? extends Asset<?>>... types) {
        if (types == null || types.length == 0) {
            this.types = null;
            return this;
        }

        this.types = types;
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
        LogicGroup<AttributePredicate> predicateLogicGroup = new LogicGroup<>(Arrays.stream(attributeNames).map(name -> new AttributePredicate(name, null)).collect(Collectors.toList()));
        predicateLogicGroup.operator = LogicGroup.Operator.OR;
        return attributes(predicateLogicGroup);
    }

    public AssetQuery attributeName(String attributeName) {
        return attributes(new AttributePredicate(attributeName, null));
    }

    public AssetQuery attributeValue(String name, ValuePredicate valuePredicate) {
        return attributes(new AttributePredicate(new StringPredicate(name), valuePredicate));
    }

    public AssetQuery attributeValue(String name) {
        return attributeValue(name, new ValueEmptyPredicate().negate(true));
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

    public AssetQuery orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "select=" + select +
                ", ids=" + (ids != null ? Arrays.toString(ids) : "null") +
                ", name=" + Arrays.toString(names) +
                ", parent=" + Arrays.toString(parents) +
                ", path=" + Arrays.toString(paths) +
                ", realm=" + realm +
                ", userId='" + Arrays.toString(userIds) + '\'' +
                ", type=" + Arrays.toString(types) +
                ", attribute=" + (attributes != null ? attributes.toString() : "null") +
                ", orderBy=" + orderBy +
                ", recursive=" + recursive +
                '}';
    }
}
