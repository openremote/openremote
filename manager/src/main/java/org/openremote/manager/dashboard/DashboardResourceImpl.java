/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.manager.dashboard;

import jakarta.ws.rs.WebApplicationException;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardAccess;
import org.openremote.model.dashboard.DashboardResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.DashboardQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.*;

public class DashboardResourceImpl extends ManagerWebResource implements DashboardResource {

    public static final Logger LOG = Logger.getLogger(DashboardResourceImpl.class.getName());

    protected final DashboardStorageService dashboardStorageService;
    protected final MessageBrokerService messageBrokerService;

    public DashboardResourceImpl(TimerService timerService, ManagerIdentityService identityService, DashboardStorageService dashboardStorageService, MessageBrokerService messageBrokerService) {
        super(timerService, identityService);
        this.dashboardStorageService = dashboardStorageService;
        this.messageBrokerService = messageBrokerService;
    }

    @Override
    public Dashboard[] getAllRealmDashboards(RequestParams requestParams, String realm) {
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if(!isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }
        return dashboardStorageService.query(this.createDashboardQuery(realm), getUserId());
    }

    @Override
    public Dashboard get(RequestParams requestParams, String realm, String dashboardId) {
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        Dashboard[] dashboards = dashboardStorageService.query(
                this.createDashboardQuery(realm).ids(dashboardId).limit(1),
                getUserId()
        );
        // If no dashboards were returned, check whether it existed, and return a different response.
        if(dashboards.length == 0) {
            if(this.dashboardStorageService.exists(dashboardId, realm)) {
                throw new WebApplicationException(FORBIDDEN);
            } else {
                throw new WebApplicationException(NOT_FOUND);
            }
        }

        return dashboards[0];
    }

    @Override
    public Dashboard[] query(RequestParams requestParams, DashboardQuery query) {
        if(query == null) {
            query = this.createDashboardQuery(getRequestRealmName());
        }
        if(query.realm == null || query.realm.name == null) {
            query.realm(new RealmPredicate(getRequestRealmName()));
        }

        // Filter access fields in query object, to prevent cross-realm access
        if(!isSuperUser() && !(getAuthenticatedRealmName().equals(query.realm.name))) {
            query.conditions.getDashboard().viewAccess(filterAccess(query.conditions.getDashboard().getViewAccess(), DashboardAccess.PRIVATE, DashboardAccess.SHARED));
            query.conditions.getDashboard().editAccess(filterAccess(query.conditions.getDashboard().getEditAccess(), DashboardAccess.PRIVATE, DashboardAccess.SHARED));
        }

        return dashboardStorageService.query(query, getUserId());
    }

    public DashboardAccess[] filterAccess(DashboardAccess[] access, DashboardAccess... filter) {
        return Arrays.stream(access).filter(a -> Arrays.stream(filter).noneMatch(f -> f.equals(a))).toArray(DashboardAccess[]::new);
    }

    @Override
    public Dashboard create(RequestParams requestParams, Dashboard dashboard) {
        String realm = dashboard.getRealm();
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if(!isRealmActiveAndAccessible(dashboard.getRealm())) {
            throw new WebApplicationException(FORBIDDEN);
        }
        try {
            dashboard.setOwnerId(getUserId());
            dashboard.setViewAccess(DashboardAccess.SHARED);
            dashboard.setEditAccess(DashboardAccess.SHARED);

            return this.dashboardStorageService.createNew(ValueUtil.clone(dashboard));

        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Dashboard update(RequestParams requestParams, Dashboard dashboard) {
        String realm = dashboard.getRealm();
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if(!isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }
        try {
            return this.dashboardStorageService.update(ValueUtil.clone(dashboard), realm, getUserId());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex, NOT_FOUND);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String realm, String dashboardId) {
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if(!isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException(FORBIDDEN);
        }
        try {
            this.dashboardStorageService.delete(dashboardId, realm, getUserId());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex, NOT_FOUND);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Function that builds an {@link DashboardQuery} object, based on user permissions.
     * Automatically fills NULL values, adjusts access filters based on roles, etc.
     *
     * @param realm
     * @return An instantiated DashboardQuery with the correct permissions based on the user.
     */
    protected DashboardQuery createDashboardQuery(String realm) {
        if(realm == null) {
            realm = getRequestRealmName();
        }
        DashboardQuery query = new DashboardQuery().realm(new RealmPredicate(realm));
        Set<DashboardAccess> userViewAccess = new HashSet<>(Set.of(query.conditions.getDashboard().getViewAccess()));
        Set<DashboardAccess> userEditAccess = new HashSet<>(Set.of(query.conditions.getDashboard().getEditAccess()));
        Set<DashboardQuery.AssetAccess> assetAccess = new HashSet<>(Set.of(query.conditions.getAsset().getAccess()));

        // (unauthenticated) User always has access to public dashboards
        userViewAccess.add(DashboardAccess.PUBLIC);
        userEditAccess.add(DashboardAccess.PUBLIC);
        assetAccess.add(DashboardQuery.AssetAccess.REALM);

        // Adjust query object based on user roles/permissions
        if (isSuperUser() || (isAuthenticated() && realm.equals(getAuthenticatedRealmName()))) {
            assetAccess.add(DashboardQuery.AssetAccess.LINKED);
            if (hasResourceRole(ClientRole.READ_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                Collections.addAll(userViewAccess, DashboardAccess.SHARED, DashboardAccess.PRIVATE);
            }
            if (hasResourceRole(ClientRole.WRITE_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                Collections.addAll(userEditAccess, DashboardAccess.SHARED, DashboardAccess.PRIVATE);
            }
            if (isRestrictedUser()) {
                assetAccess = new HashSet<>(Set.of(DashboardQuery.AssetAccess.RESTRICTED));
            }
        } else {
            // If not logged in, force only public read/write access
            userViewAccess = new HashSet<>(Set.of(DashboardAccess.PUBLIC));
            userEditAccess = new HashSet<>(Set.of(DashboardAccess.PUBLIC));
            assetAccess = new HashSet<>(Set.of(DashboardQuery.AssetAccess.REALM));
        }

        // Build query object and return
        return query.conditions(new DashboardQuery.Conditions(
                        new DashboardQuery.DashboardConditions()
                                .viewAccess(userViewAccess.toArray(new DashboardAccess[0]))
                                .editAccess(userEditAccess.toArray(new DashboardAccess[0])),
                        new DashboardQuery.AssetConditions()
                                .access(assetAccess.toArray(new DashboardQuery.AssetAccess[0]))
                )
        );
    }
}
