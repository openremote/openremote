package org.openremote.manager.server.datapoint;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetState;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.Datapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.postgresql.util.PGInterval;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Store and retrieve datapoints for asset attributes.
 */
public class AssetDatapointService implements ContainerService, Consumer<AssetState> {

    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new AssetDatapointResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                container.getService(AssetStorageService.class),
                this
            )
        );

    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void accept(AssetState assetState) {
        if (Datapoint.isDatapointsCapable(assetState.getAttribute())
            && assetState.getAttribute().isStoreDatapoints()
            && assetState.getAttribute().getStateEvent().isPresent()) {
            LOG.finest("Storing datapoint for: " + assetState);
            AssetDatapoint assetDatapoint = new AssetDatapoint(assetState.getAttribute().getStateEvent().get());
            persistenceService.doTransaction(entityManager -> entityManager.persist(assetDatapoint));
        }
    }

    public List<AssetDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "select dp from AssetDatapoint dp " +
                "where dp.entityId = :assetId " +
                "and dp.attributeName = :attributeName " +
                "order by dp.timestamp desc",
            AssetDatapoint.class)
            .setParameter("assetId", attributeRef.getEntityId())
            .setParameter("attributeName", attributeRef.getAttributeName())
            .getResultList());
    }

    public NumberDatapoint[] aggregateDatapoints(AssetAttribute attribute,
                                                 DatapointInterval datapointInterval,
                                                 long timestamp) {
        LOG.fine("Aggregating datapoints for: " + attribute);

        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<NumberDatapoint[]>() {
                @Override
                public NumberDatapoint[] execute(Connection connection) throws SQLException {

                    String truncateX;
                    String step;
                    String interval;
                    Function<Timestamp, String> labelFunction;

                    SimpleDateFormat dayFormat = new SimpleDateFormat("dd. MMM yyyy");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    switch (datapointInterval) {
                        case HOUR:
                            truncateX = "minute";
                            step = "1 minute";
                            interval = "1 hour";
                            labelFunction = timeFormat::format;
                            break;
                        case DAY:
                            truncateX = "hour";
                            step = "1 hour";
                            interval = "1 day";
                            labelFunction = timeFormat::format;
                            break;
                        case WEEK:
                            truncateX = "day";
                            step = "1 day";
                            interval = "7 day";
                            labelFunction = dayFormat::format;
                            break;
                        case MONTH:
                            truncateX = "day";
                            step = "1 day";
                            interval = "1 month";
                            labelFunction = dayFormat::format;
                            break;
                        case YEAR:
                            truncateX = "month";
                            step = "1 month";
                            interval = "1 year";
                            labelFunction = dayFormat::format;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't handle interval: " + datapointInterval);
                    }

                    StringBuilder query = new StringBuilder();

                    query.append("select TS as X, coalesce(AVG_VALUE, null) as Y " +
                        " from ( " +
                        "       select date_trunc(?, GS)::timestamp TS " +
                        "       from generate_series(to_timestamp(?) - ?, to_timestamp(?), ?) GS " +
                        "       ) TS " +
                        "  left join ( " +
                        "       select " +
                        "           date_trunc(?, to_timestamp(TIMESTAMP / 1000))::timestamp as TS, ");

                    switch (attribute.getTypeOrThrow().getValueType()) {
                        case NUMBER:
                            query.append(" AVG(VALUE::text::numeric) as AVG_VALUE ");
                            break;
                        case BOOLEAN:
                            query.append(" AVG(case when VALUE::text::boolean is true then 1 else 0 end) as AVG_VALUE ");
                            break;
                        default:
                            throw new IllegalArgumentException("Can't aggregate number datapoints for type of: " + attribute);
                    }

                    query.append(" from ASSET_DATAPOINT " +
                        "         where " +
                        "           to_timestamp(TIMESTAMP / 1000) >= to_timestamp(?) - ? " +
                        "           and " +
                        "           to_timestamp(TIMESTAMP / 1000) <= to_timestamp(?) " +
                        "           and " +
                        "           ENTITY_ID = ? and ATTRIBUTE_NAME = ? " +
                        "         group by TS " +
                        "  ) DP using (TS) " +
                        " order by TS asc "
                    );

                    PreparedStatement st = connection.prepareStatement(query.toString());

                    long timestampSeconds = timestamp / 1000;
                    st.setString(1, truncateX);
                    st.setLong(2, timestampSeconds);
                    st.setObject(3, new PGInterval(interval));
                    st.setLong(4, timestampSeconds);
                    st.setObject(5, new PGInterval(step));
                    st.setString(6, truncateX);
                    st.setLong(7, timestampSeconds);
                    st.setObject(8, new PGInterval(interval));
                    st.setLong(9, timestampSeconds);
                    st.setString(10, attributeRef.getEntityId());
                    st.setString(11, attributeRef.getAttributeName());

                    try (ResultSet rs = st.executeQuery()) {
                        List<NumberDatapoint> result = new ArrayList<>();
                        while (rs.next()) {
                            String label = labelFunction.apply(rs.getTimestamp(1));
                            Number value = rs.getObject(2) != null ? rs.getDouble(2) : null;
                            result.add(new NumberDatapoint(label, value));
                        }
                        return result.toArray(new NumberDatapoint[result.size()]);
                    }
                }
            })
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
