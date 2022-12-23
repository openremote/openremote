/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.setup.load1;

import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.attribute.AttributeEvent;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;

/**
 * This service listens for {@link org.openremote.model.asset.impl.WeatherAsset}s to be added to the system then
 * it generates attribute updates for the {@link WeatherAsset#RAINFALL} and {@link WeatherAsset#TEMPERATURE} attributes
 * periodically
 */
public class AttributeValueGeneratorService extends RouteBuilder implements ContainerService {

    protected final int UPDATE_MILLIS = 5000;
    protected static final Logger LOG = Logger.getLogger(AttributeValueGeneratorService.class.getName());
    protected AssetProcessingService assetProcessingService;
    protected ScheduledExecutorService executorService;
    protected Set<String> assetIds = new ConcurrentHashSet<>();
    protected ScheduledFuture<?> updater;

    @Override
    public void init(Container container) throws Exception {
        assetProcessingService = container.getService(AssetProcessingService.class);
        executorService = container.getExecutorService();
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
         updater = executorService.scheduleWithFixedDelay(this::sendValues, UPDATE_MILLIS, UPDATE_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(Container container) throws Exception {
        if (updater != null) {
            updater.cancel(true);
        }
        updater = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        // If any asset was modified in the database, detect changed attributes
        from(PERSISTENCE_TOPIC)
            .routeId("WeatherAssetChanges")
            .filter(and(isPersistenceEventForEntityType(Asset.class), exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                return persistenceEvent.getEntity() instanceof WeatherAsset && persistenceEvent.getCause() == PersistenceEvent.Cause.CREATE;
            }))
            .process(exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                onAssetCreated((WeatherAsset)persistenceEvent.getEntity());
            });
    }

    protected void onAssetCreated(WeatherAsset weatherAsset) {
        assetIds.add(weatherAsset.getId());
    }

    protected void sendValues() {
        for (String assetId : assetIds) {
            LOG.fine("Updating attributes on asset ID: " + assetId);
            assetProcessingService.sendAttributeEvent(new AttributeEvent(assetId, WeatherAsset.TEMPERATURE, Math.random()*40));
            assetProcessingService.sendAttributeEvent(new AttributeEvent(assetId, WeatherAsset.RAINFALL, Math.random()*10));
        }
    }
}
