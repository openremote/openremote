package org.openremote.manager.datapoint;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaItemType;
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
import java.util.Map;
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
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS;

/**
 * Store and retrieve datapoints for asset attributes and periodically purge data points based on
 * {@link MetaItemType#DATA_POINTS_MAX_AGE_DAYS} {@link org.openremote.model.attribute.MetaItem}
 * and {@link #DATA_POINTS_MAX_AGE_DAYS} setting; storage duration defaults to {@value #DATA_POINTS_MAX_AGE_DAYS_DEFAULT}
 * days.
 */
public class AssetDatapointService implements ContainerService, AssetUpdateProcessor {

    public static final String DATA_POINTS_MAX_AGE_DAYS = "DATA_POINTS_MAX_AGE_DAYS";
    public static final int DATA_POINTS_MAX_AGE_DAYS_DEFAULT = 31;
    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());
    public static final int PRIORITY = AssetStorageService.PRIORITY + 100;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected ScheduledExecutorService executorService;
    protected int maxDatapointAgeDays;
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
                new AssetDatapointResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        container.getService(AssetStorageService.class),
                        this
                )
        );

        maxDatapointAgeDays = getInteger(container.getConfig(), DATA_POINTS_MAX_AGE_DAYS, DATA_POINTS_MAX_AGE_DAYS_DEFAULT);

        if (maxDatapointAgeDays <= 0) {
            LOG.warning(DATA_POINTS_MAX_AGE_DAYS + " value is not a valid value so data points won't be auto purged");
        }
    }

    @Override
    public void start(Container container) throws Exception {
        if (maxDatapointAgeDays > 0) {
            dataPointsPurgeScheduledFuture = executorService.scheduleAtFixedRate(
                this::purgeDataPoints,
                getFirstRunMillis(timerService.getNow()),
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

    protected static boolean attributeIsStoreDatapoint(Attribute<?> attribute) {
        return attribute.getMetaValue(STORE_DATA_POINTS).orElse(attribute.hasMeta(MetaItemType.AGENT_LINK));
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset<?> asset,
                                      Attribute<?> attribute,
                                      Source source) throws AssetProcessingException {

        if (attributeIsStoreDatapoint(attribute) && attribute.getValue().isPresent()) { // Don't store datapoints with null value

            // Perform upsert on datapoint (datapoint isn't immutable then really and tied to postgresql but prevents entire attribute event from failing)
            LOG.finest("Storing datapoint for: " + attribute);

            PGobject pgJsonValue = new PGobject();
            pgJsonValue.setType("jsonb");
            try {
                pgJsonValue.setValue(Values.asJSON(attribute.getValue().orElse(null)).orElse("null"));
            } catch (SQLException e) {
                throw new AssetProcessingException(AssetProcessingException.Reason.STATE_STORAGE_FAILED, "Failed to insert or update asset data point for attribute: " + attribute);
            }

            em.unwrap(Session.class).doWork(connection -> {
                PreparedStatement st = connection.prepareStatement("INSERT INTO asset_datapoint (entity_id, attribute_name, value, timestamp) \n" +
                    "VALUES (?, ?, ?, ?)\n" +
                    "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE \n" +
                    "  SET value = excluded.value");

                st.setString(1, asset.getId());
                st.setString(2, attribute.getName());
                st.setObject(3, pgJsonValue);
                st.setTimestamp(4, new java.sql.Timestamp(attribute.getTimestamp().orElseGet(timerService::getCurrentTimeMillis)));
                st.executeUpdate();
            });
        }
        return false;
    }

    public List<AssetDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                        "select dp from AssetDatapoint dp " +
                                "where dp.assetId = :assetId " +
                                "and dp.attributeName = :attributeName " +
                                "order by dp.timestamp desc",
                        AssetDatapoint.class)
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
                    "select count(dp) from AssetDatapoint dp" :
                    "select count(dp) from AssetDatapoint dp where dp.assetId = :assetId and dp.attributeName = :attributeName";

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
                                               LocalDateTime fromTimestamp,
                                               LocalDateTime toTimestamp) {

        Asset<?> asset = assetStorageService.find(attributeRef.getId());
        if (asset == null) {
            throw new IllegalStateException("Asset not found: " + attributeRef.getId());
        }

        Attribute<?> assetAttribute = asset.getAttribute(attributeRef.getName())
            .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeRef.getName()));

        return getValueDatapoints(asset.getId(), assetAttribute, datapointInterval, fromTimestamp, toTimestamp);
    }

    public ValueDatapoint<?>[] getValueDatapoints(String assetId, Attribute<?> attribute,
                                               DatapointInterval datapointInterval,
                                               LocalDateTime fromTimestamp,
                                               LocalDateTime toTimestamp) {

        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());

        LOG.finer("Getting datapoints for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<ValueDatapoint<?>[]>() {
                    @Override
                    public ValueDatapoint<?>[] execute(Connection connection) throws SQLException {

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

                            query.append(" from ASSET_DATAPOINT " +
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
                            query.append("select distinct TIMESTAMP AS X, value AS Y from ASSET_DATAPOINT " +
                                "where " +
                                "TIMESTAMP >= ?" +
                                "and " +
                                "TIMESTAMP <= ? " +
                                "and " +
                                "ENTITY_ID = ? and ATTRIBUTE_NAME = ? "
                            );
                        }

                        try (PreparedStatement st = connection.prepareStatement(query.toString())) {

                            if (downsample) {
                                st.setString(1, truncateX);
                                st.setObject(2, fromTimestamp);
                                st.setObject(3, toTimestamp);
                                st.setObject(4, new PGInterval(interval));
                                st.setString(5, truncateX);
                                st.setObject(6, fromTimestamp);
                                st.setObject(7, toTimestamp);
                                st.setString(8, attributeRef.getId());
                                st.setString(9, attributeRef.getName());
                                st.setObject(10, new PGInterval(interval));
                            } else {
                                st.setObject(1, fromTimestamp);
                                st.setObject(2, toTimestamp);
                                st.setString(3, attributeRef.getId());
                                st.setString(4, attributeRef.getName());
                            }

                            try (ResultSet rs = st.executeQuery()) {
                                List<ValueDatapoint<?>> result = new ArrayList<>();
                                while (rs.next()) {
                                    Object value = rs.getObject(2) != null ? Values.getValueCoerced(rs.getObject(2), Double.class).orElse(null) : null;
                                    result.add(new ValueDatapoint<>(rs.getTimestamp(1).getTime(), value));
                                }
                                return result.toArray(new ValueDatapoint<?>[0]);
                            }
                        }
                    }
                })
        );
    }

    protected void purgeDataPoints() {
        LOG.info("Starting data points purge daily task");

        try {
            // Get list of attributes that have custom durations
            List<Asset<?>> assets = assetStorageService.findAll(
                new AssetQuery()
                    .attributes(
                        new AttributePredicate().meta(
                            new NameValuePredicate(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, null)
                        ))
                    .select(AssetQuery.Select.selectExcludePathAndParentInfo()));

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
                "delete from AssetDatapoint dp " +
                    "where dp.timestamp < :dt" + buildWhereClause(attributes, true)
            ).setParameter("dt", Date.from(timerService.getNow().truncatedTo(DAYS).minus(maxDatapointAgeDays, DAYS))).executeUpdate());

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
                            "delete from AssetDatapoint dp " +
                                "where dp.timestamp < :dt" + buildWhereClause(attrs, false)
                        ).setParameter("dt", Date.from(timerService.getNow().truncatedTo(DAYS).minus(age, DAYS))).executeUpdate());
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "An error occurred whilst deleting data points, this should not happen", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }

        LOG.info("Finished data points purge daily task");
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

    protected long getFirstRunMillis(Instant currentTime) {
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
