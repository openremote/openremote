package org.openremote.manager.dashboard;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardResource;
import org.openremote.model.http.RequestParams;

public class DashboardResourceImpl extends ManagerWebResource implements DashboardResource {


    public DashboardResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
    }


    @Override
    public Dashboard<?>[] getAllUserDashboards(RequestParams requestParams) {
        System.out.println("Getting all User Dashboards...");
        return new Dashboard[0];
    }

    @Override
    public Dashboard<?>[] create(RequestParams requestParams, Dashboard<?> dashboard) {
        System.out.println("Creating new User Dashboards...");
        return new Dashboard[0];
    }
}
