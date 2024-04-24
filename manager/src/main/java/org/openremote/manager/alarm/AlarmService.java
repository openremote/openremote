package org.openremote.manager.alarm;

import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.manager.notification.NotificationService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.alarm.*;
import org.openremote.model.alarm.Alarm.Status;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.manager.event.ClientEventService;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.notification.*;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.openremote.model.alarm.Alarm.Source.*;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AlarmService extends RouteBuilder implements ContainerService {

    private static final Logger LOGGER = Logger.getLogger(org.openremote.manager.alarm.AlarmService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected MessageBrokerService messageBrokerService;
    protected NotificationService notificationService;
    protected ClientEventService clientEventService;

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
        this.notificationService = container.getService(NotificationService.class);
        this.clientEventService = container.getService(ClientEventService.class);
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        container.getService(ManagerWebService.class).addApiSingleton(
                new AlarmResourceImpl(this,
                        container.getService(MessageBrokerService.class),
                        container.getService(AssetStorageService.class),
                        container.getService(ManagerIdentityService.class))
        );
        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(AlarmEvent.class) || authContext == null) {
                return false;
            }

            // If not a super user force a filter for the users realm
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

    public SentAlarm sendAlarm(Alarm alarm) {
        return sendAlarm(alarm, MANUAL, "");
    }

    public SentAlarm sendAlarm(Alarm alarm, Alarm.Source source, String sourceId) {
        try {
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
                TypedQuery<Long> query = entityManager.createQuery("select max(id) from SentAlarm", Long.class);
                sentAlarm.setId(query.getSingleResult());

                clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.CREATE));
                if(alarm.getAssignee() != null && alarm.getSeverity() == Alarm.Severity.HIGH){
                    sendAssigneeNotification(alarm);
                }
                return sentAlarm;
            });
        } catch (Exception e) {
            String msg = "Failed to create alarm: " + (alarm != null ? alarm.getTitle() : ' ');
            LOGGER.log(Level.WARNING, msg, e);
            return new SentAlarm();
        }
    }

    private void sendAssigneeNotification(Alarm alarm) {
        try {
            List<Notification.Target> assignee = new ArrayList<>();
            assignee.add(new Notification.Target(Notification.TargetType.USER, alarm.getAssignee()));
            Notification email = new Notification();
            email.setName("New Alarm")
                    .setMessage(new EmailNotificationMessage()
                    .setText("Assigned to alarm: " + alarm.getTitle() + "\n" +
                            "Description: " + alarm.getContent() + "\n" +
                            "Severity: " + alarm.getSeverity() + "\n" +
                            "Status: " + alarm.getStatus())
                    .setSubject("New Alarm Notification"))
                    .setTargets(assignee);

            Notification push = new Notification();
            push.setName("New Alarm")
                    .setMessage(
                    new PushNotificationMessage()
                            .setTitle("Alarm: " + alarm.getTitle())
                            .setBody("Assigned to alarm: " + alarm.getTitle() + "\n" +
                                    "Description: " + alarm.getContent() + "\n" +
                                    "Severity: " + alarm.getSeverity() + "\n" +
                                    "Status: " + alarm.getStatus())
                    )
                    .setTargets(assignee);

            notificationService.sendNotification(push);
            notificationService.sendNotification(email);


            LOGGER.info("Notifying user of new alarm: " + alarm.getTitle() + ": " + alarm.getAssignee());

        } catch (Exception e) {
            String msg = "Failed to send email concerning alarm: " + alarm.getTitle();
            LOGGER.log(Level.WARNING, msg, e);
        }
    }

    public void assignUser(Long alarmId, String userId){
        try {
            persistenceService.doTransaction(entityManager -> {
                Query query = entityManager.createQuery("UPDATE SentAlarm SET assigneeId=:assigneeId WHERE id =:id");
                query.setParameter("id", alarmId);
                query.setParameter("assigneeId", userId);
                query.executeUpdate();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAlarm(Long id, SentAlarm alarm) {
        try {
            persistenceService.doTransaction(entityManager -> {
                Query query = entityManager.createQuery("UPDATE SentAlarm SET title=:title, content=:content, severity=:severity, status=:status, lastModified=:lastModified, assigneeId=:assigneeId WHERE id =:id");
                query.setParameter("id", id);
                query.setParameter("title", alarm.getTitle());
                query.setParameter("content", alarm.getContent());
                query.setParameter("severity", alarm.getSeverity());
                query.setParameter("status", alarm.getStatus());
                query.setParameter("lastModified", new Timestamp(timerService.getCurrentTimeMillis()));
                query.setParameter("assigneeId", alarm.getAssigneeId());
                query.executeUpdate();
            });
            clientEventService.publishEvent(new AlarmEvent(alarm.getRealm(), PersistenceEvent.Cause.UPDATE));
        } catch (Exception e) {
            String msg = "Failed to update alarm: " + alarm.getTitle();
            LOGGER.log(Level.WARNING, msg, e);
        }
    }

    public void assignUser(Long alarmId, String userId, String realm) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            PreparedStatement st;

            try {
                st = connection.prepareStatement("INSERT INTO ALARM_USER_LINK (alarm_id, realm, user_id, created_on) VALUES (?, ?, ?, ?) ON CONFLICT (alarm_id, realm, user_id) DO NOTHING");
                st.setLong(1, alarmId);
                st.setString(2, realm);
                st.setObject(3, userId);
                st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                st.addBatch();

                st.executeBatch();

            } catch (Exception e) {
                String msg = "Failed to create user alarm link";
                throw new IllegalStateException(msg, e);
            }
        }));
    }

    public List<AlarmUserLink> getUserLinks(Long alarmId, String realm) throws IllegalArgumentException {
        try {
            return persistenceService.doReturningTransaction(entityManager -> {
                StringBuilder sb = new StringBuilder();
                Map<String, Object> parameters = new HashMap<>(2);
                sb.append("select al from AlarmUserLink al where 1=1");

                if (!isNullOrEmpty(realm)) {
                    sb.append(" and al.id.realm = :realm");
                    parameters.put("realm", realm);
                }
                if (alarmId != null) {
                    sb.append(" and al.id.alarmId = :alarmId");
                    parameters.put("alarmId", alarmId);
                }
                sb.append(" order by al.createdOn desc");

                TypedQuery<AlarmUserLink> query = entityManager.createQuery(sb.toString(), AlarmUserLink.class);
                parameters.forEach(query::setParameter);
                return query.getResultList();
            });

        } catch (Exception e) {
            String msg = "Failed to get user alarm links";
            throw new IllegalStateException(msg, e);
        }
    }

    public void linkAssets(List<String> assetIds, String realm, Long alarmId) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            try {
                PreparedStatement st = connection.prepareStatement("INSERT INTO ALARM_ASSET_LINK (sentalarm_id, realm, asset_id, created_on) VALUES (?, ?, ?, ?) ON CONFLICT (sentalarm_id, realm, asset_id) DO NOTHING");;
                for (String assetId : assetIds) {
                    st.setLong(1, alarmId);
                    st.setString(2, realm);
                    st.setString(3, assetId);
                    st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                    st.addBatch();
                }
                st.executeBatch();

            } catch (Exception e) {
                String msg = "Failed to create asset alarm link";
                throw new IllegalStateException(msg, e);
            }
        }));
    }

    public void linkAssets(List<AlarmAssetLink> links) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            try {
                PreparedStatement st = connection.prepareStatement("INSERT INTO ALARM_ASSET_LINK (sentalarm_id, realm, asset_id, created_on) VALUES (?, ?, ?, ?) ON CONFLICT (sentalarm_id, realm, asset_id) DO NOTHING");;
                for (AlarmAssetLink link : links) {
                    st.setLong(1, link.getId().getAlarmId());
                    st.setString(2, link.getId().getRealm());
                    st.setString(3, link.getId().getAssetId());
                    st.setTimestamp(4, new Timestamp(timerService.getCurrentTimeMillis()));
                    st.addBatch();
                }
                st.executeBatch();

            } catch (Exception e) {
                String msg = "Failed to create asset alarm link";
                throw new IllegalStateException(msg, e);
            }
        }));
    }

    public List<AlarmAssetLink> getAssetLinks(Long alarmId, String realm) throws IllegalArgumentException {
        try {
            return persistenceService.doReturningTransaction(entityManager -> {
                StringBuilder sb = new StringBuilder();
                Map<String, Object> parameters = new HashMap<>(2);
                sb.append("select al from AlarmAssetLink al where 1=1");

                if (!isNullOrEmpty(realm)) {
                    sb.append(" and al.id.realm = :realm");
                    parameters.put("realm", realm);
                }
                if (alarmId != null) {
                    sb.append(" and al.id.sentalarmId = :alarmId");
                    parameters.put("alarmId", alarmId);
                }
                sb.append(" order by al.createdOn desc");

                TypedQuery<AlarmAssetLink> query = entityManager.createQuery(sb.toString(), AlarmAssetLink.class);
                parameters.forEach(query::setParameter);

                return query.getResultList();

            });

        } catch (Exception e) {
            String msg = "Failed to get asset alarm links";
            throw new IllegalStateException(msg, e);
        }
    }

    public List<SentAlarm> getAlarmsByAssetId(String assetId) throws IllegalArgumentException {
        try {
            return persistenceService.doReturningTransaction(entityManager -> {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT sa FROM SentAlarm sa ");
                sb.append("JOIN AlarmAssetLink aal ON sa.id = aal.id.sentalarmId ");
                sb.append("WHERE aal.id.assetId = :assetId ");
                sb.append("ORDER BY sa.createdOn DESC");

                TypedQuery<SentAlarm> query = entityManager.createQuery(sb.toString(), SentAlarm.class);
                query.setParameter("assetId", assetId);

                return query.getResultList();
            });

        } catch (Exception e) {
            String msg = "Failed to get alarms by assetId";
            throw new IllegalStateException(msg, e);
        }
    }

    public List<SentAlarm> getOpenAlarms() throws IllegalArgumentException {
        try {
            return persistenceService.doReturningTransaction(entityManager -> {
                TypedQuery<SentAlarm> query = entityManager.createQuery("SELECT sa FROM SentAlarm sa WHERE sa.status = 'OPEN' ORDER BY sa.createdOn DESC", SentAlarm.class);
                return query.getResultList();
            });

        } catch (Exception e) {
            String msg = "Failed to get open alarms";
            throw new IllegalStateException(msg, e);
        }
    }

    public List<SentAlarm> getAlarms() throws IllegalArgumentException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("select n from SentAlarm n where 1=1");
            List<Object> parameters = new ArrayList<>();
            builder.append(" order by n.createdOn desc");
            return persistenceService.doReturningTransaction(entityManager -> {
                TypedQuery<SentAlarm> query = entityManager.createQuery(builder.toString(), SentAlarm.class);
                IntStream.range(0, parameters.size())
                        .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
                return query.getResultList();

            });
        } catch (Exception e) {
            String msg = "Failed to get alarms";
            throw new IllegalStateException(msg, e);
        }
    }

    public void removeAlarm(Long id, String realm) {
        try {
            persistenceService.doTransaction(entityManager -> entityManager
                    .createQuery("delete SentAlarm where id = :id")
                    .setParameter("id", id)
                    .executeUpdate()
            );
            clientEventService.publishEvent(new AlarmEvent(realm, PersistenceEvent.Cause.DELETE));
        } catch (Exception e) {
            String msg = "Failed to remove alarm";
            throw new IllegalStateException(msg, e);
        }
    }

    public void removeAlarms(List<Long> ids, String realm) throws IllegalArgumentException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("delete from SentAlarm n where n.id in :ids");

            persistenceService.doTransaction(entityManager -> {
                Query query = entityManager.createQuery(builder.toString());
                query.setParameter("ids", ids);
                query.executeUpdate();
            });

            clientEventService.publishEvent(new AlarmEvent(realm, PersistenceEvent.Cause.DELETE));
        } catch (Exception e) {
            String msg = "Failed to remove alarms";
            throw new IllegalStateException(msg, e);
        }
    }
}
