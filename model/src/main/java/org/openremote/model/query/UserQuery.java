package org.openremote.model.query;


import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;

public class UserQuery<CHILD extends UserQuery<CHILD>> {

    // Restriction predicates
    public TenantPredicate tenantPredicate;
    public UserAssetPredicate assetPredicate;
    public PathPredicate pathPredicate;

    public UserQuery() {
    }

    @SuppressWarnings("unchecked")
    public CHILD tenant(TenantPredicate tenantPredicate) {
        this.tenantPredicate = tenantPredicate;
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    public CHILD asset(UserAssetPredicate assetPredicate) {
        this.assetPredicate = assetPredicate;
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    public CHILD assetPath(PathPredicate pathPredicate) {
        this.pathPredicate = pathPredicate;
        return (CHILD) this;
    }
}
