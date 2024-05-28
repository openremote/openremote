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

import jakarta.persistence.Query;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.IdentityProvider;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import jakarta.persistence.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.model.query.filter.StringPredicate.toSQLParameter;
import static org.openremote.model.Constants.MASTER_REALM;

// TODO: Normalise interface for Basic and Keycloak providers and add client CRUD
/**
 * SPI for implementations used by {@link ManagerIdentityService}, provides CRUD of
 * {@link User} and {@link Realm}.
 */
public interface ManagerIdentityProvider extends IdentityProvider {

    User[] queryUsers(UserQuery userQuery);

    User getUser(String userId);

    User getUserByUsername(String realm, String username);

    User createUpdateUser(String realm, User user, String password, boolean allowUpdate);

    void deleteUser(String realm, String userId);

    void resetPassword(String realm, String userId, Credential credential);

    String resetSecret(String realm, String userId, String secret);

    Role[] getRoles(String realm, String client);

    void updateClientRoles(String realm, String client, Role[] roles);

    Role[] getUserRoles(String realm, String userId, String client);

    Role[] getUserRealmRoles(String realm, String userId);

    void updateUserRoles(String realm, String userId, String client, String...roles);

    void updateUserRealmRoles(String realm, String userId, String...roles);

    boolean isMasterRealmAdmin(String userId);

    boolean isRestrictedUser(AuthContext authContext);

    boolean isUserInRealm(String userId, String realm);

    Realm[] getRealms();

    Realm getRealm(String realm);

    void updateRealm(Realm realm);

    Realm createRealm(Realm realm);

    void deleteRealm(String realm);

    boolean isRealmActiveAndAccessible(AuthContext authContext, Realm realm);

    boolean isRealmActiveAndAccessible(AuthContext authContext, String realm);

    boolean realmExists(String realm);

    /**
     * Superusers can subscribe to all events, regular users must be in the same realm as the filter and any
     * required roles must match. If the authenticated party is a restricted user, this returns <code>false.</code>
     *
     * @return <code>true</code> if the authenticated party can subscribe to events with the given filter.
     */
    boolean canSubscribeWith(AuthContext auth, RealmFilter<?> filter, ClientRole... requiredRoles);

    /**
     * Returns the frontend URL to be used for frontend apps to authenticate
     */
    String getFrontendURI();

    /*
     * BELOW ARE STATIC HELPER METHODS
     */

    default String[] addRealmRoles(String realm, String userId, String...roles) {
        Set<String> realmRoles = Arrays.stream(getUserRealmRoles(realm, userId)).filter(role -> role.isAssigned() || Arrays.stream(roles).anyMatch(r -> role.getName().equals(r))).map(Role::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        realmRoles.addAll(Arrays.asList(roles));
        return realmRoles.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    static User[] getUsersFromDb(PersistenceService persistenceService, UserQuery query) {
        StringBuilder sb = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        final UserQuery userQuery = query != null ? query : new UserQuery();

        // Limit to regular/service users
        if (userQuery.serviceUsers != null) {
            // Not ideal mutating the query but seems excessive cloning it
            if (userQuery.usernames == null) {
                userQuery.usernames = new StringPredicate[1];
                userQuery.usernames(new StringPredicate(AssetQuery.Match.BEGIN, User.SERVICE_ACCOUNT_PREFIX).negate(!userQuery.serviceUsers));
            } else {
                userQuery.usernames = Arrays.copyOf(userQuery.usernames, userQuery.usernames.length+1);
                userQuery.usernames[userQuery.usernames.length-1] = new StringPredicate(AssetQuery.Match.BEGIN, User.SERVICE_ACCOUNT_PREFIX).negate(!userQuery.serviceUsers);
            }
        }

        // BUILD SELECT
        sb.append("SELECT u.*, (SELECT C.SECRET FROM PUBLIC.CLIENT C WHERE C.ID = SERVICE_ACCOUNT_CLIENT_LINK) as secret, r.name as realm");

        // BUILD FROM
        sb.append(" FROM public.user_entity u join PUBLIC.REALM r on r.ID = u.REALM_ID");
        if (userQuery.assets != null || userQuery.pathPredicate != null) {
            sb.append(" join user_asset_link ua on ua.user_id = u.id");
        }

        // BUILD WHERE
        sb.append(" WHERE TRUE");
        if (userQuery.realmPredicate != null && !TextUtil.isNullOrEmpty(userQuery.realmPredicate.name)) {
            sb.append(" AND r.name = ?").append(parameters.size() + 1);
            parameters.add(userQuery.realmPredicate.name);
        }
        if (userQuery.assets != null) {
            sb.append(" AND ua.asset_id IN (?").append(parameters.size() + 1).append(")");
            parameters.add(Arrays.asList(userQuery.assets));
        }
        if (userQuery.pathPredicate != null && userQuery.pathPredicate.path != null && userQuery.pathPredicate.path.length > 0) {
            sb.append(" AND ?").append(parameters.size() + 1).append("\\:\\:text[] <@ get_asset_tree_path(ua.asset_id)");
            parameters.add("{" + String.join(",", userQuery.pathPredicate.path) + "}");
        }
        if (userQuery.ids != null && userQuery.ids.length > 0) {
            sb.append(" AND u.id IN (?").append(parameters.size() + 1).append(")");
            parameters.add(Arrays.asList(userQuery.ids));
        }
        if (userQuery.usernames != null && userQuery.usernames.length > 0) {
            sb.append(" and (FALSE");

            for (StringPredicate pred : userQuery.usernames) {
                final int pos = parameters.size() + 1;
                // No case support for username
                pred.caseSensitive = false;
                sb.append(" or upper(u.username)");
                sb.append(toSQLParameter(pred, pos, false));
                parameters.add(pred.prepareValue());
            }
            sb.append(")");
        }
        if (userQuery.realmRoles != null && userQuery.realmRoles.length > 0) {
            sb.append(" and (FALSE");

            Arrays.stream(userQuery.realmRoles).forEach(realmPredicate -> {
                sb.append(" OR ");
                if (realmPredicate.negate) {
                    // Negation implies does not contain this match so use a sub query in the where clause with not
                    // exists otherwise it will just match any other row
                    sb.append("NOT ");
                }
                sb.append("EXISTS (SELECT urm.user_id from public.user_role_mapping urm join public.keycloak_role kr on urm.role_id = kr.id where urm.user_id = u.id and not kr.client_role and");

                sb.append(realmPredicate.caseSensitive ? " kr.name" : " upper(kr.name)");
                sb.append(StringPredicate.toSQLParameter(realmPredicate, parameters.size() + 1, realmPredicate.negate));
                parameters.add(realmPredicate.prepareValue());

                sb.append(")");
            });

            sb.append(")");
        }
        if (userQuery.attributes != null && userQuery.attributes.length > 0) {
            sb.append(" AND (TRUE");

            Arrays.stream(userQuery.attributes).forEach(attributePredicate -> {
                sb.append(" AND ");
                if (attributePredicate.negated) {
                    // Negation implies does not contain this match so use a sub query in the where clause with not
                    // exists otherwise it will just match any other row
                    sb.append("NOT ");
                }
                sb.append("EXISTS (SELECT att.user_id from public.user_attribute att where att.user_id = u.id and");

                sb.append(attributePredicate.name.caseSensitive ? " att.name" : " upper(att.name)");
                sb.append(StringPredicate.toSQLParameter(attributePredicate.name, parameters.size() + 1, attributePredicate.negated));
                parameters.add(attributePredicate.name.prepareValue());

                if (attributePredicate.value != null) {
                    sb.append(" and ");
                    sb.append(attributePredicate.name.caseSensitive ? "att.value" : "upper(att.value)");
                    sb.append(StringPredicate.toSQLParameter(attributePredicate.value, parameters.size() + 1, attributePredicate.negated));
                    parameters.add(attributePredicate.value.prepareValue());
                }

                sb.append(")");
            });

            sb.append(")");
        }

        // BUILD ORDER BY
        if (userQuery.orderBy != null) {
            if (userQuery.orderBy.property != null) {
                sb.append(" ORDER BY");
                switch(userQuery.orderBy.property) {
                    case CREATED_ON:
                        sb.append(" u.created_on");
                        break;
                    case FIRST_NAME:
                        sb.append(" u.first_name");
                        break;
                    case LAST_NAME:
                        sb.append(" u.last_name");
                        break;
                    case USERNAME:
                        // Remove service user prefix
                        sb.append(" replace(u.username, '").append(User.SERVICE_ACCOUNT_PREFIX).append("', '')");
                        break;
                    case EMAIL:
                        sb.append(" u.email");
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported order by value: " + userQuery.orderBy.property);
                }
                if (userQuery.orderBy.descending) {
                    sb.append(" DESC");
                }
            }
        }

        List<User> users = persistenceService.doReturningTransaction(entityManager -> {
            Query sqlQuery = entityManager.createNativeQuery(sb.toString(), User.class);
            IntStream.rangeClosed(1, parameters.size()).forEach(i -> sqlQuery.setParameter(i, parameters.get(i-1)));

            if (userQuery.limit != null && userQuery.limit > 0) {
                sqlQuery.setMaxResults(userQuery.limit);
            }

            if (userQuery.offset != null && userQuery.offset > 0) {
                sqlQuery.setFirstResult(query.offset);
            }

            List<User> userList = sqlQuery.getResultList();
            return userList;
        });

        if (userQuery.select != null && userQuery.select.basic) {
            users.forEach(user -> {
                // Clear out data and leave only basic info
                user.setAttributes((UserAttribute[]) null);
                user.setEmail(null);
                user.setRealmId(null);
                user.setSecret(null);
            });
        }
        return users.toArray(new User[0]);
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

    static User getUserByIdFromDb(PersistenceService persistenceService, String userId) {
        return persistenceService.doReturningTransaction(em -> {
            List<User> result =
                em.createQuery("select u from User u where u.id = :userId", User.class)
                    .setParameter("userId", userId)
                    .getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    static List<String> getUserIds(PersistenceService persistenceService, String realm, List<String> usernames) {
        List<String> CIUsernames = usernames.stream().map(String::toLowerCase).toList();

        return persistenceService.doReturningTransaction(em -> {
            Map<String, String> usernameIdMap = em.createQuery(
                "select u.username, u.id from User u join Realm r on r.id = u.realmId where u.username in :usernames and r.name = :realm", Tuple.class)
                    .setParameter("usernames", CIUsernames)
                    .setParameter("realm", realm)
                    .getResultList()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            tuple -> (String) tuple.get(0),
                            tuple -> (String) tuple.get(1)
                        )
                    );

            return CIUsernames.stream().map(usernameIdMap::get).collect(Collectors.toList());
        });
    }

    @SuppressWarnings("unchecked")
    static Realm[] getRealmsFromDb(PersistenceService persistenceService) {
        return persistenceService.doReturningTransaction(entityManager -> {
            List<Realm> realms = (List<Realm>)entityManager.createNativeQuery(
                "select *, (select ra.VALUE from PUBLIC.REALM_ATTRIBUTE ra where ra.REALM_ID = r.ID and ra.name = 'displayName') as displayName from public.realm r  where r.not_before is null or r.not_before = 0 or r.not_before <= extract('epoch' from now())"
                , Realm.class).getResultList();

            // Make sure the master realm is always on top
            realms.sort((o1, o2) -> {
                if (o1.getName().equals(MASTER_REALM))
                    return -1;
                if (o2.getName().equals(MASTER_REALM))
                    return 1;
                return o1.getName().compareTo(o2.getName());
            });

            // TODO: Remove this once migrated to hibernate 6.2.x+
            realms.forEach(r -> r.getRealmRoles().size());

            return realms.toArray(new Realm[realms.size()]);
        });
    }

    static Realm getRealmFromDb(PersistenceService persistenceService, String realm) {
        return persistenceService.doReturningTransaction(em -> {
                List<Realm> realms = em.createQuery("select r from Realm r where r.name = :realm", Realm.class)
                    .setParameter("realm", realm).getResultList();

                // TODO: Remove this once migrated to hibernate 6.2.x+
                realms.forEach(r -> r.getRealmRoles().size());

                return realms.size() == 1 ? realms.get(0) : null;
            }
        );
    }

    static boolean realmExistsFromDb(PersistenceService persistenceService, String realm) {
        return persistenceService.doReturningTransaction(em -> {

            long count = (long)em.createNativeQuery(
                "select count(*) from public.realm r where r.name = :realm and r.enabled = true and (r.not_before is null or r.not_before = 0 or r.not_before <= extract('epoch' from now()))",
                Long.class).setParameter("realm", realm).getSingleResult();

            return count > 0;
        });
    }

    static boolean userInRealmFromDb(PersistenceService persistenceService, String userId, String realm) {
        return persistenceService.doReturningTransaction(em -> {
            User user = em.find(User.class, userId);
            return (user != null && realm.equals(user.getRealm()));
        });
    }
}
