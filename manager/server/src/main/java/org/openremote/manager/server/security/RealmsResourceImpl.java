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

import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.security.RealmsResource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.logging.Logger;

public class RealmsResourceImpl extends WebResource implements RealmsResource {

    private static final Logger LOG = Logger.getLogger(RealmsResourceImpl.class.getName());

    protected final ManagerIdentityService managerIdentityService;

    public RealmsResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public RealmRepresentation[] getRealms(RequestParams requestParams) {
        try {
            List<RealmRepresentation> realms = managerIdentityService.getRealms(requestParams).findAll();
            return realms.toArray(new RealmRepresentation[realms.size()]);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public RealmRepresentation getRealm(RequestParams requestParams, String realmName) {
        try {
            return managerIdentityService.getRealms(requestParams).realm(realmName).toRepresentation();
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateRealm(@BeanParam RequestParams requestParams, String realmName, RealmRepresentation realm) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realmName).update(realm);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void createRealm(@BeanParam RequestParams requestParams, RealmRepresentation realm) {
        try {
            managerIdentityService.getRealms(requestParams).create(realm);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void deleteRealm(@BeanParam RequestParams requestParams, String realmName) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realmName).remove();
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
}
