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
package org.openremote.manager.server.security;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.openremote.container.web.WebResource;
import org.openremote.manager.server.i18n.I18NService;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.security.Credential;
import org.openremote.manager.shared.security.User;
import org.openremote.manager.shared.security.UserResource;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.manager.shared.validation.ConstraintViolationReport;

import javax.ws.rs.BeanParam;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.openremote.container.util.JsonUtil.convert;
import static org.openremote.manager.shared.Constants.MASTER_REALM;
import static org.openremote.manager.shared.Constants.MASTER_REALM_ADMIN_USER;
import static org.openremote.manager.shared.validation.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

public class UserResourceImpl extends WebResource implements UserResource {

    private static final Logger LOG = Logger.getLogger(UserResourceImpl.class.getName());

    protected final ManagerIdentityService managerIdentityService;

    public UserResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public User[] getAll(RequestParams requestParams, String realm) {
        try {
            List<UserRepresentation> userRepresentations =
                managerIdentityService.getRealms(requestParams).realm(realm).users().search(null, 0, Integer.MAX_VALUE);
            List<User> users = new ArrayList<>();
            for (UserRepresentation userRepresentation : userRepresentations) {
                users.add(convertUser(realm, userRepresentation));
            }
            return users.toArray(new User[users.size()]);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public User get(RequestParams requestParams, String realm, String userId) {
        try {
            return convertUser(
                realm,
                managerIdentityService.getRealms(requestParams).realm(realm).users().get(userId).toRepresentation()
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void update(RequestParams requestParams, String realm, String userId, User user) {
        ConstraintViolationReport violationReport;
        if ((violationReport = isIllegalMasterAdminUserMutation(requestParams, realm, user)) != null) {
            throw new WebApplicationException(
                Response.status(BAD_REQUEST)
                    .header(VIOLATION_EXCEPTION_HEADER, "true")
                    .entity(violationReport)
                    .build()
            );
        }
        try {
            managerIdentityService.getRealms(requestParams).realm(realm).users().get(userId).update(
                convert(getContainer().JSON, UserRepresentation.class, user)
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void create(RequestParams requestParams, String realm, User user) {
        try {
            Response response = managerIdentityService.getRealms(requestParams).realm(realm).users().create(
                convert(getContainer().JSON, UserRepresentation.class, user)
            );
            if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
                throw new WebApplicationException(
                    Response.status(response.getStatus())
                        .entity(response.getEntity())
                        .build()
                );
            }
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
        ConstraintViolationReport violationReport;
        if ((violationReport = isIllegalMasterAdminUserDeletion(requestParams, realm, userId)) != null) {
            throw new WebApplicationException(
                Response.status(BAD_REQUEST)
                    .header(VIOLATION_EXCEPTION_HEADER, "true")
                    .entity(violationReport)
                    .build()
            );
        }
        try {
            Response response = managerIdentityService.getRealms(requestParams).realm(realm).users().delete(userId);
            if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
                throw new WebApplicationException(
                    Response.status(response.getStatus())
                        .entity(response.getEntity())
                        .build()
                );
            }
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
            managerIdentityService.getRealms(requestParams).realm(realm).users().get(userId).resetPassword(
                convert(getContainer().JSON, CredentialRepresentation.class, credential)
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected User convertUser(String realm, UserRepresentation userRepresentation) {
        User user = convert(getContainer().JSON, User.class, userRepresentation);
        user.setRealm(realm);
        return user;
    }

    protected ConstraintViolationReport isIllegalMasterAdminUserDeletion(RequestParams requestParams, String realm, String userId) {
        if (!realm.equals(MASTER_REALM))
            return null;

        UserRepresentation masterAdminUser = getMasterRealmAdminUser(requestParams);
        if (!masterAdminUser.getId().equals(userId))
            return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();
        List<ConstraintViolation> violations = new ArrayList<>();
        violations.add(new ConstraintViolation(
            ConstraintViolation.Type.PARAMETER,
            validationMessages.getString("User.masterAdminDeleted")
        ));
        ConstraintViolationReport report = new ConstraintViolationReport();
        report.setParameterViolations(violations.toArray(new ConstraintViolation[violations.size()]));
        return report;
    }

    protected ConstraintViolationReport isIllegalMasterAdminUserMutation(RequestParams requestParams, String realm, User user) {
        if (!realm.equals(MASTER_REALM))
            return null;

        UserRepresentation masterAdminUser = getMasterRealmAdminUser(requestParams);
        if (!masterAdminUser.getId().equals(user.getId()))
            return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();

        List<ConstraintViolation> violations = new ArrayList<>();
        if (user.getEnabled() == null || !user.getEnabled()) {
            ConstraintViolation violation = new ConstraintViolation(
                ConstraintViolation.Type.PARAMETER,
                "User.enabled",
                validationMessages.getString("User.masterAdminDisabled")
            );
            violations.add(violation);
        }
        if (user.getUsername() == null || !user.getUsername().equals(MASTER_REALM_ADMIN_USER)) {
            ConstraintViolation violation = new ConstraintViolation(
                ConstraintViolation.Type.PARAMETER,
                "User.username",
                validationMessages.getString("User.masterAdminUsernameChanged")
            );
            violations.add(violation);
        }
        if (violations.size() > 0) {
            ConstraintViolationReport report = new ConstraintViolationReport();
            report.setParameterViolations(violations.toArray(new ConstraintViolation[violations.size()]));
            return report;
        }
        return null;
    }

    protected UserRepresentation getMasterRealmAdminUser(RequestParams requestParams) {
        List<UserRepresentation> adminUsers = managerIdentityService
            .getRealms(requestParams).realm(MASTER_REALM)
            .users().search(MASTER_REALM_ADMIN_USER, null, null);
        if (adminUsers.size() == 0) {
            throw new IllegalStateException("Can't load master realm admin user");
        } else if (adminUsers.size() > 1) {
            throw new IllegalStateException("Several master realm admin users, this should not be possible.");
        }
        return adminUsers.get(0);
    }
}

