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
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.util.Pair;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.agent.AgentAttributes;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.*;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.ProtocolConfiguration;
import org.openremote.model.asset.UserAsset;
import org.postgresql.util.PGobject;

import javax.persistence.EntityManager;
import java.sql.*;
import java.util.ArrayList;
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
        Asset agent = find(agentLink.getEntityId(), true, false);
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

    public Function<AttributeRef, ProtocolConfiguration> getAgentLinkResolver() {
        return agentLinkResolver;
    }

    public ServerAsset find(String assetId) {
        return find(new AssetQuery().id(assetId));
    }

    /**
     * @param loadComplete If the whole asset data (including path and attributes) should be loaded.
     */
    public ServerAsset find(String assetId, boolean loadComplete) {
        return find(new AssetQuery().select(new AssetQuery.Select(loadComplete, false)).id(assetId));
    }

    /**
     * @param loadComplete    If the whole asset data (including path and attributes) should be loaded.
     * @param filterProtected If the asset attributes should be filtered and only protected details included.
     */
    public ServerAsset find(String assetId, boolean loadComplete, boolean filterProtected) {
        return find(new AssetQuery().select(new AssetQuery.Select(loadComplete, filterProtected)).id(assetId));
    }

    public ServerAsset find(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> find(em, query));
    }

    public List<ServerAsset> findAll(AssetQuery query) {
        return persistenceService.doReturningTransaction(em -> findAll(em, query));
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
                ServerAsset parent = find(em, asset.getParentId(), true, false);
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
                List<ServerAsset> children = findAll(em, new AssetQuery()
                    .parent(new AssetQuery.Parent(asset.getId()))
                );
                if (children.size() > 0)
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

    public boolean isUserAsset(String userId, String assetId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            UserAsset userAsset = entityManager.find(UserAsset.class, new UserAsset(userId, assetId));
            return userAsset != null;
        });
    }

    public void storeUserAsset(String userId, String assetId) {
        persistenceService.doTransaction(entityManager -> {
            UserAsset userAsset = new UserAsset(userId, assetId);
            entityManager.merge(userAsset);
        });
    }

    /* ####################################################################################### */

    protected interface ParameterBinder extends Consumer<PreparedStatement> {
        @Override
        default void accept(PreparedStatement st) {
            try {
                acceptStatement(st);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        void acceptStatement(PreparedStatement st) throws SQLException;
    }

    protected ServerAsset find(EntityManager em, String assetId, boolean loadComplete, boolean filterProtected) {
        return find(em, new AssetQuery().select(new AssetQuery.Select(loadComplete, filterProtected)).id(assetId));
    }

    public ServerAsset find(EntityManager em, AssetQuery query) {
        List<ServerAsset> result = findAll(em, query);
        if (result.size() == 0)
            return null;
        if (result.size() > 1) {
            throw new IllegalArgumentException("Query returned more than one asset");
        }
        return result.get(0);

    }

    protected List<ServerAsset> findAll(EntityManager em, AssetQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSelectString(query));
        sb.append(buildFromString(query));
        Pair<String, List<ParameterBinder>> whereClause = buildWhereClause(query);
        sb.append(whereClause.key);
        return em.unwrap(Session.class).doReturningWork(new AbstractReturningWork<List<ServerAsset>>() {
            @Override
            public List<ServerAsset> execute(Connection connection) throws SQLException {
                LOG.fine("Preparing asset query: " + sb.toString());
                PreparedStatement st = connection.prepareStatement(sb.toString());
                LOG.fine("Query binders: " + whereClause.value.size());
                for (ParameterBinder binder : whereClause.value) {
                    binder.accept(st);
                }
                try (ResultSet rs = st.executeQuery()) {
                    List<ServerAsset> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapResultTuple(query, rs));
                    }
                    return result;
                }
            }
        });
    }

    protected String buildSelectString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append("select ");
        sb.append("A.ID as ID, A.OBJ_VERSION as OBJ_VERSION, A.CREATED_ON as CREATED_ON, A.NAME as NAME, A.ASSET_TYPE as ASSET_TYPE, ");
        sb.append("A.PARENT_ID as PARENT_ID, ");
        sb.append(query.parent != null && query.parent.noParent
            ? " NULL PARENT_NAME, NULL as PARENT_TYPE, "
            : " P.NAME as PARENT_NAME, P.ASSET_TYPE as PARENT_TYPE, "
        );
        sb.append("A.REALM_ID as REALM_ID, R.NAME as TENANT_NAME, ");
        sb.append("(select RA.VALUE from REALM_ATTRIBUTE RA where RA.REALM_ID = R.ID and RA.NAME = 'displayName') as TENANT_DISPLAY_NAME, ");
        sb.append("A.LOCATION as LOCATION");

        if (query.select != null && query.select.loadComplete) {
            sb.append(", get_asset_tree_path(A.ID) as PATH ");
            sb.append(", A.ATTRIBUTES as ATTRIBUTES");
        } else {
            sb.append(", NULL as PATH, NULL as ATTRIBUTES");
        }

        return sb.toString();
    }

    protected String buildFromString(AssetQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append(" from ASSET A cross join (select * from REALM) as R ");


        if (query.parent != null && !query.parent.noParent) {
            sb.append(" cross join ASSET P ");
        } else {
            sb.append(" left outer join ASSET P on A.PARENT_ID = P.ID");
        }

        if (query.id != null) {
            return sb.toString();
        }

        if (query.userId != null) {
            sb.append(" cross join USER_ASSET ua ");
        }

        if (query.hasAttributeRestrictions()) {
            sb.append(" cross join jsonb_each(A.ATTRIBUTES) as AX ");
            sb.append(" cross join jsonb_array_elements(jsonb_extract_path(AX.VALUE, 'meta')) as AM ");
        }

        return sb.toString();
    }

    protected Pair<String, List<ParameterBinder>> buildWhereClause(AssetQuery query) {
        StringBuilder sb = new StringBuilder();
        List<ParameterBinder> binders = new ArrayList<>();

        sb.append(" where r.ID = a.REALM_ID ");

        if (query.id != null) {
            sb.append(" and A.ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.id));
            return new Pair<>(sb.toString(), binders);
        }

        if (query.name != null) {
            sb.append(query.name.caseSensitive ? " and A.NAME " : " and upper(A.NAME) ");
            sb.append(query.name.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.name.prepareValue()));
        }

        if (query.parent != null) {
            if (query.parent.id != null) {
                sb.append(" and p.ID = a.PARENT_ID ");
                sb.append(" and A.PARENT_ID = ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parent.id));
            } else if (query.parent.type != null) {
                sb.append(" and p.ID = a.PARENT_ID ");
                sb.append(" and P.ASSET_TYPE = ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.parent.type));
            } else if (query.parent.noParent) {
                sb.append(" and A.PARENT_ID is null ");
            } else {
            }
        }

        if (query.realm != null && query.realm.id != null) {
            sb.append(" and R.ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.realm.id));
        } else if (query.realm != null && query.realm.name != null) {
            sb.append(" and R.NAME = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.realm.id));
        }

        if (query.userId != null) {
            sb.append(" and ua.ASSET_ID = a.ID and ua.USER_ID = ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.userId));
        }

        if (query.type != null) {
            sb.append(query.type.caseSensitive ? " and A.ASSET_TYPE" : " and upper(A.ASSET_TYPE) ");
            sb.append(query.type.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
            final int pos = binders.size() + 1;
            binders.add(st -> st.setString(pos, query.type.prepareValue()));
        }

        if (query.attributeMeta != null) {
            if (query.attributeMeta.itemNameSearch != null) {
                sb.append(query.attributeMeta.itemNameSearch.caseSensitive
                    ? " and jsonb_extract_path_text(AM.VALUE, 'name') "
                    : " and upper(jsonb_extract_path_text(AM.VALUE, 'name')) "
                );
                sb.append(query.attributeMeta.itemNameSearch.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                final int pos = binders.size() + 1;
                binders.add(st -> st.setString(pos, query.attributeMeta.itemNameSearch.prepareValue()));
            }
            if (query.attributeMeta.itemValueSearch != null) {
                if (query.attributeMeta.itemValueSearch instanceof AssetQuery.StringSearch) {
                    AssetQuery.StringSearch stringSearch = (AssetQuery.StringSearch) query.attributeMeta.itemValueSearch;
                    sb.append(stringSearch.caseSensitive
                        ? " and jsonb_extract_path(AM.VALUE, 'value') "
                        : " and upper(jsonb_extract_path(AM.VALUE, 'value')) "
                    );
                    sb.append(stringSearch.match == AssetQuery.Match.EXACT ? " = ? " : " like ? ");
                    final int pos = binders.size() + 1;
                    binders.add(st -> st.setString(pos, stringSearch.prepareValue()));
                } else if (query.attributeMeta.itemValueSearch instanceof AssetQuery.BooleanSearch) {
                    AssetQuery.BooleanSearch booleanSearch = (AssetQuery.BooleanSearch) query.attributeMeta.itemValueSearch;
                    sb.append(" and jsonb_extract_path(AM.VALUE, 'value') = to_jsonb(")
                        .append(booleanSearch.predicate)
                        .append(") ");
                }
            }
            // TODO Implement AssetQuery.DecimalSearch
        }

        return new Pair<>(sb.toString(), binders);
    }

    protected ServerAsset mapResultTuple(AssetQuery query, ResultSet rs) throws SQLException {
        LOG.fine("Mapping asset query result tuple: " + rs.getString("ID"));

        return new ServerAsset(
            query.select != null && query.select.filterProtected,
            rs.getString("ID"), rs.getLong("OBJ_VERSION"), rs.getTimestamp("CREATED_ON"), rs.getString("NAME"), rs.getString("ASSET_TYPE"),
            rs.getString("PARENT_ID"), rs.getString("PARENT_NAME"), rs.getString("PARENT_TYPE"),
            rs.getString("REALM_ID"), rs.getString("TENANT_NAME"), rs.getString("TENANT_DISPLAY_NAME"),
            rs.getObject("LOCATION"), rs.getArray("PATH"), rs.getString("ATTRIBUTES")
        );
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

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}