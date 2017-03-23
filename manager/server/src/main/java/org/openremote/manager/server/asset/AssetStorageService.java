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

import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.hibernate.Session;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.agent.AgentAttributes;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.security.RealmView;
import org.openremote.model.*;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.ProtocolConfiguration;
import org.openremote.model.asset.UserAsset;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.model.asset.AssetType.AGENT;

public class AssetStorageService implements ContainerService, Consumer<AssetUpdate> {

    private static final Logger LOG = Logger.getLogger(AssetStorageService.class.getName());

    protected final Pattern attributeNamePattern = Pattern.compile(Attribute.ATTRIBUTE_NAME_PATTERN);

    protected PersistenceService persistenceService;
    protected ManagerIdentityService managerIdentityService;
    protected AssetProcessingService assetProcessingService;

    final protected Function<AttributeRef, ProtocolConfiguration> agentLinkResolver = agentLink -> {
        // Resolve the agent and the protocol configuration
        // TODO This is very inefficient and requires Hibernate second-level caching
        Asset agent = find(agentLink.getEntityId(), true);
        if (agent != null && agent.getWellKnownType().equals(AGENT)) {
            AgentAttributes agentAttributes = new AgentAttributes(agent);
            return agentAttributes.getProtocolConfiguration(agentLink.getAttributeName());
        }
        return null;
    };

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        managerIdentityService = container.getService(ManagerIdentityService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new AssetResourceImpl(
                managerIdentityService,
                this,
                container.getService(AssetProcessingService.class)
            )
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Function<AttributeRef, ProtocolConfiguration> getAgentLinkResolver() {
        return agentLinkResolver;
    }

    /**
     * Find assets without a parent in a realm.
     *
     * @param realmId      The realm to search.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The root assets of the realm ordered ascending by creation date, or an empty array if there is no data.
     */
    public ServerAsset[] findRoot(String realmId, boolean loadComplete) {
        if (realmId == null || realmId.length() == 0)
            throw new IllegalArgumentException("Realm must be provided to query assets");
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ServerAsset.class, null, null, realmId, null, null, true, loadComplete, null
            )).getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    /**
     * Retrieve the children of an asset.
     *
     * @param parentId     The ID of the parent asset.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The child assets ordered ascending by creation date, or an empty array if there is no data.
     */
    public ServerAsset[] findChildren(String parentId, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ServerAsset.class, null, parentId, null, null, null, false, loadComplete, null
            )).getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    /**
     * Retrieve the children of an asset in a particular realm.
     *
     * @param parentId     The ID of the parent asset.
     * @param realmId      The realm in which both parent asset and its children must be.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The child assets ordered ascending by creation date, or an empty array if there is no data.
     */
    public ServerAsset[] findChildrenInRealm(String parentId, String realmId, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ServerAsset.class, null, parentId, realmId, null, null, false, loadComplete, null
            )).getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    /**
     * Find an asset by primary key and populate all transient details (path, tenant).
     *
     * @param assetId      The primary key identifier value of the asset.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The asset or <code>null</code> if the asset doesn't exist.
     */
    public ServerAsset find(String assetId, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            try {
                return em.createQuery(buildAssetQuery(
                    em.getCriteriaBuilder(),
                    ServerAsset.class, assetId, null, null, null, null, false, loadComplete, null
                )).getSingleResult();
            } catch (NoResultException ex) {
                return null;
            }
        });
    }

    /**
     * Find assets by type in all realms.
     *
     * @param assetType    The type of the assets to find.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The found assets or an empty array
     */
    public ServerAsset[] findByType(String assetType, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ServerAsset.class, null, null, null, null, assetType, false, loadComplete, null
            )).getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }

    /**
     * Find children of the given asset by type.
     *
     * @param parentId     The ID of the parent asset.
     * @param assetType    The well-known type of the assets to find.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The found assets or an empty array
     */
    public ServerAsset[] findChildrenByType(String parentId, AssetType assetType, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            List<ServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ServerAsset.class, null, parentId, null, null, assetType.getValue(), false, loadComplete, null
            )).getResultList();
            return result.toArray(new ServerAsset[result.size()]);
        });
    }


    /**
     * Find protected assets linked to a user.
     *
     * @param userId       The primary key identifier value of the user.
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     * @return The found assets or an empty array.
     */
    public ProtectedServerAsset[] findProtectedOfUser(String userId, boolean loadComplete) {
        return persistenceService.doReturningTransaction(em -> {
            List<ProtectedServerAsset> result = em.createQuery(buildAssetQuery(
                em.getCriteriaBuilder(),
                ProtectedServerAsset.class, null, null, null, userId, null, false, loadComplete, null
            )).getResultList();
            return result.toArray(new ProtectedServerAsset[result.size()]);
        });
    }

    /**
     * Confirm if protected assets linked to a user contains given asset.
     *
     * @return The found assets or an empty array.
     */
    public boolean findProtectedOfUserContains(String userId, String assetId) {
        return persistenceService.doReturningTransaction(em -> {
            List result = em.createQuery(
                "select ua.assetId from UserAsset ua where ua.userId = :userId and ua.assetId = :assetId"
            ).setParameter("assetId", assetId).setParameter("userId", userId).getResultList();
            return result.size() > 0;
        });
    }

    public void storeProtected(String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = new UserAsset(userId, assetId);
            entityManager.merge(userAsset);
        });
    }

    public void deleteProtected(String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = entityManager.find(UserAsset.class, new UserAsset(userId, assetId));
            if (userAsset != null) {
                entityManager.remove(userAsset);
            }
        });
    }

    /**
     * @return The current stored asset state.
     * @throws IllegalArgumentException if the realm or parent is illegal, or other asset constraint is violated.
     */
    public ServerAsset merge(ServerAsset asset) {
        return persistenceService.doReturningTransaction(em -> {
            // Validate realm
            if (!managerIdentityService.isActiveTenantRealmId(asset.getRealmId())) {
                throw new IllegalStateException("Realm not found/active: " + asset.getRealmId());
            }
            // Validate parent
            if (asset.getParentId() != null) {
                // If this is a not a root asset...
                ServerAsset parent = loadAsset(em, asset.getParentId(), true);
                // .. the parent must exist
                if (parent == null)
                    throw new IllegalStateException("Parent not found: " + asset.getParentId());
                // ... the parent can not be a child of the asset
                if (parent.pathContains(asset.getId()))
                    throw new IllegalStateException("Invalid parent");
            }
            // Validate attribute names
            Attributes attributes = new Attributes(asset.getAttributes());
            for (Attribute attribute : attributes.get()) {
                if (!attributeNamePattern.matcher(attribute.getName()).matches()) {
                    throw new IllegalStateException(
                        "Invalid attribute name (must match '" + Attribute.ATTRIBUTE_NAME_PATTERN + "'): " + attribute.getName()
                    );
                }
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
                if (findChildren(asset.getId(), false).length > 0)
                    return false;
                em.remove(asset);
            }
            return true;
        });
    }

    // Here for convenience - a single service to handle Asset/AssetAttribute events
    public void updateAttributeValue(AttributeEvent attributeEvent) {
        assetProcessingService.updateAttributeValue(attributeEvent);
    }

    @Override
    public void accept(AssetUpdate assetUpdate) {
        String assetId = assetUpdate.getAsset().getId();
        String attributeName = assetUpdate.getAttribute().getName();
        JsonValue value = assetUpdate.getAttribute().getValue();
        // Some sanity checking, of course the timestamp should never be -1 if we store updated attribute state
        long timestamp = assetUpdate.getAttribute().getValueTimestamp();
        String valueTimestamp = Long.toString(
            timestamp >= 0 ? timestamp : System.currentTimeMillis()
        );
        if (!storeAttributeValue(assetId, attributeName, value, valueTimestamp)) {
            throw new RuntimeException("Database update failed, no rows updated");
        }
    }

    protected ServerAsset loadAsset(EntityManager em, String assetId, boolean loadDetails) {
        ServerAsset asset = em.find(ServerAsset.class, assetId);
        if (asset == null)
            return null;
        if (!loadDetails)
            return asset;

        asset.setTenantRealm(managerIdentityService.getActiveTenantRealm(asset.getRealmId()));
        asset.setTenantDisplayName(managerIdentityService.getActiveTenantDisplayName(asset.getRealmId()));

        asset.setPath(em.unwrap(Session.class).doReturningWork(connection -> {
            String query =
                "with recursive ASSET_TREE(ID, PARENT_ID, PATH) as (" +
                    " select A1.ID, A1.PARENT_ID, array[text(A1.ID)] from ASSET A1 where A1.PARENT_ID is null" +
                    " union all" +
                    " select A2.ID, A2.PARENT_ID, array_append(AT.PATH, text(A2.ID)) from ASSET A2, ASSET_TREE AT where A2.PARENT_ID = AT.ID" +
                    ") select PATH from ASSET_TREE where ID = ?";

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

    protected boolean storeAttributeValue(String assetId, String attributeName, JsonValue value, String timestamp) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(connection -> {

                String update =
                    "update ASSET" +
                        " set ATTRIBUTES = jsonb_set(jsonb_set(ATTRIBUTES, ?, ?, true), ?, ?, true)" +
                        " where ID = ? and ATTRIBUTES -> ? is not null";
                try (PreparedStatement statement = connection.prepareStatement(update)) {

                    // Bind the value (and check we don't have a SQL injection hole in attribute name!)
                    if (!attributeNamePattern.matcher(attributeName).matches()) {
                        LOG.fine(
                            "Invalid attribute name (must match '" + Attribute.ATTRIBUTE_NAME_PATTERN + "'): " + attributeName
                        );
                        return false;
                    }

                    Array attributeValuePath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "value"}
                    );
                    statement.setArray(1, attributeValuePath);

                    PGobject pgJsonValue = new PGobject();
                    pgJsonValue.setType("jsonb");
                    // Careful, do not set Java null (as returned by value.toJson()) here! It will erase your whole SQL column!
                    pgJsonValue.setValue(value == null || value.getType() == JsonType.NULL ? "null" : value.toJson());
                    statement.setObject(2, pgJsonValue);

                    // Bind the value timestamp
                    Array attributeValueTimestampPath = connection.createArrayOf(
                        "text",
                        new String[]{attributeName, "valueTimestamp"}
                    );
                    statement.setArray(3, attributeValueTimestampPath);
                    PGobject pgJsonValueTimestamp = new PGobject();
                    pgJsonValueTimestamp.setType("jsonb");
                    pgJsonValueTimestamp.setValue(timestamp);
                    statement.setObject(4, pgJsonValueTimestamp);

                    // Bind asset ID and attribute name
                    statement.setString(5, assetId);
                    statement.setString(6, attributeName);

                    int updatedRows = statement.executeUpdate();
                    LOG.fine("Stored asset '" + assetId + "' attribute '" + attributeName + "' value, affected rows: " + updatedRows);
                    return updatedRows == 1;
                }
            })
        );
    }

    protected <T extends Asset> CriteriaQuery<T> buildAssetQuery(CriteriaBuilder cb,
                                                                 Class<T> resultClass,
                                                                 String assetId,
                                                                 String parentAssetId,
                                                                 String realmId,
                                                                 String userId,
                                                                 String type,
                                                                 boolean withoutParent,
                                                                 boolean loadComplete,
                                                                 String orderBy) {
        CriteriaQuery<T> criteria = cb.createQuery(resultClass);
        Root<ServerAsset> assetRoot = criteria.from(ServerAsset.class);
        Root<ServerAsset> assetParentRoot = criteria.from(ServerAsset.class);
        Root<RealmView> realmViewRoot = criteria.from(RealmView.class);
        Root<UserAsset> userAssetRoot = userId != null ? criteria.from(UserAsset.class) : null;
        List<Predicate> predicates = new ArrayList<Predicate>() {{
            add(cb.equal(assetRoot.get("id"), assetParentRoot.get("id")));
            add(cb.equal(assetRoot.get("realmId"), realmViewRoot.get("id")));
            if (assetId != null) {
                add(cb.equal(assetRoot.get("id"), assetId));
            }
            if (!withoutParent && parentAssetId != null) {
                add(cb.equal(assetRoot.get("parentId"), parentAssetId));
            } else if (withoutParent) {
                add(cb.isNull(assetRoot.get("parentId")));
            }
            if (realmId != null) {
                add(cb.equal(assetRoot.get("realmId"), realmId));
            }
            if (type != null) {
                add(cb.equal(assetRoot.get("type"), type));
            }
            if (userId != null) {
                add(cb.equal(userAssetRoot.get("userId"), userId));
                add(cb.equal(assetRoot.get("id"), userAssetRoot.get("assetId")));
            }
        }};
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        if (assetId == null) {
            criteria.orderBy(cb.asc(assetRoot.get(orderBy != null ? orderBy : "createdOn")));
        }
        List<Selection> projectionArgs = new ArrayList<Selection>() {{
            add(assetRoot.get("id"));
            add(assetRoot.get("version"));
            add(assetRoot.get("createdOn"));
            add(assetRoot.get("name"));
            add(assetRoot.get("type"));
            add(assetRoot.get("parentId"));
            add(assetParentRoot.get("name"));
            add(assetParentRoot.get("type"));
            if (loadComplete)
                add(assetRoot.get("path"));
            add(assetRoot.get("realmId"));
            add(realmViewRoot.get("name"));
            add(realmViewRoot.get("displayName"));
            add(assetRoot.get("location"));
            if (loadComplete)
                add(assetRoot.get("attributes"));
        }};
        return criteria.select(
            cb.construct(
                resultClass,
                projectionArgs.toArray(new Selection[projectionArgs.size()])
            )
        );
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}