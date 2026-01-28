package org.openremote.manager.datapoint;

import org.hibernate.Session;
import org.openremote.agent.protocol.ProtocolDatapointService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.OutdatedAttributeEvent;
import org.openremote.model.datapoint.DatapointExportFormat;
import org.openremote.model.datapoint.DatapointQueryTooLargeException;
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
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.value.MetaHolder;
import org.openremote.model.value.MetaItemType;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.*;
import java.sql.Date;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.TreeSet;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openremote.model.syslog.SyslogCategory.DATA;
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
    private static final Logger DATA_EXPORT_LOG = SyslogCategory.getLogger(DATA, AssetDatapointResourceImpl.class);
    protected static final String EXPORT_STORAGE_DIR_NAME = "datapoint";
    protected int maxDatapointAgeDays;
    protected int datapointExportLimit;

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
    public PipedInputStream exportDatapoints(AttributeRef[] attributeRefs,
                                                  long fromTimestamp,
                                                  long toTimestamp) throws IOException {
        return exportDatapoints(attributeRefs, fromTimestamp, toTimestamp, DatapointExportFormat.CSV);
    }

    /**
     * Exports datapoints as CSV using SQL; the export path used in the SQL query must also be mapped into the manager
     * container so it can be accessed by this process.
     */
    public PipedInputStream exportDatapoints(AttributeRef[] attributeRefs,
                                                  long fromTimestamp,
                                                  long toTimestamp,
                                                  DatapointExportFormat format) throws IOException {
        try {
            String query = getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp, null);

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

    protected PipedInputStream doExportDatapoints(AttributeRef[] attributeRefs,
                                                       long fromTimestamp,
                                                       long toTimestamp,
                                                       DatapointExportFormat format) throws IOException {
        // Increase buffer size (default is only 1 KB)
        PipedInputStream in = new PipedInputStream(1024 * 1024 * 4); // 4 MB
        PipedOutputStream out = new PipedOutputStream(in);

        final String TO_STDOUT_WITH_CSV_FORMAT = ") TO STDOUT WITH (FORMAT CSV, HEADER, DELIMITER ',');";
        String query;

        if (format == DatapointExportFormat.CSV_CROSSTAB || format == DatapointExportFormat.CSV_CROSSTAB_MINUTE) {
            Set<String> headers = getAttributeHeaders(attributeRefs);

            // Category query using VALUES clause with dollar-quoting to avoid escaping issues
            String categoryValues = headers.stream()
                    .map(header -> "('" + header.replace("'", "''") + "')")
                    .collect(Collectors.joining(", "));
            String categoryQuery = "SELECT header FROM (VALUES " + categoryValues + ") AS t(header)";

            // Column definitions for crosstab result
            String attributeColumns = headers.stream()
                    .map(header -> "\"" + header + "\" text")
                    .collect(Collectors.joining(", "));

            query = String.format(
                    "copy (select * from crosstab('%s', $cat$%s$cat$) as ct(timestamp timestamp, %s)%s",
                    getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp, format),
                    categoryQuery, attributeColumns, TO_STDOUT_WITH_CSV_FORMAT);
        } else {
            query = "copy (" + getSelectExportQuery(attributeRefs, fromTimestamp, toTimestamp, format) + TO_STDOUT_WITH_CSV_FORMAT;
        }

        // Execute query asynchronously
        scheduledExecutorService.schedule(() -> {
            boolean success = false;
            try {
                persistenceService.doTransaction(em -> {
                    Session session = em.unwrap(Session.class);
                    session.doWork(connection -> {
                        try {
                            PGConnection pgConnection = connection.unwrap(PGConnection.class);
                            CopyManager copyManager = pgConnection.getCopyAPI();
                            copyManager.copyOut(query, out);
                            out.flush();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to export datapoints", e);
                        }
                    });
                });
                success = true;
            } catch (Exception e) {
                DATA_EXPORT_LOG.log(Level.SEVERE, "Datapoint export failed", e);
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    DATA_EXPORT_LOG.log(Level.WARNING, "Failed to close output stream", e);
                }
                if (!success) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        DATA_EXPORT_LOG.log(Level.SEVERE, "Failed to close piped input stream", e);
                    }
                }
            }
        }, 0, TimeUnit.MILLISECONDS);

        return in;
    }
    
    /**
     * Function for building CSV attribute headers when format will make
     * a separate column per attribute.
     * Uses TreeSet for alphabetical ordering required by crosstab queries.
     */
    private Set<String> getAttributeHeaders(AttributeRef[] attributeRefs) {
        Set<String> headers = new TreeSet<>();
        Arrays.stream(attributeRefs).forEach(attr -> {
            String assetName = assetStorageService.findNames(attr.getId()).toString().replaceAll("(^\\[|\\]$)", "");
            headers.add(assetName + " : " + attr.getName());
        });
        return headers;
    }

    /**
     * Function for building an SQL query that SELECTs the content for the data export.
     * For crosstab formats, uses escaped quotes ('') as these are embedded in a crosstab string argument.
     */
    protected String getSelectExportQuery(AttributeRef[] attributeRefs, long fromTimestamp, long toTimestamp, DatapointExportFormat format) {
        long fromSeconds = fromTimestamp / 1000;
        long toSeconds = toTimestamp / 1000;

        if (format == null) {
            format = DatapointExportFormat.CSV;
        }

        if (format == DatapointExportFormat.CSV_CROSSTAB_MINUTE) {
            String attributeFilter = Arrays.stream(attributeRefs)
                    .map(attr -> String.format("(ad.entity_id = ''%s'' and ad.attribute_name = ''%s'')", attr.getId(), attr.getName()))
                    .collect(Collectors.joining(" or "));
            return String.format(
                    "select public.time_bucket(''1 minute'', ad.timestamp) as bucket_timestamp, " +
                    "a.name || '' : '' || ad.attribute_name as header, " +
                    "CASE " +
                    "  WHEN jsonb_typeof((array_agg(ad.value))[1]) = ''number'' THEN " +
                    "    round(avg((ad.value#>>''{}'')::numeric) FILTER (WHERE jsonb_typeof(ad.value) = ''number''), 3)::text " +
                    "  ELSE (array_agg(ad.value ORDER BY ad.timestamp DESC) FILTER (WHERE jsonb_typeof(ad.value) != ''number''))[1]#>>''{}''" +
                    "END as value " +
                    "from asset_datapoint ad " +
                    "join asset a on ad.entity_id = a.id " +
                    "where ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (%s) " +
                    "group by bucket_timestamp, header " +
                    "order by bucket_timestamp, header",
                    fromSeconds, toSeconds, attributeFilter);
        } else if (format == DatapointExportFormat.CSV_CROSSTAB) {
            String attributeFilter = Arrays.stream(attributeRefs)
                    .map(attr -> String.format("(ad.entity_id = ''%s'' and ad.attribute_name = ''%s'')", attr.getId(), attr.getName()))
                    .collect(Collectors.joining(" or "));
            return String.format(
                    "select ad.timestamp, a.name || '' : '' || ad.attribute_name as header, ad.value " +
                    "from asset_datapoint ad " +
                    "join asset a on ad.entity_id = a.id " +
                    "where ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (%s) " +
                    "order by ad.timestamp, header",
                    fromSeconds, toSeconds, attributeFilter);
        } else {
            // Plain CSV format - no quote escaping needed
            String attributeFilter = Arrays.stream(attributeRefs)
                    .map(attr -> String.format("(ad.entity_id = '%s' and ad.attribute_name = '%s')", attr.getId(), attr.getName()))
                    .collect(Collectors.joining(" or "));
            return String.format(
                    "select ad.timestamp, a.name, ad.attribute_name, value " +
                    "from asset_datapoint ad, asset a " +
                    "where ad.entity_id = a.id and ad.timestamp >= to_timestamp(%d) and ad.timestamp <= to_timestamp(%d) and (%s)",
                    fromSeconds, toSeconds, attributeFilter);
        }
    }
}
