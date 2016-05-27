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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.manager.shared.security.Tenant;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TenantResourceImpl extends WebResource implements TenantResource {

    private static final Logger LOG = Logger.getLogger(TenantResourceImpl.class.getName());

    protected final ManagerIdentityService managerIdentityService;

    public TenantResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public Tenant[] getAll(RequestParams requestParams) {
        try {
            List<RealmRepresentation> realms = managerIdentityService.getRealms(requestParams).findAll();
            List<Tenant> validatedRealms = new ArrayList<>();
            for (RealmRepresentation realm : realms) {
                validatedRealms.add(convertTo(realm));
            }
            return validatedRealms.toArray(new Tenant[validatedRealms.size()]);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Tenant get(RequestParams requestParams, String realm) {
        try {
            return convertTo(managerIdentityService.getRealms(requestParams).realm(realm).toRepresentation());
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void update(RequestParams requestParams, String realm, Tenant tenant) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realm).update(convertFrom(tenant));
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void create(RequestParams requestParams, Tenant tenant) {
        try {
            managerIdentityService.getRealms(requestParams).create(convertFrom(tenant));
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void delete(RequestParams requestParams, String realm) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realm).remove();
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected Tenant convertTo(RealmRepresentation realm) {
        ObjectMapper json = getContainer().JSON;
        Map<String,Object> props = json.convertValue(realm, Map.class);
        return json.convertValue(props, Tenant.class);
    }

    protected RealmRepresentation convertFrom(Tenant tenant) {
        ObjectMapper json = getContainer().JSON;
        Map<String,Object> props = json.convertValue(tenant, Map.class);
        return json.convertValue(props, RealmRepresentation.class);
    }

}
