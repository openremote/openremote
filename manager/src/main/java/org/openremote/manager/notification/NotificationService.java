/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.notification;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
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
import org.openremote.model.notification.Notification;
import org.openremote.model.notification.NotificationSendResult;
import org.openremote.model.notification.RepeatFrequency;
import org.openremote.model.notification.SentNotification;
import org.openremote.model.query.UserQuery;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.*;
import static org.openremote.manager.notification.NotificationProcessingException.Reason.*;
import static org.openremote.model.notification.Notification.HEADER_SOURCE;
import static org.openremote.model.notification.Notification.Source.*;

// TODO Implement notification purging - configurable MAX_AGE for notifications?
public class NotificationService extends RouteBuilder implements ContainerService {

    public static final String NOTIFICATION_QUEUE = "seda://NotificationQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected MessageBrokerService messageBrokerService;
    protected Map<String, NotificationHandler> notificationHandlerMap = new HashMap<>();

    protected static Processor handleNotificationProcessingException(Logger logger) {
        return exchange -> {
            Notification notification = exchange.getIn().getBody(Notification.class);
            Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

            StringBuilder error = new StringBuilder();

            Notification.Source source = exchange.getIn().getHeader(HEADER_SOURCE, "unknown source", Notification.Source.class);
            if (source != null) {
                error.append("Error processing from ").append(source);
            }

            String protocolName = exchange.getIn().getHeader(Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, String.class);
            if (protocolName != null) {
                error.append(" (protocol: ").append(protocolName).append(")");
            }

            // TODO Better exception handling - dead letter queue?
            if (exception instanceof NotificationProcessingException) {
                NotificationProcessingException processingException = (NotificationProcessingException) exception;
                error.append(" - ").append(processingException.getReasonPhrase());
                error.append(": ").append(notification.toString());
                logger.warning(error.toString());
            } else {
                error.append(": ").append(notification.toString());
                logger.log(Level.WARNING, error.toString(), exception);
            }

            // Make the exception available if MEP is InOut
            exchange.getOut().setBody(exception);
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

        container.getServices(NotificationHandler.class).forEach(notificationHandler ->
                notificationHandlerMap.put(notificationHandler.getTypeName(), notificationHandler));

        container.getService(ManagerWebService.class).getApiSingletons().add(
                new NotificationResourceImpl(this,
                        container.getService(MessageBrokerService.class),
                        container.getService(AssetStorageService.class),
                        container.getService(ManagerIdentityService.class))
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

        from(NOTIFICATION_QUEUE)
                .routeId("NotificationQueueProcessor")
                .doTry()
                .process(exchange -> {
                    Notification notification = exchange.getIn().getBody(Notification.class);

                    if (notification == null) {
                        throw new NotificationProcessingException(MISSING_NOTIFICATION, "Notification must be set");
                    }

                    LOG.finest("Processing: " + notification.getName());

                    if (notification.getMessage() == null) {
                        throw new NotificationProcessingException(MISSING_MESSAGE, "Notification message must be set");
                    }

                    Notification.Source source = exchange.getIn().getHeader(HEADER_SOURCE, () -> null, Notification.Source.class);

                    if (source == null) {
                        throw new NotificationProcessingException(MISSING_SOURCE);
                    }

                    // Validate handler and message
                    NotificationHandler handler = notificationHandlerMap.get(notification.getMessage().getType());
                    if (handler == null) {
                        throw new NotificationProcessingException(UNSUPPORTED_MESSAGE_TYPE, "No handler for message type: " + notification.getMessage().getType());
                    }
                    if (!handler.isMessageValid(notification.getMessage())) {
                        throw new NotificationProcessingException(INVALID_MESSAGE);
                    }

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
                            if (authContext == null) {
                                // Anonymous clients cannot send notifications
                                throw new NotificationProcessingException(INSUFFICIENT_ACCESS);
                            }

                            realm = authContext.getAuthenticatedRealm();
                            userId = authContext.getUserId();
                            sourceId.set(userId);
                            isSuperUser = authContext.isSuperUser();
                            isRestrictedUser = identityService.getIdentityProvider().isRestrictedUser(authContext);
                            break;

                        case GLOBAL_RULESET:
                            isSuperUser = true;
                            break;

                        case TENANT_RULESET:
                            realm = exchange.getIn().getHeader(Notification.HEADER_SOURCE_ID, String.class);
                            sourceId.set(realm);
                            break;

                        case ASSET_RULESET:
                            assetId = exchange.getIn().getHeader(Notification.HEADER_SOURCE_ID, String.class);
                            sourceId.set(assetId);
                            Asset<?> asset = assetStorageService.find(assetId, false);
                            realm = asset.getRealm();
                            break;
                    }

                    LOG.info("Sending " + notification.getMessage().getType() + " notification '" + notification.getName() + "': '" + source + ":" + sourceId.get() + "' -> " + notification.getTargets());

                    // Check access permissions
                    checkAccess(source, sourceId.get(), notification.getTargets(), realm, userId, isSuperUser, isRestrictedUser, assetId);

                    // Get the list of notification targets
                    List<Notification.Target> mappedTargetsList = handler.getTargets(source, sourceId.get(), notification.getTargets(), notification.getMessage());

                    if (mappedTargetsList == null || mappedTargetsList.isEmpty()) {
                        throw new NotificationProcessingException(MISSING_TARGETS, "Notification targets must be set");
                    }

                    // Filter targets based on repeat frequency
                    if (!TextUtil.isNullOrEmpty(notification.getName()) && (!TextUtil.isNullOrEmpty(notification.getRepeatInterval()) || notification.getRepeatFrequency() != null)) {
                        mappedTargetsList = mappedTargetsList.stream()
                            .filter(target -> okToSendNotification(source, sourceId.get(), target, notification))
                            .collect(Collectors.toList());
                    }

                    // Send message to each applicable target
                    mappedTargetsList.forEach(
                        target ->
                            persistenceService.doTransaction(em -> {

                                // commit the notification first to get the ID
                                SentNotification sentNotification = new SentNotification()
                                    .setName(notification.getName())
                                    .setType(notification.getMessage().getType())
                                    .setSource(source)
                                    .setSourceId(sourceId.get())
                                    .setTarget(target.getType())
                                    .setTargetId(target.getId())
                                    .setMessage(notification.getMessage())
                                    .setSentOn(Date.from(timerService.getNow()));

                                sentNotification = em.merge(sentNotification);
                                long id = sentNotification.getId();

                                try {
                                    NotificationSendResult result = handler.sendMessage(
                                        id,
                                        source,
                                        sourceId.get(),
                                        target,
                                        notification.getMessage());

                                    if (result.isSuccess()) {
                                        LOG.info("Notification sent '" + id + "': " + target);
                                    } else {
                                        LOG.warning("Notification failed '" + id + "': " + target + ", reason=" + result.getMessage());
                                        sentNotification.setError(TextUtil.isNullOrEmpty(result.getMessage()) ? "Unknown error" : result.getMessage());
                                    }
                                    // Merge the sent notification again with the message included just in case the handler modified the message
                                    sentNotification.setMessage(notification.getMessage());
                                    em.merge(sentNotification);
                                } catch (Exception e) {
                                    LOG.log(Level.SEVERE,
                                        "Notification handler threw an exception whilst sending notification '" + id + "'",
                                        e);
                                    sentNotification.setError(TextUtil.isNullOrEmpty(e.getMessage()) ? "Unknown error" : e.getMessage());
                                    em.merge(sentNotification);
                                }
                            })
                    );
                })
                .endDoTry()
                .doCatch(NotificationProcessingException.class)
                .process(handleNotificationProcessingException(LOG));
    }

    public void sendNotification(Notification notification) throws NotificationProcessingException {
        sendNotification(notification, INTERNAL, "");
    }

    public void sendNotification(Notification notification, Notification.Source source, String sourceId) throws NotificationProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Notification.HEADER_SOURCE, source);
        headers.put(Notification.HEADER_SOURCE_ID, sourceId);
        messageBrokerService.getProducerTemplate().sendBodyAndHeaders(NotificationService.NOTIFICATION_QUEUE, notification, headers);
    }

    public void setNotificationDelivered(long id) {
        setNotificationDelivered(id, timerService.getCurrentTimeMillis());
    }

    public void setNotificationDelivered(long id, long timestamp) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE SentNotification SET deliveredOn=:timestamp WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("timestamp", new Date(timestamp));
            query.executeUpdate();
        });
    }

    public void setNotificationAcknowleged(long id, String acknowledgement) {
        setNotificationAcknowleged(id, acknowledgement, timerService.getCurrentTimeMillis());
    }

    public void setNotificationAcknowleged(long id, String acknowledgement, long timestamp) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE SentNotification SET acknowledgedOn=:timestamp, acknowledgement=:acknowledgement WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("timestamp", new Date(timestamp));
            query.setParameter("acknowledgement", acknowledgement);
            query.executeUpdate();
        });
    }

    public SentNotification getSentNotification(Long notificationId) {
        return persistenceService.doReturningTransaction(em -> em.find(SentNotification.class, notificationId));
    }

    public List<SentNotification> getNotifications(List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> tenantIds, List<String> userIds, List<String> assetIds) throws IllegalArgumentException {
        StringBuilder builder = new StringBuilder();
        builder.append("select n from SentNotification n where 1=1");
        List<Object> parameters = new ArrayList<>();
        processCriteria(builder, parameters, ids, types, fromTimestamp, toTimestamp, tenantIds, userIds, assetIds, false);
        builder.append(" order by n.sentOn asc");
        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<SentNotification> query = entityManager.createQuery(builder.toString(), SentNotification.class);
            IntStream.range(0, parameters.size())
                    .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            return query.getResultList();
        });
    }

    public void removeNotification(Long id) {
        persistenceService.doTransaction(entityManager -> entityManager
                .createQuery("delete SentNotification where id = :id")
                .setParameter("id", id)
                .executeUpdate()
        );
    }

    public void removeNotifications(List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> tenantIds, List<String> userIds, List<String> assetIds) throws IllegalArgumentException {

        StringBuilder builder = new StringBuilder();
        builder.append("delete from SentNotification n where 1=1");
        List<Object> parameters = new ArrayList<>();
        processCriteria(builder, parameters, ids, types, fromTimestamp, toTimestamp, tenantIds, userIds, assetIds, true);

        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery(builder.toString());
            IntStream.range(0, parameters.size())
                    .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            query.executeUpdate();
        });
    }

    protected void processCriteria(StringBuilder builder, List<Object> parameters, List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> tenantIds, List<String> userIds, List<String> assetIds, boolean isRemove) {
        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasTypes = types != null && !types.isEmpty();
        boolean hasTenants = tenantIds != null && !tenantIds.isEmpty();
        boolean hasUsers = userIds != null && !userIds.isEmpty();
        boolean hasAssets = assetIds != null && !assetIds.isEmpty();
        int counter = 0;

        if (hasIds) {
            counter++;
        }
        if (hasTypes) {
            counter++;
        }
        if (hasTenants) {
            counter++;
        }
        if (hasUsers) {
            counter++;
        }
        if (hasAssets) {
            counter++;
        }

        if (isRemove && fromTimestamp == null && toTimestamp == null && counter == 0) {
            LOG.info("No filters set for remove notifications request so not allowed");
            throw new IllegalArgumentException("No criteria specified");
        }

        if (hasIds) {
            builder.append(" AND n.id IN ?")
                    .append(parameters.size() + 1);
            parameters.add(ids);
            return;
        }

        if (hasTypes) {
            builder.append(" AND n.type IN ?")
                    .append(parameters.size() + 1);
            parameters.add(types);
        }

        if (fromTimestamp != null) {
            builder.append(" AND n.sentOn >= ?")
                    .append(parameters.size() + 1);

            parameters.add(new Date(fromTimestamp));
        }

        if (toTimestamp != null) {
            builder.append(" AND n.sentOn <= ?")
                    .append(parameters.size() + 1);

            parameters.add(new Date(toTimestamp));
        }

        if (hasAssets) {
            builder.append(" AND n.target = ?")
                    .append(parameters.size() + 1)
                    .append(" AND n.targetId IN ?")
                    .append(parameters.size() + 2);

            parameters.add(Notification.TargetType.ASSET);
            parameters.add(assetIds);

        } else if (hasUsers) {
            builder.append(" AND n.target = ?")
                    .append(parameters.size() + 1)
                    .append(" AND n.targetId IN ?")
                    .append(parameters.size() + 2);

            parameters.add(Notification.TargetType.USER);
            parameters.add(userIds);

        } else if (hasTenants) {
            builder.append(" AND n.target = ?")
                    .append(parameters.size() + 1)
                    .append(" AND n.targetId IN ?")
                    .append(parameters.size() + 2);

            parameters.add(Notification.TargetType.TENANT);
            parameters.add(tenantIds);
        }
    }

    protected Instant getRepeatAfterTimestamp(Notification notification, Instant lastSend) {
        Instant timestamp = null;

        if (TextUtil.isNullOrEmpty(notification.getName())) {
            return null;
        }

        if (notification.getRepeatFrequency() != null) {

            switch (notification.getRepeatFrequency()) {

                case HOURLY:
                    timestamp = lastSend.truncatedTo(HOURS).plus(1, HOURS);
                    break;
                case DAILY:
                    timestamp = lastSend.truncatedTo(DAYS).plus(1, DAYS);
                    break;
                case WEEKLY:
                    timestamp = lastSend.truncatedTo(WEEKS).plus(1, WEEKS);
                    break;
                case MONTHLY:
                    timestamp = lastSend.truncatedTo(MONTHS).plus(1, MONTHS);
                    break;
                case ANNUALLY:
                    timestamp = lastSend.truncatedTo(YEARS).plus(1, YEARS);
                    break;
            }
        } else if (!TextUtil.isNullOrEmpty(notification.getRepeatInterval())) {
            timestamp = lastSend.plus(TimeUtil.parseTimeDuration(notification.getRepeatInterval()), ChronoUnit.MILLIS);
        }

        return timestamp;
    }

    @SuppressWarnings("unchecked")
    protected void checkAccess(Notification.Source source, String sourceId, List<Notification.Target> targets, String realm, String userId, boolean isSuperUser, boolean isRestrictedUser, String assetId) throws NotificationProcessingException {

        if (isSuperUser) {
            return;
        }

        if (targets == null || targets.isEmpty()) {
            return;
        }

        targets.forEach(target -> {

            switch (target.getType()) {

                case TENANT:
                    if (source == CLIENT || source == ASSET_RULESET) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS);
                    }
                case USER:
                    if (TextUtil.isNullOrEmpty(realm) || isRestrictedUser) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS);
                    }

                    // Requester must be in the same realm as all target users
                    boolean realmMatch = false;

                    if (target.getType() == Notification.TargetType.USER) {
                        realmMatch = Arrays.stream(identityService.getIdentityProvider().queryUsers(new UserQuery().ids(target.getId())))
                                .allMatch(user -> realm.equals(user.getRealm()));
                    } else {
                        // Can only send to the same realm as the requestor realm
                        realmMatch = realm.equals(target.getId());
                    }

                    if (!realmMatch) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS, "Targets must all be in the same realm as the requestor");
                    }
                    break;

                case ASSET:
                    if (TextUtil.isNullOrEmpty(realm)) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS);
                    }

                    // If requestor is restricted user check all target assets are linked to that user
                    if (isRestrictedUser && !assetStorageService.isUserAssets(userId, Collections.singletonList(target.getId()))) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS, "Targets must all be linked to the requesting restricted user");
                    }

                    // Target assets must be in the same realm as requester
                    if (!assetStorageService.isRealmAssets(realm, Collections.singletonList(target.getId()))) {
                        throw new NotificationProcessingException(INSUFFICIENT_ACCESS, "Targets must all be in the same realm as the requestor");
                    }

                    // Target assets must be descendants of the requesting asset
                    if (!TextUtil.isNullOrEmpty(assetId)) {
                        if (!assetStorageService.isDescendantAssets(assetId, Collections.singletonList(target.getId()))) {
                            throw new NotificationProcessingException(INSUFFICIENT_ACCESS, "Targets must all be descendants of the requesting asset");
                        }
                    }
                    break;
            }
        });
    }

    protected boolean okToSendNotification(Notification.Source source, String sourceId, Notification.Target target, Notification notification) {

        if (notification.getRepeatFrequency() == RepeatFrequency.ALWAYS) {
            return true;
        }

        Date lastSend = persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
                "SELECT n.sentOn FROM SentNotification n WHERE n.source =:source AND n.sourceId =:sourceId AND n.target =:target AND n.targetId =:targetId AND n.name =:name ORDER BY n.sentOn DESC", Date.class)
                .setParameter("source", source)
                .setParameter("sourceId", sourceId)
                .setParameter("target", target.getType())
                .setParameter("targetId", target.getId())
                .setParameter("name", notification.getName())
                .setMaxResults(1)
                .getResultList()).stream().findFirst().orElse(null);

        return lastSend == null ||
                (notification.getRepeatFrequency() != RepeatFrequency.ONCE &&
                        timerService.getNow().plusSeconds(1).isAfter(getRepeatAfterTimestamp(notification, lastSend.toInstant())));
    }
}
