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
package org.openremote.model.asset;

import java.util.Locale;

/**
 * Encapsulate asset query details. A query returns a collection of
 * {@link Asset} instances, unless if the {@link #id} is set, then only
 * a single instance can be found.
 */
public class AssetQuery {

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

    public interface Search {

    }

    static public class StringSearch  {
        public Match match = Match.EXACT;
        public boolean caseSensitive = true;
        public String value;

        public StringSearch() {
        }

        public StringSearch(String value) {
            this.value = value;
        }

        public StringSearch(Match match, String value) {
            this.match = match;
            this.value = value;
        }

        public StringSearch(Match match, boolean caseSensitive, String value) {
            this.match = match;
            this.caseSensitive = caseSensitive;
            this.value = value;
        }

        public StringSearch match(Match match) {
            this.match = match;
            return this;
        }

        public StringSearch caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public StringSearch search(String search) {
            this.value = search;
            return this;
        }

        public String prepareValue() {
            String s = match.prepare(this.value);
            if (!caseSensitive)
                s = s.toUpperCase(Locale.ROOT);
            return s;
        }
    }

    static public class BooleanSearch implements Search {

        public boolean predicate;

        public BooleanSearch() {
        }

        public BooleanSearch(boolean predicate) {
            this.predicate = predicate;
        }

        public BooleanSearch predicate(boolean predicate) {
            this.predicate = predicate;
            return this;
        }
    }

    static public class Parent {
        public String id;
        public String type;
        public boolean noParent;

        public Parent() {
        }

        public Parent(String id) {
            this.id = id;
        }

        public Parent(boolean noParent) {
            this.noParent = noParent;
        }

        public Parent id(String id) {
            this.id = id;
            return this;
        }

        public Parent type(String type) {
            this.type = type;
            return this;
        }

        public Parent noParent(boolean noParent) {
            this.noParent = noParent;
            return this;
        }
    }

    static public class Realm {
        public String id;
        public String name;

        public Realm() {
        }

        public Realm(String id) {
            this.id = id;
        }

        public Realm id(String id) {
            this.id = id;
            return this;
        }

        public Realm name(String name) {
            this.name = name;
            return this;
        }
    }

    static public class AttributeMeta {
        public StringSearch itemNameSearch;
        public Search itemValueSearch;

        public AttributeMeta() {
        }

        public AttributeMeta(StringSearch itemNameSearch) {
            this.itemNameSearch = itemNameSearch;
        }

        public AttributeMeta(AssetMeta assetMeta) {
            this.itemNameSearch = new StringSearch(assetMeta.getName());
        }

        public AttributeMeta(Search itemValueSearch) {
            this.itemValueSearch = itemValueSearch;
        }

        public AttributeMeta(StringSearch itemNameSearch, Search itemValueSearch) {
            this.itemNameSearch = itemNameSearch;
            this.itemValueSearch = itemValueSearch;
        }

        public AttributeMeta(AssetMeta assetMeta, Search itemValueSearch) {
            this(new StringSearch(assetMeta.getName()), itemValueSearch);
        }

        public AttributeMeta itemNameSearch(StringSearch itemNameSearch) {
            this.itemNameSearch = itemNameSearch;
            return this;
        }

        public AttributeMeta itemNameSearch(AssetMeta assetMeta) {
            return itemNameSearch(new StringSearch(assetMeta.getName()));
        }

        public AttributeMeta itemValueSearch(Search itemValueSearch) {
            this.itemValueSearch = itemValueSearch;
            return this;
        }
    }

    static public class Select {
        public boolean loadComplete;
        public boolean filterProtected;

        public Select() {
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

    static public class OrderBy {
        public String property;
        public boolean descending;

        public OrderBy() {
        }

        public OrderBy(String property) {
            this.property = property;
        }

        public OrderBy(String property, boolean descending) {
            this.property = property;
            this.descending = descending;
        }

        public OrderBy property(String property) {
            this.property = property;
            return this;
        }

        public OrderBy descending(boolean descending) {
            this.descending = descending;
            return this;
        }
    }

    public Select select = new Select(false, false);

    /**
     * If this value is set, no other restrictions will be applied and only a single result should be returned.
     */
    public String id;

    public StringSearch name;
    public Parent parent;
    public Realm realm;
    public String userId;
    public StringSearch type;
    public AttributeMeta attributeMeta;
    public OrderBy orderBy = new OrderBy("createdOn");

    public AssetQuery() {
    }

    public AssetQuery select(Select select) {
        this.select = select;
        return this;
    }

    public AssetQuery name(StringSearch name) {
        this.name = name;
        return this;
    }

    public AssetQuery id(String id) {
        this.id = id;
        return this;
    }

    public AssetQuery parent(Parent parent) {
        this.parent = parent;
        return this;
    }

    public AssetQuery realm(Realm realm) {
        this.realm = realm;
        return this;
    }

    public AssetQuery userId(String userId) {
        this.userId = userId;
        return this;
    }

    public AssetQuery type(StringSearch type) {
        this.type = type;
        return this;
    }

    public AssetQuery type(AssetType assetType) {
        return type(new StringSearch(assetType.getValue()));
    }

    public AssetQuery attributeMeta(AttributeMeta attributeMeta) {
        this.attributeMeta = attributeMeta;
        return this;
    }

    public AssetQuery orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public boolean hasAttributeRestrictions() {
        return attributeMeta != null;
    }
}
