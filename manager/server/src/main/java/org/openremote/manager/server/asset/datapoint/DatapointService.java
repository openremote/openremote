package org.openremote.manager.server.asset.datapoint;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.AttributeState;
import org.openremote.model.datapoint.AssetDatapoint;

import java.util.logging.Logger;

/**
 * Store and retrieve datapoints.
 */
public class DatapointService implements ContainerService {

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

    public void storeAssetDatapoint(AttributeState attributeState) {
        AssetDatapoint assetDatapoint = new AssetDatapoint(attributeState);
        // TODO
        
    }

}
