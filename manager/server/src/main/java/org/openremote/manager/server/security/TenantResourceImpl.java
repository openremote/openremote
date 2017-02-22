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

import org.openremote.container.web.WebResource;
import org.openremote.manager.server.i18n.I18NService;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.manager.shared.validation.ConstraintViolationReport;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.openremote.model.Constants.*;
import static org.openremote.manager.shared.validation.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

public class TenantResourceImpl extends WebResource implements TenantResource {

    protected final ManagerIdentityService managerIdentityService;

    public TenantResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public Tenant[] getAll(RequestParams requestParams) {
        try {
            return managerIdentityService.getTenants(requestParams.getBearerAuth());
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Tenant get(RequestParams requestParams, String realm) {
        try {
            return managerIdentityService.getTenant(requestParams.getBearerAuth(), realm);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void update(RequestParams requestParams, String realm, Tenant tenant) {
        // TODO If the realm name changes, what happens to all the assets?
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
            managerIdentityService.updateTenant(requestParams.getBearerAuth(), realm, tenant);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void create(RequestParams requestParams, Tenant tenant) {
        try {
            managerIdentityService.createTenant(requestParams.getBearerAuth(), tenant);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String realm) {
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
            managerIdentityService.deleteTenant(requestParams.getBearerAuth(), realm);
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
        violations.add(new ConstraintViolation(
            ConstraintViolation.Type.PARAMETER,
            validationMessages.getString("Tenant.masterDeleted")
        ));
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
            ConstraintViolation violation = new ConstraintViolation(
                ConstraintViolation.Type.PARAMETER,
                "Tenant.enabled",
                validationMessages.getString("Tenant.masterDisabled")
            );
            violations.add(violation);
        }
        if (tenant.getRealm() == null || !tenant.getRealm().equals(MASTER_REALM)) {
            ConstraintViolation violation = new ConstraintViolation(
                ConstraintViolation.Type.PARAMETER,
                "Tenant.realm",
                validationMessages.getString("Tenant.masterRealmChanged")
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

}
