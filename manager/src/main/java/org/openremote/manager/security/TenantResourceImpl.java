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
import org.openremote.container.timer.TimerService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Container;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.TenantResource;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.openremote.model.Constants.MASTER_REALM;

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
    public Tenant[] getAccessible(RequestParams requestParams) {
        try {
            Tenant[] tenants = isSuperUser() ? identityService.getIdentityProvider().getTenants() : new Tenant[] {getAuthenticatedTenant()};
            return Arrays.stream(tenants).map(tenant -> new Tenant().setRealm(tenant.getRealm()).setDisplayName(tenant.getDisplayName())).toArray(Tenant[]::new);
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
            LOG.info("Forbidden access for user '" + getUsername() + "': " + tenant);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return tenant;
    }

    @Override
    public void update(RequestParams requestParams, String realm, Tenant tenant) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        throwIfIllegalMasterRealmMutation(realm, tenant);

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
            identityService.getIdentityProvider().createTenant(tenant);
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
        throwIfIllegalMasterRealmDeletion(realm);

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

    protected void throwIfIllegalMasterRealmDeletion(String realm) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM))
            return;

        throw new NotAllowedException("The master realm cannot be deleted");
    }

    protected void throwIfIllegalMasterRealmMutation(String realm, Tenant tenant) throws WebApplicationException {
        if (!realm.equals(MASTER_REALM))
            return;

        if (tenant.getEnabled() == null || !tenant.getEnabled()) {
            throw new NotAllowedException("The master realm cannot be disabled");
        }

        if (tenant.getRealm() == null || !tenant.getRealm().equals(MASTER_REALM)) {
            throw new NotAllowedException("The master realm identifier cannot be changed");
        }
    }
}
