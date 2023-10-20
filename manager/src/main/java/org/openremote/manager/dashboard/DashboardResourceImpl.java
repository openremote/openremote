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
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;

import java.util.Collections;

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
        boolean publicOnly = true;
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        try {
            // Realm should be enabled. Also takes unauthenticated users into count.
            if(!isRealmActiveAndAccessible(realm)) {
                throw new WebApplicationException(FORBIDDEN);
            }
            // if not authenticated, or having no INSIGHTS access, only fetch public dashboards
            if(isAuthenticated()) {
                publicOnly = (!hasResourceRole(ClientRole.READ_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID));
            }
            return this.dashboardStorageService.query(null, realm, getUserId(), publicOnly, false);

        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Dashboard get(RequestParams requestParams, String realm, String dashboardId) {
        boolean publicOnly = true;
        if(realm == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        try {
            // Realm should be enabled. Also takes unauthenticated users into count.
            if(!isRealmActiveAndAccessible(realm)) {
                throw new WebApplicationException(FORBIDDEN);
            }
            // if not authenticated, or having no INSIGHTS access, only take public dashboards into count
            if(isAuthenticated()) {
                publicOnly = (!hasResourceRole(ClientRole.READ_INSIGHTS.getValue(), Constants.KEYCLOAK_CLIENT_ID));
            }
            Dashboard[] dashboards = this.dashboardStorageService.query(Collections.singletonList(dashboardId), realm, getUserId(), publicOnly, false);
            if(dashboards.length == 0) {
                if(this.dashboardStorageService.exists(dashboardId, realm)) {
                    throw new WebApplicationException(FORBIDDEN); // when no dashboard returned from query, but it does exist.
                } else {
                    throw new WebApplicationException(NOT_FOUND); // aka it does not exist
                }
            }
            return dashboards[0]; // only return first entry

        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }
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
}
