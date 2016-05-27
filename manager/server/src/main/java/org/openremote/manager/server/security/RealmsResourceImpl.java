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
import org.openremote.manager.shared.security.RealmsResource;
import org.openremote.manager.shared.security.ValidatedRealmRepresentation;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RealmsResourceImpl extends WebResource implements RealmsResource {

    private static final Logger LOG = Logger.getLogger(RealmsResourceImpl.class.getName());

    protected final ManagerIdentityService managerIdentityService;

    public RealmsResourceImpl(ManagerIdentityService managerIdentityService) {
        this.managerIdentityService = managerIdentityService;
    }

    @Override
    public ValidatedRealmRepresentation[] getRealms(RequestParams requestParams) {
        try {
            List<RealmRepresentation> realms = managerIdentityService.getRealms(requestParams).findAll();
            List<ValidatedRealmRepresentation> validatedRealms = new ArrayList<>();
            for (RealmRepresentation realm : realms) {
                validatedRealms.add(convert(realm));
            }
            return validatedRealms.toArray(new ValidatedRealmRepresentation[validatedRealms.size()]);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public ValidatedRealmRepresentation getRealm(RequestParams requestParams, String realmName) {
        try {
            return convert(managerIdentityService.getRealms(requestParams).realm(realmName).toRepresentation());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void updateRealm(RequestParams requestParams, String realmName, ValidatedRealmRepresentation realm) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realmName).update(realm);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void createRealm(RequestParams requestParams, ValidatedRealmRepresentation realm) {
        try {
            managerIdentityService.getRealms(requestParams).create(realm);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void deleteRealm(RequestParams requestParams, String realmName) {
        try {
            managerIdentityService.getRealms(requestParams).realm(realmName).remove();
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected ValidatedRealmRepresentation convert(RealmRepresentation realm) {
        ObjectMapper json = getContainer().JSON;
        Map<String,Object> props = json.convertValue(realm, Map.class);
        return json.convertValue(props, ValidatedRealmRepresentation.class);
    }
}
