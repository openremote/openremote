package org.openremote.manager.datapoint;

import org.hibernate.exception.GenericJDBCException;
import org.openremote.agent.protocol.ProtocolDatapointService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.OutdatedAttributeEvent;
import org.openremote.model.datapoint.DatapointQueryTooLargeException;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeWriteFailure;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.MetaItemType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openremote.model.util.MapAccess.getInteger;
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS;

/**
 * Store and retrieve datapoints for asset attributes and periodically purge data points based on
 * {@link MetaItemType#DATA_POINTS_MAX_AGE_DAYS} {@link org.openremote.model.attribute.MetaItem}
 * and {@link #OR_DATA_POINTS_MAX_AGE_DAYS} setting; storage duration defaults to {@value #OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT}
 * days.
 */
public class AssetDatapointService extends AbstractDatapointService<AssetDatapoint> implements ProtocolDatapointService {

    public static final String OR_DATA_POINTS_MAX_AGE_DAYS = "OR_DATA_POINTS_MAX_AGE_DAYS";
    public static final int OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT = 31;
    public static final String OR_DATA_POINTS_EXPORT_LIMIT = "OR_DATA_POINTS_EXPORT_LIMIT";
    public static final int OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT = 1000000;
    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());
    protected static final String EXPORT_STORAGE_DIR_NAME = "datapoint";
    protected int maxDatapointAgeDays;
    protected int datapointExportLimit;
    protected Path exportPath;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        container.getService(ManagerWebService.class).addApiSingleton(
            new AssetDatapointResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                container.getService(AssetStorageService.class),
                this
            )
        );

        maxDatapointAgeDays = getInteger(container.getConfig(), OR_DATA_POINTS_MAX_AGE_DAYS, OR_DATA_POINTS_MAX_AGE_DAYS_DEFAULT);

        if (maxDatapointAgeDays <= 0) {
            LOG.warning(OR_DATA_POINTS_MAX_AGE_DAYS + " value is not a valid value so data points won't be auto purged");
        } else {
            LOG.log(Level.INFO, "Data point purge interval days = " + maxDatapointAgeDays);
        }

        datapointExportLimit = getInteger(container.getConfig(), OR_DATA_POINTS_EXPORT_LIMIT, OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT);

        if (datapointExportLimit <= 0) {
            LOG.warning(OR_DATA_POINTS_EXPORT_LIMIT + " value is not a valid value so the export data points won't be limited");
        } else {
            LOG.log(Level.INFO, "Data point export limit = " + datapointExportLimit);
        }

        Path storageDir = persistenceService.getStorageDir();
        exportPath = storageDir.resolve(EXPORT_STORAGE_DIR_NAME);
        // Ensure export dir exists and is writable
        Files.createDirectories(exportPath);
        if (!exportPath.toFile().setWritable(true, false)) {
            LOG.log(Level.WARNING, "Failed to set export dir write flag; data export may not work");
        }
    }

    @Override
    public void start(Container container) throws Exception {
        if (maxDatapointAgeDays > 0) {
            dataPointsPurgeScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::purgeDataPoints,
                getFirstPurgeMillis(timerService.getNow()),
                Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
            );
        }

        ClientEventService clientEventService = container.getService(ClientEventService.class);
        clientEventService.addSubscription(AttributeEvent.class, null, this::onAttributeEvent);
        clientEventService.addSubscription(OutdatedAttributeEvent.class, null, this::onOutdatedAttributeEvent);
    }

    public static boolean attributeIsStoreDatapoint(MetaHolder attributeInfo) {
        return attributeInfo.getMetaValue(STORE_DATA_POINTS).orElse(attributeInfo.hasMeta(MetaItemType.AGENT_LINK));
    }

    public void onAttributeEvent(AttributeEvent attributeEvent) {
        if (attributeIsStoreDatapoint(attributeEvent) && attributeEvent.getValue().isPresent()) { // Don't store datapoints with null value
            try {
                upsertValue(attributeEvent.getId(), attributeEvent.getName(), attributeEvent.getValue().orElse(null), Instant.ofEpochMilli(attributeEvent.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime());
            } catch (Exception e) {
                throw new AssetProcessingException(AttributeWriteFailure.STATE_STORAGE_FAILED, "Failed to insert or update asset data point for attribute: " + attributeEvent, e);
            }
        }
    }

    public void onOutdatedAttributeEvent(OutdatedAttributeEvent outdatedAttributeEvent) {
        // Store the outdated event for historical analysis
        onAttributeEvent(outdatedAttributeEvent.getEvent());
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

            // Calculate the default purge cutoff date
            Instant defaultCutoff = timerService.getNow().truncatedTo(DAYS).minus(maxDatapointAgeDays, DAYS);

            // Try to use TimescaleDB drop_chunks for efficient purging of whole chunks
            // This avoids decompression overhead on compressed/hypercore chunks
            boolean usedDropChunks = tryDropChunks(defaultCutoff, attributes);

            if (!usedDropChunks) {
                // Fallback to DELETE for non-TimescaleDB or if drop_chunks failed
                LOG.fine("Purging data points of attributes that use default max age days of " + maxDatapointAgeDays + " using DELETE");
                persistenceService.doTransaction(em -> em.createQuery(
                    "delete from AssetDatapoint dp " +
                        "where dp.timestamp < :dt" + buildWhereClause(attributes, true)
                ).setParameter("dt", Date.from(defaultCutoff)).executeUpdate());
            }

            if (!attributes.isEmpty()) {
                // Purge data points that have specific age constraints
                // These must use DELETE as they are attribute-specific and cannot use drop_chunks
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
        } catch (GenericJDBCException e) {
            if (e.getSQLException().getSQLState().equals("53400") && e.getSQLException().getMessage().contains("tuple decompression limit exceeded by operation")) {
                LOG.log(Level.SEVERE, "Failed to run data points purge", e);
            } else {
                LOG.log(Level.WARNING, "Failed to run data points purge", e);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }

        // Purge old exports
        try {
            long oneDayMillis = 24*60*60*1000;
            File[] obsoleteExports = exportPath.toFile()
                .listFiles(file ->
                    file.isFile()
                        && file.getName().endsWith("csv")
                        && file.lastModified() < timerService.getCurrentTimeMillis() - oneDayMillis
                );

            if (obsoleteExports != null) {
                Arrays.stream(obsoleteExports)
                    .forEach(file -> {
                        boolean success = false;
                        try {
                            success = file.delete();
                        } catch (SecurityException e) {
                            LOG.log(Level.WARNING, "Cannot access the export file to delete it", e);
                        }
                        if (!success) {
                            LOG.log(Level.WARNING, "Failed to delete obsolete export '" + file.getName() + "'");
                        }
                    });
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to purge old exports", e);
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

    /**
     * Attempts to use TimescaleDB's drop_chunks() function for efficient purging of whole chunks.
     * This avoids the decompression overhead that occurs when using DELETE on compressed/hypercore chunks.
     * <p>
     * The strategy is:
     * 1. Query chunk metadata to find chunks that are entirely before the cutoff date
     * 2. Use drop_chunks() to remove those chunks instantly (no decompression needed)
     * 3. For the partial chunk spanning the cutoff, use DELETE only for that chunk's data
     * <p>
     * If attributes with custom retention exist, we exclude them from drop_chunks and handle them
     * separately with DELETE since drop_chunks cannot filter by entity_id/attribute_name.
     *
     * @param defaultCutoff The cutoff instant - data older than this should be purged
     * @param customRetentionAttributes Attributes with custom DATA_POINTS_MAX_AGE_DAYS that should be excluded
     * @return true if drop_chunks was used successfully, false if fallback to DELETE is needed
     */
    protected boolean tryDropChunks(Instant defaultCutoff, List<Pair<String, Attribute<?>>> customRetentionAttributes) {
        try {
            return persistenceService.doReturningTransaction(em -> {
                // First check if this is a TimescaleDB hypertable
                String checkHypertableQuery = 
                    "SELECT COUNT(*) FROM timescaledb_information.hypertables " +
                    "WHERE hypertable_name = 'asset_datapoint'";
                
                Number hypertableCount = (Number) em.createNativeQuery(checkHypertableQuery).getSingleResult();
                if (hypertableCount.intValue() == 0) {
                    LOG.fine("asset_datapoint is not a TimescaleDB hypertable, using DELETE fallback");
                    return false;
                }

                // Get chunk information to understand the time ranges
                // We need to find the oldest chunk that ends AFTER the cutoff (partial chunk)
                String chunkInfoQuery = 
                    "SELECT chunk_name, range_start, range_end " +
                    "FROM timescaledb_information.chunks " +
                    "WHERE hypertable_name = 'asset_datapoint' " +
                    "ORDER BY range_start ASC";
                
                @SuppressWarnings("unchecked")
                List<Object[]> chunks = em.createNativeQuery(chunkInfoQuery).getResultList();
                
                if (chunks.isEmpty()) {
                    LOG.fine("No chunks found for asset_datapoint hypertable");
                    return true; // Nothing to purge
                }

                Timestamp cutoffTimestamp = Timestamp.from(defaultCutoff);
                Instant oldestChunkRangeEnd = null;
                int chunksToDropCount = 0;

                // Find chunks that are entirely before the cutoff
                for (Object[] chunk : chunks) {
                    // TimescaleDB returns OffsetDateTime for range_start/range_end
                    Instant rangeEndInstant = TimeUtil.toInstant(chunk[2]);
                    if (rangeEndInstant.isBefore(defaultCutoff) || rangeEndInstant.equals(defaultCutoff)) {
                        chunksToDropCount++;
                    } else {
                        // This chunk spans the cutoff or is after it
                        if (oldestChunkRangeEnd == null) {
                            oldestChunkRangeEnd = TimeUtil.toInstant(chunk[1]);
                        }
                    }
                }

                if (chunksToDropCount == 0) {
                    LOG.fine("No complete chunks to drop, all data is within retention period or in partial chunks");
                    // Still need to handle partial chunk with DELETE
                    if (oldestChunkRangeEnd != null && oldestChunkRangeEnd.isBefore(defaultCutoff)) {
                        LOG.fine("Deleting data from partial chunk using DELETE");
                        em.createQuery(
                            "delete from AssetDatapoint dp where dp.timestamp < :dt" + 
                            buildWhereClause(customRetentionAttributes, true)
                        ).setParameter("dt", Date.from(defaultCutoff)).executeUpdate();
                    }
                    return true;
                }

                // If there are attributes with custom retention, we cannot use drop_chunks for the whole table
                // because drop_chunks doesn't support filtering by entity_id/attribute_name
                if (!customRetentionAttributes.isEmpty()) {
                    // Find the minimum custom retention age
                    int minCustomAge = customRetentionAttributes.stream()
                        .mapToInt(attr -> attr.value.getMetaValue(MetaItemType.DATA_POINTS_MAX_AGE_DAYS).orElse(Integer.MAX_VALUE))
                        .min()
                        .orElse(Integer.MAX_VALUE);
                    
                    // We can only safely drop chunks that are older than ALL retention periods
                    Instant safeDropCutoff = timerService.getNow().truncatedTo(DAYS).minus(minCustomAge, DAYS);
                    if (safeDropCutoff.isAfter(defaultCutoff)) {
                        safeDropCutoff = defaultCutoff;
                    }
                    
                    LOG.info("Using safe drop cutoff of " + safeDropCutoff + " due to custom retention attributes");
                    cutoffTimestamp = Timestamp.from(safeDropCutoff);
                }

                // Use drop_chunks to efficiently remove whole chunks
                // This is instant and doesn't require decompression
                LOG.info("Dropping " + chunksToDropCount + " chunks older than " + cutoffTimestamp);
                
                // Use public.drop_chunks with explicit casts - TimescaleDB requires timestamp without time zone
                String dropChunksQuery = String.format(
                    "SELECT public.drop_chunks('asset_datapoint', older_than => '%s'::timestamp)",
                        cutoffTimestamp.toLocalDateTime().toString()
                );
                em.createNativeQuery(dropChunksQuery).getResultList();

                // Handle the partial chunk (if any) with DELETE for data between chunk start and cutoff
                // This is a smaller DELETE that only affects one chunk
                if (oldestChunkRangeEnd != null && oldestChunkRangeEnd.isBefore(defaultCutoff)) {
                    LOG.fine("Deleting remaining data from partial chunk between " + oldestChunkRangeEnd + " and " + defaultCutoff);
                    em.createQuery(
                        "delete from AssetDatapoint dp where dp.timestamp >= :chunkStart and dp.timestamp < :cutoff" +
                        buildWhereClause(customRetentionAttributes, true)
                    ).setParameter("chunkStart", Date.from(oldestChunkRangeEnd))
                     .setParameter("cutoff", Date.from(defaultCutoff))
                     .executeUpdate();
                }

                LOG.info("Successfully purged data points using TimescaleDB drop_chunks");
                return true;
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to use TimescaleDB drop_chunks, falling back to DELETE", e);
            return false;
        }
    }

    /**
     * Exports datapoints as CSV using SQL; the export path used in the SQL query must also be mapped into the manager
     * container so it can be accessed by this process.
     */
    public ScheduledFuture<File> exportDatapoints(AttributeRef[] attributeRefs,
                                                  long fromTimestamp,
                                                  long toTimestamp) {
        try {
            String query = getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp);

            // Verify the query is 'legal' and can be executed
            if(canQueryDatapoints(query, null, datapointExportLimit)) {
                return doExportDatapoints(attributeRefs, fromTimestamp, toTimestamp);
            }
            throw new RuntimeException("Could not export datapoints.");

        } catch (DatapointQueryTooLargeException dqex) {
            String msg = "Could not export data points. It exceeds the data limit of " + datapointExportLimit + " data points.";
            getLogger().log(Level.WARNING, msg, dqex);
            throw dqex;
        }
    }

    protected ScheduledFuture<File> doExportDatapoints(AttributeRef[] attributeRefs,
                                                       long fromTimestamp,
                                                       long toTimestamp) {

        return scheduledExecutorService.schedule(() -> {
            String fileName = UniqueIdentifierGenerator.generateId() + ".csv";
            StringBuilder sb = new StringBuilder("copy (")
                    .append(getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp))
                    .append(") to '/storage/")
                    .append(EXPORT_STORAGE_DIR_NAME)
                    .append("/")
                    .append(fileName)
                    .append("' delimiter ',' CSV HEADER;");

            persistenceService.doTransaction(em -> em.createNativeQuery(sb.toString()).executeUpdate());

            // The same path must resolve in both the postgresql container and the manager container
            return exportPath.resolve(fileName).toFile();
        }, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Function for building an SQL query that SELECTs the content for the data export.
     * Currently, it includes timestamp, asset name, attribute name, and the data point value.
     * Be aware: this SQL query does NOT contain any CSV-related statements.
     */
    protected String getSelectExportQuery(AttributeRef[] attributeRefs, long fromTimestamp, long toTimestamp) {
        return new StringBuilder(String.format("select ad.timestamp, a.name, ad.attribute_name, value from asset_datapoint ad, asset a where ad.entity_id = a.id and ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (", fromTimestamp / 1000, toTimestamp / 1000))
                .append(Arrays.stream(attributeRefs).map(attributeRef -> String.format("(ad.entity_id = '%s' and ad.attribute_name = '%s')", attributeRef.getId(), attributeRef.getName())).collect(Collectors.joining(" or ")))
                .append(")").toString();
    }
}
