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

import org.apache.http.HttpStatus;
import org.openremote.container.Container;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.i18n.I18NService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.security.Tenant;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.http.ConstraintViolationReport;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.TenantResource;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.http.BadRequestError.VIOLATION_EXCEPTION_HEADER;

public class TenantResourceImpl extends ManagerWebResource implements TenantResource {

    private static final Logger LOG = Logger.getLogger(TenantResourceImpl.class.getName());
    protected Container container;

    public TenantResourceImpl(TimerService timerService, ManagerIdentityService identityService, Container container) {
        super(timerService, identityService);
        this.container = container;
    }

    @Override
    public Tenant[] getAll(RequestParams requestParams) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            return identityService.getIdentityProvider().getTenants();
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Tenant get(RequestParams requestParams, String realm) {
        Tenant tenant = identityService.getIdentityProvider().getTenant(realm);
        if (tenant == null)
            throw new WebApplicationException(NOT_FOUND);
        if (!isTenantActiveAndAccessible(tenant)) {
            LOG.fine("Forbidden access for user '" + getUsername() + "': " + tenant);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return tenant;
    }

    @Override
    public void update(RequestParams requestParams, String realm, Tenant tenant) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        ConstraintViolationReport violationReport;
        if ((violationReport = isIllegalMasterRealmMutation(realm, tenant)) != null) {
            throw new WebApplicationException(
                Response.status(BAD_REQUEST)
                    .header(VIOLATION_EXCEPTION_HEADER, "true")
                    .entity(violationReport)
                    .build()
            );
        }
        try {
            identityService.getIdentityProvider().updateTenant(
                tenant
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex.getCause(), HttpStatus.SC_CONFLICT);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void create(RequestParams requestParams, Tenant tenant) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            identityService.getIdentityProvider().createTenant(
                tenant
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String realm) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        // TODO Delete all assets in that realm?
        ConstraintViolationReport violationReport;
        if ((violationReport = isIllegalMasterRealmDeletion(realm)) != null) {
            throw new WebApplicationException(
                Response.status(BAD_REQUEST)
                    .header(VIOLATION_EXCEPTION_HEADER, "true")
                    .entity(violationReport)
                    .build()
            );
        }
        try {
            identityService.getIdentityProvider().deleteTenant(
                realm
            );
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected ConstraintViolationReport isIllegalMasterRealmDeletion(String realm) {
        if (!realm.equals(MASTER_REALM))
            return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();
        List<ConstraintViolation> violations = new ArrayList<>();
        ConstraintViolation violation = new ConstraintViolation();
        violation.setConstraintType(ConstraintViolation.Type.PARAMETER);
        violation.setMessage(validationMessages.getString("Tenant.masterDeleted"));
        violations.add(violation);
        ConstraintViolationReport report = new ConstraintViolationReport();
        report.setParameterViolations(violations.toArray(new ConstraintViolation[violations.size()]));
        return report;
    }

    protected ConstraintViolationReport isIllegalMasterRealmMutation(String realm, Tenant tenant) {
        if (!realm.equals(MASTER_REALM))
            return null;

        ResourceBundle validationMessages = getContainer().getService(I18NService.class).getValidationMessages();

        List<ConstraintViolation> violations = new ArrayList<>();
        if (tenant.getEnabled() == null || !tenant.getEnabled()) {
            ConstraintViolation violation = new ConstraintViolation();
            violation.setConstraintType(ConstraintViolation.Type.PARAMETER);
            violation.setPath("Tenant.enabled");
            violation.setMessage(validationMessages.getString("Tenant.masterDisabled"));
            violations.add(violation);
        }
        if (tenant.getRealm() == null || !tenant.getRealm().equals(MASTER_REALM)) {
            ConstraintViolation violation = new ConstraintViolation();
            violation.setConstraintType(ConstraintViolation.Type.PARAMETER);
            violation.setPath("Tenant.realm");
            violation.setMessage(validationMessages.getString("Tenant.masterRealmChanged"));
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
