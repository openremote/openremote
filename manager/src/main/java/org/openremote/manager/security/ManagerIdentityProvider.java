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

import org.openremote.container.security.AuthContext;
import org.openremote.container.security.IdentityProvider;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.model.asset.Asset;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.security.*;

import java.io.IOException;
import java.util.List;

/**
 * SPI for implementations used by {@link ManagerIdentityService}, provides CRUD of
 * {@link User} and {@link Tenant}.
 */
public interface ManagerIdentityProvider extends IdentityProvider {

    User[] getUsers(ClientRequestInfo clientRequestInfo, String realm);

    User[] getUsers(List<String> userIds);

    User[] getUsers(UserQuery userQuery);

    User getUser(ClientRequestInfo clientRequestInfo, String realm, String userId);

    User getUser(String realm, String userName);

    void updateUser(ClientRequestInfo clientRequestInfo, String realm, String userId, User user);

    void createUser(ClientRequestInfo clientRequestInfo, String realm, User user);

    void deleteUser(ClientRequestInfo clientRequestInfo, String realm, String userId);

    void resetPassword(ClientRequestInfo clientRequestInfo, String realm, String userId, Credential credential);

    Role[] getRoles(ClientRequestInfo clientRequestInfo, String realm, String userId);

    void updateRoles(ClientRequestInfo clientRequestInfo, String realm, String userId, Role[] roles);

    boolean isMasterRealmAdmin(ClientRequestInfo clientRequestInfo, String userId);

    boolean isRestrictedUser(String userId);

    boolean isUserInTenant(String userId, String realm);

    Tenant[] getTenants();

    Tenant getTenant(String realm);

    void updateTenant(ClientRequestInfo clientRequestInfo, String realm, Tenant tenant) throws Exception;

    void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant) throws Exception;

    void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant, TenantEmailConfig emailConfig) throws Exception;

    void deleteTenant(ClientRequestInfo clientRequestInfo, String realm) throws Exception;

    boolean isTenantActiveAndAccessible(AuthContext authContext, Tenant tenant);

    boolean isTenantActiveAndAccessible(AuthContext authContext, Asset asset);

    boolean tenantExists(String realm);

    /**
     * Superusers can subscribe to all events, regular users must be in the same realm as the filter and any
     * required roles must match. If the authenticated party is a restricted user, this returns <code>false.</code>
     *
     * @return <code>true</code> if the authenticated party can subscribe to events with the given filter.
     */
    boolean canSubscribeWith(AuthContext auth, TenantFilter filter, ClientRole... requiredRoles);
}
