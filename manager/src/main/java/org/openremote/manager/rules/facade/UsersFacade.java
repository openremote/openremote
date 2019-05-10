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
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.query.filter.UserAssetPredicate;
import org.openremote.model.rules.*;
import org.openremote.model.security.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Restricts rule RHS access to the scope of the engine (a rule in asset scope can not access users in global scope).
 */
public class UsersFacade<T extends Ruleset> extends Users {

    protected final RulesEngineId<T> rulesEngineId;
    protected final AssetStorageService assetStorageService;
    protected final NotificationService notificationService;
    protected final ManagerIdentityService identityService;

    public class UsersResctrictedQueryFacade extends Users.RestrictedQuery {

        @Override
        public Users.RestrictedQuery tenant(TenantPredicate tenantPredicate) {
            if (GlobalRuleset.class.isAssignableFrom(rulesEngineId.getScope()))
                return super.tenant(tenantPredicate);
            throw new IllegalArgumentException("Overriding query restriction is not allowed in this rules scope");
        }

        @Override
        public List<String> getResults() {
            return stream().collect(Collectors.toList());
        }

        @Override
        public Stream<String> stream() {
            // Do security checks to ensure correct scoping
            // No restriction for global rulesets
            if (TenantRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                // Restrict tenant
                tenantPredicate = new TenantPredicate(
                    rulesEngineId.getRealm().orElseThrow(() -> new IllegalArgumentException("Realm ID missing: " + rulesEngineId))
                );
            } else if (AssetRuleset.class.isAssignableFrom(rulesEngineId.getScope())) {
                tenantPredicate = null;
                String assetId = rulesEngineId.getAssetId().orElseThrow(() -> new IllegalArgumentException("Asset ID missing: " + rulesEngineId));

                // Asset must be this engines asset or a child
                if (assetPredicate != null) {
                    if (assetId.equals(assetPredicate.id)) {
                        pathPredicate = null;
                    } else {
                        pathPredicate = new PathPredicate(rulesEngineId.getAssetId().orElseThrow(IllegalArgumentException::new));
                    }
                } else if (pathPredicate != null) {
                    // Path must contain this engines asset ID
                    List<String> path = new ArrayList<>(Arrays.asList(pathPredicate.path));
                    if (!path.contains(assetId)) {
                        path.add(assetId);
                        pathPredicate.path = path.toArray(new String[pathPredicate.path.length + 1]);
                    }
                } else {
                    // Force scope to this asset
                    assetPredicate = new UserAssetPredicate(assetId);
                }
            }

            return Arrays.stream(identityService.getIdentityProvider().getUsers(this))
                .map(User::getId);
        }
    }

    public UsersFacade(RulesEngineId<T> rulesEngineId, AssetStorageService assetStorageService, NotificationService notificationService, ManagerIdentityService identityService) {
        this.rulesEngineId = rulesEngineId;
        this.assetStorageService = assetStorageService;
        this.notificationService = notificationService;
        this.identityService = identityService;
    }

    @Override
    public Users.RestrictedQuery query() {
        return new UsersResctrictedQueryFacade();
    }
}
