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
package org.openremote.manager.client;

import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.admin.overview.AdminOverviewActivity;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.realms.AdminRealmActivity;
import org.openremote.manager.client.admin.realms.AdminRealmsActivity;
import org.openremote.manager.client.admin.realms.AdminRealmPlace;
import org.openremote.manager.client.admin.realms.AdminRealmsPlace;
import org.openremote.manager.client.admin.users.AdminUsersActivity;
import org.openremote.manager.client.admin.users.AdminUsersPlace;
import org.openremote.manager.client.assets.AssetDetailActivity;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.flows.FlowsActivity;
import org.openremote.manager.client.flows.FlowsPlace;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.map.MapActivity;
import org.openremote.manager.client.map.MapPlace;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.mvp.AppActivityMapper;
import org.openremote.manager.client.mvp.RoleRequiredException;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.user.UserAccountActivity;
import org.openremote.manager.client.user.UserAccountPlace;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;

import java.util.logging.Logger;

public class ManagerActivityMapper implements AppActivityMapper {

    private static final Logger LOG = Logger.getLogger(ManagerActivityMapper.class.getName());

    protected final SecurityService securityService;
    protected final EventBus eventBus;
    protected final ManagerMessages managerMessages;
    protected final Provider<AssetDetailActivity> assetsActivityProvider;
    protected final Provider<MapActivity> mapActivityProvider;
    protected final Provider<FlowsActivity> flowsActivityProvider;
    protected final Provider<AdminOverviewActivity> adminOverviewActivityProvider;
    protected final Provider<AdminRealmsActivity> adminRealmsActivityProvider;
    protected final Provider<AdminRealmActivity> adminRealmActivityProvider;
    protected final Provider<AdminUsersActivity> adminUsersActivityProvider;
    protected final Provider<UserAccountActivity> userProfileActivityProvider;

    @Inject
    public ManagerActivityMapper(SecurityService securityService,
                                 EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 Provider<AssetDetailActivity> assetsActivityProvider,
                                 Provider<MapActivity> mapActivityProvider,
                                 Provider<FlowsActivity> flowsActivityProvider,
                                 Provider<AdminOverviewActivity> adminOverviewActivityProvider,
                                 Provider<AdminRealmsActivity> adminRealmsActivityProvider,
                                 Provider<AdminRealmActivity> adminRealmActivityProvider,
                                 Provider<AdminUsersActivity> adminUsersActivityProvider,
                                 Provider<UserAccountActivity> userProfileActivityProvider) {
        this.securityService = securityService;
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.assetsActivityProvider = assetsActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.flowsActivityProvider = flowsActivityProvider;
        this.adminOverviewActivityProvider = adminOverviewActivityProvider;
        this.adminRealmsActivityProvider = adminRealmsActivityProvider;
        this.adminRealmActivityProvider = adminRealmActivityProvider;
        this.adminUsersActivityProvider = adminUsersActivityProvider;
        this.userProfileActivityProvider = userProfileActivityProvider;
    }

    public AppActivity getActivity(Place place) {
        try {
            if (place instanceof AssetsPlace) {
                return assetsActivityProvider.get().init(securityService, (AssetsPlace) place);
            }
            if (place instanceof MapPlace) {
                return mapActivityProvider.get().init(securityService, (MapPlace) place);
            }
            if (place instanceof FlowsPlace) {
                return flowsActivityProvider.get().init(securityService, (FlowsPlace) place);
            }
            if (place instanceof AdminOverviewPlace) {
                return adminOverviewActivityProvider.get().init(securityService, (AdminOverviewPlace) place);
            }
            if (place instanceof AdminRealmsPlace) {
                return adminRealmsActivityProvider.get().init(securityService, (AdminRealmsPlace) place);
            }
            if (place instanceof AdminRealmPlace) {
                return adminRealmActivityProvider.get().init(securityService, (AdminRealmPlace) place);
            }
            if (place instanceof AdminUsersPlace) {
                return adminUsersActivityProvider.get().init(securityService, (AdminUsersPlace) place);
            }
            if (place instanceof UserAccountPlace) {
                return userProfileActivityProvider.get().init(securityService, (UserAccountPlace) place);
            }
        } catch (RoleRequiredException ex) {
            LOG.warning("Access denied, missing required role '" + ex.getRequiredRole() + "': " + place);
            eventBus.dispatch(new ShowFailureEvent(managerMessages.accessDenied(), 5000));
        }
        return null;
    }
}
