package org.openremote.manager.datapoint;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeWriteFailure;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaItemType;

import javax.persistence.EntityManager;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
public class AssetDatapointService extends AbstractDatapointService<AssetDatapoint> implements AssetUpdateProcessor {

    public static final String DATA_POINTS_MAX_AGE_DAYS = "DATA_POINTS_MAX_AGE_DAYS";
    public static final int DATA_POINTS_MAX_AGE_DAYS_DEFAULT = 31;
    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());
    protected int maxDatapointAgeDays;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

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
                getFirstPurgeMillis(timerService.getNow()),
                Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
            );
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
            try {
                upsertValue(asset.getId(), attribute.getName(), attribute.getValue().orElse(null), LocalDateTime.ofInstant(Instant.ofEpochMilli(attribute.getTimestamp().orElseGet(timerService::getCurrentTimeMillis)), ZoneId.systemDefault()));
            } catch (Exception e) {
                throw new AssetProcessingException(AttributeWriteFailure.STATE_STORAGE_FAILED, "Failed to insert or update asset data point for attribute: " + attribute, e);
            }
        }
        return false;
    }

    @Override
    protected Class<AssetDatapoint> getDatapointClass() {
        return AssetDatapoint.class;
    }

    @Override
    protected String getDatapointTableName() {
        return AssetDatapoint.TABLE_NAME;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
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
}
