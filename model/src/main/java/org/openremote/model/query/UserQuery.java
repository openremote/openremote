package org.openremote.model.query;


import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;

import java.util.Arrays;

public class UserQuery {

    // Restriction predicates
    public TenantPredicate tenantPredicate;
    public UserAssetPredicate assetPredicate;
    public PathPredicate pathPredicate;
    public String[] ids;
    public String[] usernames;
    public int limit;

    public UserQuery() {
    }

    public UserQuery tenant(TenantPredicate tenantPredicate) {
        this.tenantPredicate = tenantPredicate;
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

    public UserQuery usernames(String...usernames) {
        this.usernames = usernames;
        return this;
    }

    public UserQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "tenantPredicate=" + tenantPredicate +
            ", assetPredicate=" + assetPredicate +
            ", pathPredicate=" + pathPredicate +
            ", ids=" + (ids != null ? Arrays.toString(ids) : "null") +
            ", usernames=" + (usernames != null ? Arrays.toString(usernames) : "null") +
            ", limit=" + limit +
            '}';
    }
}
