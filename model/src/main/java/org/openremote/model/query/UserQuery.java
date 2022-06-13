package org.openremote.model.query;

import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;

import java.util.Arrays;

public class UserQuery {

    public static class Select {
        public boolean basic;
        public boolean excludeServiceUsers;
        public boolean excludeRegularUsers;
        public boolean excludeSystemUsers;

        public Select excludeServiceUsers(boolean excludeServiceUsers) {
            this.excludeServiceUsers = excludeServiceUsers;
            return this;
        }

        public Select excludeRegularUsers(boolean excludeRegularUsers) {
            this.excludeRegularUsers = excludeRegularUsers;
            return this;
        }

        public Select excludeSystemUsers(boolean excludeSystemUsers) {
            this.excludeSystemUsers = excludeSystemUsers;
            return this;
        }

        public Select basic(boolean basic) {
            this.basic = basic;
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "excludeServiceUsers=" + excludeServiceUsers +
                ", excludeRegularUsers=" + excludeRegularUsers +
                ", excludeSystemUsers=" + excludeSystemUsers +
                ", basic=" + basic +
                '}';
        }
    }

    public static class OrderBy {

        public enum Property {
            CREATED_ON,
            FIRST_NAME,
            LAST_NAME,
            USERNAME,
            EMAIL
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

    // Restriction predicates
    public RealmPredicate realmPredicate;
    public UserAssetPredicate assetPredicate;
    public PathPredicate pathPredicate;
    public String[] ids;
    public Select select;
    public StringPredicate[] usernames;
    public Integer limit;
    public Integer offset;
    public OrderBy orderBy;

    public UserQuery() {
    }

    public UserQuery realm(RealmPredicate realmPredicate) {
        this.realmPredicate = realmPredicate;
        return this;
    }

    public UserQuery asset(UserAssetPredicate assetPredicate) {
        this.assetPredicate = assetPredicate;
        return this;
    }

    public UserQuery assetPath(PathPredicate pathPredicate) {
        this.pathPredicate = pathPredicate;
        return this;
    }

    public UserQuery ids(String...ids) {
        this.ids = ids;
        return this;
    }

    public UserQuery usernames(StringPredicate...usernames) {
        this.usernames = usernames;
        return this;
    }

    public UserQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    public UserQuery offset(int offset) {
        this.offset = offset;
        return this;
    }

    public UserQuery orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public UserQuery select(Select select) {
        this.select = select;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realmPredicate=" + realmPredicate +
            ", assetPredicate=" + assetPredicate +
            ", pathPredicate=" + pathPredicate +
            ", ids=" + (ids != null ? Arrays.toString(ids) : "null") +
            ", usernames=" + (usernames != null ? Arrays.toString(usernames) : "null") +
            ", limit=" + limit +
            ", offset=" + offset +
            ", orderBy=" + orderBy +
            ", select=" + select +
            '}';
    }
}
