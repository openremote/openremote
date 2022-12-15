package org.openremote.manager.alert;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.alert.Alert;
import org.openremote.model.alert.SentAlert;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.query.UserQuery;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.openremote.manager.alert.AlertProcessingException.Reason.*;
import static org.openremote.model.alert.Alert.HEADER_TRIGGER;
import static org.openremote.model.alert.Alert.Trigger.*;


public class AlertService extends RouteBuilder implements ContainerService {

    public static final String ALERT_QUEUE = "seda://AlertQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    private static final Logger LOG = Logger.getLogger(AlertService.class.getName());

    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected MessageBrokerService messageBrokerService;

    protected static Processor handleAlertProcessingException(Logger logger) {
        return exchange -> {
            Alert alert = exchange.getIn().getBody(Alert.class);
            Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

            StringBuilder error = new StringBuilder();

            Alert.Trigger trigger = exchange.getIn().getHeader(HEADER_TRIGGER, "unknown trigger", Alert.Trigger.class);
            if (trigger != null) {
                error.append("Error processing from ").append(trigger);
            }

            String protocolName = exchange.getIn().getHeader(Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, String.class);
            if (protocolName != null) {
                error.append(" (protocol: ").append(protocolName).append(")");
            }

            // TODO Better exception handling - dead letter queue?
            if (exception instanceof AlertProcessingException) {
                AlertProcessingException processingException = (AlertProcessingException) exception;
                error.append(" - ").append(processingException.getReasonPhrase());
                error.append(": ").append(alert.toString());
                logger.warning(error.toString());
            } else {
                error.append(": ").append(alert.toString());
                logger.log(Level.WARNING, error.toString(), exception);
            }

            exchange.getMessage().setBody(false);
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
        createdAlert(new Alert("test", "test", Alert.Severity.LOW));
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
                        throw new AlertProcessingException(MISSING_ALERT, "Alert must be set");
                    }

                    LOG.finest("Processing: " + alert.getTitle());

                    if (alert.getContent().isEmpty()) {
                        throw new AlertProcessingException(MISSING_CONTENT, "Alert content must be set");
                    }

                    Alert.Trigger trigger = exchange.getIn().getHeader(HEADER_TRIGGER, () -> null, Alert.Trigger.class);

                    if (trigger == null) {
                        throw new AlertProcessingException(MISSING_TRIGGER);
                    }

                    AtomicReference<String> triggerId = new AtomicReference<>("");
                    triggerId.set(exchange.getIn().getHeader(Alert.HEADER_TRIGGER_ID, String.class));

                    String msg = "Generating alert " + alert.getTitle()  + "': '" + trigger + ":" + triggerId + "'," + " severity: '" + alert.getSeverity() + "'";
                    LOG.fine(msg);

                    LOG.log(Level.FINE, AlertService.class.getName(), AlertService.class.getMethod("configure").getName(), msg);

                    LOG.



                    persistenceService.doTransaction(em -> {
                        SentAlert sentAlert = new SentAlert()
                                .setTitle(alert.getTitle())
                                .setContent(alert.getContent())
                                .setTrigger(trigger)
                                .setTriggerId(triggerId.get())
                                .setSeverity(alert.getSeverity())
                                .setStatus(alert.getStatus());

                        sentAlert = em.merge(sentAlert);
                    });

                    exchange.getMessage().setBody(true);

                })
                .endDoTry()
                .doCatch(AlertProcessingException.class)
                .process(handleAlertProcessingException(LOG));
    }


    public boolean createdAlert(Alert alert) { return createAlert(alert, INTERNAL, ""); }

    public boolean createAlert(Alert alert, Alert.Trigger trigger, String triggerId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Alert.HEADER_TRIGGER, trigger);
        headers.put(Alert.HEADER_TRIGGER_ID, triggerId);
        return messageBrokerService.getFluentProducerTemplate().withBody(alert).withHeaders(headers).to(AlertService.ALERT_QUEUE).request(Boolean.class);
    }

}
