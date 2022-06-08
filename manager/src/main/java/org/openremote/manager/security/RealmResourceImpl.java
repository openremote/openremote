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
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.security.Realm;
import org.openremote.model.security.RealmResource;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.openremote.model.Constants.MASTER_REALM;

public class RealmResourceImpl extends ManagerWebResource implements RealmResource {

    private static final Logger LOG = Logger.getLogger(RealmResourceImpl.class.getName());
    protected Container container;

    public RealmResourceImpl(TimerService timerService, ManagerIdentityService identityService, Container container) {
        super(timerService, identityService);
        this.container = container;
    }

    @Override
    public Realm[] getAll(RequestParams requestParams) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            return identityService.getIdentityProvider().getRealms();
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Realm[] getAccessible(RequestParams requestParams) {
        try {
            Realm[] realms;

            if (isSuperUser()) {
                realms = identityService.getIdentityProvider().getRealms();
            } else {
                realms = new Realm[] {
                    (isAuthenticated() ? getAuthenticatedRealm() : getRequestRealm())
                };
            }
            return Arrays.stream(realms).map(realm -> new Realm().setName(realm.getName()).setDisplayName(realm.getDisplayName())).toArray(Realm[]::new);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public Realm get(RequestParams requestParams, String realmName) {
        Realm realm = identityService.getIdentityProvider().getRealm(realmName);
        if (realm == null)
            throw new WebApplicationException(NOT_FOUND);
        if (!isRealmActiveAndAccessible(realm)) {
            LOG.info("Forbidden access for user '" + getUsername() + "': " + realm);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return realm;
    }

    @Override
    public void update(RequestParams requestParams, String realmName, Realm realm) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        throwIfIllegalMasterRealmMutation(realmName, realm);

        try {
            identityService.getIdentityProvider().updateRealm(realm);
        } catch (ClientErrorException ex) {
            throw new WebApplicationException(ex.getCause(), ex.getResponse().getStatus());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(ex.getCause(), HttpStatus.SC_CONFLICT);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public void create(RequestParams requestParams, Realm realm) {
        if (!isSuperUser()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            identityService.getIdentityProvider().createRealm(realm);
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
            identityService.getIdentityProvider().deleteRealm(
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

    protected void throwIfIllegalMasterRealmMutation(String realmName, Realm realm) throws WebApplicationException {
        if (!realmName.equals(MASTER_REALM))
            return;

        if (realm.getEnabled() == null || !realm.getEnabled()) {
            throw new NotAllowedException("The master realmName cannot be disabled");
        }

        if (realm.getName() == null || !realm.getName().equals(MASTER_REALM)) {
            throw new NotAllowedException("The master realmName identifier cannot be changed");
        }
    }
}
