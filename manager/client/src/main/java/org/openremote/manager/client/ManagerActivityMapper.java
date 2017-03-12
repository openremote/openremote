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
package org.openremote.manager.client;

import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.admin.overview.AdminOverviewActivity;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantActivity;
import org.openremote.manager.client.admin.tenant.AdminTenantsActivity;
import org.openremote.manager.client.admin.tenant.AdminTenantPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantsPlace;
import org.openremote.manager.client.admin.users.AdminUserActivity;
import org.openremote.manager.client.admin.users.AdminUserPlace;
import org.openremote.manager.client.admin.users.AdminUsersActivity;
import org.openremote.manager.client.admin.users.AdminUsersPlace;
import org.openremote.manager.client.assets.asset.AssetActivity;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.AssetsDashboardActivity;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.apps.AppsActivity;
import org.openremote.manager.client.apps.AppsPlace;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.map.MapActivity;
import org.openremote.manager.client.map.MapPlace;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.mvp.AppActivityMapper;
import org.openremote.manager.client.mvp.RoleRequiredException;
import org.openremote.manager.client.rules.RulesGlobalActivity;
import org.openremote.manager.client.rules.RulesGlobalPlace;
import org.openremote.manager.client.rules.asset.RulesAssetActivity;
import org.openremote.manager.client.rules.asset.RulesAssetPlace;
import org.openremote.manager.client.rules.tenant.RulesTenantActivity;
import org.openremote.manager.client.rules.tenant.RulesTenantPlace;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.user.UserAccountActivity;
import org.openremote.manager.client.user.UserAccountPlace;
import org.openremote.manager.client.event.ShowFailureEvent;

import java.util.logging.Logger;

public class ManagerActivityMapper implements AppActivityMapper {

    private static final Logger LOG = Logger.getLogger(ManagerActivityMapper.class.getName());

    protected final SecurityService securityService;
    protected final EventBus eventBus;
    protected final ManagerMessages managerMessages;
    protected final Provider<AssetsDashboardActivity> assetsDashboardActivityProvider;
    protected final Provider<AssetActivity> assetActivityProvider;
    protected final Provider<MapActivity> mapActivityProvider;
    protected final Provider<RulesGlobalActivity> rulesGlobalActivityProvider;
    protected final Provider<RulesTenantActivity> rulesTenantActivityProvider;
    protected final Provider<RulesAssetActivity> rulesAssetActivityProvider;
    protected final Provider<AppsActivity> appsActivityProvider;
    protected final Provider<AdminOverviewActivity> adminOverviewActivityProvider;
    protected final Provider<AdminTenantsActivity> adminTenantsActivityProvider;
    protected final Provider<AdminTenantActivity> adminTenantActivityProvider;
    protected final Provider<AdminUsersActivity> adminUsersActivityProvider;
    protected final Provider<AdminUserActivity> adminUserActivityProvider;
    protected final Provider<UserAccountActivity> userProfileActivityProvider;

    @Inject
    public ManagerActivityMapper(SecurityService securityService,
                                 EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 Provider<AssetsDashboardActivity> assetsDashboardActivityProvider,
                                 Provider<AssetActivity> assetActivityProvider,
                                 Provider<MapActivity> mapActivityProvider,
                                 Provider<RulesGlobalActivity> rulesGlobalActivityProvider,
                                 Provider<RulesTenantActivity> rulesTenantActivityProvider,
                                 Provider<RulesAssetActivity> rulesAssetActivityProvider,
                                 Provider<AppsActivity> appsActivityProvider,
                                 Provider<AdminOverviewActivity> adminOverviewActivityProvider,
                                 Provider<AdminTenantsActivity> adminTenantsActivityProvider,
                                 Provider<AdminTenantActivity> adminTenantActivityProvider,
                                 Provider<AdminUsersActivity> adminUsersActivityProvider,
                                 Provider<AdminUserActivity> adminUserActivityProvider,
                                 Provider<UserAccountActivity> userProfileActivityProvider) {
        this.securityService = securityService;
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.assetsDashboardActivityProvider = assetsDashboardActivityProvider;
        this.assetActivityProvider = assetActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.rulesGlobalActivityProvider = rulesGlobalActivityProvider;
        this.rulesTenantActivityProvider = rulesTenantActivityProvider;
        this.rulesAssetActivityProvider = rulesAssetActivityProvider;
        this.appsActivityProvider = appsActivityProvider;
        this.adminOverviewActivityProvider = adminOverviewActivityProvider;
        this.adminTenantsActivityProvider = adminTenantsActivityProvider;
        this.adminTenantActivityProvider = adminTenantActivityProvider;
        this.adminUsersActivityProvider = adminUsersActivityProvider;
        this.adminUserActivityProvider = adminUserActivityProvider;
        this.userProfileActivityProvider = userProfileActivityProvider;
    }

    public AppActivity getActivity(Place place) {
        try {
            if (place instanceof AssetsDashboardPlace) {
                return assetsDashboardActivityProvider.get().init(securityService, (AssetsDashboardPlace) place);
            }
            if (place instanceof AssetPlace) {
                return assetActivityProvider.get().init(securityService, (AssetPlace) place);
            }
            if (place instanceof MapPlace) {
                return mapActivityProvider.get().init(securityService, (MapPlace) place);
            }
            if (place instanceof RulesGlobalPlace) {
                return rulesGlobalActivityProvider.get().init(securityService, (RulesGlobalPlace) place);
            }
            if (place instanceof RulesTenantPlace) {
                return rulesTenantActivityProvider.get().init(securityService, (RulesTenantPlace) place);
            }
            if (place instanceof RulesAssetPlace) {
                return rulesAssetActivityProvider.get().init(securityService, (RulesAssetPlace) place);
            }
            if (place instanceof AppsPlace) {
                return appsActivityProvider.get().init(securityService, (AppsPlace) place);
            }
            if (place instanceof AdminOverviewPlace) {
                return adminOverviewActivityProvider.get().init(securityService, (AdminOverviewPlace) place);
            }
            if (place instanceof AdminTenantsPlace) {
                return adminTenantsActivityProvider.get().init(securityService, (AdminTenantsPlace) place);
            }
            if (place instanceof AdminTenantPlace) {
                return adminTenantActivityProvider.get().init(securityService, (AdminTenantPlace) place);
            }
            if (place instanceof AdminUsersPlace) {
                return adminUsersActivityProvider.get().init(securityService, (AdminUsersPlace) place);
            }
            if (place instanceof AdminUserPlace) {
                return adminUserActivityProvider.get().init(securityService, (AdminUserPlace) place);
            }
            if (place instanceof UserAccountPlace) {
                return userProfileActivityProvider.get().init(securityService, (UserAccountPlace) place);
            }

            LOG.severe("No activity available for place: " + place);

        } catch (RoleRequiredException ex) {
            LOG.warning("Access denied, missing required role '" + ex.getRequiredRole() + "': " + place);
            eventBus.dispatch(new ShowFailureEvent(managerMessages.accessDenied(), 5000));
        }
        return null;
    }
}
