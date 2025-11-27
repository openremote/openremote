package org.openremote.manager.datapoint;

import org.openremote.agent.protocol.ProtocolDatapointService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.OutdatedAttributeEvent;
import org.openremote.model.datapoint.DatapointExportFormat;
import org.openremote.model.datapoint.DatapointQueryTooLargeException;
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
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

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
     * Exports datapoints as CSV using SQL; the export path used in the SQL query must also be mapped into the manager
     * container so it can be accessed by this process.
     * Backwards compatible overload with default format.
     */
    public ScheduledFuture<File> exportDatapoints(AttributeRef[] attributeRefs,
                                                  long fromTimestamp,
                                                  long toTimestamp) {
        return exportDatapoints(attributeRefs, fromTimestamp, toTimestamp, DatapointExportFormat.CSV);
    }

    /**
     * Exports datapoints as CSV using SQL; the export path used in the SQL query must also be mapped into the manager
     * container so it can be accessed by this process.
     */
    public ScheduledFuture<File> exportDatapoints(AttributeRef[] attributeRefs,
                                                  long fromTimestamp,
                                                  long toTimestamp,
                                                  DatapointExportFormat format) {
        try {
            String query = getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp);

            // Verify the query is 'legal' and can be executed
            if(canQueryDatapoints(query, null, datapointExportLimit)) {
                return doExportDatapoints(attributeRefs, fromTimestamp, toTimestamp, format);
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
                                                       long toTimestamp,
                                                       DatapointExportFormat format) {

        return scheduledExecutorService.schedule(() -> {
            String fileName = UniqueIdentifierGenerator.generateId() + ".csv";
            if (format == DatapointExportFormat.CSV_CROSSTAB) {
                String attributeFilter = getAttributeFilter(attributeRefs);
                StringBuilder sb = new StringBuilder(String.format(
                        "copy (select * from crosstab( " +
                                "'select ad.timestamp, a.name || '' \\: '' || ad.attribute_name as header, ad.value " +
                                "from asset_datapoint ad " +
                                "join asset a on ad.entity_id = a.id " +
                                "where ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (%s) " +
                                "order by ad.timestamp, header', " +
                                "'select distinct a.name || '' \\: '' || ad.attribute_name as header " +
                                "from asset_datapoint ad " +
                                "join asset a on ad.entity_id = a.id " +
                                "where %s " +
                                "order by header') " +
                                "as ct(timestamp timestamp, %s) " +
                                ") to '/storage/" + EXPORT_STORAGE_DIR_NAME + "/" + fileName + "' delimiter ',' CSV HEADER;",
                        fromTimestamp / 1000, toTimestamp / 1000, attributeFilter, attributeFilter, getAttributeColumns(attributeRefs)
                ));
                persistenceService.doTransaction(em -> em.createNativeQuery(sb.toString()).executeUpdate());
            } else if (format == DatapointExportFormat.CSV_CROSSTAB_MINUTE) {
                String attributeFilter = getAttributeFilter(attributeRefs);
                StringBuilder sb = new StringBuilder(String.format(
                        "copy (select * from crosstab( " +
                                "'select public.time_bucket(''%s'', ad.timestamp) as bucket_timestamp, " +
                                "a.name || '' \\: '' || ad.attribute_name as header, " +
                                "CASE " +
                                "  WHEN jsonb_typeof((array_agg(ad.value))[1]) = ''number'' THEN " +
                                "    round(avg((ad.value#>>''{}'')::numeric) FILTER (WHERE jsonb_typeof(ad.value) = ''number''), 3)::text " +
                                "  ELSE (array_agg(ad.value ORDER BY ad.timestamp DESC) FILTER (WHERE jsonb_typeof(ad.value) != ''number''))[1]#>>''{}''" +
                                "END as value " +
                                "from asset_datapoint ad " +
                                "join asset a on ad.entity_id = a.id " +
                                "where ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (%s) " +
                                "group by bucket_timestamp, header " +
                                "order by bucket_timestamp, header', " +
                                "'select distinct a.name || '' \\: '' || ad.attribute_name as header " +
                                "from asset_datapoint ad " +
                                "join asset a on ad.entity_id = a.id " +
                                "where %s " +
                                "order by header') " +
                                "as ct(timestamp timestamp, %s) " +
                                ") to '/storage/" + EXPORT_STORAGE_DIR_NAME + "/" + fileName + "' delimiter ',' CSV HEADER;",
                        "1 minute", fromTimestamp / 1000, toTimestamp / 1000, attributeFilter, attributeFilter, getAttributeColumns(attributeRefs)
                ));

                persistenceService.doTransaction(em -> em.createNativeQuery(sb.toString()).executeUpdate());
            }
            else {
                StringBuilder sb = new StringBuilder("copy (")
                        .append(getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp))
                        .append(") to '/storage/")
                        .append(EXPORT_STORAGE_DIR_NAME)
                        .append("/")
                        .append(fileName)
                        .append("' delimiter ',' CSV HEADER;");
                persistenceService.doTransaction(em -> em.createNativeQuery(sb.toString()).executeUpdate());
            }



            // The same path must resolve in both the postgresql container and the manager container
            return exportPath.resolve(fileName).toFile();
        }, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Function for building a WHERE clause filter for the requested attributes.
     * Used in crosstab queries to ensure only the requested attributes are included.
     * Note: Uses double single quotes for SQL string escaping within crosstab queries.
     */
    private String getAttributeFilter(AttributeRef[] attributeRefs) {
        return Arrays.stream(attributeRefs)
                .map(attr -> String.format("(ad.entity_id = ''%s'' and ad.attribute_name = ''%s'')",
                        attr.getId(), attr.getName()))
                .collect(Collectors.joining(" or "));
    }

    /**
     * Function for building CSV attribute headers when format will make
     * a separate column per attribute.
     */
    private String getAttributeColumns(AttributeRef[] attributeRefs) {
        // Create a set to keep track of unique headers
        Set<String> headers = new HashSet<>();

        // Build unique headers in the format "AssetName: AttributeName"
        Arrays.stream(attributeRefs).forEach(attr -> {
            String assetName = assetStorageService.findNames(attr.getId()).toString().replaceAll("(^\\[|\\]$)", "");
            String attributeName = attr.getName();
            headers.add(assetName + ": " + attributeName);
        });

        // Return as a single string with the assembled header.
        return headers.stream()
                .map(header -> "\"" + header + "\" text")
                .collect(Collectors.joining(", "));
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
