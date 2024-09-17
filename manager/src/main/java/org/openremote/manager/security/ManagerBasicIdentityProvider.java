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

import org.hibernate.Session;
import org.openremote.model.Container;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicIdentityProvider;
import org.openremote.container.security.basic.PasswordStorage;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import java.sql.PreparedStatement;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;

public class ManagerBasicIdentityProvider extends BasicIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerBasicIdentityProvider.class.getName());
    protected ManagerIdentityService identityService;
    protected String adminPassword;

    @Override
    public void init(Container container) {
        super.init(container);
        this.identityService = container.getService(ManagerIdentityService.class);
        adminPassword = container.getConfig().getOrDefault(OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT);
    }

    @Override
    public void start(Container container) {
        super.start(container);

        // Create master realm and admin user
        if (!realmExists(MASTER_REALM)) {
            LOG.info("Creating master realm and admin user");

            // Configure the master realm
            persistenceService.doTransaction(em -> em.unwrap(Session.class).doWork(connection -> {
                String sql = "insert into PUBLIC.REALM(ID, NAME, ENABLED) values ('master', 'master', true)";
                PreparedStatement st = connection.prepareStatement(sql);
                st.executeUpdate();
                st.close();

                sql = "insert into PUBLIC.REALM_ATTRIBUTE(REALM_ID, NAME, VALUE) values ('master', 'displayName', 'Master')";
                st = connection.prepareStatement(sql);
                st.executeUpdate();
                st.close();
            }));

            User adminUser = new User();
            adminUser.setUsername(MASTER_REALM_ADMIN_USER);
            createUpdateUser(MASTER_REALM, adminUser, adminPassword, true);
        }
    }

    @Override
    protected Set<String> getDefaultRoles() {
        return ClientRole.ALL_ROLES;
    }

    @Override
    public User[] queryUsers(UserQuery userQuery) {
        return ManagerIdentityProvider.getUsersFromDb(persistenceService, userQuery);
    }

    @Override
    public User getUser(String userId) {
        return ManagerIdentityProvider.getUserByIdFromDb(persistenceService, userId);
    }

    @Override
    public User getUserByUsername(String realm, String username) {
        return ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
    }

    @Override
    public User createUpdateUser(String realm, User user, String password, boolean allowUpdate) {
        if (!realm.equals(MASTER_REALM)) {
            throw new UnsupportedOperationException("This provider does not support realms other than master");
        }

        if (TextUtil.isNullOrEmpty(password)) {
            throw new IllegalStateException("Password must be specified for basic identity provider");
        }

        LOG.info("Creating user: " + user);
        user.setId(UUID.randomUUID().toString());
        persistenceService.doTransaction(em -> em.unwrap(Session.class).doWork(connection -> {
            String sql = "insert into PUBLIC.USER_ENTITY(ID, REALM_ID, USERNAME, PASSWORD, FIRST_NAME, LAST_NAME, EMAIL, ENABLED) values (?, ?, ?, ?, ?, ?, ?, ?)";
            if(allowUpdate) {
                sql += " ON CONFLICT (ID, USERNAME) DO UPDATE";
            }
            sql += " SET username = excluded.username, password = excluded.password, first_name = excluded.first_name, last_name = excluded.last_name, email = excluded.email, enabled = excluded.enabled";
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setString(1, UUID.randomUUID().toString());
                st.setString(2, MASTER_REALM); // For master REALM NAME and ID are the same
                st.setString(3, user.getUsername());
                st.setString(4, PasswordStorage.createHash(password));
                st.setString(5, user.getFirstName());
                st.setString(6, user.getLastName());
                st.setString(7, user.getEmail());
                st.setBoolean(8, user.getEnabled() != null ? user.getEnabled() : true);
                st.executeUpdate();
            }
        }));
        return user;
    }

    @Override
    public void deleteUser(String realm, String userId) {
        LOG.info("Deleting user: " + userId);
        persistenceService.doTransaction(em -> {
            User user = em.find(User.class, userId);
            if (user != null) {
                em.remove(user);
            } else {
                LOG.info("Cannot delete user as ID not found: " + userId);
            }
        });
    }

    @Override
    public void resetPassword(String realm, String userId, Credential credential) {
        throw new UnsupportedOperationException("This provider does not support password reset");
    }

    @Override
    public String resetSecret(String realm, String userId, String secret) {
        throw new UnsupportedOperationException("This provider does not support secret reset");
    }

    @Override
    public Role[] getRoles(String realm, String client) {
        if (client != null && !MASTER_REALM.equals(client)) {
            throw new IllegalStateException("This provider only has a single master realm");
        }
        return ClientRole.ALL_ROLES.stream()
            .map(role -> new Role(UUID.randomUUID().toString(), role, false, true, null))
            .toArray(Role[]::new);
    }

    @Override
    public void updateClientRoles(String realm, String client, Role[] roles) {
        throw new UnsupportedOperationException("This provider does not support updating roles");
    }

    @Override
    public Role[] getUserRoles(String realm, String userId, String client) {
        return ClientRole.ALL_ROLES.stream()
            .map(role -> new Role(UUID.randomUUID().toString(), role, false, true, null))
            .toArray(Role[]::new);
    }

    @Override
    public Role[] getUserRealmRoles(String realm, String userId) {
        throw new UnsupportedOperationException("This provider does not support user realm roles");
    }

    @Override
    public void updateUserRoles(String realm, String userId, String client, String... roles) {
        throw new UnsupportedOperationException("This provider does not support updating user roles");
    }

    @Override
    public void updateUserRealmRoles(String realm, String userId, String... roles) {
        throw new UnsupportedOperationException("This provider does not support updating user realm roles");
    }

    @Override
    public boolean isMasterRealmAdmin(String userId) {
        return true;
    }

    @Override
    public boolean isRestrictedUser(AuthContext authContext) {
        return false;
    }

    @Override
    public boolean isUserInRealm(String userId, String realm) {
        return ManagerIdentityProvider.userInRealmFromDb(persistenceService, userId, realm);
    }

    @Override
    public Realm[] getRealms() {
        return ManagerIdentityProvider.getRealmsFromDb(persistenceService);
    }

    @Override
    public Realm getRealm(String realm) {
        return ManagerIdentityProvider.getRealmFromDb(persistenceService, realm);
    }

    @Override
    public void updateRealm(Realm realm) {
        throw new UnsupportedOperationException("This provider does not support modifying realms");
    }

    @Override
    public Realm createRealm(Realm realm) {
        throw new UnsupportedOperationException("This provider does not support multiple realms");
    }

    @Override
    public void deleteRealm(String realm) {
        throw new UnsupportedOperationException("This provider does not support multiple realms");
    }

    @Override
    public boolean isRealmActiveAndAccessible(AuthContext authContext, Realm realm) {
        return Objects.equals(realm.getId(), MASTER_REALM);
    }

    @Override
    public boolean isRealmActiveAndAccessible(AuthContext authContext, String realm) {
        return Objects.equals(realm, MASTER_REALM);
    }

    @Override
    public boolean realmExists(String realm) {
        return ManagerIdentityProvider.realmExistsFromDb(persistenceService, realm);
    }

    @Override
    public boolean canSubscribeWith(AuthContext auth, RealmFilter<?> filter, ClientRole... requiredRoles) {
        // TODO Doesn't really respect the description of the interface
        return auth.isSuperUser();
    }

    @Override
    public String getFrontendURI() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
