/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.manager.dashboard;

import jakarta.persistence.Query;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.dashboard.DashboardAccess;
import org.openremote.model.query.DashboardQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DashboardStorageService extends RouteBuilder implements ContainerService {

    protected static final Logger LOG = Logger.getLogger(DashboardStorageService.class.getName());

    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;

    @Override
    public void configure() throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        persistenceService = container.getService(PersistenceService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        container.getService(ManagerWebService.class).addApiSingleton(
                new DashboardResourceImpl(
                        container.getService(TimerService.class),
                        identityService,
                        this,
                        container.getService(MessageBrokerService.class)
                )
        );
    }

    @Override
    public void start(Container container) throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */

    }

    /**
     * Pulls dashboards from the database based on the query object.
     * Useful to specifically filter and request dashboards.
     * @param dashboardQuery see {@link DashboardQuery} for specification
     * @return List of dashboards present in the database
     */
    protected Dashboard[] query(DashboardQuery dashboardQuery, String userId) {

        return (Dashboard[]) persistenceService.doReturningTransaction((em) -> {

            StringBuilder sql = new StringBuilder("SELECT * FROM Dashboard WHERE realm LIKE :realm");
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("realm", dashboardQuery.realm.name);

            if(dashboardQuery.ids != null) {
                this.appendSqlIdFilter(sql, dashboardQuery, parameters);
            }
            if(dashboardQuery.names != null) {
                this.appendSqlNamesFilter(sql, dashboardQuery, parameters);
            }
            if(dashboardQuery.userIds != null) {
                this.appendSqlUserIdsFilter(sql, dashboardQuery, parameters);
            }
            if(dashboardQuery.conditions.getDashboard() != null) {
                this.appendSqlDashboardConditionsFilter(sql, dashboardQuery, parameters, userId);
            }
            if(dashboardQuery.conditions.getAsset() != null) {
                this.appendSqlAssetConditionsFilter(sql, dashboardQuery, parameters, userId);
            }

            /** TODO: Implement SELECT filtering {@link org.openremote.model.query.DashboardQuery.Select} */

            // Apply pagination
            if(dashboardQuery.start != null) {
                sql.append(" OFFSET :start");
                parameters.put("start", dashboardQuery.start);
            }
            if(dashboardQuery.limit != null) {
                sql.append(" LIMIT :limit");
                parameters.put("limit", dashboardQuery.limit);
            }

            // Create query object and apply parameters
            Query query = em.createNativeQuery(sql.toString(), Dashboard.class);
            parameters.forEach(query::setParameter);

            return query.getResultList().toArray(new Dashboard[0]);
        });
    }

    /**
     * Builds SQL section for filtering dashboards by ID,
     * and applies the necessary data to the sqlBuilder and sqlParams parameters.
     * @return {@link StringBuilder} used for building
     */
    protected StringBuilder appendSqlIdFilter(StringBuilder sqlBuilder, DashboardQuery query, Map<String, Object> sqlParams) {
        sqlBuilder.append(" AND id IN (:ids)");
        sqlParams.put("ids", List.of(query.ids));
        return sqlBuilder;
    }

    /**
     * Builds SQL section for filtering dashboards by display name using {@link StringPredicate},
     * and applies the necessary data to the sqlBuilder and sqlParams parameters.
     * @return {@link StringBuilder} used for building
     */
    protected StringBuilder appendSqlNamesFilter(StringBuilder sqlBuilder, DashboardQuery query, Map<String, Object> sqlParams) {
        IntStream.range(0, query.names.length).forEach(index -> {
            String key = "name" + index;
            StringPredicate pred = query.names[index];
            sqlBuilder.append(" AND ").append(pred.caseSensitive ? "display_name" : "UPPER(display_name)").append(pred.negate ? " NOT" : "");
            switch (pred.match) {
                case BEGIN -> sqlBuilder.append(" LIKE :").append(key).append(" || '%'");
                case CONTAINS -> sqlBuilder.append(" LIKE '%' || :").append(key).append(" || '%'");
                case END -> sqlBuilder.append(" LIKE '%' || :").append(key);
                default -> sqlBuilder.append(" = :").append(key);
            }
            sqlParams.put(key, pred.value);
        });
        return sqlBuilder;
    }

    protected StringBuilder appendSqlUserIdsFilter(StringBuilder sqlBuilder, DashboardQuery query, Map<String, Object> sqlParams) {
        sqlBuilder.append(" AND owner_id IN (:ownerIds)");
        sqlParams.put("ownerIds", List.of(query.userIds));
        return sqlBuilder;
    }

    /**
     * Builds SQL section for filtering dashboards by dashboard conditions, such as {@link DashboardAccess} (PRIVATE, SHARED, PUBLIC),
     * and applies the necessary data to the sqlBuilder and sqlParams parameters.
     * @return {@link StringBuilder} used for building
     */
    protected StringBuilder appendSqlDashboardConditionsFilter(StringBuilder sqlBuilder, DashboardQuery query, Map<String, Object> sqlParams, String userId) {
        var dashboardConditions = query.conditions.getDashboard();
        if(dashboardConditions.getViewAccess() != null || dashboardConditions.getEditAccess() != null) {

            List<DashboardAccess> viewAccess = new ArrayList<>(Arrays.asList(dashboardConditions.getViewAccess()));
            List<DashboardAccess> editAccess = new ArrayList<>(Arrays.asList(dashboardConditions.getEditAccess()));
            sqlBuilder.append(" AND (");

            // When requesting PRIVATE dashboards, check whether the user is also the owner
            if(userId != null && viewAccess.contains(DashboardAccess.PRIVATE)) {
                viewAccess.remove(DashboardAccess.PRIVATE);
                sqlBuilder.append("(view_access = 2 AND owner_id = :userId) OR ");
                sqlParams.put("userId", userId);
            }
            if(userId != null && editAccess.contains(DashboardAccess.PRIVATE)) {
                editAccess.remove(DashboardAccess.PRIVATE);
                sqlBuilder.append("(edit_access = 2 AND owner_id = :userId) OR ");
                sqlParams.put("userId", userId);
            }

            // Append simple SQL filter by dashboard access
            sqlBuilder.append("(view_access IN (:viewAccess) OR edit_access IN (:editAccess)))");

            sqlParams.put("viewAccess", viewAccess.stream().map(DashboardAccess::ordinal).collect(Collectors.toList()));
            sqlParams.put("editAccess", editAccess.stream().map(DashboardAccess::ordinal).collect(Collectors.toList()));

            /** TODO: Implement filtering by {@link org.openremote.model.query.DashboardQuery.DashboardConditions.minWidgets} */

        }
        return sqlBuilder;
    }

    /**
     * Builds SQL section for filtering dashboards by assets present in the widgets.
     * For example, to only query dashboards when the user has access to the assets used.
     * Using {@link org.openremote.model.query.DashboardQuery.AssetAccess},
     * it applies the necessary data to the sqlBuilder and sqlParams parameters.
     * @return {@link StringBuilder} used for building
     */
    protected StringBuilder appendSqlAssetConditionsFilter(StringBuilder sqlBuilder, DashboardQuery query, Map<String, Object> sqlParams, String userId) {
        var assetConditions = query.conditions.getAsset();
        if(assetConditions.access != null) {
            List<DashboardQuery.AssetAccess> levels = Arrays.asList(assetConditions.access);
            if (levels.size() == 1 && levels.contains(DashboardQuery.AssetAccess.RESTRICTED)) {

                // Gather asset ids the user is linked to
                List<UserAssetLink> userAssetLinks = assetStorageService.findUserAssetLinks(query.realm.name, userId, null);
                List<String> assetIds = userAssetLinks.stream().map(ua -> ua.getId().getAssetId()).collect(Collectors.toList());

                // AT_LEAST_ONE - When user has access to the assets of at least 1 widget
                if(assetConditions.minAmount == DashboardQuery.ConditionMinAmount.AT_LEAST_ONE) {
                    sqlBuilder.append(" AND (template IS NULL OR template->'widgets' IS NULL OR EXISTS (");
                    sqlBuilder.append("SELECT 1 FROM jsonb_array_elements(COALESCE(template->'widgets', '[]')) AS j(widget) ");
                    sqlBuilder.append("LEFT JOIN jsonb_array_elements(COALESCE(widget->'widgetConfig'->'attributeRefs', '[]')) AS a(attributeRef) ");
                    sqlBuilder.append("ON a->>'id' IN (:assetIds)))");
                }
                // ALL - User needs access to the assets of ALL widgets (or the dashboard has no widgets)
                else if(assetConditions.minAmount == DashboardQuery.ConditionMinAmount.ALL) {
                    sqlBuilder.append(" AND NOT EXISTS (");
                    sqlBuilder.append("SELECT 1 FROM jsonb_array_elements(template->'widgets') AS j(widget), ");
                    sqlBuilder.append("jsonb_array_elements(widget->'widgetConfig'->'attributeRefs') AS a(attributeRef) ");
                    sqlBuilder.append("WHERE a->>'id' NOT IN (:assetIds))");
                }
                sqlParams.put("assetIds", assetIds);
            }
        }

        /** TODO: Implement filtering by ({@link org.openremote.model.query.DashboardQuery.AssetConditions.parents} */

        return sqlBuilder;
    }

    // Method to check if a dashboardId actually exists in the database
    // Useful for when query() does not return any accessible dashboard for that user, and check if it does however exist.
    protected boolean exists(String dashboardId, String realm) {
        if(dashboardId == null) {
            throw new IllegalArgumentException("No dashboardId is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        return this.query(new DashboardQuery()
                .realm(new RealmPredicate(realm))
                .ids(dashboardId),
                null
        ).length > 0;
    }


    // Creation of initial dashboard (so no updating!)
    protected Dashboard createNew(Dashboard dashboard) {
        if(dashboard == null) {
            throw new IllegalArgumentException("No dashboard is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {
            if(dashboard.getId() != null && dashboard.getId().length() > 0) {
                Dashboard d = em.find(Dashboard.class, dashboard.getId()); // checking whether dashboard is already in database
                if(d != null) {
                    throw new IllegalArgumentException("This dashboard has already been created.");
                }
            }
            return em.merge(dashboard);
        });
    }

    // Update of an existing dashboard
    protected Dashboard update(Dashboard dashboard, String realm, String userId) throws IllegalArgumentException {
        if(dashboard == null) {
            throw new IllegalArgumentException("No dashboard is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        if(userId == null) {
            throw new IllegalArgumentException("No userId is specified.");
        }
        // Query the dashboards with the same ID (which is only 1), and that userId is able to EDIT
        var query = new DashboardQuery()
                .ids(dashboard.getId())
                .realm(new RealmPredicate(realm))
                .userIds(userId)
                .limit(1)
                .conditions(new DashboardQuery.Conditions().dashboard(
                        new DashboardQuery.DashboardConditions()
                                .viewAccess(new DashboardAccess[]{})
                                .editAccess(new DashboardAccess[]{DashboardAccess.SHARED, DashboardAccess.PRIVATE})
                ));
        Dashboard[] dashboards = this.query(query, userId);
        if(dashboards != null && dashboards.length > 0) {
            Dashboard d = dashboards[0];
            return persistenceService.doReturningTransaction(em -> {
                dashboard.setVersion(d.getVersion());
                return em.merge(dashboard);
            });
        } else {
            throw new IllegalArgumentException("This dashboard does not exist!");
        }
    }

    protected boolean delete(String dashboardId, String realm, String userId) throws IllegalArgumentException {
        if(dashboardId == null) {
            throw new IllegalArgumentException("No dashboardId is specified.");
        }
        if(realm == null) {
            throw new IllegalArgumentException("No realm is specified.");
        }
        if(userId == null) {
            throw new IllegalArgumentException("No userId is specified.");
        }
        return persistenceService.doReturningTransaction(em -> {

            // Query the dashboards with the same ID (which is only 1), and that userId is able to EDIT
            Dashboard[] dashboards = this.query(new DashboardQuery()
                    .ids(dashboardId)
                    .realm(new RealmPredicate(realm))
                    .userIds(userId)
                    .limit(1)
                    .conditions(new DashboardQuery.Conditions().dashboard(
                            new DashboardQuery.DashboardConditions().editAccess(new DashboardAccess[]{ DashboardAccess.SHARED, DashboardAccess.PRIVATE })
                    )),
                    userId
            );
            if(dashboards == null || dashboards.length == 0) {
                throw new IllegalArgumentException("No dashboards could be found.");
            }
            Query query = em.createQuery("DELETE from Dashboard d where d.id=?1 and d.realm =?2");
            query.setParameter(1, dashboardId);
            query.setParameter(2, realm);
            query.executeUpdate();
            return true;
        });
    }
}
