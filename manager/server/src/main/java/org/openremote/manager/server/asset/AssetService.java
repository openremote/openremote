/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.server.asset;

import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetInfo;

import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_HEADER;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;

public class AssetService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetService.class.getName());

    protected MessageBrokerService messageBrokerService;
    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        persistenceService = container.getService(PersistenceService.class);

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(PERSISTENCE_EVENT_TOPIC)
                    .filter(body().isInstanceOf(Asset.class))
                    .process(exchange -> {
                        LOG.info("### ASSET PERSISTENCE EVENT: " + exchange.getIn().getHeader(PERSISTENCE_EVENT_HEADER));
                        LOG.info("### ASSET: " + exchange.getIn().getBody());
                    });
            }
        });
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new AssetResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public AssetInfo[] getRoot() {
        return persistenceService.doTransaction(em -> {
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.shared.asset.AssetInfo(" +
                        "a.id, a.name, a.type, a.parent.id" +
                        ") from Asset a where a.parent is null order by a.createdOn asc",
                    AssetInfo.class
                ).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    public AssetInfo[] getChildren(String parentId) {
        return persistenceService.doTransaction(em -> {
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.shared.asset.AssetInfo(" +
                        "a.id, a.name, a.type, a.parent.id" +
                        ") from Asset a where a.parent.id = :parentId order by a.createdOn asc",
                    AssetInfo.class
                ).setParameter("parentId", parentId).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    public ServerAsset get(String assetId) {
        return persistenceService.doTransaction(em -> {
            return loadAsset(em, assetId);
        });
    }

    public void update(ServerAsset asset) {
        persistenceService.doTransaction(em -> {
            validateParent(em, asset);
            em.merge(asset);
        });
    }

    public void create(ServerAsset asset) {
        persistenceService.doTransaction(em -> {
            validateParent(em, asset);
            em.persist(asset);
        });
    }

    public void delete(String assetId) {
        persistenceService.doTransaction(em -> {
            Asset asset = em.find(ServerAsset.class, assetId);
            if (asset != null) {
                em.remove(asset);
            }
        });
    }

    protected ServerAsset loadAsset(EntityManager em, String assetId) {
        ServerAsset asset = em.find(ServerAsset.class, assetId);
        if (asset == null)
            return null;
        asset.setPath(em.unwrap(Session.class).doReturningWork(connection -> {
            String query =
                "WITH RECURSIVE ASSET_TREE(ID, PARENT_ID, PATH) AS (" +
                    " SELECT a1.ID, a1.PARENT_ID, ARRAY[text(a1.ID)] FROM ASSET a1 WHERE a1.PARENT_ID IS NULL" +
                    " UNION ALL" +
                    " SELECT a2.ID, a2.PARENT_ID, array_append(at.PATH, text(a2.ID)) FROM ASSET a2, ASSET_TREE at WHERE a2.PARENT_ID = at.ID" +
                    ") SELECT PATH FROM ASSET_TREE WHERE ID = ?";

            ResultSet result = null;
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, assetId);
                result = statement.executeQuery();
                if (result.next()) {
                    return (String[]) result.getArray("PATH").getArray();
                }
                return null;
            } finally {
                if (result != null)
                    result.close();
            }
        }));
        return asset;
    }

    protected void validateParent(EntityManager em, Asset asset) {
        if (asset.getParentId() == null)
            return;
        ServerAsset parent = loadAsset(em, asset.getParentId());
        if (parent == null)
            throw new IllegalStateException("Parent asset not found: " + asset.getParentId());
        if (Arrays.asList(parent.getPath()).contains(asset.getId()))
            throw new IllegalStateException("Parent asset can not be a child of the asset: " + asset.getParentId());
    }

}