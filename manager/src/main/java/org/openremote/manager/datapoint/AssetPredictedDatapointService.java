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
package org.openremote.manager.datapoint;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.agent.protocol.ProtocolPredictedAssetService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.Values;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;

public class AssetPredictedDatapointService implements ContainerService, ProtocolPredictedAssetService {

    public static final int PRIORITY = AssetStorageService.PRIORITY + 300;
    private static final Logger LOG = Logger.getLogger(AssetPredictedDatapointService.class.getName());

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

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new AssetPredictedDatapointResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                container.getService(AssetStorageService.class),
                this
            )
        );
    }

    @Override
    public void start(Container container) throws Exception {
        dataPointsPurgeScheduledFuture = executorService.scheduleAtFixedRate(
            this::purgeDataPoints,
            getFirstRunMillis(timerService.getNow()),
            Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop(Container container) throws Exception {
        if (dataPointsPurgeScheduledFuture != null) {
            dataPointsPurgeScheduledFuture.cancel(true);
        }
    }

    public long getDatapointsCount() {
        return getDatapointsCount(null);
    }

    public long getDatapointsCount(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> {

            String queryStr = attributeRef == null ?
                "select count(dp) from AssetPredictedDatapoint dp" :
                "select count(dp) from AssetPredictedDatapoint dp where dp.assetId = :assetId and dp.attributeName = :attributeName";

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
                                               String truncate,
                                               String interval,
                                               LocalDateTime fromTimestamp,
                                               LocalDateTime toTimestamp) {
        String assetId = attributeRef.getId();
        String attributeName = attributeRef.getName();

        Asset<?> asset = assetStorageService.find(assetId);
        if (asset == null) {
            throw new IllegalStateException("Asset not found: " + assetId);
        }

        Attribute<?> attribute = asset.getAttribute(attributeName)
            .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeName));

        LOG.finer("Getting predicted datapoints for: " + attributeName);

        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<ValueDatapoint<?>[]>() {
                @Override
                public ValueDatapoint<?>[] execute(Connection connection) throws SQLException {


                    Class<?> attributeType = attribute.getType().getType();
                    boolean isNumber = Number.class.isAssignableFrom(attributeType);
                    boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
                    StringBuilder query = new StringBuilder();
                    boolean downsample = isNumber || isBoolean;

                    if (downsample) {
                        // TODO: Change this to use something like this max min decimation algorithm https://knowledge.ni.com/KnowledgeArticleDetails?id=kA00Z0000019YLKSA2&l=en-GB)
                        query.append("select PERIOD.TS as X, coalesce(AVG_VALUE, null) as Y " +
                            " from ( " +
                            "       select date_trunc(?, GS)::timestamp TS " +
                            "       from generate_series(?, ?, ?) GS " +
                            "       ) PERIOD " +
                            "  left join ( " +
                            "       select " +
                            "           date_trunc(?, TIMESTAMP)::timestamp as TS, ");

                        if (isNumber) {
                            query.append(" AVG(VALUE::text::numeric) as AVG_VALUE ");
                        } else {
                            query.append(" AVG(case when VALUE::text::boolean is true then 1 else 0 end) as AVG_VALUE ");
                        }

                        query.append(" from ASSET_PREDICTED_DATAPOINT " +
                            "         where " +
                            "           TIMESTAMP >= ? " +
                            "           and " +
                            "           TIMESTAMP <= ? " +
                            "           and " +
                            "           ENTITY_ID = ? and ATTRIBUTE_NAME = ? " +
                            "         group by TS " +
                            "  ) DP on DP.TS >= PERIOD.TS and DP.TS < PERIOD.TS + ? " +
                            " order by PERIOD.TS asc "
                        );

                    } else {
                        query.append("select distinct TIMESTAMP AS X, value AS Y from ASSET_PREDICTED_DATAPOINT " +
                            "where " +
                            "TIMESTAMP >= ? " +
                            "and " +
                            "TIMESTAMP < ? " +
                            "and " +
                            "ENTITY_ID = ? and ATTRIBUTE_NAME = ? "
                        );
                    }

                    try (PreparedStatement st = connection.prepareStatement(query.toString())) {

                        if (downsample) {
                            st.setString(1, truncate);
                            st.setObject(2, fromTimestamp);
                            st.setObject(3, toTimestamp);
                            st.setObject(4, new PGInterval(interval));
                            st.setString(5, truncate);
                            st.setObject(6, fromTimestamp);
                            st.setObject(7, toTimestamp);
                            st.setString(8, assetId);
                            st.setString(9, attributeName);
                            st.setObject(10, new PGInterval(interval));
                        } else {
                            st.setObject(1, fromTimestamp);
                            st.setObject(2, toTimestamp);
                            st.setString(3, assetId);
                            st.setString(4, attributeName);
                        }

                        try (ResultSet rs = st.executeQuery()) {
                            List<ValueDatapoint<?>> result = new ArrayList<>();
                            while (rs.next()) {
                                Object value = rs.getObject(2) != null ? Values.getValueCoerced(rs.getObject(2), attributeType).orElse(null) : null;
                                result.add(new ValueDatapoint<>(rs.getTimestamp(1).getTime(), value));
                            }
                            return result.toArray(new ValueDatapoint<?>[0]);
                        }
                    }
                }
            })
        );
    }

    public ValueDatapoint<?>[] getValueDatapoints(AttributeRef attributeRef,
                                                  DatapointInterval datapointInterval,
                                                  LocalDateTime fromTimestamp,
                                                  LocalDateTime toTimestamp) {
        String truncateX;
        String interval;

        switch (datapointInterval) {
            case MINUTE:
                truncateX = "minute";
                interval = "1 minute";
                break;
            case HOUR:
                truncateX = "hour";
                interval = "1 hour";
                break;
            case DAY:
                truncateX = "day";
                interval = "1 day";
                break;
            case WEEK:
                truncateX = "day";
                interval = "7 day";
                break;
            case MONTH:
                truncateX = "day";
                interval = "1 month";
                break;
            case YEAR:
                truncateX = "month";
                interval = "1 year";
                break;
            default:
                throw new IllegalArgumentException("Can't handle interval: " + datapointInterval);
        }

        return getValueDatapoints(attributeRef, truncateX, interval, fromTimestamp, toTimestamp);
    }

    public void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp) {
        updateValue(attributeRef.getId(), attributeRef.getName(), value, timestamp);
    }

    public void updateValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) {
        persistenceService.doTransaction(em -> upsertValue(em, assetId, attributeName, value, timestamp));
    }

    protected void upsertValue(EntityManager em, String assetId, String attributeName, Object value, LocalDateTime timestamp) {
        PGobject pgJsonValue = new PGobject();
        pgJsonValue.setType("jsonb");
        try {
            pgJsonValue.setValue(Values.asJSON(value).orElse("null"));
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to store predicted data point: " + new AttributeRef(assetId, attributeName), e);
        }

        em.unwrap(Session.class).doWork(connection -> {
            PreparedStatement st = connection.prepareStatement("INSERT INTO asset_predicted_datapoint (entity_id, attribute_name, value, timestamp) \n" +
                "VALUES (?, ?, ?, ?)\n" +
                "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE \n" +
                "  SET value = excluded.value");

            st.setString(1, assetId);
            st.setString(2, attributeName);
            st.setObject(3, pgJsonValue);
            st.setObject(4, timestamp);
            st.executeUpdate();
        });
    }

    protected void purgeDataPoints() {
        LOG.info("Starting data points purge daily task");

        try {
            // Purge data points not in the above list using default duration
            LOG.finer("Purging predicted data points older than 1 day");

            persistenceService.doTransaction(em -> em.createQuery(
                "delete from AssetPredictedDatapoint dp " +
                    "where dp.timestamp < :dt"
            ).setParameter("dt", Date.from(timerService.getNow().truncatedTo(DAYS).minus(1, DAYS))).executeUpdate());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }

        LOG.info("Finished data points purge daily task");
    }

    protected long getFirstRunMillis(Instant currentTime) {
        // Schedule purge at approximately 2AM daily
        return ChronoUnit.MILLIS.between(
            currentTime,
            currentTime.truncatedTo(DAYS).plus(26, ChronoUnit.HOURS));
    }
}
