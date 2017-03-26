package org.openremote.manager.server.datapoint;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.asset.AssetUpdate;
import org.openremote.model.AttributeRef;
import org.openremote.model.datapoint.AssetDatapoint;

import java.util.List;
import java.util.logging.Logger;
import java.util.function.Consumer;

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
        if (assetUpdate.getAttribute().isStoreDatapoints()) {
            LOG.finest("Storing asset update data point: " + assetUpdate);
            AssetDatapoint assetDatapoint = new AssetDatapoint(assetUpdate.getAttribute().getStateEvent(assetUpdate.getAssetId()));
            persistenceService.doTransaction(entityManager -> entityManager.persist(assetDatapoint));
        } else {
            LOG.finest("Ignoring asset update as attribute is not a data point: " + assetUpdate);
        }
    }

    public List<AssetDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "select dp from AssetDatapoint dp " +
                "where dp.entityId = :assetId " +
                "and dp.attributeName = :attributeName " +
                "order by dp.timestamp asc",
            AssetDatapoint.class)
            .setParameter("assetId", attributeRef.getEntityId())
            .setParameter("attributeName", attributeRef.getAttributeName())
            .getResultList());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
