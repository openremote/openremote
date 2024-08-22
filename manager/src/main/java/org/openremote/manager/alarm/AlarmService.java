package org.openremote.manager.alarm;

import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.security.ManagerIdentityProvider;
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
import org.openremote.model.security.User;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;

import static org.openremote.model.alarm.Alarm.Source.*;

public class AlarmService extends RouteBuilder implements ContainerService {

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

    private SentAlarm validateExistingAlarm(Long alarmId) {
        SentAlarm alarm = getAlarm(alarmId);
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm does not exist");
        }
        return alarm;
    }

    private void validateRealmAccessibleToUser(String userId, String realm) {
        ManagerIdentityProvider identityProvider = identityService.getIdentityProvider();
        User user = identityProvider.getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("User does not exist");
        }
        if (!identityProvider.isMasterRealmAdmin(userId) && !identityProvider.isUserInRealm(userId, realm)) {
            throw new IllegalArgumentException("User does not have access to '" + realm + "' realm");
        }
    }

    public SentAlarm sendAlarm(Alarm alarm) {
        return sendAlarm(alarm, MANUAL, "");
    }

    public SentAlarm sendAlarm(Alarm alarm, Alarm.Source source, String sourceId) {
        Objects.requireNonNull(alarm, "Alarm cannot be null");
        Objects.requireNonNull(alarm.getRealm(), "Alarm realm cannot be null");
        Objects.requireNonNull(alarm.getTitle(), "Alarm title cannot be null");
        Objects.requireNonNull(alarm.getSeverity(), "Alarm severity cannot be null");

        Objects.requireNonNull(source, "Source cannot be null");
        Objects.requireNonNull(sourceId, "Source ID cannot be null");

        long timestamp = timerService.getCurrentTimeMillis();

        return persistenceService.doReturningTransaction(entityManager -> {
            SentAlarm sentAlarm = new SentAlarm()
                    .setAssigneeId(alarm.getAssignee())
                    .setRealm(alarm.getRealm())
                    .setTitle(alarm.getTitle())
                    .setContent(alarm.getContent())
                    .setSeverity(alarm.getSeverity())
                    .setStatus(alarm.getStatus())
                    .setSource(source)
                    .setSourceId(sourceId)
                    .setCreatedOn(new Date(timestamp))
                    .setLastModified(new Date(timestamp));

            entityManager.merge(sentAlarm);

            clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.CREATE));
            if (alarm.getSeverity() == Alarm.Severity.HIGH) {
                sendAssigneeNotification(sentAlarm);
            }
            return sentAlarm;
        });
    }

    private void sendAssigneeNotification(SentAlarm alarm) {
        if (alarm.getAssigneeId() == null) {
            // TODO Get users by role??
            return;
        }

        User user = identityService.getIdentityProvider().getUser(alarm.getAssigneeId());

        String text = "Assigned to alarm: " + alarm.getTitle() + "\n" +
                "Description: " + alarm.getContent() + "\n" +
                "Severity: " + alarm.getSeverity() + "\n" +
                "Status: " + alarm.getStatus();

        Notification email = new Notification();
        email.setName("New Alarm")
                .setMessage(new EmailNotificationMessage()
                        .setText(text)
                        .setSubject("New Alarm Notification")
                        .setTo(new EmailNotificationMessage.Recipient(user.getFullName(), user.getEmail())));

        Notification push = new Notification();
        push.setName("New Alarm")
                .setMessage(
                        new PushNotificationMessage()
                                .setTitle("Alarm: " + alarm.getTitle())
                                .setBody(text)
                )
                .setTargets(List.of(new Notification.Target(Notification.TargetType.USER, alarm.getAssigneeId())));

        notificationService.sendNotificationAsync(push, Notification.Source.INTERNAL, "alarms");
        notificationService.sendNotificationAsync(email, Notification.Source.INTERNAL, "alarms");
    }

    public void updateAlarm(Long alarmId, SentAlarm alarm) {
        SentAlarm oldAlarm = validateExistingAlarm(alarmId);
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

        if (alarm.getSeverity() == Alarm.Severity.HIGH && newAssigneeId != null && !newAssigneeId.equals(oldAssigneeId)) {
            sendAssigneeNotification(alarm);
        }
    }

    public void linkAssets(List<String> assetIds, String realm, Long alarmId) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            PreparedStatement st = connection.prepareStatement("""
                    insert into ALARM_ASSET_LINK (sentalarm_id, realm, asset_id, created_on) values (?, ?, ?, ?)
                    on conflict (sentalarm_id, realm, asset_id) do nothing
                    """);
            for (String assetId : assetIds) {
                st.setLong(1, alarmId);
                st.setString(2, realm);
                st.setString(3, assetId);
                st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                st.addBatch();
            }
            st.executeBatch();
        }));
    }

    public void linkAssets(List<AlarmAssetLink> links) {
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

    public List<AlarmAssetLink> getAssetLinks(Long alarmId, String realm) throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery("""
                                select al from AlarmAssetLink al
                                where al.id.realm = :realm and al.id.sentalarmId = :alarmId
                                order by al.createdOn desc
                                """, AlarmAssetLink.class)
                        .setParameter("realm", realm)
                        .setParameter("alarmId", alarmId)
                        .getResultList()
        );
    }

    public List<SentAlarm> getAlarmsByAssetId(String assetId) throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery("""
                                select sa from SentAlarm sa
                                join AlarmAssetLink aal on sa.id = aal.id.sentalarmId
                                where aal.id.assetId = :assetId
                                order by sa.createdOn desc
                                """, SentAlarm.class)
                        .setParameter("assetId", assetId)
                        .getResultList()
        );
    }

    public List<SentAlarm> getOpenAlarms() throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery("select sa from SentAlarm sa where sa.status = 'OPEN' order by sa.createdOn desc", SentAlarm.class)
                .getResultList());
    }

    public SentAlarm getAlarm(Long alarmId) throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery("select n from SentAlarm n where n.id = :id", SentAlarm.class)
                .setParameter("id", alarmId)
                .getSingleResult());
    }

    public List<SentAlarm> getAlarms(List<Long> alarmIds) throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery("select n from SentAlarm n where n.id in :ids", SentAlarm.class)
                        .setParameter("ids", alarmIds)
                        .getResultList()
        );
    }

    public List<SentAlarm> getAlarms(String realm) throws IllegalArgumentException {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery("select n from SentAlarm n where n.realm = :realm  order by n.createdOn desc", SentAlarm.class)
                        .setParameter("realm", realm)
                        .getResultList()
        );
    }

    public void removeAlarm(Long alarmId, String realm) {
        persistenceService.doTransaction(entityManager -> entityManager
                .createQuery("delete SentAlarm where id = :id")
                .setParameter("id", alarmId)
                .executeUpdate()
        );
        clientEventService.publishEvent(new AlarmEvent(realm, PersistenceEvent.Cause.DELETE));
    }

    public void removeAlarms(List<Long> alarmIds, Set<String> realms) throws IllegalArgumentException {
        persistenceService.doTransaction(entityManager ->
                entityManager.createQuery("delete from SentAlarm n where n.id in :ids")
                        .setParameter("ids", alarmIds)
                        .executeUpdate()
        );

        realms.forEach(realm -> clientEventService.publishEvent(new AlarmEvent(realm, PersistenceEvent.Cause.DELETE)));
    }
}
