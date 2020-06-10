package org.openremote.manager.datapoint;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.MetaPredicate;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import org.postgresql.util.PGInterval;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Store and retrieve datapoints for asset attributes and periodically purge data points based on
 * {@link MetaItemType#DATA_POINTS_MAX_AGE_DAYS} {@link org.openremote.model.attribute.MetaItem}
 * and {@link #DATA_POINTS_MAX_AGE_DAYS} setting; storage duration defaults to {@value #DATA_POINTS_MAX_AGE_DAYS_DEFAULT}
 * days.
 */
public class AssetDatapointService implements ContainerService, AssetUpdateProcessor {

    public static final String DATA_POINTS_MAX_AGE_DAYS = "DATA_POINTS_MAX_AGE_DAYS";
    public static final String DATA_POINTS_MAX_AGE_DAYS_DEFAULT = "31";
    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());
    public static final int PRIORITY = AssetStorageService.PRIORITY + 100;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected ManagerExecutorService managerExecutorService;
    protected int maxDatapointAgeDays;
    protected ScheduledFuture dataPointsPurgeScheduledFuture;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        managerExecutorService = container.getService(ManagerExecutorService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
                new AssetDatapointResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        container.getService(AssetStorageService.class),
                        this
                )
        );

        maxDatapointAgeDays = Integer.parseInt(
                container.getConfig().getOrDefault(DATA_POINTS_MAX_AGE_DAYS, DATA_POINTS_MAX_AGE_DAYS_DEFAULT)
        );

        if (maxDatapointAgeDays <= 0) {
            LOG.warning(DATA_POINTS_MAX_AGE_DAYS + " value is not a valid value so data points won't be auto purged");
        }
    }

    @Override
    public void start(Container container) throws Exception {
        if (maxDatapointAgeDays > 0) {
            dataPointsPurgeScheduledFuture = managerExecutorService.scheduleAtFixedRate(
                    this::purgeDataPoints,

                getFirstRunMillis(timerService.getNow()),
                Duration.ofDays(1).toMillis());
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (dataPointsPurgeScheduledFuture != null) {
            dataPointsPurgeScheduledFuture.cancel(true);
        }
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset asset,
                                      AssetAttribute attribute,
                                      Source source) throws AssetProcessingException {
        if (attribute.isStoreDatapoints()
                && attribute.getStateEvent().isPresent()
                && attribute.getStateEvent().get().getValue().isPresent()) { // Don't store datapoints with null value
            LOG.finest("Storing datapoint for: " + attribute);
            AssetDatapoint assetDatapoint = new AssetDatapoint(attribute.getStateEvent().get());
            em.persist(assetDatapoint);
        }
        return false;
    }

    public List<AssetDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                        "select dp from AssetDatapoint dp " +
                                "where dp.entityId = :assetId " +
                                "and dp.attributeName = :attributeName " +
                                "order by dp.timestamp desc",
                        AssetDatapoint.class)
                        .setParameter("assetId", attributeRef.getEntityId())
                        .setParameter("attributeName", attributeRef.getAttributeName())
                        .getResultList());
    }


    public long getDatapointsCount() {
        return getDatapointsCount(null);
    }

    public long getDatapointsCount(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> {

            String queryStr = attributeRef == null ?
                    "select count(dp) from AssetDatapoint dp" :
                    "select count(dp) from AssetDatapoint dp where dp.entityId = :assetId and dp.attributeName = :attributeName";

            TypedQuery<Long> query = entityManager.createQuery(
                    queryStr,
                    Long.class);

            if (attributeRef != null) {
                query
                    .setParameter("assetId", attributeRef.getEntityId())
                    .setParameter("attributeName", attributeRef.getAttributeName());
            }

            return query.getSingleResult();
        });
    }

    public ValueDatapoint[] getValueDatapoints(AttributeRef attributeRef,
                                               DatapointInterval datapointInterval,
                                               long fromTimestamp,
                                               long toTimestamp) {

        Asset asset = assetStorageService.find(attributeRef.getEntityId());
        if (asset == null) {
            throw new IllegalStateException("Asset not found: " + attributeRef.getEntityId());
        }
        AssetAttribute assetAttribute = asset.getAttribute(attributeRef.getAttributeName())
            .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeRef.getAttributeName()));

        return getValueDatapoints(assetAttribute, datapointInterval, fromTimestamp, toTimestamp);
    }

    public ValueDatapoint[] getValueDatapoints(AssetAttribute attribute,
                                               DatapointInterval datapointInterval,
                                               long fromTimestamp,
                                               long toTimestamp) {

        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        ValueType attributeValueType = attribute.getTypeOrThrow().getValueType();

        LOG.fine("Getting datapoints for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->
                entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<ValueDatapoint[]>() {
                    @Override
                    public ValueDatapoint[] execute(Connection connection) throws SQLException {

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

                        StringBuilder query = new StringBuilder();
                        boolean downsample = attributeValueType == ValueType.NUMBER || attributeValueType == ValueType.BOOLEAN;

                        if (downsample) {

                            // TODO: Change this to use something like this max min decimation algorithm https://knowledge.ni.com/KnowledgeArticleDetails?id=kA00Z0000019YLKSA2&l=en-GB)
                            query.append("select TS as X, coalesce(AVG_VALUE, null) as Y " +
                                " from ( " +
                                "       select date_trunc(?, GS)::timestamp TS " +
                                "       from generate_series(to_timestamp(?), to_timestamp(?), ?) GS " +
                                "       ) TS " +
                                "  left join ( " +
                                "       select " +
                                "           date_trunc(?, TIMESTAMP)::timestamp as TS, ");

                            if (attributeValueType == ValueType.NUMBER) {
                                query.append(" AVG(VALUE::text::numeric) as AVG_VALUE ");
                            } else {
                                query.append(" AVG(case when VALUE::text::boolean is true then 1 else 0 end) as AVG_VALUE ");
                            }

                            query.append(" from ASSET_DATAPOINT " +
                                "         where " +
                                "           TIMESTAMP >= to_timestamp(?) " +
                                "           and " +
                                "           TIMESTAMP <= to_timestamp(?) " +
                                "           and " +
                                "           ENTITY_ID = ? and ATTRIBUTE_NAME = ? " +
                                "         group by TS " +
                                "  ) DP using (TS) " +
                                " order by TS asc "
                            );
                        } else {
                            query.append("select distinct TIMESTAMP AS X, value AS Y from ASSET_DATAPOINT " +
                                "where " +
                                "TIMESTAMP >= to_timestamp(?)" +
                                "and " +
                                "TIMESTAMP <= to_timestamp(?) " +
                                "and " +
                                "ENTITY_ID = ? and ATTRIBUTE_NAME = ? "
                            );
                        }

                        try (PreparedStatement st = connection.prepareStatement(query.toString())) {

                            long fromTimestampSeconds = fromTimestamp / 1000;
                            long toTimestampSeconds = toTimestamp / 1000;
                            if (downsample) {
                                st.setString(1, truncateX);
                                st.setLong(2, fromTimestampSeconds);
                                st.setLong(3, toTimestampSeconds);
                                st.setObject(4, new PGInterval(interval));
                                st.setString(5, truncateX);
                                st.setLong(6, fromTimestampSeconds);
                                st.setLong(7, toTimestampSeconds);
                                st.setString(8, attributeRef.getEntityId());
                                st.setString(9, attributeRef.getAttributeName());
                            } else {
                                st.setLong(1, fromTimestampSeconds);
                                st.setLong(2, toTimestampSeconds);
                                st.setString(3, attributeRef.getEntityId());
                                st.setString(4, attributeRef.getAttributeName());
                            }

                            try (ResultSet rs = st.executeQuery()) {
                                List<ValueDatapoint<?>> result = new ArrayList<>();
                                while (rs.next()) {
                                    Value value = rs.getObject(2) != null ? Values.parseOrNull(rs.getString(2)) : null;
                                    result.add(new ValueDatapoint<>(rs.getTimestamp(1).getTime(), value));
                                }
                                return result.toArray(new ValueDatapoint[result.size()]);
                            }
                        }
                    }
                })
        );
    }

    protected void purgeDataPoints() {
        LOG.info("Starting data points purge daily task");

        // Get list of attributes that have custom durations
        List<Asset> assets = assetStorageService.findAll(
                new AssetQuery()
                        .attributeMeta(
                                new MetaPredicate(MetaItemType.DATA_POINTS_MAX_AGE_DAYS),
                                new MetaPredicate(MetaItemType.STORE_DATA_POINTS))
                        .select(AssetQuery.Select.selectExcludePathAndParentInfo()));

        List<AssetAttribute> attributes = assets.stream()
                .map(asset -> asset
                        .getAttributesStream()
                        .filter(assetAttribute ->
                                assetAttribute.isStoreDatapoints()
                                        && assetAttribute.hasMetaItem(MetaItemType.DATA_POINTS_MAX_AGE_DAYS))
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
            Map<Integer, List<AssetAttribute>> ageAttributeRefMap = attributes.stream()
                    .collect(groupingBy(attribute ->
                            attribute
                                    .getMetaItem(MetaItemType.DATA_POINTS_MAX_AGE_DAYS)
                                    .flatMap(metaItem ->
                                            Values.getIntegerCoerced(metaItem.getValue().orElse(null)))
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

        LOG.info("Finished data points purge daily task");
    }

    protected String buildWhereClause(List<AssetAttribute> attributes, boolean negate) {

        if (attributes.isEmpty()) {
            return "";
        }

        String whereStr = attributes.stream()
                .map(assetAttribute -> {
                    AttributeRef attributeRef = assetAttribute.getReferenceOrThrow();
                    return "('" + attributeRef.getEntityId() + "','" + attributeRef.getAttributeName() + "')";
                })
                .collect(Collectors.joining(","));

        return " and (dp.entityId, dp.attributeName) " + (negate ? "not " : "") + "in (" + whereStr + ")";
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
