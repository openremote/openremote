package org.openremote.manager.server.asset.datapoint;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.attribute.AttributeStateChange;
import org.openremote.manager.server.attribute.AttributeStateConsumerResult;
import org.openremote.manager.server.attribute.AttributeStateChangeConsumer;
import org.openremote.model.datapoint.AssetDatapoint;

import java.util.logging.Logger;

/**
 * Store and retrieve datapoints.
 */
public class DatapointService implements ContainerService, AttributeStateChangeConsumer {

    private static final Logger LOG = Logger.getLogger(DatapointService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public AttributeStateConsumerResult consumeAttributeStateChange(AttributeStateChange attributeStateChange) {
        ServerAsset asset = (ServerAsset)attributeStateChange.getAttributeParent();
        AssetDatapoint assetDatapoint = new AssetDatapoint(asset.getId(), attributeStateChange.getAttribute().getName(), attributeStateChange.getNewValue());
        // TODO
        return AttributeStateConsumerResult.OK;
    }
}
