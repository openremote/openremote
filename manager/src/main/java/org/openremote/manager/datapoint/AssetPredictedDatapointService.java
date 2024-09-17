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

import org.openremote.agent.protocol.ProtocolPredictedDatapointService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetPredictedDatapoint;
import org.openremote.model.datapoint.ValueDatapoint;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.HOURS;

public class AssetPredictedDatapointService extends AbstractDatapointService<AssetPredictedDatapoint> implements ProtocolPredictedDatapointService {

    private static final Logger LOG = Logger.getLogger(AssetPredictedDatapointService.class.getName());

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        container.getService(ManagerWebService.class).addApiSingleton(
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
            getFirstPurgeMillis(timerService.getNow()),
            Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS
        );
    }

    public void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp) {
        updateValue(attributeRef.getId(), attributeRef.getName(), value, timestamp);
    }

    public void updateValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) {
        upsertValue(assetId, attributeName, value, timestamp);
    }

    public void updateValues(String assetId, String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps) {
        persistenceService.doTransaction(em -> upsertValues(assetId, attributeName, valuesAndTimestamps));
    }

    public void purgeValues(String assetId, String attributeName) {
        persistenceService.doTransaction(em -> em.createQuery(
            "delete from " + getDatapointClass().getSimpleName() + " dp where dp.assetId=?1 and dp.attributeName=?2"
        ).setParameter(1, assetId).setParameter(2, attributeName).executeUpdate());
    }

    @Override
    protected Class<AssetPredictedDatapoint> getDatapointClass() {
        return AssetPredictedDatapoint.class;
    }

    @Override
    protected String getDatapointTableName() {
        return AssetPredictedDatapoint.TABLE_NAME;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected long getFirstPurgeMillis(Instant currentTime) {
        return super.getFirstPurgeMillis(currentTime) - 1800000; // Run half hour before default
    }

    protected void purgeDataPoints() {
        try {
            // Purge data points not in the above list using default duration
            LOG.finest("Purging predicted data points older than now");
            doPurge("where dp.timestamp < :dt", Date.from(timerService.getNow().truncatedTo(HOURS)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to run data points purge", e);
        }
    }
}
