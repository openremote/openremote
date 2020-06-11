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
package org.openremote.manager.predicted;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.agent.protocol.ProtocolPredictedAssetService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;
import org.postgresql.util.PGInterval;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AssetPredictedDatapointService implements ContainerService, ProtocolPredictedAssetService {

    public static final int PRIORITY = AssetStorageService.PRIORITY + 300;
    private static final Logger LOG = Logger.getLogger(AssetPredictedDatapointService.class.getName());

    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        assetStorageService = container.getService(AssetStorageService.class);

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

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public long getDatapointsCount() {
        return getDatapointsCount(null);
    }

    public long getDatapointsCount(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> {

            String queryStr = attributeRef == null ?
                "select count(dp) from AssetPredictedDatapoint dp" :
                "select count(dp) from AssetPredictedDatapoint dp where dp.entityId = :assetId and dp.attributeName = :attributeName";

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
        ValueType attributeValueType = asset.getAttribute(attributeRef.getAttributeName())
            .orElseThrow(() -> new IllegalStateException("Attribute not found: " + attributeRef.getAttributeName()))
            .getTypeOrThrow()
            .getValueType();

        LOG.fine("Getting predicted datapoints for: " + attributeRef);

        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<ValueDatapoint[]>() {
                @Override
                public ValueDatapoint[] execute(Connection connection) throws SQLException {

                    StringBuilder query = new StringBuilder();
                    boolean downsample = (attributeValueType == ValueType.NUMBER || attributeValueType == ValueType.BOOLEAN);

                    String truncateX = null;
                    String interval = null;

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

                        query.append(" from ASSET_PREDICTED_DATAPOINT " +
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
                        query.append("select distinct TIMESTAMP AS X, value AS Y from ASSET_PREDICTED_DATAPOINT " +
                            "where " +
                            "TIMESTAMP >= to_timestamp(?) " +
                            "and " +
                            "TIMESTAMP < to_timestamp(?) " +
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

    public void updateValue(AttributeRef attributeRef, Value value, long timestamp) {
        persistenceService.doTransaction(em -> upsertValue(em, attributeRef.getEntityId(), attributeRef.getAttributeName(), value, timestamp));
    }

    public void updateValue(String assetId, String attributeName, Value value, long timestamp) {
        updateValue(new AttributeRef(assetId, attributeName), value, timestamp);
    }

    private void upsertValue(EntityManager entityManager, String assetId, String attributeName, Value value, long timestamp) {
        entityManager.createNativeQuery(String.format("INSERT INTO asset_predicted_datapoint (entity_id, attribute_name, value, timestamp) \n" +
            "VALUES ('%1$s', '%2$s', '%3$s', to_timestamp(%4$d))\n" +
            "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE \n" +
            "  SET value = excluded.value", assetId, attributeName, value.toJson(), timestamp))
            .executeUpdate();
    }
}
