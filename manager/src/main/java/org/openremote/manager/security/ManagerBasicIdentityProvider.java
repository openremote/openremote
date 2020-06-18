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
import org.openremote.container.Container;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicIdentityProvider;
import org.openremote.container.security.basic.PasswordStorage;
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;

public class ManagerBasicIdentityProvider extends BasicIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerBasicIdentityProvider.class.getName());
    final protected ManagerIdentityService identityService;
    final protected String adminPassword;

    public ManagerBasicIdentityProvider(Container container) {
        super(container.getService(ManagerPersistenceService.class));
        this.identityService = container.getService(ManagerIdentityService.class);
        adminPassword = container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT);
    }

    @Override
    public void start() {
        super.start();

        // Create master tenant and admin user
        if (!tenantExists(MASTER_REALM)) {
            LOG.info("Creating master tenant and admin user");

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
            createUser(MASTER_REALM, adminUser, adminPassword);
        }
    }

    @Override
    protected Set<String> getDefaultRoles() {
        return ClientRole.ALL_ROLES;
    }

    @Override
    public User[] getUsers(String realm) {
        return getUsers(new UserQuery().tenant(new TenantPredicate(realm)));
    }

    @Override
    public User[] getUsers(List<String> userIds) {
        return getUsers(new UserQuery().ids(userIds.toArray(new String[0])));
    }

    @Override
    public User[] getUsers(UserQuery userQuery) {
        return ManagerIdentityProvider.getUsersFromDb(persistenceService, userQuery);
    }

    @Override
    public User getUser(String realm, String userId) {
        return ManagerIdentityProvider.getUserByIdFromDb(persistenceService, realm, userId);
    }

    @Override
    public User getUserByUsername(String realm, String username) {
        return ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
    }

    @Override
    public void updateUser(String realm, User user) {
        LOG.info("Updating user: " + user);
        persistenceService.doTransaction(em -> em.merge(user));
    }

    @Override
    public User createUser(String realm, User user, String password) {
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
    public Role[] getRoles(String realm, String userId) {
        return ClientRole.ALL_ROLES.stream()
            .map(role -> new Role(UUID.randomUUID().toString(), role, false, true))
            .toArray(Role[]::new);
    }

    @Override
    public void updateRoles(String realm, String username, ClientRole[] roles) {
        throw new UnsupportedOperationException("This provider does not support updating roles");
    }

    @Override
    public boolean isMasterRealmAdmin(String userId) {
        return true;
    }

    @Override
    public boolean isRestrictedUser(String userId) {
        return false;
    }

    @Override
    public boolean isUserInTenant(String userId, String realm) {
        return ManagerIdentityProvider.userInTenantFromDb(persistenceService, userId, realm);
    }

    @Override
    public Tenant[] getTenants() {
        return ManagerIdentityProvider.getTenantsFromDb(persistenceService);
    }

    @Override
    public Tenant getTenant(String realm) {
        return ManagerIdentityProvider.getTenantFromDb(persistenceService, realm);
    }

    @Override
    public void updateTenant(Tenant tenant) {
        throw new UnsupportedOperationException("This provider does not support modifying tenants");
    }

    @Override
    public Tenant createTenant(Tenant tenant) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");
    }

    @Override
    public void deleteTenant(String realm) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");
    }

    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, Tenant tenant) {
        return Objects.equals(tenant.getId(), MASTER_REALM);
    }

    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, String realm) {
        return Objects.equals(realm, MASTER_REALM);
    }

    @Override
    public boolean tenantExists(String realm) {
        return ManagerIdentityProvider.tenantExistsFromDb(persistenceService, realm);
    }

    @Override
    public boolean canSubscribeWith(AuthContext auth, TenantFilter filter, ClientRole... requiredRoles) {
        // TODO Doesn't really respect the description of the interface
        return auth.isSuperUser();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
