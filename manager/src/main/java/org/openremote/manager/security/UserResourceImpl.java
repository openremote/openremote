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
import org.openremote.container.timer.TimerService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.*;

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.Objects;

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.Constants.MASTER_REALM;

public class UserResourceImpl extends ManagerWebResource implements UserResource {

    public UserResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
    }

    @Override
    public User[] getAll(RequestParams requestParams, String realm) {
        AuthContext authContext = getAuthContext();
        boolean isAdmin = authContext.hasResourceRole(ClientRole.READ_ADMIN.getValue(), authContext.getClientId());
        boolean isBasicRead = authContext.hasResourceRole(ClientRole.READ_USERS.getValue(), authContext.getClientId());

        if (!isAdmin && !isBasicRead) {
             throw new ForbiddenException("Insufficient permissions to read users");
        }

        try {
            User[] users = identityService.getIdentityProvider().getUsers(
                realm
            );

            if (isAdmin) {
                return users;
            } else {
                return Arrays.stream(users)
                    .map(user ->
                        new User()
                            .setUsername(user.getUsername())
                            .setId(user.getId())
                            .setFirstName(user.getFirstName())
                            .setLastName(user.getLastName()))
                    .toArray(User[]::new);
            }
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User get(RequestParams requestParams, String realm, String userId) {
        if (!isSuperUser() && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Regular users can only retrieve their own roles");
        }

        try {
            return identityService.getIdentityProvider().getUser(
                realm, userId
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
        return get(requestParams, getRequestRealm(), getUserId());
    }

    @Override
    public void update(RequestParams requestParams, String realm, String userId, User user) {

        throwIfIllegalMasterAdminUserMutation(requestParams, realm, user);

        try {
            identityService.getIdentityProvider().updateUser(
                realm, user
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User create(RequestParams requestParams, String realm, User user) {
        try {
            return identityService.getIdentityProvider().createUser(
                realm, user,
                null);
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
            identityService.getIdentityProvider().deleteUser(
                realm, userId
            );
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
            identityService.getIdentityProvider().resetPassword(
                realm, userId, credential
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Role[] getCurrentUserRoles(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new ForbiddenException("Must be authenticated");
        }
        return getUserRoles(requestParams, getRequestRealm(), getUserId());
    }

    @Override
    public Role[] getUserRoles(@BeanParam RequestParams requestParams, String realm, String userId) {
        if (!isSuperUser() && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Regular users can only retrieve their own roles");
        }

        try {
            return identityService.getIdentityProvider().getUserRoles(
                realm, userId
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateUserRoles(@BeanParam RequestParams requestParams, String realm, String userId, Role[] roles) {
        try {
            identityService.getIdentityProvider().updateUserRoles(
                realm,
                userId,
                KEYCLOAK_CLIENT_ID,
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
        try {
            return identityService.getIdentityProvider().getRoles(
                realm,
                null);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateRoles(RequestParams requestParams, String realm, Role[] roles) {
        try {
            identityService.getIdentityProvider().updateRoles(
                realm,
                KEYCLOAK_CLIENT_ID,
                roles);
        } catch (ClientErrorException ex) {
            ex.printStackTrace(System.out);
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected void throwIfIllegalMasterAdminUserDeletion(RequestParams requestParams, String realm, String userId) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM))
            return;

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(
            userId
        )) return;

        throw new NotAllowedException("The master realm admin user cannot be deleted");
    }

    protected void throwIfIllegalMasterAdminUserMutation(RequestParams requestParams, String realm, User user) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM))
            return;

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(
            user.getId()
        )) return;

        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new NotAllowedException("The master realm admin user cannot be disabled");
        }
    }
}

