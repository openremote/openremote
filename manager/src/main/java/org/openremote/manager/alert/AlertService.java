package org.openremote.manager.alert;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.alert.Alert;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.notification.Notification.HEADER_SOURCE;

//extends RouteBuilder
public class AlertService extends RouteBuilder implements ContainerService {

    public static final String ALERT_QUEUE = "seda://AlertQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    private static final Logger LOG = Logger.getLogger(AlertService.class.getName());

    protected PersistenceService persistenceService;
    protected MessageBrokerService messageBrokerService;

    protected static Processor handleAlertProcessingException(Logger logger) {
        return exchange -> {
        };
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.persistenceService = container.getService(PersistenceService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

//        LOG.info("[Custom Log Message] INITIALIZING ALERT_SERVICE");
//        LOG.severe("Crash Now!!!");
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void configure() throws Exception {

        from(ALERT_QUEUE)
                .routeId("AlertQueueProcessor")
                .doTry()
                .process( exchange -> {
                    Alert alert = exchange.getIn().getBody(Alert.class);

                    if (alert == null) {
                        //TODO: throw and exception
                    }

                    LOG.finest("Processing: " + alert.getTitle());

                })
                .endDoTry()
                .doCatch(Exception.class)
                .process(handleAlertProcessingException(LOG));
    }
}
