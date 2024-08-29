/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.manager.alarm;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.alarm.*;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.manager.event.ClientEventService;

import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.notification.*;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.security.User;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.alarm.Alarm.Source.*;

/**
 * A service for managing {@link SentAlarm}s. It also provides functionality for managing links between {@link SentAlarm}s
 * and {@link org.openremote.model.asset.Asset}s using {@link AlarmAssetLink}s.
 */
public class AlarmService extends RouteBuilder implements ContainerService {

    public static final Logger LOG = Logger.getLogger(AlarmService.class.getName());

    private ClientEventService clientEventService;
    private ManagerIdentityService identityService;
    private NotificationService notificationService;
    private PersistenceService persistenceService;
    private TimerService timerService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.clientEventService = container.getService(ClientEventService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.notificationService = container.getService(NotificationService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.timerService = container.getService(TimerService.class);
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        container.getService(ManagerWebService.class).addApiSingleton(
                new AlarmResourceImpl(timerService, identityService, this)
        );
        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(AlarmEvent.class) || authContext == null) {
                return false;
            }

            // If not a superuser force a filter for the users realm
            if (!authContext.isSuperUser()) {
                @SuppressWarnings("unchecked")
                EventSubscription<AlarmEvent> subscription = (EventSubscription<AlarmEvent>) eventSubscription;
                subscription.setFilter(new RealmFilter<>(authContext.getAuthenticatedRealmName()));
            }

            return true;
        });
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

    }

    /**
     * Returns a set of all realms of the given alarms.
     */
    protected Set<String> getAlarmRealms(List<SentAlarm> alarms) {
        return alarms == null ? Set.of() : alarms.stream().map(SentAlarm::getRealm).collect(Collectors.toSet());
    }

    /**
     * Throws an {@link IllegalArgumentException} if {@code alarmId} is null or negative.
     */
    protected void validateAlarmId(Long alarmId) {
        if (alarmId == null) {
            throw new IllegalArgumentException("Missing alarm ID");
        }
        if (alarmId < 0) {
            throw new IllegalArgumentException("Alarm ID cannot be negative");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given {@code alarmIds} are null, empty or negative.
     */
    protected void validateAlarmIds(List<Long> alarmIds) {
        if (alarmIds == null || alarmIds.isEmpty()) {
            throw new IllegalArgumentException("Missing alarm IDs");
        }
        alarmIds.forEach(this::validateAlarmId);
    }

    /**
     * Validates if the {@code alarm} is non-null and the user can access to the alarm realm.
     */
    protected void validateAlarmExistsAndAccessible(SentAlarm alarm, String userId) {
        if (alarm == null) {
            throw new EntityNotFoundException("Alarm does not exist");
        }
        validateRealmAccessibleToUser(userId, alarm.getRealm());
    }

    /**
     * Validates if the {@code alarms} are non-null, match the alarmIds length and the user has access to all alarm realms.
     */
    protected void validateAlarmsExistAndAccessible(List<Long> alarmIds, List<SentAlarm> alarms, String userId) {
        if (alarms == null || alarmIds.size() != alarms.size()) {
            throw new EntityNotFoundException("One or more alarms do not exist");
        }
        validateRealmsAccessibleToUser(userId, getAlarmRealms(alarms));
    }

    /**
     * Validates if the user has access to the given realm.
     */
    protected void validateRealmAccessibleToUser(String userId, String realm) {
        validateRealmsAccessibleToUser(userId, Set.of(realm));
    }

    /**
     * Validates if the user has access to all given realms.
     */
    protected void validateRealmsAccessibleToUser(String userId, Set<String> realms) {
        if (userId == null) {
            return;
        }
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        User user = identityProvider.getUser(userId);
        if (user == null) {
            throw new IllegalStateException("User does not exist");
        }

        if (!identityProvider.isMasterRealmAdmin(userId) &&
                (realms.size() > 1 || !identityProvider.isUserInRealm(userId, realms.stream().findFirst().orElse(null)))) {
            throw new SecurityException("Realm is not active or inaccessible");
        }
    }

    /**
     * Sends an alarm if the given user has access to the alarm realm.
     */
    public SentAlarm sendAlarm(Alarm alarm, String userId) {
        Objects.requireNonNull(alarm, "Alarm cannot be null");
        Objects.requireNonNull(alarm.getRealm(), "Alarm realm cannot be null");
        Objects.requireNonNull(alarm.getTitle(), "Alarm title cannot be null");
        Objects.requireNonNull(alarm.getSeverity(), "Alarm severity cannot be null");
        Objects.requireNonNull(alarm.getSource(), "Source cannot be null");
        Objects.requireNonNull(alarm.getSourceId(), "Source ID cannot be null");

        validateRealmAccessibleToUser(userId, alarm.getRealm());

        long timestamp = timerService.getCurrentTimeMillis();

        SentAlarm result = persistenceService.doReturningTransaction(entityManager -> {
            SentAlarm sentAlarm = new SentAlarm()
                    .setAssigneeId(alarm.getAssignee())
                    .setRealm(alarm.getRealm())
                    .setTitle(alarm.getTitle())
                    .setContent(alarm.getContent())
                    .setSeverity(alarm.getSeverity())
                    .setStatus(alarm.getStatus())
                    .setSource(alarm.getSource())
                    .setSourceId(alarm.getSourceId())
                    .setCreatedOn(new Date(timestamp))
                    .setLastModified(new Date(timestamp));

            entityManager.merge(sentAlarm);

            return sentAlarm;
        });

        if (result != null) {
            clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.CREATE));
            if (alarm.getSeverity() == Alarm.Severity.HIGH) {
                Set<String> excludeUserIds = alarm.getSource() == MANUAL ? Set.of(alarm.getSourceId()) : Set.of();
                sendAssigneeNotification(result, excludeUserIds);
            }
        }

        return result;
    }

    /**
     * Sends e-mail and push notifications for an alarm to the alarm assignee. If an assignee is not set all users having
     * the {@link Constants#READ_ADMIN_ROLE} or{@link Constants#WRITE_ALARMS_ROLE} are notified.
     *
     * @param alarm the alarm to send e-mail and push notifications for
     * @param excludeUserIds users matching these user IDs will are excluded from the notifications
     */
    protected void sendAssigneeNotification(SentAlarm alarm, Set<String> excludeUserIds) {
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();

        List<User> users = new ArrayList<>();
        if (alarm.getAssigneeId() == null) {
            UserQuery userQuery = new UserQuery()
                    .realm(new RealmPredicate(alarm.getRealm()))
                    .realmRoles(new StringPredicate(Constants.REALM_ADMIN_ROLE), new StringPredicate(Constants.WRITE_ALARMS_ROLE))
                    .serviceUsers(false);
            users.addAll(Arrays.asList(identityProvider.queryUsers(userQuery)));
        } else {
            users.add(identityProvider.getUser(alarm.getAssigneeId()));
        }

        users.removeIf(user -> excludeUserIds.contains(user.getId()));

        if (users.isEmpty()) {
            LOG.fine("No matching users to send alarm notification");
            return;
        }

        LOG.fine("Sending alarm notification to " + users.size() + " matching user(s)");

        String text = "Assigned to alarm: " + alarm.getTitle() + "\n" +
                "Description: " + alarm.getContent() + "\n" +
                "Severity: " + alarm.getSeverity() + "\n" +
                "Status: " + alarm.getStatus();

        Notification email = new Notification()
                .setName("New Alarm")
                .setMessage(new EmailNotificationMessage()
                        .setText(text)
                        .setSubject("New Alarm Notification")
                        .setTo(users.stream()
                                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                                .map(user -> new EmailNotificationMessage.Recipient(user.getFullName(), user.getEmail())).toList()));

        Notification push = new Notification();
        push.setName("New Alarm")
                .setMessage(
                        new PushNotificationMessage()
                                .setTitle("Alarm: " + alarm.getTitle())
                                .setBody(text)
                )
                .setTargets(users.stream().map(user -> new Notification.Target(Notification.TargetType.USER, user.getId())).toList());

        notificationService.sendNotificationAsync(push, Notification.Source.INTERNAL, "alarms");
        notificationService.sendNotificationAsync(email, Notification.Source.INTERNAL, "alarms");
    }

    /**
     * Updates an existing alarm if the given user has access to the alarm realm.
     */
    public void updateAlarm(Long alarmId, String userId, SentAlarm alarm) {
        SentAlarm oldAlarm = getAlarm(alarmId, userId);
        String oldAssigneeId = oldAlarm.getAssigneeId();
        String newAssigneeId = alarm.getAssigneeId();

        if (newAssigneeId != null) {
            validateRealmAccessibleToUser(newAssigneeId, alarm.getRealm());
        }

        persistenceService.doTransaction(entityManager -> entityManager.createQuery("""
                        update SentAlarm set title=:title, content=:content, severity=:severity, status=:status, lastModified=:lastModified, assigneeId=:assigneeId
                        where id =:id
                        """)
                .setParameter("id", alarmId)
                .setParameter("title", alarm.getTitle())
                .setParameter("content", alarm.getContent())
                .setParameter("severity", alarm.getSeverity())
                .setParameter("status", alarm.getStatus())
                .setParameter("lastModified", new Timestamp(timerService.getCurrentTimeMillis()))
                .setParameter("assigneeId", newAssigneeId)
                .executeUpdate());

        clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.UPDATE));

        if (alarm.getSeverity() == Alarm.Severity.HIGH) {
            Set<String> excludeUserIds = Stream.of(userId, oldAssigneeId).filter(Objects::nonNull).collect(Collectors.toSet());
            sendAssigneeNotification(alarm, excludeUserIds);
        }
    }

    /**
     * Links one asset to an existing alarm.
     */
    public void linkAsset(String assetId, String realm, Long alarmId) {
        linkAssets(List.of(assetId), realm, alarmId);
    }

    /**
     * Links multiple assets to an existing alarm.
     */
    public void linkAssets(List<String> assetIds, String realm, Long alarmId) {
        linkAssets(assetIds.stream().map(assetId -> new AlarmAssetLink(realm, alarmId, assetId)).toList(), null);
    }

    /**
     * Links multiple assets to existing alarms if the given user has access to the alarm realms.
     */
    public void linkAssets(List<AlarmAssetLink> links, String userId) {
        if (links == null || links.isEmpty()) {
            throw new IllegalArgumentException("Missing links");
        }

        if (userId != null) {
            Set<String> realms = links.stream().map(link -> link.getId().getRealm()).collect(Collectors.toSet());
            validateRealmsAccessibleToUser(userId, realms);
        }

        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            PreparedStatement st = connection.prepareStatement("""
                    insert into ALARM_ASSET_LINK (sentalarm_id, realm, asset_id, created_on) values (?, ?, ?, ?)
                    on conflict (sentalarm_id, realm, asset_id) do nothing
                    """);
            for (AlarmAssetLink link : links) {
                st.setLong(1, link.getId().getAlarmId());
                st.setString(2, link.getId().getRealm());
                st.setString(3, link.getId().getAssetId());
                st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                st.addBatch();
            }
            st.executeBatch();
        }));
    }

    /**
     * Returns the assets linked to an alarm if the given user has access to the alarm realm.
     */
    public List<AlarmAssetLink> getAssetLinks(Long alarmId, String userId, String realm) throws IllegalArgumentException {
        getAlarm(alarmId, userId);
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery("""
                                select aal from AlarmAssetLink aal
                                where aal.id.realm = :realm and aal.id.sentalarmId = :alarmId
                                order by aal.createdOn desc
                                """, AlarmAssetLink.class)
                        .setParameter("realm", realm)
                        .setParameter("alarmId", alarmId)
                        .getResultList()
        );
    }

    /**
     * Retrieves the details of an existing alarm if the given user has access to the alarm realm.
     */
    public SentAlarm getAlarm(Long alarmId, String userId) throws IllegalArgumentException {
        validateAlarmId(alarmId);
        SentAlarm alarm;
        try {
            alarm = persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery("select sa from SentAlarm sa where sa.id = :id", SentAlarm.class)
                    .setParameter("id", alarmId)
                    .getSingleResult());
        } catch (PersistenceException e) {
            alarm = null;
        }
        validateAlarmExistsAndAccessible(alarm, userId);
        return alarm;
    }

    /**
     * Retrieves the details of existing alarms if the given user has access to the alarm realms.
     */
    public List<SentAlarm> getAlarms(List<Long> alarmIds, String userId) throws IllegalArgumentException {
        validateAlarmIds(alarmIds);
        List<SentAlarm> alarms;
        try {
            alarms = persistenceService.doReturningTransaction(entityManager ->
                    entityManager.createQuery("select sa from SentAlarm sa where sa.id in :ids", SentAlarm.class)
                            .setParameter("ids", alarmIds)
                            .getResultList()
            );
        } catch (PersistenceException e) {
            alarms = null;
        }
        validateAlarmsExistAndAccessible(alarmIds, alarms, userId);
        return alarms;
    }

    /**
     * Retrieves all existing alarms in a realm. The {@code status}, {@code assetId} and {@code assigneeId} parameters
     * are optional and if non-null are used for filtering the alarms.
     */
    public List<SentAlarm> getAlarms(String realm, Alarm.Status status, String assetId, String assigneeId) throws IllegalArgumentException {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder sb = new StringBuilder("select sa from SentAlarm sa ");

        if (assetId != null) {
            sb.append("join AlarmAssetLink aal on sa.id = aal.id.sentalarmId where sa.realm = :realm and aal.id.assetId = :assetId ");
            parameters.put("assetId", assetId);
        } else {
            sb.append("where sa.realm = :realm ");
        }
        parameters.put("realm", realm);

        if (status != null) {
            sb.append("and sa.status = :status ");
            parameters.put("status", status);
        }
        if (assigneeId != null) {
            sb.append("and sa.assigneeId = :assigneeId ");
            parameters.put("assigneeId", assigneeId);
        }
        sb.append("order by sa.createdOn desc");

        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<SentAlarm> query = entityManager.createQuery(sb.toString(), SentAlarm.class);
            parameters.forEach(query::setParameter);
            return query.getResultList();
        });
    }

    /**
     * Removes an existing alarms if the given user has access to the alarm realm.
     */
    public void removeAlarm(Long alarmId, String userId) {
        SentAlarm alarm = getAlarm(alarmId, userId);
        persistenceService.doTransaction(entityManager -> entityManager
                .createQuery("delete SentAlarm where id = :id")
                .setParameter("id", alarmId)
                .executeUpdate()
        );
        clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.DELETE));
    }

    /**
     * Removes existing alarms if the given user has access to the alarm realms.
     */
    public void removeAlarms(List<Long> alarmIds, String userId) throws IllegalArgumentException {
        List<SentAlarm> alarms = getAlarms(alarmIds, userId);
        persistenceService.doTransaction(entityManager ->
                entityManager.createQuery("delete from SentAlarm sa where sa.id in :ids")
                        .setParameter("ids", alarmIds)
                        .executeUpdate()
        );

        getAlarmRealms(alarms).forEach(realm -> clientEventService.publishEvent(new AlarmEvent(realm, PersistenceEvent.Cause.DELETE)));
    }
}
