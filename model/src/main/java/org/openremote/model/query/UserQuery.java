package org.openremote.model.query;


import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;

public class UserQuery {

    // Restriction predicates
    public TenantPredicate tenantPredicate;
    public UserAssetPredicate assetPredicate;
    public PathPredicate pathPredicate;
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
            ", limit=" + limit +
            '}';
    }
}
