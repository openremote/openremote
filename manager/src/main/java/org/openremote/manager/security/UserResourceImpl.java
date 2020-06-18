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

import org.openremote.container.timer.TimerService;
import org.openremote.manager.i18n.I18NService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.http.ConstraintViolationReport;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.*;

import javax.ws.rs.BeanParam;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.http.BadRequestError.VIOLATION_EXCEPTION_HEADER;

public class UserResourceImpl extends ManagerWebResource implements UserResource {

    public UserResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
    }

    @Override
    public User[] getAll(RequestParams requestParams, String realm) {
        try {
            return identityService.getIdentityProvider().getUsers(
                realm
            );
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
    public void create(RequestParams requestParams, String realm, User user) {
        try {
            identityService.getIdentityProvider().createUser(
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
    public Role[] getRoles(@BeanParam RequestParams requestParams, String realm, String userId) {
        if (!isSuperUser() && !Objects.equals(getUserId(), userId)) {
            throw new ForbiddenException("Regular users can only retrieve their own roles");
        }

        try {
            return identityService.getIdentityProvider().getRoles(
                realm, userId
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
        return getRoles(requestParams, getRequestRealm(), getUserId());
    }

    @Override
    public void updateRoles(@BeanParam RequestParams requestParams, String realm, String userId, Role[] roles) {
        try {
            identityService.getIdentityProvider().updateRoles(
                realm,
                userId,
                Arrays.stream(roles)
                    .filter(Role::isAssigned)
                    .map(r -> Arrays.stream(ClientRole.values()).filter(cr -> cr.getValue().equals(r.getName())).findFirst().orElse(null))
                    .toArray(ClientRole[]::new)
            );
        } catch (ClientErrorException ex) {
            ex.printStackTrace(System.out);
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected ConstraintViolationReport isIllegalMasterAdminUserDeletion(RequestParams requestParams, String realm, String userId) {
        if (!realm.equals(MASTER_REALM))
            return null;

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(
            userId
        )) return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();
        List<ConstraintViolation> violations = new ArrayList<>();
        ConstraintViolation violation = new ConstraintViolation();
        violation.setConstraintType(ConstraintViolation.Type.PARAMETER);
        violation.setMessage(validationMessages.getString("User.masterAdminDeleted"));
        violations.add(violation);
        ConstraintViolationReport report = new ConstraintViolationReport();
        report.setParameterViolations(violations.toArray(new ConstraintViolation[violations.size()]));
        return report;
    }

    protected ConstraintViolationReport isIllegalMasterAdminUserMutation(RequestParams requestParams, String realm, User user) {
        if (!realm.equals(MASTER_REALM))
            return null;

        if (!identityService.getIdentityProvider().isMasterRealmAdmin(
            user.getId()
        )) return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();

        List<ConstraintViolation> violations = new ArrayList<>();
        if (user.getEnabled() == null || !user.getEnabled()) {
            ConstraintViolation violation = new ConstraintViolation();
            violation.setConstraintType(ConstraintViolation.Type.PARAMETER);
            violation.setPath("User.enabled");
            violation.setMessage(validationMessages.getString("User.masterAdminDisabled"));
            violations.add(violation);
        }
        if (violations.size() > 0) {
            ConstraintViolationReport report = new ConstraintViolationReport();
            report.setParameterViolations(violations.toArray(new ConstraintViolation[violations.size()]));
            return report;
        }
        return null;
    }

}

