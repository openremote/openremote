package org.openremote.manager.alarm;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.alarm.AlarmUserLink;
import org.openremote.model.alarm.Alarm.Status;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.notification.Notification;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.util.logging.Level.FINE;
import static org.openremote.model.alarm.Alarm.HEADER_SOURCE;
import static org.openremote.model.alarm.Alarm.Source.*;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AlarmService extends RouteBuilder implements ContainerService {

    public static final String ALARM_QUEUE = "seda://AlarmQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    private static final Logger LOG = Logger.getLogger(org.openremote.manager.alarm.AlarmService.class.getName());
    protected TimerService timerService;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService identityService;
    protected MessageBrokerService messageBrokerService;
    //protected Map<String, NotificationHandler> notificationHandlerMap = new HashMap<>();

    protected static Processor handleAlarmProcessingException(Logger logger) {
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

            if (exception instanceof AlarmProcessingException) {
                AlarmProcessingException processingException = (AlarmProcessingException) exception;
                error.append(" - ").append(processingException.getReasonPhrase());
                error.append(": ").append(alarm.toString());
                logger.warning(error.toString());
            } else {
                error.append(": ").append(alarm.toString());
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
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.assetStorageService = container.getService(AssetStorageService.class);
        this.identityService = container.getService(ManagerIdentityService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        container.getService(ManagerWebService.class).addApiSingleton(
                new AlarmResourceImpl(this,
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

    }

    public SentAlarm sendAlarm(Alarm alarm) {
        return sendAlarm(alarm, INTERNAL, "", "master");
    }

    public SentAlarm sendAlarm(Alarm alarm, Alarm.Source source, String sourceId, String realm) {
        try {
            Long timestamp = timerService.getCurrentTimeMillis();
            return persistenceService.doReturningTransaction(entityManager -> {
                SentAlarm sentAlarm = new SentAlarm()
                        .setRealm(realm)
                        .setTitle(alarm.getTitle())
                        .setContent(alarm.getContent())
                        .setSeverity(alarm.getSeverity())
                        .setStatus(alarm.getStatus())
                        .setSource(source)
                        .setSourceId(sourceId)
                        .setCreatedOn(new Date(timestamp));

                entityManager.merge(sentAlarm);
                TypedQuery<Long> query = entityManager.createQuery("select max(id) from SentAlarm", Long.class);
                sentAlarm.setId(query.getSingleResult());
                return sentAlarm;
            });
        } catch (Exception e) {
            String msg = "Failed to create alarm: " + alarm.getTitle();
            LOG.log(Level.WARNING, msg, e);
            return new SentAlarm();
        }
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
        if(status == Status.ACKNOWLEDGED.toString()){
            this.setAlarmAcknowledged(id);
        }
    }



    public void updateAlarm(Long id, Alarm alarm) {
        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery("UPDATE SentAlarm SET title=:title, content=:content, severity=:severity, status=:status WHERE id =:id");
            query.setParameter("id", id);
            query.setParameter("title", alarm.getTitle());
            query.setParameter("content", alarm.getContent());
            query.setParameter("severity", alarm.getSeverity());
            query.setParameter("status", alarm.getStatus());
            query.executeUpdate();
        });
    }

    public void assignUser(Long alarmId, String userId, String realm) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            if (LOG.isLoggable(FINE)) {
                LOG.fine("Storing user alarm link");
            }
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
                LOG.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
        }));
    }

    public List<AlarmUserLink> getUserLinks(Long alarmId, String realm) throws IllegalArgumentException {
        if (LOG.isLoggable(FINE)) {
            LOG.fine("Getting user alarm links");
        }

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
                List<AlarmUserLink> result = query.getResultList();
                return result;
            });

        } catch (Exception e) {
            String msg = "Failed to get user alarm links";
            LOG.log(Level.WARNING, msg, e);
            throw new IllegalStateException(msg, e);
        }
}

    public void linkAssets(ArrayList<String> assetIds, String realm, Long alarmId) {
        persistenceService.doTransaction(entityManager -> entityManager.unwrap(Session.class).doWork(connection -> {
            if (LOG.isLoggable(FINE)) {
                LOG.fine("Storing asset alarm link");
            }

            try {
                PreparedStatement st = connection.prepareStatement("INSERT INTO ALARM_ASSET_LINK (alarm_id, realm, asset_id, created_on) VALUES (?, ?, ?, ?) ON CONFLICT (alarm_id, realm, asset_id) DO NOTHING");;
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
                LOG.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
        }));
    }

    public List<AlarmAssetLink> getAssetLinks(Long alarmId, String realm) throws IllegalArgumentException {
            if (LOG.isLoggable(FINE)) {
                LOG.fine("Getting asset alarm links");
            }

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
                        sb.append(" and al.id.alarmId = :alarmId");
                        parameters.put("alarmId", alarmId);
                    }
                    sb.append(" order by al.createdOn desc");

                    TypedQuery<AlarmAssetLink> query = entityManager.createQuery(sb.toString(), AlarmAssetLink.class);
                    parameters.forEach(query::setParameter);
                    List<AlarmAssetLink> result = query.getResultList();
                    return result;
                });

            } catch (Exception e) {
                String msg = "Failed to get asset alarm links";
                LOG.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
    }

    public SentAlarm getSentAlarm(String alarmId) {
        return persistenceService.doReturningTransaction(em -> em.find(SentAlarm.class, alarmId));
    }

    public List<SentAlarm> getAlarms() throws IllegalArgumentException {
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
    }

    public void removeAlarm(Long id) {
        persistenceService.doTransaction(entityManager -> entityManager
                .createQuery("delete SentAlarm where id = :id")
                .setParameter("id", id)
                .executeUpdate()
        );
    }

    public void removeAlarms(List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> realmIds, List<String> userIds, List<String> assetIds) throws IllegalArgumentException {

        StringBuilder builder = new StringBuilder();
        builder.append("delete from SentAlarm n where 1=1");
        List<Object> parameters = new ArrayList<>();
        processCriteria(builder, parameters, ids, types, fromTimestamp, toTimestamp, realmIds, userIds, assetIds, true);

        persistenceService.doTransaction(entityManager -> {
            Query query = entityManager.createQuery(builder.toString());
            IntStream.range(0, parameters.size())
                    .forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            query.executeUpdate();
        });
    }

    protected void processCriteria(StringBuilder builder, List<Object> parameters, List<Long> ids, List<String> types, Long fromTimestamp, Long toTimestamp, List<String> realmIds, List<String> userIds, List<String> assetIds, boolean isRemove) {
        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasTypes = types != null && !types.isEmpty();
        boolean hasRealms = realmIds != null && !realmIds.isEmpty();
        boolean hasUsers = userIds != null && !userIds.isEmpty();
        boolean hasAssets = assetIds != null && !assetIds.isEmpty();
        int counter = 0;

        if (hasIds) {
            counter++;
        }
        if (hasTypes) {
            counter++;
        }
        if (hasRealms) {
            counter++;
        }
        if (hasUsers) {
            counter++;
        }
        if (hasAssets) {
            counter++;
        }

        if (isRemove && fromTimestamp == null && toTimestamp == null && counter == 0) {
            LOG.fine("No filters set for remove notifications request so not allowed");
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

        } else if (hasRealms) {
            builder.append(" AND n.target = ?")
                    .append(parameters.size() + 1)
                    .append(" AND n.targetId IN ?")
                    .append(parameters.size() + 2);

            parameters.add(Notification.TargetType.REALM);
            parameters.add(realmIds);
        }
    }
}
