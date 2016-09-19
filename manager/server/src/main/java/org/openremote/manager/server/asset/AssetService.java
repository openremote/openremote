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
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.server.event.EventPredicate;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.asset.*;

import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.matchesEntityType;

public class AssetService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetService.class.getName());

    // TODO this is quite a hack to get some publish/subscribe system going, it will have to be replaced
    final protected Map<String, Long> assetListeners = new ConcurrentHashMap<>();
    final protected int ASSET_LISTENERS_MAX_LIFETIME_SECONDS = 60;
    final protected ScheduledExecutorService assetListenerScheduler = Executors.newScheduledThreadPool(1);

    protected MessageBrokerService messageBrokerService;
    protected EventService eventService;
    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        eventService = container.getService(EventService.class);
        persistenceService = container.getService(PersistenceService.class);

        // Max life time of listeners is 60 seconds
        assetListenerScheduler.scheduleAtFixedRate(() -> {
            assetListeners.forEach((sessionKey, timeStamp) -> {
                if (timeStamp + (ASSET_LISTENERS_MAX_LIFETIME_SECONDS * 1000) < System.currentTimeMillis()) {
                    LOG.fine("Removing asset listener session due to timeout: " + sessionKey);
                    assetListeners.remove(sessionKey);
                }
            });
        }, ASSET_LISTENERS_MAX_LIFETIME_SECONDS, ASSET_LISTENERS_MAX_LIFETIME_SECONDS, TimeUnit.SECONDS);

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                // TODO None of this is secure or considers the realm of an asset or the logged-in user's realm
                from(EventService.INCOMING_EVENT_QUEUE)
                    .filter(new EventPredicate<>(SubscribeAssetModified.class))
                    .process(exchange -> {
                        String sessionKey = exchange.getIn().getHeader(WebsocketConstants.SESSION_KEY, String.class);
                        Long timestamp = System.currentTimeMillis();
                        LOG.fine("Subscribing/updating subscription to asset change events for session: " + sessionKey);
                        assetListeners.put(sessionKey, timestamp);
                    });

                from(EventService.INCOMING_EVENT_QUEUE)
                    .filter(new EventPredicate<>(UnsubscribeAssetModified.class))
                    .process(exchange -> {
                        String sessionKey = exchange.getIn().getHeader(WebsocketConstants.SESSION_KEY, String.class);
                        LOG.fine("Unsubscribing from asset change events for session: " + sessionKey);
                        assetListeners.remove(sessionKey);
                    });

                from(PERSISTENCE_EVENT_TOPIC)
                    .filter(matchesEntityType(Asset.class))
                    .process(exchange -> {
                        PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                        //noinspection unchecked
                        for (AssetModifiedEvent assetModifiedEvent : createAssetModifiedEvents(persistenceEvent)) {
                            LOG.fine("Publishing asset modified event to asset listeners: " + assetListeners.size());
                            for (String sessionKey : assetListeners.keySet()) {
                                eventService.sendEvent(sessionKey, assetModifiedEvent);
                            }
                        }
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

    public AssetInfo[] getRoot(String realm) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doTransaction(em -> {
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
        return persistenceService.doTransaction(em -> {
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

    public ServerAsset[] findByTypeInAllRealms(AssetType assetType) {
        return persistenceService.doTransaction(em -> {
            List<ServerAsset> result =
                em.createQuery(
                    "select a from Asset a where a.type = :assetType order by a.createdOn asc",
                    ServerAsset.class)
                    .setParameter("assetType", assetType.getValue())
                    .getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    public AssetInfo[] getChildren(String parentId) {
        return persistenceService.doTransaction(em -> {
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
        return persistenceService.doTransaction(em -> {
            return loadAsset(em, assetId);
        });
    }

    public ServerAsset merge(ServerAsset asset) {
        return persistenceService.doTransaction(em -> {
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

    private AssetModifiedEvent[] createAssetModifiedEvents(PersistenceEvent<Asset> persistenceEvent) {
        List<AssetModifiedEvent> events = new ArrayList<>();

        Asset asset = persistenceEvent.getEntity();
        // Assets are a composite structure and we want to be able to fire events that say
        // "a child was removed/inserted for this or that parent". To do this we need to
        // compare old and new entity state, figuring out which parent was modified. There
        // is also the special case of assets without parent: In that case we fire a "children
        // modified" for a special "empty" asset, which clients should consider to be the root
        // item of the whole tree. It has no name, no id, no parent, etc.
        List<String> modifiedParentIds = new ArrayList<>();

        AssetModifiedEvent.Cause cause = null;

        switch (persistenceEvent.getCause()) {
            case INSERT:
                cause = AssetModifiedEvent.Cause.CREATE;
                if (asset.getParentId() != null) {
                    modifiedParentIds.add(asset.getParentId());
                } else {
                    // The "root without id" asset was modified
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                }
                break;
            case UPDATE:
                cause = AssetModifiedEvent.Cause.UPDATE;

                // Find out if the asset has a new parent
                String previousParentId = persistenceEvent.getPreviousState("parentId");
                String currentParentId = persistenceEvent.getCurrentState("parentId");
                if (previousParentId == null && currentParentId == null) {
                    // The "root without id" asset was modified
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                } else {
                    if (previousParentId == null) {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                        modifiedParentIds.add(currentParentId);
                    } else if (currentParentId == null) {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                        modifiedParentIds.add(previousParentId);
                    }
                }

                // Only send "children modified" of the parent if the name of the asset changed
                String previousName = persistenceEvent.getPreviousState("name");
                String currentName = persistenceEvent.getCurrentState("name");
                boolean isEqualName = Objects.equals(previousName, currentName);
                if (!isEqualName){
                    modifiedParentIds.add(asset.getParentId());
                }

                break;
            case DELETE:
                cause = AssetModifiedEvent.Cause.DELETE;
                if (asset.getParentId() != null) {
                    modifiedParentIds.add(asset.getParentId());
                } else {
                    // The "root without id" asset was modified
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                }
                break;
        }

        events.add(new AssetModifiedEvent(asset, cause));

        for (String modifiedParentId : modifiedParentIds) {
            Asset parent = get(modifiedParentId);
            if (parent != null) {
                events.add(new AssetModifiedEvent(parent, AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
            }
        }

        return events.toArray(new AssetModifiedEvent[events.size()]);
    }

}