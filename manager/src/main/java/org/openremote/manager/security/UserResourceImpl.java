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
package org.openremote.manager.security;

import org.openremote.container.security.AuthContext;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.mqtt.MQTTBrokerService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.security.*;

import jakarta.ws.rs.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.Constants.MASTER_REALM;

public class UserResourceImpl extends ManagerWebResource implements UserResource {

    protected MQTTBrokerService mqttBrokerService;

    public UserResourceImpl(TimerService timerService, ManagerIdentityService identityService, MQTTBrokerService mqttBrokerService) {
        super(timerService, identityService);
        this.mqttBrokerService = mqttBrokerService;
    }

    @Override
    public User[] query(RequestParams requestParams, UserQuery query) {
        AuthContext authContext = getAuthContext();
        boolean isAdmin = authContext.hasResourceRole(ClientRole.READ_ADMIN.getValue(), Constants.KEYCLOAK_CLIENT_ID);
        boolean isRestricted = !isAdmin && authContext.hasResourceRole(ClientRole.READ_USERS.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        if (!isAdmin && !isRestricted) {
             throw new ForbiddenException("Insufficient permissions to read users");
        }

        if (query == null) {
            query = new UserQuery();
        }

        if (isRestricted) {
            if (query.select == null) {
                query.select = new UserQuery.Select();
            }
            query.select.basic(true);
        }

        if (!authContext.isSuperUser()) {
            // Force realm to match users
            query.realm(new RealmPredicate(authContext.getAuthenticatedRealmName()));

            // Hide system accounts from non super users
            if (query.attributes == null) {
                query.attributes(new UserQuery.AttributeValuePredicate(true, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE), null));
            } else {
                List<UserQuery.AttributeValuePredicate> attributeValuePredicates = new ArrayList<>(Arrays.asList(query.attributes));
                attributeValuePredicates.add(new UserQuery.AttributeValuePredicate(true, new StringPredicate(User.SYSTEM_ACCOUNT_ATTRIBUTE), null));
                query.attributes(attributeValuePredicates.toArray(UserQuery.AttributeValuePredicate[]::new));
            }
        }

        // Prevent service

        try {
            return identityService.getIdentityProvider().queryUsers(query);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User get(RequestParams requestParams, String realm, String userId) {
        boolean hasAdminReadRole = hasResourceRole(ClientRole.READ_ADMIN.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        if (!hasAdminReadRole && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Can only retrieve own user info unless you have role '" + ClientRole.READ_ADMIN + "'");
        }

        try {
            return identityService.getIdentityProvider().getUser(
                userId
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User getCurrent(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new ForbiddenException("Must be authenticated");
        }
        return get(requestParams, getRequestRealmName(), getUserId());
    }

    @Override
    public User update(RequestParams requestParams, String realm, User user) {

        throwIfIllegalMasterAdminUserMutation(requestParams, realm, user);

        try {
            return identityService.getIdentityProvider().createUpdateUser(realm, user, null, true);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User create(RequestParams requestParams, String realm, User user) {

        try {
            return identityService.getIdentityProvider().createUpdateUser(realm, user, null, false);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String realm, String userId) {
        throwIfIllegalMasterAdminUserDeletion(requestParams, realm, userId);

        try {
            identityService.getIdentityProvider().deleteUser(realm, userId);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void resetPassword(@BeanParam RequestParams requestParams, String realm, String userId, Credential credential) {
        try {
            identityService.getIdentityProvider().resetPassword(realm, userId, credential);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public String resetSecret(RequestParams requestParams, String realm, String userId) {
        try {
            return identityService.getIdentityProvider().resetSecret(realm, userId, null);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Role[] getCurrentUserRoles(RequestParams requestParams) {
        return getCurrentUserClientRoles(requestParams, KEYCLOAK_CLIENT_ID);
    }

    @Override
    public Role[] getCurrentUserClientRoles(RequestParams requestParams, String clientId) {
        if (!isAuthenticated()) {
            throw new ForbiddenException("Must be authenticated");
        }

        return getUserClientRoles(requestParams, getRequestRealmName(), getUserId(), clientId);
    }

    @Override
    public Role[] getCurrentUserRealmRoles(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new ForbiddenException("Must be authenticated");
        }

        return getUserRealmRoles(requestParams, getRequestRealmName(), getUserId());
    }

    @Override
    public Role[] getUserRoles(RequestParams requestParams, String realm, String userId) {
        return getUserClientRoles(requestParams, realm, userId, KEYCLOAK_CLIENT_ID);
    }

    @Override
    public Role[] getUserClientRoles(@BeanParam RequestParams requestParams, String realm, String userId, String clientId) {
        boolean hasAdminReadRole = hasResourceRole(ClientRole.READ_ADMIN.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        if (!hasAdminReadRole && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Can only retrieve own user roles unless you have role '" + ClientRole.READ_ADMIN + "'");
        }

        try {
            return identityService.getIdentityProvider().getUserRoles(
                realm, userId, clientId
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Role[] getUserRealmRoles(RequestParams requestParams, String realm, String userId) {
        boolean hasAdminReadRole = hasResourceRole(ClientRole.READ_ADMIN.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        if (!hasAdminReadRole && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Can only retrieve own user roles unless you have role '" + ClientRole.READ_ADMIN + "'");
        }

        try {
            return identityService.getIdentityProvider().getUserRealmRoles(
                    realm, userId
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateUserRoles(RequestParams requestParams, String realm, String userId, Role[] roles) {
        updateUserClientRoles(requestParams, realm, userId, roles, KEYCLOAK_CLIENT_ID);
    }

    @Override
    public void updateUserClientRoles(@BeanParam RequestParams requestParams, String realm, String userId, Role[] roles, String clientId) {
        try {
            identityService.getIdentityProvider().updateUserRoles(
                realm,
                userId,
                clientId,
                Arrays.stream(roles)
                    .filter(Role::isAssigned)
                    .map(Role::getName)
                    .toArray(String[]::new));
        } catch (ClientErrorException ex) {
            ex.printStackTrace(System.out);
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateUserRealmRoles(RequestParams requestParams, String realm, String userId, Role[] roles) {
        try {
            identityService.getIdentityProvider().updateUserRealmRoles(
                    realm,
                    userId,
                    Arrays.stream(roles)
                            .filter(Role::isAssigned)
                            .map(Role::getName)
                            .toArray(String[]::new));
        } catch (ClientErrorException ex) {
            ex.printStackTrace(System.out);
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Role[] getRoles(RequestParams requestParams, String realm) {
        return getClientRoles(requestParams, realm, KEYCLOAK_CLIENT_ID);
    }

    @Override
    public Role[] getClientRoles(RequestParams requestParams, String realm, String clientId) {
        try {
            return identityService.getIdentityProvider().getRoles(
                realm,
                clientId);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateRoles(RequestParams requestParams, String realm, Role[] roles) {
        updateClientRoles(requestParams, realm, roles, KEYCLOAK_CLIENT_ID);
    }

    @Override
    public void updateClientRoles(RequestParams requestParams, String realm, Role[] roles, String clientId) {
        try {
            identityService.getIdentityProvider().updateClientRoles(
                realm,
                clientId,
                roles);
        } catch (ClientErrorException ex) {
            ex.printStackTrace(System.out);
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new NotFoundException(ex);
        }
    }

    @Override
    public UserSession[] getUserSessions(RequestParams requestParams, String realm, String userId) {
        boolean hasAdminReadRole = hasResourceRole(ClientRole.READ_ADMIN.getValue(), Constants.KEYCLOAK_CLIENT_ID);

        if (!hasAdminReadRole && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Can only retrieve own user sessions unless you have role '" + ClientRole.READ_ADMIN + "'");
        }

        return mqttBrokerService.getUserConnections(userId).stream().map(connection -> new UserSession(
            MQTTBrokerService.getConnectionIDString(connection),
            connection.getSubject() != null ? KeycloakIdentityProvider.getSubjectName(connection.getSubject()) : userId,
            connection.getCreationTime(),
            connection.getRemoteAddress())).toArray(UserSession[]::new);
    }

    @Override
    public void disconnectUserSession(RequestParams requestParams, String realm, String sessionID) {
        if (!mqttBrokerService.disconnectSession(sessionID)) {
            throw new NotFoundException("User session not found");
        }
    }

    protected void throwIfIllegalMasterAdminUserDeletion(RequestParams requestParams, String realm, String userId) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM)) {
            return;
        }

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(userId)) {
            return;
        }

        throw new NotAllowedException("The master realm admin user cannot be deleted");
    }

    protected void throwIfIllegalMasterAdminUserMutation(RequestParams requestParams, String realm, User user) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM)) {
            return;
        }

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(user.getId())) {
            return;
        }

        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new NotAllowedException("The master realm admin user cannot be disabled");
        }
    }
}

