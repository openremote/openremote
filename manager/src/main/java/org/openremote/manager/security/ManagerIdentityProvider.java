/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.security;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.IdentityProvider;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.openremote.model.Constants.MASTER_REALM;

// TODO: Normalise interface for Basic and Keycloak providers and add client CRUD
/**
 * SPI for implementations used by {@link ManagerIdentityService}, provides CRUD of
 * {@link User} and {@link Tenant}.
 */
public interface ManagerIdentityProvider extends IdentityProvider {

    User[] getUsers(String realm);

    User[] getUsers(List<String> userIds);

    User[] getUsers(UserQuery userQuery);

    User getUser(String realm, String userId);

    User getUserByUsername(String realm, String username);

    void updateUser(String realm, User user);

    User createUser(String realm, User user, String password);

    void deleteUser(String realm, String userId);

    void resetPassword(String realm, String userId, Credential credential);

    Role[] getRoles(String realm, String userId);

    void updateRoles(String realm, String username, ClientRole[] roles);

    boolean isMasterRealmAdmin(String userId);

    boolean isRestrictedUser(String userId);

    boolean isUserInTenant(String userId, String realm);

    Tenant[] getTenants();

    Tenant getTenant(String realm);

    void updateTenant(Tenant tenant);

    Tenant createTenant(Tenant tenant);

    void deleteTenant(String realm);

    boolean isTenantActiveAndAccessible(AuthContext authContext, Tenant tenant);

    boolean isTenantActiveAndAccessible(AuthContext authContext, String realm);

    boolean tenantExists(String realm);

    /**
     * Superusers can subscribe to all events, regular users must be in the same realm as the filter and any
     * required roles must match. If the authenticated party is a restricted user, this returns <code>false.</code>
     *
     * @return <code>true</code> if the authenticated party can subscribe to events with the given filter.
     */
    boolean canSubscribeWith(AuthContext auth, TenantFilter filter, ClientRole... requiredRoles);


    /*
     * BELOW ARE STATIC HELPER METHODS
     */

    static User[] getUsersFromDb(PersistenceService persistenceService, UserQuery userQuery) {
        StringBuilder sb = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        // BUILD SELECT
        sb.append("SELECT DISTINCT(u)");

        // BUILD FROM
        sb.append(" FROM User u");
        if (userQuery.assetPredicate != null || userQuery.pathPredicate != null) {
            sb.append(" join UserAsset ua on ua.id.userId = u.id");
        }

        // BUILD WHERE
        sb.append(" WHERE 1=1");
        if (userQuery.tenantPredicate != null && !TextUtil.isNullOrEmpty(userQuery.tenantPredicate.realm)) {
            sb.append(" AND u.realm = ?").append(parameters.size() + 1);
            parameters.add(userQuery.tenantPredicate.realm);
        }
        if (userQuery.assetPredicate != null) {
            sb.append(" AND ua.id.assetId = ?").append(parameters.size() + 1);
            parameters.add(userQuery.assetPredicate.id);
        }
        if (userQuery.pathPredicate != null) {
            sb.append(" AND ?").append(parameters.size() + 1).append(" <@ get_asset_tree_path(ua.asset_id)");
            parameters.add(userQuery.pathPredicate.path);
        }
        if (userQuery.ids != null && userQuery.ids.length > 0) {
            sb.append(" AND u.id IN (?").append(parameters.size() + 1);
            parameters.add(userQuery.ids[0]);

            for (int i = 1; i < userQuery.ids.length; i++) {
                sb.append(",?").append(parameters.size() + 1);
                parameters.add(userQuery.ids[i]);
            }
            sb.append(")");
        }
        if (userQuery.usernames != null && userQuery.usernames.length > 0) {
            sb.append(" AND u.username IN (?").append(parameters.size() + 1);
            parameters.add(userQuery.usernames[0]);

            for (int i = 1; i < userQuery.usernames.length; i++) {
                sb.append(",?").append(parameters.size() + 1);
                parameters.add(userQuery.usernames[i]);
            }
            sb.append(")");
        }

        // BUILD LIMIT
        if (userQuery.limit > 0) {
            sb.append(" LIMIT ").append(userQuery.limit);
        }

        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<User> query = entityManager.createQuery(sb.toString(), User.class);
            IntStream.range(0, parameters.size()).forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            List<User> users = query.getResultList();
            return users.toArray(new User[users.size()]);
        });
    }

    static User getUserByUsernameFromDb(PersistenceService persistenceService, String realm, String username) {
        return persistenceService.doReturningTransaction(em -> {
            List<User> result =
                em.createQuery("select u from User u where u.realm = :realm and u.username = :username", User.class)
                    .setParameter("realm", realm)
                    .setParameter("username", username)
                    .getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    static User getUserByIdFromDb(PersistenceService persistenceService, String realm, String userId) {
        return persistenceService.doReturningTransaction(em -> {
            List<User> result =
                em.createQuery("select u from User u where u.realm = :realm and u.id = :userId", User.class)
                    .setParameter("realm", realm)
                    .setParameter("userId", userId)
                    .getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    static Tenant[] getTenantsFromDb(PersistenceService persistenceService) {
        return persistenceService.doReturningTransaction(entityManager -> {
            List<Tenant> realms = entityManager.createQuery(
                "select t from Tenant t where t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())"
                , Tenant.class).getResultList();

            // Make sure the master tenant is always on top
            realms.sort((o1, o2) -> {
                if (o1.getRealm().equals(MASTER_REALM))
                    return -1;
                if (o2.getRealm().equals(MASTER_REALM))
                    return 1;
                return o1.getRealm().compareTo(o2.getRealm());
            });

            return realms.toArray(new Tenant[realms.size()]);
        });
    }

    static Tenant getTenantFromDb(PersistenceService persistenceService, String realm) {
        return persistenceService.doReturningTransaction(em -> {
                List<Tenant> tenants = em.createQuery("select t from Tenant t where t.realm = :realm and t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())", Tenant.class)
                    .setParameter("realm", realm).getResultList();
                return tenants.size() == 1 ? tenants.get(0) : null;
            }
        );
    }

    static boolean tenantExistsFromDb(PersistenceService persistenceService, String realm) {
        return persistenceService.doReturningTransaction(em -> {

            long count = em.createQuery(
                "select count(t) from Tenant t where t.realm = :realm and t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())",
                Long.class).setParameter("realm", realm).getSingleResult();

            return count > 0;
        });
    }

    static boolean userInTenantFromDb(PersistenceService persistenceService, String userId, String realm) {
        return persistenceService.doReturningTransaction(em -> {
            User user = em.find(User.class, userId);
            return (user != null && realm.equals(user.getRealm()));
        });
    }
}
