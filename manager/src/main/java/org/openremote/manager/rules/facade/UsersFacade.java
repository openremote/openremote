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
package org.openremote.manager.rules.facade;

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.rules.RulesEngineId;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.Asset;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.rules.*;
import org.openremote.model.user.UserQuery;

import java.util.List;
import java.util.function.Consumer;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not access users in global scope).
 */
public class UsersFacade<T extends Ruleset> extends Users {

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetStorageService assetStorageService;
    protected final NotificationService notificationService;
    protected final ManagerIdentityService identityService;

    public UsersFacade(RulesEngineId<T> rulesEngineId, AssetStorageService assetStorageService, NotificationService notificationService, ManagerIdentityService identityService) {
        this.rulesEngineId = rulesEngineId;
        this.assetStorageService = assetStorageService;
        this.notificationService = notificationService;
        this.identityService = identityService;
    }

    @Override
    public Users.RestrictedQuery query() {
        Users.RestrictedQuery query = new Users.RestrictedQuery() {
            @Override
            public Users.RestrictedQuery tenant(UserQuery.TenantPredicate tenantPredicate) {
                if (GlobalRuleset.class.isAssignableFrom(rulesEngineId.getScope()))
                    return super.tenant(tenantPredicate);
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public Users.RestrictedQuery asset(UserQuery.AssetPredicate assetPredicate) {
                if (GlobalRuleset.class.isAssignableFrom(rulesEngineId.getScope()))
                    return super.asset(assetPredicate);
                if (TenantRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                    return super.asset(assetPredicate);
                    // TODO: should only be allowed if asset belongs to tenant
                }
                if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                    return super.asset(assetPredicate);
                    // TODO: should only be allowed if restricted asset is descendant of scope's asset
                }
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public List<String> getResults() {
                return notificationService.findAllUsersWithToken(this);
            }

            @Override
            public void applyResults(Consumer<List<String>> usersIdListConsumer) {
                usersIdListConsumer.accept(getResults());
            }
        };

        if (TenantRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            query.tenantPredicate = new UserQuery.TenantPredicate(
                rulesEngineId.getRealmId().orElseThrow(() -> new IllegalArgumentException("Realm ID missing: " + rulesEngineId))
            );
        }
        if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            String assetId = rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ID missing: " + rulesEngineId));
            Asset restrictedAsset = assetStorageService.find(assetId, true);
            if (restrictedAsset == null) {
                // An asset was deleted in the database, but its ruleset is still deployed. This
                // can happen while the PersistenceEvent of the deletion is not processed and the
                // rules engine fires. This exception marks the ruleset deployment as execution
                // error status, and as soon as the PersistenceEvent is processed, the ruleset
                // deployment is removed and the problem resolved.
                throw new IllegalStateException("Asset is no longer available: " + rulesEngineId);
            }
            query.assetPredicate = new UserQuery.AssetPredicate(assetId);
        }
        return query;
    }

    @Override
    public void storeAndNotify(String userId, AlertNotification alert) {
        if (TenantRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            boolean userIsInTenant = identityService.getIdentityProvider().isUserInTenant(
                userId,
                rulesEngineId.getRealmId().orElseThrow(() -> new IllegalArgumentException("Realm ID missing: " + rulesEngineId))
            );
            if (!userIsInTenant) {
                throw new IllegalArgumentException("User not in tenant " + rulesEngineId + ": " + userId);
            }
        }
        if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
            boolean userIsLinkedToAsset = assetStorageService.isUserAsset(
                userId,
                rulesEngineId.getAssetId().orElseThrow(() -> new IllegalStateException("Asset ID missing: " + rulesEngineId))
            );
            if (!userIsLinkedToAsset) {
                throw new IllegalArgumentException("User not linked to asset " + rulesEngineId + ": " + userId);
            }
        }
        notificationService.storeAndNotify(userId, alert);
    }

}
