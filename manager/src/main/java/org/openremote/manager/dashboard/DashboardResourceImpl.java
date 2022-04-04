package org.openremote.manager.dashboard;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.ValueUtil;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class DashboardResourceImpl extends ManagerWebResource implements DashboardResource {

    protected final DashboardStorageService dashboardStorageService;
    protected final MessageBrokerService messageBrokerService;

    public DashboardResourceImpl(TimerService timerService, ManagerIdentityService identityService, DashboardStorageService dashboardStorageService, MessageBrokerService messageBrokerService) {
        super(timerService, identityService);
        this.dashboardStorageService = dashboardStorageService;
        this.messageBrokerService = messageBrokerService;
    }


    @Override
    public Dashboard[] getAllUserDashboards(RequestParams requestParams) {
        System.out.println("Getting all User Dashboards...");
        return this.dashboardStorageService.getAll();
    }

    @Override
    public Dashboard[] create(RequestParams requestParams, Dashboard dashboard) {
        try {
            /* TODO: temporarily disabled for executing without authorization
            //  if(!isTenantActiveAndAccessible(dashboard.getRealm())) {
            //    throw new WebApplicationException(FORBIDDEN);
            //}*/
            if(!dashboard.getTemplate().checkValidity()) {
                throw new WebApplicationException(BAD_REQUEST);
            }
            Dashboard storedDashboard = this.dashboardStorageService.createNew(ValueUtil.clone(dashboard));

        } catch (IllegalStateException ex) {
            throw new WebApplicationException(ex, BAD_REQUEST);
        }
        return new Dashboard[0];
    }
}
