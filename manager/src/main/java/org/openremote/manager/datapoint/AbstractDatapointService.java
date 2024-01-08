/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.manager.datapoint;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.TypedQuery;
import jakarta.validation.constraints.NotNull;
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
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.util.ValueUtil;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        upsertValue(assetId, attributeName, value, timestamp.toInstant(ZoneOffset.UTC).toEpochMilli());
    }
    public void upsertValue(String assetId, String attributeName, Object value, long timestamp) throws IllegalStateException {
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

    public void upsertValues(String assetId, String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps) throws IllegalStateException {
        persistenceService.doTransaction(em ->
                em.unwrap(Session.class).doWork(connection -> {

                    getLogger().finest("Storing datapoints for: id=" + assetId + ", name=" + attributeName + ", count=" + valuesAndTimestamps.size());
                    PreparedStatement st;

                    try {
                        st = getUpsertPreparedStatement(connection);

                        for (ValueDatapoint<?> valueAndTimestamp : valuesAndTimestamps) {
                            setUpsertValues(st, assetId, attributeName, valueAndTimestamp.getValue(), valueAndTimestamp.getTimestamp());
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

    public List<ValueDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                        "select new org.openremote.model.datapoint.ValueDatapoint(dp.timestamp, dp.value) from " + getDatapointClass().getSimpleName() + " dp " +
                                "where dp.assetId = :assetId " +
                                "and dp.attributeName = :attributeName " +
                                "order by dp.timestamp desc",
                        ValueDatapoint.class)
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

    public List<ValueDatapoint<?>> queryDatapoints(String assetId, String attributeName, AssetDatapointQuery datapointQuery) {
        Asset<?> asset = assetStorageService.find(assetId, true);
        if(asset == null) {
            throw new IllegalStateException("Asset not found: " + assetId);
        }
        Attribute<?> assetAttribute = asset.getAttribute(attributeName)
                .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeName));

        return queryDatapoints(asset.getId(), assetAttribute, datapointQuery);
    }

    public List<ValueDatapoint<?>> queryDatapoints(String assetId, Attribute<?> attribute, @NotNull AssetDatapointQuery datapointQuery) {

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Map<Integer, Object> parameters = datapointQuery.getSQLParameters(attributeRef);

        getLogger().finest("Querying datapoints for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<>() {

                    @Override
                    public List<ValueDatapoint<?>> execute(Connection connection) throws SQLException {

                        Class<?> attributeType = attribute.getTypeClass();
                        boolean isNumber = Number.class.isAssignableFrom(attributeType);
                        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);

                        String query;
                        try {
                            query = datapointQuery.getSQLQuery(getDatapointTableName(), attributeType);
                        } catch (IllegalStateException ise) {
                            getLogger().log(Level.WARNING, ise.getMessage());
                            throw ise;
                        }
                        try (PreparedStatement st = connection.prepareStatement(query)) {

                            if(!parameters.isEmpty()) {
                                for(Map.Entry<Integer, Object> param : parameters.entrySet()) {
                                    if(param.getValue() instanceof String) {
                                        st.setString(param.getKey(), param.getValue().toString());
                                    } else {
                                        st.setObject(param.getKey(), param.getValue());
                                    }
                                }
                            }

                            try (ResultSet rs = st.executeQuery()) {
                                List<ValueDatapoint<?>> result = new ArrayList<>();
                                while (rs.next()) {
                                    Object value = null;
                                    if (rs.getObject(2) != null) {
                                        if(isNumber || isBoolean) {
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
                                return result;
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

    protected void setUpsertValues(PreparedStatement st, String assetId, String attributeName, Object value, long timestamp) throws Exception {
        PGobject pgJsonValue = new PGobject();
        pgJsonValue.setType("jsonb");
        pgJsonValue.setValue(ValueUtil.asJSON(value).orElse("null"));
        st.setString(1, assetId);
        st.setString(2, attributeName);
        st.setObject(3, pgJsonValue);
        st.setObject(4, Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDateTime());
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
