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

import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.anomalyDetection.AnomalyDetectionService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;
import org.postgresql.util.PGobject;
import scala.math.BigInt;

import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openremote.container.util.MapAccess.getInteger;

public class AssetAnomalyDatapointService implements ContainerService {
    public static final int PRIORITY = AssetStorageService.PRIORITY + 100;
    public static final String OR_DATA_POINTS_MAX_AGE_DAYS = "OR_DATA_POINTS_MAX_AGE_DAYS";
    public static final int OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT = 31;
    private static final Logger LOG = Logger.getLogger(AssetAnomalyDatapointService.class.getName());

    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected ScheduledFuture<?> dataPointsPurgeScheduledFuture;
    protected int maxDatapointAgeDays = 1;

    @Override
    public int getPriority() {
        return ContainerService.super.getPriority();
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        executorService = container.getExecutorService();

        maxDatapointAgeDays = getInteger(container.getConfig(), OR_DATA_POINTS_MAX_AGE_DAYS, OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT);

        if (maxDatapointAgeDays <= 0) {
            LOG.warning(OR_DATA_POINTS_MAX_AGE_DAYS + " value is not a valid value so data points won't be auto purged");
        }

        container.getService(ManagerWebService.class).addApiSingleton(
                new AssetAnomalyDatapointResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        container.getService(AssetStorageService.class),
                        this
                )
        );
    }


    public void start(Container container) throws Exception {
        if (maxDatapointAgeDays > 0) {
            dataPointsPurgeScheduledFuture = executorService.scheduleAtFixedRate(
                    this::purgeDataPoints,
                    getFirstPurgeMillis(timerService.getNow()) + 1000*60*30, // Run half hour after default
                    Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (dataPointsPurgeScheduledFuture != null) {
            dataPointsPurgeScheduledFuture.cancel(true);
        }
    }

    public long countAnomaliesInAlarm(long alarmId){
        return persistenceService.doReturningTransaction(entityManager -> {

            String queryStr = "select count(dp) from AttributeAnomaly dp " +" where dp.alarmId = :alarm_id ";

            TypedQuery<Long> query = entityManager.createQuery(
                    queryStr,
                    Long.class);

                query.setParameter("alarm_id", alarmId);
            return query.getSingleResult();
        });
    }

    public AttributeAnomaly[] getAnommalies(String assetId, String attributeName, AssetDatapointQuery datapointQuery) {
        Asset<?> asset = assetStorageService.find(assetId, true);
        if(asset == null) {
            throw new IllegalStateException("Asset not found: " + assetId);
        }
        Attribute<?> assetAttribute = asset.getAttribute(attributeName)
                .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeName));

        return this.getAnommalies(asset.getId(), assetAttribute, datapointQuery);
    }

    public AttributeAnomaly[] getAnommalies(String assetId, Attribute<?> attribute, AssetDatapointQuery datapointQuery) {

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Map<Integer, Object> parameters = datapointQuery.getSQLParameters(attributeRef);

        getLogger().finest("Querying anomalies for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<>() {

                    @Override
                    public AttributeAnomaly[] execute(Connection connection) throws SQLException {

                        Class<?> attributeType = attribute.getType().getType();
                        boolean isNumber = Number.class.isAssignableFrom(attributeType);

                        String query;
                        try {
                            query = "select * from " + getDatapointTableName() + " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? and anomaly_type != 1 and anomaly_type != 0 order by timestamp desc";
                        } catch (IllegalStateException ise) {
                            getLogger().log(Level.WARNING, ise.getMessage());
                            throw ise;
                        }
                        try (PreparedStatement st = connection.prepareStatement(query)) {

                            if(parameters.size() > 0) {
                                for(Map.Entry<Integer, Object> param : parameters.entrySet()) {
                                    if(param.getValue() instanceof String) {
                                        st.setString(param.getKey(), param.getValue().toString());
                                    } else {
                                        st.setObject(param.getKey(), param.getValue());
                                    }
                                }
                            }

                            try (ResultSet rs = st.executeQuery()) {
                                List<AttributeAnomaly> result = new ArrayList<>();
                                while (rs.next()) {
                                    if (rs.getObject(4) != null) {
                                        String assetId = rs.getString("entity_id");
                                        String attributeName = rs.getString("attribute_name");
                                        Date timestamp = rs.getTimestamp("timestamp");
                                        int anomalyType = rs.getInt("anomaly_type");
                                        result.add(new AttributeAnomaly(assetId,attributeName,timestamp,anomalyType));
                                    }
                                }
                                return result.toArray(new AttributeAnomaly[0]);
                            }
                        }
                    }
                })
        );
    }
    public void updateValue(String assetId, String attributeName, AttributeAnomaly.AnomalyType anomalyType, LocalDateTime timestamp, Long alarmId) {
        persistenceService.doTransaction(em ->
                em.unwrap(Session.class).doWork(connection -> {

                    getLogger().finest("Storing anomaly datapoint for: id=" + assetId + ", name=" + attributeName + ", timestamp=" + timestamp + ", anomalyType=" + anomalyType);
                    PreparedStatement st;

                    try {
                        st = getUpsertPreparedStatement(connection);
                        setUpsertValues(st, assetId, attributeName, anomalyType.ordinal(), timestamp, alarmId);
                        st.executeUpdate();
                    } catch (Exception e) {
                        String msg = "Failed to insert/update data point: ";
                        getLogger().log(Level.WARNING, msg, e);
                        throw new IllegalStateException(msg, e);
                    }
                }));
    }

    protected PreparedStatement getUpsertPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO " + getDatapointTableName() + " (entity_id, attribute_name, anomaly_type, timestamp, alarm_id) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE " +
                "SET anomaly_type = excluded.anomaly_type");
    }

    protected void setUpsertValues(PreparedStatement st, String assetId, String attributeName, Integer anomalyType, LocalDateTime timestamp, Long alarmId) throws Exception {
        st.setString(1, assetId);
        st.setString(2, attributeName);
        st.setInt(3, anomalyType);
        st.setObject(4, timestamp);
        st.setLong(5, alarmId);
    }


    protected Class<AttributeAnomaly> getAttributeAnomalyClass() {
        return AttributeAnomaly.class;
    }

    protected String getDatapointTableName() {
        return AttributeAnomaly.TABLE_NAME;
    }


    protected Logger getLogger() {
        return LOG;
    }



    protected void purgeDataPoints() {
        LOG.info("Running data points purge daily task");

        try {
            // Get list of attributes that have custom durations
            List<Asset<?>> assets = assetStorageService.findAll(
                    new AssetQuery()
                            .attributes(
                                    new AttributePredicate().meta(
                                            new NameValuePredicate(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, null)
                                    )));

            List<Pair<String, Attribute<?>>> attributes = assets.stream()
                    .map(asset -> asset
                            .getAttributes().stream()
                            .filter(assetAttribute -> assetAttribute.hasMeta(MetaItemType.DATA_POINTS_MAX_AGE_DAYS))
                            .map(assetAttribute -> new Pair<String, Attribute<?>>(asset.getId(), assetAttribute))
                            .collect(toList()))
                    .flatMap(List::stream)
                    .collect(toList());

            // Purge data points not in the above list using default duration
            LOG.fine("Purging data points of attributes that use default max age days of " + maxDatapointAgeDays);

            persistenceService.doTransaction(em -> em.createQuery(
                    "delete from AttributeAnomaly dp " +
                            "where dp.timestamp < :dt" + buildWhereClause(attributes, true)
            ).setParameter("dt", java.sql.Date.from(timerService.getNow().truncatedTo(DAYS).minus(maxDatapointAgeDays, DAYS))).executeUpdate());

            if (!attributes.isEmpty()) {
                // Purge data points that have specific age constraints
                Map<Integer, List<Pair<String, Attribute<?>>>> ageAttributeRefMap = attributes.stream()
                        .collect(groupingBy(attributeRef ->
                                attributeRef.value
                                        .getMetaValue(MetaItemType.DATA_POINTS_MAX_AGE_DAYS)
                                        .orElse(maxDatapointAgeDays)));

                ageAttributeRefMap.forEach((age, attrs) -> {
                    LOG.fine("Purging data points of " + attrs.size() + " attributes that use a max age of " + age);

                    try {
                        persistenceService.doTransaction(em -> em.createQuery(
                                "delete from AttributeAnomaly dp " +
                                        "where dp.timestamp < :dt" + buildWhereClause(attrs, false)
                        ).setParameter("dt", java.sql.Date.from(timerService.getNow().truncatedTo(DAYS).minus(age, DAYS))).executeUpdate());
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "An error occurred whilst deleting data points, this should not happen", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }
    }

    protected String buildWhereClause(List<Pair<String, Attribute<?>>> attributes, boolean negate) {

        if (attributes.isEmpty()) {
            return "";
        }

        String whereStr = attributes.stream()
                .map(attributeRef -> "('" + attributeRef.key + "','" + attributeRef.value.getName() + "')")
                .collect(Collectors.joining(","));

        return " and (dp.assetId, dp.attributeName) " + (negate ? "not " : "") + "in (" + whereStr + ")";
    }
    protected long getFirstPurgeMillis(Instant currentTime) {
        // Schedule purge at approximately 3AM daily
        return ChronoUnit.MILLIS.between(
                currentTime,
                currentTime.truncatedTo(DAYS).plus(27, ChronoUnit.HOURS));
    }
}
