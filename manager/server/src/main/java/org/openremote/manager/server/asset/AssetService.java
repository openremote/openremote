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
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.asset.*;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.asset.AssetType;

import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.manager.server.event.EventPredicates.isEventType;

public class AssetService extends RouteBuilder implements ContainerService {

    protected MessageBrokerService messageBrokerService;
    protected EventService eventService;
    protected PersistenceService persistenceService;
    protected AssetListenerSubscriptions assetListenerSubscriptions;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        eventService = container.getService(EventService.class);
        persistenceService = container.getService(PersistenceService.class);

        assetListenerSubscriptions = new AssetListenerSubscriptions(eventService) {
            @Override
            protected Asset getAsset(String assetId) {
                return AssetService.this.get(assetId);
            }
        };

        messageBrokerService.getContext().addRoutes(this);
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

    @Override
    public void configure() throws Exception {
        // TODO None of this is secure or considers the realm of an asset or the logged-in user's realm
        from(EventService.INCOMING_EVENT_QUEUE)
            .filter(isEventType(SubscribeAssetModified.class))
            .process(exchange -> {
                String sessionKey = EventService.getSessionKey(exchange);
                assetListenerSubscriptions.addSubscription(
                    sessionKey,
                    exchange.getIn().getBody(SubscribeAssetModified.class)
                );
            });

        from(EventService.INCOMING_EVENT_QUEUE)
            .filter(isEventType(UnsubscribeAssetModified.class))
            .process(exchange -> {
                String sessionKey = EventService.getSessionKey(exchange);
                assetListenerSubscriptions.removeSubscription(
                    sessionKey,
                    exchange.getIn().getBody(UnsubscribeAssetModified.class)
                );
            });

        from(PERSISTENCE_EVENT_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                assetListenerSubscriptions.dispatch(persistenceEvent);
            });
    }

    public AssetInfo[] getRoot(String realm) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            List<AssetInfo> result = em.createQuery(
                "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                    "a.id, a.version, a.name, a.realm, a.type, a.parent.id, a.location" +
                    ") from Asset a where a.parent is null and a.realm = :realm order by a.createdOn asc",
                AssetInfo.class
            ).setParameter("realm", realm).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    public ServerAsset[] findByType(String realm, AssetType assetType) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result =
                em.createQuery(
                    "select a from Asset a where a.realm = :realm and a.type = :assetType order by a.createdOn asc",
                    ServerAsset.class)
                    .setParameter("realm", realm)
                    .setParameter("assetType", assetType.getValue())
                    .getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    public ServerAsset[] findChildrenByType(String parentId, AssetType assetType) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result =
                em.createQuery(
                    "select a from Asset a where a.type = :assetType and a.parentId = :parentId order by a.createdOn asc",
                    ServerAsset.class)
                    .setParameter("assetType", assetType.getValue())
                    .setParameter("parentId", parentId)
                    .getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    public ServerAsset[] findByTypeInAllRealms(String assetType) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result =
                em.createQuery(
                    "select a from Asset a where a.type = :assetType order by a.createdOn asc",
                    ServerAsset.class)
                    .setParameter("assetType", assetType)
                    .getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    public AssetInfo[] getChildren(String parentId) {
        return persistenceService.doReturningTransaction(em -> {
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                        "a.id, a.version, a.name, a.realm, a.type, a.parent.id, a.location" +
                        ") from Asset a where a.parent.id = :parentId order by a.createdOn asc",
                    AssetInfo.class
                ).setParameter("parentId", parentId).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    public ServerAsset get(String assetId) {
        return persistenceService.doReturningTransaction(em -> loadAsset(em, assetId));
    }

    public ServerAsset merge(ServerAsset asset) {
        return persistenceService.doReturningTransaction(em -> {
            validateParent(em, asset);
            return em.merge(asset);
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

    /* TODO: What is the asset lifecycle and how do we handle tree integrity?
    public void deleteChildren(String parentId) {
        persistenceService.doTransaction(em -> {
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                        "a.id, a.version, a.name, a.realm, a.type, a.parent.id, a.location" +
                        ") from Asset a where a.parent.id = :parentId order by a.createdOn asc",
                    AssetInfo.class
                ).setParameter("parentId", parentId).getResultList();
            for (AssetInfo assetInfo : result) {
                Asset asset = em.find(ServerAsset.class, assetInfo.getId());
                if (asset != null) {
                    em.remove(asset);
                }
            }
        });
    }
    */

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