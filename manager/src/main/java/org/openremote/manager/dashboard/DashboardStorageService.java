package org.openremote.manager.dashboard;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

public class DashboardStorageService extends RouteBuilder implements ContainerService {

    protected ManagerIdentityService identityService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {
        System.out.println("Configuring DashboardStorageService..");
    }

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        container.getService(ManagerWebService.class).addApiSingleton(
                new DashboardResourceImpl(
                        container.getService(TimerService.class),
                        identityService,
                        this,
                        container.getService(MessageBrokerService.class)
                )
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }
}
