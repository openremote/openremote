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
package org.openremote.manager.server.rules.facade;

import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.notification.NotificationService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.notification.AlertNotification;
import org.openremote.model.rules.*;
import org.openremote.model.user.UserQuery;

import java.util.List;
import java.util.function.Consumer;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not access users in global scope).
 */
public class UsersFacade<T extends Ruleset> extends Users {

    protected final Class<T> rulesetType;
    protected final String rulesetId;
    protected final AssetStorageService assetStorageService;
    protected final NotificationService notificationService;
    protected final ManagerIdentityService identityService;

    public UsersFacade(Class<T> rulesetType, String rulesetId, AssetStorageService assetStorageService, NotificationService notificationService, ManagerIdentityService identityService) {
        this.rulesetType = rulesetType;
        this.rulesetId = rulesetId;
        this.assetStorageService = assetStorageService;
        this.notificationService = notificationService;
        this.identityService = identityService;
    }

    @Override
    public Users.RestrictedQuery query() {
        Users.RestrictedQuery query = new Users.RestrictedQuery() {
            @Override
            public Users.RestrictedQuery tenant(UserQuery.TenantPredicate tenantPredicate) {
                if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                    return super.tenant(tenantPredicate);
                throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
            }

            @Override
            public Users.RestrictedQuery asset(UserQuery.AssetPredicate assetPredicate) {
                if (GlobalRuleset.class.isAssignableFrom(rulesetType))
                    return super.asset(assetPredicate);
                if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
                    return super.asset(assetPredicate);
                    // TODO: should only be allowed if asset belongs to tenant
                }
                if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
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

        if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
            query.tenantPredicate = new UserQuery.TenantPredicate(rulesetId);
        }
        if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
            ServerAsset restrictedAsset = assetStorageService.find(rulesetId, true);
            if (restrictedAsset == null) {
                throw new IllegalStateException("Asset is no longer available for this deployment: " + rulesetId);
            }
            query.assetPredicate = new UserQuery.AssetPredicate(rulesetId);
        }
        return query;

    }

    @Override
    public void storeAndNotify(String userId, AlertNotification alert) {
        if (TenantRuleset.class.isAssignableFrom(rulesetType)) {
            boolean userIsInTenant = identityService.getIdentityProvider().isUserInTenant(userId, rulesetId);
            if (!userIsInTenant) {
                throw new IllegalArgumentException("User not in tenant: " + rulesetId);
            }
        }
        if (AssetRuleset.class.isAssignableFrom(rulesetType)) {
            boolean userIsLinkedToAsset = assetStorageService.isUserAsset(userId, rulesetId);
            if (!userIsLinkedToAsset) {
                throw new IllegalArgumentException("User not linked to asset: " + rulesetId);
            }
        }
        notificationService.storeAndNotify(userId, alert);
    }

}
