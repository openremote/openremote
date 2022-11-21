package org.openremote.manager.webhook;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.timer.TimerService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.webhook.Webhook;

import java.util.logging.Logger;

public class WebhookService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebhookService.class.getName());
    protected TimerService timerService;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {

    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public void sendHttpRequest(Webhook webhook) {
        LOG.info("Sending HTTP Request for webhook...");
    }
}
