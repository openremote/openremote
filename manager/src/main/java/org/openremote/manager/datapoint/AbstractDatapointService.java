package org.openremote.manager.datapoint;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.Datapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGobject;

import javax.persistence.TypedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Base class for all classes that store and retrieve {@link org.openremote.model.datapoint.Datapoint}.
 */
public abstract class AbstractDatapointService<T extends Datapoint> implements ContainerService {

    public static final int PRIORITY = AssetStorageService.PRIORITY + 100;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected ScheduledFuture<?> dataPointsPurgeScheduledFuture;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        executorService = container.getExecutorService();
    }

    @Override
    public void stop(Container container) throws Exception {
        if (dataPointsPurgeScheduledFuture != null) {
            dataPointsPurgeScheduledFuture.cancel(true);
        }
    }

    public void upsertValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) throws IllegalStateException {
        persistenceService.doTransaction(em ->
                em.unwrap(Session.class).doWork(connection -> {

                    getLogger().finest("Storing datapoint for: id=" + assetId + ", name=" + attributeName + ", timestamp=" + timestamp + ", value=" + value);
                    PreparedStatement st;

                    try {
                        st = getUpsertPreparedStatement(connection);
                        setUpsertValues(st, assetId, attributeName, value, timestamp);
                        st.executeUpdate();
                    } catch (Exception e) {
                        String msg = "Failed to insert/update data point: ";
                        getLogger().log(Level.WARNING, msg, e);
                        throw new IllegalStateException(msg, e);
                    }
                }));
    }

    public void upsertValues(String assetId, String attributeName, List<Pair<?, LocalDateTime>> valuesAndTimestamps) throws IllegalStateException {
        persistenceService.doTransaction(em ->
                em.unwrap(Session.class).doWork(connection -> {

                    getLogger().finest("Storing datapoints for: id=" + assetId + ", name=" + attributeName + ", count=" + valuesAndTimestamps.size());
                    PreparedStatement st;

                    try {
                        st = getUpsertPreparedStatement(connection);

                        for (Pair<?, LocalDateTime> valueAndTimestamp : valuesAndTimestamps) {
                            setUpsertValues(st, assetId, attributeName, valueAndTimestamp.key, valueAndTimestamp.value);
                            st.addBatch();
                        }
                        st.executeBatch();
                    } catch (Exception e) {
                        String msg = "Failed to insert/update data points: " + assetId + ", name=" + attributeName + ", count=" + valuesAndTimestamps.size();
                        getLogger().log(Level.WARNING, msg, e);
                        throw new IllegalStateException(msg, e);
                    }
                }));
    }

    public List<T> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                        "select dp from " + getDatapointClass().getSimpleName() + " dp " +
                                "where dp.assetId = :assetId " +
                                "and dp.attributeName = :attributeName " +
                                "order by dp.timestamp desc",
                        getDatapointClass())
                        .setParameter("assetId", attributeRef.getId())
                        .setParameter("attributeName", attributeRef.getName())
                        .getResultList());
    }

    public long getDatapointsCount() {
        return getDatapointsCount(null);
    }

    public long getDatapointsCount(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> {

            String queryStr = attributeRef == null ?
                    "select count(dp) from " + getDatapointClass().getSimpleName() + " dp" :
                    "select count(dp) from " + getDatapointClass().getSimpleName() + " dp where dp.assetId = :assetId and dp.attributeName = :attributeName";

            TypedQuery<Long> query = entityManager.createQuery(
                    queryStr,
                    Long.class);

            if (attributeRef != null) {
                query
                        .setParameter("assetId", attributeRef.getId())
                        .setParameter("attributeName", attributeRef.getName());
            }

            return query.getSingleResult();
        });
    }

    public ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef,
                                                  DatapointInterval datapointInterval,
                                                  Integer stepSize,
                                                  LocalDateTime fromTimestamp,
                                                  LocalDateTime toTimestamp) {

        Asset<?> asset = assetStorageService.find(attributeRef.getId());
        if (asset == null) {
            throw new IllegalStateException("Asset not found: " + attributeRef.getId());
        }

        Attribute<?> assetAttribute = asset.getAttribute(attributeRef.getName())
                .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeRef.getName()));

        return getValueDatapoints(asset.getId(), assetAttribute, datapointInterval, stepSize, fromTimestamp, toTimestamp);
    }

    public ValueDatapoint<?>[] getValueDatapoints(String assetId,
                                                  Attribute<?> attribute,
                                                  DatapointInterval datapointInterval,
                                                  final Integer stepSize,
                                                  LocalDateTime fromTimestamp,
                                                  LocalDateTime toTimestamp) {

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        getLogger().finer("Getting datapoints for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->

                entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<ValueDatapoint<?>[]>() {
                    @Override
                    public ValueDatapoint<?>[] execute(Connection connection) throws SQLException {

                        Class<?> attributeType = attribute.getType().getType();
                        boolean isNumber = Number.class.isAssignableFrom(attributeType);
                        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
                        StringBuilder query = new StringBuilder();
                        boolean downsample = isNumber || isBoolean;

                        String truncate = null;
                        String part = null;
                        String interval = null;
                        String stepStr = null;
                        String partQuery = "date_part(?, ?)::int";
                        String partQuery2 = "date_part(?, TIMESTAMP)::int";
                        int step = 1;

                        if (downsample) {

                            switch (datapointInterval) {

                                case MINUTE:
                                    // This works with minutes of the day so not constrained to step size < 60
                                    step = stepSize == null ? 1 : Math.max(1, Math.min(1440, stepSize));
                                    truncate = "day";
                                    part = "min";
                                    interval = "min";
                                    partQuery = "(date_part('hour', ?)::int * 60 + date_part(?, ?)::int)";
                                    partQuery2 = "(date_part('hour', TIMESTAMP)::int * 60 + date_part(?, TIMESTAMP)::int)";
                                    break;
                                case HOUR:
                                    step = stepSize == null ? 1 : Math.max(1, Math.min(24, stepSize));
                                    truncate = "day";
                                    part = "hour";
                                    interval = "hour";
                                    break;
                                case DAY:
                                    step = stepSize == null ? 1 : Math.max(1, Math.min(365, stepSize));
                                    truncate = "year";
                                    part = "doy";
                                    interval = "day";
                                    break;
                                case WEEK:
                                    step = stepSize == null ? 1 : Math.max(1, Math.min(53, stepSize));
                                    truncate = "year";
                                    part = "week";
                                    interval = "week";
                                    break;
                                case MONTH:
                                    step = stepSize == null ? 1 : Math.max(1, Math.min(12, stepSize));
                                    truncate = "year";
                                    part = "month";
                                    interval = "month";
                                    break;
                                case YEAR:
                                    step = stepSize == null ? 1 : Math.max(1, stepSize);
                                    truncate = "decade";
                                    part = "year";
                                    interval = "year";
                                    break;
                                default:
                                    throw new UnsupportedOperationException("Can't handle interval: " + datapointInterval);
                            }
                            stepStr = step + " " + interval;

                            // TODO: Change this to use something like this max min decimation algorithm https://knowledge.ni.com/KnowledgeArticleDetails?id=kA00Z0000019YLKSA2&l=en-GB)
                            query.append("select PERIOD as X, AVG_VALUE as Y " +
                                    "from generate_series(date_trunc(?, ?) + " + partQuery + " / ? * ?, date_trunc(?, ?) + " + partQuery + " / ? * ?, ?) PERIOD left join ( " +
                                    "select (date_trunc(?, TIMESTAMP) + " + partQuery2 + " / ? * ?)::timestamp as TS, ");

                            if (isNumber) {
                                query.append(" AVG(VALUE::text::numeric) as AVG_VALUE ");
                            } else {
                                query.append(" AVG(case when VALUE::text::boolean is true then 1 else 0 end) as AVG_VALUE ");
                            }

                            query.append("from " + getDatapointTableName() +
                                    " where TIMESTAMP >= date_trunc(?, ?) and TIMESTAMP < (date_trunc(?, ?) + ?) and ENTITY_ID = ? and ATTRIBUTE_NAME = ? group by TS) DP on DP.TS = PERIOD order by PERIOD asc");

                        } else {
                            query.append("select distinct TIMESTAMP AS X, value AS Y from " + getDatapointTableName() +
                                    " where " +
                                    "TIMESTAMP >= ?" +
                                    "and " +
                                    "TIMESTAMP <= ? " +
                                    "and " +
                                    "ENTITY_ID = ? and ATTRIBUTE_NAME = ?"
                            );
                        }

                        try (PreparedStatement st = connection.prepareStatement(query.toString())) {

                            if (downsample) {
                                int counter = 1;
                                boolean isMinute = datapointInterval == DatapointInterval.MINUTE;

                                st.setString(counter++, truncate);
                                st.setObject(counter++, fromTimestamp);
                                if (isMinute) {
                                    st.setObject(counter++, fromTimestamp);
                                }
                                st.setString(counter++, part);
                                st.setObject(counter++, fromTimestamp);
                                st.setInt(counter++, step);
                                st.setObject(counter++, new PGInterval(stepStr));
                                st.setString(counter++, truncate);
                                st.setObject(counter++, toTimestamp);
                                if (isMinute) {
                                    st.setObject(counter++, toTimestamp);
                                }
                                st.setString(counter++, part);
                                st.setObject(counter++, toTimestamp);
                                st.setInt(counter++, step);
                                st.setObject(counter++, new PGInterval(stepStr));
                                st.setObject(counter++, new PGInterval(stepStr));
                                st.setString(counter++, truncate);
                                st.setString(counter++, part);
                                st.setInt(counter++, step);
                                st.setObject(counter++, new PGInterval(stepStr));
                                st.setString(counter++, interval);
                                st.setObject(counter++, fromTimestamp);
                                st.setString(counter++, interval);
                                st.setObject(counter++, toTimestamp);
                                st.setObject(counter++, new PGInterval(stepStr));
                                st.setString(counter++, attributeRef.getId());
                                st.setString(counter++, attributeRef.getName());
                            } else {
                                st.setObject(1, fromTimestamp);
                                st.setObject(2, toTimestamp);
                                st.setString(3, attributeRef.getId());
                                st.setString(4, attributeRef.getName());
                            }

                            try (ResultSet rs = st.executeQuery()) {
                                List<ValueDatapoint<?>> result = new ArrayList<>();
                                while (rs.next()) {
                                    Object value = null;
                                    if (rs.getObject(2) != null) {
                                        if (downsample) {
                                            value = ValueUtil.getValueCoerced(rs.getObject(2), Double.class).orElse(null);
                                        } else {
                                            if (rs.getObject(2) instanceof PGobject) {
                                                value = ValueUtil.parse(((PGobject) rs.getObject(2)).getValue()).orElse(null);
                                            } else {
                                                value = ValueUtil.getValueCoerced(rs.getObject(2), JsonNode.class).orElse(null);
                                            }
                                        }
                                    }
                                    result.add(new ValueDatapoint<>(rs.getTimestamp(1).getTime(), value));
                                }
                                return result.toArray(new ValueDatapoint<?>[0]);
                            }
                        }
                    }
                })
        );
    }

    public DatapointPeriod getDatapointPeriod(String assetId, String attributeName) {
        return persistenceService.doReturningTransaction(em ->
                em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<DatapointPeriod>() {
                    @Override
                    public DatapointPeriod execute(Connection connection) throws SQLException {
                        String tableName = getDatapointTableName();
                        String query = "SELECT DISTINCT periods.* FROM " +
                                "(SELECT entity_id, attribute_name, " +
                                "MIN(timestamp) AS oldestTimestamp, MAX(timestamp) AS latestTimestamp " +
                                "FROM " + tableName + " GROUP BY entity_id, attribute_name) AS periods " +
                                "INNER JOIN " + tableName + " ON " + tableName + ".entity_id = periods.entity_id AND " +
                                tableName + ".attribute_name = periods.attribute_name " +
                                "WHERE " + tableName + ".entity_id = ? " +
                                "AND " + tableName + ".attribute_name = ? ";
                        try (PreparedStatement st = connection.prepareStatement(query)) {
                            st.setString(1, assetId);
                            st.setString(2, attributeName);
                            try (ResultSet rs = st.executeQuery()) {
                                if (rs.next()) {
                                    return new DatapointPeriod(rs.getString(1), rs.getString(2), rs.getTimestamp(3).getTime(), rs.getTimestamp(4).getTime());
                                }
                                return new DatapointPeriod(assetId, attributeName, null, null);
                            }
                        }
                    }
                })
        );
    }

    protected PreparedStatement getUpsertPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO " + getDatapointTableName() + " (entity_id, attribute_name, value, timestamp) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE " +
                "SET value = excluded.value");
    }

    protected void setUpsertValues(PreparedStatement st, String assetId, String attributeName, Object value, LocalDateTime timestamp) throws Exception {
        PGobject pgJsonValue = new PGobject();
        pgJsonValue.setType("jsonb");
        pgJsonValue.setValue(ValueUtil.asJSON(value).orElse("null"));
        st.setString(1, assetId);
        st.setString(2, attributeName);
        st.setObject(3, pgJsonValue);
        st.setObject(4, timestamp);
    }

    protected abstract Class<T> getDatapointClass();

    protected abstract String getDatapointTableName();

    protected abstract Logger getLogger();

    protected void doPurge(String whereClause, Date date) {
        persistenceService.doTransaction(em -> em.createQuery(
                "delete from " + getDatapointClass().getSimpleName() + " dp " + whereClause
        ).setParameter("dt", date).executeUpdate());
    }

    protected long getFirstPurgeMillis(Instant currentTime) {
        // Schedule purge at approximately 3AM daily
        return ChronoUnit.MILLIS.between(
                currentTime,
                currentTime.truncatedTo(DAYS).plus(27, ChronoUnit.HOURS));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                '}';
    }
}
