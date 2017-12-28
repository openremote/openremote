package org.openremote.model.user;


public class UserQuery<CHILD extends UserQuery<CHILD>> {

    public static class TenantPredicate {
        public String realmId;
        public String realm;

        public TenantPredicate() {
        }

        public TenantPredicate(String realmId) {
            this.realmId = realmId;
        }

        public UserQuery.TenantPredicate realmId(String id) {
            this.realmId = id;
            return this;
        }

        public UserQuery.TenantPredicate realm(String name) {
            this.realm = name;
            return this;
        }
    }

    public static class AssetPredicate {
        public String id;

        public AssetPredicate() {
        }

        public AssetPredicate(String id) {
            this.id = id;
        }

        public UserQuery.AssetPredicate id(String id) {
            this.id = id;
            return this;
        }
    }

    // Restriction predicates
    public UserQuery.TenantPredicate tenantPredicate;
    public UserQuery.AssetPredicate assetPredicate;

    public UserQuery() {
    }

    @SuppressWarnings("unchecked")
    public CHILD tenant(UserQuery.TenantPredicate tenantPredicate) {
        this.tenantPredicate = tenantPredicate;
        return (CHILD) this;
    }

    @SuppressWarnings("unchecked")
    public CHILD asset(UserQuery.AssetPredicate assetPredicate) {
        this.assetPredicate = assetPredicate;
        return (CHILD) this;
    }

}
