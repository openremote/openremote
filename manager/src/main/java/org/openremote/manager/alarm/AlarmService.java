package org.openremote.manager.alarm;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.manager.alarm.AlarmProcessingException;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.UserQuery;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.*;
import static org.openremote.manager.alarm.AlarmProcessingException.Reason.*;
import static org.openremote.model.alarm.Alarm.HEADER_SOURCE;
import static org.openremote.model.alarm.Alarm.Source.*;

public class AlarmService extends RouteBuilder implements ContainerService {

    public static final String ALARM_QUEUE = "seda://AlarmQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    private static final Logger LOG = Logger.getLogger(org.openremote.manager.alarm.AlarmService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected MessageBrokerService messageBrokerService;
    //protected Map<String, NotificationHandler> notificationHandlerMap = new HashMap<>();

    protected static Processor handleNotificationProcessingException(Logger logger) {
        return exchange -> {
            Alarm alarm = exchange.getIn().getBody(Alarm.class);
            Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

            StringBuilder error = new StringBuilder();

            Alarm.Source source = exchange.getIn().getHeader(HEADER_SOURCE, "unknown source", Alarm.Source.class);
            if (source != null) {
                error.append("Error processing from ").append(source);
            }

            String protocolName = exchange.getIn().getHeader(Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, String.class);
            if (protocolName != null) {
                error.append(" (protocol: ").append(protocolName).append(")");
            }
//
//            // TODO Better exception handling - dead letter queue?
//            if (exception instanceof NotificationProcessingException) {
//                NotificationProcessingException processingException = (NotificationProcessingException) exception;
//                error.append(" - ").append(processingException.getReasonPhrase());
//                error.append(": ").append(notification.toString());
//                logger.warning(error.toString());
//            } else {
//                error.append(": ").append(notification.toString());
//                logger.log(Level.WARNING, error.toString(), exception);
//            }

            exchange.getMessage().setBody(false);
        };
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

//        container.getServices(NotificationHandler.class).forEach(notificationHandler ->
//                notificationHandlerMap.put(notificationHandler.getTypeName(), notificationHandler));
//
//        container.getService(ManagerWebService.class).addApiSingleton(
//                new NotificationResourceImpl(this,
//                        container.getService(MessageBrokerService.class),
//                        container.getService(AssetStorageService.class),
//                        container.getService(ManagerIdentityService.class))
//        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

        from(ALARM_QUEUE)
                .routeId("AlarmQueueProcessor")
                .doTry()
                .process(exchange -> {
                    SentAlarm alarm = exchange.getIn().getBody(SentAlarm.class);

                    if (alarm == null) {
                        throw new AlarmProcessingException(MISSING_ALARM, "Alarm must be set");
                    }

                    LOG.finest("Processing: " + alarm.getTitle());

                    if (alarm.getContent() == null) {
                        throw new AlarmProcessingException(MISSING_CONTENT, "Alarm content must be set");
                    }

                    Alarm.Source source = exchange.getIn().getHeader(HEADER_SOURCE, () -> null, Alarm.Source.class);

                    if (source == null) {
                        throw new AlarmProcessingException(MISSING_SOURCE);
                    }

//                    // Validate handler and message
//                    NotificationHandler handler = notificationHandlerMap.get(notification.getMessage().getType());
//                    if (handler == null) {
//                        throw new NotificationProcessingException(UNSUPPORTED_MESSAGE_TYPE, "No handler for message type: " + notification.getMessage().getType());
//                    }
//                    if (!handler.isValid()) {
//                        throw new NotificationProcessingException(NOTIFICATION_HANDLER_CONFIG_ERROR, "Handler is not valid: " + handler.getTypeName());
//                    }
//                    if (!handler.isMessageValid(notification.getMessage())) {
//                        throw new NotificationProcessingException(INVALID_MESSAGE);
//                    }

                    // Validate access and map targets to handler compatible targets
                    String realm = null;
                    String userId = null;
                    String assetId = null;
                    AtomicReference<String> sourceId = new AtomicReference<>("");
                    boolean isSuperUser = false;
                    boolean isRestrictedUser = false;

                    switch (source) {
                        case INTERNAL:
                            isSuperUser = true;
                            break;

                        case CLIENT:

                            AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
//                            if (authContext == null) {
//                                // Anonymous clients cannot send notifications
//                                throw new NotificationProcessingException(INSUFFICIENT_ACCESS);
//                            }

                            realm = authContext.getAuthenticatedRealmName();
                            userId = authContext.getUserId();
                            sourceId.set(userId);
                            isSuperUser = authContext.isSuperUser();
                            isRestrictedUser = identityService.getIdentityProvider().isRestrictedUser(authContext);
                            break;

                        case GLOBAL_RULESET:
                            isSuperUser = true;
                            break;

                        case REALM_RULESET:
                            realm = exchange.getIn().getHeader(Alarm.HEADER_SOURCE_ID, String.class);
                            sourceId.set(realm);
                            break;

                        case ASSET_RULESET:
                            assetId = exchange.getIn().getHeader(Alarm.HEADER_SOURCE_ID, String.class);
                            sourceId.set(assetId);
                            Asset<?> asset = assetStorageService.find(assetId, false);
                            realm = asset.getRealm();
                            break;
                    }

                    LOG.fine("Sending " + alarm.getContent() + " alarm '" + alarm.getTitle() + "': '" + source + ":" + sourceId.get() + "'");

//                    // Check access permissions
//                    checkAccess(source, sourceId.get(), notification.getTargets(), realm, userId, isSuperUser, isRestrictedUser, assetId);
//
//                    // Get the list of notification targets
//                    List<Notification.Target> mappedTargetsList = handler.getTargets(source, sourceId.get(), notification.getTargets(), notification.getMessage());
//
//                    if (mappedTargetsList == null || mappedTargetsList.isEmpty()) {
//                        throw new NotificationProcessingException(MISSING_TARGETS, "Notification targets must be set");
//                    }
//
//                    // Filter targets based on repeat frequency
//                    if (!TextUtil.isNullOrEmpty(notification.getName()) && (!TextUtil.isNullOrEmpty(notification.getRepeatInterval()) || notification.getRepeatFrequency() != null)) {
//                        mappedTargetsList = mappedTargetsList.stream()
//                                .filter(target -> okToSendNotification(source, sourceId.get(), target, notification))
//                                .collect(Collectors.toList());
//                    }
//
//                    // Send message to each applicable target
//                    AtomicBoolean success = new AtomicBoolean(true);
//
//                    mappedTargetsList.forEach(
//                            target -> {
//                                boolean targetSuccess = persistenceService.doReturningTransaction(em -> {
//
//                                    // commit the notification first to get the ID
//                                    SentNotification sentNotification = new SentNotification()
//                                            .setName(notification.getName())
//                                            .setType(notification.getMessage().getType())
//                                            .setSource(source)
//                                            .setSourceId(sourceId.get())
//                                            .setTarget(target.getType())
//                                            .setTargetId(target.getId())
//                                            .setMessage(notification.getMessage())
//                                            .setSentOn(Date.from(timerService.getNow()));
//
//                                    sentNotification = em.merge(sentNotification);
//                                    long id = sentNotification.getId();
//
//                                    try {
//                                        NotificationSendResult result = handler.sendMessage(
//                                                id,
//                                                source,
//                                                sourceId.get(),
//                                                target,
//                                                notification.getMessage());
//
//                                        if (result.isSuccess()) {
//                                            LOG.fine("Notification sent '" + id + "': " + target);
//                                        } else {
//                                            LOG.warning("Notification failed '" + id + "': " + target + ", reason=" + result.getMessage());
//                                            sentNotification.setError(TextUtil.isNullOrEmpty(result.getMessage()) ? "Unknown error" : result.getMessage());
//                                        }
//                                        // Merge the sent notification again with the message included just in case the handler modified the message
//                                        sentNotification.setMessage(notification.getMessage());
//                                        em.merge(sentNotification);
//                                    } catch (Exception e) {
//                                        LOG.log(Level.SEVERE,
//                                                "Notification handler threw an exception whilst sending notification '" + id + "'",
//                                                e);
//                                        sentNotification.setError(TextUtil.isNullOrEmpty(e.getMessage()) ? "Unknown error" : e.getMessage());
//                                        em.merge(sentNotification);
//                                    }
//                                    return sentNotification.getError() == null;
//                                });
//                                if (!targetSuccess) {
//                                    success.set(false);
//                                }
//                            }
//                    );
//
//                    exchange.getMessage().setBody(success.get());
                })
                .endDoTry()
                .doCatch(AlarmProcessingException.class)
                .process(handleNotificationProcessingException(LOG));
    }

    public boolean sendAlarm(Alarm alarm) {
        return sendAlarm(alarm, INTERNAL, "");
    }

    public boolean sendAlarm(Alarm alarm, Alarm.Source source, String sourceId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Alarm.HEADER_SOURCE, source);
        headers.put(Alarm.HEADER_SOURCE_ID, sourceId);
        return messageBrokerService.getFluentProducerTemplate().withBody(alarm).withHeaders(headers).to(AlarmService.ALARM_QUEUE).request(Boolean.class);
    }

    public void setAlarmAcknowledged(String id) {
        setAlarmAcknowledged(id, timerService.getCurrentTimeMillis());
    }

    public void setAlarmAcknowledged(String id, long timestamp) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE SentAlarm SET acknowledgedOn=:timestamp WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("timestamp", new Date(timestamp));
            query.executeUpdate();
        });
    }

    public void updateAlarmStatus(String id, String status) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE SentAlarm SET status=:status WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("status", status);
            query.executeUpdate();
        });
    }

    public SentAlarm getSentAlarm(String alarmId) {
        return persistenceService.doReturningTransaction(em -> em.find(SentAlarm.class, alarmId));
    }

    public List<SentAlarm> getAlarms() throws IllegalArgumentException {
        StringBuilder builder = new StringBuilder();
        builder.append("select n from SentAlarm n where 1=1");
        List<Object> parameters = new ArrayList<>();
        processCriteria(builder, parameters,false);
        builder.append(" order by n.createdOn asc");
        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<SentAlarm> query = entityManager.createQuery(builder.toString(), SentAlarm.class);
            IntStream.range(0, parameters.size())
                    .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            return query.getResultList();

        });
    }

//    public void removeAlarm(Long id) {
//        persistenceService.doTransaction(entityManager -> entityManager
//                .createQuery("delete SentAlarm where id = :id")
//                .setParameter("id", id)
//                .executeUpdate()
//        );
//    }
//
//    public void removeAlarms(List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> realmIds, List<String> userIds, List<String> assetIds) throws IllegalArgumentException {
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("delete from SentAlarm n where 1=1");
//        List<Object> parameters = new ArrayList<>();
//        processCriteria(builder, parameters, ids, types, fromTimestamp, toTimestamp, realmIds, userIds, assetIds, true);
//
//        persistenceService.doTransaction(entityManager -> {
//            Query query = entityManager.createQuery(builder.toString());
//            IntStream.range(0, parameters.size())
//                    .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
//            query.executeUpdate();
//        });
//    }

    protected void processCriteria(StringBuilder builder, List<Object> parameters, boolean isRemove) {
        //boolean hasIds = id != null;
        int counter = 0;

//        if (hasIds) {
//            counter++;
//        }

//        if (isRemove && fromTimestamp == null && toTimestamp == null && counter == 0) {
//            LOG.fine("No filters set for remove alarms request so not allowed");
//            throw new IllegalArgumentException("No criteria specified");
//        }

//        if (hasIds) {
//            builder.append(" AND n.id IN ?")
//                    .append(parameters.size() + 1);
//            parameters.add(id);
//            return;
//        }
//
//        if (fromTimestamp != null) {
//            builder.append(" AND n.createdOn >= ?")
//                    .append(parameters.size() + 1);
//
//            parameters.add(new Date(fromTimestamp));
//        }
//
//        if (toTimestamp != null) {
//            builder.append(" AND n.createdOn <= ?")
//                    .append(parameters.size() + 1);
//
//            parameters.add(new Date(toTimestamp));
//        }
    }
}
