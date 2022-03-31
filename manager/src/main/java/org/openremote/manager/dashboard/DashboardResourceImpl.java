package org.openremote.manager.dashboard;

import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardResource;
import org.openremote.model.http.RequestParams;

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
        return new Dashboard[0];
    }

    @Override
    public Dashboard[] create(RequestParams requestParams, Dashboard dashboard) {
        System.out.println("Creating new User Dashboards...");
        return new Dashboard[0];
    }
}
