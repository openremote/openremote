package org.openremote.manager.dashboard;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardResource;
import org.openremote.model.dashboard.DashboardAccess;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;

import jakarta.ws.rs.WebApplicationException;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

public class DashboardResourceImpl extends ManagerWebResource implements DashboardResource {

    protected final DashboardStorageService dashboardStorageService;
    protected final MessageBrokerService messageBrokerService;

    public DashboardResourceImpl(TimerService timerService, ManagerIdentityService identityService, DashboardStorageService dashboardStorageService, MessageBrokerService messageBrokerService) {
        super(timerService, identityService);
        this.dashboardStorageService = dashboardStorageService;
        this.messageBrokerService = messageBrokerService;
    }


    @Override
    public Dashboard[] getAllRealmDashboards(RequestParams requestParams, String realm) {

        try {
            if(isAuthenticated()) {
                if(!hasResourceRole(ClientRole.READ_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    throw new WebApplicationException(FORBIDDEN);
                }
                if(!isRealmActiveAndAccessible(realm)) {
                    throw new WebApplicationException(FORBIDDEN);
                }
            }
            return this.dashboardStorageService.query(null, realm, getUserId(), false);

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Dashboard get(RequestParams requestParams, String dashboardId) {
        try {
            if(isAuthenticated() && !hasResourceRole(ClientRole.READ_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                throw new WebApplicationException(FORBIDDEN);
            }

            Dashboard[] dashboards = this.dashboardStorageService.query(dashboardId, null, getUserId(), false);
            if(dashboards.length == 0) {
                if(this.dashboardStorageService.exists(dashboardId)) {
                    throw new WebApplicationException(FORBIDDEN);
                } else {
                    throw new WebApplicationException(NOT_FOUND);
                }
            }
            Dashboard d = dashboards[0]; // only return first entry
            if(isAuthenticated() && !isRealmActiveAndAccessible(d.getRealm())) {
                throw new WebApplicationException(FORBIDDEN);
            }
            return d;

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
    }

    @Override
    public Dashboard create(RequestParams requestParams, Dashboard dashboard) {
        try {

            // Check if access to realm
            if(!isRealmActiveAndAccessible(dashboard.getRealm())) {
                throw new WebApplicationException(FORBIDDEN);
            }
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
    public void update(RequestParams requestParams, Dashboard dashboard) {
        try {

            // Check if access to realm
            if(!isRealmActiveAndAccessible(dashboard.getRealm())) {
                throw new WebApplicationException(FORBIDDEN);
            }
            this.dashboardStorageService.update(ValueUtil.clone(dashboard), getUserId());
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(RequestParams requestParams, List<String> fields) {
        try {
            this.dashboardStorageService.delete(fields, getUserId());
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }
}
