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

import org.openremote.model.AttributeRef;

import java.util.Locale;

/**
 * Encapsulate asset query restriction, project, and ordering of results.
 * <p>
 * A query returns a collection of {@link Asset} instances, or when the
 * {@link #id} predicate is set, a single instance.
 */
public class AssetQuery {

    static public class Select {
        public boolean loadComplete;
        // TODO: Filtering attributes can be moved into database functions
        public boolean filterProtected;

        public Select() {
        }

        public Select(boolean loadComplete) {
            this.loadComplete = loadComplete;
        }

        public Select(boolean loadComplete, boolean filterProtected) {
            this.loadComplete = loadComplete;
            this.filterProtected = filterProtected;
        }

        public Select loadComplete(boolean loadComplete) {
            this.loadComplete = loadComplete;
            return this;
        }

        public Select filterProtected(boolean filterProtected) {
            this.filterProtected = filterProtected;
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

    public interface ValuePredicate {
    }

    static public class StringPredicate implements ValuePredicate {
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

    static public class BooleanPredicate implements ValuePredicate {
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

    static public class StringArrayPredicate implements ValuePredicate {
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

    static public class ParentPredicate {
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

    static public class PathPredicate {
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

    static public class TenantPredicate {
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

    static public class AttributeMetaPredicate {
        public StringPredicate itemNamePredicate;
        public ValuePredicate itemValuePredicate;

        public AttributeMetaPredicate() {
        }

        public AttributeMetaPredicate(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
        }

        public AttributeMetaPredicate(AssetMeta assetMeta) {
            this.itemNamePredicate = new StringPredicate(assetMeta.getName());
        }

        public AttributeMetaPredicate(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            this.itemValuePredicate = itemValuePredicate;
        }

        public AttributeMetaPredicate(AssetMeta assetMeta, ValuePredicate itemValuePredicate) {
            this(new StringPredicate(assetMeta.getName()), itemValuePredicate);
        }

        public AttributeMetaPredicate itemName(StringPredicate itemNamePredicate) {
            this.itemNamePredicate = itemNamePredicate;
            return this;
        }

        public AttributeMetaPredicate itemName(AssetMeta assetMeta) {
            return itemName(new StringPredicate(assetMeta.getName()));
        }

        public AttributeMetaPredicate itemValue(ValuePredicate itemValuePredicate) {
            this.itemValuePredicate = itemValuePredicate;
            return this;
        }
    }

    static public class AttributeRefPredicate extends AttributeMetaPredicate {

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
            this(assetMeta.getName(), entityId, attributeName);
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
            this(new StringPredicate(assetMeta.getName()), attributeRef);
        }
    }

    static public class OrderBy {

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
    public Select select = new Select(false, false);

    // Restriction predicates
    public String id;
    public StringPredicate name;
    public ParentPredicate parentPredicate;
    public PathPredicate pathPredicate;
    public TenantPredicate tenantPredicate;
    public String userId;
    public StringPredicate type;
    public AttributeMetaPredicate attributeMetaPredicate;

    // Ordering
    public OrderBy orderBy = new OrderBy(OrderBy.Property.CREATED_ON);

    public AssetQuery() {
    }

    public AssetQuery select(Select select) {
        this.select = select;
        return this;
    }

    /**
     * If this value is set, no other restrictions will be applied and only a single result should be returned.
     */
    public AssetQuery id(String id) {
        this.id = id;
        return this;
    }

    public AssetQuery name(StringPredicate name) {
        this.name = name;
        return this;
    }

    public AssetQuery parent(ParentPredicate parentPredicate) {
        this.parentPredicate = parentPredicate;
        return this;
    }

    public AssetQuery path(PathPredicate pathPredicate) {
        this.pathPredicate = pathPredicate;
        return this;
    }

    public AssetQuery tenant(TenantPredicate tenantPredicate) {
        this.tenantPredicate = tenantPredicate;
        return this;
    }

    public AssetQuery userId(String userId) {
        this.userId = userId;
        return this;
    }

    public AssetQuery type(StringPredicate type) {
        this.type = type;
        return this;
    }

    public AssetQuery type(AssetType assetType) {
        return type(new StringPredicate(assetType.getValue()));
    }

    public AssetQuery attributeMeta(AttributeMetaPredicate attributeMetaPredicate) {
        this.attributeMetaPredicate = attributeMetaPredicate;
        return this;
    }

    public AssetQuery orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }
}
