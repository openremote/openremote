package org.openremote.manager.server.datapoint;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.asset.AssetUpdate;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.model.Consumer;
import org.openremote.model.datapoint.AssetDatapoint;

import java.util.logging.Logger;

/**
 * Store and retrieve datapoints.
 */
public class AssetDatapointService implements ContainerService, Consumer<AssetUpdate> {

    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());

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
    public void accept(AssetUpdate assetUpdate) {
        AssetDatapoint assetDatapoint = new AssetDatapoint(assetUpdate.getNewState());
        // TODO Persist datapoint
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
