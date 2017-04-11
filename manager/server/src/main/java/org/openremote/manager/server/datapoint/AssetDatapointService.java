package org.openremote.manager.server.datapoint;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.AttributeRef;
import org.openremote.model.asset.AssetState;
import org.openremote.model.datapoint.AssetDatapoint;

import java.util.List;
import java.util.function.Consumer;
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
        if (assetState.getAttribute().isStoreDatapoints()) {
            LOG.finest("Storing data point for: " + assetState);
            AssetDatapoint assetDatapoint = new AssetDatapoint(assetState.getAttribute().getStateEvent());
            persistenceService.doTransaction(entityManager -> entityManager.persist(assetDatapoint));
        } else {
            LOG.finest("Ignoring as attribute is not a data point: " + assetState);
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
