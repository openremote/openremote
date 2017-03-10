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

import elemental.json.JsonValue;
import org.apache.camel.builder.RouteBuilder;
import org.hibernate.Session;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.agent.AgentAttributes;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.asset.SubscribeAssetModified;
import org.openremote.manager.shared.asset.UnsubscribeAssetModified;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.Consumer;
import org.openremote.model.Function;
import org.openremote.model.asset.*;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.manager.server.event.EventPredicates.isEventType;
import static org.openremote.model.asset.AssetType.AGENT;

public class AssetStorageService
    extends RouteBuilder
    implements ContainerService, Consumer<AssetStateChange<ServerAsset>> {

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());

    protected EventService eventService;
    protected PersistenceService persistenceService;
    protected AssetListenerSubscriptions assetListenerSubscriptions;
    final protected Function<AttributeRef, ProtocolConfiguration> agentLinkResolver = agentLink -> {
        // Resolve the agent and the protocol configuration
        // TODO This is very inefficient and requires Hibernate second-level caching
        Asset agent = find(agentLink.getEntityId());
        if (agent != null && agent.getWellKnownType().equals(AGENT)) {
            AgentAttributes agentAttributes = new AgentAttributes(agent);
            return agentAttributes.getProtocolConfiguration(agentLink.getAttributeName());
        }
        return null;
    };

    @Override
    public void init(Container container) throws Exception {
        eventService = container.getService(EventService.class);
        persistenceService = container.getService(PersistenceService.class);

        // TODO None of this is secure or considers the realm of an asset or the logged-in user's realm
        assetListenerSubscriptions = new AssetListenerSubscriptions(eventService) {
            @Override
            protected Asset getAsset(String assetId) {
                return AssetStorageService.this.find(assetId);
            }
        };

        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);

        container.getService(WebService.class).getApiSingletons().add(
            new AssetResourceImpl(container.getService(ManagerIdentityService.class), this)
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

        from(PERSISTENCE_TOPIC)
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                assetListenerSubscriptions.dispatch(persistenceEvent);
            });
    }

    public Function<AttributeRef, ProtocolConfiguration> getAgentLinkResolver() {
        return agentLinkResolver;
    }

    /**
     * Find assets without a parent in a realm.
     *
     * @param realm The realm to search.
     * @return The root assets of the realm ordered ascending by creation date, or an empty array if there is no data.
     */
    public AssetInfo[] findRoot(String realm) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            List<AssetInfo> result = em.createQuery(
                "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                    "a.id, a.version, a.name, a.createdOn, a.realm, a.type, a.parent.id, a.location" +
                    ") from Asset a where a.parent is null and a.realm = :realm order by a.createdOn asc",
                AssetInfo.class
            ).setParameter("realm", realm).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    /**
     * Retrieve the children of an asset.
     *
     * @param parentId The ID of the parent asset.
     * @return The child assets ordered ascending by creation date, or an empty array if there is no data.
     */
    public AssetInfo[] findChildren(String parentId) {
        return persistenceService.doReturningTransaction(em -> {
            Asset parent = loadAsset(em, parentId);
            if (parent == null)
                return new AssetInfo[0];
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                        "a.id, a.version, a.name, a.createdOn, a.realm, a.type, a.parent.id, a.location" +
                        ") from Asset a where a.parent.id = :parentId order by a.createdOn asc",
                    AssetInfo.class
                ).setParameter("parentId", parentId).getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    /**
     * Retrieve the children of an asset in a particular realm.
     *
     * @param parentId The ID of the parent asset.
     * @param realm    The realm in which both parent asset and its children must be.
     * @return The child assets ordered ascending by creation date, or an empty array if there is no data.
     */
    public AssetInfo[] findChildrenInRealm(String parentId, String realm) {
        return persistenceService.doReturningTransaction(em -> {
            Asset parent = loadAsset(em, parentId);
            if (parent == null)
                return new AssetInfo[0];
            List<AssetInfo> result =
                em.createQuery(
                    "select new org.openremote.manager.server.asset.ServerAssetInfo(" +
                        "a.id, a.version, a.name, a.createdOn, a.realm, a.type, a.parent.id, a.location" +
                        ") from Asset a, Asset p where p.id = :parentId and a.parent.id = :parentId " +
                        "and p.realm = :realm and a.realm = :realm " +
                        "order by a.createdOn asc",
                    AssetInfo.class
                ).setParameter("parentId", parentId)
                    .setParameter("realm", realm)
                    .getResultList();
            return result.toArray(new AssetInfo[result.size()]);
        });
    }

    /**
     * Find an asset by primary key.
     *
     * @param assetId The primary key identifier value of the asset.
     * @return The asset or <code>null</code> if the asset doesn't exist.
     */
    public ServerAsset find(String assetId) {
        return persistenceService.doReturningTransaction(em -> loadAsset(em, assetId));
    }

    /**
     * Find protected assets by primary keys.
     *
     * @param realm    The realm in which the assets must be.
     * @param assetIds The primary key identifier values of the assets to find.
     * @return The found assets or an empty array
     */
    public ProtectedAssetInfo[] findProtectedById(String realm, String[] assetIds) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            // TODO The SQL IN clause is not limited on Postgres, we should have some sanity check here on the number of IDs!
            List<ProtectedAssetInfo> result = em.createQuery(
                "select new org.openremote.manager.server.asset.ProtectedServerAssetInfo(" +
                    "a.id, a.version, a.name, a.createdOn, a.realm, a.type, a.parent.id, a.location, a.attributes" +
                    ") from Asset a where a.id in :assetIds and a.realm = :realm order by a.createdOn asc",
                ProtectedAssetInfo.class
            ).setParameter("realm", realm)
                .setParameter("assetIds", Arrays.asList(assetIds))
                .getResultList();
            return result.toArray(new ProtectedAssetInfo[result.size()]);
        });
    }

    /**
     * Find protected assets linked to a user.
     *
     * @param realm    The realm in which the assets must be.
     * @param username The username of the user.
     * @return The found assets or an empty array
     */
    public ProtectedAssetInfo[] findProtectedOfUser(String realm, String username) {
        return persistenceService.doReturningTransaction(em -> {
            Query userAttributesQuery = em.createNativeQuery(
                "SELECT ua.NAME, ua.VALUE " +
                    "FROM USER_ATTRIBUTE ua JOIN USER_ENTITY u ON u.ID = ua.USER_ID " +
                    "WHERE u.USERNAME = :username"
            );
            @SuppressWarnings("unchecked")
            String[] assetIds = ProtectedUserAssets.getAssetIdsFromUserAttributes(
                userAttributesQuery.setParameter("username", username).getResultList()
            );
            return findProtectedById(realm, assetIds);
        });
    }

    /**
     * Find assets by type in all realms.
     *
     * @param assetType The type of the assets to find.
     * @return The found assets or an empty array
     */
    public ServerAsset[] findByType(String assetType) {
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

    /**
     * Find assets by type in a given realm.
     *
     * @param realm     The realm in which the assets must be.
     * @param assetType The type of the assets to find.
     * @return The found assets or an empty array
     */
    public ServerAsset[] findByType(String realm, String assetType) {
        if (realm == null || realm.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result =
                em.createQuery(
                    "select a from Asset a where a.realm = :realm and a.type = :assetType order by a.createdOn asc",
                    ServerAsset.class)
                    .setParameter("realm", realm)
                    .setParameter("assetType", assetType)
                    .getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    /**
     * Find children of the given asset by type.
     *
     * @param parentId  The ID of the parent asset.
     * @param assetType The well-known type of the assets to find.
     * @return The found assets or an empty array
     */
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

    /**
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is invalid.
     */
    public ServerAsset merge(ServerAsset asset) {
        return persistenceService.doReturningTransaction(em -> {
            // Validate realm
            if (!isRealmActive(asset.getRealm())) {
                throw new IllegalStateException("Realm not found: " + asset.getRealm());
            }
            // Validate parent
            if (asset.getParentId() != null) {
                // If this is a not a root asset...
                ServerAsset parent = loadAsset(em, asset.getParentId());
                // .. the parent must exist
                if (parent == null)
                    throw new IllegalStateException("Parent not found: " + asset.getParentId());
                // ... the parent can not be a child of the asset
                if (Arrays.asList(parent.getPath()).contains(asset.getId()))
                    throw new IllegalStateException("Invalid parent");
            }
            return em.merge(asset);
        });
    }

    /**
     * @return <code>true</code> if the asset was deleted, false if the asset still has children and can't be deleted.
     */
    public boolean delete(String assetId) {
        return persistenceService.doReturningTransaction(em -> {
            Asset asset = em.find(ServerAsset.class, assetId);
            if (asset != null) {
                if (findChildren(asset.getId()).length > 0)
                    return false;
                em.remove(asset);
            }
            return true;
        });
    }

    @Override
    public void accept(AssetStateChange<ServerAsset> stateChange) {
        if (!storeAttributeState(stateChange.getNewState())) {
            throw new RuntimeException("Database update failed, no rows updated");
        };
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

    protected boolean isRealmActive(String realm) {
        return persistenceService.doReturningTransaction(entityManager -> {
            // TODO Should also check NOT_BEFORE?
            @SuppressWarnings("unchecked")
            List<Object[]> result = entityManager.createNativeQuery(
                "SELECT ID FROM REALM WHERE ENABLED = TRUE AND NAME = :realm"
            ).setParameter("realm", realm).getResultList();
            return result.size() > 0;
        });
    }

    protected boolean storeAttributeState(AttributeState attributeState) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(connection -> {

                String assetId = attributeState.getAttributeRef().getEntityId();
                String attributeName = attributeState.getAttributeRef().getAttributeName();
                JsonValue value = attributeState.getValue();

                String update =
                    "UPDATE ASSET" +
                        " SET ATTRIBUTES = jsonb_set(ATTRIBUTES, ?, ?, TRUE)" +
                        " WHERE ID = ? AND ATTRIBUTES -> ? IS NOT NULL";
                try (PreparedStatement statement = connection.prepareStatement(update)) {

                    Array attributePath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "value"}
                    );
                    statement.setArray(1, attributePath);

                    PGobject pgJsonValue = new PGobject();
                    pgJsonValue.setType("jsonb");
                    pgJsonValue.setValue(value.toJson());
                    statement.setObject(2, pgJsonValue);

                    statement.setString(3, assetId);

                    statement.setString(4, attributeName);

                    int updatedRows = statement.executeUpdate();
                    LOG.fine("Stored asset '" + assetId + "' attribute '" + attributeName + "' value, affected rows: " + updatedRows);
                    return updatedRows == 1;
                }
            })
        );
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}