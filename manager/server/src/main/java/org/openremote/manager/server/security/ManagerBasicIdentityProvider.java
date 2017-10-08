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
package org.openremote.manager.server.security;

import io.undertow.security.idm.Account;
import org.openremote.container.Container;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicAuthContext;
import org.openremote.container.security.basic.BasicIdentityProvider;
import org.openremote.container.security.basic.PasswordStorage;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.manager.server.persistence.ManagerPersistenceService;
import org.openremote.manager.shared.security.*;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.event.shared.TenantFilter;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ManagerBasicIdentityProvider extends BasicIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerBasicIdentityProvider.class.getName());

    final protected ManagerPersistenceService persistenceService;

    final protected Tenant masterTenant = new Tenant(Constants.MASTER_REALM, Constants.MASTER_REALM, "Master", true);

    public ManagerBasicIdentityProvider(Container container) {
        this.persistenceService = container.getService(ManagerPersistenceService.class);
    }

    @Override
    protected Account verifyAccount(String username, char[] password) {
        LOG.fine("Authentication attempt, querying user: " + username);
        Object[] idAndPassword = persistenceService.doReturningTransaction(em -> {
                try {
                    return (Object[]) em.createNativeQuery(
                        "select U.ID, U.PASSWORD from USER_ENTITY U where U.USERNAME = :username"
                    ).setParameter("username", username).getSingleResult();
                } catch (NoResultException | NonUniqueResultException ex) {
                    return null;
                }
            }
        );
        if (idAndPassword == null) {
            LOG.fine("Authentication failed, no such user: " + username);
            return null;
        }
        LOG.fine("Authentication attempt, verifying password: " + username);
        if (!PasswordStorage.verifyPassword(password, idAndPassword[1].toString())) {
            LOG.fine("Authentication failed, invalid password: " + username);
            return null;
        }

        LOG.fine("Authentication successful: " + username);
        final BasicAuthContext principal = new BasicAuthContext(Constants.MASTER_REALM, idAndPassword[0].toString(), username);
        return new Account() {
            @Override
            public Principal getPrincipal() {
                return principal;
            }

            @Override
            public Set<String> getRoles() {
                return ClientRole.ALL_ROLES;
            }
        };
    }

    @Override
    public User[] getUsers(ClientRequestInfo clientRequestInfo, String realm) {
        return new User[0];
    }

    @Override
    public User getUser(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        return null;
    }

    @Override
    public User getUser(String realm, String userName) {
        return null;
    }

    @Override
    public void updateUser(ClientRequestInfo clientRequestInfo, String realm, String userId, User user) {

    }

    @Override
    public void createUser(ClientRequestInfo clientRequestInfo, String realm, User user) {

    }

    @Override
    public void deleteUser(ClientRequestInfo clientRequestInfo, String realm, String userId) {

    }

    @Override
    public void resetPassword(ClientRequestInfo clientRequestInfo, String realm, String userId, Credential credential) {

    }

    @Override
    public Role[] getRoles(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        return new Role[0];
    }

    @Override
    public void updateRoles(ClientRequestInfo clientRequestInfo, String realm, String userId, Role[] roles) {

    }

    @Override
    public boolean isMasterRealmAdmin(ClientRequestInfo clientRequestInfo, String userId) {
        return false;
    }

    @Override
    public void setRestrictedUser(String userId, boolean restricted) {

    }

    @Override
    public boolean isRestrictedUser(String userId) {
        return false;
    }

    @Override
    public boolean isUserInTenant(String userId, String realmId) {
        // TODO
        return true;
    }

    @Override
    public Tenant[] getTenants(ClientRequestInfo clientRequestInfo) {
        return new Tenant[]{masterTenant};
    }

    @Override
    public Tenant getTenantForRealm(String realm) {
        return Objects.equals(realm, Constants.MASTER_REALM) ? masterTenant : null;
    }

    @Override
    public Tenant getTenantForRealmId(String realmId) {
        return Objects.equals(realmId, Constants.MASTER_REALM) ? masterTenant : null;
    }

    @Override
    public void updateTenant(ClientRequestInfo clientRequestInfo, String realm, Tenant tenant) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");

    }

    @Override
    public void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");
    }

    @Override
    public void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant, TenantEmailConfig emailConfig) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");
    }

    @Override
    public void deleteTenant(ClientRequestInfo clientRequestInfo, String realm) {
        throw new UnsupportedOperationException("This provider does not support multiple tenants");
    }

    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, Tenant tenant) {
        return Objects.equals(tenant.getId(), Constants.MASTER_REALM);
    }

    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, Asset asset) {
        return Objects.equals(asset.getRealmId(), Constants.MASTER_REALM);
    }

    @Override
    public String[] getActiveTenantIds() {
        return new String[]{Constants.MASTER_REALM};
    }

    @Override
    public boolean isActiveTenant(String realmId) {
        return Objects.equals(realmId, Constants.MASTER_REALM);
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
